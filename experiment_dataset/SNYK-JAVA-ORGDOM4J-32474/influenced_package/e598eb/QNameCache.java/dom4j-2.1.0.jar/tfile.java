// 
// Decompiled by Procyon v0.5.36
// 

package org.dom4j.tree;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.WeakHashMap;
import org.dom4j.DocumentFactory;
import org.dom4j.Namespace;
import org.dom4j.QName;
import java.util.Map;

public class QNameCache
{
    protected Map<String, QName> noNamespaceCache;
    protected Map<Namespace, Map<String, QName>> namespaceCache;
    private DocumentFactory documentFactory;
    
    public QNameCache() {
        this.noNamespaceCache = Collections.synchronizedMap(new WeakHashMap<String, QName>());
        this.namespaceCache = Collections.synchronizedMap(new WeakHashMap<Namespace, Map<String, QName>>());
    }
    
    public QNameCache(final DocumentFactory documentFactory) {
        this.noNamespaceCache = Collections.synchronizedMap(new WeakHashMap<String, QName>());
        this.namespaceCache = Collections.synchronizedMap(new WeakHashMap<Namespace, Map<String, QName>>());
        this.documentFactory = documentFactory;
    }
    
    public List<QName> getQNames() {
        final List<QName> answer = new ArrayList<QName>();
        answer.addAll(this.noNamespaceCache.values());
        for (final Map<String, QName> map : this.namespaceCache.values()) {
            answer.addAll(map.values());
        }
        return answer;
    }
    
    public QName get(String name) {
        QName answer = null;
        if (name != null) {
            answer = this.noNamespaceCache.get(name);
        }
        else {
            name = "";
        }
        if (answer == null) {
            answer = this.createQName(name);
            answer.setDocumentFactory(this.documentFactory);
            this.noNamespaceCache.put(name, answer);
        }
        return answer;
    }
    
    public QName get(String name, final Namespace namespace) {
        final Map<String, QName> cache = this.getNamespaceCache(namespace);
        QName answer = null;
        if (name != null) {
            answer = cache.get(name);
        }
        else {
            name = "";
        }
        if (answer == null) {
            answer = this.createQName(name, namespace);
            answer.setDocumentFactory(this.documentFactory);
            cache.put(name, answer);
        }
        return answer;
    }
    
    public QName get(String localName, final Namespace namespace, final String qName) {
        final Map<String, QName> cache = this.getNamespaceCache(namespace);
        QName answer = null;
        if (localName != null) {
            answer = cache.get(localName);
        }
        else {
            localName = "";
        }
        if (answer == null) {
            answer = this.createQName(localName, namespace, qName);
            answer.setDocumentFactory(this.documentFactory);
            cache.put(localName, answer);
        }
        return answer;
    }
    
    public QName get(final String qualifiedName, final String uri) {
        final int index = qualifiedName.indexOf(58);
        if (index < 0) {
            return this.get(qualifiedName, Namespace.get(uri));
        }
        final String name = qualifiedName.substring(index + 1);
        final String prefix = qualifiedName.substring(0, index);
        return this.get(name, Namespace.get(prefix, uri));
    }
    
    public QName intern(final QName qname) {
        return this.get(qname.getName(), qname.getNamespace(), qname.getQualifiedName());
    }
    
    protected Map<String, QName> getNamespaceCache(final Namespace namespace) {
        if (namespace == Namespace.NO_NAMESPACE) {
            return this.noNamespaceCache;
        }
        Map<String, QName> answer = null;
        if (namespace != null) {
            answer = this.namespaceCache.get(namespace);
        }
        if (answer == null) {
            answer = this.createMap();
            this.namespaceCache.put(namespace, answer);
        }
        return answer;
    }
    
    protected Map<String, QName> createMap() {
        return Collections.synchronizedMap(new HashMap<String, QName>());
    }
    
    protected QName createQName(final String name) {
        return new QName(name);
    }
    
    protected QName createQName(final String name, final Namespace namespace) {
        return new QName(name, namespace);
    }
    
    protected QName createQName(final String name, final Namespace namespace, final String qualifiedName) {
        return new QName(name, namespace, qualifiedName);
    }
}
