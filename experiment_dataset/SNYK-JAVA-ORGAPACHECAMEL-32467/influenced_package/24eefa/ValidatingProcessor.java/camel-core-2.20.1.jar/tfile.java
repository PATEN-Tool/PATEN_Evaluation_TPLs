// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.camel.processor.validation;

import org.slf4j.LoggerFactory;
import org.apache.camel.TypeConverter;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.camel.RuntimeTransformException;
import org.apache.camel.ExpectedBodyTypeException;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSResourceResolver;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.net.URL;
import org.xml.sax.SAXException;
import java.io.IOException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.validation.Validator;
import javax.xml.validation.Schema;
import java.io.Closeable;
import org.apache.camel.util.IOHelper;
import org.xml.sax.SAXParseException;
import java.util.Collections;
import org.xml.sax.ErrorHandler;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import java.io.InputStream;
import org.apache.camel.AsyncCallback;
import org.apache.camel.util.AsyncProcessorHelper;
import org.apache.camel.Exchange;
import org.apache.camel.converter.jaxp.XmlConverter;
import org.slf4j.Logger;
import org.apache.camel.AsyncProcessor;

public class ValidatingProcessor implements AsyncProcessor
{
    private static final Logger LOG;
    private final SchemaReader schemaReader;
    private ValidatorErrorHandler errorHandler;
    private final XmlConverter converter;
    private boolean useDom;
    private boolean useSharedSchema;
    private boolean failOnNullBody;
    private boolean failOnNullHeader;
    private String headerName;
    
    public ValidatingProcessor() {
        this.errorHandler = new DefaultValidationErrorHandler();
        this.converter = new XmlConverter();
        this.useSharedSchema = true;
        this.failOnNullBody = true;
        this.failOnNullHeader = true;
        this.schemaReader = new SchemaReader();
    }
    
    public ValidatingProcessor(final SchemaReader schemaReader) {
        this.errorHandler = new DefaultValidationErrorHandler();
        this.converter = new XmlConverter();
        this.useSharedSchema = true;
        this.failOnNullBody = true;
        this.failOnNullHeader = true;
        this.schemaReader = schemaReader;
    }
    
    @Override
    public void process(final Exchange exchange) throws Exception {
        AsyncProcessorHelper.process(this, exchange);
    }
    
    @Override
    public boolean process(final Exchange exchange, final AsyncCallback callback) {
        try {
            this.doProcess(exchange);
        }
        catch (Exception e) {
            exchange.setException(e);
        }
        callback.done(true);
        return true;
    }
    
    protected void doProcess(final Exchange exchange) throws Exception {
        Schema schema;
        if (this.isUseSharedSchema()) {
            schema = this.getSchema();
        }
        else {
            schema = this.createSchema();
        }
        final Validator validator = schema.newValidator();
        Source source = null;
        InputStream is = null;
        try {
            Result result = null;
            if (this.isInputStreamNeeded(exchange)) {
                is = this.getContentToValidate(exchange, InputStream.class);
                if (is != null) {
                    source = this.getSource(exchange, is);
                }
            }
            else {
                final Object content = this.getContentToValidate(exchange);
                if (content != null) {
                    source = this.getSource(exchange, content);
                }
            }
            if (this.shouldUseHeader()) {
                if (source == null && this.isFailOnNullHeader()) {
                    throw new NoXmlHeaderValidationException(exchange, this.headerName);
                }
            }
            else if (source == null && this.isFailOnNullBody()) {
                throw new NoXmlBodyValidationException(exchange);
            }
            if (source instanceof DOMSource) {
                result = new DOMResult();
            }
            else if (source instanceof SAXSource) {
                result = new SAXResult();
            }
            else if (source instanceof StAXSource || source instanceof StreamSource) {
                result = null;
            }
            if (source != null) {
                final ValidatorErrorHandler handler = (ValidatorErrorHandler)this.errorHandler.getClass().newInstance();
                validator.setErrorHandler(handler);
                try {
                    ValidatingProcessor.LOG.trace("Validating {}", (Object)source);
                    validator.validate(source, result);
                    handler.handleErrors(exchange, schema, result);
                }
                catch (SAXParseException e) {
                    throw new SchemaValidationException(exchange, schema, Collections.singletonList(e), Collections.emptyList(), Collections.emptyList());
                }
            }
        }
        finally {
            IOHelper.close(is);
        }
    }
    
    private Object getContentToValidate(final Exchange exchange) {
        if (this.shouldUseHeader()) {
            return exchange.getIn().getHeader(this.headerName);
        }
        return exchange.getIn().getBody();
    }
    
    private <T> T getContentToValidate(final Exchange exchange, final Class<T> clazz) {
        if (this.shouldUseHeader()) {
            return exchange.getIn().getHeader(this.headerName, clazz);
        }
        return exchange.getIn().getBody(clazz);
    }
    
    private boolean shouldUseHeader() {
        return this.headerName != null;
    }
    
    public void loadSchema() throws Exception {
        this.schemaReader.loadSchema();
    }
    
    public Schema getSchema() throws IOException, SAXException {
        return this.schemaReader.getSchema();
    }
    
    public void setSchema(final Schema schema) {
        this.schemaReader.setSchema(schema);
    }
    
    public String getSchemaLanguage() {
        return this.schemaReader.getSchemaLanguage();
    }
    
    public void setSchemaLanguage(final String schemaLanguage) {
        this.schemaReader.setSchemaLanguage(schemaLanguage);
    }
    
    public Source getSchemaSource() throws IOException {
        return this.schemaReader.getSchemaSource();
    }
    
    public void setSchemaSource(final Source schemaSource) {
        this.schemaReader.setSchemaSource(schemaSource);
    }
    
    public URL getSchemaUrl() {
        return this.schemaReader.getSchemaUrl();
    }
    
    public void setSchemaUrl(final URL schemaUrl) {
        this.schemaReader.setSchemaUrl(schemaUrl);
    }
    
    public File getSchemaFile() {
        return this.schemaReader.getSchemaFile();
    }
    
    public void setSchemaFile(final File schemaFile) {
        this.schemaReader.setSchemaFile(schemaFile);
    }
    
    public byte[] getSchemaAsByteArray() {
        return this.schemaReader.getSchemaAsByteArray();
    }
    
    public void setSchemaAsByteArray(final byte[] schemaAsByteArray) {
        this.schemaReader.setSchemaAsByteArray(schemaAsByteArray);
    }
    
    public SchemaFactory getSchemaFactory() {
        return this.schemaReader.getSchemaFactory();
    }
    
    public void setSchemaFactory(final SchemaFactory schemaFactory) {
        this.schemaReader.setSchemaFactory(schemaFactory);
    }
    
    public ValidatorErrorHandler getErrorHandler() {
        return this.errorHandler;
    }
    
    public void setErrorHandler(final ValidatorErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }
    
    @Deprecated
    public boolean isUseDom() {
        return this.useDom;
    }
    
    @Deprecated
    public void setUseDom(final boolean useDom) {
        this.useDom = useDom;
    }
    
    public boolean isUseSharedSchema() {
        return this.useSharedSchema;
    }
    
    public void setUseSharedSchema(final boolean useSharedSchema) {
        this.useSharedSchema = useSharedSchema;
    }
    
    public LSResourceResolver getResourceResolver() {
        return this.schemaReader.getResourceResolver();
    }
    
    public void setResourceResolver(final LSResourceResolver resourceResolver) {
        this.schemaReader.setResourceResolver(resourceResolver);
    }
    
    public boolean isFailOnNullBody() {
        return this.failOnNullBody;
    }
    
    public void setFailOnNullBody(final boolean failOnNullBody) {
        this.failOnNullBody = failOnNullBody;
    }
    
    public boolean isFailOnNullHeader() {
        return this.failOnNullHeader;
    }
    
    public void setFailOnNullHeader(final boolean failOnNullHeader) {
        this.failOnNullHeader = failOnNullHeader;
    }
    
    public String getHeaderName() {
        return this.headerName;
    }
    
    public void setHeaderName(final String headerName) {
        this.headerName = headerName;
    }
    
    protected SchemaFactory createSchemaFactory() {
        return this.schemaReader.createSchemaFactory();
    }
    
    protected Source createSchemaSource() throws IOException {
        return this.schemaReader.createSchemaSource();
    }
    
    protected Schema createSchema() throws SAXException, IOException {
        return this.schemaReader.createSchema();
    }
    
    protected boolean isInputStreamNeeded(final Exchange exchange) {
        final Object content = this.getContentToValidate(exchange);
        return content != null && (content instanceof InputStream || (!(content instanceof Source) && !(content instanceof String) && !(content instanceof byte[]) && !(content instanceof Node) && exchange.getContext().getTypeConverterRegistry().lookup(Source.class, content.getClass()) == null));
    }
    
    protected Source getSource(final Exchange exchange, final Object content) {
        if (this.isUseDom()) {
            return exchange.getContext().getTypeConverter().tryConvertTo(DOMSource.class, exchange, content);
        }
        if (content instanceof Source) {
            return (Source)content;
        }
        Source source = null;
        if (content instanceof InputStream) {
            return new StreamSource((InputStream)content);
        }
        if (content != null) {
            final TypeConverter tc = exchange.getContext().getTypeConverterRegistry().lookup(Source.class, content.getClass());
            if (tc != null) {
                source = tc.convertTo(Source.class, exchange, content);
            }
        }
        if (source == null) {
            source = exchange.getContext().getTypeConverter().tryConvertTo(SAXSource.class, exchange, content);
        }
        if (source == null) {
            source = exchange.getContext().getTypeConverter().tryConvertTo(StreamSource.class, exchange, content);
        }
        if (source == null) {
            source = exchange.getContext().getTypeConverter().tryConvertTo(DOMSource.class, exchange, content);
        }
        if (source == null) {
            if (this.isFailOnNullBody()) {
                throw new ExpectedBodyTypeException(exchange, Source.class);
            }
            try {
                source = this.converter.toDOMSource(this.converter.createDocument());
            }
            catch (ParserConfigurationException e) {
                throw new RuntimeTransformException(e);
            }
        }
        return source;
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)ValidatingProcessor.class);
    }
}
