// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.activemq.filter;

import java.io.Reader;
import java.io.StringReader;
import org.w3c.dom.Document;
import javax.xml.xpath.XPathConstants;
import java.io.InputStream;
import org.xml.sax.InputSource;
import org.apache.activemq.util.ByteArrayInputStream;
import javax.jms.JMSException;
import javax.jms.BytesMessage;
import javax.jms.TextMessage;
import org.apache.activemq.command.Message;
import javax.xml.xpath.XPath;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPathFactory;

public class JAXPXPathEvaluator implements XPathExpression.XPathEvaluator
{
    private static final XPathFactory FACTORY;
    private final String xpathExpression;
    private final DocumentBuilder builder;
    private final XPath xpath;
    
    public JAXPXPathEvaluator(final String xpathExpression, final DocumentBuilder builder) throws Exception {
        this.xpath = JAXPXPathEvaluator.FACTORY.newXPath();
        this.xpathExpression = xpathExpression;
        if (builder != null) {
            this.builder = builder;
            return;
        }
        throw new RuntimeException("No document builder available");
    }
    
    public boolean evaluate(final Message message) throws JMSException {
        if (message instanceof TextMessage) {
            final String text = ((TextMessage)message).getText();
            return this.evaluate(text);
        }
        if (message instanceof BytesMessage) {
            final BytesMessage bm = (BytesMessage)message;
            final byte[] data = new byte[(int)bm.getBodyLength()];
            bm.readBytes(data);
            return this.evaluate(data);
        }
        return false;
    }
    
    private boolean evaluate(final byte[] data) {
        try {
            final InputSource inputSource = new InputSource((InputStream)new ByteArrayInputStream(data));
            final Document inputDocument = this.builder.parse(inputSource);
            return (boolean)this.xpath.evaluate(this.xpathExpression, inputDocument, XPathConstants.BOOLEAN);
        }
        catch (Exception e) {
            return false;
        }
    }
    
    private boolean evaluate(final String text) {
        try {
            final InputSource inputSource = new InputSource(new StringReader(text));
            final Document inputDocument = this.builder.parse(inputSource);
            return (boolean)this.xpath.evaluate(this.xpathExpression, inputDocument, XPathConstants.BOOLEAN);
        }
        catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String toString() {
        return this.xpathExpression;
    }
    
    static {
        FACTORY = XPathFactory.newInstance();
    }
}
