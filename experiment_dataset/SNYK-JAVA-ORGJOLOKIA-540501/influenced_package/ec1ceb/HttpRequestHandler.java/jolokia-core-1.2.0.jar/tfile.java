// 
// Decompiled by Procyon v0.5.36
// 

package org.jolokia.http;

import java.util.regex.Matcher;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.jolokia.config.ConfigKey;
import javax.management.RuntimeMBeanException;
import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.InstanceNotFoundException;
import javax.management.ReflectionException;
import org.json.simple.parser.ParseException;
import java.io.Reader;
import org.json.simple.parser.JSONParser;
import java.io.InputStreamReader;
import java.io.IOException;
import org.json.simple.JSONObject;
import java.util.List;
import org.json.simple.JSONArray;
import java.io.InputStream;
import java.util.Iterator;
import java.util.HashMap;
import org.jolokia.config.ProcessingParameters;
import org.jolokia.request.JmxRequest;
import org.jolokia.request.JmxRequestFactory;
import org.json.simple.JSONAware;
import java.util.Map;
import java.util.regex.Pattern;
import org.jolokia.config.Configuration;
import org.jolokia.util.LogHandler;
import org.jolokia.backend.BackendManager;

public class HttpRequestHandler
{
    private BackendManager backendManager;
    private LogHandler logHandler;
    private Configuration config;
    private static final Pattern PATH_PREFIX_PATTERN;
    
    public HttpRequestHandler(final Configuration pConfig, final BackendManager pBackendManager, final LogHandler pLogHandler) {
        this.backendManager = pBackendManager;
        this.logHandler = pLogHandler;
        this.config = pConfig;
    }
    
    public JSONAware handleGetRequest(final String pUri, final String pPathInfo, final Map<String, String[]> pParameterMap) {
        final String pathInfo = this.extractPathInfo(pUri, pPathInfo);
        final JmxRequest jmxReq = JmxRequestFactory.createGetRequest(pathInfo, this.getProcessingParameter(pParameterMap));
        if (this.backendManager.isDebug()) {
            this.logHandler.debug("URI: " + pUri);
            this.logHandler.debug("Path-Info: " + pathInfo);
            this.logHandler.debug("Request: " + jmxReq.toString());
        }
        return (JSONAware)this.executeRequest(jmxReq);
    }
    
    private ProcessingParameters getProcessingParameter(final Map<String, String[]> pParameterMap) {
        final Map<String, String> ret = new HashMap<String, String>();
        if (pParameterMap != null) {
            for (final Map.Entry<String, String[]> entry : pParameterMap.entrySet()) {
                final String[] values = entry.getValue();
                if (values != null && values.length > 0) {
                    ret.put(entry.getKey(), values[0]);
                }
            }
        }
        return this.config.getProcessingParameters(ret);
    }
    
    public JSONAware handlePostRequest(final String pUri, final InputStream pInputStream, final String pEncoding, final Map<String, String[]> pParameterMap) throws IOException {
        if (this.backendManager.isDebug()) {
            this.logHandler.debug("URI: " + pUri);
        }
        final Object jsonRequest = this.extractJsonRequest(pInputStream, pEncoding);
        if (jsonRequest instanceof JSONArray) {
            final List<JmxRequest> jmxRequests = JmxRequestFactory.createPostRequests((List)jsonRequest, this.getProcessingParameter(pParameterMap));
            final JSONArray responseList = new JSONArray();
            for (final JmxRequest jmxReq : jmxRequests) {
                if (this.backendManager.isDebug()) {
                    this.logHandler.debug("Request: " + jmxReq.toString());
                }
                final JSONObject resp = this.executeRequest(jmxReq);
                responseList.add((Object)resp);
            }
            return (JSONAware)responseList;
        }
        if (jsonRequest instanceof JSONObject) {
            final JmxRequest jmxReq2 = JmxRequestFactory.createPostRequest((Map<String, ?>)jsonRequest, this.getProcessingParameter(pParameterMap));
            return (JSONAware)this.executeRequest(jmxReq2);
        }
        throw new IllegalArgumentException("Invalid JSON Request " + jsonRequest);
    }
    
    public Map<String, String> handleCorsPreflightRequest(final String pOrigin, final String pRequestHeaders) {
        final Map<String, String> ret = new HashMap<String, String>();
        if (pOrigin != null && this.backendManager.isCorsAccessAllowed(pOrigin)) {
            ret.put("Access-Control-Allow-Origin", "null".equals(pOrigin) ? "*" : pOrigin);
            if (pRequestHeaders != null) {
                ret.put("Access-Control-Allow-Headers", pRequestHeaders);
            }
            ret.put("Access-Control-Allow-Credentials", "true");
            ret.put("Access-Control-Allow-Max-Age", "31536000");
        }
        return ret;
    }
    
    private Object extractJsonRequest(final InputStream pInputStream, final String pEncoding) throws IOException {
        InputStreamReader reader = null;
        try {
            reader = ((pEncoding != null) ? new InputStreamReader(pInputStream, pEncoding) : new InputStreamReader(pInputStream));
            final JSONParser parser = new JSONParser();
            return parser.parse((Reader)reader);
        }
        catch (ParseException exp) {
            throw new IllegalArgumentException("Invalid JSON request " + reader, (Throwable)exp);
        }
    }
    
    private JSONObject executeRequest(final JmxRequest pJmxReq) {
        try {
            return this.backendManager.handleRequest(pJmxReq);
        }
        catch (ReflectionException e) {
            return this.getErrorJSON(404, e, pJmxReq);
        }
        catch (InstanceNotFoundException e2) {
            return this.getErrorJSON(404, e2, pJmxReq);
        }
        catch (MBeanException e3) {
            return this.getErrorJSON(500, e3.getTargetException(), pJmxReq);
        }
        catch (AttributeNotFoundException e4) {
            return this.getErrorJSON(404, e4, pJmxReq);
        }
        catch (UnsupportedOperationException e5) {
            return this.getErrorJSON(500, e5, pJmxReq);
        }
        catch (IOException e6) {
            return this.getErrorJSON(500, e6, pJmxReq);
        }
        catch (IllegalArgumentException e7) {
            return this.getErrorJSON(400, e7, pJmxReq);
        }
        catch (SecurityException e8) {
            return this.getErrorJSON(403, new Exception(e8.getMessage()), pJmxReq);
        }
        catch (RuntimeMBeanException e9) {
            return this.errorForUnwrappedException(e9, pJmxReq);
        }
    }
    
    public JSONObject handleThrowable(final Throwable pThrowable) {
        if (pThrowable instanceof IllegalArgumentException) {
            return this.getErrorJSON(400, pThrowable, null);
        }
        if (pThrowable instanceof SecurityException) {
            return this.getErrorJSON(403, new Exception(pThrowable.getMessage()), null);
        }
        return this.getErrorJSON(500, pThrowable, null);
    }
    
    public JSONObject getErrorJSON(final int pErrorCode, final Throwable pExp, final JmxRequest pJmxReq) {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put((Object)"status", (Object)pErrorCode);
        jsonObject.put((Object)"error", (Object)this.getExceptionMessage(pExp));
        jsonObject.put((Object)"error_type", (Object)pExp.getClass().getName());
        this.addErrorInfo(jsonObject, pExp, pJmxReq);
        if (this.backendManager.isDebug()) {
            this.backendManager.error("Error " + pErrorCode, pExp);
        }
        if (pJmxReq != null) {
            jsonObject.put((Object)"request", (Object)pJmxReq.toJSON());
        }
        return jsonObject;
    }
    
    public void checkClientIPAccess(final String pHost, final String pAddress) {
        if (!this.backendManager.isRemoteAccessAllowed(pHost, pAddress)) {
            throw new SecurityException("No access from client " + pAddress + " allowed");
        }
    }
    
    public String extractCorsOrigin(final String pOrigin) {
        if (pOrigin == null) {
            return null;
        }
        final String origin = pOrigin.replaceAll("[\\n\\r]*", "");
        if (this.backendManager.isCorsAccessAllowed(origin)) {
            return "null".equals(origin) ? "*" : origin;
        }
        return null;
    }
    
    private void addErrorInfo(final JSONObject pErrorResp, final Throwable pExp, final JmxRequest pJmxReq) {
        final String includeStackTrace = (pJmxReq != null) ? pJmxReq.getParameter(ConfigKey.INCLUDE_STACKTRACE) : "true";
        if (includeStackTrace.equalsIgnoreCase("true") || (includeStackTrace.equalsIgnoreCase("runtime") && pExp instanceof RuntimeException)) {
            final StringWriter writer = new StringWriter();
            pExp.printStackTrace(new PrintWriter(writer));
            pErrorResp.put((Object)"stacktrace", (Object)writer.toString());
        }
        if (pJmxReq != null && pJmxReq.getParameterAsBool(ConfigKey.SERIALIZE_EXCEPTION)) {
            pErrorResp.put((Object)"error_value", this.backendManager.convertExceptionToJson(pExp, pJmxReq));
        }
    }
    
    private String getExceptionMessage(final Throwable pException) {
        final String message = pException.getLocalizedMessage();
        return pException.getClass().getName() + ((message != null) ? (" : " + message) : "");
    }
    
    private JSONObject errorForUnwrappedException(final Exception e, final JmxRequest pJmxReq) {
        final Throwable cause = e.getCause();
        final int code = (cause instanceof IllegalArgumentException) ? 400 : ((cause instanceof SecurityException) ? 403 : 500);
        return this.getErrorJSON(code, cause, pJmxReq);
    }
    
    private String extractPathInfo(final String pUri, final String pPathInfo) {
        if (pUri.contains("!//")) {
            final Matcher matcher = HttpRequestHandler.PATH_PREFIX_PATTERN.matcher(pPathInfo);
            if (matcher.find()) {
                final String prefix = matcher.group();
                final String pathInfoEncoded = pUri.replaceFirst("^.*?" + prefix, prefix);
                try {
                    return URLDecoder.decode(pathInfoEncoded, "UTF-8");
                }
                catch (UnsupportedEncodingException ex) {}
            }
        }
        return pPathInfo;
    }
    
    static {
        PATH_PREFIX_PATTERN = Pattern.compile("^/?[^/]+/");
    }
}
