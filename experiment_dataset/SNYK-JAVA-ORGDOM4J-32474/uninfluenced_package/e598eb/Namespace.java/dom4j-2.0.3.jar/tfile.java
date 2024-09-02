// 
// Decompiled by Procyon v0.5.36
// 

package org.dom4j;

import org.dom4j.tree.DefaultNamespace;
import org.dom4j.tree.NamespaceCache;
import org.dom4j.tree.AbstractNode;

public class Namespace extends AbstractNode
{
    protected static final NamespaceCache CACHE;
    public static final Namespace XML_NAMESPACE;
    public static final Namespace NO_NAMESPACE;
    private String prefix;
    private String uri;
    private int hashCode;
    
    public Namespace(final String prefix, final String uri) {
        this.prefix = ((prefix != null) ? prefix : "");
        this.uri = ((uri != null) ? uri : "");
        if (!this.prefix.isEmpty()) {
            QName.validateNCName(this.prefix);
        }
    }
    
    public static Namespace get(final String prefix, final String uri) {
        return Namespace.CACHE.get(prefix, uri);
    }
    
    public static Namespace get(final String uri) {
        return Namespace.CACHE.get(uri);
    }
    
    @Override
    public short getNodeType() {
        return 13;
    }
    
    @Override
    public int hashCode() {
        if (this.hashCode == 0) {
            this.hashCode = this.createHashCode();
        }
        return this.hashCode;
    }
    
    protected int createHashCode() {
        int result = this.uri.hashCode() ^ this.prefix.hashCode();
        if (result == 0) {
            result = 47806;
        }
        return result;
    }
    
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof Namespace) {
            final Namespace that = (Namespace)object;
            if (this.hashCode() == that.hashCode()) {
                return this.uri.equals(that.getURI()) && this.prefix.equals(that.getPrefix());
            }
        }
        return false;
    }
    
    @Override
    public String getText() {
        return this.uri;
    }
    
    @Override
    public String getStringValue() {
        return this.uri;
    }
    
    public String getPrefix() {
        return this.prefix;
    }
    
    public String getURI() {
        return this.uri;
    }
    
    public String getXPathNameStep() {
        if (this.prefix != null && !"".equals(this.prefix)) {
            return "namespace::" + this.prefix;
        }
        return "namespace::*[name()='']";
    }
    
    public String getPath(final Element context) {
        final StringBuffer path = new StringBuffer(10);
        final Element parent = this.getParent();
        if (parent != null && parent != context) {
            path.append(parent.getPath(context));
            path.append('/');
        }
        path.append(this.getXPathNameStep());
        return path.toString();
    }
    
    public String getUniquePath(final Element context) {
        final StringBuffer path = new StringBuffer(10);
        final Element parent = this.getParent();
        if (parent != null && parent != context) {
            path.append(parent.getUniquePath(context));
            path.append('/');
        }
        path.append(this.getXPathNameStep());
        return path.toString();
    }
    
    @Override
    public String toString() {
        return super.toString() + " [Namespace: prefix " + this.getPrefix() + " mapped to URI \"" + this.getURI() + "\"]";
    }
    
    public String asXML() {
        final StringBuffer asxml = new StringBuffer(10);
        final String pref = this.getPrefix();
        if (pref != null && pref.length() > 0) {
            asxml.append("xmlns:");
            asxml.append(pref);
            asxml.append("=\"");
        }
        else {
            asxml.append("xmlns=\"");
        }
        asxml.append(this.getURI());
        asxml.append("\"");
        return asxml.toString();
    }
    
    public void accept(final Visitor visitor) {
        visitor.visit(this);
    }
    
    @Override
    protected Node createXPathResult(final Element parent) {
        return new DefaultNamespace(parent, this.getPrefix(), this.getURI());
    }
    
    static {
        CACHE = new NamespaceCache();
        XML_NAMESPACE = Namespace.CACHE.get("xml", "http://www.w3.org/XML/1998/namespace");
        NO_NAMESPACE = Namespace.CACHE.get("", "");
    }
}
