// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.web.accept;

import java.io.InputStream;
import org.springframework.core.io.Resource;
import java.io.IOException;
import javax.activation.MimetypesFileTypeMap;
import org.springframework.core.io.ClassPathResource;
import javax.activation.FileTypeMap;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ClassUtils;
import java.util.Locale;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.http.MediaType;
import java.util.Map;
import org.springframework.web.util.UrlPathHelper;
import org.apache.commons.logging.Log;

public class PathExtensionContentNegotiationStrategy extends AbstractMappingContentNegotiationStrategy
{
    private static final boolean JAF_PRESENT;
    private static final Log logger;
    private static final UrlPathHelper urlPathHelper;
    private boolean useJaf;
    
    public PathExtensionContentNegotiationStrategy(final Map<String, MediaType> mediaTypes) {
        super(mediaTypes);
        this.useJaf = true;
    }
    
    public PathExtensionContentNegotiationStrategy() {
        super(null);
        this.useJaf = true;
    }
    
    public void setUseJaf(final boolean useJaf) {
        this.useJaf = useJaf;
    }
    
    @Override
    protected String getMediaTypeKey(final NativeWebRequest webRequest) {
        final HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        if (servletRequest == null) {
            PathExtensionContentNegotiationStrategy.logger.warn((Object)"An HttpServletRequest is required to determine the media type key");
            return null;
        }
        final String path = PathExtensionContentNegotiationStrategy.urlPathHelper.getLookupPathForRequest(servletRequest);
        final String filename = WebUtils.extractFullFilenameFromUrlPath(path);
        final String extension = StringUtils.getFilenameExtension(filename);
        return StringUtils.hasText(extension) ? extension.toLowerCase(Locale.ENGLISH) : null;
    }
    
    @Override
    protected void handleMatch(final String extension, final MediaType mediaType) {
    }
    
    @Override
    protected MediaType handleNoMatch(final NativeWebRequest webRequest, final String extension) {
        if (this.useJaf && PathExtensionContentNegotiationStrategy.JAF_PRESENT) {
            final MediaType jafMediaType = JafMediaTypeFactory.getMediaType("file." + extension);
            if (jafMediaType != null && !MediaType.APPLICATION_OCTET_STREAM.equals(jafMediaType)) {
                return jafMediaType;
            }
        }
        return null;
    }
    
    static {
        JAF_PRESENT = ClassUtils.isPresent("javax.activation.FileTypeMap", PathExtensionContentNegotiationStrategy.class.getClassLoader());
        logger = LogFactory.getLog((Class)PathExtensionContentNegotiationStrategy.class);
        (urlPathHelper = new UrlPathHelper()).setUrlDecode(false);
    }
    
    private static class JafMediaTypeFactory
    {
        private static final FileTypeMap fileTypeMap;
        
        private static FileTypeMap initFileTypeMap() {
            final Resource resource = (Resource)new ClassPathResource("org/springframework/mail/javamail/mime.types");
            if (resource.exists()) {
                if (PathExtensionContentNegotiationStrategy.logger.isTraceEnabled()) {
                    PathExtensionContentNegotiationStrategy.logger.trace((Object)("Loading Java Activation Framework FileTypeMap from " + resource));
                }
                InputStream inputStream = null;
                try {
                    inputStream = resource.getInputStream();
                    return new MimetypesFileTypeMap(inputStream);
                }
                catch (IOException ex) {}
                finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        }
                        catch (IOException ex2) {}
                    }
                }
            }
            if (PathExtensionContentNegotiationStrategy.logger.isTraceEnabled()) {
                PathExtensionContentNegotiationStrategy.logger.trace((Object)"Loading default Java Activation Framework FileTypeMap");
            }
            return FileTypeMap.getDefaultFileTypeMap();
        }
        
        public static MediaType getMediaType(final String filename) {
            final String mediaType = JafMediaTypeFactory.fileTypeMap.getContentType(filename);
            return StringUtils.hasText(mediaType) ? MediaType.parseMediaType(mediaType) : null;
        }
        
        static {
            fileTypeMap = initFileTypeMap();
        }
    }
}
