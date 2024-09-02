// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.uima.tools.stylemap;

import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import org.xml.sax.SAXException;
import java.io.InputStream;
import org.xml.sax.InputSource;
import java.io.ByteArrayInputStream;
import org.xml.sax.ContentHandler;
import org.apache.uima.internal.util.XMLUtils;
import java.util.Vector;
import org.xml.sax.helpers.DefaultHandler;

public class StyleMapXmlParser extends DefaultHandler
{
    private static final String FEATURE_VALUE_PREFIX = "[@";
    public Vector annotType;
    public Vector styleLabel;
    public Vector styleColor;
    public Vector featureValue;
    private StringBuffer data;
    
    public StyleMapXmlParser(final String xmlFile) {
        this.annotType = new Vector();
        this.styleLabel = new Vector();
        this.styleColor = new Vector();
        this.featureValue = new Vector();
        this.data = new StringBuffer();
        try {
            final SAXParserFactory saxParserFactory = XMLUtils.createSAXParserFactory();
            final SAXParser parser = saxParserFactory.newSAXParser();
            final XMLReader reader = parser.getXMLReader();
            reader.setContentHandler(this);
            final InputSource inputSource = new InputSource(new ByteArrayInputStream(xmlFile.getBytes()));
            reader.parse(inputSource);
        }
        catch (SAXException e) {
            e.printStackTrace();
        }
        catch (IOException e2) {
            e2.printStackTrace();
        }
        catch (ParserConfigurationException e3) {
            e3.printStackTrace();
        }
        catch (FactoryConfigurationError e4) {
            e4.printStackTrace();
        }
    }
    
    @Override
    public void startElement(final String namespaceURI, final String localName, final String qName, final Attributes atts) throws SAXException {
    }
    
    @Override
    public void endElement(final String namespaceURI, final String localName, final String qName) throws SAXException {
        if ("pattern".equals(localName) || "pattern".equals(qName)) {
            final String patternString = this.data.toString().trim();
            final int featureValueIndex = patternString.indexOf("[@");
            if (featureValueIndex == -1) {
                this.annotType.add(patternString);
                this.featureValue.add("");
            }
            else {
                final String annotationType = patternString.substring(0, featureValueIndex);
                final int equalsSignIndex = patternString.indexOf(61);
                final String featureName = patternString.substring(featureValueIndex + 2, equalsSignIndex);
                this.annotType.add(annotationType + ":" + featureName);
                final int firstQuoteIndex = patternString.indexOf("'");
                final int lastQuoteIndex = patternString.lastIndexOf("'");
                final String fValue = patternString.substring(firstQuoteIndex + 1, lastQuoteIndex);
                this.featureValue.add(fValue);
            }
        }
        else if ("label".equals(localName) || "label".equals(qName)) {
            this.styleLabel.add(this.data.toString().trim());
        }
        else if ("style".equals(localName) || "style".equals(qName)) {
            this.styleColor.add(this.data.toString().trim());
        }
        this.data.delete(0, this.data.length());
    }
    
    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        this.data.append(ch, start, length);
    }
}
