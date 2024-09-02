// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.activemq.filter;

import org.apache.activemq.command.Message;
import org.apache.commons.logging.LogFactory;
import javax.jms.JMSException;
import java.io.IOException;
import org.apache.activemq.util.JMSExceptionSupport;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;
import org.apache.commons.logging.Log;

public final class XPathExpression implements BooleanExpression
{
    private static final Log log;
    private static final String EVALUATOR_SYSTEM_PROPERTY = "org.apache.activemq.XPathEvaluatorClassName";
    private static final String DEFAULT_EVALUATOR_CLASS_NAME;
    private static final Constructor EVALUATOR_CONSTRUCTOR;
    private final String xpath;
    private final XPathEvaluator evaluator;
    
    private static Constructor getXPathEvaluatorConstructor(final String cn) throws ClassNotFoundException, SecurityException, NoSuchMethodException {
        final Class c = XPathExpression.class.getClassLoader().loadClass(cn);
        if (!XPathEvaluator.class.isAssignableFrom(c)) {
            throw new ClassCastException("" + c + " is not an instance of " + XPathEvaluator.class);
        }
        return c.getConstructor(String.class);
    }
    
    XPathExpression(final String xpath) {
        this.xpath = xpath;
        this.evaluator = this.createEvaluator(xpath);
    }
    
    private XPathEvaluator createEvaluator(final String xpath2) {
        try {
            return XPathExpression.EVALUATOR_CONSTRUCTOR.newInstance(this.xpath);
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
    
    public String toString() {
        return "XPATH " + ConstantExpression.encodeString(this.xpath);
    }
    
    public boolean matches(final MessageEvaluationContext message) throws JMSException {
        final Object object = this.evaluate(message);
        return object != null && object == Boolean.TRUE;
    }
    
    static {
        log = LogFactory.getLog(XPathExpression.class);
        DEFAULT_EVALUATOR_CLASS_NAME = XalanXPathEvaluator.class.getName();
        String cn = System.getProperty("org.apache.activemq.XPathEvaluatorClassName", XPathExpression.DEFAULT_EVALUATOR_CLASS_NAME);
        Constructor m = null;
        try {
            m = getXPathEvaluatorConstructor(cn);
        }
        catch (Throwable e) {
            XPathExpression.log.warn("Invalid " + XPathEvaluator.class.getName() + " implementation: " + cn + ", reason: " + e, e);
            cn = XPathExpression.DEFAULT_EVALUATOR_CLASS_NAME;
            try {
                m = getXPathEvaluatorConstructor(cn);
            }
            catch (Throwable e2) {
                XPathExpression.log.error("Default XPath evaluator could not be loaded", e);
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
