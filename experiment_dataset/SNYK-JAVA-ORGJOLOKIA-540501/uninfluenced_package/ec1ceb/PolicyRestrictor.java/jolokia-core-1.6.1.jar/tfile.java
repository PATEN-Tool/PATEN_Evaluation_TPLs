// 
// Decompiled by Procyon v0.5.36
// 

package org.jolokia.restrictor;

import javax.management.ObjectName;
import org.jolokia.util.RequestType;
import org.jolokia.util.HttpMethod;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import javax.management.MalformedObjectNameException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import org.xml.sax.SAXException;
import java.io.InputStream;
import org.jolokia.restrictor.policy.MBeanAccessChecker;
import org.jolokia.restrictor.policy.CorsChecker;
import org.jolokia.restrictor.policy.NetworkChecker;
import org.jolokia.restrictor.policy.RequestTypeChecker;
import org.jolokia.restrictor.policy.HttpMethodChecker;

public class PolicyRestrictor implements Restrictor
{
    private HttpMethodChecker httpChecker;
    private RequestTypeChecker requestTypeChecker;
    private NetworkChecker networkChecker;
    private CorsChecker corsChecker;
    private MBeanAccessChecker mbeanAccessChecker;
    
    public PolicyRestrictor(final InputStream pInput) {
        Exception exp = null;
        if (pInput == null) {
            throw new SecurityException("No policy file given");
        }
        try {
            final Document doc = this.createDocument(pInput);
            this.requestTypeChecker = new RequestTypeChecker(doc);
            this.httpChecker = new HttpMethodChecker(doc);
            this.networkChecker = new NetworkChecker(doc);
            this.mbeanAccessChecker = new MBeanAccessChecker(doc);
            this.corsChecker = new CorsChecker(doc);
        }
        catch (SAXException e) {
            exp = e;
        }
        catch (IOException e2) {
            exp = e2;
        }
        catch (ParserConfigurationException e3) {
            exp = e3;
        }
        catch (MalformedObjectNameException e4) {
            exp = e4;
        }
        if (exp != null) {
            throw new SecurityException("Cannot parse policy file: " + exp, exp);
        }
    }
    
    private Document createDocument(final InputStream pInput) throws ParserConfigurationException, SAXException, IOException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final String[] array;
        final String[] features = array = new String[] { "http://xml.org/sax/features/external-general-entities", "http://xml.org/sax/features/external-parameter-entities", "http://apache.org/xml/features/nonvalidating/load-external-dtd" };
        for (final String feature : array) {
            try {
                factory.setFeature(feature, false);
            }
            catch (ParserConfigurationException ex) {}
        }
        return factory.newDocumentBuilder().parse(pInput);
    }
    
    @Override
    public boolean isHttpMethodAllowed(final HttpMethod method) {
        return this.httpChecker.check(method);
    }
    
    @Override
    public boolean isTypeAllowed(final RequestType pType) {
        return this.requestTypeChecker.check(pType);
    }
    
    @Override
    public boolean isRemoteAccessAllowed(final String... pHostOrAddress) {
        return this.networkChecker.check(pHostOrAddress);
    }
    
    @Override
    public boolean isOriginAllowed(final String pOrigin, final boolean pOnlyWhenStrictCheckingIsEnabled) {
        return this.corsChecker.check(pOrigin, pOnlyWhenStrictCheckingIsEnabled);
    }
    
    @Override
    public boolean isAttributeReadAllowed(final ObjectName pName, final String pAttribute) {
        return this.check(RequestType.READ, pName, pAttribute);
    }
    
    @Override
    public boolean isAttributeWriteAllowed(final ObjectName pName, final String pAttribute) {
        return this.check(RequestType.WRITE, pName, pAttribute);
    }
    
    @Override
    public boolean isOperationAllowed(final ObjectName pName, final String pOperation) {
        return this.check(RequestType.EXEC, pName, pOperation);
    }
    
    private boolean check(final RequestType pType, final ObjectName pName, final String pValue) {
        return this.mbeanAccessChecker.check(new MBeanAccessChecker.Arg(this.isTypeAllowed(pType), pType, pName, pValue));
    }
}
