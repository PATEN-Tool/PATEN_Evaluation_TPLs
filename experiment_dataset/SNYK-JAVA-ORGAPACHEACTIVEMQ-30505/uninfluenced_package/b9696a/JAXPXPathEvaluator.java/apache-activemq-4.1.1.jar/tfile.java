// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.activemq.filter;

import java.io.Reader;
import java.io.StringReader;
import javax.xml.xpath.XPathConstants;
import java.io.InputStream;
import org.xml.sax.InputSource;
import org.apache.activemq.util.ByteArrayInputStream;
import javax.jms.JMSException;
import javax.jms.BytesMessage;
import javax.jms.TextMessage;
import org.apache.activemq.command.Message;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class JAXPXPathEvaluator implements XPathExpression.XPathEvaluator
{
    private static final XPathFactory factory;
    private javax.xml.xpath.XPathExpression expression;
    
    public JAXPXPathEvaluator(final String xpathExpression) {
        try {
            final XPath xpath = JAXPXPathEvaluator.factory.newXPath();
            this.expression = xpath.compile(xpathExpression);
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException("Invalid XPath expression: " + xpathExpression);
        }
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
            final InputSource inputSource = new InputSource(new ByteArrayInputStream(data));
            return (boolean)this.expression.evaluate(inputSource, XPathConstants.BOOLEAN);
        }
        catch (XPathExpressionException e) {
            return false;
        }
    }
    
    private boolean evaluate(final String text) {
        try {
            final InputSource inputSource = new InputSource(new StringReader(text));
            return (boolean)this.expression.evaluate(inputSource, XPathConstants.BOOLEAN);
        }
        catch (XPathExpressionException e) {
            return false;
        }
    }
    
    static {
        factory = XPathFactory.newInstance();
    }
}
