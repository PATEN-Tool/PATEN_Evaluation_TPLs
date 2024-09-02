// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.hadoop.log;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import javax.servlet.ServletException;
import org.apache.commons.logging.Log;
import java.io.PrintWriter;
import org.apache.commons.logging.impl.Jdk14Logger;
import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.commons.logging.LogFactory;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.hadoop.util.ServletUtil;
import org.apache.hadoop.http.HttpServer2;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.hadoop.classification.InterfaceAudience;
import javax.servlet.http.HttpServlet;
import java.net.URLConnection;
import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.google.common.base.Charsets;
import java.net.URL;
import java.util.regex.Pattern;
import org.apache.hadoop.classification.InterfaceStability;

@InterfaceStability.Evolving
public class LogLevel
{
    public static final String USAGES = "\nUsage: General options are:\n\t[-getlevel <host:httpPort> <name>]\n\t[-setlevel <host:httpPort> <name> <level>]\n";
    static final String MARKER = "<!-- OUTPUT -->";
    static final Pattern TAG;
    
    public static void main(final String[] args) {
        if (args.length == 3 && "-getlevel".equals(args[0])) {
            process("http://" + args[1] + "/logLevel?log=" + args[2]);
            return;
        }
        if (args.length == 4 && "-setlevel".equals(args[0])) {
            process("http://" + args[1] + "/logLevel?log=" + args[2] + "&level=" + args[3]);
            return;
        }
        System.err.println("\nUsage: General options are:\n\t[-getlevel <host:httpPort> <name>]\n\t[-setlevel <host:httpPort> <name> <level>]\n");
        System.exit(-1);
    }
    
    private static void process(final String urlstring) {
        try {
            final URL url = new URL(urlstring);
            System.out.println("Connecting to " + url);
            final URLConnection connection = url.openConnection();
            connection.connect();
            final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), Charsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("<!-- OUTPUT -->")) {
                    System.out.println(LogLevel.TAG.matcher(line).replaceAll(""));
                }
            }
            in.close();
        }
        catch (IOException ioe) {
            System.err.println("" + ioe);
        }
    }
    
    static {
        TAG = Pattern.compile("<[^>]*>");
    }
    
    @InterfaceAudience.LimitedPrivate({ "HDFS", "MapReduce" })
    @InterfaceStability.Unstable
    public static class Servlet extends HttpServlet
    {
        private static final long serialVersionUID = 1L;
        static final String FORMS = "\n<br /><hr /><h3>Get / Set</h3>\n<form>Log: <input type='text' size='50' name='log' /> <input type='submit' value='Get Log Level' /></form>\n<form>Log: <input type='text' size='50' name='log' /> Level: <input type='text' name='level' /> <input type='submit' value='Set Log Level' /></form>";
        
        public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
            if (!HttpServer2.hasAdministratorAccess(this.getServletContext(), request, response)) {
                return;
            }
            final PrintWriter out = ServletUtil.initHTML((ServletResponse)response, "Log Level");
            final String logName = ServletUtil.getParameter((ServletRequest)request, "log");
            final String level = ServletUtil.getParameter((ServletRequest)request, "level");
            if (logName != null) {
                out.println("<br /><hr /><h3>Results</h3>");
                out.println("<!-- OUTPUT -->Submitted Log Name: <b>" + logName + "</b><br />");
                final Log log = LogFactory.getLog(logName);
                out.println("<!-- OUTPUT -->Log Class: <b>" + log.getClass().getName() + "</b><br />");
                if (level != null) {
                    out.println("<!-- OUTPUT -->Submitted Level: <b>" + level + "</b><br />");
                }
                if (log instanceof Log4JLogger) {
                    process(((Log4JLogger)log).getLogger(), level, out);
                }
                else if (log instanceof Jdk14Logger) {
                    process(((Jdk14Logger)log).getLogger(), level, out);
                }
                else {
                    out.println("Sorry, " + log.getClass() + " not supported.<br />");
                }
            }
            out.println("\n<br /><hr /><h3>Get / Set</h3>\n<form>Log: <input type='text' size='50' name='log' /> <input type='submit' value='Get Log Level' /></form>\n<form>Log: <input type='text' size='50' name='log' /> Level: <input type='text' name='level' /> <input type='submit' value='Set Log Level' /></form>");
            out.println(ServletUtil.HTML_TAIL);
        }
        
        private static void process(final Logger log, final String level, final PrintWriter out) throws IOException {
            if (level != null) {
                if (!level.equals(Level.toLevel(level).toString())) {
                    out.println("<!-- OUTPUT -->Bad level : <b>" + level + "</b><br />");
                }
                else {
                    log.setLevel(Level.toLevel(level));
                    out.println("<!-- OUTPUT -->Setting Level to " + level + " ...<br />");
                }
            }
            out.println("<!-- OUTPUT -->Effective level: <b>" + log.getEffectiveLevel() + "</b><br />");
        }
        
        private static void process(java.util.logging.Logger log, final String level, final PrintWriter out) throws IOException {
            if (level != null) {
                log.setLevel(java.util.logging.Level.parse(level));
                out.println("<!-- OUTPUT -->Setting Level to " + level + " ...<br />");
            }
            java.util.logging.Level lev;
            while ((lev = log.getLevel()) == null) {
                log = log.getParent();
            }
            out.println("<!-- OUTPUT -->Effective level: <b>" + lev + "</b><br />");
        }
    }
}
