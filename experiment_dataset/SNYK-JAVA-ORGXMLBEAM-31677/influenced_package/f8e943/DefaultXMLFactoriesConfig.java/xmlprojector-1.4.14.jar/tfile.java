// 
// Decompiled by Procyon v0.5.36
// 

package org.xmlbeam.config;

import java.util.Collections;
import javax.xml.xpath.XPathFactory;
import org.xmlbeam.util.UnionIterator;
import java.util.Iterator;
import javax.xml.namespace.NamespaceContext;
import org.xmlbeam.util.intern.DOMHelper;
import javax.xml.xpath.XPath;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.Transformer;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import java.util.TreeMap;
import java.util.Map;

public class DefaultXMLFactoriesConfig implements XMLFactoriesConfig
{
    private static final String NON_EXISTING_URL = "http://xmlbeam.org/nonexisting_namespace";
    private final Map<String, String> USER_DEFINED_MAPPING;
    private NamespacePhilosophy namespacePhilosophy;
    private boolean isPrettyPrinting;
    private boolean isOmitXMLDeclaration;
    
    public DefaultXMLFactoriesConfig() {
        this.USER_DEFINED_MAPPING = new TreeMap<String, String>();
        this.namespacePhilosophy = NamespacePhilosophy.HEDONISTIC;
        this.isPrettyPrinting = true;
        this.isOmitXMLDeclaration = true;
    }
    
    @Override
    public DocumentBuilder createDocumentBuilder() {
        try {
            final DocumentBuilder documentBuilder = this.createDocumentBuilderFactory().newDocumentBuilder();
            return documentBuilder;
        }
        catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public DocumentBuilderFactory createDocumentBuilderFactory() {
        final DocumentBuilderFactory instance = DocumentBuilderFactory.newInstance();
        if (!NamespacePhilosophy.AGNOSTIC.equals(this.namespacePhilosophy)) {
            instance.setNamespaceAware(NamespacePhilosophy.HEDONISTIC.equals(this.namespacePhilosophy));
        }
        return instance;
    }
    
    @Override
    public Transformer createTransformer(final Document... document) {
        try {
            final Transformer transformer = this.createTransformerFactory().newTransformer();
            if (this.isPrettyPrinting()) {
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                transformer.setOutputProperty("indent", "yes");
            }
            if (this.isOmitXMLDeclaration()) {
                transformer.setOutputProperty("omit-xml-declaration", "yes");
            }
            return transformer;
        }
        catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public TransformerFactory createTransformerFactory() {
        return TransformerFactory.newInstance();
    }
    
    @Override
    public XPath createXPath(final Document... document) {
        final XPath xPath = this.createXPathFactory().newXPath();
        if (document == null || document.length == 0 || !NamespacePhilosophy.HEDONISTIC.equals(this.namespacePhilosophy)) {
            return xPath;
        }
        final Map<String, String> nameSpaceMapping = DOMHelper.getNamespaceMapping(document[0]);
        final NamespaceContext ctx = new NamespaceContext() {
            @Override
            public String getNamespaceURI(final String prefix) {
                if (prefix == null) {
                    throw new IllegalArgumentException("null not allowed as prefix");
                }
                if (nameSpaceMapping.containsKey(prefix)) {
                    return nameSpaceMapping.get(prefix);
                }
                if (DefaultXMLFactoriesConfig.this.USER_DEFINED_MAPPING.containsKey(prefix)) {
                    return DefaultXMLFactoriesConfig.this.USER_DEFINED_MAPPING.get(prefix);
                }
                return "http://xmlbeam.org/nonexisting_namespace";
            }
            
            @Override
            public String getPrefix(final String uri) {
                for (final Map.Entry<String, String> e : nameSpaceMapping.entrySet()) {
                    if (e.getValue().equals(uri)) {
                        return e.getKey();
                    }
                }
                for (final Map.Entry<String, String> e : DefaultXMLFactoriesConfig.this.USER_DEFINED_MAPPING.entrySet()) {
                    if (e.getValue().equals(uri)) {
                        return e.getKey();
                    }
                }
                return null;
            }
            
            @Override
            public Iterator<String> getPrefixes(final String val) {
                return new UnionIterator<String>(nameSpaceMapping.keySet().iterator(), DefaultXMLFactoriesConfig.this.USER_DEFINED_MAPPING.keySet().iterator());
            }
        };
        xPath.setNamespaceContext(ctx);
        return xPath;
    }
    
    @Override
    public XPathFactory createXPathFactory() {
        return XPathFactory.newInstance();
    }
    
    public NamespacePhilosophy getNamespacePhilosophy() {
        return this.namespacePhilosophy;
    }
    
    public boolean isPrettyPrinting() {
        return this.isPrettyPrinting;
    }
    
    public XMLFactoriesConfig setNamespacePhilosophy(final NamespacePhilosophy namespacePhilosophy) {
        this.namespacePhilosophy = namespacePhilosophy;
        return this;
    }
    
    public DefaultXMLFactoriesConfig setPrettyPrinting(final boolean on) {
        this.isPrettyPrinting = on;
        return this;
    }
    
    public boolean isOmitXMLDeclaration() {
        return this.isOmitXMLDeclaration;
    }
    
    public DefaultXMLFactoriesConfig setOmitXMLDeclaration(final boolean isOmitXMLDeclaration) {
        this.isOmitXMLDeclaration = isOmitXMLDeclaration;
        return this;
    }
    
    public NSMapping createNameSpaceMapping() {
        if (!NamespacePhilosophy.HEDONISTIC.equals(this.namespacePhilosophy)) {
            throw new IllegalStateException("To use a namespace mapping, you need to use the HEDONISTIC NamespacePhilosophy.");
        }
        return new NSMapping() {
            @Override
            public NSMapping add(final String prefix, final String uri) {
                if (prefix == null || prefix.isEmpty()) {
                    throw new IllegalArgumentException("prefix must not be empty");
                }
                if (uri == null || uri.isEmpty()) {
                    throw new IllegalArgumentException("uri must not be empty");
                }
                if (DefaultXMLFactoriesConfig.this.USER_DEFINED_MAPPING.containsKey(prefix) && !uri.equals(DefaultXMLFactoriesConfig.this.USER_DEFINED_MAPPING.get(prefix))) {
                    throw new IllegalArgumentException("The prefix '" + prefix + "' is bound to namespace '" + DefaultXMLFactoriesConfig.this.USER_DEFINED_MAPPING.get(prefix) + " already.");
                }
                DefaultXMLFactoriesConfig.this.USER_DEFINED_MAPPING.put(prefix, uri);
                return this;
            }
            
            @Override
            public NSMapping addDefaultNamespace(final String uri) {
                return this.add("xbdefaultns", uri);
            }
        };
    }
    
    @Override
    public Map<String, String> getUserDefinedNamespaceMapping() {
        return Collections.unmodifiableMap((Map<? extends String, ? extends String>)this.USER_DEFINED_MAPPING);
    }
    
    public enum NamespacePhilosophy
    {
        NIHILISTIC, 
        AGNOSTIC, 
        HEDONISTIC;
    }
    
    public interface NSMapping
    {
        NSMapping add(final String p0, final String p1);
        
        NSMapping addDefaultNamespace(final String p0);
    }
}
