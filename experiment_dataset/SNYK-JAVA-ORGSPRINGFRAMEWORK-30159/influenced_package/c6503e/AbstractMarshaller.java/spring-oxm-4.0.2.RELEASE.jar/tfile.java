// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.oxm.support;

import java.io.Reader;
import java.io.InputStream;
import java.io.Writer;
import java.io.OutputStream;
import org.xml.sax.InputSource;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.ContentHandler;
import org.springframework.util.Assert;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamWriter;
import org.springframework.oxm.UnmarshallingFailureException;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.XMLReader;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.Source;
import org.springframework.oxm.XmlMappingException;
import java.io.IOException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.sax.SAXResult;
import org.springframework.util.xml.StaxUtils;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.Result;
import org.apache.commons.logging.LogFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.logging.Log;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.Marshaller;

public abstract class AbstractMarshaller implements Marshaller, Unmarshaller
{
    protected final Log logger;
    private DocumentBuilderFactory documentBuilderFactory;
    private final Object documentBuilderFactoryMonitor;
    private boolean processExternalEntities;
    
    public AbstractMarshaller() {
        this.logger = LogFactory.getLog((Class)this.getClass());
        this.documentBuilderFactoryMonitor = new Object();
        this.processExternalEntities = false;
    }
    
    public void setProcessExternalEntities(final boolean processExternalEntities) {
        this.processExternalEntities = processExternalEntities;
    }
    
    public boolean isProcessExternalEntities() {
        return this.processExternalEntities;
    }
    
    protected abstract String getDefaultEncoding();
    
    @Override
    public final void marshal(final Object graph, final Result result) throws IOException, XmlMappingException {
        if (result instanceof DOMResult) {
            this.marshalDomResult(graph, (DOMResult)result);
        }
        else if (StaxUtils.isStaxResult(result)) {
            this.marshalStaxResult(graph, result);
        }
        else if (result instanceof SAXResult) {
            this.marshalSaxResult(graph, (SAXResult)result);
        }
        else {
            if (!(result instanceof StreamResult)) {
                throw new IllegalArgumentException("Unknown Result type: " + result.getClass());
            }
            this.marshalStreamResult(graph, (StreamResult)result);
        }
    }
    
    @Override
    public final Object unmarshal(final Source source) throws IOException, XmlMappingException {
        if (source instanceof DOMSource) {
            return this.unmarshalDomSource((DOMSource)source);
        }
        if (StaxUtils.isStaxSource(source)) {
            return this.unmarshalStaxSource(source);
        }
        if (source instanceof SAXSource) {
            return this.unmarshalSaxSource((SAXSource)source);
        }
        if (source instanceof StreamSource) {
            return this.unmarshalStreamSourceNoExternalEntitities((StreamSource)source);
        }
        throw new IllegalArgumentException("Unknown Source type: " + source.getClass());
    }
    
    protected DocumentBuilder createDocumentBuilder(final DocumentBuilderFactory factory) throws ParserConfigurationException {
        return factory.newDocumentBuilder();
    }
    
    protected DocumentBuilderFactory createDocumentBuilderFactory() throws ParserConfigurationException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        return factory;
    }
    
    protected XMLReader createXmlReader() throws SAXException {
        final XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setFeature("http://xml.org/sax/features/external-general-entities", this.isProcessExternalEntities());
        return xmlReader;
    }
    
    protected void marshalDomResult(final Object graph, final DOMResult domResult) throws XmlMappingException {
        if (domResult.getNode() == null) {
            try {
                synchronized (this.documentBuilderFactoryMonitor) {
                    if (this.documentBuilderFactory == null) {
                        this.documentBuilderFactory = this.createDocumentBuilderFactory();
                    }
                }
                final DocumentBuilder documentBuilder = this.createDocumentBuilder(this.documentBuilderFactory);
                domResult.setNode(documentBuilder.newDocument());
            }
            catch (ParserConfigurationException ex) {
                throw new UnmarshallingFailureException("Could not create document placeholder for DOMResult: " + ex.getMessage(), ex);
            }
        }
        this.marshalDomNode(graph, domResult.getNode());
    }
    
    protected void marshalStaxResult(final Object graph, final Result staxResult) throws XmlMappingException {
        final XMLStreamWriter streamWriter = StaxUtils.getXMLStreamWriter(staxResult);
        if (streamWriter != null) {
            this.marshalXmlStreamWriter(graph, streamWriter);
        }
        else {
            final XMLEventWriter eventWriter = StaxUtils.getXMLEventWriter(staxResult);
            if (eventWriter == null) {
                throw new IllegalArgumentException("StaxResult contains neither XMLStreamWriter nor XMLEventConsumer");
            }
            this.marshalXmlEventWriter(graph, eventWriter);
        }
    }
    
    protected void marshalSaxResult(final Object graph, final SAXResult saxResult) throws XmlMappingException {
        final ContentHandler contentHandler = saxResult.getHandler();
        Assert.notNull((Object)contentHandler, "ContentHandler not set on SAXResult");
        final LexicalHandler lexicalHandler = saxResult.getLexicalHandler();
        this.marshalSaxHandlers(graph, contentHandler, lexicalHandler);
    }
    
    protected void marshalStreamResult(final Object graph, final StreamResult streamResult) throws XmlMappingException, IOException {
        if (streamResult.getOutputStream() != null) {
            this.marshalOutputStream(graph, streamResult.getOutputStream());
        }
        else {
            if (streamResult.getWriter() == null) {
                throw new IllegalArgumentException("StreamResult contains neither OutputStream nor Writer");
            }
            this.marshalWriter(graph, streamResult.getWriter());
        }
    }
    
    protected Object unmarshalDomSource(final DOMSource domSource) throws XmlMappingException {
        if (domSource.getNode() == null) {
            try {
                synchronized (this.documentBuilderFactoryMonitor) {
                    if (this.documentBuilderFactory == null) {
                        this.documentBuilderFactory = this.createDocumentBuilderFactory();
                    }
                }
                final DocumentBuilder documentBuilder = this.createDocumentBuilder(this.documentBuilderFactory);
                domSource.setNode(documentBuilder.newDocument());
            }
            catch (ParserConfigurationException ex) {
                throw new UnmarshallingFailureException("Could not create document placeholder for DOMSource: " + ex.getMessage(), ex);
            }
        }
        return this.unmarshalDomNode(domSource.getNode());
    }
    
    protected Object unmarshalStaxSource(final Source staxSource) throws XmlMappingException {
        final XMLStreamReader streamReader = StaxUtils.getXMLStreamReader(staxSource);
        if (streamReader != null) {
            return this.unmarshalXmlStreamReader(streamReader);
        }
        final XMLEventReader eventReader = StaxUtils.getXMLEventReader(staxSource);
        if (eventReader != null) {
            return this.unmarshalXmlEventReader(eventReader);
        }
        throw new IllegalArgumentException("StaxSource contains neither XMLStreamReader nor XMLEventReader");
    }
    
    protected Object unmarshalSaxSource(final SAXSource saxSource) throws XmlMappingException, IOException {
        if (saxSource.getXMLReader() == null) {
            try {
                saxSource.setXMLReader(this.createXmlReader());
            }
            catch (SAXException ex) {
                throw new UnmarshallingFailureException("Could not create XMLReader for SAXSource", ex);
            }
        }
        if (saxSource.getInputSource() == null) {
            saxSource.setInputSource(new InputSource());
        }
        return this.unmarshalSaxReader(saxSource.getXMLReader(), saxSource.getInputSource());
    }
    
    protected Object unmarshalStreamSourceNoExternalEntitities(final StreamSource streamSource) throws XmlMappingException, IOException {
        InputSource inputSource;
        if (streamSource.getInputStream() != null) {
            inputSource = new InputSource(streamSource.getInputStream());
            inputSource.setEncoding(this.getDefaultEncoding());
        }
        else if (streamSource.getReader() != null) {
            inputSource = new InputSource(streamSource.getReader());
        }
        else {
            inputSource = new InputSource(streamSource.getSystemId());
        }
        return this.unmarshalSaxSource(new SAXSource(inputSource));
    }
    
    protected Object unmarshalStreamSource(final StreamSource streamSource) throws XmlMappingException, IOException {
        if (streamSource.getInputStream() != null) {
            return this.unmarshalInputStream(streamSource.getInputStream());
        }
        if (streamSource.getReader() != null) {
            return this.unmarshalReader(streamSource.getReader());
        }
        throw new IllegalArgumentException("StreamSource contains neither InputStream nor Reader");
    }
    
    protected abstract void marshalDomNode(final Object p0, final Node p1) throws XmlMappingException;
    
    protected abstract void marshalXmlEventWriter(final Object p0, final XMLEventWriter p1) throws XmlMappingException;
    
    protected abstract void marshalXmlStreamWriter(final Object p0, final XMLStreamWriter p1) throws XmlMappingException;
    
    protected abstract void marshalSaxHandlers(final Object p0, final ContentHandler p1, final LexicalHandler p2) throws XmlMappingException;
    
    protected abstract void marshalOutputStream(final Object p0, final OutputStream p1) throws XmlMappingException, IOException;
    
    protected abstract void marshalWriter(final Object p0, final Writer p1) throws XmlMappingException, IOException;
    
    protected abstract Object unmarshalDomNode(final Node p0) throws XmlMappingException;
    
    protected abstract Object unmarshalXmlEventReader(final XMLEventReader p0) throws XmlMappingException;
    
    protected abstract Object unmarshalXmlStreamReader(final XMLStreamReader p0) throws XmlMappingException;
    
    protected abstract Object unmarshalSaxReader(final XMLReader p0, final InputSource p1) throws XmlMappingException, IOException;
    
    protected abstract Object unmarshalInputStream(final InputStream p0) throws XmlMappingException, IOException;
    
    protected abstract Object unmarshalReader(final Reader p0) throws XmlMappingException, IOException;
}
