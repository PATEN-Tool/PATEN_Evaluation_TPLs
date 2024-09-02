// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.activemq.filter;

import org.apache.activemq.command.Message;
import org.slf4j.LoggerFactory;
import javax.jms.JMSException;
import java.io.IOException;
import org.apache.activemq.util.JMSExceptionSupport;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Map;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.lang.reflect.Constructor;
import org.slf4j.Logger;

public final class XPathExpression implements BooleanExpression
{
    private static final Logger LOG;
    private static final String EVALUATOR_SYSTEM_PROPERTY = "org.apache.activemq.XPathEvaluatorClassName";
    private static final String DEFAULT_EVALUATOR_CLASS_NAME = "org.apache.activemq.filter.XalanXPathEvaluator";
    public static final String DOCUMENT_BUILDER_FACTORY_FEATURE = "org.apache.activemq.documentBuilderFactory.feature";
    private static final Constructor EVALUATOR_CONSTRUCTOR;
    private static DocumentBuilder builder;
    private final String xpath;
    private final XPathEvaluator evaluator;
    
    XPathExpression(final String xpath) {
        this.xpath = xpath;
        this.evaluator = this.createEvaluator(xpath);
    }
    
    private static Constructor getXPathEvaluatorConstructor(final String cn) throws ClassNotFoundException, SecurityException, NoSuchMethodException {
        final Class c = XPathExpression.class.getClassLoader().loadClass(cn);
        if (!XPathEvaluator.class.isAssignableFrom(c)) {
            throw new ClassCastException("" + c + " is not an instance of " + XPathEvaluator.class);
        }
        return c.getConstructor(String.class, DocumentBuilder.class);
    }
    
    protected static void setupFeatures(final DocumentBuilderFactory factory) {
        final Properties properties = System.getProperties();
        final List<String> features = new ArrayList<String>();
        for (final Map.Entry<Object, Object> prop : properties.entrySet()) {
            final String key = prop.getKey();
            if (key.startsWith("org.apache.activemq.documentBuilderFactory.feature")) {
                final String uri = key.split("org.apache.activemq.documentBuilderFactory.feature:")[1];
                final Boolean value = Boolean.valueOf(prop.getValue());
                try {
                    factory.setFeature(uri, value);
                    features.add("feature " + uri + " value " + value);
                }
                catch (ParserConfigurationException e) {
                    XPathExpression.LOG.warn("DocumentBuilderFactory doesn't support the feature {} with value {}, due to {}.", new Object[] { uri, value, e });
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
    
    private XPathEvaluator createEvaluator(final String xpath2) {
        try {
            return XPathExpression.EVALUATOR_CONSTRUCTOR.newInstance(this.xpath, XPathExpression.builder);
        }
        catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException)cause;
            }
            throw new RuntimeException("Invalid XPath Expression: " + this.xpath + " reason: " + e.getMessage(), e);
        }
        catch (Throwable e2) {
            throw new RuntimeException("Invalid XPath Expression: " + this.xpath + " reason: " + e2.getMessage(), e2);
        }
    }
    
    @Override
    public Object evaluate(final MessageEvaluationContext message) throws JMSException {
        try {
            if (message.isDropped()) {
                return null;
            }
            return this.evaluator.evaluate(message.getMessage()) ? Boolean.TRUE : Boolean.FALSE;
        }
        catch (IOException e) {
            throw JMSExceptionSupport.create(e);
        }
    }
    
    @Override
    public String toString() {
        return "XPATH " + ConstantExpression.encodeString(this.xpath);
    }
    
    @Override
    public boolean matches(final MessageEvaluationContext message) throws JMSException {
        final Object object = this.evaluate(message);
        return object != null && object == Boolean.TRUE;
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)XPathExpression.class);
        XPathExpression.builder = null;
        String cn = System.getProperty("org.apache.activemq.XPathEvaluatorClassName", "org.apache.activemq.filter.XalanXPathEvaluator");
        Constructor m = null;
        try {
            m = getXPathEvaluatorConstructor(cn);
            final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            builderFactory.setIgnoringElementContentWhitespace(true);
            builderFactory.setIgnoringComments(true);
            try {
                builderFactory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", Boolean.TRUE);
                builderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                builderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                builderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            }
            catch (ParserConfigurationException e) {
                XPathExpression.LOG.warn("Error setting document builder factory feature", (Throwable)e);
            }
            setupFeatures(builderFactory);
            XPathExpression.builder = builderFactory.newDocumentBuilder();
        }
        catch (Throwable e2) {
            XPathExpression.LOG.warn("Invalid " + XPathEvaluator.class.getName() + " implementation: " + cn + ", reason: " + e2, e2);
            cn = "org.apache.activemq.filter.XalanXPathEvaluator";
            try {
                m = getXPathEvaluatorConstructor(cn);
            }
            catch (Throwable e3) {
                XPathExpression.LOG.error("Default XPath evaluator could not be loaded", e2);
            }
        }
        finally {
            EVALUATOR_CONSTRUCTOR = m;
        }
    }
    
    public interface XPathEvaluator
    {
        boolean evaluate(final Message p0) throws JMSException;
    }
}
