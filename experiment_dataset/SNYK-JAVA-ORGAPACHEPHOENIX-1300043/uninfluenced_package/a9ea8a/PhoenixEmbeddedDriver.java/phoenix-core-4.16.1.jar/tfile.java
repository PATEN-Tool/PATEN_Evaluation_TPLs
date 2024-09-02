// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.phoenix.jdbc;

import java.util.Hashtable;
import org.slf4j.LoggerFactory;
import com.google.common.collect.Maps;
import java.util.Iterator;
import org.apache.phoenix.query.HBaseFactoryProvider;
import org.apache.hadoop.security.SecurityUtil;
import org.apache.hadoop.security.authentication.util.KerberosUtil;
import org.apache.hadoop.conf.Configuration;
import java.io.IOException;
import org.apache.hadoop.security.UserGroupInformation;
import java.util.StringTokenizer;
import org.apache.phoenix.exception.SQLExceptionInfo;
import org.apache.phoenix.exception.SQLExceptionCode;
import org.apache.hadoop.hbase.security.User;
import com.google.common.collect.ImmutableMap;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import org.apache.phoenix.query.ConnectionQueryServices;
import java.util.Map;
import org.apache.phoenix.util.PropertiesUtil;
import java.sql.Connection;
import java.util.Properties;
import java.sql.SQLException;
import org.apache.phoenix.query.QueryServices;
import org.apache.phoenix.util.ReadOnlyProps;
import java.sql.DriverPropertyInfo;
import javax.annotation.concurrent.Immutable;
import org.apache.phoenix.util.SQLCloseable;
import java.sql.Driver;

@Immutable
public abstract class PhoenixEmbeddedDriver implements Driver, SQLCloseable
{
    private static final String DNC_JDBC_PROTOCOL_SUFFIX = "//";
    private static final String DRIVER_NAME = "PhoenixEmbeddedDriver";
    private static final String TERMINATOR = ";";
    private static final String DELIMITERS = ";:";
    private static final String TEST_URL_AT_END = ";test=true";
    private static final String TEST_URL_IN_MIDDLE = ";test=true;";
    private static final DriverPropertyInfo[] EMPTY_INFO;
    public static final String MAJOR_VERSION_PROP = "DriverMajorVersion";
    public static final String MINOR_VERSION_PROP = "DriverMinorVersion";
    public static final String DRIVER_NAME_PROP = "DriverName";
    public static final ReadOnlyProps DEFAULT_PROPS;
    
    PhoenixEmbeddedDriver() {
    }
    
    protected ReadOnlyProps getDefaultProps() {
        return PhoenixEmbeddedDriver.DEFAULT_PROPS;
    }
    
    public abstract QueryServices getQueryServices() throws SQLException;
    
    @Override
    public boolean acceptsURL(final String url) throws SQLException {
        if (url.startsWith("jdbc:phoenix")) {
            if (url.length() == "jdbc:phoenix".length()) {
                return true;
            }
            if (';' == url.charAt("jdbc:phoenix".length())) {
                return true;
            }
            if (':' == url.charAt("jdbc:phoenix".length())) {
                final int protoLength = "jdbc:phoenix".length() + 1;
                if (url.length() == protoLength) {
                    return true;
                }
                if (url.startsWith("jdbc:phoenix:thin")) {
                    return false;
                }
                if (!url.startsWith("//", protoLength)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public Connection connect(final String url, final Properties info) throws SQLException {
        if (!this.acceptsURL(url)) {
            return null;
        }
        return this.createConnection(url, info);
    }
    
    protected final Connection createConnection(final String url, final Properties info) throws SQLException {
        final Properties augmentedInfo = PropertiesUtil.deepCopy(info);
        augmentedInfo.putAll(this.getDefaultProps().asMap());
        final ConnectionQueryServices connectionServices = this.getConnectionQueryServices(url, augmentedInfo);
        final PhoenixConnection connection = connectionServices.connect(url, augmentedInfo);
        return connection;
    }
    
    protected abstract ConnectionQueryServices getConnectionQueryServices(final String p0, final Properties p1) throws SQLException;
    
    @Override
    public int getMajorVersion() {
        return 4;
    }
    
    @Override
    public int getMinorVersion() {
        return 16;
    }
    
    @Override
    public DriverPropertyInfo[] getPropertyInfo(final String url, final Properties info) throws SQLException {
        return PhoenixEmbeddedDriver.EMPTY_INFO;
    }
    
    @Override
    public boolean jdbcCompliant() {
        return false;
    }
    
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
    
    @Override
    public void close() throws SQLException {
    }
    
    public static boolean isTestUrl(final String url) {
        return url.endsWith(";test=true") || url.contains(";test=true;");
    }
    
    static {
        EMPTY_INFO = new DriverPropertyInfo[0];
        DEFAULT_PROPS = new ReadOnlyProps((Map<String, String>)ImmutableMap.of((Object)"DriverMajorVersion", (Object)Integer.toString(4), (Object)"DriverMinorVersion", (Object)Integer.toString(16), (Object)"DriverName", (Object)"PhoenixEmbeddedDriver"));
    }
    
    public static class ConnectionInfo
    {
        private static final org.slf4j.Logger LOGGER;
        private static final Object KERBEROS_LOGIN_LOCK;
        private static final char WINDOWS_SEPARATOR_CHAR = '\\';
        private static final String REALM_EQUIVALENCY_WARNING_MSG = "Provided principal does not contan a realm and the default realm cannot be determined. Ignoring realm equivalency check.";
        private final Integer port;
        private final String rootNode;
        private final String zookeeperQuorum;
        private final boolean isConnectionless;
        private final String principal;
        private final String keytab;
        private final User user;
        
        private static SQLException getMalFormedUrlException(final String url) {
            return new SQLExceptionInfo.Builder(SQLExceptionCode.MALFORMED_CONNECTION_URL).setMessage(url).build().buildException();
        }
        
        public String getZookeeperConnectionString() {
            return this.getZookeeperQuorum() + ":" + this.getPort();
        }
        
        private static boolean isMultiPortUrl(final String portStr) {
            final int commaIndex = portStr.indexOf(44);
            if (commaIndex > 0) {
                try {
                    Integer.parseInt(portStr.substring(0, commaIndex));
                    return true;
                }
                catch (NumberFormatException ex) {}
            }
            return false;
        }
        
        public static ConnectionInfo create(String url) throws SQLException {
            url = ((url == null) ? "" : url);
            if (url.isEmpty() || url.equalsIgnoreCase("jdbc:phoenix:") || url.equalsIgnoreCase("jdbc:phoenix")) {
                return defaultConnectionInfo(url);
            }
            url = (url.startsWith("jdbc:phoenix") ? url.substring("jdbc:phoenix".length()) : (':' + url));
            StringTokenizer tokenizer;
            int nTokens;
            String[] tokens;
            String token;
            for (tokenizer = new StringTokenizer(url, ";:", true), nTokens = 0, tokens = new String[5], token = null; tokenizer.hasMoreTokens() && !(token = tokenizer.nextToken()).equals(";") && tokenizer.hasMoreTokens() && nTokens < tokens.length; tokens[nTokens++] = token) {
                token = tokenizer.nextToken();
                if (";:".contains(token)) {
                    throw getMalFormedUrlException(url);
                }
            }
            if (tokenizer.hasMoreTokens() && !";".equals(token)) {
                final String extraToken = tokenizer.nextToken();
                if ('\\' != extraToken.charAt(0)) {
                    throw getMalFormedUrlException(url);
                }
                final String prevToken = tokens[nTokens - 1];
                tokens[nTokens - 1] = prevToken + ":" + extraToken;
                if (tokenizer.hasMoreTokens() && !(token = tokenizer.nextToken()).equals(";")) {
                    throw getMalFormedUrlException(url);
                }
            }
            String quorum = null;
            Integer port = null;
            String rootNode = null;
            String principal = null;
            String keytabFile = null;
            int tokenIndex = 0;
            if (nTokens > tokenIndex) {
                quorum = tokens[tokenIndex++];
                if (nTokens > tokenIndex) {
                    try {
                        port = Integer.parseInt(tokens[tokenIndex]);
                        if (port < 0) {
                            throw getMalFormedUrlException(url);
                        }
                        ++tokenIndex;
                    }
                    catch (NumberFormatException e) {
                        if (isMultiPortUrl(tokens[tokenIndex])) {
                            throw getMalFormedUrlException(url);
                        }
                    }
                    if (nTokens > tokenIndex) {
                        if (tokens[tokenIndex].startsWith("/")) {
                            rootNode = tokens[tokenIndex++];
                        }
                        if (nTokens > tokenIndex) {
                            principal = tokens[tokenIndex++];
                            if (nTokens > tokenIndex) {
                                keytabFile = tokens[tokenIndex++];
                                if (tokenIndex < tokens.length) {
                                    final String nextToken = tokens[tokenIndex++];
                                    if (null != nextToken && '\\' == nextToken.charAt(0)) {
                                        keytabFile = keytabFile + ":" + nextToken;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return new ConnectionInfo(quorum, port, rootNode, principal, keytabFile);
        }
        
        public ConnectionInfo normalize(final ReadOnlyProps props, final Properties info) throws SQLException {
            String zookeeperQuorum = this.getZookeeperQuorum();
            Integer port = this.getPort();
            String rootNode = this.getRootNode();
            String keytab = this.getKeytab();
            String principal = this.getPrincipal();
            if (zookeeperQuorum == null) {
                zookeeperQuorum = props.get("hbase.zookeeper.quorum");
                if (zookeeperQuorum == null) {
                    throw new SQLExceptionInfo.Builder(SQLExceptionCode.MALFORMED_CONNECTION_URL).setMessage(this.toString()).build().buildException();
                }
            }
            if (port == null) {
                if (!this.isConnectionless) {
                    final String portStr = props.get("hbase.zookeeper.property.clientPort");
                    if (portStr != null) {
                        try {
                            port = Integer.parseInt(portStr);
                        }
                        catch (NumberFormatException e2) {
                            throw new SQLExceptionInfo.Builder(SQLExceptionCode.MALFORMED_CONNECTION_URL).setMessage(this.toString()).build().buildException();
                        }
                    }
                }
            }
            else if (this.isConnectionless) {
                throw new SQLExceptionInfo.Builder(SQLExceptionCode.MALFORMED_CONNECTION_URL).setMessage("Port may not be specified when using the connectionless url \"" + this.toString() + "\"").build().buildException();
            }
            if (rootNode == null) {
                if (!this.isConnectionless) {
                    rootNode = props.get("zookeeper.znode.parent");
                }
            }
            else if (this.isConnectionless) {
                throw new SQLExceptionInfo.Builder(SQLExceptionCode.MALFORMED_CONNECTION_URL).setMessage("Root node may not be specified when using the connectionless url \"" + this.toString() + "\"").build().buildException();
            }
            if (principal == null && !this.isConnectionless) {
                principal = props.get("hbase.myclient.principal");
            }
            if (keytab == null && !this.isConnectionless) {
                keytab = props.get("hbase.myclient.keytab");
            }
            if (!this.isConnectionless()) {
                final boolean credsProvidedInUrl = null != principal && null != keytab;
                final boolean credsProvidedInProps = info.containsKey("hbase.myclient.principal") && info.containsKey("hbase.myclient.keytab");
                Label_0543: {
                    if (!credsProvidedInUrl) {
                        if (!credsProvidedInProps) {
                            break Label_0543;
                        }
                    }
                    try {
                        UserGroupInformation currentUser = UserGroupInformation.getCurrentUser();
                        if (!currentUser.hasKerberosCredentials() || !isSameName(currentUser.getUserName(), principal)) {
                            synchronized (ConnectionInfo.KERBEROS_LOGIN_LOCK) {
                                currentUser = UserGroupInformation.getCurrentUser();
                                if (!currentUser.hasKerberosCredentials() || !isSameName(currentUser.getUserName(), principal)) {
                                    final Configuration config = this.getConfiguration(props, info, principal, keytab);
                                    ConnectionInfo.LOGGER.info("Trying to connect to a secure cluster as {} with keytab {}", (Object)config.get("hbase.myclient.principal"), (Object)config.get("hbase.myclient.keytab"));
                                    UserGroupInformation.setConfiguration(config);
                                    User.login(config, "hbase.myclient.keytab", "hbase.myclient.principal", (String)null);
                                    ConnectionInfo.LOGGER.info("Successful login to secure cluster");
                                }
                            }
                        }
                        else {
                            ConnectionInfo.LOGGER.debug("Already logged in as {}", (Object)currentUser);
                        }
                        return new ConnectionInfo(zookeeperQuorum, port, rootNode, principal, keytab);
                    }
                    catch (IOException e) {
                        throw new SQLExceptionInfo.Builder(SQLExceptionCode.CANNOT_ESTABLISH_CONNECTION).setRootCause(e).build().buildException();
                    }
                }
                ConnectionInfo.LOGGER.debug("Principal and keytab not provided, not attempting Kerberos login");
            }
            return new ConnectionInfo(zookeeperQuorum, port, rootNode, principal, keytab);
        }
        
        static boolean isSameName(final String currentName, final String newName) throws IOException {
            return isSameName(currentName, newName, null, getDefaultKerberosRealm());
        }
        
        static String getDefaultKerberosRealm() {
            try {
                return KerberosUtil.getDefaultRealm();
            }
            catch (Exception e) {
                if (ConnectionInfo.LOGGER.isDebugEnabled()) {
                    ConnectionInfo.LOGGER.debug("Provided principal does not contan a realm and the default realm cannot be determined. Ignoring realm equivalency check.", (Throwable)e);
                }
                else {
                    ConnectionInfo.LOGGER.warn("Provided principal does not contan a realm and the default realm cannot be determined. Ignoring realm equivalency check.");
                }
                return null;
            }
        }
        
        static boolean isSameName(final String currentName, final String newName, final String hostname) throws IOException {
            return isSameName(currentName, newName, hostname, getDefaultKerberosRealm());
        }
        
        static boolean isSameName(final String currentName, String newName, final String hostname, final String defaultRealm) throws IOException {
            final boolean newNameContainsRealm = newName.indexOf(64) != -1;
            if (newName.contains("_HOST")) {
                if (newNameContainsRealm) {
                    newName = SecurityUtil.getServerPrincipal(newName, hostname);
                }
                else if (newName.endsWith("/_HOST")) {
                    newName = newName.substring(0, newName.length() - 5) + hostname;
                }
            }
            if (!newNameContainsRealm && defaultRealm != null) {
                return currentName.equals(newName + "@" + defaultRealm);
            }
            return currentName.equals(newName);
        }
        
        private Configuration getConfiguration(final ReadOnlyProps props, final Properties info, final String principal, final String keytab) {
            final Configuration config = HBaseFactoryProvider.getConfigurationFactory().getConfiguration();
            for (final Map.Entry<String, String> entry : props) {
                config.set((String)entry.getKey(), (String)entry.getValue());
            }
            if (info != null) {
                for (final Object key : ((Hashtable<Object, V>)info).keySet()) {
                    config.set((String)key, info.getProperty((String)key));
                }
            }
            if (null != principal) {
                config.set("hbase.myclient.principal", principal);
            }
            if (null != keytab) {
                config.set("hbase.myclient.keytab", keytab);
            }
            return config;
        }
        
        public ConnectionInfo(final String zookeeperQuorum, final Integer port, final String rootNode, final String principal, final String keytab) {
            this.zookeeperQuorum = zookeeperQuorum;
            this.port = port;
            this.rootNode = rootNode;
            this.isConnectionless = "none".equals(zookeeperQuorum);
            this.principal = principal;
            this.keytab = keytab;
            try {
                this.user = User.getCurrent();
            }
            catch (IOException e) {
                throw new RuntimeException("Couldn't get the current user!!", e);
            }
            if (null == this.user) {
                throw new RuntimeException("Acquired null user which should never happen");
            }
        }
        
        public ConnectionInfo(final String zookeeperQuorum, final Integer port, final String rootNode) {
            this(zookeeperQuorum, port, rootNode, null, null);
        }
        
        public ConnectionInfo(final ConnectionInfo other) {
            this(other.zookeeperQuorum, other.port, other.rootNode, other.principal, other.keytab);
        }
        
        public ReadOnlyProps asProps() {
            final Map<String, String> connectionProps = (Map<String, String>)Maps.newHashMapWithExpectedSize(3);
            if (this.getZookeeperQuorum() != null) {
                connectionProps.put("hbase.zookeeper.quorum", this.getZookeeperQuorum());
            }
            if (this.getPort() != null) {
                connectionProps.put("hbase.zookeeper.property.clientPort", this.getPort().toString());
            }
            if (this.getRootNode() != null) {
                connectionProps.put("zookeeper.znode.parent", this.getRootNode());
            }
            if (this.getPrincipal() != null && this.getKeytab() != null) {
                connectionProps.put("hbase.myclient.principal", this.getPrincipal());
                connectionProps.put("hbase.myclient.keytab", this.getKeytab());
            }
            return connectionProps.isEmpty() ? ReadOnlyProps.EMPTY_PROPS : new ReadOnlyProps(connectionProps.entrySet().iterator());
        }
        
        public boolean isConnectionless() {
            return this.isConnectionless;
        }
        
        public String getZookeeperQuorum() {
            return this.zookeeperQuorum;
        }
        
        public Integer getPort() {
            return this.port;
        }
        
        public String getRootNode() {
            return this.rootNode;
        }
        
        public String getKeytab() {
            return this.keytab;
        }
        
        public String getPrincipal() {
            return this.principal;
        }
        
        public User getUser() {
            return this.user;
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = 31 * result + ((this.zookeeperQuorum == null) ? 0 : this.zookeeperQuorum.hashCode());
            result = 31 * result + ((this.port == null) ? 0 : this.port.hashCode());
            result = 31 * result + ((this.rootNode == null) ? 0 : this.rootNode.hashCode());
            result = 31 * result + ((this.principal == null) ? 0 : this.principal.hashCode());
            result = 31 * result + ((this.keytab == null) ? 0 : this.keytab.hashCode());
            result = 31 * result + this.user.hashCode();
            return result;
        }
        
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (this.getClass() != obj.getClass()) {
                return false;
            }
            final ConnectionInfo other = (ConnectionInfo)obj;
            if (!other.user.equals((Object)this.user)) {
                return false;
            }
            if (this.zookeeperQuorum == null) {
                if (other.zookeeperQuorum != null) {
                    return false;
                }
            }
            else if (!this.zookeeperQuorum.equals(other.zookeeperQuorum)) {
                return false;
            }
            if (this.port == null) {
                if (other.port != null) {
                    return false;
                }
            }
            else if (!this.port.equals(other.port)) {
                return false;
            }
            if (this.rootNode == null) {
                if (other.rootNode != null) {
                    return false;
                }
            }
            else if (!this.rootNode.equals(other.rootNode)) {
                return false;
            }
            if (this.principal == null) {
                if (other.principal != null) {
                    return false;
                }
            }
            else if (!this.principal.equals(other.principal)) {
                return false;
            }
            if (this.keytab == null) {
                if (other.keytab != null) {
                    return false;
                }
            }
            else if (!this.keytab.equals(other.keytab)) {
                return false;
            }
            return true;
        }
        
        @Override
        public String toString() {
            return this.zookeeperQuorum + ((this.port == null) ? "" : (":" + this.port)) + ((this.rootNode == null) ? "" : (":" + this.rootNode)) + ((this.principal == null) ? "" : (":" + this.principal)) + ((this.keytab == null) ? "" : (":" + this.keytab));
        }
        
        public String toUrl() {
            return "jdbc:phoenix:" + this.toString();
        }
        
        private static ConnectionInfo defaultConnectionInfo(final String url) throws SQLException {
            final Configuration config = HBaseFactoryProvider.getConfigurationFactory().getConfiguration();
            final String quorum = config.get("hbase.zookeeper.quorum");
            if (quorum == null || quorum.isEmpty()) {
                throw getMalFormedUrlException(url);
            }
            final String clientPort = config.get("hbase.zookeeper.property.clientPort");
            final Integer port = (clientPort == null) ? null : Integer.valueOf(Integer.parseInt(clientPort));
            if (port == null || port < 0) {
                throw getMalFormedUrlException(url);
            }
            final String znodeParent = config.get("zookeeper.znode.parent");
            ConnectionInfo.LOGGER.debug("Getting default jdbc connection url " + quorum + ":" + port + ":" + znodeParent);
            return new ConnectionInfo(quorum, port, znodeParent);
        }
        
        static {
            LOGGER = LoggerFactory.getLogger((Class)ConnectionInfo.class);
            KERBEROS_LOGIN_LOCK = new Object();
        }
    }
}
