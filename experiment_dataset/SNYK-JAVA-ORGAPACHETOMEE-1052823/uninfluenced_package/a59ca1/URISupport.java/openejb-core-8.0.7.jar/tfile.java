// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.openejb.util;

import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;
import java.net.URLEncoder;
import java.util.Collections;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.net.URI;

public class URISupport
{
    public static URI relativize(final URI a, URI b) {
        if (a == null || b == null) {
            return b;
        }
        if (!a.isAbsolute() && b.isAbsolute()) {
            return b;
        }
        if (!b.isAbsolute()) {
            b = a.resolve(b);
        }
        final List<String> pathA = Arrays.asList(a.getPath().split("/"));
        final List<String> pathB = Arrays.asList(b.getPath().split("/"));
        int limit;
        int lastMatch;
        for (limit = Math.min(pathA.size(), pathB.size()), lastMatch = 0; lastMatch < limit; ++lastMatch) {
            final String aa = pathA.get(lastMatch);
            final String bb = pathB.get(lastMatch);
            if (!aa.equals(bb)) {
                break;
            }
        }
        final List<String> path = new ArrayList<String>();
        for (int x = pathA.size() - lastMatch; x > 0; --x) {
            path.add("..");
        }
        final List<String> remaining = pathB.subList(lastMatch, pathB.size());
        path.addAll(remaining);
        try {
            return new URI(null, null, Join.join("/", path), b.getQuery(), b.getFragment());
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    public static Map<String, String> parseQuery(final String uri) throws URISyntaxException {
        try {
            final Map<String, String> rc = new LinkedHashMap<String, String>();
            if (uri != null) {
                final String[] split;
                final String[] parameters = split = uri.split("&");
                for (final String parameter : split) {
                    final int p = parameter.indexOf(61);
                    if (p >= 0) {
                        final String name = URLDecoder.decode(parameter.substring(0, p), "UTF-8");
                        final String value = URLDecoder.decode(parameter.substring(p + 1), "UTF-8");
                        rc.put(name, value);
                    }
                    else {
                        rc.put(parameter, null);
                    }
                }
            }
            return rc;
        }
        catch (UnsupportedEncodingException e) {
            throw (URISyntaxException)new URISyntaxException(e.toString(), "Invalid encoding").initCause(e);
        }
    }
    
    public static Map<String, String> parseParamters(final URI uri) throws URISyntaxException {
        return (uri.getQuery() == null) ? Collections.EMPTY_MAP : parseQuery(stripPrefix(uri.getQuery(), "?"));
    }
    
    public static URI removeQuery(final URI uri) throws URISyntaxException {
        return createURIWithQuery(uri, null);
    }
    
    public static URI createURIWithQuery(final URI uri, final String query) throws URISyntaxException {
        return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), query, uri.getFragment());
    }
    
    public static CompositeData parseComposite(final URI uri) throws URISyntaxException {
        final CompositeData rc = new CompositeData();
        rc.scheme = uri.getScheme();
        final String ssp = stripPrefix(uri.getSchemeSpecificPart().trim(), "//").trim();
        parseComposite(uri, rc, ssp);
        rc.fragment = uri.getFragment();
        return rc;
    }
    
    private static void parseComposite(final URI uri, final CompositeData rc, final String ssp) throws URISyntaxException {
        if (!checkParenthesis(ssp)) {
            throw new URISyntaxException(uri.toString(), "Not a matching number of '(' and ')' parenthesis");
        }
        final int intialParen = ssp.indexOf(40);
        String componentString;
        String params;
        if (intialParen == 0) {
            rc.host = ssp.substring(0, intialParen);
            int p = rc.host.indexOf(47);
            if (p >= 0) {
                rc.path = rc.host.substring(p);
                rc.host = rc.host.substring(0, p);
            }
            p = ssp.lastIndexOf(41);
            componentString = ssp.substring(intialParen + 1, p);
            params = ssp.substring(p + 1).trim();
        }
        else {
            componentString = ssp;
            params = "";
        }
        final String[] components = splitComponents(componentString);
        rc.components = new URI[components.length];
        for (int i = 0; i < components.length; ++i) {
            rc.components[i] = new URI(components[i].trim());
        }
        int p = params.indexOf(63);
        if (p >= 0) {
            if (p > 0) {
                rc.path = stripPrefix(params.substring(0, p), "/");
            }
            rc.parameters = parseQuery(params.substring(p + 1));
        }
        else {
            if (params.length() > 0) {
                rc.path = stripPrefix(params, "/");
            }
            rc.parameters = new LinkedHashMap();
        }
    }
    
    private static String[] splitComponents(final String str) {
        final ArrayList<String> l = new ArrayList<String>();
        int last = 0;
        int depth = 0;
        final char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; ++i) {
            switch (chars[i]) {
                case '(': {
                    ++depth;
                    break;
                }
                case ')': {
                    --depth;
                    break;
                }
                case ',': {
                    if (depth == 0) {
                        final String s = str.substring(last, i);
                        l.add(s);
                        last = i + 1;
                        break;
                    }
                    break;
                }
            }
        }
        final String s2 = str.substring(last);
        if (s2.length() != 0) {
            l.add(s2);
        }
        final String[] rc = new String[l.size()];
        l.toArray(rc);
        return rc;
    }
    
    public static String stripPrefix(final String value, final String prefix) {
        if (value.startsWith(prefix)) {
            return value.substring(prefix.length());
        }
        return value;
    }
    
    public static URI stripScheme(final URI uri) throws URISyntaxException {
        return URLs.uri(stripPrefix(uri.getRawSchemeSpecificPart().trim(), "//"));
    }
    
    public static String createQueryString(final Map options) throws URISyntaxException {
        try {
            if (options.size() > 0) {
                final StringBuilder rc = new StringBuilder();
                boolean first = true;
                for (final Object o : options.keySet()) {
                    if (first) {
                        first = false;
                    }
                    else {
                        rc.append("&");
                    }
                    final String key = (String)o;
                    final String value = (String)options.get(key);
                    rc.append(URLEncoder.encode(key, "UTF-8"));
                    rc.append("=");
                    rc.append(URLEncoder.encode(value, "UTF-8"));
                }
                return rc.toString();
            }
            return "";
        }
        catch (UnsupportedEncodingException e) {
            throw (URISyntaxException)new URISyntaxException(e.toString(), "Invalid encoding").initCause(e);
        }
    }
    
    public static URI createRemainingURI(final URI originalURI, final Map params) throws URISyntaxException {
        String s = createQueryString(params);
        if (s.length() == 0) {
            s = null;
        }
        return createURIWithQuery(originalURI, s);
    }
    
    public static URI changeScheme(final URI bindAddr, final String scheme) throws URISyntaxException {
        return new URI(scheme, bindAddr.getUserInfo(), bindAddr.getHost(), bindAddr.getPort(), bindAddr.getPath(), bindAddr.getQuery(), bindAddr.getFragment());
    }
    
    public static boolean checkParenthesis(final String str) {
        boolean result = true;
        if (str != null) {
            int open = 0;
            int closed = 0;
            for (int i = 0; (i = str.indexOf(40, i)) >= 0; ++i, ++open) {}
            for (int i = 0; (i = str.indexOf(41, i)) >= 0; ++i, ++closed) {}
            result = (open == closed);
        }
        return result;
    }
    
    public int indexOfParenthesisMatch(final String str) {
        final int result = -1;
        return -1;
    }
    
    public static URI addParameters(final URI uri, final Map<String, String> newParameters) throws URISyntaxException {
        if (newParameters == null || newParameters.size() == 0) {
            return uri;
        }
        final Map<String, String> parameters = new HashMap<String, String>(parseParamters(uri));
        final Set<String> keys = newParameters.keySet();
        for (final String key : keys) {
            if (!parameters.containsKey(key)) {
                parameters.put(key, newParameters.get(key));
            }
        }
        return createRemainingURI(uri, parameters);
    }
    
    public static class CompositeData
    {
        String scheme;
        String path;
        URI[] components;
        Map parameters;
        String fragment;
        public String host;
        
        public URI[] getComponents() {
            return this.components;
        }
        
        public String getFragment() {
            return this.fragment;
        }
        
        public Map getParameters() {
            return this.parameters;
        }
        
        public String getScheme() {
            return this.scheme;
        }
        
        public String getPath() {
            return this.path;
        }
        
        public String getHost() {
            return this.host;
        }
        
        public URI toURI() throws URISyntaxException {
            final StringBuilder sb = new StringBuilder();
            if (this.scheme != null) {
                sb.append(this.scheme);
                sb.append(':');
            }
            if (this.host != null && this.host.length() != 0) {
                sb.append(this.host);
            }
            else {
                sb.append('(');
                for (int i = 0; i < this.components.length; ++i) {
                    if (i != 0) {
                        sb.append(',');
                    }
                    sb.append(this.components[i].toString());
                }
                sb.append(')');
            }
            if (this.path != null) {
                sb.append('/');
                sb.append(this.path);
            }
            if (!this.parameters.isEmpty()) {
                sb.append("?");
                sb.append(URISupport.createQueryString(this.parameters));
            }
            if (this.fragment != null) {
                sb.append("#");
                sb.append(this.fragment);
            }
            return URLs.uri(sb.toString());
        }
    }
}
