// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.uima.internal.util;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.XMLReader;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXNotRecognizedException;
import org.apache.uima.util.Level;
import org.apache.uima.UIMAFramework;
import javax.xml.parsers.SAXParserFactory;
import org.w3c.dom.Text;
import java.lang.reflect.Constructor;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import java.io.IOException;
import java.io.Writer;

public abstract class XMLUtils
{
    private static final String ACCESS_EXTERNAL_STYLESHEET = "http://javax.xml.XMLConstants/property/accessExternalStylesheet";
    private static final String ACCESS_EXTERNAL_DTD = "http://javax.xml.XMLConstants/property/accessExternalDTD";
    private static final String DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    private static final String EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
    private static final String EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
    
    public static void normalize(final String aStr, final StringBuffer aResultBuf) {
        normalize(aStr, aResultBuf, false);
    }
    
    public static void normalize(final String aStr, final StringBuffer aResultBuf, final boolean aNewlinesToSpaces) {
        if (aStr != null) {
            for (int len = aStr.length(), i = 0; i < len; ++i) {
                final char c = aStr.charAt(i);
                if (c > '\u007f') {
                    aResultBuf.append("&#").append((int)c).append(';');
                }
                else {
                    switch (c) {
                        case '<': {
                            aResultBuf.append("&lt;");
                            break;
                        }
                        case '>': {
                            aResultBuf.append("&gt;");
                            break;
                        }
                        case '&': {
                            aResultBuf.append("&amp;");
                            break;
                        }
                        case '\"': {
                            aResultBuf.append("&quot;");
                            break;
                        }
                        case '\n': {
                            aResultBuf.append(aNewlinesToSpaces ? " " : "\n");
                            break;
                        }
                        case '\r': {
                            aResultBuf.append(aNewlinesToSpaces ? " " : "\r");
                            break;
                        }
                        default: {
                            aResultBuf.append(c);
                            break;
                        }
                    }
                }
            }
        }
    }
    
    public static void writeNormalizedString(final String aStr, final Writer aWriter, final boolean aNewlinesToSpaces) throws IOException {
        if (aStr == null) {
            return;
        }
        for (int len = aStr.length(), i = 0; i < len; ++i) {
            final char c = aStr.charAt(i);
            switch (c) {
                case '<': {
                    aWriter.write("&lt;");
                    break;
                }
                case '>': {
                    aWriter.write("&gt;");
                    break;
                }
                case '&': {
                    aWriter.write("&amp;");
                    break;
                }
                case '\"': {
                    aWriter.write("&quot;");
                    break;
                }
                case '\n': {
                    aWriter.write(aNewlinesToSpaces ? " " : "\n");
                    break;
                }
                case '\r': {
                    aWriter.write(aNewlinesToSpaces ? " " : "\r");
                    break;
                }
                default: {
                    aWriter.write(c);
                    break;
                }
            }
        }
    }
    
    public static void writePrimitiveValue(final Object aObj, final Writer aWriter) throws IOException {
        String className = aObj.getClass().getName();
        final int lastDotIndex = className.lastIndexOf(".");
        if (lastDotIndex > -1) {
            className = className.substring(lastDotIndex + 1).toLowerCase();
        }
        aWriter.write("<");
        aWriter.write(className);
        aWriter.write(">");
        writeNormalizedString(aObj.toString(), aWriter, true);
        aWriter.write("</");
        aWriter.write(className);
        aWriter.write(">");
    }
    
    public static Element getChildByTagName(final Element aElem, final String aName) {
        final NodeList matches = aElem.getElementsByTagName(aName);
        for (int i = 0; i < matches.getLength(); ++i) {
            final Element childElem = (Element)matches.item(i);
            if (childElem.getParentNode() == aElem) {
                return childElem;
            }
        }
        return null;
    }
    
    public static Element getFirstChildElement(final Element aElem) {
        final NodeList children = aElem.getChildNodes();
        for (int len = children.getLength(), i = 0; i < len; ++i) {
            final Node curNode = children.item(i);
            if (curNode instanceof Element) {
                return (Element)curNode;
            }
        }
        return null;
    }
    
    public static Object readPrimitiveValue(final Element aElem) {
        String tagName = aElem.getTagName();
        if (tagName.endsWith("_p")) {
            tagName = tagName.substring(0, tagName.lastIndexOf("_p"));
        }
        final char[] chars = tagName.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        final String className = "java.lang." + new String(chars);
        final String stringifiedObject = getText(aElem, true);
        try {
            final Class<?> theClass = Class.forName(className);
            final Constructor<?> constructor = theClass.getConstructor(String.class);
            return constructor.newInstance(stringifiedObject);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e2) {
            return null;
        }
    }
    
    public static String getText(final Element aElem) {
        final StringBuffer buf = new StringBuffer();
        final NodeList children = aElem.getChildNodes();
        for (int len = children.getLength(), i = 0; i < len; ++i) {
            final Node curNode = children.item(i);
            if (curNode instanceof Text) {
                buf.append(((Text)curNode).getData());
            }
            else if (curNode instanceof Element) {
                buf.append('<').append(((Element)curNode).getTagName()).append('>');
                buf.append(getText((Element)curNode));
                buf.append("</").append(((Element)curNode).getTagName()).append('>');
            }
        }
        return buf.toString().trim();
    }
    
    public static String getText(final Element aElem, final boolean aExpandEnvVarRefs) {
        final StringBuffer buf = new StringBuffer();
        final NodeList children = aElem.getChildNodes();
        for (int len = children.getLength(), i = 0; i < len; ++i) {
            final Node curNode = children.item(i);
            if (curNode instanceof Text) {
                buf.append(((Text)curNode).getData());
            }
            else if (curNode instanceof Element) {
                final Element subElem = (Element)curNode;
                if (aExpandEnvVarRefs && "envVarRef".equals(subElem.getTagName())) {
                    final String varName = getText(subElem, false);
                    final String value = System.getProperty(varName);
                    if (value != null) {
                        buf.append(value);
                    }
                }
                else {
                    buf.append('<').append(((Element)curNode).getTagName()).append('>');
                    buf.append(getText((Element)curNode, aExpandEnvVarRefs));
                    buf.append("</").append(((Element)curNode).getTagName()).append('>');
                }
            }
        }
        return buf.toString().trim();
    }
    
    public static final int checkForNonXmlCharacters(final String s) {
        return checkForNonXmlCharacters(s, false);
    }
    
    public static final int checkForNonXmlCharacters(final String s, final boolean xml11) {
        if (s == null) {
            return -1;
        }
        for (int i = 0; i < s.length(); ++i) {
            final char c = s.charAt(i);
            if (!isValidXmlUtf16int(c, xml11)) {
                if (c >= '\ud800' && c <= '\udbff') {
                    final int iNext = i + 1;
                    if (iNext < s.length()) {
                        final char cNext = s.charAt(iNext);
                        if (cNext >= '\udc00' && cNext <= '\udfff') {
                            ++i;
                            continue;
                        }
                    }
                }
                return i;
            }
        }
        return -1;
    }
    
    public static final int checkForNonXmlCharacters(final char[] ch, final int start, final int length, final boolean xml11) {
        if (ch == null) {
            return -1;
        }
        for (int i = start; i < start + length; ++i) {
            final char c = ch[i];
            if (!isValidXmlUtf16int(c, xml11)) {
                if (c >= '\ud800' && c <= '\udbff') {
                    final int iNext = i + 1;
                    if (iNext < start + length) {
                        final char cNext = ch[iNext];
                        if (cNext >= '\udc00' && cNext <= '\udfff') {
                            ++i;
                            continue;
                        }
                    }
                }
                return i;
            }
        }
        return -1;
    }
    
    private static final boolean isValidXmlUtf16int(final char c, final boolean xml11) {
        if (xml11) {
            return (c >= '\u0001' && c <= '\ud7ff') || (c >= '\ue000' && c <= '\ufffd');
        }
        return c == '\t' || c == '\n' || c == '\r' || (c >= ' ' && c <= '\ud7ff') || (c >= '\ue000' && c <= '\ufffd');
    }
    
    public static SAXParserFactory createSAXParserFactory() {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        }
        catch (SAXNotRecognizedException e) {
            UIMAFramework.getLogger().log(Level.WARNING, "SAXParserFactory didn't recognize feature http://apache.org/xml/features/disallow-doctype-decl");
        }
        catch (SAXNotSupportedException e2) {
            UIMAFramework.getLogger().log(Level.WARNING, "SAXParserFactory doesn't support feature http://apache.org/xml/features/disallow-doctype-decl");
        }
        catch (ParserConfigurationException e3) {
            UIMAFramework.getLogger().log(Level.WARNING, "SAXParserFactory doesn't support feature http://apache.org/xml/features/disallow-doctype-decl");
        }
        try {
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        }
        catch (SAXNotRecognizedException e) {
            UIMAFramework.getLogger().log(Level.WARNING, "SAXParserFactory didn't recognize feature http://apache.org/xml/features/nonvalidating/load-external-dtd");
        }
        catch (SAXNotSupportedException e2) {
            UIMAFramework.getLogger().log(Level.WARNING, "SAXParserFactory doesn't support feature http://apache.org/xml/features/nonvalidating/load-external-dtd");
        }
        catch (ParserConfigurationException e3) {
            UIMAFramework.getLogger().log(Level.WARNING, "SAXParserFactory doesn't support feature http://apache.org/xml/features/nonvalidating/load-external-dtd");
        }
        factory.setXIncludeAware(false);
        return factory;
    }
    
    public static XMLReader createXMLReader() throws SAXException {
        final XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        try {
            xmlReader.setFeature("http://xml.org/sax/features/external-general-entities", false);
        }
        catch (SAXNotRecognizedException e) {
            UIMAFramework.getLogger().log(Level.WARNING, "XMLReader didn't recognize feature http://xml.org/sax/features/external-general-entities");
        }
        catch (SAXNotSupportedException e2) {
            UIMAFramework.getLogger().log(Level.WARNING, "XMLReader doesn't support feature http://xml.org/sax/features/external-general-entities");
        }
        try {
            xmlReader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        }
        catch (SAXNotRecognizedException e) {
            UIMAFramework.getLogger().log(Level.WARNING, "XMLReader didn't recognize feature http://xml.org/sax/features/external-parameter-entities");
        }
        catch (SAXNotSupportedException e2) {
            UIMAFramework.getLogger().log(Level.WARNING, "XMLReader doesn't support feature http://xml.org/sax/features/external-parameter-entities");
        }
        try {
            xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        }
        catch (SAXNotRecognizedException e) {
            UIMAFramework.getLogger().log(Level.WARNING, "XMLReader didn't recognized feature http://apache.org/xml/features/nonvalidating/load-external-dtd");
        }
        catch (SAXNotSupportedException e2) {
            UIMAFramework.getLogger().log(Level.WARNING, "XMLReader doesn't support feature http://apache.org/xml/features/nonvalidating/load-external-dtd");
        }
        return xmlReader;
    }
    
    public static SAXTransformerFactory createSaxTransformerFactory() {
        final SAXTransformerFactory saxTransformerFactory = (SAXTransformerFactory)TransformerFactory.newInstance();
        try {
            saxTransformerFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
        }
        catch (IllegalArgumentException e) {
            UIMAFramework.getLogger().log(Level.WARNING, "SAXTransformerFactory didn't recognize setting attribute http://javax.xml.XMLConstants/property/accessExternalDTD");
        }
        try {
            saxTransformerFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalStylesheet", "");
        }
        catch (IllegalArgumentException e) {
            UIMAFramework.getLogger().log(Level.WARNING, "SAXTransformerFactory didn't recognize setting attribute http://javax.xml.XMLConstants/property/accessExternalStylesheet");
        }
        return saxTransformerFactory;
    }
    
    public static TransformerFactory createTransformerFactory() {
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            transformerFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
        }
        catch (IllegalArgumentException e) {
            UIMAFramework.getLogger().log(Level.WARNING, "TransformerFactory didn't recognize setting attribute http://javax.xml.XMLConstants/property/accessExternalDTD");
        }
        try {
            transformerFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalStylesheet", "");
        }
        catch (IllegalArgumentException e) {
            UIMAFramework.getLogger().log(Level.WARNING, "TransformerFactory didn't recognize setting attribute http://javax.xml.XMLConstants/property/accessExternalStylesheet");
        }
        return transformerFactory;
    }
    
    public static DocumentBuilderFactory createDocumentBuilderFactory() {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        }
        catch (ParserConfigurationException e1) {
            UIMAFramework.getLogger().log(Level.WARNING, "DocumentBuilderFactory didn't recognize setting feature http://apache.org/xml/features/disallow-doctype-decl");
        }
        try {
            documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        }
        catch (ParserConfigurationException e2) {
            UIMAFramework.getLogger().log(Level.WARNING, "DocumentBuilderFactory doesn't support feature http://apache.org/xml/features/nonvalidating/load-external-dtd");
        }
        documentBuilderFactory.setXIncludeAware(false);
        documentBuilderFactory.setExpandEntityReferences(false);
        return documentBuilderFactory;
    }
}
