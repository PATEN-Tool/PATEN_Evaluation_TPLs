// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.uima.adapter.vinci.util;

import org.xml.sax.Attributes;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.uima.util.Level;
import org.apache.uima.UIMAFramework;
import org.apache.uima.internal.util.XMLUtils;
import org.w3c.dom.Document;
import org.xml.sax.helpers.DefaultHandler;

public class Descriptor extends DefaultHandler
{
    private String serviceName;
    private int instanceCount;
    private String resourceSpecifierPath;
    private String filterString;
    private String namingServiceHost;
    private int serverSocketTimeout;
    private int threadPoolMinSize;
    private int threadPoolMaxSize;
    
    public Descriptor(final String filePath) {
        this.serviceName = "";
        this.instanceCount = 0;
        this.resourceSpecifierPath = "";
        this.filterString = "";
        this.namingServiceHost = "localhost";
        this.serverSocketTimeout = 300000;
        this.threadPoolMinSize = 1;
        this.threadPoolMaxSize = 20;
        try {
            this.parse(filePath);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private Document parse(final String configFile) {
        final Document doc = null;
        try {
            final SAXParserFactory factory = XMLUtils.createSAXParserFactory();
            factory.setValidating(false);
            final SAXParser parser = factory.newSAXParser();
            parser.parse(configFile, this);
            UIMAFramework.getLogger().log(Level.CONFIG, "Resource::" + this.getResourceSpecifierPath());
            UIMAFramework.getLogger().log(Level.CONFIG, "Instance Count::" + this.getInstanceCount());
            UIMAFramework.getLogger().log(Level.CONFIG, "Service Name::" + this.getServiceName());
            UIMAFramework.getLogger().log(Level.CONFIG, "Filter String::" + this.getFilterString());
            UIMAFramework.getLogger().log(Level.CONFIG, "Naming Service Host::" + this.getNamingServiceHost());
            UIMAFramework.getLogger().log(Level.CONFIG, "Server Socket Timeout::" + this.getServerSocketTimeout());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return doc;
    }
    
    private String getAttribute(final String attrName, final Attributes attribs) {
        String attributeValue = null;
        for (int i = 0; attribs != null && i < attribs.getLength(); ++i) {
            final String attributeName = this.getName(attribs.getLocalName(i), attribs.getQName(i));
            if (attrName.equals(attributeName)) {
                attributeValue = attribs.getValue(i);
            }
        }
        return attributeValue;
    }
    
    private String getName(final String s1, final String s2) {
        if (s1 == null || "".equals(s1)) {
            return s2;
        }
        return s1;
    }
    
    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attribs) {
        final String elementName = this.getName(localName, qName);
        if (elementName.equals("service")) {
            this.setServiceName(this.getAttribute("name", attribs));
        }
        else if (elementName.equals("parameter")) {
            final String att = this.getAttribute("name", attribs);
            final String value = this.getAttribute("value", attribs);
            if ("resourceSpecifierPath".equals(att)) {
                this.setResourceSpecifierPath(value);
            }
            else if ("filterString".equals(att)) {
                this.setFilterString(value);
            }
            else if ("numInstances".equals(att)) {
                try {
                    this.setInstanceCount(Integer.parseInt(value));
                }
                catch (NumberFormatException nbe) {
                    this.setInstanceCount(1);
                }
            }
            else if ("serverSocketTimeout".equals(att)) {
                try {
                    this.setServerSocketTimeout(Integer.parseInt(value));
                }
                catch (NumberFormatException nbe) {
                    this.setServerSocketTimeout(300000);
                }
            }
            else if ("namingServiceHost".equals(att)) {
                try {
                    this.setNamingServiceHost(value);
                }
                catch (NumberFormatException nbe) {
                    this.setNamingServiceHost("localhost");
                }
            }
            else if ("threadPoolMinSize".equals(att)) {
                try {
                    this.setThreadPoolMinSize(Integer.parseInt(value));
                }
                catch (NumberFormatException nbe) {
                    this.setThreadPoolMinSize(1);
                }
            }
            else if ("threadPoolMaxSize".equals(att)) {
                try {
                    this.setThreadPoolMaxSize(Integer.parseInt(value));
                }
                catch (NumberFormatException nbe) {
                    this.setThreadPoolMaxSize(20);
                }
            }
        }
    }
    
    public int getThreadPoolMaxSize() {
        return this.threadPoolMaxSize;
    }
    
    public void setThreadPoolMaxSize(final int threadPoolMaxSize) {
        this.threadPoolMaxSize = threadPoolMaxSize;
    }
    
    public int getThreadPoolMinSize() {
        return this.threadPoolMinSize;
    }
    
    public void setThreadPoolMinSize(final int threadPoolMinSize) {
        this.threadPoolMinSize = threadPoolMinSize;
    }
    
    public int getInstanceCount() {
        return this.instanceCount;
    }
    
    public String getResourceSpecifierPath() {
        return this.resourceSpecifierPath;
    }
    
    public String getServiceName() {
        return this.serviceName;
    }
    
    public void setInstanceCount(final int instanceCount) {
        this.instanceCount = instanceCount;
    }
    
    public void setResourceSpecifierPath(final String resourceSpecifierPath) {
        this.resourceSpecifierPath = resourceSpecifierPath;
    }
    
    public void setServiceName(final String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getFilterString() {
        return this.filterString;
    }
    
    public void setFilterString(final String filterString) {
        this.filterString = filterString;
    }
    
    public String getNamingServiceHost() {
        return this.namingServiceHost;
    }
    
    public void setNamingServiceHost(final String namingServiceHost) {
        this.namingServiceHost = namingServiceHost;
    }
    
    public int getServerSocketTimeout() {
        return this.serverSocketTimeout;
    }
    
    public void setServerSocketTimeout(final int serverSocketTimeout) {
        this.serverSocketTimeout = serverSocketTimeout;
    }
}
