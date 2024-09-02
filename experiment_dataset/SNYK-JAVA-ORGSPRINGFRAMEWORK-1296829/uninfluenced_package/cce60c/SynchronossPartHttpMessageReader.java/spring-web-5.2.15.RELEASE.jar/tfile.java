// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.http.codec.multipart;

import java.nio.file.StandardOpenOption;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.Channels;
import java.nio.file.OpenOption;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.codec.DecodingException;
import org.synchronoss.cloud.nio.multipart.MultipartUtils;
import org.synchronoss.cloud.nio.stream.storage.StreamStorage;
import org.synchronoss.cloud.nio.multipart.DefaultPartBodyStreamStorageFactory;
import reactor.core.publisher.SignalType;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import reactor.core.CoreSubscriber;
import org.synchronoss.cloud.nio.multipart.PartBodyStreamStorageFactory;
import org.synchronoss.cloud.nio.multipart.Multipart;
import org.synchronoss.cloud.nio.multipart.MultipartContext;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.nio.charset.Charset;
import org.springframework.util.Assert;
import org.synchronoss.cloud.nio.multipart.NioMultipartParser;
import org.synchronoss.cloud.nio.multipart.NioMultipartParserListener;
import reactor.core.publisher.FluxSink;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.BaseSubscriber;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import reactor.core.publisher.Mono;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.core.codec.Hints;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import java.util.Map;
import org.springframework.http.ReactiveHttpInputMessage;
import java.util.Iterator;
import org.springframework.lang.Nullable;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import java.util.List;
import java.nio.file.Path;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.LoggingCodecSupport;

public class SynchronossPartHttpMessageReader extends LoggingCodecSupport implements HttpMessageReader<Part>
{
    private static final DataBufferFactory bufferFactory;
    private static final String FILE_STORAGE_DIRECTORY_PREFIX = "synchronoss-file-upload-";
    private int maxInMemorySize;
    private long maxDiskUsagePerPart;
    private int maxParts;
    private Path fileStorageDirectory;
    
    public SynchronossPartHttpMessageReader() {
        this.maxInMemorySize = 262144;
        this.maxDiskUsagePerPart = -1L;
        this.maxParts = -1;
        this.fileStorageDirectory = createTempDirectory();
    }
    
    public void setMaxInMemorySize(final int byteCount) {
        this.maxInMemorySize = byteCount;
    }
    
    public int getMaxInMemorySize() {
        return this.maxInMemorySize;
    }
    
    public void setMaxDiskUsagePerPart(final long maxDiskUsagePerPart) {
        this.maxDiskUsagePerPart = maxDiskUsagePerPart;
    }
    
    public long getMaxDiskUsagePerPart() {
        return this.maxDiskUsagePerPart;
    }
    
    public void setMaxParts(final int maxParts) {
        this.maxParts = maxParts;
    }
    
    public int getMaxParts() {
        return this.maxParts;
    }
    
    @Override
    public List<MediaType> getReadableMediaTypes() {
        return MultipartHttpMessageReader.MIME_TYPES;
    }
    
    @Override
    public boolean canRead(final ResolvableType elementType, @Nullable final MediaType mediaType) {
        if (Part.class.equals(elementType.toClass())) {
            if (mediaType == null) {
                return true;
            }
            for (final MediaType supportedMediaType : this.getReadableMediaTypes()) {
                if (supportedMediaType.isCompatibleWith(mediaType)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public Flux<Part> read(final ResolvableType elementType, final ReactiveHttpInputMessage message, final Map<String, Object> hints) {
        String s;
        final StringBuilder sb;
        return (Flux<Part>)Flux.create((Consumer)new SynchronossPartGenerator(message, this.fileStorageDirectory)).doOnNext(part -> {
            if (!Hints.isLoggingSuppressed((Map)hints)) {
                LogFormatUtils.traceDebug(this.logger, traceOn -> {
                    new StringBuilder().append(Hints.getLogPrefix((Map)hints)).append("Parsed ");
                    if (this.isEnableLoggingRequestDetails()) {
                        s = LogFormatUtils.formatValue((Object)part, !traceOn);
                    }
                    else {
                        s = "parts '" + part.name() + "' (content masked)";
                    }
                    return sb.append(s).toString();
                });
            }
        });
    }
    
    @Override
    public Mono<Part> readMono(final ResolvableType elementType, final ReactiveHttpInputMessage message, final Map<String, Object> hints) {
        return (Mono<Part>)Mono.error((Throwable)new UnsupportedOperationException("Cannot read multipart request body into single Part"));
    }
    
    private static Path createTempDirectory() {
        try {
            return Files.createTempDirectory("synchronoss-file-upload-", (FileAttribute<?>[])new FileAttribute[0]);
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
    
    static {
        bufferFactory = (DataBufferFactory)new DefaultDataBufferFactory();
    }
    
    private class SynchronossPartGenerator extends BaseSubscriber<DataBuffer> implements Consumer<FluxSink<Part>>
    {
        private final ReactiveHttpInputMessage inputMessage;
        private final LimitedPartBodyStreamStorageFactory storageFactory;
        private final Path fileStorageDirectory;
        @Nullable
        private NioMultipartParserListener listener;
        @Nullable
        private NioMultipartParser parser;
        
        public SynchronossPartGenerator(final ReactiveHttpInputMessage inputMessage, final Path fileStorageDirectory) {
            this.storageFactory = new LimitedPartBodyStreamStorageFactory();
            this.inputMessage = inputMessage;
            this.fileStorageDirectory = fileStorageDirectory;
        }
        
        public void accept(final FluxSink<Part> sink) {
            final HttpHeaders headers = this.inputMessage.getHeaders();
            final MediaType mediaType = headers.getContentType();
            Assert.state(mediaType != null, "No content type set");
            final int length = this.getContentLength(headers);
            final Charset charset = Optional.ofNullable(mediaType.getCharset()).orElse(StandardCharsets.UTF_8);
            final MultipartContext context = new MultipartContext(mediaType.toString(), length, charset.name());
            this.listener = (NioMultipartParserListener)new FluxSinkAdapterListener(sink, context, this.storageFactory);
            this.parser = Multipart.multipart(context).saveTemporaryFilesTo(this.fileStorageDirectory.toString()).usePartBodyStreamStorageFactory((PartBodyStreamStorageFactory)this.storageFactory).forNIO(this.listener);
            this.inputMessage.getBody().subscribe((CoreSubscriber)this);
        }
        
        protected void hookOnNext(final DataBuffer buffer) {
            Assert.state(this.parser != null && this.listener != null, "Not initialized yet");
            final int size = buffer.readableByteCount();
            this.storageFactory.increaseByteCount(size);
            final byte[] resultBytes = new byte[size];
            buffer.read(resultBytes);
            try {
                this.parser.write(resultBytes);
            }
            catch (IOException ex) {
                this.cancel();
                final int index = this.storageFactory.getCurrentPartIndex();
                this.listener.onError("Parser error for part [" + index + "]", (Throwable)ex);
            }
            finally {
                DataBufferUtils.release(buffer);
            }
        }
        
        protected void hookOnError(final Throwable ex) {
            if (this.listener != null) {
                final int index = this.storageFactory.getCurrentPartIndex();
                this.listener.onError("Failure while parsing part[" + index + "]", ex);
            }
        }
        
        protected void hookOnComplete() {
            if (this.listener != null) {
                this.listener.onAllPartsFinished();
            }
        }
        
        protected void hookFinally(final SignalType type) {
            try {
                if (this.parser != null) {
                    this.parser.close();
                }
            }
            catch (IOException ex) {}
        }
        
        private int getContentLength(final HttpHeaders headers) {
            final long length = headers.getContentLength();
            return ((int)length == length) ? ((int)length) : -1;
        }
    }
    
    private class LimitedPartBodyStreamStorageFactory implements PartBodyStreamStorageFactory
    {
        private final PartBodyStreamStorageFactory storageFactory;
        private int index;
        private boolean isFilePart;
        private long partSize;
        
        private LimitedPartBodyStreamStorageFactory() {
            this.storageFactory = (PartBodyStreamStorageFactory)((SynchronossPartHttpMessageReader.this.maxInMemorySize > 0) ? new DefaultPartBodyStreamStorageFactory(SynchronossPartHttpMessageReader.this.maxInMemorySize) : new DefaultPartBodyStreamStorageFactory());
            this.index = 1;
        }
        
        public int getCurrentPartIndex() {
            return this.index;
        }
        
        public StreamStorage newStreamStorageForPartBody(final Map<String, List<String>> headers, final int index) {
            this.index = index;
            this.isFilePart = (MultipartUtils.getFileName((Map)headers) != null);
            this.partSize = 0L;
            if (SynchronossPartHttpMessageReader.this.maxParts > 0 && index > SynchronossPartHttpMessageReader.this.maxParts) {
                throw new DecodingException("Too many parts (" + index + " allowed)");
            }
            return this.storageFactory.newStreamStorageForPartBody((Map)headers, index);
        }
        
        public void increaseByteCount(final long byteCount) {
            this.partSize += byteCount;
            if (SynchronossPartHttpMessageReader.this.maxInMemorySize > 0 && !this.isFilePart && this.partSize >= SynchronossPartHttpMessageReader.this.maxInMemorySize) {
                throw new DataBufferLimitException("Part[" + this.index + "] exceeded the in-memory limit of " + SynchronossPartHttpMessageReader.this.maxInMemorySize + " bytes");
            }
            if (SynchronossPartHttpMessageReader.this.maxDiskUsagePerPart > 0L && this.isFilePart && this.partSize > SynchronossPartHttpMessageReader.this.maxDiskUsagePerPart) {
                throw new DecodingException("Part[" + this.index + "] exceeded the disk usage limit of " + SynchronossPartHttpMessageReader.this.maxDiskUsagePerPart + " bytes");
            }
        }
        
        public void partFinished() {
            ++this.index;
            this.isFilePart = false;
            this.partSize = 0L;
        }
    }
    
    private static class FluxSinkAdapterListener implements NioMultipartParserListener
    {
        private final FluxSink<Part> sink;
        private final MultipartContext context;
        private final LimitedPartBodyStreamStorageFactory storageFactory;
        private final AtomicInteger terminated;
        
        FluxSinkAdapterListener(final FluxSink<Part> sink, final MultipartContext context, final LimitedPartBodyStreamStorageFactory factory) {
            this.terminated = new AtomicInteger(0);
            this.sink = sink;
            this.context = context;
            this.storageFactory = factory;
        }
        
        public void onPartFinished(final StreamStorage storage, final Map<String, List<String>> headers) {
            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.putAll(headers);
            this.storageFactory.partFinished();
            this.sink.next((Object)this.createPart(storage, httpHeaders));
        }
        
        private Part createPart(final StreamStorage storage, final HttpHeaders httpHeaders) {
            final String filename = MultipartUtils.getFileName((Map)httpHeaders);
            if (filename != null) {
                return new SynchronossFilePart(httpHeaders, filename, storage);
            }
            if (MultipartUtils.isFormField((Map)httpHeaders, this.context)) {
                final String value = MultipartUtils.readFormParameterValue(storage, (Map)httpHeaders);
                return new SynchronossFormFieldPart(httpHeaders, value);
            }
            return new SynchronossPart(httpHeaders, storage);
        }
        
        public void onError(final String message, final Throwable cause) {
            if (this.terminated.getAndIncrement() == 0) {
                this.sink.error((Throwable)new DecodingException(message, cause));
            }
        }
        
        public void onAllPartsFinished() {
            if (this.terminated.getAndIncrement() == 0) {
                this.sink.complete();
            }
        }
        
        public void onNestedPartStarted(final Map<String, List<String>> headersFromParentPart) {
        }
        
        public void onNestedPartFinished() {
        }
    }
    
    private abstract static class AbstractSynchronossPart implements Part
    {
        private final String name;
        private final HttpHeaders headers;
        
        AbstractSynchronossPart(final HttpHeaders headers) {
            Assert.notNull((Object)headers, "HttpHeaders is required");
            this.name = MultipartUtils.getFieldName((Map)headers);
            this.headers = headers;
        }
        
        @Override
        public String name() {
            return this.name;
        }
        
        @Override
        public HttpHeaders headers() {
            return this.headers;
        }
        
        @Override
        public String toString() {
            return "Part '" + this.name + "', headers=" + this.headers;
        }
    }
    
    private static class SynchronossPart extends AbstractSynchronossPart
    {
        private final StreamStorage storage;
        
        SynchronossPart(final HttpHeaders headers, final StreamStorage storage) {
            super(headers);
            Assert.notNull((Object)storage, "StreamStorage is required");
            this.storage = storage;
        }
        
        @Override
        public Flux<DataBuffer> content() {
            return (Flux<DataBuffer>)DataBufferUtils.readInputStream((Callable)this.getStorage()::getInputStream, SynchronossPartHttpMessageReader.bufferFactory, 4096);
        }
        
        protected StreamStorage getStorage() {
            return this.storage;
        }
    }
    
    private static class SynchronossFilePart extends SynchronossPart implements FilePart
    {
        private static final OpenOption[] FILE_CHANNEL_OPTIONS;
        private final String filename;
        
        SynchronossFilePart(final HttpHeaders headers, final String filename, final StreamStorage storage) {
            super(headers, storage);
            this.filename = filename;
        }
        
        @Override
        public String filename() {
            return this.filename;
        }
        
        @Override
        public Mono<Void> transferTo(final Path dest) {
            ReadableByteChannel input = null;
            FileChannel output = null;
            try {
                input = Channels.newChannel(this.getStorage().getInputStream());
                output = FileChannel.open(dest, SynchronossFilePart.FILE_CHANNEL_OPTIONS);
                long written;
                for (long size = (input instanceof FileChannel) ? ((FileChannel)input).size() : Long.MAX_VALUE, totalWritten = 0L; totalWritten < size; totalWritten += written) {
                    written = output.transferFrom(input, totalWritten, size - totalWritten);
                    if (written <= 0L) {
                        break;
                    }
                }
            }
            catch (IOException ex) {
                return (Mono<Void>)Mono.error((Throwable)ex);
            }
            finally {
                if (input != null) {
                    try {
                        input.close();
                    }
                    catch (IOException ex2) {}
                }
                if (output != null) {
                    try {
                        output.close();
                    }
                    catch (IOException ex3) {}
                }
            }
            return (Mono<Void>)Mono.empty();
        }
        
        @Override
        public String toString() {
            return "Part '" + this.name() + "', filename='" + this.filename + "'";
        }
        
        static {
            FILE_CHANNEL_OPTIONS = new OpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE };
        }
    }
    
    private static class SynchronossFormFieldPart extends AbstractSynchronossPart implements FormFieldPart
    {
        private final String content;
        
        SynchronossFormFieldPart(final HttpHeaders headers, final String content) {
            super(headers);
            this.content = content;
        }
        
        @Override
        public String value() {
            return this.content;
        }
        
        @Override
        public Flux<DataBuffer> content() {
            final byte[] bytes = this.content.getBytes(this.getCharset());
            return (Flux<DataBuffer>)Flux.just((Object)SynchronossPartHttpMessageReader.bufferFactory.wrap(bytes));
        }
        
        private Charset getCharset() {
            final String name = MultipartUtils.getCharEncoding((Map)this.headers());
            return (name != null) ? Charset.forName(name) : StandardCharsets.UTF_8;
        }
        
        @Override
        public String toString() {
            return "Part '" + this.name() + "=" + this.content + "'";
        }
    }
}
