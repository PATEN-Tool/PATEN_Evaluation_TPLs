// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.hadoop.conf;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import javax.servlet.ServletException;
import java.io.Writer;
import org.apache.hadoop.http.HttpServer2;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.classification.InterfaceAudience;
import javax.servlet.http.HttpServlet;

@InterfaceAudience.LimitedPrivate({ "HDFS", "MapReduce" })
@InterfaceStability.Unstable
public class ConfServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    protected static final String FORMAT_JSON = "json";
    protected static final String FORMAT_XML = "xml";
    
    private Configuration getConfFromContext() {
        final Configuration conf = (Configuration)this.getServletContext().getAttribute("hadoop.conf");
        assert conf != null;
        return conf;
    }
    
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        if (!HttpServer2.isInstrumentationAccessAllowed(this.getServletContext(), request, response)) {
            return;
        }
        final String format = parseAcceptHeader(request);
        if ("xml".equals(format)) {
            response.setContentType("text/xml; charset=utf-8");
        }
        else if ("json".equals(format)) {
            response.setContentType("application/json; charset=utf-8");
        }
        final String name = request.getParameter("name");
        final Writer out = response.getWriter();
        try {
            writeResponse(this.getConfFromContext(), out, format, name);
        }
        catch (BadFormatException bfe) {
            response.sendError(400, bfe.getMessage());
        }
        catch (IllegalArgumentException iae) {
            response.sendError(404, iae.getMessage());
        }
        out.close();
    }
    
    @VisibleForTesting
    static String parseAcceptHeader(final HttpServletRequest request) {
        final String format = request.getHeader("Accept");
        return (format != null && format.contains("json")) ? "json" : "xml";
    }
    
    static void writeResponse(final Configuration conf, final Writer out, final String format, final String propertyName) throws IOException, IllegalArgumentException, BadFormatException {
        if ("json".equals(format)) {
            Configuration.dumpConfiguration(conf, propertyName, out);
        }
        else {
            if (!"xml".equals(format)) {
                throw new BadFormatException("Bad format: " + format);
            }
            conf.writeXml(propertyName, out);
        }
    }
    
    static void writeResponse(final Configuration conf, final Writer out, final String format) throws IOException, BadFormatException {
        writeResponse(conf, out, format, null);
    }
    
    public static class BadFormatException extends Exception
    {
        private static final long serialVersionUID = 1L;
        
        public BadFormatException(final String msg) {
            super(msg);
        }
    }
}
