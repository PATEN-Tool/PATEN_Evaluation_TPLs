// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.activemq.artemis.selector.filter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.traversal.NodeIterator;
import org.apache.xpath.objects.XObject;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Node;
import org.apache.xpath.CachedXPathAPI;
import java.io.Reader;
import org.xml.sax.InputSource;
import java.io.StringReader;

public class XalanXPathEvaluator implements XPathExpression.XPathEvaluator
{
    private final String xpath;
    
    public XalanXPathEvaluator(final String xpath) {
        this.xpath = xpath;
    }
    
    @Override
    public boolean evaluate(final Filterable m) throws FilterException {
        final String stringBody = m.getBodyAs(String.class);
        return stringBody != null && this.evaluate(stringBody);
    }
    
    protected boolean evaluate(final String text) {
        return this.evaluate(new InputSource(new StringReader(text)));
    }
    
    protected boolean evaluate(final InputSource inputSource) {
        try {
            final DocumentBuilder dbuilder = this.createDocumentBuilder();
            final Document doc = dbuilder.parse(inputSource);
            final CachedXPathAPI cachedXPathAPI = new CachedXPathAPI();
            final XObject result = cachedXPathAPI.eval((Node)doc, this.xpath);
            if (result.bool()) {
                return true;
            }
            final NodeIterator iterator = cachedXPathAPI.selectNodeIterator((Node)doc, this.xpath);
            return iterator.nextNode() != null;
        }
        catch (Throwable e) {
            return false;
        }
    }
    
    private DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder();
    }
}
