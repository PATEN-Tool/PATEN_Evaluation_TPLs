// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.hadoop.log;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.io.IOException;
import javax.servlet.ServletException;
import org.apache.commons.logging.Log;
import java.io.PrintWriter;
import javax.servlet.ServletContext;
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
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.google.common.base.Charsets;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.HttpsURLConnection;
import org.apache.hadoop.security.authentication.client.ConnectionConfigurator;
import org.apache.hadoop.security.authentication.client.Authenticator;
import org.apache.hadoop.security.authentication.client.KerberosAuthenticator;
import org.apache.hadoop.security.ssl.SSLFactory;
import org.apache.hadoop.security.authentication.client.AuthenticatedURL;
import java.net.URLConnection;
import java.net.URL;
import org.apache.hadoop.HadoopIllegalArgumentException;
import com.google.common.annotations.VisibleForTesting;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.conf.Configuration;
import java.util.regex.Pattern;
import org.apache.hadoop.classification.InterfaceStability;

@InterfaceStability.Evolving
public class LogLevel
{
    public static final String USAGES = "\nUsage: Command options are:\n\t[-getlevel <host:port> <classname> [-protocol (http|https)]\n\t[-setlevel <host:port> <classname> <level> [-protocol (http|https)]\n";
    public static final String PROTOCOL_HTTP = "http";
    public static final String PROTOCOL_HTTPS = "https";
    static final String MARKER = "<!-- OUTPUT -->";
    static final Pattern TAG;
    
    public static void main(final String[] args) throws Exception {
        final CLI cli = new CLI(new Configuration());
        System.exit(ToolRunner.run(cli, args));
    }
    
    private static void printUsage() {
        System.err.println("\nUsage: Command options are:\n\t[-getlevel <host:port> <classname> [-protocol (http|https)]\n\t[-setlevel <host:port> <classname> <level> [-protocol (http|https)]\n");
        GenericOptionsParser.printGenericCommandUsage(System.err);
    }
    
    public static boolean isValidProtocol(final String protocol) {
        return protocol.equals("http") || protocol.equals("https");
    }
    
    static {
        TAG = Pattern.compile("<[^>]*>");
    }
    
    private enum Operations
    {
        GETLEVEL, 
        SETLEVEL, 
        UNKNOWN;
    }
    
    @VisibleForTesting
    static class CLI extends Configured implements Tool
    {
        private Operations operation;
        private String protocol;
        private String hostName;
        private String className;
        private String level;
        
        CLI(final Configuration conf) {
            this.operation = Operations.UNKNOWN;
            this.setConf(conf);
        }
        
        @Override
        public int run(final String[] args) throws Exception {
            try {
                this.parseArguments(args);
                this.sendLogLevelRequest();
            }
            catch (HadoopIllegalArgumentException e) {
                printUsage();
                return -1;
            }
            return 0;
        }
        
        private void sendLogLevelRequest() throws HadoopIllegalArgumentException, Exception {
            switch (this.operation) {
                case GETLEVEL: {
                    this.doGetLevel();
                    break;
                }
                case SETLEVEL: {
                    this.doSetLevel();
                    break;
                }
                default: {
                    throw new HadoopIllegalArgumentException("Expect either -getlevel or -setlevel");
                }
            }
        }
        
        public void parseArguments(final String[] args) throws HadoopIllegalArgumentException {
            if (args.length == 0) {
                throw new HadoopIllegalArgumentException("No arguments specified");
            }
            int nextArgIndex = 0;
            while (nextArgIndex < args.length) {
                if (args[nextArgIndex].equals("-getlevel")) {
                    nextArgIndex = this.parseGetLevelArgs(args, nextArgIndex);
                }
                else if (args[nextArgIndex].equals("-setlevel")) {
                    nextArgIndex = this.parseSetLevelArgs(args, nextArgIndex);
                }
                else {
                    if (!args[nextArgIndex].equals("-protocol")) {
                        throw new HadoopIllegalArgumentException("Unexpected argument " + args[nextArgIndex]);
                    }
                    nextArgIndex = this.parseProtocolArgs(args, nextArgIndex);
                }
            }
            if (this.operation == Operations.UNKNOWN) {
                throw new HadoopIllegalArgumentException("Must specify either -getlevel or -setlevel");
            }
            if (this.protocol == null) {
                this.protocol = "http";
            }
        }
        
        private int parseGetLevelArgs(final String[] args, final int index) throws HadoopIllegalArgumentException {
            if (this.operation != Operations.UNKNOWN) {
                throw new HadoopIllegalArgumentException("Redundant -getlevel command");
            }
            if (index + 2 >= args.length) {
                throw new HadoopIllegalArgumentException("-getlevel needs two parameters");
            }
            this.operation = Operations.GETLEVEL;
            this.hostName = args[index + 1];
            this.className = args[index + 2];
            return index + 3;
        }
        
        private int parseSetLevelArgs(final String[] args, final int index) throws HadoopIllegalArgumentException {
            if (this.operation != Operations.UNKNOWN) {
                throw new HadoopIllegalArgumentException("Redundant -setlevel command");
            }
            if (index + 3 >= args.length) {
                throw new HadoopIllegalArgumentException("-setlevel needs three parameters");
            }
            this.operation = Operations.SETLEVEL;
            this.hostName = args[index + 1];
            this.className = args[index + 2];
            this.level = args[index + 3];
            return index + 4;
        }
        
        private int parseProtocolArgs(final String[] args, final int index) throws HadoopIllegalArgumentException {
            if (this.protocol != null) {
                throw new HadoopIllegalArgumentException("Redundant -protocol command");
            }
            if (index + 1 >= args.length) {
                throw new HadoopIllegalArgumentException("-protocol needs one parameter");
            }
            this.protocol = args[index + 1];
            if (!LogLevel.isValidProtocol(this.protocol)) {
                throw new HadoopIllegalArgumentException("Invalid protocol: " + this.protocol);
            }
            return index + 2;
        }
        
        private void doGetLevel() throws Exception {
            this.process(this.protocol + "://" + this.hostName + "/logLevel?log=" + this.className);
        }
        
        private void doSetLevel() throws Exception {
            this.process(this.protocol + "://" + this.hostName + "/logLevel?log=" + this.className + "&level=" + this.level);
        }
        
        private URLConnection connect(final URL url) throws Exception {
            final AuthenticatedURL.Token token = new AuthenticatedURL.Token();
            URLConnection connection;
            if ("https".equals(url.getProtocol())) {
                final SSLFactory clientSslFactory = new SSLFactory(SSLFactory.Mode.CLIENT, this.getConf());
                clientSslFactory.init();
                final SSLSocketFactory sslSocketF = clientSslFactory.createSSLSocketFactory();
                final AuthenticatedURL aUrl = new AuthenticatedURL((Authenticator)new KerberosAuthenticator(), (ConnectionConfigurator)clientSslFactory);
                connection = aUrl.openConnection(url, token);
                final HttpsURLConnection httpsConn = (HttpsURLConnection)connection;
                httpsConn.setSSLSocketFactory(sslSocketF);
            }
            else {
                final AuthenticatedURL aUrl = new AuthenticatedURL((Authenticator)new KerberosAuthenticator());
                connection = aUrl.openConnection(url, token);
            }
            connection.connect();
            return connection;
        }
        
        private void process(final String urlString) throws Exception {
            final URL url = new URL(urlString);
            System.out.println("Connecting to " + url);
            final URLConnection connection = this.connect(url);
            final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), Charsets.UTF_8));
            while (true) {
                final String line = in.readLine();
                if (line == null) {
                    break;
                }
                if (!line.startsWith("<!-- OUTPUT -->")) {
                    continue;
                }
                System.out.println(LogLevel.TAG.matcher(line).replaceAll(""));
            }
            in.close();
        }
    }
    
    @InterfaceAudience.LimitedPrivate({ "HDFS", "MapReduce" })
    @InterfaceStability.Unstable
    public static class Servlet extends HttpServlet
    {
        private static final long serialVersionUID = 1L;
        static final String FORMS = "\n<br /><hr /><h3>Get / Set</h3>\n<form>Class Name: <input type='text' size='50' name='log' /> <input type='submit' value='Get Log Level' /></form>\n<form>Class Name: <input type='text' size='50' name='log' /> Level: <input type='text' name='level' /> <input type='submit' value='Set Log Level' /></form>";
        
        public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
            final ServletContext servletContext = this.getServletContext();
            if (!HttpServer2.isStaticUserAndNoneAuthType(servletContext, request) && !HttpServer2.hasAdministratorAccess(servletContext, request, response)) {
                return;
            }
            final PrintWriter out = ServletUtil.initHTML((ServletResponse)response, "Log Level");
            final String logName = ServletUtil.getParameter((ServletRequest)request, "log");
            final String level = ServletUtil.getParameter((ServletRequest)request, "level");
            if (logName != null) {
                out.println("<br /><hr /><h3>Results</h3>");
                out.println("<!-- OUTPUT -->Submitted Class Name: <b>" + logName + "</b><br />");
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
            out.println("\n<br /><hr /><h3>Get / Set</h3>\n<form>Class Name: <input type='text' size='50' name='log' /> <input type='submit' value='Get Log Level' /></form>\n<form>Class Name: <input type='text' size='50' name='log' /> Level: <input type='text' name='level' /> <input type='submit' value='Set Log Level' /></form>");
            out.println(ServletUtil.HTML_TAIL);
        }
        
        private static void process(final Logger log, final String level, final PrintWriter out) throws IOException {
            if (level != null) {
                if (!level.equalsIgnoreCase(Level.toLevel(level).toString())) {
                    out.println("<!-- OUTPUT -->Bad Level : <b>" + level + "</b><br />");
                }
                else {
                    log.setLevel(Level.toLevel(level));
                    out.println("<!-- OUTPUT -->Setting Level to " + level + " ...<br />");
                }
            }
            out.println("<!-- OUTPUT -->Effective Level: <b>" + log.getEffectiveLevel() + "</b><br />");
        }
        
        private static void process(java.util.logging.Logger log, final String level, final PrintWriter out) throws IOException {
            if (level != null) {
                final String levelToUpperCase = level.toUpperCase();
                try {
                    log.setLevel(java.util.logging.Level.parse(levelToUpperCase));
                }
                catch (IllegalArgumentException e) {
                    out.println("<!-- OUTPUT -->Bad Level : <b>" + level + "</b><br />");
                }
                out.println("<!-- OUTPUT -->Setting Level to " + level + " ...<br />");
            }
            java.util.logging.Level lev;
            while ((lev = log.getLevel()) == null) {
                log = log.getParent();
            }
            out.println("<!-- OUTPUT -->Effective Level: <b>" + lev + "</b><br />");
        }
    }
}
