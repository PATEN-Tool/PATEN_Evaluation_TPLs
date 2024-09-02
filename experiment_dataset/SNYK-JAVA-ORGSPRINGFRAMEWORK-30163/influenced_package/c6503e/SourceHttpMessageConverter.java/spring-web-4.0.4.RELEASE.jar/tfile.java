// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.http.converter.xml;

import java.util.HashSet;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.HttpOutputMessage;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Result;
import java.io.OutputStream;
import javax.xml.transform.stream.StreamResult;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLInputFactory;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import java.io.ByteArrayInputStream;
import org.springframework.util.StreamUtils;
import org.xml.sax.helpers.XMLReaderFactory;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Node;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.converter.HttpMessageConversionException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.dom.DOMSource;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import javax.xml.transform.TransformerFactory;
import java.util.Set;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import javax.xml.transform.Source;

public class SourceHttpMessageConverter<T extends Source> extends AbstractHttpMessageConverter<T>
{
    private static final Set<Class<?>> SUPPORTED_CLASSES;
    private final TransformerFactory transformerFactory;
    private boolean processExternalEntities;
    
    public SourceHttpMessageConverter() {
        super(new MediaType[] { MediaType.APPLICATION_XML, MediaType.TEXT_XML, new MediaType("application", "*+xml") });
        this.transformerFactory = TransformerFactory.newInstance();
        this.processExternalEntities = false;
    }
    
    public void setProcessExternalEntities(final boolean processExternalEntities) {
        this.processExternalEntities = processExternalEntities;
    }
    
    public boolean isProcessExternalEntities() {
        return this.processExternalEntities;
    }
    
    public boolean supports(final Class<?> clazz) {
        return SourceHttpMessageConverter.SUPPORTED_CLASSES.contains(clazz);
    }
    
    @Override
    protected T readInternal(final Class<? extends T> clazz, final HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        final InputStream body = inputMessage.getBody();
        if (DOMSource.class.equals(clazz)) {
            return (T)this.readDOMSource(body);
        }
        if (SAXSource.class.equals(clazz)) {
            return (T)this.readSAXSource(body);
        }
        if (StAXSource.class.equals(clazz)) {
            return (T)this.readStAXSource(body);
        }
        if (StreamSource.class.equals(clazz) || Source.class.equals(clazz)) {
            return (T)this.readStreamSource(body);
        }
        throw new HttpMessageConversionException("Could not read class [" + clazz + "]. Only DOMSource, SAXSource, StAXSource, and StreamSource are supported.");
    }
    
    private DOMSource readDOMSource(final InputStream body) throws IOException {
        try {
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", this.processExternalEntities);
            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            final Document document = documentBuilder.parse(body);
            return new DOMSource(document);
        }
        catch (ParserConfigurationException ex) {
            throw new HttpMessageNotReadableException("Could not set feature: " + ex.getMessage(), ex);
        }
        catch (SAXException ex2) {
            throw new HttpMessageNotReadableException("Could not parse document: " + ex2.getMessage(), ex2);
        }
    }
    
    private SAXSource readSAXSource(final InputStream body) throws IOException {
        try {
            final XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setFeature("http://xml.org/sax/features/external-general-entities", this.processExternalEntities);
            final byte[] bytes = StreamUtils.copyToByteArray(body);
            return new SAXSource(reader, new InputSource(new ByteArrayInputStream(bytes)));
        }
        catch (SAXException ex) {
            throw new HttpMessageNotReadableException("Could not parse document: " + ex.getMessage(), ex);
        }
    }
    
    private Source readStAXSource(final InputStream body) {
        try {
            final XMLInputFactory inputFactory = XMLInputFactory.newFactory();
            inputFactory.setProperty("javax.xml.stream.isSupportingExternalEntities", this.processExternalEntities);
            final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(body);
            return new StAXSource(streamReader);
        }
        catch (XMLStreamException ex) {
            throw new HttpMessageNotReadableException("Could not parse document: " + ex.getMessage(), ex);
        }
    }
    
    private StreamSource readStreamSource(final InputStream body) throws IOException {
        final byte[] bytes = StreamUtils.copyToByteArray(body);
        return new StreamSource(new ByteArrayInputStream(bytes));
    }
    
    @Override
    protected Long getContentLength(final T t, final MediaType contentType) {
        if (t instanceof DOMSource) {
            try {
                final CountingOutputStream os = new CountingOutputStream();
                this.transform(t, new StreamResult(os));
                return os.count;
            }
            catch (TransformerException ex) {}
        }
        return null;
    }
    
    @Override
    protected void writeInternal(final T t, final HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        try {
            final Result result = new StreamResult(outputMessage.getBody());
            this.transform(t, result);
        }
        catch (TransformerException ex) {
            throw new HttpMessageNotWritableException("Could not transform [" + t + "] to output message", ex);
        }
    }
    
    private void transform(final Source source, final Result result) throws TransformerException {
        this.transformerFactory.newTransformer().transform(source, result);
    }
    
    static {
        (SUPPORTED_CLASSES = new HashSet<Class<?>>(5)).add(DOMSource.class);
        SourceHttpMessageConverter.SUPPORTED_CLASSES.add(SAXSource.class);
        SourceHttpMessageConverter.SUPPORTED_CLASSES.add(StAXSource.class);
        SourceHttpMessageConverter.SUPPORTED_CLASSES.add(StreamSource.class);
        SourceHttpMessageConverter.SUPPORTED_CLASSES.add(Source.class);
    }
    
    private static class CountingOutputStream extends OutputStream
    {
        long count;
        
        private CountingOutputStream() {
            this.count = 0L;
        }
        
        @Override
        public void write(final int b) throws IOException {
            ++this.count;
        }
        
        @Override
        public void write(final byte[] b) throws IOException {
            this.count += b.length;
        }
        
        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            this.count += len;
        }
    }
}
