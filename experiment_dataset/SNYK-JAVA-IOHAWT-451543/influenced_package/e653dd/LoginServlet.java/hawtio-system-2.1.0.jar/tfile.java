// 
// Decompiled by Procyon v0.5.36
// 

package io.hawt.web.auth;

import org.slf4j.LoggerFactory;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Calendar;
import javax.servlet.http.HttpSession;
import io.hawt.system.AuthHelpers;
import javax.security.auth.Subject;
import io.hawt.system.AuthenticateResult;
import org.json.JSONObject;
import io.hawt.system.Authenticator;
import io.hawt.web.ServletHelpers;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import io.hawt.system.ConfigManager;
import org.jolokia.converter.json.JsonConvertOptions;
import org.jolokia.converter.Converters;
import org.slf4j.Logger;
import javax.servlet.http.HttpServlet;

public class LoginServlet extends HttpServlet
{
    private static final long serialVersionUID = 187076436862364207L;
    private static final transient Logger LOG;
    private static final int DEFAULT_SESSION_TIMEOUT = 1800;
    private Integer timeout;
    private AuthenticationConfiguration authConfiguration;
    private Converters converters;
    private JsonConvertOptions options;
    
    public LoginServlet() {
        this.timeout = 1800;
        this.converters = new Converters();
        this.options = JsonConvertOptions.DEFAULT;
    }
    
    public void init() {
        this.authConfiguration = AuthenticationConfiguration.getConfiguration(this.getServletContext());
        this.setupSessionTimeout();
        LoginServlet.LOG.info("hawtio login is using {} HttpSession timeout", (Object)((this.timeout != null) ? (this.timeout + " sec.") : "default"));
    }
    
    private void setupSessionTimeout() {
        final ConfigManager configManager = (ConfigManager)this.getServletContext().getAttribute("ConfigManager");
        if (configManager == null) {
            return;
        }
        final String timeoutStr = configManager.get("sessionTimeout", Integer.toString(1800));
        if (timeoutStr == null) {
            return;
        }
        try {
            this.timeout = Integer.valueOf(timeoutStr);
            if (this.timeout == 0) {
                this.timeout = 1800;
            }
        }
        catch (Exception e) {
            this.timeout = 1800;
        }
    }
    
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        if (this.authConfiguration.isKeycloakEnabled()) {
            String scheme = request.getServletContext().getInitParameter("scheme");
            if (null == scheme) {
                scheme = "http";
            }
            LoginServlet.LOG.debug("scheme = {}", (Object)scheme);
            final String redirectUrl = scheme + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/";
            response.sendRedirect(redirectUrl);
        }
        else {
            request.getRequestDispatcher("/login.html").forward((ServletRequest)request, (ServletResponse)response);
        }
    }
    
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        this.clearSession(request);
        final JSONObject json = ServletHelpers.readObject(request.getReader());
        final String username = (String)json.get("username");
        final String password = (String)json.get("password");
        final AuthenticateResult result = Authenticator.authenticate(this.authConfiguration, request, username, password, subject -> {
            this.setupSession(request, subject, username);
            this.sendResponse(response, subject);
            return;
        });
        switch (result) {
            case NOT_AUTHORIZED:
            case NO_CREDENTIALS: {
                ServletHelpers.doForbidden(response);
                break;
            }
        }
    }
    
    private void clearSession(final HttpServletRequest request) {
        final HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        final Subject subject = (Subject)session.getAttribute("subject");
        if (subject != null) {
            LoginServlet.LOG.info("Logging out existing user: {}", (Object)AuthHelpers.getUsername(subject));
            Authenticator.logout(this.authConfiguration, subject);
            session.invalidate();
        }
    }
    
    private void setupSession(final HttpServletRequest request, final Subject subject, final String username) {
        final HttpSession session = request.getSession(true);
        session.setAttribute("subject", (Object)subject);
        session.setAttribute("user", (Object)username);
        session.setAttribute("org.osgi.service.http.authentication.remote.user", (Object)username);
        session.setAttribute("org.osgi.service.http.authentication.type", (Object)"BASIC");
        session.setAttribute("loginTime", (Object)Calendar.getInstance().getTimeInMillis());
        if (this.timeout != null) {
            session.setMaxInactiveInterval((int)this.timeout);
        }
        LoginServlet.LOG.debug("Http session timeout for user {} is {} sec.", (Object)username, (Object)session.getMaxInactiveInterval());
    }
    
    private void sendResponse(final HttpServletResponse response, final Subject subject) {
        response.setContentType("application/json");
        try (final PrintWriter out = response.getWriter()) {
            final Map<String, Object> answer = new HashMap<String, Object>();
            final List<Object> principals = new ArrayList<Object>();
            for (final Principal principal : subject.getPrincipals()) {
                final Map<String, String> data = new HashMap<String, String>();
                data.put("type", principal.getClass().getName());
                data.put("name", principal.getName());
                principals.add(data);
            }
            final List<Object> credentials = new ArrayList<Object>();
            for (final Object credential : subject.getPublicCredentials()) {
                final Map<String, Object> data2 = new HashMap<String, Object>();
                data2.put("type", credential.getClass().getName());
                data2.put("credential", credential);
                credentials.add(data2);
            }
            answer.put("principals", principals);
            answer.put("credentials", credentials);
            ServletHelpers.writeObject(this.converters, this.options, out, answer);
        }
        catch (IOException e) {
            LoginServlet.LOG.error("Failed to send response", (Throwable)e);
        }
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)LoginServlet.class);
    }
}
