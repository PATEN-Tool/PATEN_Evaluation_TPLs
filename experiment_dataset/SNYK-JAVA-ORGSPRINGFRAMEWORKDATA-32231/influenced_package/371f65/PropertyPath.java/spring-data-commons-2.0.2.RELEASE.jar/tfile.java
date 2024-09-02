// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.data.mapping;

import java.beans.ConstructorProperties;
import org.springframework.util.ConcurrentReferenceHashMap;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.Stack;
import java.util.ArrayList;
import java.util.Iterator;
import org.springframework.util.StringUtils;
import org.springframework.util.Assert;
import java.util.List;
import java.util.Collections;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.data.util.TypeInformation;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.data.util.Streamable;

public class PropertyPath implements Streamable<PropertyPath>
{
    private static final String DELIMITERS = "_\\.";
    private static final String ALL_UPPERCASE = "[A-Z0-9._$]+";
    private static final Pattern SPLITTER;
    private static final Pattern SPLITTER_FOR_QUOTED;
    private static final Map<Key, PropertyPath> CACHE;
    private final TypeInformation<?> owningType;
    private final String name;
    private final TypeInformation<?> typeInformation;
    private final TypeInformation<?> actualTypeInformation;
    private final boolean isCollection;
    @Nullable
    private PropertyPath next;
    
    PropertyPath(final String name, final Class<?> owningType) {
        this(name, ClassTypeInformation.from(owningType), Collections.emptyList());
    }
    
    PropertyPath(final String name, final TypeInformation<?> owningType, final List<PropertyPath> base) {
        Assert.hasText(name, "Name must not be null or empty!");
        Assert.notNull((Object)owningType, "Owning type must not be null!");
        Assert.notNull((Object)base, "Perviously found properties must not be null!");
        final String propertyName = name.matches("[A-Z0-9._$]+") ? name : StringUtils.uncapitalize(name);
        final TypeInformation<?> propertyType = owningType.getProperty(propertyName);
        if (propertyType == null) {
            throw new PropertyReferenceException(propertyName, owningType, base);
        }
        this.owningType = owningType;
        this.typeInformation = propertyType;
        this.isCollection = propertyType.isCollectionLike();
        this.name = propertyName;
        this.actualTypeInformation = ((propertyType.getActualType() == null) ? propertyType : propertyType.getRequiredActualType());
    }
    
    public TypeInformation<?> getOwningType() {
        return this.owningType;
    }
    
    public String getSegment() {
        return this.name;
    }
    
    public PropertyPath getLeafProperty() {
        PropertyPath result;
        for (result = this; result.hasNext(); result = result.requiredNext()) {}
        return result;
    }
    
    public Class<?> getType() {
        return this.actualTypeInformation.getType();
    }
    
    @Nullable
    public PropertyPath next() {
        return this.next;
    }
    
    public boolean hasNext() {
        return this.next != null;
    }
    
    public String toDotPath() {
        if (this.hasNext()) {
            return this.getSegment() + "." + this.requiredNext().toDotPath();
        }
        return this.getSegment();
    }
    
    public boolean isCollection() {
        return this.isCollection;
    }
    
    @Override
    public Iterator<PropertyPath> iterator() {
        return new Iterator<PropertyPath>() {
            @Nullable
            private PropertyPath current = PropertyPath.this;
            
            @Override
            public boolean hasNext() {
                return this.current != null;
            }
            
            @Nullable
            @Override
            public PropertyPath next() {
                final PropertyPath result = this.current;
                if (result == null) {
                    return null;
                }
                this.current = result.next();
                return result;
            }
            
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    private PropertyPath requiredNext() {
        final PropertyPath result = this.next;
        if (result == null) {
            throw new IllegalStateException("No next path available! Clients should call hasNext() before invoking this method!");
        }
        return result;
    }
    
    public static PropertyPath from(final String source, final Class<?> type) {
        return from(source, ClassTypeInformation.from(type));
    }
    
    public static PropertyPath from(final String source, final TypeInformation<?> type) {
        Assert.hasText(source, "Source must not be null or empty!");
        Assert.notNull((Object)type, "TypeInformation must not be null or empty!");
        final List<String> iteratorSource;
        Matcher matcher2;
        final Matcher matcher;
        final Iterator<String> parts;
        PropertyPath result;
        final Stack<PropertyPath> current;
        final IllegalStateException ex;
        return PropertyPath.CACHE.computeIfAbsent(Key.of(type, source), it -> {
            iteratorSource = new ArrayList<String>();
            if (isQuoted(it.path)) {
                matcher2 = PropertyPath.SPLITTER_FOR_QUOTED.matcher(it.path.replace("\\Q", "").replace("\\E", ""));
            }
            else {
                matcher2 = PropertyPath.SPLITTER.matcher("_" + it.path);
            }
            matcher = matcher2;
            while (matcher.find()) {
                iteratorSource.add(matcher.group(1));
            }
            parts = iteratorSource.iterator();
            result = null;
            current = new Stack<PropertyPath>();
            while (parts.hasNext()) {
                if (result == null) {
                    result = create(parts.next(), it.type, current);
                    current.push(result);
                }
                else {
                    current.push(create(parts.next(), current));
                }
            }
            if (result == null) {
                new IllegalStateException(String.format("Expected parsing to yield a PropertyPath from %s but got null!", source));
                throw ex;
            }
            else {
                return result;
            }
        });
    }
    
    private static boolean isQuoted(final String source) {
        return source.matches("^\\\\Q.*\\\\E$");
    }
    
    private static PropertyPath create(final String source, final Stack<PropertyPath> base) {
        final PropertyPath previous = base.peek();
        final PropertyPath propertyPath = create(source, previous.typeInformation.getRequiredActualType(), base);
        return previous.next = propertyPath;
    }
    
    private static PropertyPath create(final String source, final TypeInformation<?> type, final List<PropertyPath> base) {
        return create(source, type, "", base);
    }
    
    private static PropertyPath create(final String source, final TypeInformation<?> type, final String addTail, final List<PropertyPath> base) {
        PropertyReferenceException exception = null;
        PropertyPath current = null;
        try {
            current = new PropertyPath(source, type, base);
            if (!base.isEmpty()) {
                base.get(base.size() - 1).next = current;
            }
            final List<PropertyPath> newBase = new ArrayList<PropertyPath>(base);
            newBase.add(current);
            if (StringUtils.hasText(addTail)) {
                current.next = create(addTail, current.actualTypeInformation, newBase);
            }
            return current;
        }
        catch (PropertyReferenceException e) {
            if (current != null) {
                throw e;
            }
            exception = e;
            final Pattern pattern = Pattern.compile("\\p{Lu}+\\p{Ll}*$");
            final Matcher matcher = pattern.matcher(source);
            if (matcher.find() && matcher.start() != 0) {
                final int position = matcher.start();
                final String head = source.substring(0, position);
                final String tail = source.substring(position);
                try {
                    return create(head, type, tail + addTail, base);
                }
                catch (PropertyReferenceException e2) {
                    throw e2.hasDeeperResolutionDepthThan(exception) ? e2 : exception;
                }
            }
            throw exception;
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s.%s", this.owningType.getType().getSimpleName(), this.toDotPath());
    }
    
    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PropertyPath)) {
            return false;
        }
        final PropertyPath other = (PropertyPath)o;
        if (!other.canEqual(this)) {
            return false;
        }
        final Object this$owningType = this.getOwningType();
        final Object other$owningType = other.getOwningType();
        Label_0065: {
            if (this$owningType == null) {
                if (other$owningType == null) {
                    break Label_0065;
                }
            }
            else if (this$owningType.equals(other$owningType)) {
                break Label_0065;
            }
            return false;
        }
        final Object this$name = this.name;
        final Object other$name = other.name;
        Label_0102: {
            if (this$name == null) {
                if (other$name == null) {
                    break Label_0102;
                }
            }
            else if (this$name.equals(other$name)) {
                break Label_0102;
            }
            return false;
        }
        final Object this$typeInformation = this.getTypeInformation();
        final Object other$typeInformation = other.getTypeInformation();
        Label_0139: {
            if (this$typeInformation == null) {
                if (other$typeInformation == null) {
                    break Label_0139;
                }
            }
            else if (this$typeInformation.equals(other$typeInformation)) {
                break Label_0139;
            }
            return false;
        }
        final Object this$actualTypeInformation = this.actualTypeInformation;
        final Object other$actualTypeInformation = other.actualTypeInformation;
        Label_0176: {
            if (this$actualTypeInformation == null) {
                if (other$actualTypeInformation == null) {
                    break Label_0176;
                }
            }
            else if (this$actualTypeInformation.equals(other$actualTypeInformation)) {
                break Label_0176;
            }
            return false;
        }
        if (this.isCollection() != other.isCollection()) {
            return false;
        }
        final Object this$next = this.next;
        final Object other$next = other.next;
        if (this$next == null) {
            if (other$next == null) {
                return true;
            }
        }
        else if (this$next.equals(other$next)) {
            return true;
        }
        return false;
    }
    
    protected boolean canEqual(final Object other) {
        return other instanceof PropertyPath;
    }
    
    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $owningType = this.getOwningType();
        result = result * 59 + (($owningType == null) ? 43 : $owningType.hashCode());
        final Object $name = this.name;
        result = result * 59 + (($name == null) ? 43 : $name.hashCode());
        final Object $typeInformation = this.getTypeInformation();
        result = result * 59 + (($typeInformation == null) ? 43 : $typeInformation.hashCode());
        final Object $actualTypeInformation = this.actualTypeInformation;
        result = result * 59 + (($actualTypeInformation == null) ? 43 : $actualTypeInformation.hashCode());
        result = result * 59 + (this.isCollection() ? 79 : 97);
        final Object $next = this.next;
        result = result * 59 + (($next == null) ? 43 : $next.hashCode());
        return result;
    }
    
    public TypeInformation<?> getTypeInformation() {
        return this.typeInformation;
    }
    
    static {
        SPLITTER = Pattern.compile("(?:[%s]?([%s]*?[^%s]+))".replaceAll("%s", "_\\."));
        SPLITTER_FOR_QUOTED = Pattern.compile("(?:[%s]?([%s]*?[^%s]+))".replaceAll("%s", "\\."));
        CACHE = (Map)new ConcurrentReferenceHashMap();
    }
    
    private static final class Key
    {
        private final TypeInformation<?> type;
        private final String path;
        
        @ConstructorProperties({ "type", "path" })
        private Key(final TypeInformation<?> type, final String path) {
            this.type = type;
            this.path = path;
        }
        
        public static Key of(final TypeInformation<?> type, final String path) {
            return new Key(type, path);
        }
        
        public TypeInformation<?> getType() {
            return this.type;
        }
        
        public String getPath() {
            return this.path;
        }
        
        @Override
        public boolean equals(final Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Key)) {
                return false;
            }
            final Key other = (Key)o;
            final Object this$type = this.getType();
            final Object other$type = other.getType();
            Label_0055: {
                if (this$type == null) {
                    if (other$type == null) {
                        break Label_0055;
                    }
                }
                else if (this$type.equals(other$type)) {
                    break Label_0055;
                }
                return false;
            }
            final Object this$path = this.getPath();
            final Object other$path = other.getPath();
            if (this$path == null) {
                if (other$path == null) {
                    return true;
                }
            }
            else if (this$path.equals(other$path)) {
                return true;
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $type = this.getType();
            result = result * 59 + (($type == null) ? 43 : $type.hashCode());
            final Object $path = this.getPath();
            result = result * 59 + (($path == null) ? 43 : $path.hashCode());
            return result;
        }
        
        @Override
        public String toString() {
            return "PropertyPath.Key(type=" + this.getType() + ", path=" + this.getPath() + ")";
        }
    }
}
