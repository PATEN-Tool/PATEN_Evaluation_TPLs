// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.http.converter.xml;

import java.io.Reader;
import java.io.StringReader;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.MarshalException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.ClassUtils;
import javax.xml.transform.Result;
import org.xml.sax.XMLReader;
import org.xml.sax.SAXException;
import javax.xml.transform.sax.SAXSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.InputSource;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import org.springframework.http.converter.HttpMessageConversionException;
import javax.xml.bind.UnmarshalException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import javax.xml.transform.Source;
import org.springframework.http.HttpHeaders;
import org.springframework.core.annotation.AnnotationUtils;
import javax.xml.bind.annotation.XmlType;
import java.lang.annotation.Annotation;
import javax.xml.bind.annotation.XmlRootElement;
import org.springframework.lang.Nullable;
import org.springframework.http.MediaType;
import org.xml.sax.EntityResolver;

public class Jaxb2RootElementHttpMessageConverter extends AbstractJaxb2HttpMessageConverter<Object>
{
    private boolean supportDtd;
    private boolean processExternalEntities;
    private static final EntityResolver NO_OP_ENTITY_RESOLVER;
    
    public Jaxb2RootElementHttpMessageConverter() {
        this.supportDtd = false;
        this.processExternalEntities = false;
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
    
    @Override
    public boolean canRead(final Class<?> clazz, @Nullable final MediaType mediaType) {
        return (clazz.isAnnotationPresent(XmlRootElement.class) || clazz.isAnnotationPresent(XmlType.class)) && this.canRead(mediaType);
    }
    
    @Override
    public boolean canWrite(final Class<?> clazz, @Nullable final MediaType mediaType) {
        return AnnotationUtils.findAnnotation((Class)clazz, (Class)XmlRootElement.class) != null && this.canWrite(mediaType);
    }
    
    @Override
    protected boolean supports(final Class<?> clazz) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    protected Object readFromSource(final Class<?> clazz, final HttpHeaders headers, Source source) throws IOException {
        try {
            source = this.processSource(source);
            final Unmarshaller unmarshaller = this.createUnmarshaller(clazz);
            if (clazz.isAnnotationPresent(XmlRootElement.class)) {
                return unmarshaller.unmarshal(source);
            }
            final JAXBElement<?> jaxbElement = unmarshaller.unmarshal(source, clazz);
            return jaxbElement.getValue();
        }
        catch (NullPointerException ex) {
            if (!this.isSupportDtd()) {
                throw new HttpMessageNotReadableException("NPE while unmarshalling. This can happen due to the presence of DTD declarations which are disabled.", ex);
            }
            throw ex;
        }
        catch (UnmarshalException ex2) {
            throw new HttpMessageNotReadableException("Could not unmarshal to [" + clazz + "]: " + ex2.getMessage(), ex2);
        }
        catch (JAXBException ex3) {
            throw new HttpMessageConversionException("Invalid JAXB setup: " + ex3.getMessage(), ex3);
        }
    }
    
    protected Source processSource(final Source source) {
        if (source instanceof StreamSource) {
            final StreamSource streamSource = (StreamSource)source;
            final InputSource inputSource = new InputSource(streamSource.getInputStream());
            try {
                final XMLReader xmlReader = XMLReaderFactory.createXMLReader();
                xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !this.isSupportDtd());
                final String featureName = "http://xml.org/sax/features/external-general-entities";
                xmlReader.setFeature(featureName, this.isProcessExternalEntities());
                if (!this.isProcessExternalEntities()) {
                    xmlReader.setEntityResolver(Jaxb2RootElementHttpMessageConverter.NO_OP_ENTITY_RESOLVER);
                }
                return new SAXSource(xmlReader, inputSource);
            }
            catch (SAXException ex) {
                this.logger.warn((Object)"Processing of external entities could not be disabled", (Throwable)ex);
                return source;
            }
        }
        return source;
    }
    
    @Override
    protected void writeToResult(final Object o, final HttpHeaders headers, final Result result) throws IOException {
        try {
            final Class<?> clazz = (Class<?>)ClassUtils.getUserClass(o);
            final Marshaller marshaller = this.createMarshaller(clazz);
            this.setCharset(headers.getContentType(), marshaller);
            marshaller.marshal(o, result);
        }
        catch (MarshalException ex) {
            throw new HttpMessageNotWritableException("Could not marshal [" + o + "]: " + ex.getMessage(), ex);
        }
        catch (JAXBException ex2) {
            throw new HttpMessageConversionException("Invalid JAXB setup: " + ex2.getMessage(), ex2);
        }
    }
    
    private void setCharset(@Nullable final MediaType contentType, final Marshaller marshaller) throws PropertyException {
        if (contentType != null && contentType.getCharset() != null) {
            marshaller.setProperty("jaxb.encoding", contentType.getCharset().name());
        }
    }
    
    static {
        final InputSource inputSource;
        NO_OP_ENTITY_RESOLVER = ((publicId, systemId) -> {
            new InputSource(new StringReader(""));
            return inputSource;
        });
    }
}
