// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.velocity.tools.view;

import java.io.PrintWriter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.commons.lang3.StringEscapeUtils;
import java.io.Writer;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;

public class VelocityViewServlet extends HttpServlet
{
    public static final String BUFFER_OUTPUT_PARAM = "org.apache.velocity.tools.bufferOutput";
    private static final long serialVersionUID = -3329444102562079189L;
    private transient VelocityView view;
    private boolean bufferOutput;
    
    public VelocityViewServlet() {
        this.bufferOutput = false;
    }
    
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        this.getVelocityView();
        final String buffer = this.findInitParameter(config, "org.apache.velocity.tools.bufferOutput");
        if (buffer != null && buffer.equals("true")) {
            this.bufferOutput = true;
            this.getLog().debug("VelocityViewServlet will buffer mergeTemplate output.");
        }
    }
    
    protected String findInitParameter(final ServletConfig config, final String key) {
        String param = config.getInitParameter(key);
        if (param == null || param.length() == 0) {
            final ServletContext servletContext = config.getServletContext();
            param = servletContext.getInitParameter(key);
        }
        return param;
    }
    
    protected VelocityView getVelocityView() {
        if (this.view == null) {
            this.setVelocityView(ServletUtils.getVelocityView(this.getServletConfig()));
            assert this.view != null;
        }
        return this.view;
    }
    
    protected void setVelocityView(final VelocityView view) {
        this.view = view;
    }
    
    protected String getVelocityProperty(final String name, final String alternate) {
        return this.getVelocityView().getProperty(name, alternate);
    }
    
    protected Logger getLog() {
        return this.getVelocityView().getLog();
    }
    
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        this.doRequest(request, response);
    }
    
    public void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        this.doRequest(request, response);
    }
    
    protected void doRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        Context context = null;
        try {
            this.initRequest(request, response);
            context = this.createContext(request, response);
            this.fillContext(context, request);
            this.setContentType(request, response);
            final Template template = this.handleRequest(request, response, context);
            this.mergeTemplate(template, context, response);
        }
        catch (IOException e) {
            this.error(request, response, e);
            throw e;
        }
        catch (ResourceNotFoundException e2) {
            this.manageResourceNotFound(request, response, e2);
        }
        catch (RuntimeException e3) {
            this.error(request, response, e3);
            throw e3;
        }
        finally {
            this.requestCleanup(request, response, context);
        }
    }
    
    protected void initRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        try {
            request.setCharacterEncoding(this.getVelocityProperty("input.encoding", "UTF-8"));
        }
        catch (UnsupportedEncodingException uee) {
            this.error(request, response, uee);
            throw uee;
        }
    }
    
    protected Template handleRequest(final HttpServletRequest request, final HttpServletResponse response, final Context ctx) {
        return this.getTemplate(request, response);
    }
    
    protected Context createContext(final HttpServletRequest request, final HttpServletResponse response) {
        return (Context)this.getVelocityView().createContext(request, response);
    }
    
    protected void fillContext(final Context context, final HttpServletRequest request) {
    }
    
    protected void setContentType(final HttpServletRequest request, final HttpServletResponse response) {
        response.setContentType(this.getVelocityView().getDefaultContentType());
    }
    
    protected Template getTemplate(final HttpServletRequest request, final HttpServletResponse response) {
        return this.getVelocityView().getTemplate(request);
    }
    
    protected Template getTemplate(final String name) {
        return this.getVelocityView().getTemplate(name);
    }
    
    protected void mergeTemplate(final Template template, final Context context, final HttpServletResponse response) throws IOException {
        Writer writer;
        if (this.bufferOutput) {
            writer = new StringWriter();
        }
        else {
            writer = response.getWriter();
        }
        this.getVelocityView().merge(template, context, writer);
        if (this.bufferOutput) {
            response.getWriter().write(writer.toString());
        }
    }
    
    protected void error(final HttpServletRequest request, final HttpServletResponse response, final Throwable e) {
        final String path = ServletUtils.getPath(request);
        if (response.isCommitted()) {
            this.getLog().error("An error occured but the response headers have already been sent.");
            this.getLog().error("Error processing a template for path '{}'", (Object)path, (Object)e);
            return;
        }
        try {
            this.getLog().error("Error processing a template for path '{}'", (Object)path, (Object)e);
            final StringBuilder html = new StringBuilder();
            html.append("<html>\n");
            html.append("<head><title>Error</title></head>\n");
            html.append("<body>\n");
            html.append("<h2>VelocityView : Error processing a template for path '");
            html.append(path);
            html.append("'</h2>\n");
            Throwable cause = e;
            final String why = cause.getMessage();
            if (why != null && why.length() > 0) {
                html.append(StringEscapeUtils.escapeHtml4(why));
                html.append("\n<br>\n");
            }
            if (cause instanceof MethodInvocationException) {
                cause = cause.getCause();
            }
            final StringWriter sw = new StringWriter();
            cause.printStackTrace(new PrintWriter(sw));
            html.append("<pre>\n");
            html.append(StringEscapeUtils.escapeHtml4(sw.toString()));
            html.append("</pre>\n");
            html.append("</body>\n");
            html.append("</html>");
            response.getWriter().write(html.toString());
        }
        catch (Exception e2) {
            final String msg = "Exception while printing error screen";
            this.getLog().error(msg, (Throwable)e2);
            throw new RuntimeException(msg, e);
        }
    }
    
    protected void manageResourceNotFound(final HttpServletRequest request, final HttpServletResponse response, final ResourceNotFoundException e) throws IOException {
        final String path = ServletUtils.getPath(request);
        this.getLog().debug("Resource not found for path '{}'", (Object)path, (Object)e);
        final String message = e.getMessage();
        if (!response.isCommitted() && path != null && message != null && message.contains("'" + path + "'")) {
            response.sendError(404, path);
            return;
        }
        this.error(request, response, (Throwable)e);
        throw e;
    }
    
    protected void requestCleanup(final HttpServletRequest request, final HttpServletResponse response, final Context context) {
    }
}
