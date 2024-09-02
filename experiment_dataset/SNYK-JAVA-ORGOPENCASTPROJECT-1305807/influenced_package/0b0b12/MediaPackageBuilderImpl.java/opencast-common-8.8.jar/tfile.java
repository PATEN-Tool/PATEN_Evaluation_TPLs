// 
// Decompiled by Procyon v0.5.36
// 

package org.opencastproject.mediapackage;

import org.slf4j.LoggerFactory;
import java.net.URISyntaxException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPath;
import java.net.URI;
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.NodeList;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import org.opencastproject.mediapackage.identifier.Id;
import org.slf4j.Logger;

public class MediaPackageBuilderImpl implements MediaPackageBuilder
{
    private static final Logger logger;
    protected MediaPackageSerializer serializer;
    
    public MediaPackageBuilderImpl() {
        this.serializer = null;
    }
    
    public MediaPackageBuilderImpl(final MediaPackageSerializer serializer) {
        this.serializer = null;
        if (serializer == null) {
            throw new IllegalArgumentException("Serializer may not be null");
        }
        this.serializer = serializer;
    }
    
    @Override
    public MediaPackage createNew() {
        return new MediaPackageImpl();
    }
    
    @Override
    public MediaPackage createNew(final Id identifier) {
        return new MediaPackageImpl(identifier);
    }
    
    @Override
    public MediaPackage loadFromXml(final InputStream is) throws MediaPackageException {
        if (this.serializer != null) {
            try {
                final Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
                rewriteUrls(xml, this.serializer);
                return MediaPackageImpl.valueOf(xml);
            }
            catch (Exception e) {
                throw new MediaPackageException("Error deserializing paths in media package", e);
            }
        }
        return MediaPackageImpl.valueOf(is);
    }
    
    @Override
    public MediaPackageSerializer getSerializer() {
        return this.serializer;
    }
    
    @Override
    public void setSerializer(final MediaPackageSerializer serializer) {
        this.serializer = serializer;
    }
    
    @Override
    public MediaPackage loadFromXml(final String xml) throws MediaPackageException {
        InputStream in = null;
        try {
            in = IOUtils.toInputStream(xml, "UTF-8");
            return this.loadFromXml(in);
        }
        catch (IOException e) {
            throw new MediaPackageException(e);
        }
        finally {
            IOUtils.closeQuietly(in);
        }
    }
    
    @Override
    public MediaPackage loadFromXml(final Node xml) throws MediaPackageException {
        if (this.serializer != null) {
            try {
                rewriteUrls(xml, this.serializer);
                return MediaPackageImpl.valueOf(xml);
            }
            catch (Exception e) {
                throw new MediaPackageException("Error deserializing paths in media package", e);
            }
        }
        return MediaPackageImpl.valueOf(xml);
    }
    
    private static void rewriteUrls(final Node xml, final MediaPackageSerializer serializer) throws XPathExpressionException, URISyntaxException {
        final XPath xPath = XPathFactory.newInstance().newXPath();
        final NodeList nodes = (NodeList)xPath.evaluate("//*[local-name() = 'url']", xml, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); ++i) {
            final Node uri = nodes.item(i).getFirstChild();
            if (uri != null) {
                final String uriStr = uri.getNodeValue();
                final String trimmedUriStr = uriStr.trim();
                if (!trimmedUriStr.equals(uriStr)) {
                    MediaPackageBuilderImpl.logger.warn("Detected invalid URI. Trying to fix it by removing spaces from beginning/end.");
                }
                uri.setNodeValue(serializer.decodeURI(new URI(trimmedUriStr)).toString());
            }
        }
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)MediaPackageBuilderImpl.class);
    }
}
