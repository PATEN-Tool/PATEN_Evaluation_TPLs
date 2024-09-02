// 
// Decompiled by Procyon v0.5.36
// 

package org.xmlbeam.config;

import javax.xml.xpath.XPathFactory;
import java.util.Iterator;
import java.util.Map;
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

public class DefaultXMLFactoriesConfig implements XMLFactoriesConfig
{
    private static final String NON_EXISTING_URL = "http://xmlbeam.org/nonexisting_namespace";
    private NamespacePhilosophy namespacePhilosophy;
    private boolean isPrettyPrinting;
    private boolean isOmitXMLDeclaration;
    
    public DefaultXMLFactoriesConfig() {
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
                return "http://xmlbeam.org/nonexisting_namespace";
            }
            
            @Override
            public String getPrefix(final String uri) {
                for (final Map.Entry<String, String> e : nameSpaceMapping.entrySet()) {
                    if (e.getValue().equals(uri)) {
                        return e.getKey();
                    }
                }
                return null;
            }
            
            @Override
            public Iterator<String> getPrefixes(final String val) {
                return nameSpaceMapping.keySet().iterator();
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
    
    public enum NamespacePhilosophy
    {
        NIHILISTIC, 
        AGNOSTIC, 
        HEDONISTIC;
    }
}
