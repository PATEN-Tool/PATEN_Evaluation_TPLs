// 
// Decompiled by Procyon v0.5.36
// 

package io.hawt.web.auth.keycloak;

import org.slf4j.LoggerFactory;
import java.io.PrintWriter;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.net.URL;
import javax.servlet.ServletException;
import java.io.InputStream;
import java.io.Closeable;
import java.io.IOException;
import io.hawt.util.IOHelper;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import io.hawt.system.ConfigManager;
import io.hawt.web.auth.AuthenticationConfiguration;
import org.slf4j.Logger;
import javax.servlet.http.HttpServlet;

public class KeycloakServlet extends HttpServlet
{
    private static final long serialVersionUID = 3464713772839013741L;
    private static final transient Logger LOG;
    public static final String KEYCLOAK_CLIENT_CONFIG = "keycloakClientConfig";
    public static final String HAWTIO_KEYCLOAK_CLIENT_CONFIG = "hawtio.keycloakClientConfig";
    private String keycloakConfig;
    private AuthenticationConfiguration authConfiguration;
    private boolean keycloakEnabled;
    
    public KeycloakServlet() {
        this.keycloakConfig = null;
    }
    
    public void init() throws ServletException {
        final ConfigManager config = (ConfigManager)this.getServletContext().getAttribute("ConfigManager");
        this.authConfiguration = AuthenticationConfiguration.getConfiguration(this.getServletContext());
        this.keycloakEnabled = this.authConfiguration.isKeycloakEnabled();
        KeycloakServlet.LOG.info("Keycloak integration is {}", (Object)(this.keycloakEnabled ? "enabled" : "disabled"));
        if (!this.keycloakEnabled) {
            return;
        }
        String keycloakConfigFile = config.get("keycloakClientConfig", null);
        if (System.getProperty("hawtio.keycloakClientConfig") != null) {
            keycloakConfigFile = System.getProperty("hawtio.keycloakClientConfig");
        }
        if (keycloakConfigFile == null || keycloakConfigFile.length() == 0) {
            keycloakConfigFile = this.defaultKeycloakConfigLocation();
        }
        KeycloakServlet.LOG.info("Will load keycloak config from location: {}", (Object)keycloakConfigFile);
        final InputStream is = this.loadFile(keycloakConfigFile);
        if (is == null) {
            KeycloakServlet.LOG.warn("Keycloak client configuration not found!");
        }
        else {
            try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                final String keycloakConfig = IOHelper.readFully(reader);
                this.keycloakConfig = keycloakConfig.replaceAll(" ", "").replaceAll(System.lineSeparator(), "");
            }
            catch (IOException ioe) {
                KeycloakServlet.LOG.warn("Couldn't read keycloak configuration file", (Throwable)ioe);
            }
            finally {
                IOHelper.close((Closeable)is, "keycloakInputStream", KeycloakServlet.LOG);
            }
        }
    }
    
    protected String defaultKeycloakConfigLocation() {
        final String karafBase = System.getProperty("karaf.base");
        if (karafBase != null) {
            return karafBase + "/etc/keycloak.json";
        }
        final String jettyHome = System.getProperty("jetty.home");
        if (jettyHome != null) {
            return jettyHome + "/etc/keycloak.json";
        }
        final String tomcatHome = System.getProperty("catalina.home");
        if (tomcatHome != null) {
            return tomcatHome + "/conf/keycloak.json";
        }
        final String jbossHome = System.getProperty("jboss.server.config.dir");
        if (jbossHome != null) {
            return jbossHome + "/keycloak.json";
        }
        return "classpath:keycloak.json";
    }
    
    protected InputStream loadFile(String keycloakConfigFile) {
        if (keycloakConfigFile.startsWith("classpath:")) {
            final String classPathLocation = keycloakConfigFile.substring(10);
            return this.getClass().getClassLoader().getResourceAsStream(classPathLocation);
        }
        try {
            if (!keycloakConfigFile.contains(":")) {
                keycloakConfigFile = "file://" + keycloakConfigFile;
            }
            return new URL(keycloakConfigFile).openStream();
        }
        catch (Exception e) {
            KeycloakServlet.LOG.warn("Couldn't find keycloak config file on location: " + keycloakConfigFile);
            KeycloakServlet.LOG.debug("Couldn't find keycloak config file", (Throwable)e);
            return null;
        }
    }
    
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        final String pathInfo2;
        final String pathInfo = pathInfo2 = request.getPathInfo();
        switch (pathInfo2) {
            case "/enabled": {
                this.renderJSONResponse(response, String.valueOf(this.keycloakEnabled));
                break;
            }
            case "/client-config": {
                if (this.keycloakConfig == null) {
                    response.sendError(404, "Keycloak client configuration not found");
                    break;
                }
                this.renderJSONResponse(response, this.keycloakConfig);
                break;
            }
            case "/validate-subject-matches": {
                final String keycloakUser = request.getParameter("keycloakUser");
                if (keycloakUser == null || keycloakUser.length() == 0) {
                    KeycloakServlet.LOG.warn("Parameter 'keycloakUser' not found");
                }
                final boolean valid = this.validateKeycloakUser(request, keycloakUser);
                this.renderJSONResponse(response, String.valueOf(valid));
                break;
            }
        }
    }
    
    protected boolean validateKeycloakUser(final HttpServletRequest request, final String keycloakUser) {
        final HttpSession session = request.getSession(false);
        if (session == null) {
            return true;
        }
        final String username = (String)session.getAttribute("user");
        if (username != null && !username.equals(keycloakUser)) {
            KeycloakServlet.LOG.debug("No matching username found. JAAS username: {}, keycloakUsername: {}. Invalidating session", (Object)username, (Object)keycloakUser);
            session.invalidate();
            return false;
        }
        return true;
    }
    
    private void renderJSONResponse(final HttpServletResponse response, final String text) throws ServletException, IOException {
        response.setContentType("application/json");
        final PrintWriter writer = response.getWriter();
        writer.println(text);
        writer.flush();
        writer.close();
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)KeycloakServlet.class);
    }
}
