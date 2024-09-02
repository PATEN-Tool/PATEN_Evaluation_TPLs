// 
// Decompiled by Procyon v0.5.36
// 

package org.jboss.resteasy.core.registry;

import java.security.AccessController;
import java.lang.reflect.Method;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.spi.HttpRequest;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jboss.resteasy.spi.ResourceInvoker;
import javax.ws.rs.core.MultivaluedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.core.MultivaluedMap;

public class RootNode
{
    protected SegmentNode root;
    protected int size;
    protected MultivaluedMap<String, MethodExpression> bounded;
    protected ConcurrentHashMap<MatchCache.Key, MatchCache> cache;
    private static int CACHE_SIZE;
    private static boolean CACHE;
    
    public RootNode() {
        this.root = new SegmentNode("");
        this.size = 0;
        this.bounded = (MultivaluedMap<String, MethodExpression>)new MultivaluedHashMap();
        this.cache = new ConcurrentHashMap<MatchCache.Key, MatchCache>();
    }
    
    public int getSize() {
        return this.size;
    }
    
    public MultivaluedMap<String, ResourceInvoker> getBounded() {
        final MultivaluedHashMap<String, ResourceInvoker> rtn = (MultivaluedHashMap<String, ResourceInvoker>)new MultivaluedHashMap();
        for (final Map.Entry<String, List<MethodExpression>> entry : this.bounded.entrySet()) {
            for (final MethodExpression exp : entry.getValue()) {
                rtn.add((Object)entry.getKey(), (Object)exp.getInvoker());
            }
        }
        return (MultivaluedMap<String, ResourceInvoker>)rtn;
    }
    
    public ResourceInvoker match(final HttpRequest request, final int start) {
        if (!RootNode.CACHE || (request.getHttpHeaders().getMediaType() != null && !request.getHttpHeaders().getMediaType().getParameters().isEmpty())) {
            return this.root.match(request, start).invoker;
        }
        final MatchCache.Key key = new MatchCache.Key(request, start);
        MatchCache match = this.cache.get(key);
        if (match != null) {
            request.setAttribute("RESTEASY_CHOSEN_ACCEPT", (Object)match.chosen);
        }
        else {
            match = this.root.match(request, start);
            if (match.match != null && match.match.expression.getNumGroups() == 0 && match.invoker instanceof ResourceMethodInvoker) {
                match.match = null;
                if (this.cache.size() >= RootNode.CACHE_SIZE) {
                    this.cache.clear();
                }
                this.cache.putIfAbsent(key, match);
            }
        }
        return match.invoker;
    }
    
    public void removeBinding(final String path, final Method method) {
        final List<MethodExpression> expressions = (List<MethodExpression>)this.bounded.get((Object)path);
        if (expressions == null) {
            return;
        }
        for (final MethodExpression expression : expressions) {
            final ResourceInvoker invoker = expression.getInvoker();
            if (invoker.getMethod().equals(method)) {
                expression.parent.targets.remove(expression);
                expressions.remove(expression);
                if (expressions.size() == 0) {
                    this.bounded.remove((Object)path);
                }
                --this.size;
                if (invoker instanceof ResourceMethodInvoker) {
                    ((ResourceMethodInvoker)invoker).cleanup();
                }
            }
        }
    }
    
    public void addInvoker(final String path, final ResourceInvoker invoker) {
        final MethodExpression expression = this.addExpression(path, invoker);
        ++this.size;
        this.bounded.add((Object)path, (Object)expression);
    }
    
    protected MethodExpression addExpression(String path, final ResourceInvoker invoker) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (!"".equals(path)) {
            final int expidx = path.indexOf(123);
            MethodExpression exp;
            if (expidx > -1) {
                int i;
                for (i = expidx; i - 1 > -1 && path.charAt(i - 1) != '/'; --i) {}
                String staticPath = null;
                if (i > 0) {
                    staticPath = path.substring(0, i - 1);
                }
                SegmentNode node = this.root;
                if (staticPath != null) {
                    final String[] split3;
                    final String[] split = split3 = staticPath.split("/");
                    for (final String segment : split3) {
                        SegmentNode tmp = node.children.get(segment);
                        if (tmp == null) {
                            tmp = new SegmentNode(segment);
                            node.children.put(segment, tmp);
                        }
                        node = tmp;
                    }
                }
                if (invoker instanceof ResourceMethodInvoker) {
                    exp = new MethodExpression(node, path, invoker);
                }
                else {
                    exp = new MethodExpression(node, path, invoker, "(/.+)?");
                }
                node.addExpression(exp);
            }
            else {
                final String[] split2 = path.split("/");
                SegmentNode node2 = this.root;
                for (final String segment2 : split2) {
                    SegmentNode tmp2 = node2.children.get(segment2);
                    if (tmp2 == null) {
                        tmp2 = new SegmentNode(segment2);
                        node2.children.put(segment2, tmp2);
                    }
                    node2 = tmp2;
                }
                if (invoker instanceof ResourceMethodInvoker) {
                    exp = new MethodExpression(node2, path, invoker);
                    node2.addExpression(exp);
                }
                else {
                    exp = new MethodExpression(node2, path, invoker, "(.*)");
                    node2.addExpression(exp);
                }
            }
            return exp;
        }
        if (invoker instanceof ResourceMethodInvoker) {
            final MethodExpression expression = new MethodExpression(this.root, "", invoker);
            this.root.addExpression(expression);
            return expression;
        }
        final MethodExpression expression = new MethodExpression(this.root, "", invoker, "(.*)");
        this.root.addExpression(expression);
        return expression;
    }
    
    static {
        RootNode.CACHE_SIZE = 2048;
        RootNode.CACHE = true;
        if (System.getSecurityManager() == null) {
            RootNode.CACHE = Boolean.parseBoolean(System.getProperty("resteasy.match.cache.enabled", "true"));
            RootNode.CACHE_SIZE = Integer.getInteger("resteasy.match.cache.size", 2048);
        }
        else {
            RootNode.CACHE = AccessController.doPrivileged(() -> Boolean.parseBoolean(System.getProperty("resteasy.match.cache.enabled", "true")));
            RootNode.CACHE_SIZE = AccessController.doPrivileged(() -> Integer.getInteger("resteasy.match.cache.size", 2048));
        }
    }
}
