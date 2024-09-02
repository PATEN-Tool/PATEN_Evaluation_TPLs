// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.http.client.utils;

import java.util.Iterator;
import java.util.Stack;
import org.apache.http.HttpHost;
import java.net.URISyntaxException;
import java.net.URI;
import org.apache.http.annotation.Immutable;

@Immutable
public class URIUtils
{
    public static URI createURI(final String scheme, final String host, final int port, final String path, final String query, final String fragment) throws URISyntaxException {
        final StringBuilder buffer = new StringBuilder();
        if (host != null) {
            if (scheme != null) {
                buffer.append(scheme);
                buffer.append("://");
            }
            buffer.append(host);
            if (port > 0) {
                buffer.append(':');
                buffer.append(port);
            }
        }
        if (path == null || !path.startsWith("/")) {
            buffer.append('/');
        }
        if (path != null) {
            buffer.append(path);
        }
        if (query != null) {
            buffer.append('?');
            buffer.append(query);
        }
        if (fragment != null) {
            buffer.append('#');
            buffer.append(fragment);
        }
        return new URI(buffer.toString());
    }
    
    public static URI rewriteURI(final URI uri, final HttpHost target, final boolean dropFragment) throws URISyntaxException {
        if (uri == null) {
            throw new IllegalArgumentException("URI may not be null");
        }
        if (target != null) {
            return createURI(target.getSchemeName(), target.getHostName(), target.getPort(), normalizePath(uri.getRawPath()), uri.getRawQuery(), dropFragment ? null : uri.getRawFragment());
        }
        return createURI(null, null, -1, normalizePath(uri.getRawPath()), uri.getRawQuery(), dropFragment ? null : uri.getRawFragment());
    }
    
    private static String normalizePath(String path) {
        if (path == null) {
            return null;
        }
        int n;
        for (n = 0; n < path.length() && path.charAt(n) == '/'; ++n) {}
        if (n > 1) {
            path = path.substring(n - 1);
        }
        return path;
    }
    
    public static URI rewriteURI(final URI uri, final HttpHost target) throws URISyntaxException {
        return rewriteURI(uri, target, false);
    }
    
    public static URI rewriteURI(final URI uri) throws URISyntaxException {
        if (uri == null) {
            throw new IllegalArgumentException("URI may not be null");
        }
        if (uri.getFragment() != null) {
            return createURI(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getRawPath(), uri.getRawQuery(), null);
        }
        return uri;
    }
    
    public static URI resolve(final URI baseURI, final String reference) {
        return resolve(baseURI, URI.create(reference));
    }
    
    public static URI resolve(final URI baseURI, URI reference) {
        if (baseURI == null) {
            throw new IllegalArgumentException("Base URI may nor be null");
        }
        if (reference == null) {
            throw new IllegalArgumentException("Reference URI may nor be null");
        }
        final String s = reference.toString();
        if (s.startsWith("?")) {
            return resolveReferenceStartingWithQueryString(baseURI, reference);
        }
        final boolean emptyReference = s.length() == 0;
        if (emptyReference) {
            reference = URI.create("#");
        }
        URI resolved = baseURI.resolve(reference);
        if (emptyReference) {
            final String resolvedString = resolved.toString();
            resolved = URI.create(resolvedString.substring(0, resolvedString.indexOf(35)));
        }
        return removeDotSegments(resolved);
    }
    
    private static URI resolveReferenceStartingWithQueryString(final URI baseURI, final URI reference) {
        String baseUri = baseURI.toString();
        baseUri = ((baseUri.indexOf(63) > -1) ? baseUri.substring(0, baseUri.indexOf(63)) : baseUri);
        return URI.create(baseUri + reference.toString());
    }
    
    private static URI removeDotSegments(final URI uri) {
        final String path = uri.getPath();
        if (path == null || path.indexOf("/.") == -1) {
            return uri;
        }
        final String[] inputSegments = path.split("/");
        final Stack<String> outputSegments = new Stack<String>();
        for (int i = 0; i < inputSegments.length; ++i) {
            if (inputSegments[i].length() != 0) {
                if (!".".equals(inputSegments[i])) {
                    if ("..".equals(inputSegments[i])) {
                        if (!outputSegments.isEmpty()) {
                            outputSegments.pop();
                        }
                    }
                    else {
                        outputSegments.push(inputSegments[i]);
                    }
                }
            }
        }
        final StringBuilder outputBuffer = new StringBuilder();
        for (final String outputSegment : outputSegments) {
            outputBuffer.append('/').append(outputSegment);
        }
        try {
            return new URI(uri.getScheme(), uri.getAuthority(), outputBuffer.toString(), uri.getQuery(), uri.getFragment());
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    public static HttpHost extractHost(final URI uri) {
        if (uri == null) {
            return null;
        }
        HttpHost target = null;
        if (uri.isAbsolute()) {
            int port = uri.getPort();
            String host = uri.getHost();
            if (host == null) {
                host = uri.getAuthority();
                if (host != null) {
                    final int at = host.indexOf(64);
                    if (at >= 0) {
                        if (host.length() > at + 1) {
                            host = host.substring(at + 1);
                        }
                        else {
                            host = null;
                        }
                    }
                    if (host != null) {
                        final int colon = host.indexOf(58);
                        if (colon >= 0) {
                            final int pos = colon + 1;
                            int len = 0;
                            for (int i = pos; i < host.length() && Character.isDigit(host.charAt(i)); ++i) {
                                ++len;
                            }
                            if (len > 0) {
                                try {
                                    port = Integer.parseInt(host.substring(pos, pos + len));
                                }
                                catch (NumberFormatException ex) {}
                            }
                            host = host.substring(0, colon);
                        }
                    }
                }
            }
            final String scheme = uri.getScheme();
            if (host != null) {
                target = new HttpHost(host, port, scheme);
            }
        }
        return target;
    }
    
    private URIUtils() {
    }
}
