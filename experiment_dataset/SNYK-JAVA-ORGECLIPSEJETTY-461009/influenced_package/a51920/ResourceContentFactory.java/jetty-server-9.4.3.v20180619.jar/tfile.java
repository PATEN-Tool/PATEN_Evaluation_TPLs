// 
// Decompiled by Procyon v0.5.36
// 

package org.eclipse.jetty.server;

import java.util.Map;
import java.util.HashMap;
import org.eclipse.jetty.http.ResourceHttpContent;
import java.io.IOException;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.http.CompressedContentFormat;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.http.HttpContent;

public class ResourceContentFactory implements HttpContent.ContentFactory
{
    private final ResourceFactory _factory;
    private final MimeTypes _mimeTypes;
    private final CompressedContentFormat[] _precompressedFormats;
    
    public ResourceContentFactory(final ResourceFactory factory, final MimeTypes mimeTypes, final CompressedContentFormat[] precompressedFormats) {
        this._factory = factory;
        this._mimeTypes = mimeTypes;
        this._precompressedFormats = precompressedFormats;
    }
    
    public HttpContent getContent(final String pathInContext, final int maxBufferSize) throws IOException {
        final Resource resource = this._factory.getResource(pathInContext);
        final HttpContent loaded = this.load(pathInContext, resource, maxBufferSize);
        return loaded;
    }
    
    private HttpContent load(final String pathInContext, final Resource resource, final int maxBufferSize) throws IOException {
        if (resource == null || !resource.exists()) {
            return null;
        }
        if (resource.isDirectory()) {
            return (HttpContent)new ResourceHttpContent(resource, this._mimeTypes.getMimeByExtension(resource.toString()), maxBufferSize);
        }
        final String mt = this._mimeTypes.getMimeByExtension(pathInContext);
        if (this._precompressedFormats.length > 0) {
            final Map<CompressedContentFormat, HttpContent> compressedContents = new HashMap<CompressedContentFormat, HttpContent>(this._precompressedFormats.length);
            for (final CompressedContentFormat format : this._precompressedFormats) {
                final String compressedPathInContext = pathInContext + format._extension;
                final Resource compressedResource = this._factory.getResource(compressedPathInContext);
                if (compressedResource.exists() && compressedResource.lastModified() >= resource.lastModified() && compressedResource.length() < resource.length()) {
                    compressedContents.put(format, (HttpContent)new ResourceHttpContent(compressedResource, this._mimeTypes.getMimeByExtension(compressedPathInContext), maxBufferSize));
                }
            }
            if (!compressedContents.isEmpty()) {
                return (HttpContent)new ResourceHttpContent(resource, mt, maxBufferSize, (Map)compressedContents);
            }
        }
        return (HttpContent)new ResourceHttpContent(resource, mt, maxBufferSize);
    }
    
    @Override
    public String toString() {
        return "ResourceContentFactory[" + this._factory + "]@" + this.hashCode();
    }
}
