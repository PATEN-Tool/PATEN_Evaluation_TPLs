// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.http.converter.json;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MappingJackson2HttpMessageConverter extends AbstractJackson2HttpMessageConverter
{
    private String jsonPrefix;
    
    public MappingJackson2HttpMessageConverter() {
        super(new ObjectMapper(), new MediaType[] { new MediaType("application", "json", MappingJackson2HttpMessageConverter.DEFAULT_CHARSET), new MediaType("application", "*+json", MappingJackson2HttpMessageConverter.DEFAULT_CHARSET) });
    }
    
    public void setJsonPrefix(final String jsonPrefix) {
        this.jsonPrefix = jsonPrefix;
    }
    
    public void setPrefixJson(final boolean prefixJson) {
        this.jsonPrefix = (prefixJson ? "{} && " : null);
    }
    
    @Override
    protected void writePrefix(final JsonGenerator generator, final Object object) throws IOException {
        if (this.jsonPrefix != null) {
            generator.writeRaw(this.jsonPrefix);
        }
        final String jsonpFunction = (object instanceof MappingJacksonValue) ? ((MappingJacksonValue)object).getJsonpFunction() : null;
        if (jsonpFunction != null) {
            generator.writeRaw(jsonpFunction + "(");
        }
    }
    
    @Override
    protected void writeSuffix(final JsonGenerator generator, final Object object) throws IOException {
        final String jsonpFunction = (object instanceof MappingJacksonValue) ? ((MappingJacksonValue)object).getJsonpFunction() : null;
        if (jsonpFunction != null) {
            generator.writeRaw(");");
        }
    }
}
