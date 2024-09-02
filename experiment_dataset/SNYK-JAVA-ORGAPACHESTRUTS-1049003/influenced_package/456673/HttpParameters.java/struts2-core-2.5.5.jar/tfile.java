// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.struts2.dispatcher;

import java.util.TreeMap;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Collection;
import java.util.TreeSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

public class HttpParameters implements Cloneable
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
    
    public Parameter get(final String name) {
        if (this.parameters.containsKey(name)) {
            return this.parameters.get(name);
        }
        return new Parameter.EmptyHttpParameter(name);
    }
    
    public Set<String> getNames() {
        return new TreeSet<String>(this.parameters.keySet());
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
    
    public Map<String, String[]> toMap() {
        final Map<String, String[]> result = new HashMap<String, String[]>(this.parameters.size());
        for (final Map.Entry<String, Parameter> entry : this.parameters.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getMultipleValues());
        }
        return result;
    }
    
    public HttpParameters appendAll(final Map<String, Parameter> newParams) {
        this.parameters.putAll(newParams);
        return this;
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
            for (final Map.Entry<String, Object> entry : this.requestParameterMap.entrySet()) {
                final String name = entry.getKey();
                final Object value = entry.getValue();
                parameters.put(name, new Parameter.Request(name, value));
            }
            return new HttpParameters(parameters, null);
        }
    }
}
