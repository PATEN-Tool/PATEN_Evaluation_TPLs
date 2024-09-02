// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.oxm.support;

import java.io.StringReader;
import java.io.Reader;
import java.io.InputStream;
import java.io.Writer;
import java.io.OutputStream;
import org.xml.sax.InputSource;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.Source;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.ContentHandler;
import org.springframework.util.Assert;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamWriter;
import org.w3c.dom.Node;
import org.springframework.oxm.XmlMappingException;
import java.io.IOException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.sax.SAXResult;
import org.springframework.util.xml.StaxUtils;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.Result;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.XMLReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.oxm.UnmarshallingFailureException;
import org.w3c.dom.Document;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.logging.Log;
import org.xml.sax.EntityResolver;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.Marshaller;

public abstract class AbstractMarshaller implements Marshaller, Unmarshaller
{
    private static final EntityResolver NO_OP_ENTITY_RESOLVER;
    protected final Log logger;
    private boolean supportDtd;
    private boolean processExternalEntities;
    @Nullable
    private DocumentBuilderFactory documentBuilderFactory;
    private final Object documentBuilderFactoryMonitor;
    
    public AbstractMarshaller() {
        this.logger = LogFactory.getLog((Class)this.getClass());
        this.supportDtd = false;
        this.processExternalEntities = false;
        this.documentBuilderFactoryMonitor = new Object();
    }
    
    public void setSupportDtd(final boolean supportDtd) {
        this.supportDtd = supportDtd;
    }
    
    public boolean isSupportDtd() {
        return this.supportDtd;
    }
    
    public void setProcessExternalEntities(final boolean processExternalEntities) {
        this.processExternalEntities = processExternalEntities;
        if (processExternalEntities) {
            this.supportDtd = true;
        }
    }
    
    public boolean isProcessExternalEntities() {
        return this.processExternalEntities;
    }
    
    protected Document buildDocument() {
        try {
            final DocumentBuilder documentBuilder;
            synchronized (this.documentBuilderFactoryMonitor) {
                if (this.documentBuilderFactory == null) {
                    this.documentBuilderFactory = this.createDocumentBuilderFactory();
                }
                documentBuilder = this.createDocumentBuilder(this.documentBuilderFactory);
            }
            return documentBuilder.newDocument();
        }
        catch (ParserConfigurationException ex) {
            throw new UnmarshallingFailureException("Could not create document placeholder: " + ex.getMessage(), ex);
        }
    }
    
    protected DocumentBuilderFactory createDocumentBuilderFactory() throws ParserConfigurationException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !this.isSupportDtd());
        factory.setFeature("http://xml.org/sax/features/external-general-entities", this.isProcessExternalEntities());
        return factory;
    }
    
    protected DocumentBuilder createDocumentBuilder(final DocumentBuilderFactory factory) throws ParserConfigurationException {
        final DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        if (!this.isProcessExternalEntities()) {
            documentBuilder.setEntityResolver(AbstractMarshaller.NO_OP_ENTITY_RESOLVER);
        }
        return documentBuilder;
    }
    
    protected XMLReader createXmlReader() throws SAXException {
        final XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !this.isSupportDtd());
        xmlReader.setFeature("http://xml.org/sax/features/external-general-entities", this.isProcessExternalEntities());
        if (!this.isProcessExternalEntities()) {
            xmlReader.setEntityResolver(AbstractMarshaller.NO_OP_ENTITY_RESOLVER);
        }
        return xmlReader;
    }
    
    @Nullable
    protected String getDefaultEncoding() {
        return null;
    }
    
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
    
    protected void marshalDomResult(final Object graph, final DOMResult domResult) throws XmlMappingException {
        if (domResult.getNode() == null) {
            domResult.setNode(this.buildDocument());
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
            return this.unmarshalStreamSource((StreamSource)source);
        }
        throw new IllegalArgumentException("Unknown Source type: " + source.getClass());
    }
    
    protected Object unmarshalDomSource(final DOMSource domSource) throws XmlMappingException {
        if (domSource.getNode() == null) {
            domSource.setNode(this.buildDocument());
        }
        try {
            return this.unmarshalDomNode(domSource.getNode());
        }
        catch (NullPointerException ex) {
            if (!this.isSupportDtd()) {
                throw new UnmarshallingFailureException("NPE while unmarshalling. This can happen on JDK 1.6 due to the presence of DTD declarations, which are disabled.", ex);
            }
            throw ex;
        }
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
        try {
            return this.unmarshalSaxReader(saxSource.getXMLReader(), saxSource.getInputSource());
        }
        catch (NullPointerException ex2) {
            if (!this.isSupportDtd()) {
                throw new UnmarshallingFailureException("NPE while unmarshalling. This can happen on JDK 1.6 due to the presence of DTD declarations, which are disabled.");
            }
            throw ex2;
        }
    }
    
    protected Object unmarshalStreamSource(final StreamSource streamSource) throws XmlMappingException, IOException {
        if (streamSource.getInputStream() != null) {
            if (this.isProcessExternalEntities() && this.isSupportDtd()) {
                return this.unmarshalInputStream(streamSource.getInputStream());
            }
            final InputSource inputSource = new InputSource(streamSource.getInputStream());
            inputSource.setEncoding(this.getDefaultEncoding());
            return this.unmarshalSaxSource(new SAXSource(inputSource));
        }
        else {
            if (streamSource.getReader() == null) {
                return this.unmarshalSaxSource(new SAXSource(new InputSource(streamSource.getSystemId())));
            }
            if (this.isProcessExternalEntities() && this.isSupportDtd()) {
                return this.unmarshalReader(streamSource.getReader());
            }
            return this.unmarshalSaxSource(new SAXSource(new InputSource(streamSource.getReader())));
        }
    }
    
    protected abstract void marshalDomNode(final Object p0, final Node p1) throws XmlMappingException;
    
    protected abstract void marshalXmlEventWriter(final Object p0, final XMLEventWriter p1) throws XmlMappingException;
    
    protected abstract void marshalXmlStreamWriter(final Object p0, final XMLStreamWriter p1) throws XmlMappingException;
    
    protected abstract void marshalSaxHandlers(final Object p0, final ContentHandler p1, @Nullable final LexicalHandler p2) throws XmlMappingException;
    
    protected abstract void marshalOutputStream(final Object p0, final OutputStream p1) throws XmlMappingException, IOException;
    
    protected abstract void marshalWriter(final Object p0, final Writer p1) throws XmlMappingException, IOException;
    
    protected abstract Object unmarshalDomNode(final Node p0) throws XmlMappingException;
    
    protected abstract Object unmarshalXmlEventReader(final XMLEventReader p0) throws XmlMappingException;
    
    protected abstract Object unmarshalXmlStreamReader(final XMLStreamReader p0) throws XmlMappingException;
    
    protected abstract Object unmarshalSaxReader(final XMLReader p0, final InputSource p1) throws XmlMappingException, IOException;
    
    protected abstract Object unmarshalInputStream(final InputStream p0) throws XmlMappingException, IOException;
    
    protected abstract Object unmarshalReader(final Reader p0) throws XmlMappingException, IOException;
    
    static {
        final InputSource inputSource;
        NO_OP_ENTITY_RESOLVER = ((publicId, systemId) -> {
            new InputSource(new StringReader(""));
            return inputSource;
        });
    }
}
