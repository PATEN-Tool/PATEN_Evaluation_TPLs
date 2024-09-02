// 
// Decompiled by Procyon v0.5.36
// 

package com.opensymphony.xwork2.config.entities;

import java.util.Collections;
import java.util.Iterator;
import org.apache.commons.lang.StringUtils;
import com.opensymphony.xwork2.util.location.Location;
import java.util.Collection;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.io.Serializable;
import com.opensymphony.xwork2.util.location.Located;

public class ActionConfig extends Located implements Serializable
{
    public static final String DEFAULT_METHOD = "execute";
    public static final String WILDCARD = "*";
    protected List<InterceptorMapping> interceptors;
    protected Map<String, String> params;
    protected Map<String, ResultConfig> results;
    protected List<ExceptionMappingConfig> exceptionMappings;
    protected String className;
    protected String methodName;
    protected String packageName;
    protected String name;
    protected Set<String> allowedMethods;
    
    protected ActionConfig(final String packageName, final String name, final String className) {
        this.packageName = packageName;
        this.name = name;
        this.className = className;
        this.params = new LinkedHashMap<String, String>();
        this.results = new LinkedHashMap<String, ResultConfig>();
        this.interceptors = new ArrayList<InterceptorMapping>();
        this.exceptionMappings = new ArrayList<ExceptionMappingConfig>();
        this.allowedMethods = new HashSet<String>();
    }
    
    protected ActionConfig(final ActionConfig orig) {
        this.name = orig.name;
        this.className = orig.className;
        this.methodName = orig.methodName;
        this.packageName = orig.packageName;
        this.params = new LinkedHashMap<String, String>(orig.params);
        this.interceptors = new ArrayList<InterceptorMapping>(orig.interceptors);
        this.results = new LinkedHashMap<String, ResultConfig>(orig.results);
        this.exceptionMappings = new ArrayList<ExceptionMappingConfig>(orig.exceptionMappings);
        this.allowedMethods = new HashSet<String>(orig.allowedMethods);
    }
    
    public String getName() {
        return this.name;
    }
    
    public String getClassName() {
        return this.className;
    }
    
    public List<ExceptionMappingConfig> getExceptionMappings() {
        return this.exceptionMappings;
    }
    
    public List<InterceptorMapping> getInterceptors() {
        return this.interceptors;
    }
    
    public Set<String> getAllowedMethods() {
        return this.allowedMethods;
    }
    
    public String getMethodName() {
        return this.methodName;
    }
    
    public String getPackageName() {
        return this.packageName;
    }
    
    public Map<String, String> getParams() {
        return this.params;
    }
    
    public Map<String, ResultConfig> getResults() {
        return this.results;
    }
    
    public boolean isAllowedMethod(final String method) {
        return (this.allowedMethods.size() == 1 && "*".equals(this.allowedMethods.iterator().next())) || method.equals((this.methodName != null) ? this.methodName : "execute") || this.allowedMethods.contains(method);
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ActionConfig)) {
            return false;
        }
        final ActionConfig actionConfig = (ActionConfig)o;
        Label_0054: {
            if (this.className != null) {
                if (this.className.equals(actionConfig.className)) {
                    break Label_0054;
                }
            }
            else if (actionConfig.className == null) {
                break Label_0054;
            }
            return false;
        }
        Label_0087: {
            if (this.name != null) {
                if (this.name.equals(actionConfig.name)) {
                    break Label_0087;
                }
            }
            else if (actionConfig.name == null) {
                break Label_0087;
            }
            return false;
        }
        Label_0120: {
            if (this.interceptors != null) {
                if (this.interceptors.equals(actionConfig.interceptors)) {
                    break Label_0120;
                }
            }
            else if (actionConfig.interceptors == null) {
                break Label_0120;
            }
            return false;
        }
        Label_0153: {
            if (this.methodName != null) {
                if (this.methodName.equals(actionConfig.methodName)) {
                    break Label_0153;
                }
            }
            else if (actionConfig.methodName == null) {
                break Label_0153;
            }
            return false;
        }
        Label_0186: {
            if (this.params != null) {
                if (this.params.equals(actionConfig.params)) {
                    break Label_0186;
                }
            }
            else if (actionConfig.params == null) {
                break Label_0186;
            }
            return false;
        }
        Label_0219: {
            if (this.results != null) {
                if (this.results.equals(actionConfig.results)) {
                    break Label_0219;
                }
            }
            else if (actionConfig.results == null) {
                break Label_0219;
            }
            return false;
        }
        if (this.allowedMethods != null) {
            if (this.allowedMethods.equals(actionConfig.allowedMethods)) {
                return true;
            }
        }
        else if (actionConfig.allowedMethods == null) {
            return true;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        int result = (this.interceptors != null) ? this.interceptors.hashCode() : 0;
        result = 31 * result + ((this.params != null) ? this.params.hashCode() : 0);
        result = 31 * result + ((this.results != null) ? this.results.hashCode() : 0);
        result = 31 * result + ((this.exceptionMappings != null) ? this.exceptionMappings.hashCode() : 0);
        result = 31 * result + ((this.className != null) ? this.className.hashCode() : 0);
        result = 31 * result + ((this.methodName != null) ? this.methodName.hashCode() : 0);
        result = 31 * result + ((this.packageName != null) ? this.packageName.hashCode() : 0);
        result = 31 * result + ((this.name != null) ? this.name.hashCode() : 0);
        result = 31 * result + ((this.allowedMethods != null) ? this.allowedMethods.hashCode() : 0);
        return result;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{ActionConfig ");
        sb.append(this.name).append(" (");
        sb.append(this.className);
        if (this.methodName != null) {
            sb.append(".").append(this.methodName).append("()");
        }
        sb.append(")");
        sb.append(" - ").append(this.location);
        sb.append("}");
        return sb.toString();
    }
    
    public static class Builder implements InterceptorListHolder
    {
        protected ActionConfig target;
        private boolean gotMethods;
        
        public Builder(final ActionConfig toClone) {
            this.target = new ActionConfig(toClone);
            this.addAllowedMethod(toClone.getAllowedMethods());
        }
        
        public Builder(final String packageName, final String name, final String className) {
            this.target = new ActionConfig(packageName, name, className);
        }
        
        public Builder packageName(final String name) {
            this.target.packageName = name;
            return this;
        }
        
        public Builder name(final String name) {
            this.target.name = name;
            return this;
        }
        
        public Builder className(final String name) {
            this.target.className = name;
            return this;
        }
        
        public Builder defaultClassName(final String name) {
            if (StringUtils.isEmpty(this.target.className)) {
                this.target.className = name;
            }
            return this;
        }
        
        public Builder methodName(final String method) {
            this.target.methodName = method;
            return this;
        }
        
        public Builder addExceptionMapping(final ExceptionMappingConfig exceptionMapping) {
            this.target.exceptionMappings.add(exceptionMapping);
            return this;
        }
        
        public Builder addExceptionMappings(final Collection<? extends ExceptionMappingConfig> mappings) {
            this.target.exceptionMappings.addAll(mappings);
            return this;
        }
        
        public Builder exceptionMappings(final Collection<? extends ExceptionMappingConfig> mappings) {
            this.target.exceptionMappings.clear();
            this.target.exceptionMappings.addAll(mappings);
            return this;
        }
        
        public Builder addInterceptor(final InterceptorMapping interceptor) {
            this.target.interceptors.add(interceptor);
            return this;
        }
        
        public Builder addInterceptors(final List<InterceptorMapping> interceptors) {
            this.target.interceptors.addAll(interceptors);
            return this;
        }
        
        public Builder interceptors(final List<InterceptorMapping> interceptors) {
            this.target.interceptors.clear();
            this.target.interceptors.addAll(interceptors);
            return this;
        }
        
        public Builder addParam(final String name, final String value) {
            this.target.params.put(name, value);
            return this;
        }
        
        public Builder addParams(final Map<String, String> params) {
            this.target.params.putAll(params);
            return this;
        }
        
        public Builder addResultConfig(final ResultConfig resultConfig) {
            this.target.results.put(resultConfig.getName(), resultConfig);
            return this;
        }
        
        public Builder addResultConfigs(final Collection<ResultConfig> configs) {
            for (final ResultConfig rc : configs) {
                this.target.results.put(rc.getName(), rc);
            }
            return this;
        }
        
        public Builder addResultConfigs(final Map<String, ResultConfig> configs) {
            this.target.results.putAll(configs);
            return this;
        }
        
        public Builder addAllowedMethod(final String methodName) {
            this.target.allowedMethods.add(methodName);
            return this;
        }
        
        public Builder addAllowedMethod(final Collection<String> methods) {
            if (methods != null) {
                this.gotMethods = true;
                this.target.allowedMethods.addAll(methods);
            }
            return this;
        }
        
        public Builder location(final Location loc) {
            this.target.location = loc;
            return this;
        }
        
        public ActionConfig build() {
            this.embalmTarget();
            final ActionConfig result = this.target;
            this.target = new ActionConfig(this.target);
            return result;
        }
        
        protected void embalmTarget() {
            if (!this.gotMethods && this.target.allowedMethods.isEmpty()) {
                this.target.allowedMethods.add("*");
            }
            this.target.params = Collections.unmodifiableMap((Map<? extends String, ? extends String>)this.target.params);
            this.target.results = Collections.unmodifiableMap((Map<? extends String, ? extends ResultConfig>)this.target.results);
            this.target.interceptors = Collections.unmodifiableList((List<? extends InterceptorMapping>)this.target.interceptors);
            this.target.exceptionMappings = Collections.unmodifiableList((List<? extends ExceptionMappingConfig>)this.target.exceptionMappings);
            this.target.allowedMethods = Collections.unmodifiableSet((Set<? extends String>)this.target.allowedMethods);
        }
    }
}
