// 
// Decompiled by Procyon v0.5.36
// 

package org.opencastproject.caption.converters;

import org.slf4j.LoggerFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import javax.xml.parsers.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParserFactory;
import java.util.LinkedList;
import javax.xml.transform.Transformer;
import java.util.Iterator;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import java.io.Writer;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStreamWriter;
import org.apache.commons.io.IOUtils;
import java.io.OutputStream;
import org.opencastproject.caption.impl.CaptionImpl;
import org.w3c.dom.Node;
import org.opencastproject.caption.util.TimeUtil;
import org.opencastproject.caption.api.Time;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import org.opencastproject.caption.api.IllegalTimeFormatException;
import org.opencastproject.caption.impl.TimeImpl;
import org.w3c.dom.Element;
import java.io.IOException;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import org.opencastproject.caption.api.CaptionConverterException;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import org.opencastproject.caption.api.Caption;
import java.util.List;
import java.io.InputStream;
import org.slf4j.Logger;
import org.opencastproject.caption.api.CaptionConverter;

public class DFXPCaptionConverter implements CaptionConverter
{
    private static final Logger logger;
    private static final String EXTENSION = "dfxp.xml";
    
    public List<Caption> importCaption(final InputStream in, final String language) throws CaptionConverterException {
        final List<Caption> collection = new ArrayList<Caption>();
        Document doc;
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.parse(in);
            doc.getDocumentElement().normalize();
        }
        catch (ParserConfigurationException e) {
            throw new CaptionConverterException("Could not parse captions", (Throwable)e);
        }
        catch (SAXException e2) {
            throw new CaptionConverterException("Could not parse captions", (Throwable)e2);
        }
        catch (IOException e3) {
            throw new CaptionConverterException("Could not parse captions", (Throwable)e3);
        }
        final NodeList divElements = doc.getElementsByTagName("div");
        Element targetDiv = null;
        if (language != null) {
            for (int i = 0; i < divElements.getLength(); ++i) {
                final Element n = (Element)divElements.item(i);
                if (n.getAttribute("xml:lang").equals(language)) {
                    targetDiv = n;
                    break;
                }
            }
        }
        else {
            if (divElements.getLength() > 1) {
                DFXPCaptionConverter.logger.warn("More than one <div> element available. Parsing first one...");
            }
            if (divElements.getLength() != 0) {
                targetDiv = (Element)divElements.item(0);
            }
        }
        if (targetDiv == null) {
            DFXPCaptionConverter.logger.warn("No suitable <div> element found for language {}", (Object)language);
        }
        else {
            final NodeList pElements = targetDiv.getElementsByTagName("p");
            Time time = null;
            try {
                time = (Time)new TimeImpl(0, 0, 0, 0);
            }
            catch (IllegalTimeFormatException ex) {}
            for (int j = 0; j < pElements.getLength(); ++j) {
                try {
                    final Caption caption = this.parsePElement((Element)pElements.item(j));
                    if (caption.getStartTime().compareTo((Object)time) < 0 || caption.getStopTime().compareTo((Object)caption.getStartTime()) <= 0) {
                        DFXPCaptionConverter.logger.warn("Caption with invalid time encountered. Skipping...");
                    }
                    else {
                        collection.add(caption);
                    }
                }
                catch (IllegalTimeFormatException e4) {
                    DFXPCaptionConverter.logger.warn("Caption with invalid time format encountered. Skipping...");
                }
            }
        }
        return collection;
    }
    
    private Caption parsePElement(final Element p) throws IllegalTimeFormatException {
        final Time begin = TimeUtil.importDFXP(p.getAttribute("begin").trim());
        final Time end = TimeUtil.importDFXP(p.getAttribute("end").trim());
        final String[] textArray = this.getTextCore(p).split("\n");
        return (Caption)new CaptionImpl(begin, end, textArray);
    }
    
    private String getTextCore(final Node p) {
        final StringBuffer captionText = new StringBuffer();
        final NodeList list = p.getChildNodes();
        for (int i = 0; i < list.getLength(); ++i) {
            if (list.item(i).getNodeType() == 3) {
                captionText.append(list.item(i).getTextContent());
            }
            else if ("br".equals(list.item(i).getNodeName())) {
                captionText.append("\n");
            }
            else {
                captionText.append(this.getTextCore(list.item(i)));
            }
        }
        return captionText.toString().trim();
    }
    
    public void exportCaption(final OutputStream outputStream, final List<Caption> captions, final String language) throws IOException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = null;
        InputStream is = null;
        try {
            final DocumentBuilder builder = factory.newDocumentBuilder();
            is = DFXPCaptionConverter.class.getResourceAsStream("/templates/template.dfxp.xml");
            doc = builder.parse(is);
        }
        catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        catch (SAXException e2) {
            throw new RuntimeException(e2);
        }
        catch (IOException e3) {
            throw new RuntimeException(e3);
        }
        finally {
            IOUtils.closeQuietly(is);
        }
        final Node bodyNode = doc.getElementsByTagName("body").item(0);
        final Element divNode = doc.createElement("div");
        divNode.setAttribute("xml:lang", (language != null) ? language : "und");
        bodyNode.appendChild(divNode);
        for (final Caption caption : captions) {
            final Element newNode = doc.createElement("p");
            newNode.setAttribute("begin", TimeUtil.exportToDFXP(caption.getStartTime()));
            newNode.setAttribute("end", TimeUtil.exportToDFXP(caption.getStopTime()));
            final String[] captionText = caption.getCaption();
            newNode.appendChild(doc.createTextNode(captionText[0]));
            for (int i = 1; i < captionText.length; ++i) {
                newNode.appendChild(doc.createElement("br"));
                newNode.appendChild(doc.createTextNode(captionText[i]));
            }
            divNode.appendChild(newNode);
        }
        final OutputStreamWriter osw = new OutputStreamWriter(outputStream, "UTF-8");
        final StreamResult result = new StreamResult(osw);
        final DOMSource source = new DOMSource(doc);
        final TransformerFactory tfactory = TransformerFactory.newInstance();
        try {
            final Transformer transformer = tfactory.newTransformer();
            transformer.transform(source, result);
            osw.flush();
        }
        catch (TransformerConfigurationException e4) {
            throw new RuntimeException(e4);
        }
        catch (TransformerException e5) {
            throw new RuntimeException(e5);
        }
        finally {
            IOUtils.closeQuietly((Writer)osw);
        }
    }
    
    public String[] getLanguageList(final InputStream input) throws CaptionConverterException {
        final List<String> langList = new LinkedList<String>();
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            final SAXParser parser = factory.newSAXParser();
            final DefaultHandler handler = new DefaultHandler() {
                @Override
                public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
                    if ("div".equals(qName)) {
                        final String lang = attributes.getValue("xml:lang");
                        if (lang == null) {
                            DFXPCaptionConverter.logger.warn("Missing xml:lang attribute for div element.");
                        }
                        else if (langList.contains(lang)) {
                            DFXPCaptionConverter.logger.warn("Multiple div elements with same language.");
                        }
                        else {
                            langList.add(lang);
                        }
                    }
                }
            };
            parser.parse(input, handler);
        }
        catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        catch (SAXException e2) {
            throw new CaptionConverterException("Could not parse captions", (Throwable)e2);
        }
        catch (IOException e3) {
            throw new RuntimeException(e3);
        }
        return langList.toArray(new String[0]);
    }
    
    public String getExtension() {
        return "dfxp.xml";
    }
    
    public MediaPackageElement.Type getElementType() {
        return MediaPackageElement.Type.Attachment;
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)DFXPCaptionConverter.class);
    }
}
