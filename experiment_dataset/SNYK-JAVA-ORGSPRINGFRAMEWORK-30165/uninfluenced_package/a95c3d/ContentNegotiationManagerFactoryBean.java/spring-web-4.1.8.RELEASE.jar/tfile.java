// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.web.accept;

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import org.springframework.util.Assert;
import java.util.Iterator;
import java.util.Locale;
import org.springframework.util.CollectionUtils;
import java.util.Properties;
import java.util.HashMap;
import javax.servlet.ServletContext;
import org.springframework.http.MediaType;
import java.util.Map;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.context.ServletContextAware;
import org.springframework.beans.factory.FactoryBean;

public class ContentNegotiationManagerFactoryBean implements FactoryBean<ContentNegotiationManager>, ServletContextAware, InitializingBean
{
    private boolean favorPathExtension;
    private boolean favorParameter;
    private boolean ignoreAcceptHeader;
    private Map<String, MediaType> mediaTypes;
    private boolean ignoreUnknownPathExtensions;
    private Boolean useJaf;
    private String parameterName;
    private ContentNegotiationStrategy defaultNegotiationStrategy;
    private ContentNegotiationManager contentNegotiationManager;
    private ServletContext servletContext;
    
    public ContentNegotiationManagerFactoryBean() {
        this.favorPathExtension = true;
        this.favorParameter = false;
        this.ignoreAcceptHeader = false;
        this.mediaTypes = new HashMap<String, MediaType>();
        this.ignoreUnknownPathExtensions = true;
        this.parameterName = "format";
    }
    
    public void setFavorPathExtension(final boolean favorPathExtension) {
        this.favorPathExtension = favorPathExtension;
    }
    
    public void setMediaTypes(final Properties mediaTypes) {
        if (!CollectionUtils.isEmpty((Map)mediaTypes)) {
            for (final Map.Entry<Object, Object> entry : mediaTypes.entrySet()) {
                final String extension = entry.getKey().toLowerCase(Locale.ENGLISH);
                this.mediaTypes.put(extension, MediaType.valueOf(entry.getValue()));
            }
        }
    }
    
    public void addMediaType(final String fileExtension, final MediaType mediaType) {
        this.mediaTypes.put(fileExtension, mediaType);
    }
    
    public void addMediaTypes(final Map<String, MediaType> mediaTypes) {
        if (mediaTypes != null) {
            this.mediaTypes.putAll(mediaTypes);
        }
    }
    
    public void setIgnoreUnknownPathExtensions(final boolean ignoreUnknownPathExtensions) {
        this.ignoreUnknownPathExtensions = ignoreUnknownPathExtensions;
    }
    
    public void setUseJaf(final boolean useJaf) {
        this.useJaf = useJaf;
    }
    
    private boolean isUseJafTurnedOff() {
        return this.useJaf != null && !this.useJaf;
    }
    
    public void setFavorParameter(final boolean favorParameter) {
        this.favorParameter = favorParameter;
    }
    
    public void setParameterName(final String parameterName) {
        Assert.notNull((Object)parameterName, "parameterName is required");
        this.parameterName = parameterName;
    }
    
    public void setIgnoreAcceptHeader(final boolean ignoreAcceptHeader) {
        this.ignoreAcceptHeader = ignoreAcceptHeader;
    }
    
    public void setDefaultContentType(final MediaType defaultContentType) {
        this.defaultNegotiationStrategy = new FixedContentNegotiationStrategy(defaultContentType);
    }
    
    public void setDefaultContentTypeStrategy(final ContentNegotiationStrategy defaultStrategy) {
        this.defaultNegotiationStrategy = defaultStrategy;
    }
    
    public void setServletContext(final ServletContext servletContext) {
        this.servletContext = servletContext;
    }
    
    public void afterPropertiesSet() {
        final List<ContentNegotiationStrategy> strategies = new ArrayList<ContentNegotiationStrategy>();
        if (this.favorPathExtension) {
            PathExtensionContentNegotiationStrategy strategy;
            if (this.servletContext != null && !this.isUseJafTurnedOff()) {
                strategy = new ServletPathExtensionContentNegotiationStrategy(this.servletContext, this.mediaTypes);
            }
            else {
                strategy = new PathExtensionContentNegotiationStrategy(this.mediaTypes);
            }
            strategy.setIgnoreUnknownExtensions(this.ignoreUnknownPathExtensions);
            if (this.useJaf != null) {
                strategy.setUseJaf(this.useJaf);
            }
            strategies.add(strategy);
        }
        if (this.favorParameter) {
            final ParameterContentNegotiationStrategy strategy2 = new ParameterContentNegotiationStrategy(this.mediaTypes);
            strategy2.setParameterName(this.parameterName);
            strategies.add(strategy2);
        }
        if (!this.ignoreAcceptHeader) {
            strategies.add(new HeaderContentNegotiationStrategy());
        }
        if (this.defaultNegotiationStrategy != null) {
            strategies.add(this.defaultNegotiationStrategy);
        }
        this.contentNegotiationManager = new ContentNegotiationManager(strategies);
    }
    
    public ContentNegotiationManager getObject() {
        return this.contentNegotiationManager;
    }
    
    public Class<?> getObjectType() {
        return ContentNegotiationManager.class;
    }
    
    public boolean isSingleton() {
        return true;
    }
}
