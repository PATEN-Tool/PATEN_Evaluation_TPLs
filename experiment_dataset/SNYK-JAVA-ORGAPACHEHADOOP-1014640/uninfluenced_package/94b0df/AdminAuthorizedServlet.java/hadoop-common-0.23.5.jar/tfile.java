// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.hadoop.http;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.mortbay.jetty.servlet.DefaultServlet;

public class AdminAuthorizedServlet extends DefaultServlet
{
    private static final long serialVersionUID = 1L;
    
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        if (HttpServer.hasAdministratorAccess(this.getServletContext(), request, response)) {
            super.doGet(request, response);
        }
    }
}
