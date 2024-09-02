// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.http.client.utils;

import java.util.Stack;
import org.apache.http.conn.routing.RouteInfo;
import java.util.Locale;
import org.apache.http.util.TextUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import org.apache.http.util.Args;
import org.apache.http.HttpHost;
import java.net.URISyntaxException;
import java.net.URI;
import java.util.EnumSet;

public class URIUtils
{
    public static final EnumSet<UriFlag> NO_FLAGS;
    public static final EnumSet<UriFlag> DROP_FRAGMENT;
    public static final EnumSet<UriFlag> NORMALIZE;
    public static final EnumSet<UriFlag> DROP_FRAGMENT_AND_NORMALIZE;
    
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
    
    @Deprecated
    public static URI rewriteURI(final URI uri, final HttpHost target, final boolean dropFragment) throws URISyntaxException {
        return rewriteURI(uri, target, dropFragment ? URIUtils.DROP_FRAGMENT : URIUtils.NO_FLAGS);
    }
    
    public static URI rewriteURI(final URI uri, final HttpHost target, final EnumSet<UriFlag> flags) throws URISyntaxException {
        Args.notNull((Object)uri, "URI");
        Args.notNull((Object)flags, "URI flags");
        if (uri.isOpaque()) {
            return uri;
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
        if (flags.contains(UriFlag.DROP_FRAGMENT)) {
            uribuilder.setFragment(null);
        }
        if (flags.contains(UriFlag.NORMALIZE)) {
            final List<String> originalPathSegments = uribuilder.getPathSegments();
            final List<String> pathSegments = new ArrayList<String>(originalPathSegments);
            final Iterator<String> it = pathSegments.iterator();
            while (it.hasNext()) {
                final String pathSegment = it.next();
                if (pathSegment.isEmpty() && it.hasNext()) {
                    it.remove();
                }
            }
            if (pathSegments.size() != originalPathSegments.size()) {
                uribuilder.setPathSegments(pathSegments);
            }
        }
        if (uribuilder.isPathEmpty()) {
            uribuilder.setPathSegments("");
        }
        return uribuilder.build();
    }
    
    public static URI rewriteURI(final URI uri, final HttpHost target) throws URISyntaxException {
        return rewriteURI(uri, target, URIUtils.NORMALIZE);
    }
    
    public static URI rewriteURI(final URI uri) throws URISyntaxException {
        Args.notNull((Object)uri, "URI");
        if (uri.isOpaque()) {
            return uri;
        }
        final URIBuilder uribuilder = new URIBuilder(uri);
        if (uribuilder.getUserInfo() != null) {
            uribuilder.setUserInfo(null);
        }
        if (uribuilder.getPathSegments().isEmpty()) {
            uribuilder.setPathSegments("");
        }
        if (TextUtils.isEmpty((CharSequence)uribuilder.getPath())) {
            uribuilder.setPath("/");
        }
        if (uribuilder.getHost() != null) {
            uribuilder.setHost(uribuilder.getHost().toLowerCase(Locale.ROOT));
        }
        uribuilder.setFragment(null);
        return uribuilder.build();
    }
    
    public static URI rewriteURIForRoute(final URI uri, final RouteInfo route) throws URISyntaxException {
        return rewriteURIForRoute(uri, route, true);
    }
    
    public static URI rewriteURIForRoute(final URI uri, final RouteInfo route, final boolean normalizeUri) throws URISyntaxException {
        if (uri == null) {
            return null;
        }
        if (route.getProxyHost() != null && !route.isTunnelled()) {
            return uri.isAbsolute() ? rewriteURI(uri) : rewriteURI(uri, route.getTargetHost(), normalizeUri ? URIUtils.DROP_FRAGMENT_AND_NORMALIZE : URIUtils.DROP_FRAGMENT);
        }
        return uri.isAbsolute() ? rewriteURI(uri, null, normalizeUri ? URIUtils.DROP_FRAGMENT_AND_NORMALIZE : URIUtils.DROP_FRAGMENT) : rewriteURI(uri);
    }
    
    public static URI resolve(final URI baseURI, final String reference) {
        return resolve(baseURI, URI.create(reference));
    }
    
    public static URI resolve(final URI baseURI, final URI reference) {
        Args.notNull((Object)baseURI, "Base URI");
        Args.notNull((Object)reference, "Reference URI");
        final String s = reference.toASCIIString();
        if (s.startsWith("?")) {
            String baseUri = baseURI.toASCIIString();
            final int i = baseUri.indexOf(63);
            baseUri = ((i > -1) ? baseUri.substring(0, i) : baseUri);
            return URI.create(baseUri + s);
        }
        final boolean emptyReference = s.isEmpty();
        URI resolved;
        if (emptyReference) {
            resolved = baseURI.resolve(URI.create("#"));
            final String resolvedString = resolved.toASCIIString();
            resolved = URI.create(resolvedString.substring(0, resolvedString.indexOf(35)));
        }
        else {
            resolved = baseURI.resolve(reference);
        }
        try {
            return normalizeSyntax(resolved);
        }
        catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    public static URI normalizeSyntax(final URI uri) throws URISyntaxException {
        if (uri.isOpaque() || uri.getAuthority() == null) {
            return uri;
        }
        final URIBuilder builder = new URIBuilder(uri);
        final List<String> inputSegments = builder.getPathSegments();
        final Stack<String> outputSegments = new Stack<String>();
        for (final String inputSegment : inputSegments) {
            if (".".equals(inputSegment)) {
                continue;
            }
            if ("..".equals(inputSegment)) {
                if (outputSegments.isEmpty()) {
                    continue;
                }
                outputSegments.pop();
            }
            else {
                outputSegments.push(inputSegment);
            }
        }
        if (outputSegments.size() == 0) {
            outputSegments.add("");
        }
        builder.setPathSegments(outputSegments);
        if (builder.getScheme() != null) {
            builder.setScheme(builder.getScheme().toLowerCase(Locale.ROOT));
        }
        if (builder.getHost() != null) {
            builder.setHost(builder.getHost().toLowerCase(Locale.ROOT));
        }
        return builder.build();
    }
    
    public static HttpHost extractHost(final URI uri) {
        if (uri == null) {
            return null;
        }
        if (uri.isAbsolute()) {
            if (uri.getHost() == null) {
                if (uri.getAuthority() == null) {
                    return null;
                }
                String content = uri.getAuthority();
                int at = content.indexOf(64);
                if (at != -1) {
                    content = content.substring(at + 1);
                }
                final String scheme = uri.getScheme();
                at = content.indexOf(":");
                Label_0118: {
                    if (at != -1) {
                        final String hostname = content.substring(0, at);
                        try {
                            final String portText = content.substring(at + 1);
                            final int port = TextUtils.isEmpty((CharSequence)portText) ? -1 : Integer.parseInt(portText);
                            break Label_0118;
                        }
                        catch (NumberFormatException ex) {
                            return null;
                        }
                    }
                    final String hostname = content;
                    final int port = -1;
                    try {
                        return new HttpHost(hostname, port, scheme);
                    }
                    catch (IllegalArgumentException ex2) {
                        return null;
                    }
                }
            }
            return new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
        }
        return null;
    }
    
    public static URI resolve(final URI originalURI, final HttpHost target, final List<URI> redirects) throws URISyntaxException {
        Args.notNull((Object)originalURI, "Request URI");
        URIBuilder uribuilder;
        if (redirects == null || redirects.isEmpty()) {
            uribuilder = new URIBuilder(originalURI);
        }
        else {
            uribuilder = new URIBuilder(redirects.get(redirects.size() - 1));
            String frag = uribuilder.getFragment();
            for (int i = redirects.size() - 1; frag == null && i >= 0; frag = redirects.get(i).getFragment(), --i) {}
            uribuilder.setFragment(frag);
        }
        if (uribuilder.getFragment() == null) {
            uribuilder.setFragment(originalURI.getFragment());
        }
        if (target != null && !uribuilder.isAbsolute()) {
            uribuilder.setScheme(target.getSchemeName());
            uribuilder.setHost(target.getHostName());
            uribuilder.setPort(target.getPort());
        }
        return uribuilder.build();
    }
    
    private URIUtils() {
    }
    
    static {
        NO_FLAGS = EnumSet.noneOf(UriFlag.class);
        DROP_FRAGMENT = EnumSet.of(UriFlag.DROP_FRAGMENT);
        NORMALIZE = EnumSet.of(UriFlag.NORMALIZE);
        DROP_FRAGMENT_AND_NORMALIZE = EnumSet.of(UriFlag.DROP_FRAGMENT, UriFlag.NORMALIZE);
    }
    
    public enum UriFlag
    {
        DROP_FRAGMENT, 
        NORMALIZE;
    }
}
