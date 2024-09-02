// 
// Decompiled by Procyon v0.5.36
// 

package org.jolokia.restrictor;

import javax.management.ObjectName;
import org.jolokia.util.RequestType;
import org.jolokia.util.HttpMethod;
import org.w3c.dom.Document;
import javax.management.MalformedObjectNameException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import org.jolokia.restrictor.policy.MBeanAccessChecker;
import org.jolokia.restrictor.policy.NetworkChecker;
import org.jolokia.restrictor.policy.RequestTypeChecker;
import org.jolokia.restrictor.policy.HttpMethodChecker;

public class PolicyRestrictor implements Restrictor
{
    private HttpMethodChecker httpChecker;
    private RequestTypeChecker requestTypeChecker;
    private NetworkChecker networkChecker;
    private MBeanAccessChecker mbeanAccessChecker;
    
    public PolicyRestrictor(final InputStream pInput) {
        Exception exp = null;
        if (pInput == null) {
            throw new SecurityException("No policy file given");
        }
        try {
            final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pInput);
            this.requestTypeChecker = new RequestTypeChecker(doc);
            this.httpChecker = new HttpMethodChecker(doc);
            this.networkChecker = new NetworkChecker(doc);
            this.mbeanAccessChecker = new MBeanAccessChecker(doc);
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
    
    public boolean isHttpMethodAllowed(final HttpMethod method) {
        return this.httpChecker.check(method);
    }
    
    public boolean isTypeAllowed(final RequestType pType) {
        return this.requestTypeChecker.check(pType);
    }
    
    public boolean isRemoteAccessAllowed(final String... pHostOrAddress) {
        return this.networkChecker.check(pHostOrAddress);
    }
    
    public boolean isAttributeReadAllowed(final ObjectName pName, final String pAttribute) {
        return this.check(RequestType.READ, pName, pAttribute);
    }
    
    public boolean isAttributeWriteAllowed(final ObjectName pName, final String pAttribute) {
        return this.check(RequestType.WRITE, pName, pAttribute);
    }
    
    public boolean isOperationAllowed(final ObjectName pName, final String pOperation) {
        return this.check(RequestType.EXEC, pName, pOperation);
    }
    
    private boolean check(final RequestType pType, final ObjectName pName, final String pValue) {
        return this.mbeanAccessChecker.check(new MBeanAccessChecker.Arg(this.isTypeAllowed(pType), pType, pName, pValue));
    }
}
