// 
// Decompiled by Procyon v0.5.36
// 

package org.jboss.resteasy.core.registry;

import org.jboss.resteasy.core.ResourceMethodInvoker;
import java.lang.reflect.Method;
import org.jboss.resteasy.spi.HttpRequest;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jboss.resteasy.spi.ResourceInvoker;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

public class RootNode
{
    protected SegmentNode root;
    protected int size;
    protected MultivaluedMap<String, MethodExpression> bounded;
    
    public RootNode() {
        this.root = new SegmentNode("");
        this.size = 0;
        this.bounded = (MultivaluedMap<String, MethodExpression>)new MultivaluedHashMap();
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
        return this.root.match(request, start);
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
}
