// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.uima.util;

import org.xml.sax.SAXParseException;
import org.apache.uima.cas.impl.OutOfTypeSystemData;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.XMLReader;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.xml.sax.InputSource;
import org.xml.sax.ContentHandler;
import org.apache.uima.internal.util.XMLUtils;
import org.apache.uima.cas.SerialFormat;
import java.io.IOException;
import org.xml.sax.SAXException;
import org.apache.uima.cas.CAS;
import java.io.InputStream;

public abstract class XmlCasDeserializer
{
    public static void deserialize(final InputStream aStream, final CAS aCAS) throws SAXException, IOException {
        deserialize(aStream, aCAS, false);
    }
    
    public static void deserialize(final InputStream aStream, final CAS aCAS, final boolean aLenient) throws SAXException, IOException {
        deserializeR(aStream, aCAS, aLenient);
    }
    
    static SerialFormat deserializeR(final InputStream aStream, final CAS aCAS, final boolean aLenient) throws SAXException, IOException {
        final XMLReader xmlReader = XMLUtils.createXMLReader();
        final XmlCasDeserializerHandler handler = new XmlCasDeserializerHandler(aCAS, aLenient);
        xmlReader.setContentHandler(handler);
        xmlReader.parse(new InputSource(aStream));
        return (handler.mDelegateHandler instanceof XmiCasDeserializer.XmiCasDeserializerHandler) ? SerialFormat.XMI : SerialFormat.XCAS;
    }
    
    static class XmlCasDeserializerHandler extends DefaultHandler
    {
        private CAS mCAS;
        private boolean mLenient;
        private ContentHandler mDelegateHandler;
        
        XmlCasDeserializerHandler(final CAS cas, final boolean lenient) {
            this.mCAS = cas;
            this.mLenient = lenient;
        }
        
        @Override
        public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
            if (this.mDelegateHandler == null) {
                final String xmiVer = attributes.getValue("xmi:version");
                if (xmiVer != null && xmiVer.length() > 0) {
                    final XmiCasDeserializer deser = new XmiCasDeserializer(this.mCAS.getTypeSystem());
                    this.mDelegateHandler = deser.getXmiCasHandler(this.mCAS, this.mLenient);
                }
                else if ("CAS".equals(localName)) {
                    final XCASDeserializer deser2 = new XCASDeserializer(this.mCAS.getTypeSystem());
                    this.mDelegateHandler = deser2.getXCASHandler(this.mCAS, this.mLenient ? new OutOfTypeSystemData() : null);
                }
                else {
                    final XmiCasDeserializer deser = new XmiCasDeserializer(this.mCAS.getTypeSystem());
                    this.mDelegateHandler = deser.getXmiCasHandler(this.mCAS, this.mLenient);
                }
                this.mDelegateHandler.startDocument();
            }
            this.mDelegateHandler.startElement(uri, localName, qName, attributes);
        }
        
        @Override
        public void characters(final char[] ch, final int start, final int length) throws SAXException {
            this.mDelegateHandler.characters(ch, start, length);
        }
        
        @Override
        public void endDocument() throws SAXException {
            this.mDelegateHandler.endDocument();
        }
        
        @Override
        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
            this.mDelegateHandler.endElement(uri, localName, qName);
        }
        
        @Override
        public void error(final SAXParseException e) throws SAXException {
            throw e;
        }
        
        @Override
        public void fatalError(final SAXParseException e) throws SAXException {
            throw e;
        }
        
        @Override
        public void warning(final SAXParseException e) throws SAXException {
            throw e;
        }
    }
}
