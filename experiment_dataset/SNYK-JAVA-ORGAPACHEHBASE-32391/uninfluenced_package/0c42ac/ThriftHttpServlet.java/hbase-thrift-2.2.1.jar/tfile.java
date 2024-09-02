// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.hadoop.hbase.thrift;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import java.util.Base64;
import org.ietf.jgss.Oid;
import org.apache.hadoop.hbase.security.SecurityUtil;
import org.ietf.jgss.GSSManager;
import org.slf4j.LoggerFactory;
import java.security.PrivilegedExceptionAction;
import org.apache.hadoop.security.authorize.AuthorizationException;
import org.apache.hadoop.security.authorize.ProxyUsers;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.TProcessor;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.apache.yetus.audience.InterfaceAudience;
import org.apache.thrift.server.TServlet;

@InterfaceAudience.Private
public class ThriftHttpServlet extends TServlet
{
    private static final long serialVersionUID = 1L;
    private static final Logger LOG;
    private final transient UserGroupInformation serviceUGI;
    private final transient UserGroupInformation httpUGI;
    private final transient HBaseServiceHandler handler;
    private final boolean doAsEnabled;
    private final boolean securityEnabled;
    public static final String NEGOTIATE = "Negotiate";
    
    public ThriftHttpServlet(final TProcessor processor, final TProtocolFactory protocolFactory, final UserGroupInformation serviceUGI, final Configuration conf, final HBaseServiceHandler handler, final boolean securityEnabled, final boolean doAsEnabled) throws IOException {
        super(processor, protocolFactory);
        this.serviceUGI = serviceUGI;
        this.handler = handler;
        this.securityEnabled = securityEnabled;
        this.doAsEnabled = doAsEnabled;
        if (securityEnabled) {
            UserGroupInformation.setConfiguration(conf);
            this.httpUGI = UserGroupInformation.loginUserFromKeytabAndReturnUGI(conf.get("hbase.thrift.spnego.principal"), conf.get("hbase.thrift.spnego.keytab.file"));
        }
        else {
            this.httpUGI = null;
        }
    }
    
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        String effectiveUser = request.getRemoteUser();
        if (this.securityEnabled) {
            final String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isEmpty()) {
                response.addHeader("WWW-Authenticate", "Negotiate");
                response.sendError(401);
                return;
            }
            try {
                final RemoteUserIdentity identity = this.doKerberosAuth(request);
                effectiveUser = identity.principal;
                response.addHeader("WWW-Authenticate", "Negotiate " + identity.outToken);
            }
            catch (HttpAuthenticationException e) {
                ThriftHttpServlet.LOG.error("Kerberos Authentication failed", (Throwable)e);
                response.addHeader("WWW-Authenticate", "Negotiate");
                response.sendError(401, "Authentication Error: " + e.getMessage());
                return;
            }
        }
        if (effectiveUser == null) {
            effectiveUser = this.serviceUGI.getShortUserName();
        }
        final String doAsUserFromQuery = request.getHeader("doAs");
        if (doAsUserFromQuery != null) {
            if (!this.doAsEnabled) {
                throw new ServletException("Support for proxyuser is not configured");
            }
            final UserGroupInformation remoteUser = UserGroupInformation.createRemoteUser(effectiveUser);
            final UserGroupInformation ugi = UserGroupInformation.createProxyUser(doAsUserFromQuery, remoteUser);
            try {
                ProxyUsers.authorize(ugi, request.getRemoteAddr());
            }
            catch (AuthorizationException e2) {
                throw new ServletException((Throwable)e2);
            }
            effectiveUser = doAsUserFromQuery;
        }
        this.handler.setEffectiveUser(effectiveUser);
        super.doPost(request, response);
    }
    
    private RemoteUserIdentity doKerberosAuth(final HttpServletRequest request) throws HttpAuthenticationException {
        final HttpKerberosServerAction action = new HttpKerberosServerAction(request, this.httpUGI);
        try {
            final String principal = (String)this.httpUGI.doAs((PrivilegedExceptionAction)action);
            return new RemoteUserIdentity(principal, action.outToken);
        }
        catch (Exception e) {
            ThriftHttpServlet.LOG.info("Failed to authenticate with {} kerberos principal", (Object)this.httpUGI.getUserName());
            throw new HttpAuthenticationException(e);
        }
    }
    
    static {
        LOG = LoggerFactory.getLogger(ThriftHttpServlet.class.getName());
    }
    
    private static class RemoteUserIdentity
    {
        final String outToken;
        final String principal;
        
        RemoteUserIdentity(final String principal, final String outToken) {
            this.principal = principal;
            this.outToken = outToken;
        }
    }
    
    private static class HttpKerberosServerAction implements PrivilegedExceptionAction<String>
    {
        final HttpServletRequest request;
        final UserGroupInformation httpUGI;
        String outToken;
        
        HttpKerberosServerAction(final HttpServletRequest request, final UserGroupInformation httpUGI) {
            this.outToken = null;
            this.request = request;
            this.httpUGI = httpUGI;
        }
        
        @Override
        public String run() throws HttpAuthenticationException {
            final GSSManager manager = GSSManager.getInstance();
            GSSContext gssContext = null;
            final String serverPrincipal = SecurityUtil.getPrincipalWithoutRealm(this.httpUGI.getUserName());
            try {
                final Oid kerberosMechOid = new Oid("1.2.840.113554.1.2.2");
                final Oid spnegoMechOid = new Oid("1.3.6.1.5.5.2");
                final Oid krb5PrincipalOid = new Oid("1.2.840.113554.1.2.2.1");
                final GSSName serverName = manager.createName(serverPrincipal, krb5PrincipalOid);
                final GSSCredential serverCreds = manager.createCredential(serverName, 0, new Oid[] { kerberosMechOid, spnegoMechOid }, 2);
                gssContext = manager.createContext(serverCreds);
                final String serviceTicketBase64 = this.getAuthHeader(this.request);
                final byte[] inToken = Base64.getDecoder().decode(serviceTicketBase64);
                final byte[] res = gssContext.acceptSecContext(inToken, 0, inToken.length);
                if (res != null) {
                    this.outToken = Base64.getEncoder().encodeToString(res).replace("\n", "");
                }
                if (!gssContext.isEstablished()) {
                    throw new HttpAuthenticationException("Kerberos authentication failed: unable to establish context with the service ticket provided by the client.");
                }
                return SecurityUtil.getUserFromPrincipal(gssContext.getSrcName().toString());
            }
            catch (GSSException e) {
                throw new HttpAuthenticationException("Kerberos authentication failed: ", e);
            }
            finally {
                if (gssContext != null) {
                    try {
                        gssContext.dispose();
                    }
                    catch (GSSException e2) {
                        ThriftHttpServlet.LOG.warn("Error while disposing GSS Context", (Throwable)e2);
                    }
                }
            }
        }
        
        private String getAuthHeader(final HttpServletRequest request) throws HttpAuthenticationException {
            final String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isEmpty()) {
                throw new HttpAuthenticationException("Authorization header received from the client is empty.");
            }
            final int beginIndex = "Negotiate ".length();
            final String authHeaderBase64String = authHeader.substring(beginIndex);
            if (authHeaderBase64String.isEmpty()) {
                throw new HttpAuthenticationException("Authorization header received from the client does not contain any data.");
            }
            return authHeaderBase64String;
        }
    }
}
