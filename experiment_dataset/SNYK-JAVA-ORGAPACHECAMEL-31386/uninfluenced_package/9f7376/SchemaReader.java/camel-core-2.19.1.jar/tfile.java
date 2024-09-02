// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.camel.processor.validation;

import org.slf4j.LoggerFactory;
import java.io.Closeable;
import org.apache.camel.util.IOHelper;
import org.apache.camel.converter.IOConverter;
import org.apache.camel.util.ResourceHelper;
import java.io.InputStream;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import org.xml.sax.SAXException;
import java.io.IOException;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.CamelContext;
import org.w3c.dom.ls.LSResourceResolver;
import java.io.File;
import java.net.URL;
import javax.xml.validation.SchemaFactory;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import org.slf4j.Logger;

public class SchemaReader
{
    public static final String ACCESS_EXTERNAL_DTD = "CamelXmlValidatorAccessExternalDTD";
    private static final Logger LOG;
    private String schemaLanguage;
    private volatile Schema schema;
    private Source schemaSource;
    private volatile SchemaFactory schemaFactory;
    private URL schemaUrl;
    private File schemaFile;
    private byte[] schemaAsByteArray;
    private final String schemaResourceUri;
    private LSResourceResolver resourceResolver;
    private final CamelContext camelContext;
    
    public SchemaReader() {
        this.schemaLanguage = "http://www.w3.org/2001/XMLSchema";
        this.camelContext = null;
        this.schemaResourceUri = null;
    }
    
    public SchemaReader(final CamelContext camelContext, final String schemaResourceUri) {
        this.schemaLanguage = "http://www.w3.org/2001/XMLSchema";
        ObjectHelper.notNull(camelContext, "camelContext");
        ObjectHelper.notNull(schemaResourceUri, "schemaResourceUri");
        this.camelContext = camelContext;
        this.schemaResourceUri = schemaResourceUri;
    }
    
    public void loadSchema() throws Exception {
        this.schema = this.createSchema();
    }
    
    public Schema getSchema() throws IOException, SAXException {
        if (this.schema == null) {
            synchronized (this) {
                if (this.schema == null) {
                    this.schema = this.createSchema();
                }
            }
        }
        return this.schema;
    }
    
    public void setSchema(final Schema schema) {
        this.schema = schema;
    }
    
    public String getSchemaLanguage() {
        return this.schemaLanguage;
    }
    
    public void setSchemaLanguage(final String schemaLanguage) {
        this.schemaLanguage = schemaLanguage;
    }
    
    public Source getSchemaSource() throws IOException {
        if (this.schemaSource == null) {
            this.schemaSource = this.createSchemaSource();
        }
        return this.schemaSource;
    }
    
    public void setSchemaSource(final Source schemaSource) {
        this.schemaSource = schemaSource;
    }
    
    public URL getSchemaUrl() {
        return this.schemaUrl;
    }
    
    public void setSchemaUrl(final URL schemaUrl) {
        this.schemaUrl = schemaUrl;
    }
    
    public File getSchemaFile() {
        return this.schemaFile;
    }
    
    public void setSchemaFile(final File schemaFile) {
        this.schemaFile = schemaFile;
    }
    
    public byte[] getSchemaAsByteArray() {
        return this.schemaAsByteArray;
    }
    
    public void setSchemaAsByteArray(final byte[] schemaAsByteArray) {
        this.schemaAsByteArray = schemaAsByteArray;
    }
    
    public SchemaFactory getSchemaFactory() {
        if (this.schemaFactory == null) {
            synchronized (this) {
                if (this.schemaFactory == null) {
                    this.schemaFactory = this.createSchemaFactory();
                }
            }
        }
        return this.schemaFactory;
    }
    
    public void setSchemaFactory(final SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
    }
    
    public LSResourceResolver getResourceResolver() {
        return this.resourceResolver;
    }
    
    public void setResourceResolver(final LSResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }
    
    protected SchemaFactory createSchemaFactory() {
        final SchemaFactory factory = SchemaFactory.newInstance(this.schemaLanguage);
        if (this.getResourceResolver() != null) {
            factory.setResourceResolver(this.getResourceResolver());
        }
        if (this.camelContext != null) {
            if (Boolean.parseBoolean(this.camelContext.getGlobalOptions().get("CamelXmlValidatorAccessExternalDTD"))) {
                return factory;
            }
        }
        try {
            factory.setProperty("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
        }
        catch (SAXException e) {
            SchemaReader.LOG.warn(e.getMessage(), (Throwable)e);
        }
        return factory;
    }
    
    protected Source createSchemaSource() throws IOException {
        throw new IllegalArgumentException("You must specify either a schema, schemaFile, schemaSource, schemaUrl, or schemaUri property");
    }
    
    protected Schema createSchema() throws SAXException, IOException {
        final SchemaFactory factory = this.getSchemaFactory();
        final URL url = this.getSchemaUrl();
        if (url != null) {
            synchronized (this) {
                return factory.newSchema(url);
            }
        }
        final File file = this.getSchemaFile();
        if (file != null) {
            synchronized (this) {
                return factory.newSchema(file);
            }
        }
        byte[] bytes = this.getSchemaAsByteArray();
        if (bytes != null) {
            synchronized (this) {
                return factory.newSchema(new StreamSource(new ByteArrayInputStream(this.schemaAsByteArray)));
            }
        }
        if (this.schemaResourceUri != null) {
            synchronized (this) {
                bytes = this.readSchemaResource();
                return factory.newSchema(new StreamSource(new ByteArrayInputStream(bytes)));
            }
        }
        final Source source = this.getSchemaSource();
        synchronized (this) {
            return factory.newSchema(source);
        }
    }
    
    protected byte[] readSchemaResource() throws IOException {
        SchemaReader.LOG.debug("reading schema resource: {}", (Object)this.schemaResourceUri);
        final InputStream is = ResourceHelper.resolveMandatoryResourceAsInputStream(this.camelContext, this.schemaResourceUri);
        byte[] bytes = null;
        try {
            bytes = IOConverter.toBytes(is);
        }
        finally {
            IOHelper.close(is);
        }
        return bytes;
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)SchemaReader.class);
    }
}
