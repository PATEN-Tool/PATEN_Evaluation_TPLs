// 
// Decompiled by Procyon v0.5.36
// 

package org.dom4j;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.regex.Pattern;
import org.dom4j.tree.QNameCache;
import org.dom4j.util.SingletonStrategy;
import java.io.Serializable;

public class QName implements Serializable
{
    private static SingletonStrategy<QNameCache> singleton;
    private static final String NAME_START_CHAR = "_A-Za-z\u00c0-\u00d6\u00d8-\u00f6\u00f8-\u02ff\u0370-\u037d\u037f-\u1fff\u200c-\u200d\u2070-\u218f\u2c00-\u2fef\u3001-\ud7ff\uf900-\ufdcf\ufdf0-\ufffd";
    private static final String NAME_CHAR = "_A-Za-z\u00c0-\u00d6\u00d8-\u00f6\u00f8-\u02ff\u0370-\u037d\u037f-\u1fff\u200c-\u200d\u2070-\u218f\u2c00-\u2fef\u3001-\ud7ff\uf900-\ufdcf\ufdf0-\ufffd-.0-9·\u0300-\u036f\u203f-\u2040";
    private static final String NCNAME = "[_A-Za-z\u00c0-\u00d6\u00d8-\u00f6\u00f8-\u02ff\u0370-\u037d\u037f-\u1fff\u200c-\u200d\u2070-\u218f\u2c00-\u2fef\u3001-\ud7ff\uf900-\ufdcf\ufdf0-\ufffd][_A-Za-z\u00c0-\u00d6\u00d8-\u00f6\u00f8-\u02ff\u0370-\u037d\u037f-\u1fff\u200c-\u200d\u2070-\u218f\u2c00-\u2fef\u3001-\ud7ff\uf900-\ufdcf\ufdf0-\ufffd-.0-9·\u0300-\u036f\u203f-\u2040]*";
    private static final Pattern RE_NAME;
    private static final Pattern RE_NCNAME;
    private static final Pattern RE_QNAME;
    private String name;
    private String qualifiedName;
    private transient Namespace namespace;
    private int hashCode;
    private DocumentFactory documentFactory;
    
    public QName(final String name) {
        this(name, Namespace.NO_NAMESPACE);
    }
    
    public QName(final String name, final Namespace namespace) {
        this.name = ((name == null) ? "" : name);
        this.namespace = ((namespace == null) ? Namespace.NO_NAMESPACE : namespace);
        if (this.namespace.equals(Namespace.NO_NAMESPACE)) {
            validateName(this.name);
        }
        else {
            validateNCName(this.name);
        }
    }
    
    public QName(final String name, final Namespace namespace, final String qualifiedName) {
        this.name = ((name == null) ? "" : name);
        this.qualifiedName = qualifiedName;
        this.namespace = ((namespace == null) ? Namespace.NO_NAMESPACE : namespace);
        validateNCName(this.name);
        validateQName(this.qualifiedName);
    }
    
    public static QName get(final String name) {
        return getCache().get(name);
    }
    
    public static QName get(final String name, final Namespace namespace) {
        return getCache().get(name, namespace);
    }
    
    public static QName get(final String name, final String prefix, final String uri) {
        if ((prefix == null || prefix.length() == 0) && uri == null) {
            return get(name);
        }
        if (prefix == null || prefix.length() == 0) {
            return getCache().get(name, Namespace.get(uri));
        }
        if (uri == null) {
            return get(name);
        }
        return getCache().get(name, Namespace.get(prefix, uri));
    }
    
    public static QName get(final String qualifiedName, final String uri) {
        if (uri == null) {
            return getCache().get(qualifiedName);
        }
        return getCache().get(qualifiedName, uri);
    }
    
    public static QName get(final String localName, final Namespace namespace, final String qualifiedName) {
        return getCache().get(localName, namespace, qualifiedName);
    }
    
    public String getName() {
        return this.name;
    }
    
    public String getQualifiedName() {
        if (this.qualifiedName == null) {
            final String prefix = this.getNamespacePrefix();
            if (prefix != null && prefix.length() > 0) {
                this.qualifiedName = prefix + ":" + this.name;
            }
            else {
                this.qualifiedName = this.name;
            }
        }
        return this.qualifiedName;
    }
    
    public Namespace getNamespace() {
        return this.namespace;
    }
    
    public String getNamespacePrefix() {
        if (this.namespace == null) {
            return "";
        }
        return this.namespace.getPrefix();
    }
    
    public String getNamespaceURI() {
        if (this.namespace == null) {
            return "";
        }
        return this.namespace.getURI();
    }
    
    @Override
    public int hashCode() {
        if (this.hashCode == 0) {
            this.hashCode = (this.getName().hashCode() ^ this.getNamespaceURI().hashCode());
            if (this.hashCode == 0) {
                this.hashCode = 47806;
            }
        }
        return this.hashCode;
    }
    
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof QName) {
            final QName that = (QName)object;
            if (this.hashCode() == that.hashCode()) {
                return this.getName().equals(that.getName()) && this.getNamespaceURI().equals(that.getNamespaceURI());
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        return super.toString() + " [name: " + this.getName() + " namespace: \"" + this.getNamespace() + "\"]";
    }
    
    public DocumentFactory getDocumentFactory() {
        return this.documentFactory;
    }
    
    public void setDocumentFactory(final DocumentFactory documentFactory) {
        this.documentFactory = documentFactory;
    }
    
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(this.namespace.getPrefix());
        out.writeObject(this.namespace.getURI());
    }
    
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        final String prefix = (String)in.readObject();
        final String uri = (String)in.readObject();
        this.namespace = Namespace.get(prefix, uri);
    }
    
    private static QNameCache getCache() {
        final QNameCache cache = QName.singleton.instance();
        return cache;
    }
    
    private static void validateName(final String name) {
        if (!QName.RE_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException(String.format("Illegal character in name: '%s'.", name));
        }
    }
    
    protected static void validateNCName(final String ncname) {
        if (!QName.RE_NCNAME.matcher(ncname).matches()) {
            throw new IllegalArgumentException(String.format("Illegal character in local name: '%s'.", ncname));
        }
    }
    
    private static void validateQName(final String qname) {
        if (!QName.RE_QNAME.matcher(qname).matches()) {
            throw new IllegalArgumentException(String.format("Illegal character in qualified name: '%s'.", qname));
        }
    }
    
    static {
        QName.singleton = null;
        RE_NAME = Pattern.compile("[:_A-Za-z\u00c0-\u00d6\u00d8-\u00f6\u00f8-\u02ff\u0370-\u037d\u037f-\u1fff\u200c-\u200d\u2070-\u218f\u2c00-\u2fef\u3001-\ud7ff\uf900-\ufdcf\ufdf0-\ufffd][:_A-Za-z\u00c0-\u00d6\u00d8-\u00f6\u00f8-\u02ff\u0370-\u037d\u037f-\u1fff\u200c-\u200d\u2070-\u218f\u2c00-\u2fef\u3001-\ud7ff\uf900-\ufdcf\ufdf0-\ufffd-.0-9·\u0300-\u036f\u203f-\u2040]*");
        RE_NCNAME = Pattern.compile("[_A-Za-z\u00c0-\u00d6\u00d8-\u00f6\u00f8-\u02ff\u0370-\u037d\u037f-\u1fff\u200c-\u200d\u2070-\u218f\u2c00-\u2fef\u3001-\ud7ff\uf900-\ufdcf\ufdf0-\ufffd][_A-Za-z\u00c0-\u00d6\u00d8-\u00f6\u00f8-\u02ff\u0370-\u037d\u037f-\u1fff\u200c-\u200d\u2070-\u218f\u2c00-\u2fef\u3001-\ud7ff\uf900-\ufdcf\ufdf0-\ufffd-.0-9·\u0300-\u036f\u203f-\u2040]*");
        RE_QNAME = Pattern.compile("(?:[_A-Za-z\u00c0-\u00d6\u00d8-\u00f6\u00f8-\u02ff\u0370-\u037d\u037f-\u1fff\u200c-\u200d\u2070-\u218f\u2c00-\u2fef\u3001-\ud7ff\uf900-\ufdcf\ufdf0-\ufffd][_A-Za-z\u00c0-\u00d6\u00d8-\u00f6\u00f8-\u02ff\u0370-\u037d\u037f-\u1fff\u200c-\u200d\u2070-\u218f\u2c00-\u2fef\u3001-\ud7ff\uf900-\ufdcf\ufdf0-\ufffd-.0-9·\u0300-\u036f\u203f-\u2040]*:)?[_A-Za-z\u00c0-\u00d6\u00d8-\u00f6\u00f8-\u02ff\u0370-\u037d\u037f-\u1fff\u200c-\u200d\u2070-\u218f\u2c00-\u2fef\u3001-\ud7ff\uf900-\ufdcf\ufdf0-\ufffd][_A-Za-z\u00c0-\u00d6\u00d8-\u00f6\u00f8-\u02ff\u0370-\u037d\u037f-\u1fff\u200c-\u200d\u2070-\u218f\u2c00-\u2fef\u3001-\ud7ff\uf900-\ufdcf\ufdf0-\ufffd-.0-9·\u0300-\u036f\u203f-\u2040]*");
        try {
            final String defaultSingletonClass = "org.dom4j.util.SimpleSingleton";
            Class<SingletonStrategy> clazz = null;
            try {
                String singletonClass = defaultSingletonClass;
                singletonClass = System.getProperty("org.dom4j.QName.singleton.strategy", singletonClass);
                clazz = (Class<SingletonStrategy>)Class.forName(singletonClass);
            }
            catch (Exception exc1) {
                try {
                    final String singletonClass2 = defaultSingletonClass;
                    clazz = (Class<SingletonStrategy>)Class.forName(singletonClass2);
                }
                catch (Exception ex) {}
            }
            (QName.singleton = clazz.newInstance()).setSingletonClassName(QNameCache.class.getName());
        }
        catch (Exception ex2) {}
    }
}
