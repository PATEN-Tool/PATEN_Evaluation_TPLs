// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.hadoop.hbase.thrift;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.apache.hadoop.hbase.util.Base64;
import org.ietf.jgss.Oid;
import org.apache.hadoop.hbase.security.SecurityUtil;
import org.ietf.jgss.GSSManager;
import org.slf4j.LoggerFactory;
import java.security.PrivilegedExceptionAction;
import java.io.IOException;
import org.apache.hadoop.security.authorize.AuthorizationException;
import org.apache.hadoop.security.authorize.ProxyUsers;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.TProcessor;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.apache.yetus.audience.InterfaceAudience;
import org.apache.thrift.server.TServlet;

@InterfaceAudience.Private
public class ThriftHttpServlet extends TServlet
{
    private static final long serialVersionUID = 1L;
    private static final Logger LOG;
    private final transient UserGroupInformation realUser;
    private final transient Configuration conf;
    private final boolean securityEnabled;
    private final boolean doAsEnabled;
    private transient ThriftServerRunner.HBaseHandler hbaseHandler;
    public static final String WWW_AUTHENTICATE = "WWW-Authenticate";
    public static final String AUTHORIZATION = "Authorization";
    public static final String NEGOTIATE = "Negotiate";
    
    public ThriftHttpServlet(final TProcessor processor, final TProtocolFactory protocolFactory, final UserGroupInformation realUser, final Configuration conf, final ThriftServerRunner.HBaseHandler hbaseHandler, final boolean securityEnabled, final boolean doAsEnabled) {
        super(processor, protocolFactory);
        this.realUser = realUser;
        this.conf = conf;
        this.hbaseHandler = hbaseHandler;
        this.securityEnabled = securityEnabled;
        this.doAsEnabled = doAsEnabled;
    }
    
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        String effectiveUser = request.getRemoteUser();
        if (this.securityEnabled) {
            try {
                final RemoteUserIdentity identity = this.doKerberosAuth(request);
                effectiveUser = identity.principal;
                response.addHeader("WWW-Authenticate", "Negotiate " + identity.outToken);
            }
            catch (HttpAuthenticationException e) {
                ThriftHttpServlet.LOG.error("Kerberos Authentication failed", (Throwable)e);
                response.setStatus(401);
                response.addHeader("WWW-Authenticate", "Negotiate");
                response.getWriter().println("Authentication Error: " + e.getMessage());
                return;
            }
        }
        final String doAsUserFromQuery = request.getHeader("doAs");
        if (effectiveUser == null) {
            effectiveUser = this.realUser.getShortUserName();
        }
        if (doAsUserFromQuery != null) {
            if (!this.doAsEnabled) {
                throw new ServletException("Support for proxyuser is not configured");
            }
            final UserGroupInformation remoteUser = UserGroupInformation.createRemoteUser(effectiveUser);
            final UserGroupInformation ugi = UserGroupInformation.createProxyUser(doAsUserFromQuery, remoteUser);
            try {
                ProxyUsers.authorize(ugi, request.getRemoteAddr(), this.conf);
            }
            catch (AuthorizationException e2) {
                throw new ServletException(e2.getMessage());
            }
            effectiveUser = doAsUserFromQuery;
        }
        this.hbaseHandler.setEffectiveUser(effectiveUser);
        super.doPost(request, response);
    }
    
    private RemoteUserIdentity doKerberosAuth(final HttpServletRequest request) throws HttpAuthenticationException {
        final HttpKerberosServerAction action = new HttpKerberosServerAction(request, this.realUser);
        try {
            final String principal = (String)this.realUser.doAs((PrivilegedExceptionAction)action);
            return new RemoteUserIdentity(principal, action.outToken);
        }
        catch (Exception e) {
            ThriftHttpServlet.LOG.error("Failed to perform authentication");
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
        HttpServletRequest request;
        UserGroupInformation serviceUGI;
        String outToken;
        
        HttpKerberosServerAction(final HttpServletRequest request, final UserGroupInformation serviceUGI) {
            this.outToken = null;
            this.request = request;
            this.serviceUGI = serviceUGI;
        }
        
        @Override
        public String run() throws HttpAuthenticationException {
            final GSSManager manager = GSSManager.getInstance();
            GSSContext gssContext = null;
            final String serverPrincipal = SecurityUtil.getPrincipalWithoutRealm(this.serviceUGI.getUserName());
            try {
                final Oid kerberosMechOid = new Oid("1.2.840.113554.1.2.2");
                final Oid spnegoMechOid = new Oid("1.3.6.1.5.5.2");
                final Oid krb5PrincipalOid = new Oid("1.2.840.113554.1.2.2.1");
                final GSSName serverName = manager.createName(serverPrincipal, krb5PrincipalOid);
                final GSSCredential serverCreds = manager.createCredential(serverName, 0, new Oid[] { kerberosMechOid, spnegoMechOid }, 2);
                gssContext = manager.createContext(serverCreds);
                final String serviceTicketBase64 = this.getAuthHeader(this.request);
                final byte[] inToken = Base64.decode(serviceTicketBase64);
                final byte[] res = gssContext.acceptSecContext(inToken, 0, inToken.length);
                if (res != null) {
                    this.outToken = Base64.encodeBytes(res).replace("\n", "");
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
            if (authHeaderBase64String == null || authHeaderBase64String.isEmpty()) {
                throw new HttpAuthenticationException("Authorization header received from the client does not contain any data.");
            }
            return authHeaderBase64String;
        }
    }
}
