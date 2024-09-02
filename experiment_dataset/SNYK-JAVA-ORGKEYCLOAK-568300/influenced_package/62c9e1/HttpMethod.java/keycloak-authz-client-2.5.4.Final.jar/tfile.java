// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.authorization.client.util;

import java.io.UnsupportedEncodingException;
import java.util.List;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.NameValuePair;
import java.util.ArrayList;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.StatusLine;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import java.util.Iterator;
import org.apache.http.util.EntityUtils;
import java.util.Map;
import java.util.HashMap;
import org.keycloak.authorization.client.Configuration;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.HttpClient;

public class HttpMethod<R>
{
    private final HttpClient httpClient;
    private final RequestBuilder builder;
    protected final Configuration configuration;
    protected final HashMap<String, String> headers;
    protected final HashMap<String, String> params;
    private HttpMethodResponse<R> response;
    
    public HttpMethod(final Configuration configuration, final RequestBuilder builder) {
        this(configuration, builder, (HashMap)new HashMap(), (HashMap)new HashMap());
    }
    
    public HttpMethod(final Configuration configuration, final RequestBuilder builder, final HashMap<String, String> params, final HashMap<String, String> headers) {
        this.configuration = configuration;
        this.httpClient = configuration.getHttpClient();
        this.builder = builder;
        this.params = params;
        this.headers = headers;
    }
    
    public void execute() {
        this.execute(new HttpResponseProcessor<R>() {
            @Override
            public R process(final byte[] entity) {
                return null;
            }
        });
    }
    
    public R execute(final HttpResponseProcessor<R> responseProcessor) {
        byte[] bytes = null;
        try {
            for (final Map.Entry<String, String> header : this.headers.entrySet()) {
                this.builder.setHeader((String)header.getKey(), (String)header.getValue());
            }
            this.preExecute(this.builder);
            final HttpResponse response = this.httpClient.execute(this.builder.build());
            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                bytes = EntityUtils.toByteArray(entity);
            }
            final StatusLine statusLine = response.getStatusLine();
            final int statusCode = statusLine.getStatusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new HttpResponseException("Unexpected response from server: " + statusCode + " / " + statusLine.getReasonPhrase(), statusCode, statusLine.getReasonPhrase(), bytes);
            }
            if (bytes == null) {
                return null;
            }
            return responseProcessor.process(bytes);
        }
        catch (HttpResponseException e) {
            throw e;
        }
        catch (Exception e2) {
            throw new RuntimeException("Error executing http method [" + this.builder + "]. Response : " + new String(bytes), e2);
        }
    }
    
    protected void preExecute(final RequestBuilder builder) {
        for (final Map.Entry<String, String> param : this.params.entrySet()) {
            builder.addParameter((String)param.getKey(), (String)param.getValue());
        }
    }
    
    public HttpMethod<R> authorizationBearer(final String bearer) {
        this.builder.addHeader("Authorization", "Bearer " + bearer);
        return this;
    }
    
    public HttpMethodResponse<R> response() {
        return this.response = new HttpMethodResponse<R>(this);
    }
    
    public HttpMethodAuthenticator<R> authentication() {
        return new HttpMethodAuthenticator<R>(this);
    }
    
    public HttpMethod<R> param(final String name, final String value) {
        this.params.put(name, value);
        return this;
    }
    
    public HttpMethod<R> json(final byte[] entity) {
        this.builder.addHeader("Content-Type", "application/json");
        this.builder.setEntity((HttpEntity)new ByteArrayEntity(entity));
        return this;
    }
    
    public HttpMethod<R> form() {
        return new HttpMethod<R>(this.configuration, this.builder, this.params, this.headers) {
            @Override
            protected void preExecute(final RequestBuilder builder) {
                if (this.params != null) {
                    final List<NameValuePair> formparams = new ArrayList<NameValuePair>();
                    for (final Map.Entry<String, String> param : this.params.entrySet()) {
                        formparams.add((NameValuePair)new BasicNameValuePair((String)param.getKey(), (String)param.getValue()));
                    }
                    try {
                        builder.setEntity((HttpEntity)new UrlEncodedFormEntity((List)formparams, "UTF-8"));
                    }
                    catch (UnsupportedEncodingException e) {
                        throw new RuntimeException("Error creating form parameters");
                    }
                }
            }
        };
    }
}
