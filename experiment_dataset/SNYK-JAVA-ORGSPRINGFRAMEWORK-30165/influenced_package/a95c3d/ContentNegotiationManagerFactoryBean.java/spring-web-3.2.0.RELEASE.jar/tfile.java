// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.web.accept;

import java.util.Hashtable;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.CollectionUtils;
import javax.servlet.ServletContext;
import org.springframework.http.MediaType;
import java.util.Properties;
import org.springframework.web.context.ServletContextAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.FactoryBean;

public class ContentNegotiationManagerFactoryBean implements FactoryBean<ContentNegotiationManager>, InitializingBean, ServletContextAware
{
    private boolean favorPathExtension;
    private boolean favorParameter;
    private boolean ignoreAcceptHeader;
    private Properties mediaTypes;
    private Boolean useJaf;
    private String parameterName;
    private MediaType defaultContentType;
    private ContentNegotiationManager contentNegotiationManager;
    private ServletContext servletContext;
    
    public ContentNegotiationManagerFactoryBean() {
        this.favorPathExtension = true;
        this.favorParameter = false;
        this.ignoreAcceptHeader = false;
        this.mediaTypes = new Properties();
    }
    
    public void setFavorPathExtension(final boolean favorPathExtension) {
        this.favorPathExtension = favorPathExtension;
    }
    
    public void setMediaTypes(final Properties mediaTypes) {
        if (!CollectionUtils.isEmpty((Map)mediaTypes)) {
            for (final Map.Entry<Object, Object> entry : mediaTypes.entrySet()) {
                final String extension = entry.getKey().toLowerCase(Locale.ENGLISH);
                ((Hashtable<String, MediaType>)this.mediaTypes).put(extension, MediaType.valueOf(entry.getValue()));
            }
        }
    }
    
    public Properties getMediaTypes() {
        return this.mediaTypes;
    }
    
    public void setUseJaf(final boolean useJaf) {
        this.useJaf = useJaf;
    }
    
    public void setFavorParameter(final boolean favorParameter) {
        this.favorParameter = favorParameter;
    }
    
    public void setParameterName(final String parameterName) {
        this.parameterName = parameterName;
    }
    
    public void setIgnoreAcceptHeader(final boolean ignoreAcceptHeader) {
        this.ignoreAcceptHeader = ignoreAcceptHeader;
    }
    
    public void setDefaultContentType(final MediaType defaultContentType) {
        this.defaultContentType = defaultContentType;
    }
    
    public void setServletContext(final ServletContext servletContext) {
        this.servletContext = servletContext;
    }
    
    public void afterPropertiesSet() throws Exception {
        final List<ContentNegotiationStrategy> strategies = new ArrayList<ContentNegotiationStrategy>();
        final Map<String, MediaType> mediaTypesMap = new HashMap<String, MediaType>();
        CollectionUtils.mergePropertiesIntoMap(this.mediaTypes, (Map)mediaTypesMap);
        if (this.favorPathExtension) {
            PathExtensionContentNegotiationStrategy strategy;
            if (this.servletContext != null) {
                strategy = new ServletPathExtensionContentNegotiationStrategy(this.servletContext, mediaTypesMap);
            }
            else {
                strategy = new PathExtensionContentNegotiationStrategy(mediaTypesMap);
            }
            if (this.useJaf != null) {
                strategy.setUseJaf(this.useJaf);
            }
            strategies.add(strategy);
        }
        if (this.favorParameter) {
            final ParameterContentNegotiationStrategy strategy2 = new ParameterContentNegotiationStrategy(mediaTypesMap);
            strategy2.setParameterName(this.parameterName);
            strategies.add(strategy2);
        }
        if (!this.ignoreAcceptHeader) {
            strategies.add(new HeaderContentNegotiationStrategy());
        }
        if (this.defaultContentType != null) {
            strategies.add(new FixedContentNegotiationStrategy(this.defaultContentType));
        }
        final ContentNegotiationStrategy[] array = strategies.toArray(new ContentNegotiationStrategy[strategies.size()]);
        this.contentNegotiationManager = new ContentNegotiationManager(array);
    }
    
    public Class<?> getObjectType() {
        return ContentNegotiationManager.class;
    }
    
    public boolean isSingleton() {
        return true;
    }
    
    public ContentNegotiationManager getObject() throws Exception {
        return this.contentNegotiationManager;
    }
}
