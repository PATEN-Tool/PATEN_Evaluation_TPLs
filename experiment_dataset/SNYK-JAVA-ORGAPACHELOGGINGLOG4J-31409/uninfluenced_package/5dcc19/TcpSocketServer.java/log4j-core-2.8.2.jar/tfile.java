// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.logging.log4j.core.net.server;

import org.apache.logging.log4j.core.util.Closer;
import java.io.OptionalDataException;
import java.io.EOFException;
import org.apache.logging.log4j.core.LogEventListener;
import org.apache.logging.log4j.core.util.Log4jThread;
import com.beust.jcommander.validators.PositiveInteger;
import com.beust.jcommander.Parameter;
import java.util.Iterator;
import java.net.Socket;
import org.apache.logging.log4j.message.EntryMessage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.util.BasicCommandLineArguments;
import java.util.List;
import java.util.Collections;
import java.net.InetAddress;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentMap;
import java.io.InputStream;

public class TcpSocketServer<T extends InputStream> extends AbstractSocketServer<T>
{
    private final ConcurrentMap<Long, SocketHandler> handlers;
    private final ServerSocket serverSocket;
    
    public static TcpSocketServer<InputStream> createJsonSocketServer(final int port) throws IOException {
        TcpSocketServer.LOGGER.entry(new Object[] { "createJsonSocketServer", port });
        final TcpSocketServer<InputStream> socketServer = new TcpSocketServer<InputStream>(port, new JsonInputStreamLogEventBridge());
        return (TcpSocketServer<InputStream>)TcpSocketServer.LOGGER.exit((Object)socketServer);
    }
    
    public static TcpSocketServer<ObjectInputStream> createSerializedSocketServer(final int port) throws IOException {
        TcpSocketServer.LOGGER.entry(new Object[] { port });
        final TcpSocketServer<ObjectInputStream> socketServer = new TcpSocketServer<ObjectInputStream>(port, new ObjectInputStreamLogEventBridge());
        return (TcpSocketServer<ObjectInputStream>)TcpSocketServer.LOGGER.exit((Object)socketServer);
    }
    
    public static TcpSocketServer<ObjectInputStream> createSerializedSocketServer(final int port, final int backlog, final InetAddress localBindAddress) throws IOException {
        return createSerializedSocketServer(port, backlog, localBindAddress, Collections.emptyList());
    }
    
    public static TcpSocketServer<ObjectInputStream> createSerializedSocketServer(final int port, final int backlog, final InetAddress localBindAddress, final List<String> allowedClasses) throws IOException {
        TcpSocketServer.LOGGER.entry(new Object[] { port });
        final TcpSocketServer<ObjectInputStream> socketServer = new TcpSocketServer<ObjectInputStream>(port, backlog, localBindAddress, new ObjectInputStreamLogEventBridge(allowedClasses));
        return (TcpSocketServer<ObjectInputStream>)TcpSocketServer.LOGGER.exit((Object)socketServer);
    }
    
    public static TcpSocketServer<InputStream> createXmlSocketServer(final int port) throws IOException {
        TcpSocketServer.LOGGER.entry(new Object[] { port });
        final TcpSocketServer<InputStream> socketServer = new TcpSocketServer<InputStream>(port, new XmlInputStreamLogEventBridge());
        return (TcpSocketServer<InputStream>)TcpSocketServer.LOGGER.exit((Object)socketServer);
    }
    
    public static void main(final String[] args) throws Exception {
        final CommandLineArguments cla = BasicCommandLineArguments.parseCommandLine(args, TcpSocketServer.class, new CommandLineArguments());
        if (cla.isHelp()) {
            return;
        }
        if (cla.getConfigLocation() != null) {
            ConfigurationFactory.setConfigurationFactory(new ServerConfigurationFactory(cla.getConfigLocation()));
        }
        final TcpSocketServer<ObjectInputStream> socketServer = createSerializedSocketServer(cla.getPort(), cla.getBacklog(), cla.getLocalBindAddress(), cla.getAllowedClasses());
        final Thread serverThread = socketServer.startNewThread();
        if (cla.isInteractive()) {
            socketServer.awaitTermination(serverThread);
        }
    }
    
    public TcpSocketServer(final int port, final int backlog, final InetAddress localBindAddress, final LogEventBridge<T> logEventInput) throws IOException {
        this(port, logEventInput, new ServerSocket(port, backlog, localBindAddress));
    }
    
    public TcpSocketServer(final int port, final LogEventBridge<T> logEventInput) throws IOException {
        this(port, logEventInput, extracted(port));
    }
    
    private static ServerSocket extracted(final int port) throws IOException {
        return new ServerSocket(port);
    }
    
    public TcpSocketServer(final int port, final LogEventBridge<T> logEventInput, final ServerSocket serverSocket) throws IOException {
        super(port, logEventInput);
        this.handlers = new ConcurrentHashMap<Long, SocketHandler>();
        this.serverSocket = serverSocket;
    }
    
    @Override
    public void run() {
        final EntryMessage entry = this.logger.traceEntry();
        while (this.isActive()) {
            if (this.serverSocket.isClosed()) {
                return;
            }
            try {
                this.logger.debug("Listening for a connection {}...", (Object)this.serverSocket);
                final Socket clientSocket = this.serverSocket.accept();
                this.logger.debug("Acepted connection on {}...", (Object)this.serverSocket);
                this.logger.debug("Socket accepted: {}", (Object)clientSocket);
                clientSocket.setSoLinger(true, 0);
                final SocketHandler handler = new SocketHandler(clientSocket);
                this.handlers.put(handler.getId(), handler);
                handler.start();
            }
            catch (IOException e) {
                if (this.serverSocket.isClosed()) {
                    this.logger.traceExit(entry);
                    return;
                }
                this.logger.error("Exception encountered on accept. Ignoring. Stack trace :", (Throwable)e);
            }
        }
        for (final Map.Entry<Long, SocketHandler> handlerEntry : this.handlers.entrySet()) {
            final SocketHandler handler2 = handlerEntry.getValue();
            handler2.shutdown();
            try {
                handler2.join();
            }
            catch (InterruptedException ex) {}
        }
        this.logger.traceExit(entry);
    }
    
    @Override
    public void shutdown() throws IOException {
        final EntryMessage entry = this.logger.traceEntry();
        this.setActive(false);
        Thread.currentThread().interrupt();
        this.serverSocket.close();
        this.logger.traceExit(entry);
    }
    
    protected static class CommandLineArguments extends AbstractSocketServer.CommandLineArguments
    {
        @Parameter(names = { "--backlog", "-b" }, validateWith = PositiveInteger.class, description = "Server socket backlog.")
        private int backlog;
        
        protected CommandLineArguments() {
            this.backlog = 50;
        }
        
        int getBacklog() {
            return this.backlog;
        }
        
        void setBacklog(final int backlog) {
            this.backlog = backlog;
        }
    }
    
    private class SocketHandler extends Log4jThread
    {
        private final T inputStream;
        private volatile boolean shutdown;
        
        public SocketHandler(final Socket socket) throws IOException {
            this.shutdown = false;
            this.inputStream = (T)TcpSocketServer.this.logEventInput.wrapStream(socket.getInputStream());
        }
        
        @Override
        public void run() {
            final EntryMessage entry = TcpSocketServer.this.logger.traceEntry();
            boolean closed = false;
            try {
                try {
                    while (!this.shutdown) {
                        TcpSocketServer.this.logEventInput.logEvents((T)this.inputStream, TcpSocketServer.this);
                    }
                }
                catch (EOFException e3) {
                    closed = true;
                }
                catch (OptionalDataException e) {
                    TcpSocketServer.this.logger.error("OptionalDataException eof=" + e.eof + " length=" + e.length, (Throwable)e);
                }
                catch (IOException e2) {
                    TcpSocketServer.this.logger.error("IOException encountered while reading from socket", (Throwable)e2);
                }
                if (!closed) {
                    Closer.closeSilently(this.inputStream);
                }
            }
            finally {
                TcpSocketServer.this.handlers.remove(this.getId());
            }
            TcpSocketServer.this.logger.traceExit(entry);
        }
        
        public void shutdown() {
            this.shutdown = true;
            this.interrupt();
        }
    }
}
