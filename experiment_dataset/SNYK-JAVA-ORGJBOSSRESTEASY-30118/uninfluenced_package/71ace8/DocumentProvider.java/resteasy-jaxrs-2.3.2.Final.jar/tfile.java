// 
// Decompiled by Procyon v0.5.36
// 

package org.jboss.resteasy.plugins.providers;

import javax.xml.transform.TransformerException;
import org.jboss.resteasy.spi.WriterException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Node;
import javax.xml.transform.dom.DOMSource;
import java.io.OutputStream;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import org.jboss.resteasy.spi.ReaderException;
import java.io.InputStream;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.MediaType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.servlet.ServletConfig;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import org.jboss.resteasy.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;
import org.w3c.dom.Document;

@Provider
@Produces({ "text/*+xml", "application/*+xml" })
@Consumes({ "text/*+xml", "application/*+xml" })
public class DocumentProvider extends AbstractEntityProvider<Document>
{
    private static final Logger logger;
    private final TransformerFactory transformerFactory;
    private final DocumentBuilderFactory documentBuilder;
    private boolean expandEntityReferences;
    
    public DocumentProvider(@Context final ServletConfig servletConfig) {
        this.expandEntityReferences = true;
        this.documentBuilder = DocumentBuilderFactory.newInstance();
        this.transformerFactory = TransformerFactory.newInstance();
        try {
            final ServletContext context = servletConfig.getServletContext();
            final String s = context.getInitParameter("resteasy.document.expand.entity.references");
            this.expandEntityReferences = (s == null || Boolean.parseBoolean(s));
        }
        catch (Exception e) {
            DocumentProvider.logger.debug("Unable to retrieve ServletContext: expandEntityReferences defaults to true");
        }
    }
    
    public boolean isReadable(final Class<?> clazz, final Type type, final Annotation[] annotation, final MediaType mediaType) {
        return Document.class.isAssignableFrom(clazz);
    }
    
    public Document readFrom(final Class<Document> clazz, final Type type, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String, String> headers, final InputStream input) throws IOException, WebApplicationException {
        try {
            this.documentBuilder.setExpandEntityReferences(this.expandEntityReferences);
            return this.documentBuilder.newDocumentBuilder().parse(input);
        }
        catch (Exception e) {
            throw new ReaderException(e);
        }
    }
    
    public boolean isWriteable(final Class<?> clazz, final Type type, final Annotation[] annotation, final MediaType mediaType) {
        return Document.class.isAssignableFrom(clazz);
    }
    
    public void writeTo(final Document document, final Class<?> clazz, final Type type, final Annotation[] annotation, final MediaType mediaType, final MultivaluedMap<String, Object> headers, final OutputStream output) throws IOException, WebApplicationException {
        try {
            final DOMSource source = new DOMSource(document);
            final StreamResult result = new StreamResult(output);
            this.transformerFactory.newTransformer().transform(source, result);
        }
        catch (TransformerException te) {
            throw new WriterException(te);
        }
    }
    
    static {
        logger = Logger.getLogger(DocumentProvider.class);
    }
}
