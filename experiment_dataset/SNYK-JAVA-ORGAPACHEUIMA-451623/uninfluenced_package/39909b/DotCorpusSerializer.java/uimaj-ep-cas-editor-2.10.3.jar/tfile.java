// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.uima.caseditor.core.model.dotcorpus;

import java.util.Iterator;
import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.apache.uima.util.XMLSerializer;
import java.io.OutputStream;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.eclipse.core.runtime.IStatus;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.uima.caseditor.editor.AnnotationStyle;
import java.awt.Color;
import org.w3c.dom.Element;
import java.io.IOException;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.apache.uima.internal.util.XMLUtils;
import java.io.InputStream;

public class DotCorpusSerializer
{
    private static final String CONFIG_ELEMENT = "config";
    private static final String CORPUS_ELEMENT = "corpus";
    private static final String CORPUS_FOLDER_ATTRIBUTE = "folder";
    private static final String STYLE_ELEMENT = "style";
    private static final String STYLE_TYPE_ATTRIBUTE = "type";
    private static final String STYLE_STYLE_ATTRIBUTE = "style";
    private static final String STYLE_COLOR_ATTRIBUTE = "color";
    private static final String STYLE_LAYER_ATTRIBUTE = "layer";
    private static final String STYLE_CONFIG_ATTRIBUTE = "config";
    private static final String TYPESYSTEM_ELEMENT = "typesystem";
    private static final String TYPESYTEM_FILE_ATTRIBUTE = "file";
    private static final String CAS_PROCESSOR_ELEMENT = "processor";
    private static final String CAS_PROCESSOR_FOLDER_ATTRIBUTE = "folder";
    private static final String EDITOR_ELEMENT = "editor";
    private static final String EDITOR_LINE_LENGTH_ATTRIBUTE = "line-length-hint";
    private static final String SHOWN_ELEMENT = "shown";
    private static final String SHOWN_TYPE_ATTRIBUTE = "type";
    private static final String SHOWN_IS_VISISBLE_ATTRIBUTE = "visible";
    
    public static DotCorpus parseDotCorpus(final InputStream dotCorpusStream) throws CoreException {
        final DocumentBuilderFactory documentBuilderFacoty = XMLUtils.createDocumentBuilderFactory();
        DocumentBuilder documentBuilder;
        try {
            documentBuilder = documentBuilderFacoty.newDocumentBuilder();
        }
        catch (ParserConfigurationException e) {
            final String message = "This should never happen:" + ((e.getMessage() != null) ? e.getMessage() : "");
            final IStatus s = (IStatus)new Status(4, "org.apache.uima.caseditor", 0, message, (Throwable)e);
            throw new CoreException(s);
        }
        Document dotCorpusDOM;
        try {
            dotCorpusDOM = documentBuilder.parse(dotCorpusStream);
        }
        catch (SAXException e2) {
            final String message2 = (e2.getMessage() != null) ? e2.getMessage() : "";
            final IStatus s2 = (IStatus)new Status(4, "org.apache.uima.caseditor", 0, message2, (Throwable)e2);
            throw new CoreException(s2);
        }
        catch (IOException e3) {
            final String message2 = (e3.getMessage() != null) ? e3.getMessage() : "";
            final IStatus s2 = (IStatus)new Status(4, "org.apache.uima.caseditor", 0, message2, (Throwable)e3);
            throw new CoreException(s2);
        }
        final DotCorpus dotCorpus = new DotCorpus();
        final Element configElement = dotCorpusDOM.getDocumentElement();
        if ("config".equals(configElement.getNodeName())) {}
        final NodeList corporaChildNodes = configElement.getChildNodes();
        for (int i = 0; i < corporaChildNodes.getLength(); ++i) {
            final Node corporaChildNode = corporaChildNodes.item(i);
            if (corporaChildNode instanceof Element) {
                final Element corporaChildElement = (Element)corporaChildNode;
                if ("typesystem".equals(corporaChildElement.getNodeName())) {
                    dotCorpus.setTypeSystemFilename(corporaChildElement.getAttribute("file"));
                }
                else if ("corpus".equals(corporaChildElement.getNodeName())) {
                    final String corpusFolderName = corporaChildElement.getAttribute("folder");
                    dotCorpus.addCorpusFolder(corpusFolderName);
                }
                else if ("style".equals(corporaChildElement.getNodeName())) {
                    final String type = corporaChildElement.getAttribute("type");
                    final String styleString = corporaChildElement.getAttribute("style");
                    final int colorInteger = Integer.parseInt(corporaChildElement.getAttribute("color"));
                    final Color color = new Color(colorInteger);
                    final String drawingLayerString = corporaChildElement.getAttribute("layer");
                    String drawingConfigString = corporaChildElement.getAttribute("config");
                    if (drawingConfigString.length() == 0) {
                        drawingConfigString = null;
                    }
                    int drawingLayer;
                    try {
                        drawingLayer = Integer.parseInt(drawingLayerString);
                    }
                    catch (NumberFormatException e4) {
                        drawingLayer = 0;
                    }
                    final AnnotationStyle style = new AnnotationStyle(type, AnnotationStyle.Style.valueOf(styleString), color, drawingLayer, drawingConfigString);
                    dotCorpus.setStyle(style);
                }
                else if ("processor".equals(corporaChildElement.getNodeName())) {
                    dotCorpus.addCasProcessorFolder(corporaChildElement.getAttribute("folder"));
                }
                else if ("editor".equals(corporaChildElement.getNodeName())) {
                    final String lineLengthHintString = corporaChildElement.getAttribute("line-length-hint");
                    final int lineLengthHint = Integer.parseInt(lineLengthHintString);
                    dotCorpus.setEditorLineLength(lineLengthHint);
                }
                else {
                    if (!"shown".equals(corporaChildElement.getNodeName())) {
                        final String message3 = "Unexpected element: " + corporaChildElement.getNodeName();
                        final IStatus s3 = (IStatus)new Status(4, "org.apache.uima.caseditor", 0, message3, (Throwable)null);
                        throw new CoreException(s3);
                    }
                    final String type = corporaChildElement.getAttribute("type");
                    final String isVisisbleString = corporaChildElement.getAttribute("visible");
                    final boolean isVisible = Boolean.parseBoolean(isVisisbleString);
                    if (isVisible) {
                        dotCorpus.setShownType(type);
                    }
                }
            }
        }
        return dotCorpus;
    }
    
    public static void serialize(final DotCorpus dotCorpus, final OutputStream out) throws CoreException {
        final XMLSerializer xmlSerializer = new XMLSerializer(out, true);
        final ContentHandler xmlSerHandler = xmlSerializer.getContentHandler();
        try {
            xmlSerHandler.startDocument();
            xmlSerHandler.startElement("", "config", "config", new AttributesImpl());
            for (final String corpusFolder : dotCorpus.getCorpusFolderNameList()) {
                final AttributesImpl corpusFolderAttributes = new AttributesImpl();
                corpusFolderAttributes.addAttribute("", "", "folder", "", corpusFolder);
                xmlSerHandler.startElement("", "corpus", "corpus", corpusFolderAttributes);
                xmlSerHandler.endElement("", "corpus", "corpus");
            }
            for (final AnnotationStyle style : dotCorpus.getAnnotationStyles()) {
                final AttributesImpl styleAttributes = new AttributesImpl();
                styleAttributes.addAttribute("", "", "type", "", style.getAnnotation());
                styleAttributes.addAttribute("", "", "style", "", style.getStyle().name());
                final Color color = style.getColor();
                final Integer colorInt = new Color(color.getRed(), color.getGreen(), color.getBlue()).getRGB();
                styleAttributes.addAttribute("", "", "color", "", colorInt.toString());
                styleAttributes.addAttribute("", "", "layer", "", Integer.toString(style.getLayer()));
                if (style.getConfiguration() != null) {
                    styleAttributes.addAttribute("", "", "config", "", style.getConfiguration());
                }
                xmlSerHandler.startElement("", "style", "style", styleAttributes);
                xmlSerHandler.endElement("", "style", "style");
            }
            for (final String type : dotCorpus.getShownTypes()) {
                final AttributesImpl shownAttributes = new AttributesImpl();
                shownAttributes.addAttribute("", "", "type", "", type);
                shownAttributes.addAttribute("", "", "visible", "", "true");
                xmlSerHandler.startElement("", "shown", "shown", shownAttributes);
                xmlSerHandler.endElement("", "shown", "shown");
            }
            if (dotCorpus.getTypeSystemFileName() != null) {
                final AttributesImpl typeSystemFileAttributes = new AttributesImpl();
                typeSystemFileAttributes.addAttribute("", "", "file", "", dotCorpus.getTypeSystemFileName());
                xmlSerHandler.startElement("", "typesystem", "typesystem", typeSystemFileAttributes);
                xmlSerHandler.endElement("", "typesystem", "typesystem");
            }
            for (final String folder : dotCorpus.getCasProcessorFolderNames()) {
                final AttributesImpl taggerConfigAttributes = new AttributesImpl();
                taggerConfigAttributes.addAttribute("", "", "folder", "", folder);
                xmlSerHandler.startElement("", "processor", "processor", taggerConfigAttributes);
                xmlSerHandler.endElement("", "processor", "processor");
            }
            if (dotCorpus.getEditorLineLengthHint() != 80) {
                final AttributesImpl editorLineLengthHintAttributes = new AttributesImpl();
                editorLineLengthHintAttributes.addAttribute("", "", "line-length-hint", "", Integer.toString(dotCorpus.getEditorLineLengthHint()));
                xmlSerHandler.startElement("", "editor", "editor", editorLineLengthHintAttributes);
                xmlSerHandler.endElement("", "editor", "editor");
            }
            xmlSerHandler.endElement("", "config", "config");
            xmlSerHandler.endDocument();
        }
        catch (SAXException e) {
            final String message = (e.getMessage() != null) ? e.getMessage() : "";
            final IStatus s = (IStatus)new Status(4, "org.apache.uima.caseditor", 0, message, (Throwable)e);
            throw new CoreException(s);
        }
    }
}
