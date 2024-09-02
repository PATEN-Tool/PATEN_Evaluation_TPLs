// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.wink.client.internal;

import org.slf4j.LoggerFactory;
import org.apache.wink.client.internal.handlers.HandlerContextImpl;
import org.apache.wink.common.internal.WinkConfiguration;
import org.apache.wink.client.internal.handlers.ClientRequestImpl;
import org.apache.wink.client.handlers.HandlerContext;
import org.apache.wink.client.ClientRequest;
import org.apache.wink.client.ClientRuntimeException;
import org.apache.wink.common.internal.i18n.Messages;
import org.apache.wink.common.RuntimeContext;
import org.apache.wink.common.internal.runtime.RuntimeContextTLS;
import org.apache.wink.client.EntityType;
import java.lang.reflect.Type;
import org.apache.wink.client.ClientWebException;
import org.apache.wink.client.ClientResponse;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.core.Cookie;
import org.apache.wink.common.internal.utils.HeaderUtils;
import java.util.Locale;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import org.apache.wink.common.internal.CaseInsensitiveMultivaluedMap;
import java.net.URI;
import org.slf4j.Logger;
import javax.ws.rs.core.UriBuilder;
import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.wink.client.ClientConfig;
import org.apache.wink.common.internal.registry.ProvidersRegistry;
import org.apache.wink.client.Resource;

public class ResourceImpl implements Resource
{
    private static final String USER_AGENT = "Wink Client v1.1.2";
    private ProvidersRegistry providersRegistry;
    private ClientConfig config;
    private MultivaluedMap<String, String> headers;
    private Map<String, Object> attributes;
    private UriBuilder uriBuilder;
    private static Logger logger;
    
    public ResourceImpl(final URI uri, final ClientConfig config, final ProvidersRegistry providersRegistry) {
        this.config = config;
        this.providersRegistry = providersRegistry;
        this.uriBuilder = UriBuilder.fromUri(uri);
        this.headers = (MultivaluedMap<String, String>)new CaseInsensitiveMultivaluedMap();
        this.attributes = new HashMap<String, Object>();
    }
    
    public Resource accept(final String... values) {
        final String header = (String)this.headers.getFirst((Object)"Accept");
        this.headers.putSingle((Object)"Accept", (Object)this.appendHeaderValues(header, values));
        return this;
    }
    
    public Resource accept(final MediaType... values) {
        final String header = (String)this.headers.getFirst((Object)"Accept");
        this.headers.putSingle((Object)"Accept", (Object)this.appendHeaderValues(header, values));
        return this;
    }
    
    public Resource acceptLanguage(final String... values) {
        final String header = (String)this.headers.getFirst((Object)"Accept-Language");
        this.headers.putSingle((Object)"Accept-Language", (Object)this.appendHeaderValues(header, values));
        return this;
    }
    
    public Resource acceptLanguage(final Locale... values) {
        final String[] types = new String[values.length];
        for (int i = 0; i < values.length; ++i) {
            types[i] = HeaderUtils.localeToLanguage(values[i]);
        }
        return this;
    }
    
    public Resource contentType(final String mediaType) {
        this.headers.putSingle((Object)"Content-Type", (Object)mediaType);
        return this;
    }
    
    public Resource contentType(final MediaType mediaType) {
        return this.contentType(mediaType.toString());
    }
    
    public Resource cookie(final String value) {
        this.headers.add((Object)"Cookie", (Object)value);
        return this;
    }
    
    public Resource cookie(final Cookie value) {
        return this.cookie(value.toString());
    }
    
    public Resource header(final String name, final String... values) {
        if (name == null) {
            return this;
        }
        final StringBuilder finalHeaderValue = new StringBuilder();
        boolean isFirstHeader = true;
        for (final String value : values) {
            if (value != null && value.trim().length() != 0) {
                if (!isFirstHeader) {
                    finalHeaderValue.append(", ");
                }
                else {
                    isFirstHeader = false;
                }
                finalHeaderValue.append(value);
            }
        }
        if (finalHeaderValue.length() > 0) {
            this.headers.add((Object)name, (Object)finalHeaderValue.toString());
        }
        return this;
    }
    
    public Resource queryParam(final String key, final Object... values) {
        this.uriBuilder.queryParam(key, values);
        return this;
    }
    
    public Resource queryParams(final MultivaluedMap<String, String> params) {
        for (final String query : params.keySet()) {
            this.queryParam(query, ((List)params.get((Object)query)).toArray());
        }
        return this;
    }
    
    public Resource attribute(final String key, final Object value) {
        this.attributes.put(key, value);
        return this;
    }
    
    public Object attribute(final String key) {
        return this.attributes.get(key);
    }
    
    public UriBuilder getUriBuilder() {
        return this.uriBuilder;
    }
    
    public Resource uri(final URI uri) {
        this.uriBuilder = UriBuilder.fromUri(uri);
        return this;
    }
    
    public Resource uri(final String uri) {
        this.uriBuilder = UriBuilder.fromUri(uri);
        return this;
    }
    
    private ClientResponse invokeNoException(final String method, final Object requestEntity) {
        try {
            return this.invoke(method, ClientResponse.class, requestEntity);
        }
        catch (ClientWebException e) {
            return e.getResponse();
        }
    }
    
    public <T> T invoke(final String method, final Class<T> responseEntity, final Object requestEntity) {
        final ClientResponse response = this.invoke(method, responseEntity, responseEntity, requestEntity);
        if (responseEntity == null) {
            return null;
        }
        if (ClientResponse.class.equals(responseEntity)) {
            return (T)response;
        }
        return response.getEntity(responseEntity);
    }
    
    public <T> T invoke(final String method, final EntityType<T> responseEntity, final Object requestEntity) {
        if (responseEntity == null) {
            this.invoke(method, null, null, requestEntity);
            return null;
        }
        final ClientResponse response = this.invoke(method, responseEntity.getRawClass(), responseEntity.getType(), requestEntity);
        if (ClientResponse.class.equals(responseEntity.getRawClass())) {
            return (T)response;
        }
        return response.getEntity(responseEntity);
    }
    
    private ClientResponse invoke(final String method, final Class<?> responseEntity, final Type responseEntityType, final Object requestEntity) {
        final ClientRequest request = this.createClientRequest(method, responseEntity, responseEntityType, requestEntity);
        final HandlerContext context = this.createHandlerContext();
        final ProvidersRegistry providersRegistry = request.getAttribute(ProvidersRegistry.class);
        final ClientRuntimeContext runtimeContext = new ClientRuntimeContext(providersRegistry);
        final RuntimeContext saved = RuntimeContextTLS.getRuntimeContext();
        RuntimeContextTLS.setRuntimeContext(runtimeContext);
        try {
            final ClientResponse response = context.doChain(request);
            final int statusCode = response.getStatusCode();
            if (ClientUtils.isErrorCode(statusCode)) {
                ResourceImpl.logger.trace(Messages.getMessage("clientResponseIsErrorCode", String.valueOf(statusCode)));
                throw new ClientWebException(request, response);
            }
            return response;
        }
        catch (ClientWebException e) {
            throw e;
        }
        catch (ClientRuntimeException e2) {
            throw e2;
        }
        catch (Exception e3) {
            throw new ClientRuntimeException(e3);
        }
        finally {
            RuntimeContextTLS.setRuntimeContext(saved);
        }
    }
    
    private <T> ClientRequest createClientRequest(final String method, final Class<T> responseEntity, final Type responseEntityType, final Object requestEntity) {
        final ClientRequest request = new ClientRequestImpl();
        request.setEntity(requestEntity);
        final URI requestURI = this.uriBuilder.build(new Object[0]);
        request.setURI(requestURI);
        request.setMethod(method);
        request.getHeaders().putAll((Map)this.headers);
        if (ResourceImpl.logger.isTraceEnabled()) {
            final Integer requestEntityInfo = (requestEntity == null) ? null : Integer.valueOf(System.identityHashCode(requestEntity));
            ResourceImpl.logger.trace(Messages.getMessage("clientIssueRequest", method, requestURI, requestEntityInfo, this.headers.keySet()));
        }
        if (this.headers.getFirst((Object)"User-Agent") == null) {
            request.getHeaders().add((Object)"User-Agent", (Object)"Wink Client v1.1.2");
        }
        request.getAttributes().putAll(this.attributes);
        request.setAttribute(ProvidersRegistry.class, this.providersRegistry);
        request.setAttribute(WinkConfiguration.class, this.config);
        request.setAttribute(ClientConfig.class, this.config);
        request.getAttributes().put("response.entity.generic.type", responseEntityType);
        request.getAttributes().put("response.entity.class.type", responseEntity);
        return request;
    }
    
    private HandlerContext createHandlerContext() {
        final HandlerContext context = new HandlerContextImpl(this.config.getHandlers());
        return context;
    }
    
    public ClientResponse head() {
        return this.invokeNoException("HEAD", null);
    }
    
    public ClientResponse options() {
        return this.invokeNoException("OPTIONS", null);
    }
    
    public <T> T delete(final Class<T> responseEntity) {
        return this.invoke("DELETE", responseEntity, null);
    }
    
    public <T> T delete(final EntityType<T> responseEntity) {
        return this.invoke("DELETE", responseEntity, null);
    }
    
    public ClientResponse delete() {
        return this.invokeNoException("DELETE", null);
    }
    
    public <T> T get(final Class<T> responseEntity) {
        return this.invoke("GET", responseEntity, null);
    }
    
    public <T> T get(final EntityType<T> responseEntity) {
        return this.invoke("GET", responseEntity, null);
    }
    
    public ClientResponse get() {
        return this.invokeNoException("GET", null);
    }
    
    public <T> T post(final Class<T> responseEntity, final Object requestEntity) {
        return this.invoke("POST", responseEntity, requestEntity);
    }
    
    public <T> T post(final EntityType<T> responseEntity, final Object requestEntity) {
        return this.invoke("POST", responseEntity, requestEntity);
    }
    
    public ClientResponse post(final Object requestEntity) {
        return this.invokeNoException("POST", requestEntity);
    }
    
    public <T> T put(final Class<T> responseEntity, final Object requestEntity) {
        return this.invoke("PUT", responseEntity, requestEntity);
    }
    
    public <T> T put(final EntityType<T> responseEntity, final Object requestEntity) {
        return this.invoke("PUT", responseEntity, requestEntity);
    }
    
    public ClientResponse put(final Object requestEntity) {
        return this.invokeNoException("PUT", requestEntity);
    }
    
    private <T> String toHeaderString(final T[] objects) {
        String delim = "";
        final StringBuilder sb = new StringBuilder();
        for (final T t : objects) {
            sb.append(delim);
            sb.append(t.toString());
            delim = ", ";
        }
        return sb.toString();
    }
    
    private <T> String appendHeaderValues(final String value, final T[] objects) {
        final StringBuilder builder = new StringBuilder((value != null) ? value : "");
        builder.append((value != null) ? ", " : "");
        builder.append(this.toHeaderString(objects));
        return builder.toString();
    }
    
    static {
        ResourceImpl.logger = LoggerFactory.getLogger((Class)ResourceImpl.class);
    }
}
