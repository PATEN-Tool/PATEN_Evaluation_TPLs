// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.phoenix.jdbc;

import com.google.common.collect.Maps;
import java.util.StringTokenizer;
import org.apache.phoenix.exception.SQLExceptionInfo;
import org.apache.phoenix.exception.SQLExceptionCode;
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
public abstract class PhoenixEmbeddedDriver implements Driver, Jdbc7Shim.Driver, SQLCloseable
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
    public static final ReadOnlyProps DEFFAULT_PROPS;
    
    PhoenixEmbeddedDriver() {
    }
    
    protected ReadOnlyProps getDefaultProps() {
        return PhoenixEmbeddedDriver.DEFFAULT_PROPS;
    }
    
    public abstract QueryServices getQueryServices();
    
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
    public java.sql.Connection connect(final String url, final Properties info) throws SQLException {
        if (!this.acceptsURL(url)) {
            return null;
        }
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
        return 5;
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
        DEFFAULT_PROPS = new ReadOnlyProps((Map<String, String>)ImmutableMap.of((Object)"DriverMajorVersion", (Object)Integer.toString(4), (Object)"DriverMinorVersion", (Object)Integer.toString(5), (Object)"DriverName", (Object)"PhoenixEmbeddedDriver"));
    }
    
    public static class ConnectionInfo
    {
        private final Integer port;
        private final String rootNode;
        private final String zookeeperQuorum;
        private final boolean isConnectionless;
        private final String principal;
        private final String keytab;
        
        private static SQLException getMalFormedUrlException(final String url) {
            return new SQLExceptionInfo.Builder(SQLExceptionCode.MALFORMED_CONNECTION_URL).setMessage(url).build().buildException();
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
                throw getMalFormedUrlException(url);
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
                            }
                        }
                    }
                }
            }
            return new ConnectionInfo(quorum, port, rootNode, principal, keytabFile);
        }
        
        public ConnectionInfo normalize(final ReadOnlyProps props) throws SQLException {
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
                        catch (NumberFormatException e) {
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
            return new ConnectionInfo(zookeeperQuorum, port, rootNode, principal, keytab);
        }
        
        public ConnectionInfo(final String zookeeperQuorum, final Integer port, final String rootNode, final String principal, final String keytab) {
            this.zookeeperQuorum = zookeeperQuorum;
            this.port = port;
            this.rootNode = rootNode;
            this.isConnectionless = "none".equals(zookeeperQuorum);
            this.principal = principal;
            this.keytab = keytab;
        }
        
        public ConnectionInfo(final String zookeeperQuorum, final Integer port, final String rootNode) {
            this(zookeeperQuorum, port, rootNode, null, null);
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
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = 31 * result + ((this.zookeeperQuorum == null) ? 0 : this.zookeeperQuorum.hashCode());
            result = 31 * result + ((this.port == null) ? 0 : this.port.hashCode());
            result = 31 * result + ((this.rootNode == null) ? 0 : this.rootNode.hashCode());
            result = 31 * result + ((this.principal == null) ? 0 : this.principal.hashCode());
            result = 31 * result + ((this.keytab == null) ? 0 : this.keytab.hashCode());
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
    }
}
