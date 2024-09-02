// 
// Decompiled by Procyon v0.5.36
// 

package org.picketlink.common.util;

import org.picketlink.common.PicketLinkLoggerFactory;
import javax.xml.transform.dom.DOMResult;
import org.w3c.dom.DOMConfiguration;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathException;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import javax.xml.namespace.QName;
import javax.xml.transform.Transformer;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import java.io.Writer;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import javax.xml.transform.dom.DOMSource;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.picketlink.common.exceptions.ParsingException;
import java.io.Reader;
import java.io.StringReader;
import org.picketlink.common.exceptions.ProcessingException;
import org.w3c.dom.DOMException;
import org.w3c.dom.DocumentType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.picketlink.common.exceptions.ConfigurationException;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilderFactory;
import org.picketlink.common.PicketLinkLogger;

public class DocumentUtil
{
    private static final PicketLinkLogger logger;
    private static DocumentBuilderFactory documentBuilderFactory;
    
    public static boolean containsNode(final Document doc, final Node node) {
        if (node.getNodeType() == 1) {
            final Element elem = (Element)node;
            final NodeList nl = doc.getElementsByTagNameNS(elem.getNamespaceURI(), elem.getLocalName());
            return nl != null && nl.getLength() > 0;
        }
        throw new UnsupportedOperationException();
    }
    
    public static Document createDocument() throws ConfigurationException {
        final DocumentBuilderFactory factory = getDocumentBuilderFactory();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        }
        catch (ParserConfigurationException e) {
            throw new ConfigurationException(e);
        }
        return builder.newDocument();
    }
    
    public static Document createDocumentWithBaseNamespace(final String baseNamespace, final String localPart) throws ProcessingException {
        try {
            final DocumentBuilderFactory factory = getDocumentBuilderFactory();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.getDOMImplementation().createDocument(baseNamespace, localPart, null);
        }
        catch (DOMException e) {
            throw DocumentUtil.logger.processingError(e);
        }
        catch (ParserConfigurationException e2) {
            throw DocumentUtil.logger.processingError(e2);
        }
    }
    
    public static Document getDocument(final String docString) throws ConfigurationException, ParsingException, ProcessingException {
        return getDocument(new StringReader(docString));
    }
    
    public static Document getDocument(final Reader reader) throws ConfigurationException, ProcessingException, ParsingException {
        try {
            final DocumentBuilderFactory factory = getDocumentBuilderFactory();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(reader));
        }
        catch (ParserConfigurationException e) {
            throw DocumentUtil.logger.configurationError(e);
        }
        catch (SAXException e2) {
            throw DocumentUtil.logger.parserError(e2);
        }
        catch (IOException e3) {
            throw DocumentUtil.logger.processingError(e3);
        }
    }
    
    public static Document getDocument(final File file) throws ConfigurationException, ProcessingException, ParsingException {
        final DocumentBuilderFactory factory = getDocumentBuilderFactory();
        try {
            final DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(file);
        }
        catch (ParserConfigurationException e) {
            throw DocumentUtil.logger.configurationError(e);
        }
        catch (SAXException e2) {
            throw DocumentUtil.logger.parserError(e2);
        }
        catch (IOException e3) {
            throw DocumentUtil.logger.processingError(e3);
        }
    }
    
    public static Document getDocument(final InputStream is) throws ConfigurationException, ProcessingException, ParsingException {
        final DocumentBuilderFactory factory = getDocumentBuilderFactory();
        try {
            final DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(is);
        }
        catch (ParserConfigurationException e) {
            throw DocumentUtil.logger.configurationError(e);
        }
        catch (SAXException e2) {
            throw DocumentUtil.logger.parserError(e2);
        }
        catch (IOException e3) {
            throw DocumentUtil.logger.processingError(e3);
        }
    }
    
    public static String getDocumentAsString(final Document signedDoc) throws ProcessingException, ConfigurationException {
        final Source source = new DOMSource(signedDoc);
        final StringWriter sw = new StringWriter();
        final Result streamResult = new StreamResult(sw);
        final Transformer xformer = TransformerUtil.getTransformer();
        try {
            xformer.transform(source, streamResult);
        }
        catch (TransformerException e) {
            throw DocumentUtil.logger.processingError(e);
        }
        return sw.toString();
    }
    
    public static String getDOMElementAsString(final Element element) throws ProcessingException, ConfigurationException {
        final Source source = new DOMSource(element);
        final StringWriter sw = new StringWriter();
        final Result streamResult = new StreamResult(sw);
        final Transformer xformer = TransformerUtil.getTransformer();
        try {
            xformer.transform(source, streamResult);
        }
        catch (TransformerException e) {
            throw DocumentUtil.logger.processingError(e);
        }
        return sw.toString();
    }
    
    public static Element getElement(final Document doc, final QName elementQName) {
        NodeList nl = doc.getElementsByTagNameNS(elementQName.getNamespaceURI(), elementQName.getLocalPart());
        if (nl.getLength() == 0) {
            nl = doc.getElementsByTagNameNS("*", elementQName.getLocalPart());
            if (nl.getLength() == 0) {
                nl = doc.getElementsByTagName(elementQName.getPrefix() + ":" + elementQName.getLocalPart());
            }
            if (nl.getLength() == 0) {
                return null;
            }
        }
        return (Element)nl.item(0);
    }
    
    public static Element getChildElement(final Element doc, final QName elementQName) {
        NodeList nl = doc.getElementsByTagNameNS(elementQName.getNamespaceURI(), elementQName.getLocalPart());
        if (nl.getLength() == 0) {
            nl = doc.getElementsByTagNameNS("*", elementQName.getLocalPart());
            if (nl.getLength() == 0) {
                nl = doc.getElementsByTagName(elementQName.getPrefix() + ":" + elementQName.getLocalPart());
            }
            if (nl.getLength() == 0) {
                return null;
            }
        }
        return (Element)nl.item(0);
    }
    
    public static InputStream getNodeAsStream(final Node node) throws ConfigurationException, ProcessingException {
        return getSourceAsStream(new DOMSource(node));
    }
    
    public static InputStream getSourceAsStream(final Source source) throws ConfigurationException, ProcessingException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final Result streamResult = new StreamResult(baos);
        final Transformer transformer = TransformerUtil.getTransformer();
        try {
            transformer.transform(source, streamResult);
        }
        catch (TransformerException e) {
            throw DocumentUtil.logger.processingError(e);
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }
    
    public static String getNodeAsString(final Node node) throws ConfigurationException, ProcessingException {
        final Source source = new DOMSource(node);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final Result streamResult = new StreamResult(baos);
        final Transformer transformer = TransformerUtil.getTransformer();
        try {
            transformer.transform(source, streamResult);
        }
        catch (TransformerException e) {
            throw DocumentUtil.logger.processingError(e);
        }
        return new String(baos.toByteArray());
    }
    
    public static Node getNodeWithAttribute(final Document document, final String nsURI, final String nodeName, final String attributeName, final String attributeValue) throws XPathException, TransformerFactoryConfigurationError, TransformerException {
        final NodeList nl = document.getElementsByTagNameNS(nsURI, nodeName);
        for (int len = (nl != null) ? nl.getLength() : 0, i = 0; i < len; ++i) {
            final Node n = nl.item(i);
            if (n.getNodeType() == 1) {
                final Element el = (Element)n;
                String attrValue = el.getAttributeNS(nsURI, attributeName);
                if (attributeValue.equals(attrValue)) {
                    return el;
                }
                attrValue = el.getAttribute(attributeName);
                if (attributeValue.equals(attrValue)) {
                    return el;
                }
            }
        }
        return null;
    }
    
    public static Document normalizeNamespaces(final Document doc) {
        final DOMConfiguration docConfig = doc.getDomConfig();
        docConfig.setParameter("namespaces", Boolean.TRUE);
        doc.normalizeDocument();
        return doc;
    }
    
    public static Source getXMLSource(final Document doc) {
        return new DOMSource(doc);
    }
    
    public static String asString(final Document doc) {
        String str = null;
        try {
            str = getDocumentAsString(doc);
        }
        catch (Exception ex) {}
        return str;
    }
    
    public static void logNodes(final Document doc) {
        visit(doc, 0);
    }
    
    public static Node getNodeFromSource(final Source source) throws ProcessingException, ConfigurationException {
        try {
            final Transformer transformer = TransformerUtil.getTransformer();
            final DOMResult result = new DOMResult();
            TransformerUtil.transform(transformer, source, result);
            return result.getNode();
        }
        catch (ParsingException te) {
            throw DocumentUtil.logger.processingError(te);
        }
    }
    
    public static Document getDocumentFromSource(final Source source) throws ProcessingException, ConfigurationException {
        try {
            final Transformer transformer = TransformerUtil.getTransformer();
            final DOMResult result = new DOMResult();
            TransformerUtil.transform(transformer, source, result);
            return (Document)result.getNode();
        }
        catch (ParsingException te) {
            throw DocumentUtil.logger.processingError(te);
        }
    }
    
    private static void visit(final Node node, final int level) {
        final NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); ++i) {
            final Node childNode = list.item(i);
            DocumentUtil.logger.trace("Node=" + childNode.getNamespaceURI() + "::" + childNode.getLocalName());
            visit(childNode, level + 1);
        }
    }
    
    private static DocumentBuilderFactory getDocumentBuilderFactory() {
        final boolean tccl_jaxp = SystemPropertiesUtil.getSystemProperty("picketlink.jaxp.tccl", "false").equalsIgnoreCase("true");
        final ClassLoader prevTCCL = SecurityActions.getTCCL();
        if (DocumentUtil.documentBuilderFactory == null) {
            try {
                if (tccl_jaxp) {
                    SecurityActions.setTCCL(DocumentUtil.class.getClassLoader());
                }
                (DocumentUtil.documentBuilderFactory = DocumentBuilderFactory.newInstance()).setNamespaceAware(true);
                DocumentUtil.documentBuilderFactory.setXIncludeAware(true);
            }
            finally {
                if (tccl_jaxp) {
                    SecurityActions.setTCCL(prevTCCL);
                }
            }
        }
        return DocumentUtil.documentBuilderFactory;
    }
    
    static {
        logger = PicketLinkLoggerFactory.getLogger();
    }
}
