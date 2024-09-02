// 
// Decompiled by Procyon v0.5.36
// 

package org.eclipse.jetty.servlets;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.ServletRequest;
import org.eclipse.jetty.util.URIUtil;
import jakarta.servlet.RequestDispatcher;
import java.util.ArrayList;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;

@Deprecated
public class ConcatServlet extends HttpServlet
{
    private boolean _development;
    private long _lastModified;
    
    public void init() throws ServletException {
        this._lastModified = System.currentTimeMillis();
        this._development = Boolean.parseBoolean(this.getInitParameter("development"));
    }
    
    protected long getLastModified(final HttpServletRequest req) {
        return this._development ? -1L : this._lastModified;
    }
    
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        final String query = request.getQueryString();
        if (query == null) {
            response.sendError(204);
            return;
        }
        final List<RequestDispatcher> dispatchers = new ArrayList<RequestDispatcher>();
        final String[] parts = query.split("\\&");
        String type = null;
        for (final String part : parts) {
            final String path = URIUtil.canonicalPath(URIUtil.decodePath(part));
            if (path == null) {
                response.sendError(404);
                return;
            }
            if (this.startsWith(path, "/WEB-INF/") || this.startsWith(path, "/META-INF/")) {
                response.sendError(404);
                return;
            }
            final String t = this.getServletContext().getMimeType(path);
            if (t != null) {
                if (type == null) {
                    type = t;
                }
                else if (!type.equals(t)) {
                    response.sendError(415);
                    return;
                }
            }
            final RequestDispatcher dispatcher = this.getServletContext().getRequestDispatcher(part);
            if (dispatcher != null) {
                dispatchers.add(dispatcher);
            }
        }
        if (type != null) {
            response.setContentType(type);
        }
        for (final RequestDispatcher dispatcher2 : dispatchers) {
            dispatcher2.include((ServletRequest)request, (ServletResponse)response);
        }
    }
    
    private boolean startsWith(final String path, final String prefix) {
        return prefix.regionMatches(true, 0, path, 0, prefix.length());
    }
}
