// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.http.converter.xml;

import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.MediaType;
import java.io.OutputStream;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import javax.xml.transform.TransformerException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageConversionException;
import java.io.InputStream;
import org.xml.sax.InputSource;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import org.springframework.http.HttpHeaders;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.Source;

public class SourceHttpMessageConverter<T extends Source> extends AbstractXmlHttpMessageConverter<T>
{
    public boolean supports(final Class<?> clazz) {
        return DOMSource.class.equals(clazz) || SAXSource.class.equals(clazz) || StreamSource.class.equals(clazz) || Source.class.equals(clazz);
    }
    
    @Override
    protected T readFromSource(final Class clazz, final HttpHeaders headers, final Source source) throws IOException {
        try {
            if (DOMSource.class.equals(clazz)) {
                final DOMResult domResult = new DOMResult();
                this.transform(source, domResult);
                return (T)new DOMSource(domResult.getNode());
            }
            if (SAXSource.class.equals(clazz)) {
                final ByteArrayInputStream bis = this.transformToByteArrayInputStream(source);
                return (T)new SAXSource(new InputSource(bis));
            }
            if (StreamSource.class.equals(clazz) || Source.class.equals(clazz)) {
                final ByteArrayInputStream bis = this.transformToByteArrayInputStream(source);
                return (T)new StreamSource(bis);
            }
            throw new HttpMessageConversionException("Could not read class [" + clazz + "]. Only DOMSource, SAXSource, and StreamSource are supported.");
        }
        catch (TransformerException ex) {
            throw new HttpMessageNotReadableException("Could not transform from [" + source + "] to [" + clazz + "]", ex);
        }
    }
    
    private ByteArrayInputStream transformToByteArrayInputStream(final Source source) throws TransformerException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        this.transform(source, new StreamResult(bos));
        return new ByteArrayInputStream(bos.toByteArray());
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
    protected void writeToResult(final T t, final HttpHeaders headers, final Result result) throws IOException {
        try {
            this.transform(t, result);
        }
        catch (TransformerException ex) {
            throw new HttpMessageNotWritableException("Could not transform [" + t + "] to [" + result + "]", ex);
        }
    }
    
    private static class CountingOutputStream extends OutputStream
    {
        private long count;
        
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
