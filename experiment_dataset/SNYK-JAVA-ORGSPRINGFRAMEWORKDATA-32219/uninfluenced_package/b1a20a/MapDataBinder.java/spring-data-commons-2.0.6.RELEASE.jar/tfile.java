// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.data.web;

import org.springframework.core.CollectionFactory;
import java.util.List;
import org.springframework.expression.TypedValue;
import org.springframework.expression.AccessException;
import org.springframework.util.Assert;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.Expression;
import org.springframework.expression.EvaluationContext;
import java.beans.PropertyDescriptor;
import org.springframework.data.util.TypeInformation;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.core.MethodParameter;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.lang.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.mapping.PropertyReferenceException;
import lombok.NonNull;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.beans.AbstractPropertyAccessor;
import org.springframework.beans.ConfigurablePropertyAccessor;
import javax.annotation.Nonnull;
import java.util.Map;
import java.util.HashMap;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.bind.WebDataBinder;

class MapDataBinder extends WebDataBinder
{
    private final Class<?> type;
    private final ConversionService conversionService;
    
    public MapDataBinder(final Class<?> type, final ConversionService conversionService) {
        super((Object)new HashMap());
        this.type = type;
        this.conversionService = conversionService;
    }
    
    @Nonnull
    public Map<String, Object> getTarget() {
        final Object target = super.getTarget();
        if (target == null) {
            throw new IllegalStateException("Target bean should never be null!");
        }
        return (Map<String, Object>)target;
    }
    
    protected ConfigurablePropertyAccessor getPropertyAccessor() {
        return (ConfigurablePropertyAccessor)new MapPropertyAccessor(this.type, this.getTarget(), this.conversionService);
    }
    
    private static class MapPropertyAccessor extends AbstractPropertyAccessor
    {
        private static final SpelExpressionParser PARSER;
        @NonNull
        private final Class<?> type;
        @NonNull
        private final Map<String, Object> map;
        @NonNull
        private final ConversionService conversionService;
        
        public boolean isReadableProperty(final String propertyName) {
            throw new UnsupportedOperationException();
        }
        
        public boolean isWritableProperty(final String propertyName) {
            try {
                return this.getPropertyPath(propertyName) != null;
            }
            catch (PropertyReferenceException o_O) {
                return false;
            }
        }
        
        @Nullable
        public TypeDescriptor getPropertyTypeDescriptor(final String propertyName) throws BeansException {
            throw new UnsupportedOperationException();
        }
        
        @Nullable
        public Object getPropertyValue(final String propertyName) throws BeansException {
            throw new UnsupportedOperationException();
        }
        
        public void setPropertyValue(final String propertyName, @Nullable Object value) throws BeansException {
            if (!this.isWritableProperty(propertyName)) {
                throw new NotWritablePropertyException((Class)this.type, propertyName);
            }
            final PropertyPath leafProperty = this.getPropertyPath(propertyName).getLeafProperty();
            final TypeInformation<?> owningType = leafProperty.getOwningType();
            TypeInformation<?> propertyType = leafProperty.getTypeInformation();
            propertyType = (propertyName.endsWith("]") ? propertyType.getActualType() : propertyType);
            if (propertyType != null && this.conversionRequired(value, propertyType.getType())) {
                final PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor((Class)owningType.getType(), leafProperty.getSegment());
                if (descriptor == null) {
                    throw new IllegalStateException(String.format("Couldn't find PropertyDescriptor for %s on %s!", leafProperty.getSegment(), owningType.getType()));
                }
                final MethodParameter methodParameter = new MethodParameter(descriptor.getReadMethod(), -1);
                final TypeDescriptor typeDescriptor = TypeDescriptor.nested(methodParameter, 0);
                if (typeDescriptor == null) {
                    throw new IllegalStateException(String.format("Couldn't obtain type descriptor for method parameter %s!", methodParameter));
                }
                value = this.conversionService.convert(value, TypeDescriptor.forObject(value), typeDescriptor);
            }
            final EvaluationContext context = (EvaluationContext)SimpleEvaluationContext.forPropertyAccessors(new PropertyAccessor[] { (PropertyAccessor)new PropertyTraversingMapAccessor(this.type, this.conversionService) }).withConversionService(this.conversionService).withRootObject((Object)this.map).build();
            final Expression expression = MapPropertyAccessor.PARSER.parseExpression(propertyName);
            try {
                expression.setValue(context, value);
            }
            catch (SpelEvaluationException o_O) {
                throw new NotWritablePropertyException((Class)this.type, propertyName, "Could not write property!", (Throwable)o_O);
            }
        }
        
        private boolean conversionRequired(@Nullable final Object source, final Class<?> targetType) {
            return source != null && !targetType.isInstance(source) && this.conversionService.canConvert((Class)source.getClass(), (Class)targetType);
        }
        
        private PropertyPath getPropertyPath(final String propertyName) {
            final String plainPropertyPath = propertyName.replaceAll("\\[.*?\\]", "");
            return PropertyPath.from(plainPropertyPath, this.type);
        }
        
        public MapPropertyAccessor(@NonNull final Class<?> type, @NonNull final Map<String, Object> map, @NonNull final ConversionService conversionService) {
            if (type == null) {
                throw new IllegalArgumentException("type is null");
            }
            if (map == null) {
                throw new IllegalArgumentException("map is null");
            }
            if (conversionService == null) {
                throw new IllegalArgumentException("conversionService is null");
            }
            this.type = type;
            this.map = map;
            this.conversionService = conversionService;
        }
        
        static {
            PARSER = new SpelExpressionParser(new SpelParserConfiguration(false, true));
        }
        
        private static final class PropertyTraversingMapAccessor extends MapAccessor
        {
            private final ConversionService conversionService;
            private Class<?> type;
            
            public PropertyTraversingMapAccessor(final Class<?> type, final ConversionService conversionService) {
                Assert.notNull((Object)type, "Type must not be null!");
                Assert.notNull((Object)conversionService, "ConversionService must not be null!");
                this.type = type;
                this.conversionService = conversionService;
            }
            
            public boolean canRead(final EvaluationContext context, @Nullable final Object target, final String name) throws AccessException {
                return true;
            }
            
            public TypedValue read(final EvaluationContext context, @Nullable final Object target, final String name) throws AccessException {
                if (target == null) {
                    return TypedValue.NULL;
                }
                final PropertyPath path = PropertyPath.from(name, this.type);
                try {
                    return super.read(context, target, name);
                }
                catch (AccessException o_O) {
                    final Object emptyResult = path.isCollection() ? CollectionFactory.createCollection((Class)List.class, 0) : CollectionFactory.createMap((Class)Map.class, 0);
                    ((Map)target).put(name, emptyResult);
                    return new TypedValue(emptyResult, this.getDescriptor(path, emptyResult));
                }
                finally {
                    this.type = path.getType();
                }
            }
            
            private TypeDescriptor getDescriptor(final PropertyPath path, final Object emptyValue) {
                final Class<?> actualPropertyType = path.getType();
                final TypeDescriptor valueDescriptor = this.conversionService.canConvert((Class)String.class, (Class)actualPropertyType) ? TypeDescriptor.valueOf((Class)String.class) : TypeDescriptor.valueOf((Class)HashMap.class);
                return path.isCollection() ? TypeDescriptor.collection((Class)emptyValue.getClass(), valueDescriptor) : TypeDescriptor.map((Class)emptyValue.getClass(), TypeDescriptor.valueOf((Class)String.class), valueDescriptor);
            }
        }
    }
}
