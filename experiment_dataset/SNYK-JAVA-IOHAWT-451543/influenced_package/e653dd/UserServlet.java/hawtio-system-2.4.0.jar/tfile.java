// 
// Decompiled by Procyon v0.5.36
// 

package io.hawt.web.auth;

import javax.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.IOException;
import io.hawt.web.ServletHelpers;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;
import io.hawt.system.ConfigManager;
import javax.servlet.http.HttpServlet;

public class UserServlet extends HttpServlet
{
    private static final long serialVersionUID = -1239510748236245667L;
    private static final String DEFAULT_USER = "public";
    protected ConfigManager config;
    private boolean authenticationEnabled;
    
    public UserServlet() {
        this.authenticationEnabled = true;
    }
    
    public void init() throws ServletException {
        this.config = (ConfigManager)this.getServletConfig().getServletContext().getAttribute("ConfigManager");
        if (this.config != null) {
            this.authenticationEnabled = Boolean.parseBoolean(this.config.get("authenticationEnabled", "true"));
        }
        if (System.getProperty("hawtio.authenticationEnabled") != null) {
            this.authenticationEnabled = Boolean.getBoolean("hawtio.authenticationEnabled");
        }
    }
    
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        if (!this.authenticationEnabled) {
            this.sendResponse(response, "public");
            return;
        }
        final String username = this.getUsername(request, response);
        if (username == null) {
            ServletHelpers.doForbidden(response);
            return;
        }
        this.sendResponse(response, username);
    }
    
    private void sendResponse(final HttpServletResponse response, final String username) throws IOException {
        response.setContentType("application/json");
        final PrintWriter out = response.getWriter();
        out.write("\"" + username + "\"");
        out.flush();
        out.close();
    }
    
    protected String getUsername(final HttpServletRequest request, final HttpServletResponse response) {
        final HttpSession session = request.getSession(false);
        if (session != null) {
            return (String)session.getAttribute("user");
        }
        return null;
    }
}
