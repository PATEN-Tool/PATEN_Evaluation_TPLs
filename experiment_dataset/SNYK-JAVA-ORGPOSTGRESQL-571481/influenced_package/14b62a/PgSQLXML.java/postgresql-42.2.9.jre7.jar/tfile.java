// 
// Decompiled by Procyon v0.5.36
// 

package org.postgresql.jdbc;

import org.xml.sax.SAXParseException;
import javax.xml.transform.Transformer;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.TransformerException;
import org.xml.sax.ContentHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.Result;
import java.io.Writer;
import java.io.OutputStream;
import javax.xml.stream.XMLStreamReader;
import javax.xml.parsers.DocumentBuilder;
import org.postgresql.util.GT;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.sax.SAXSource;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.ErrorHandler;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.Source;
import java.io.StringReader;
import java.io.Reader;
import java.sql.SQLException;
import java.io.IOException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.xml.transform.dom.DOMResult;
import java.io.StringWriter;
import java.io.ByteArrayOutputStream;
import org.postgresql.core.BaseConnection;
import java.sql.SQLXML;

public class PgSQLXML implements SQLXML
{
    private final BaseConnection conn;
    private String data;
    private boolean initialized;
    private boolean active;
    private boolean freed;
    private ByteArrayOutputStream byteArrayOutputStream;
    private StringWriter stringWriter;
    private DOMResult domResult;
    
    public PgSQLXML(final BaseConnection conn) {
        this(conn, null, false);
    }
    
    public PgSQLXML(final BaseConnection conn, final String data) {
        this(conn, data, true);
    }
    
    private PgSQLXML(final BaseConnection conn, final String data, final boolean initialized) {
        this.conn = conn;
        this.data = data;
        this.initialized = initialized;
        this.active = false;
        this.freed = false;
    }
    
    @Override
    public synchronized void free() {
        this.freed = true;
        this.data = null;
    }
    
    @Override
    public synchronized InputStream getBinaryStream() throws SQLException {
        this.checkFreed();
        this.ensureInitialized();
        if (this.data == null) {
            return null;
        }
        try {
            return new ByteArrayInputStream(this.conn.getEncoding().encode(this.data));
        }
        catch (IOException ioe) {
            throw new PSQLException("Failed to re-encode xml data.", PSQLState.DATA_ERROR, ioe);
        }
    }
    
    @Override
    public synchronized Reader getCharacterStream() throws SQLException {
        this.checkFreed();
        this.ensureInitialized();
        if (this.data == null) {
            return null;
        }
        return new StringReader(this.data);
    }
    
    @Override
    public synchronized <T extends Source> T getSource(final Class<T> sourceClass) throws SQLException {
        this.checkFreed();
        this.ensureInitialized();
        if (this.data == null) {
            return null;
        }
        try {
            if (sourceClass == null || DOMSource.class.equals(sourceClass)) {
                final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                final DocumentBuilder builder = factory.newDocumentBuilder();
                builder.setErrorHandler(new NonPrintingErrorHandler());
                final InputSource input = new InputSource(new StringReader(this.data));
                return (T)new DOMSource(builder.parse(input));
            }
            if (SAXSource.class.equals(sourceClass)) {
                final InputSource is = new InputSource(new StringReader(this.data));
                return (T)new SAXSource(is);
            }
            if (StreamSource.class.equals(sourceClass)) {
                return (T)new StreamSource(new StringReader(this.data));
            }
            if (StAXSource.class.equals(sourceClass)) {
                final XMLInputFactory xif = XMLInputFactory.newInstance();
                final XMLStreamReader xsr = xif.createXMLStreamReader(new StringReader(this.data));
                return (T)new StAXSource(xsr);
            }
        }
        catch (Exception e) {
            throw new PSQLException(GT.tr("Unable to decode xml data.", new Object[0]), PSQLState.DATA_ERROR, e);
        }
        throw new PSQLException(GT.tr("Unknown XML Source class: {0}", sourceClass), PSQLState.INVALID_PARAMETER_TYPE);
    }
    
    @Override
    public synchronized String getString() throws SQLException {
        this.checkFreed();
        this.ensureInitialized();
        return this.data;
    }
    
    @Override
    public synchronized OutputStream setBinaryStream() throws SQLException {
        this.checkFreed();
        this.initialize();
        this.active = true;
        return this.byteArrayOutputStream = new ByteArrayOutputStream();
    }
    
    @Override
    public synchronized Writer setCharacterStream() throws SQLException {
        this.checkFreed();
        this.initialize();
        this.active = true;
        return this.stringWriter = new StringWriter();
    }
    
    @Override
    public synchronized <T extends Result> T setResult(final Class<T> resultClass) throws SQLException {
        this.checkFreed();
        this.initialize();
        if (resultClass == null || DOMResult.class.equals(resultClass)) {
            this.domResult = new DOMResult();
            this.active = true;
            return (T)this.domResult;
        }
        if (SAXResult.class.equals(resultClass)) {
            try {
                final SAXTransformerFactory transformerFactory = (SAXTransformerFactory)TransformerFactory.newInstance();
                final TransformerHandler transformerHandler = transformerFactory.newTransformerHandler();
                this.stringWriter = new StringWriter();
                transformerHandler.setResult(new StreamResult(this.stringWriter));
                this.active = true;
                return (T)new SAXResult(transformerHandler);
            }
            catch (TransformerException te) {
                throw new PSQLException(GT.tr("Unable to create SAXResult for SQLXML.", new Object[0]), PSQLState.UNEXPECTED_ERROR, te);
            }
        }
        if (StreamResult.class.equals(resultClass)) {
            this.stringWriter = new StringWriter();
            this.active = true;
            return (T)new StreamResult(this.stringWriter);
        }
        if (StAXResult.class.equals(resultClass)) {
            this.stringWriter = new StringWriter();
            try {
                final XMLOutputFactory xof = XMLOutputFactory.newInstance();
                final XMLStreamWriter xsw = xof.createXMLStreamWriter(this.stringWriter);
                this.active = true;
                return (T)new StAXResult(xsw);
            }
            catch (XMLStreamException xse) {
                throw new PSQLException(GT.tr("Unable to create StAXResult for SQLXML", new Object[0]), PSQLState.UNEXPECTED_ERROR, xse);
            }
        }
        throw new PSQLException(GT.tr("Unknown XML Result class: {0}", resultClass), PSQLState.INVALID_PARAMETER_TYPE);
    }
    
    @Override
    public synchronized void setString(final String value) throws SQLException {
        this.checkFreed();
        this.initialize();
        this.data = value;
    }
    
    private void checkFreed() throws SQLException {
        if (this.freed) {
            throw new PSQLException(GT.tr("This SQLXML object has already been freed.", new Object[0]), PSQLState.OBJECT_NOT_IN_STATE);
        }
    }
    
    private void ensureInitialized() throws SQLException {
        if (!this.initialized) {
            throw new PSQLException(GT.tr("This SQLXML object has not been initialized, so you cannot retrieve data from it.", new Object[0]), PSQLState.OBJECT_NOT_IN_STATE);
        }
        if (!this.active) {
            return;
        }
        if (this.byteArrayOutputStream != null) {
            try {
                this.data = this.conn.getEncoding().decode(this.byteArrayOutputStream.toByteArray());
            }
            catch (IOException ioe) {
                throw new PSQLException(GT.tr("Failed to convert binary xml data to encoding: {0}.", this.conn.getEncoding().name()), PSQLState.DATA_ERROR, ioe);
            }
            finally {
                this.byteArrayOutputStream = null;
                this.active = false;
            }
        }
        else if (this.stringWriter != null) {
            this.data = this.stringWriter.toString();
            this.stringWriter = null;
            this.active = false;
        }
        else if (this.domResult != null) {
            try {
                final TransformerFactory factory = TransformerFactory.newInstance();
                final Transformer transformer = factory.newTransformer();
                final DOMSource domSource = new DOMSource(this.domResult.getNode());
                final StringWriter stringWriter = new StringWriter();
                final StreamResult streamResult = new StreamResult(stringWriter);
                transformer.transform(domSource, streamResult);
                this.data = stringWriter.toString();
            }
            catch (TransformerException te) {
                throw new PSQLException(GT.tr("Unable to convert DOMResult SQLXML data to a string.", new Object[0]), PSQLState.DATA_ERROR, te);
            }
            finally {
                this.domResult = null;
                this.active = false;
            }
        }
    }
    
    private void initialize() throws SQLException {
        if (this.initialized) {
            throw new PSQLException(GT.tr("This SQLXML object has already been initialized, so you cannot manipulate it further.", new Object[0]), PSQLState.OBJECT_NOT_IN_STATE);
        }
        this.initialized = true;
    }
    
    static class NonPrintingErrorHandler implements ErrorHandler
    {
        @Override
        public void error(final SAXParseException e) {
        }
        
        @Override
        public void fatalError(final SAXParseException e) {
        }
        
        @Override
        public void warning(final SAXParseException e) {
        }
    }
}
