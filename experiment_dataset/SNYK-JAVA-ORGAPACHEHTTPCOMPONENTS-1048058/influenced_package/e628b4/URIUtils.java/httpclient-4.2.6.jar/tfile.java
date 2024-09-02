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
    @Deprecated
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
        final URIBuilder uribuilder = new URIBuilder(uri);
        if (target != null) {
            uribuilder.setScheme(target.getSchemeName());
            uribuilder.setHost(target.getHostName());
            uribuilder.setPort(target.getPort());
        }
        else {
            uribuilder.setScheme(null);
            uribuilder.setHost(null);
            uribuilder.setPort(-1);
        }
        if (dropFragment) {
            uribuilder.setFragment(null);
        }
        if (uribuilder.getPath() == null || uribuilder.getPath().length() == 0) {
            uribuilder.setPath("/");
        }
        return uribuilder.build();
    }
    
    public static URI rewriteURI(final URI uri, final HttpHost target) throws URISyntaxException {
        return rewriteURI(uri, target, false);
    }
    
    public static URI rewriteURI(final URI uri) throws URISyntaxException {
        if (uri == null) {
            throw new IllegalArgumentException("URI may not be null");
        }
        if (uri.getFragment() != null || uri.getUserInfo() != null || uri.getPath() == null || uri.getPath().length() == 0) {
            final URIBuilder uribuilder = new URIBuilder(uri);
            uribuilder.setFragment(null).setUserInfo(null);
            if (uribuilder.getPath() == null || uribuilder.getPath().length() == 0) {
                uribuilder.setPath("/");
            }
            return uribuilder.build();
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
        return normalizeSyntax(resolved);
    }
    
    private static URI resolveReferenceStartingWithQueryString(final URI baseURI, final URI reference) {
        String baseUri = baseURI.toString();
        baseUri = ((baseUri.indexOf(63) > -1) ? baseUri.substring(0, baseUri.indexOf(63)) : baseUri);
        return URI.create(baseUri + reference.toString());
    }
    
    private static URI normalizeSyntax(final URI uri) {
        if (uri.isOpaque()) {
            return uri;
        }
        final String path = (uri.getPath() == null) ? "" : uri.getPath();
        final String[] inputSegments = path.split("/");
        final Stack<String> outputSegments = new Stack<String>();
        for (final String inputSegment : inputSegments) {
            if (inputSegment.length() != 0) {
                if (!".".equals(inputSegment)) {
                    if ("..".equals(inputSegment)) {
                        if (!outputSegments.isEmpty()) {
                            outputSegments.pop();
                        }
                    }
                    else {
                        outputSegments.push(inputSegment);
                    }
                }
            }
        }
        final StringBuilder outputBuffer = new StringBuilder();
        for (final String outputSegment : outputSegments) {
            outputBuffer.append('/').append(outputSegment);
        }
        if (path.lastIndexOf(47) == path.length() - 1) {
            outputBuffer.append('/');
        }
        try {
            final String scheme = uri.getScheme().toLowerCase();
            final String auth = uri.getAuthority().toLowerCase();
            final URI ref = new URI(scheme, auth, outputBuffer.toString(), null, null);
            if (uri.getQuery() == null && uri.getFragment() == null) {
                return ref;
            }
            final StringBuilder normalized = new StringBuilder(ref.toASCIIString());
            if (uri.getQuery() != null) {
                normalized.append('?').append(uri.getRawQuery());
            }
            if (uri.getFragment() != null) {
                normalized.append('#').append(uri.getRawFragment());
            }
            return URI.create(normalized.toString());
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
