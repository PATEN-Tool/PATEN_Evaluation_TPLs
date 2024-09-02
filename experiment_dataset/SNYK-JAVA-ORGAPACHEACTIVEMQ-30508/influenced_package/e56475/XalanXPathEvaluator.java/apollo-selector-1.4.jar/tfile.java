// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.activemq.apollo.filter;

import org.w3c.dom.traversal.NodeIterator;
import org.apache.xpath.objects.XObject;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Node;
import org.apache.xpath.CachedXPathAPI;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.Reader;
import org.xml.sax.InputSource;
import java.io.StringReader;

public class XalanXPathEvaluator implements XPathExpression.XPathEvaluator
{
    private final String xpath;
    
    public XalanXPathEvaluator(final String xpath) {
        this.xpath = xpath;
    }
    
    public boolean evaluate(final Filterable m) throws FilterException {
        final String stringBody = m.getBodyAs(String.class);
        return stringBody != null && this.evaluate(stringBody);
    }
    
    protected boolean evaluate(final String text) {
        return this.evaluate(new InputSource(new StringReader(text)));
    }
    
    protected boolean evaluate(final InputSource inputSource) {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            final DocumentBuilder dbuilder = factory.newDocumentBuilder();
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
}
