// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.solr.servlet;

import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileCleaningTracker;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.lucene.util.IOUtils;
import java.io.Closeable;
import org.apache.solr.common.util.FastInputStream;
import java.util.Iterator;
import java.nio.charset.CharacterCodingException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import org.apache.solr.common.params.MultiMapSolrParams;
import org.apache.solr.api.V2HttpCall;
import java.util.Map;
import org.apache.solr.common.util.CommandOperation;
import java.util.List;
import java.security.Principal;
import org.apache.solr.request.SolrQueryRequestBase;
import java.io.File;
import org.apache.solr.common.util.ContentStreamBase;
import java.net.URL;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.RequestHandlers;
import java.util.Collection;
import org.apache.solr.util.tracing.GlobalTracer;
import org.apache.solr.common.util.ContentStream;
import java.util.ArrayList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.core.SolrCore;
import org.apache.solr.util.RTimerTree;
import javax.servlet.http.HttpServletRequest;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.util.SolrFileCleaningTracker;
import java.util.HashMap;
import java.nio.charset.Charset;

public class SolrRequestParsers
{
    public static final String MULTIPART = "multipart";
    public static final String FORMDATA = "formdata";
    public static final String RAW = "raw";
    public static final String SIMPLE = "simple";
    public static final String STANDARD = "standard";
    private static final Charset CHARSET_US_ASCII;
    public static final String INPUT_ENCODING_KEY = "ie";
    private static final byte[] INPUT_ENCODING_BYTES;
    public static final String REQUEST_TIMER_SERVLET_ATTRIBUTE = "org.apache.solr.RequestTimer";
    private final HashMap<String, SolrRequestParser> parsers;
    private final boolean enableRemoteStreams;
    private final boolean enableStreamBody;
    private StandardRequestParser standard;
    private boolean handleSelect;
    private boolean addHttpRequestToContext;
    public static final SolrRequestParsers DEFAULT;
    public static volatile SolrFileCleaningTracker fileCleaningTracker;
    private static final long WS_MASK = 140776143070721L;
    
    public SolrRequestParsers(final SolrConfig globalConfig) {
        this.parsers = new HashMap<String, SolrRequestParser>();
        this.handleSelect = true;
        int multipartUploadLimitKB;
        int formUploadLimitKB;
        if (globalConfig == null) {
            formUploadLimitKB = (multipartUploadLimitKB = Integer.MAX_VALUE);
            this.enableRemoteStreams = false;
            this.enableStreamBody = false;
            this.handleSelect = false;
            this.addHttpRequestToContext = false;
        }
        else {
            multipartUploadLimitKB = globalConfig.getMultipartUploadLimitKB();
            formUploadLimitKB = globalConfig.getFormUploadLimitKB();
            this.enableRemoteStreams = globalConfig.isEnableRemoteStreams();
            this.enableStreamBody = globalConfig.isEnableStreamBody();
            this.handleSelect = globalConfig.isHandleSelect();
            this.addHttpRequestToContext = globalConfig.isAddHttpRequestToContext();
        }
        this.init(multipartUploadLimitKB, formUploadLimitKB);
    }
    
    private SolrRequestParsers() {
        this.parsers = new HashMap<String, SolrRequestParser>();
        this.handleSelect = true;
        this.enableRemoteStreams = false;
        this.enableStreamBody = false;
        this.handleSelect = false;
        this.addHttpRequestToContext = false;
        this.init(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }
    
    private void init(final int multipartUploadLimitKB, final int formUploadLimitKB) {
        final MultipartRequestParser multi = new MultipartRequestParser(multipartUploadLimitKB);
        final RawRequestParser raw = new RawRequestParser();
        final FormDataRequestParser formdata = new FormDataRequestParser(formUploadLimitKB);
        this.standard = new StandardRequestParser(multi, raw, formdata);
        this.parsers.put("multipart", multi);
        this.parsers.put("formdata", formdata);
        this.parsers.put("raw", raw);
        this.parsers.put("simple", new SimpleRequestParser());
        this.parsers.put("standard", this.standard);
        this.parsers.put("", this.standard);
    }
    
    private static RTimerTree getRequestTimer(final HttpServletRequest req) {
        final Object reqTimer = req.getAttribute("org.apache.solr.RequestTimer");
        if (reqTimer != null && reqTimer instanceof RTimerTree) {
            return (RTimerTree)reqTimer;
        }
        return new RTimerTree();
    }
    
    public SolrQueryRequest parse(final SolrCore core, final String path, final HttpServletRequest req) throws Exception {
        final SolrRequestParser parser = this.standard;
        final ArrayList<ContentStream> streams = new ArrayList<ContentStream>(1);
        final SolrParams params = parser.parseParamsAndFillStreams(req, streams);
        if (GlobalTracer.get().tracing()) {
            GlobalTracer.get();
            GlobalTracer.getTracer().activeSpan().setTag("params", params.toString());
        }
        final SolrQueryRequest sreq = this.buildRequestFrom(core, params, streams, getRequestTimer(req), req);
        sreq.getContext().put("path", RequestHandlers.normalize(path));
        sreq.getContext().put("httpMethod", req.getMethod());
        if (this.addHttpRequestToContext) {
            sreq.getContext().put("httpRequest", req);
        }
        return sreq;
    }
    
    public SolrQueryRequest buildRequestFrom(final SolrCore core, final SolrParams params, final Collection<ContentStream> streams) throws Exception {
        return this.buildRequestFrom(core, params, streams, new RTimerTree(), null);
    }
    
    private SolrQueryRequest buildRequestFrom(final SolrCore core, final SolrParams params, final Collection<ContentStream> streams, final RTimerTree requestTimer, final HttpServletRequest req) throws Exception {
        final String contentType = params.get("stream.contentType");
        String[] strs = params.getParams("stream.url");
        if (strs != null) {
            if (!this.enableRemoteStreams) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Remote Streaming is disabled.");
            }
            for (final String url : strs) {
                final ContentStreamBase stream = (ContentStreamBase)new ContentStreamBase.URLStream(new URL(url));
                if (contentType != null) {
                    stream.setContentType(contentType);
                }
                streams.add((ContentStream)stream);
            }
        }
        strs = params.getParams("stream.file");
        if (strs != null) {
            if (!this.enableRemoteStreams) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Remote Streaming is disabled. See http://lucene.apache.org/solr/guide/requestdispatcher-in-solrconfig.html for help");
            }
            for (final String file : strs) {
                final ContentStreamBase stream = (ContentStreamBase)new ContentStreamBase.FileStream(new File(file));
                if (contentType != null) {
                    stream.setContentType(contentType);
                }
                streams.add((ContentStream)stream);
            }
        }
        strs = params.getParams("stream.body");
        if (strs != null) {
            if (!this.enableStreamBody) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Stream Body is disabled. See http://lucene.apache.org/solr/guide/requestdispatcher-in-solrconfig.html for help");
            }
            for (final String body : strs) {
                final ContentStreamBase stream = (ContentStreamBase)new ContentStreamBase.StringStream(body);
                if (contentType != null) {
                    stream.setContentType(contentType);
                }
                streams.add((ContentStream)stream);
            }
        }
        final HttpSolrCall httpSolrCall = (req == null) ? null : ((HttpSolrCall)req.getAttribute(HttpSolrCall.class.getName()));
        final SolrQueryRequestBase q = new SolrQueryRequestBase(core, params, requestTimer) {
            @Override
            public Principal getUserPrincipal() {
                return (req == null) ? null : req.getUserPrincipal();
            }
            
            @Override
            public List<CommandOperation> getCommands(final boolean validateInput) {
                if (httpSolrCall != null) {
                    return httpSolrCall.getCommands(validateInput);
                }
                return super.getCommands(validateInput);
            }
            
            @Override
            public Map<String, String> getPathTemplateValues() {
                if (httpSolrCall != null && httpSolrCall instanceof V2HttpCall) {
                    return ((V2HttpCall)httpSolrCall).getUrlParts();
                }
                return super.getPathTemplateValues();
            }
            
            @Override
            public HttpSolrCall getHttpSolrCall() {
                return httpSolrCall;
            }
        };
        if (streams != null && streams.size() > 0) {
            q.setContentStreams(streams);
        }
        return q;
    }
    
    private static HttpSolrCall getHttpSolrCall(final HttpServletRequest req) {
        return (req == null) ? null : ((HttpSolrCall)req.getAttribute(HttpSolrCall.class.getName()));
    }
    
    public static MultiMapSolrParams parseQueryString(final String queryString) {
        final Map<String, String[]> map = new HashMap<String, String[]>();
        parseQueryString(queryString, map);
        return new MultiMapSolrParams((Map)map);
    }
    
    static void parseQueryString(final String queryString, final Map<String, String[]> map) {
        if (queryString != null && queryString.length() > 0) {
            try {
                final int len = queryString.length();
                final InputStream in = new InputStream() {
                    int pos = 0;
                    
                    @Override
                    public int read() {
                        if (this.pos >= len) {
                            return -1;
                        }
                        final char ch = queryString.charAt(this.pos);
                        if (ch > '\u007f') {
                            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "URLDecoder: The query string contains a not-%-escaped byte > 127 at position " + this.pos);
                        }
                        ++this.pos;
                        return ch;
                    }
                };
                parseFormDataContent(in, Long.MAX_VALUE, StandardCharsets.UTF_8, map, true);
            }
            catch (IOException ioe) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, (Throwable)ioe);
            }
        }
    }
    
    static long parseFormDataContent(final InputStream postContent, final long maxLen, Charset charset, final Map<String, String[]> map, final boolean supportCharsetParam) throws IOException {
        CharsetDecoder charsetDecoder = supportCharsetParam ? null : getCharsetDecoder(charset);
        final LinkedList<Object> buffer = supportCharsetParam ? new LinkedList<Object>() : null;
        long len = 0L;
        long keyPos = 0L;
        long valuePos = 0L;
        final ByteArrayOutputStream keyStream = new ByteArrayOutputStream();
        final ByteArrayOutputStream valueStream = new ByteArrayOutputStream();
        ByteArrayOutputStream currentStream = keyStream;
        while (true) {
            int b = postContent.read();
            Label_0438: {
                switch (b) {
                    case -1:
                    case 38: {
                        if (keyStream.size() > 0) {
                            final byte[] keyBytes = keyStream.toByteArray();
                            final byte[] valueBytes = valueStream.toByteArray();
                            if (Arrays.equals(keyBytes, SolrRequestParsers.INPUT_ENCODING_BYTES)) {
                                if (charsetDecoder != null) {
                                    throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, supportCharsetParam ? "Query string invalid: duplicate 'ie' (input encoding) key." : "Key 'ie' (input encoding) cannot be used in POSTed application/x-www-form-urlencoded form data. To set the input encoding of POSTed form data, use the 'Content-Type' header and provide a charset!");
                                }
                                charset = Charset.forName(decodeChars(valueBytes, keyPos, getCharsetDecoder(SolrRequestParsers.CHARSET_US_ASCII)));
                                charsetDecoder = getCharsetDecoder(charset);
                                decodeBuffer(buffer, map, charsetDecoder);
                            }
                            else if (charsetDecoder == null) {
                                buffer.add(keyBytes);
                                buffer.add(keyPos);
                                buffer.add(valueBytes);
                                buffer.add(valuePos);
                            }
                            else {
                                final String key = decodeChars(keyBytes, keyPos, charsetDecoder);
                                final String value = decodeChars(valueBytes, valuePos, charsetDecoder);
                                MultiMapSolrParams.addParam(key.trim(), value, (Map)map);
                            }
                        }
                        else if (valueStream.size() > 0) {
                            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "application/x-www-form-urlencoded invalid: missing key");
                        }
                        keyStream.reset();
                        valueStream.reset();
                        valuePos = (keyPos = len + 1L);
                        currentStream = keyStream;
                        break Label_0438;
                    }
                    case 43: {
                        currentStream.write(32);
                        break Label_0438;
                    }
                    case 37: {
                        final int upper = digit16(b = postContent.read());
                        ++len;
                        final int lower = digit16(b = postContent.read());
                        ++len;
                        currentStream.write((upper << 4) + lower);
                        break Label_0438;
                    }
                    case 61: {
                        if (currentStream == keyStream) {
                            valuePos = len + 1L;
                            currentStream = valueStream;
                            break Label_0438;
                        }
                        break;
                    }
                }
                currentStream.write(b);
            }
            if (b == -1) {
                if (buffer != null && !buffer.isEmpty()) {
                    assert charsetDecoder == null;
                    decodeBuffer(buffer, map, getCharsetDecoder(charset));
                }
                return len;
            }
            ++len;
            if (len > maxLen) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "application/x-www-form-urlencoded content exceeds upload limit of " + maxLen / 1024L + " KB");
            }
        }
    }
    
    private static CharsetDecoder getCharsetDecoder(final Charset charset) {
        return charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
    }
    
    private static String decodeChars(final byte[] bytes, final long position, final CharsetDecoder charsetDecoder) {
        try {
            return charsetDecoder.decode(ByteBuffer.wrap(bytes)).toString();
        }
        catch (CharacterCodingException cce) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "URLDecoder: Invalid character encoding detected after position " + position + " of query string / form data (while parsing as " + charsetDecoder.charset().name() + ")");
        }
    }
    
    private static void decodeBuffer(final LinkedList<Object> input, final Map<String, String[]> map, final CharsetDecoder charsetDecoder) {
        final Iterator<Object> it = input.iterator();
        while (it.hasNext()) {
            final byte[] keyBytes = it.next();
            it.remove();
            final Long keyPos = it.next();
            it.remove();
            final byte[] valueBytes = it.next();
            it.remove();
            final Long valuePos = it.next();
            it.remove();
            MultiMapSolrParams.addParam(decodeChars(keyBytes, keyPos, charsetDecoder).trim(), decodeChars(valueBytes, valuePos, charsetDecoder), (Map)map);
        }
    }
    
    private static int digit16(final int b) {
        if (b == -1) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "URLDecoder: Incomplete trailing escape (%) pattern");
        }
        if (b >= 48 && b <= 57) {
            return b - 48;
        }
        if (b >= 65 && b <= 70) {
            return b - 55;
        }
        if (b >= 97 && b <= 102) {
            return b - 87;
        }
        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "URLDecoder: Invalid digit (" + (char)b + ") in escape (%) pattern");
    }
    
    public boolean isHandleSelect() {
        return this.handleSelect;
    }
    
    public void setHandleSelect(final boolean handleSelect) {
        this.handleSelect = handleSelect;
    }
    
    public boolean isAddRequestHeadersToContext() {
        return this.addHttpRequestToContext;
    }
    
    public void setAddRequestHeadersToContext(final boolean addRequestHeadersToContext) {
        this.addHttpRequestToContext = addRequestHeadersToContext;
    }
    
    private static SolrParams autodetect(final HttpServletRequest req, final ArrayList<ContentStream> streams, final FastInputStream in) throws IOException {
        String detectedContentType = null;
        boolean shouldClose = true;
        try {
            in.peek();
            final byte[] arr = in.getBuffer();
            final int pos = in.getPositionInBuffer();
            final int end = in.getEndInBuffer();
            int i = pos;
            while (i < end - 1) {
                final int ch = arr[i];
                final boolean isWhitespace = (140776143070721L >> ch & 0x1L) != 0x0L && (ch <= 32 || ch == 160);
                if (!isWhitespace) {
                    if (ch == 35 || (ch == 47 && (arr[i + 1] == 47 || arr[i + 1] == 42)) || ch == 123 || ch == 91) {
                        detectedContentType = "application/json";
                    }
                    if (ch == 60) {
                        detectedContentType = "text/xml";
                        break;
                    }
                    break;
                }
                else {
                    ++i;
                }
            }
            if (detectedContentType == null) {
                shouldClose = false;
                return null;
            }
            Long size = null;
            final String v = req.getHeader("Content-Length");
            if (v != null) {
                size = Long.valueOf(v);
            }
            streams.add((ContentStream)new InputStreamContentStream((InputStream)in, detectedContentType, size));
            final Map<String, String[]> map = new HashMap<String, String[]>();
            final String qs = req.getQueryString();
            if (qs != null) {
                parseQueryString(qs, map);
            }
            return (SolrParams)new MultiMapSolrParams((Map)map);
        }
        catch (IOException ioe) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, (Throwable)ioe);
        }
        catch (IllegalStateException ise) {
            throw (SolrException)FormDataRequestParser.getParameterIncompatibilityException().initCause((Throwable)ise);
        }
        finally {
            if (shouldClose) {
                IOUtils.closeWhileHandlingException(new Closeable[] { (Closeable)in });
            }
        }
    }
    
    static {
        CHARSET_US_ASCII = Charset.forName("US-ASCII");
        INPUT_ENCODING_BYTES = "ie".getBytes(SolrRequestParsers.CHARSET_US_ASCII);
        DEFAULT = new SolrRequestParsers();
    }
    
    static class SimpleRequestParser implements SolrRequestParser
    {
        @Override
        public SolrParams parseParamsAndFillStreams(final HttpServletRequest req, final ArrayList<ContentStream> streams) throws Exception {
            return (SolrParams)SolrRequestParsers.parseQueryString(req.getQueryString());
        }
    }
    
    static class HttpRequestContentStream extends ContentStreamBase
    {
        private final HttpServletRequest req;
        
        public HttpRequestContentStream(final HttpServletRequest req) {
            this.req = req;
            this.contentType = req.getContentType();
            final String v = req.getHeader("Content-Length");
            if (v != null) {
                this.size = Long.valueOf(v);
            }
        }
        
        public InputStream getStream() throws IOException {
            return (InputStream)new CloseShieldInputStream((InputStream)this.req.getInputStream());
        }
    }
    
    static class FileItemContentStream extends ContentStreamBase
    {
        private final FileItem item;
        
        public FileItemContentStream(final FileItem f) {
            this.item = f;
            this.contentType = this.item.getContentType();
            this.name = this.item.getName();
            this.sourceInfo = this.item.getFieldName();
            this.size = this.item.getSize();
        }
        
        public InputStream getStream() throws IOException {
            return this.item.getInputStream();
        }
    }
    
    static class RawRequestParser implements SolrRequestParser
    {
        @Override
        public SolrParams parseParamsAndFillStreams(final HttpServletRequest req, final ArrayList<ContentStream> streams) throws Exception {
            streams.add((ContentStream)new HttpRequestContentStream(req));
            return (SolrParams)SolrRequestParsers.parseQueryString(req.getQueryString());
        }
    }
    
    static class MultipartRequestParser implements SolrRequestParser
    {
        private final int uploadLimitKB;
        private DiskFileItemFactory factory;
        
        public MultipartRequestParser(final int limit) {
            this.factory = new DiskFileItemFactory();
            this.uploadLimitKB = limit;
            final FileCleaningTracker fct = SolrRequestParsers.fileCleaningTracker;
            if (fct != null) {
                this.factory.setFileCleaningTracker((FileCleaningTracker)SolrRequestParsers.fileCleaningTracker);
            }
        }
        
        @Override
        public SolrParams parseParamsAndFillStreams(final HttpServletRequest req, final ArrayList<ContentStream> streams) throws Exception {
            if (!ServletFileUpload.isMultipartContent(req)) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Not multipart content! " + req.getContentType());
            }
            final MultiMapSolrParams params = SolrRequestParsers.parseQueryString(req.getQueryString());
            final ServletFileUpload upload = new ServletFileUpload((FileItemFactory)this.factory);
            upload.setSizeMax(this.uploadLimitKB * 1024L);
            final List<FileItem> items = (List<FileItem>)upload.parseRequest(req);
            for (final FileItem item : items) {
                if (item.isFormField()) {
                    MultiMapSolrParams.addParam(item.getFieldName().trim(), item.getString(), params.getMap());
                }
                else {
                    streams.add((ContentStream)new FileItemContentStream(item));
                }
            }
            return (SolrParams)params;
        }
    }
    
    static class FormDataRequestParser implements SolrRequestParser
    {
        private static final long WS_MASK = 140776143070721L;
        private final int uploadLimitKB;
        
        public FormDataRequestParser(final int limit) {
            this.uploadLimitKB = limit;
        }
        
        public SolrParams parseParamsAndFillStreams(final HttpServletRequest req, final ArrayList<ContentStream> streams, InputStream in) throws Exception {
            final Map<String, String[]> map = new HashMap<String, String[]>();
            final String qs = req.getQueryString();
            if (qs != null) {
                SolrRequestParsers.parseQueryString(qs, map);
            }
            final long totalLength = req.getContentLength();
            final long maxLength = this.uploadLimitKB * 1024L;
            if (totalLength > maxLength) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "application/x-www-form-urlencoded content length (" + totalLength + " bytes) exceeds upload limit of " + this.uploadLimitKB + " KB");
            }
            final String cs = ContentStreamBase.getCharsetFromContentType(req.getContentType());
            final Charset charset = (cs == null) ? StandardCharsets.UTF_8 : Charset.forName(cs);
            try {
                in = (InputStream)FastInputStream.wrap((InputStream)((in == null) ? new CloseShieldInputStream((InputStream)req.getInputStream()) : in));
                final long bytesRead = SolrRequestParsers.parseFormDataContent(in, maxLength, charset, map, false);
                if (bytesRead == 0L && totalLength > 0L) {
                    throw getParameterIncompatibilityException();
                }
                return (SolrParams)new MultiMapSolrParams((Map)map);
            }
            catch (IOException ioe) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, (Throwable)ioe);
            }
            catch (IllegalStateException ise) {
                throw (SolrException)getParameterIncompatibilityException().initCause((Throwable)ise);
            }
            finally {
                IOUtils.closeWhileHandlingException(new Closeable[] { in });
            }
            return (SolrParams)new MultiMapSolrParams((Map)map);
        }
        
        @Override
        public SolrParams parseParamsAndFillStreams(final HttpServletRequest req, final ArrayList<ContentStream> streams) throws Exception {
            if (!this.isFormData(req)) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Not application/x-www-form-urlencoded content: " + req.getContentType());
            }
            return this.parseParamsAndFillStreams(req, streams, null);
        }
        
        public static SolrException getParameterIncompatibilityException() {
            return new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Solr requires that request parameters sent using application/x-www-form-urlencoded content-type can be read through the request input stream. Unfortunately, the stream was empty / not available. This may be caused by another servlet filter calling ServletRequest.getParameter*() before SolrDispatchFilter, please remove it.");
        }
        
        public boolean isFormData(final HttpServletRequest req) {
            String contentType = req.getContentType();
            if (contentType != null) {
                final int idx = contentType.indexOf(59);
                if (idx > 0) {
                    contentType = contentType.substring(0, idx);
                }
                contentType = contentType.trim();
                if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    static class StandardRequestParser implements SolrRequestParser
    {
        MultipartRequestParser multipart;
        RawRequestParser raw;
        FormDataRequestParser formdata;
        
        StandardRequestParser(final MultipartRequestParser multi, final RawRequestParser raw, final FormDataRequestParser formdata) {
            this.multipart = multi;
            this.raw = raw;
            this.formdata = formdata;
        }
        
        @Override
        public SolrParams parseParamsAndFillStreams(final HttpServletRequest req, final ArrayList<ContentStream> streams) throws Exception {
            final String contentType = req.getContentType();
            final String method = req.getMethod();
            final String uri = req.getRequestURI();
            final boolean isV2 = getHttpSolrCall(req) instanceof V2HttpCall;
            final boolean isPost = "POST".equals(method);
            if (!isPost) {
                if (isV2) {
                    return this.raw.parseParamsAndFillStreams(req, streams);
                }
                if (contentType == null) {
                    return (SolrParams)SolrRequestParsers.parseQueryString(req.getQueryString());
                }
                boolean restletPath = false;
                final int idx = uri.indexOf("/schema");
                if ((idx >= 0 && uri.endsWith("/schema")) || uri.contains("/schema/")) {
                    restletPath = true;
                }
                if (restletPath) {
                    return (SolrParams)SolrRequestParsers.parseQueryString(req.getQueryString());
                }
                if ("PUT".equals(method) || "DELETE".equals(method)) {
                    throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Unsupported method: " + method + " for request " + req);
                }
            }
            if (this.formdata.isFormData(req)) {
                final String userAgent = req.getHeader("User-Agent");
                final boolean isCurl = userAgent != null && userAgent.startsWith("curl/");
                final FastInputStream input = FastInputStream.wrap((InputStream)req.getInputStream());
                if (isCurl) {
                    final SolrParams params = autodetect(req, streams, input);
                    if (params != null) {
                        return params;
                    }
                }
                return this.formdata.parseParamsAndFillStreams(req, streams, (InputStream)input);
            }
            if (ServletFileUpload.isMultipartContent(req)) {
                return this.multipart.parseParamsAndFillStreams(req, streams);
            }
            return this.raw.parseParamsAndFillStreams(req, streams);
        }
    }
    
    static class InputStreamContentStream extends ContentStreamBase
    {
        private final InputStream is;
        
        public InputStreamContentStream(final InputStream is, final String detectedContentType, final Long size) {
            this.is = is;
            this.contentType = detectedContentType;
            this.size = size;
        }
        
        public InputStream getStream() throws IOException {
            return this.is;
        }
    }
    
    interface SolrRequestParser
    {
        SolrParams parseParamsAndFillStreams(final HttpServletRequest p0, final ArrayList<ContentStream> p1) throws Exception;
    }
}
