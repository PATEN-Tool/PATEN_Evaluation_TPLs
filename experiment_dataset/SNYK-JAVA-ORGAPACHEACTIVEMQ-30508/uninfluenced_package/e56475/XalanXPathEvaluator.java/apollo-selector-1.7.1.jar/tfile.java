// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.activemq.apollo.filter;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Map;
import java.util.ArrayList;
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
    public static final String DOCUMENT_BUILDER_FACTORY_FEATURE = "org.apache.activemq.apollo.documentBuilderFactory.feature";
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
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            this.setupFeatures(factory);
            factory.setNamespaceAware(true);
            factory.setIgnoringElementContentWhitespace(true);
            factory.setIgnoringComments(true);
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
    
    protected void setupFeatures(final DocumentBuilderFactory factory) {
        final Properties properties = System.getProperties();
        final List<String> features = new ArrayList<String>();
        for (final Map.Entry<Object, Object> prop : properties.entrySet()) {
            final String key = prop.getKey();
            if (key.startsWith("org.apache.activemq.apollo.documentBuilderFactory.feature")) {
                final String uri = key.split("org.apache.activemq.apollo.documentBuilderFactory.feature:")[1];
                final Boolean value = Boolean.valueOf(prop.getValue());
                try {
                    factory.setFeature(uri, value);
                    features.add("feature " + uri + " value " + value);
                }
                catch (ParserConfigurationException e) {
                    throw new RuntimeException("DocumentBuilderFactory doesn't support the feature " + uri + " with value " + value + ", due to " + e);
                }
            }
        }
        if (features.size() > 0) {
            final StringBuffer featureString = new StringBuffer();
            for (final String feature : features) {
                if (featureString.length() != 0) {
                    featureString.append(", ");
                }
                featureString.append(feature);
            }
        }
    }
}
