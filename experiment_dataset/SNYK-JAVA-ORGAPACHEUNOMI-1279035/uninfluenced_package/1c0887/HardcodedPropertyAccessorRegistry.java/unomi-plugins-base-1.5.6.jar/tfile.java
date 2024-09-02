// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.unomi.plugins.baseplugin.conditions;

import org.slf4j.LoggerFactory;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.function.Predicate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Iterator;
import java.util.Collection;
import java.util.ArrayList;
import org.apache.unomi.plugins.baseplugin.conditions.accessors.MapAccessor;
import org.apache.unomi.plugins.baseplugin.conditions.accessors.CampaignAccessor;
import org.apache.unomi.api.campaigns.Campaign;
import org.apache.unomi.plugins.baseplugin.conditions.accessors.CustomItemAccessor;
import org.apache.unomi.api.CustomItem;
import org.apache.unomi.plugins.baseplugin.conditions.accessors.GoalAccessor;
import org.apache.unomi.api.goals.Goal;
import org.apache.unomi.plugins.baseplugin.conditions.accessors.RuleAccessor;
import org.apache.unomi.api.rules.Rule;
import org.apache.unomi.plugins.baseplugin.conditions.accessors.SessionAccessor;
import org.apache.unomi.api.Session;
import org.apache.unomi.plugins.baseplugin.conditions.accessors.ConsentAccessor;
import org.apache.unomi.api.Consent;
import org.apache.unomi.plugins.baseplugin.conditions.accessors.ProfileAccessor;
import org.apache.unomi.api.Profile;
import org.apache.unomi.plugins.baseplugin.conditions.accessors.EventAccessor;
import org.apache.unomi.api.Event;
import org.apache.unomi.plugins.baseplugin.conditions.accessors.TimestampedItemAccessor;
import org.apache.unomi.api.TimestampedItem;
import org.apache.unomi.plugins.baseplugin.conditions.accessors.MetadataAccessor;
import org.apache.unomi.api.Metadata;
import org.apache.unomi.plugins.baseplugin.conditions.accessors.MetadataItemAccessor;
import org.apache.unomi.api.MetadataItem;
import org.apache.unomi.plugins.baseplugin.conditions.accessors.ItemAccessor;
import org.apache.unomi.api.Item;
import java.util.HashMap;
import java.util.List;
import org.apache.unomi.plugins.baseplugin.conditions.accessors.HardcodedPropertyAccessor;
import java.util.Map;
import org.slf4j.Logger;

public class HardcodedPropertyAccessorRegistry
{
    private static final Logger logger;
    protected Map<Class<?>, HardcodedPropertyAccessor> propertyAccessors;
    protected Map<Class<?>, List<Class<?>>> cachedClassAncestors;
    
    public HardcodedPropertyAccessorRegistry() {
        this.propertyAccessors = new HashMap<Class<?>, HardcodedPropertyAccessor>();
        this.cachedClassAncestors = new HashMap<Class<?>, List<Class<?>>>();
        this.propertyAccessors.put(Item.class, new ItemAccessor(this));
        this.propertyAccessors.put(MetadataItem.class, new MetadataItemAccessor(this));
        this.propertyAccessors.put(Metadata.class, new MetadataAccessor(this));
        this.propertyAccessors.put(TimestampedItem.class, new TimestampedItemAccessor(this));
        this.propertyAccessors.put(Event.class, new EventAccessor(this));
        this.propertyAccessors.put(Profile.class, new ProfileAccessor(this));
        this.propertyAccessors.put(Consent.class, new ConsentAccessor(this));
        this.propertyAccessors.put(Session.class, new SessionAccessor(this));
        this.propertyAccessors.put(Rule.class, new RuleAccessor(this));
        this.propertyAccessors.put(Goal.class, new GoalAccessor(this));
        this.propertyAccessors.put(CustomItem.class, new CustomItemAccessor(this));
        this.propertyAccessors.put(Campaign.class, new CampaignAccessor(this));
        this.propertyAccessors.put(Map.class, new MapAccessor(this));
    }
    
    protected NextTokens getNextTokens(final String expression) {
        if (expression.startsWith("[\"")) {
            final int lookupNameBeginPos = "[\"".length();
            final int lookupNameEndPos = expression.indexOf("\"]", lookupNameBeginPos);
            return this.buildNextTokens(expression, lookupNameBeginPos, lookupNameEndPos, lookupNameEndPos + 2);
        }
        if (expression.startsWith(".")) {
            final int lookupNameBeginPos = ".".length();
            final int lookupNameEndPos = this.findNextStartDelimiterPos(expression, lookupNameBeginPos);
            return this.buildNextTokens(expression, lookupNameBeginPos, lookupNameEndPos, lookupNameEndPos);
        }
        final int lookupNameBeginPos = 0;
        final int lookupNameEndPos = this.findNextStartDelimiterPos(expression, lookupNameBeginPos);
        return this.buildNextTokens(expression, lookupNameBeginPos, lookupNameEndPos, lookupNameEndPos);
    }
    
    private NextTokens buildNextTokens(final String expression, final int lookupNameBeginPos, final int lookupNameEndPos, final int leftoverStartPos) {
        final NextTokens nextTokens = new NextTokens();
        if (lookupNameEndPos >= lookupNameBeginPos) {
            nextTokens.propertyName = expression.substring(lookupNameBeginPos, lookupNameEndPos);
            nextTokens.leftoverExpression = expression.substring(leftoverStartPos);
            if ("".equals(nextTokens.leftoverExpression)) {
                nextTokens.leftoverExpression = null;
            }
        }
        else {
            nextTokens.propertyName = expression.substring(lookupNameBeginPos);
            nextTokens.leftoverExpression = null;
        }
        return nextTokens;
    }
    
    private int findNextStartDelimiterPos(final String expression, final int lookupNameBeginPos) {
        final int dotlookupNameEndPos = expression.indexOf(".", lookupNameBeginPos);
        final int squareBracketlookupNameEndPos = expression.indexOf("[\"", lookupNameBeginPos);
        int lookupNameEndPos;
        if (dotlookupNameEndPos >= lookupNameBeginPos && squareBracketlookupNameEndPos >= lookupNameBeginPos) {
            lookupNameEndPos = Math.min(dotlookupNameEndPos, squareBracketlookupNameEndPos);
        }
        else if (dotlookupNameEndPos >= lookupNameBeginPos) {
            lookupNameEndPos = dotlookupNameEndPos;
        }
        else if (squareBracketlookupNameEndPos >= lookupNameBeginPos) {
            lookupNameEndPos = squareBracketlookupNameEndPos;
        }
        else {
            lookupNameEndPos = -1;
        }
        return lookupNameEndPos;
    }
    
    public Object getProperty(final Object object, final String expression) {
        if (expression == null) {
            return object;
        }
        if (expression.trim().equals("")) {
            return object;
        }
        final NextTokens nextTokens = this.getNextTokens(expression);
        final List<Class<?>> lookupClasses = new ArrayList<Class<?>>();
        lookupClasses.add(object.getClass());
        List<Class<?>> objectClassAncestors = this.cachedClassAncestors.get(object.getClass());
        if (objectClassAncestors == null) {
            objectClassAncestors = this.collectAncestors(object.getClass(), this.propertyAccessors.keySet());
            this.cachedClassAncestors.put(object.getClass(), objectClassAncestors);
        }
        if (objectClassAncestors != null) {
            lookupClasses.addAll(objectClassAncestors);
        }
        for (final Class<?> lookupClass : lookupClasses) {
            final HardcodedPropertyAccessor propertyAccessor = this.propertyAccessors.get(lookupClass);
            if (propertyAccessor != null) {
                final Object result = propertyAccessor.getProperty(object, nextTokens.propertyName, nextTokens.leftoverExpression);
                if (!"$$$###PROPERTY_NOT_FOUND###$$$".equals(result)) {
                    return result;
                }
                continue;
            }
        }
        HardcodedPropertyAccessorRegistry.logger.warn("Couldn't find any property access for class {}. See debug log level for more information", (Object)object.getClass().getName());
        if (HardcodedPropertyAccessorRegistry.logger.isDebugEnabled()) {
            HardcodedPropertyAccessorRegistry.logger.debug("Couldn't find any property access for class {} and expression {}", (Object)object.getClass().getName(), (Object)expression);
        }
        return "$$$###PROPERTY_NOT_FOUND###$$$";
    }
    
    public List<Class<?>> collectAncestors(final Class<?> targetClass, final Set<Class<?>> availableAccessors) {
        final Set<Class<?>> parentClasses = new LinkedHashSet<Class<?>>();
        if (targetClass.getSuperclass() != null) {
            parentClasses.add(targetClass.getSuperclass());
        }
        if (targetClass.getInterfaces().length > 0) {
            parentClasses.addAll(Arrays.asList(targetClass.getInterfaces()));
        }
        final Set<Class<?>> ancestors = new LinkedHashSet<Class<?>>();
        for (final Class<?> parentClass : parentClasses) {
            ancestors.addAll(this.collectAncestors(parentClass, availableAccessors));
        }
        final Set<Class<?>> result = new LinkedHashSet<Class<?>>();
        result.addAll(parentClasses);
        result.addAll(ancestors);
        return result.stream().filter(availableAccessors::contains).collect((Collector<? super Object, ?, List<Class<?>>>)Collectors.toList());
    }
    
    static {
        logger = LoggerFactory.getLogger(HardcodedPropertyAccessorRegistry.class.getName());
    }
    
    public static class NextTokens
    {
        public String propertyName;
        public String leftoverExpression;
    }
}
