// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.struts2.dispatcher;

import java.util.TreeMap;
import java.util.Comparator;
import java.util.Collections;
import java.util.Collection;
import java.util.TreeSet;
import org.apache.struts2.interceptor.ParameterAware;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

public class HttpParameters implements Map<String, Parameter>, Cloneable
{
    private Map<String, Parameter> parameters;
    
    private HttpParameters(final Map<String, Parameter> parameters) {
        this.parameters = parameters;
    }
    
    public static Builder create(final Map requestParameterMap) {
        return new Builder(requestParameterMap);
    }
    
    public static Builder create() {
        return new Builder(new HashMap<String, Object>());
    }
    
    public HttpParameters remove(final Set<String> paramsToRemove) {
        for (final String paramName : paramsToRemove) {
            this.parameters.remove(paramName);
        }
        return this;
    }
    
    public HttpParameters remove(final String paramToRemove) {
        return this.remove(new HashSet<String>() {
            {
                this.add(paramToRemove);
            }
        });
    }
    
    public boolean contains(final String name) {
        return this.parameters.containsKey(name);
    }
    
    @Deprecated
    public Map<String, String[]> toMap() {
        final Map<String, String[]> result = new HashMap<String, String[]>(this.parameters.size());
        for (final Entry<String, Parameter> entry : this.parameters.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getMultipleValues());
        }
        return result;
    }
    
    public HttpParameters appendAll(final Map<String, Parameter> newParams) {
        this.parameters.putAll(newParams);
        return this;
    }
    
    public void applyParameters(final ParameterAware parameterAware) {
        parameterAware.setParameters(this.toMap());
    }
    
    @Override
    public int size() {
        return this.parameters.size();
    }
    
    @Override
    public boolean isEmpty() {
        return this.parameters.isEmpty();
    }
    
    @Override
    public boolean containsKey(final Object key) {
        return this.parameters.containsKey(key);
    }
    
    @Override
    public boolean containsValue(final Object value) {
        return this.parameters.containsValue(value);
    }
    
    @Override
    public Parameter get(final Object key) {
        if (this.parameters.containsKey(key)) {
            return this.parameters.get(key);
        }
        return new Parameter.Empty(String.valueOf(key));
    }
    
    @Override
    public Parameter put(final String key, final Parameter value) {
        throw new IllegalAccessError("HttpParameters are immutable, you cannot put value directly!");
    }
    
    @Override
    public Parameter remove(final Object key) {
        throw new IllegalAccessError("HttpParameters are immutable, you cannot remove object directly!");
    }
    
    @Override
    public void putAll(final Map<? extends String, ? extends Parameter> m) {
        throw new IllegalAccessError("HttpParameters are immutable, you cannot put values directly!");
    }
    
    @Override
    public void clear() {
        throw new IllegalAccessError("HttpParameters are immutable, you cannot clear values directly!");
    }
    
    @Override
    public Set<String> keySet() {
        return Collections.unmodifiableSet((Set<? extends String>)new TreeSet<String>(this.parameters.keySet()));
    }
    
    @Override
    public Collection<Parameter> values() {
        return Collections.unmodifiableCollection((Collection<? extends Parameter>)this.parameters.values());
    }
    
    @Override
    public Set<Entry<String, Parameter>> entrySet() {
        return Collections.unmodifiableSet((Set<? extends Entry<String, Parameter>>)this.parameters.entrySet());
    }
    
    @Override
    public String toString() {
        return this.parameters.toString();
    }
    
    public static class Builder
    {
        private Map<String, Object> requestParameterMap;
        private HttpParameters parent;
        
        protected Builder(final Map<String, ?> requestParameterMap) {
            (this.requestParameterMap = new HashMap<String, Object>()).putAll(requestParameterMap);
        }
        
        public Builder withParent(final HttpParameters parentParams) {
            if (parentParams != null) {
                this.parent = parentParams;
            }
            return this;
        }
        
        public Builder withExtraParams(final Map<String, ?> params) {
            if (params != null) {
                this.requestParameterMap.putAll(params);
            }
            return this;
        }
        
        public Builder withComparator(final Comparator<String> orderedComparator) {
            this.requestParameterMap = new TreeMap<String, Object>(orderedComparator);
            return this;
        }
        
        public HttpParameters build() {
            final Map<String, Parameter> parameters = (this.parent == null) ? new HashMap<String, Parameter>() : new HashMap<String, Parameter>(this.parent.parameters);
            for (final Entry<String, Object> entry : this.requestParameterMap.entrySet()) {
                final String name = entry.getKey();
                final Object value = entry.getValue();
                parameters.put(name, new Parameter.Request(name, value));
            }
            return new HttpParameters(parameters, null);
        }
        
        public HttpParameters buildNoNestedWrapping() {
            final Map<String, Parameter> parameters = (this.parent == null) ? new HashMap<String, Parameter>() : new HashMap<String, Parameter>(this.parent.parameters);
            for (final Entry<String, Object> entry : this.requestParameterMap.entrySet()) {
                final String name = entry.getKey();
                final Object value = entry.getValue();
                final Parameter parameterValue = (value instanceof Parameter) ? ((Parameter)value) : new Parameter.Request(name, value);
                parameters.put(name, parameterValue);
            }
            return new HttpParameters(parameters, null);
        }
    }
}
