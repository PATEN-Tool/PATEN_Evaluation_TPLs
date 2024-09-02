// 
// Decompiled by Procyon v0.5.36
// 

package org.jboss.remoting3.remote;

import org.xnio.channels.SslChannel;
import org.jboss.remoting3._private.Messages;
import java.util.function.Consumer;
import org.wildfly.common.Assert;
import org.wildfly.security.auth.server.SaslAuthenticationFactory;
import org.jboss.remoting3.spi.ExternalConnectionProvider;
import org.xnio.ChannelListeners;
import java.nio.channels.Channel;
import java.io.Closeable;
import org.xnio.IoUtils;
import org.xnio.Cancellable;
import org.xnio.http.HandshakeChecker;
import java.util.Map;
import org.xnio.http.HttpUpgrade;
import java.util.HashMap;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.util.Base64;
import java.security.SecureRandom;
import org.xnio.Options;
import org.xnio.ssl.SslConnection;
import javax.net.ssl.SSLContext;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.xnio.FutureResult;
import java.net.URISyntaxException;
import org.xnio.FailedIoFuture;
import org.xnio.IoFuture;
import org.xnio.StreamConnection;
import org.xnio.ChannelListener;
import java.net.InetSocketAddress;
import java.net.URI;
import java.io.IOException;
import org.jboss.remoting3.spi.ConnectionProviderContext;
import org.xnio.OptionMap;

final class HttpUpgradeConnectionProvider extends RemoteConnectionProvider
{
    public static final String MAGIC_NUMBER = "CF70DEB8-70F9-4FBA-8B4F-DFC3E723B4CD";
    public static final String SEC_JBOSS_REMOTING_KEY = "Sec-JbossRemoting-Key";
    public static final String SEC_JBOSS_REMOTING_ACCEPT = "sec-jbossremoting-accept";
    public static final String UPGRADE = "Upgrade";
    private final ProviderInterface providerInterface;
    
    HttpUpgradeConnectionProvider(final OptionMap optionMap, final ConnectionProviderContext connectionProviderContext, final String protocolName) throws IOException {
        super(optionMap, connectionProviderContext, protocolName);
        this.providerInterface = new ProviderInterface();
    }
    
    @Override
    protected IoFuture<StreamConnection> createConnection(final URI uri, final InetSocketAddress bindAddress, final InetSocketAddress destination, final OptionMap connectOptions, final ChannelListener<StreamConnection> openListener) {
        URI newUri;
        try {
            newUri = new URI("http", "", uri.getHost(), uri.getPort(), "/", "", "");
        }
        catch (URISyntaxException e) {
            return (IoFuture<StreamConnection>)new FailedIoFuture(new IOException(e));
        }
        final FutureResult<StreamConnection> returnedFuture = (FutureResult<StreamConnection>)new FutureResult(this.getExecutor());
        final ChannelListener<StreamConnection> upgradeListener = (ChannelListener<StreamConnection>)new UpgradeListener(StreamConnection.class, newUri, openListener, returnedFuture);
        final IoFuture<StreamConnection> rawFuture = super.createConnection(uri, bindAddress, destination, connectOptions, upgradeListener);
        rawFuture.addNotifier((IoFuture.Notifier)new IoFuture.HandlingNotifier<StreamConnection, FutureResult<StreamConnection>>() {
            public void handleCancelled(final FutureResult<StreamConnection> attachment) {
                attachment.setCancelled();
            }
            
            public void handleFailed(final IOException exception, final FutureResult<StreamConnection> attachment) {
                attachment.setException(exception);
            }
        }, (Object)returnedFuture);
        return (IoFuture<StreamConnection>)returnedFuture.getIoFuture();
    }
    
    @Override
    protected IoFuture<SslConnection> createSslConnection(final URI uri, final InetSocketAddress bindAddress, final InetSocketAddress destination, final OptionMap options, final AuthenticationConfiguration configuration, final SSLContext sslContext, final ChannelListener<StreamConnection> openListener) {
        URI newUri;
        try {
            newUri = new URI("https", "", uri.getHost(), uri.getPort(), "/", "", "");
        }
        catch (URISyntaxException e) {
            return (IoFuture<SslConnection>)new FailedIoFuture(new IOException(e));
        }
        final FutureResult<SslConnection> returnedFuture = (FutureResult<SslConnection>)new FutureResult(this.getExecutor());
        final OptionMap modifiedOptions = OptionMap.builder().addAll(options).set(Options.SSL_STARTTLS, false).getMap();
        final ChannelListener<StreamConnection> upgradeListener = (ChannelListener<StreamConnection>)new UpgradeListener((Class<StreamConnection>)SslConnection.class, newUri, openListener, (org.xnio.FutureResult<StreamConnection>)returnedFuture);
        final IoFuture<SslConnection> rawFuture = super.createSslConnection(uri, bindAddress, destination, modifiedOptions, configuration, sslContext, upgradeListener);
        rawFuture.addNotifier((IoFuture.Notifier)new IoFuture.HandlingNotifier<StreamConnection, FutureResult<SslConnection>>() {
            public void handleCancelled(final FutureResult<SslConnection> attachment) {
                attachment.setCancelled();
            }
            
            public void handleFailed(final IOException exception, final FutureResult<SslConnection> attachment) {
                attachment.setException(exception);
            }
        }, (Object)returnedFuture);
        return (IoFuture<SslConnection>)returnedFuture.getIoFuture();
    }
    
    @Override
    public ProviderInterface getProviderInterface() {
        return this.providerInterface;
    }
    
    protected static String createSecKey() {
        final SecureRandom random = new SecureRandom();
        final byte[] data = new byte[16];
        for (int i = 0; i < 4; ++i) {
            final int val = random.nextInt();
            data[i * 4] = (byte)val;
            data[i * 4 + 1] = (byte)(val >> 8 & 0xFF);
            data[i * 4 + 2] = (byte)(val >> 16 & 0xFF);
            data[i * 4 + 3] = (byte)(val >> 24 & 0xFF);
        }
        return Base64.getEncoder().encodeToString(data);
    }
    
    protected static String createExpectedResponse(final String secKey) throws IOException {
        try {
            final String concat = secKey + "CF70DEB8-70F9-4FBA-8B4F-DFC3E723B4CD";
            final MessageDigest digest = MessageDigest.getInstance("SHA1");
            digest.update(concat.getBytes("UTF-8"));
            final byte[] bytes = digest.digest();
            return Base64.getEncoder().encodeToString(bytes);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }
    
    private static class UpgradeListener<T extends StreamConnection> implements ChannelListener<StreamConnection>
    {
        private final Class<T> type;
        private final URI uri;
        private final ChannelListener<StreamConnection> openListener;
        private final FutureResult<T> futureResult;
        
        UpgradeListener(final Class<T> type, final URI uri, final ChannelListener<StreamConnection> openListener, final FutureResult<T> futureResult) {
            this.type = type;
            this.uri = uri;
            this.openListener = openListener;
            this.futureResult = futureResult;
        }
        
        public void handleEvent(final StreamConnection channel) {
            final Map<String, String> headers = new HashMap<String, String>();
            headers.put("Upgrade", "jboss-remoting");
            final String secKey = HttpUpgradeConnectionProvider.createSecKey();
            headers.put("Sec-JbossRemoting-Key", secKey);
            final IoFuture<T> upgradeFuture = (IoFuture<T>)HttpUpgrade.performUpgrade((StreamConnection)this.type.cast(channel), this.uri, (Map)headers, upgradeChannel -> ChannelListeners.invokeChannelListener((Channel)upgradeChannel, (ChannelListener)this.openListener), (HandshakeChecker)new RemotingHandshakeChecker(secKey));
            this.futureResult.addCancelHandler((Cancellable)new Cancellable() {
                public Cancellable cancel() {
                    if (channel.isOpen()) {
                        IoUtils.safeClose((Closeable)channel);
                    }
                    return (Cancellable)this;
                }
            });
            upgradeFuture.addNotifier((IoFuture.Notifier)new IoFuture.HandlingNotifier<T, FutureResult<T>>() {
                public void handleCancelled(final FutureResult<T> attachment) {
                    attachment.setCancelled();
                }
                
                public void handleFailed(final IOException exception, final FutureResult<T> attachment) {
                    attachment.setException(exception);
                }
                
                public void handleDone(final T data, final FutureResult<T> attachment) {
                    attachment.setResult((Object)data);
                }
            }, (Object)this.futureResult);
        }
    }
    
    private static class RemotingHandshakeChecker implements HandshakeChecker
    {
        private final String key;
        
        private RemotingHandshakeChecker(final String key) {
            this.key = key;
        }
        
        public void checkHandshake(final Map<String, String> headers) throws IOException {
            if (!headers.containsKey("sec-jbossremoting-accept")) {
                throw new IOException("No sec-jbossremoting-accept header in response");
            }
            final String expectedResponse = HttpUpgradeConnectionProvider.createExpectedResponse(this.key);
            final String response = headers.get("sec-jbossremoting-accept");
            if (!response.equals(expectedResponse)) {
                throw new IOException("sec-jbossremoting-accept value of " + response + " did not match expected " + expectedResponse);
            }
        }
    }
    
    final class ProviderInterface implements ExternalConnectionProvider
    {
        @Override
        public ConnectionAdaptorImpl createConnectionAdaptor(final OptionMap optionMap, final SaslAuthenticationFactory saslAuthenticationFactory) throws IOException {
            Assert.checkNotNullParam("optionMap", (Object)optionMap);
            Assert.checkNotNullParam("saslAuthenticationFactory", (Object)saslAuthenticationFactory);
            return new ConnectionAdaptorImpl(optionMap, saslAuthenticationFactory);
        }
    }
    
    private final class ConnectionAdaptorImpl implements Consumer<StreamConnection>
    {
        private final OptionMap optionMap;
        private final SaslAuthenticationFactory saslAuthenticationFactory;
        
        ConnectionAdaptorImpl(final OptionMap optionMap, final SaslAuthenticationFactory saslAuthenticationFactory) {
            this.optionMap = optionMap;
            this.saslAuthenticationFactory = saslAuthenticationFactory;
        }
        
        @Override
        public void accept(final StreamConnection channel) {
            if (channel.getWorker() != HttpUpgradeConnectionProvider.this.getXnioWorker()) {
                throw Messages.conn.invalidWorker();
            }
            try {
                channel.setOption(Options.TCP_NODELAY, (Object)Boolean.TRUE);
            }
            catch (IOException ex) {}
            final SslChannel sslChannel = (channel instanceof SslConnection) ? channel : null;
            final RemoteConnection connection = new RemoteConnection(channel, sslChannel, this.optionMap, HttpUpgradeConnectionProvider.this);
            final ServerConnectionOpenListener openListener = new ServerConnectionOpenListener(connection, HttpUpgradeConnectionProvider.this.getConnectionProviderContext(), this.saslAuthenticationFactory, this.optionMap);
            channel.getSinkChannel().setWriteListener((ChannelListener)connection.getWriteListener());
            Messages.conn.tracef("Accepted connection from %s to %s", (Object)channel.getPeerAddress(), (Object)channel.getLocalAddress());
            openListener.handleEvent(channel.getSourceChannel());
        }
    }
}
