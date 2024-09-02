/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.unomi.plugins.baseplugin.conditions;

import org.apache.unomi.api.*;
import org.apache.unomi.api.campaigns.Campaign;
import org.apache.unomi.api.goals.Goal;
import org.apache.unomi.api.rules.Rule;
import org.apache.unomi.plugins.baseplugin.conditions.accessors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This class contains the registry of all the hardcoded property accessors.
 * For the moment this list of accessors is hardcoded, but in a future update it could be made dynamic.
 */
public class HardcodedPropertyAccessorRegistry {

    private static final Logger logger = LoggerFactory.getLogger(HardcodedPropertyAccessorRegistry.class.getName());

    Map<String, HardcodedPropertyAccessor> propertyAccessors = new HashMap<>();

    public HardcodedPropertyAccessorRegistry() {
        propertyAccessors.put(Item.class.getName(), new ItemAccessor(this));
        propertyAccessors.put(MetadataItem.class.getName(), new MetadataItemAccessor(this));
        propertyAccessors.put(Metadata.class.getName(), new MetadataAccessor(this));
        propertyAccessors.put(TimestampedItem.class.getName(), new TimestampedItemAccessor(this));
        propertyAccessors.put(Event.class.getName(), new EventAccessor(this));
        propertyAccessors.put(Profile.class.getName(), new ProfileAccessor(this));
        propertyAccessors.put(Consent.class.getName(), new ConsentAccessor(this));
        propertyAccessors.put(Session.class.getName(), new SessionAccessor(this));
        propertyAccessors.put(Rule.class.getName(), new RuleAccessor(this));
        propertyAccessors.put(Goal.class.getName(), new GoalAccessor(this));
        propertyAccessors.put(CustomItem.class.getName(), new CustomItemAccessor(this));
        propertyAccessors.put(Campaign.class.getName(), new CampaignAccessor(this));
        propertyAccessors.put(Map.class.getName(), new MapAccessor(this));
    }

    public static class NextTokens {
        public String propertyName;
        public String leftoverExpression;
    }

    protected NextTokens getNextTokens(String expression) {
        if (expression.startsWith("[\"")) {
            int lookupNameBeginPos = "[\"".length();
            int lookupNameEndPos = expression.indexOf("\"]", lookupNameBeginPos);
            return buildNextTokens(expression, lookupNameBeginPos, lookupNameEndPos, lookupNameEndPos+2);
        } else if (expression.startsWith(".")) {
            int lookupNameBeginPos = ".".length();
            int lookupNameEndPos = findNextStartDelimiterPos(expression, lookupNameBeginPos);
            return buildNextTokens(expression, lookupNameBeginPos, lookupNameEndPos, lookupNameEndPos);
        } else {
            int lookupNameBeginPos = 0;
            int lookupNameEndPos = findNextStartDelimiterPos(expression, lookupNameBeginPos);
            return buildNextTokens(expression, lookupNameBeginPos, lookupNameEndPos, lookupNameEndPos);
        }
    }

    private NextTokens buildNextTokens(String expression, int lookupNameBeginPos, int lookupNameEndPos, int leftoverStartPos) {
        NextTokens nextTokens = new NextTokens();
        if (lookupNameEndPos >= lookupNameBeginPos) {
            nextTokens.propertyName = expression.substring(lookupNameBeginPos, lookupNameEndPos);
            nextTokens.leftoverExpression = expression.substring(leftoverStartPos);
            if ("".equals(nextTokens.leftoverExpression)) {
                nextTokens.leftoverExpression = null;
            }
        } else {
            nextTokens.propertyName = expression.substring(lookupNameBeginPos);
            nextTokens.leftoverExpression = null;
        }
        return nextTokens;
    }

    private int findNextStartDelimiterPos(String expression, int lookupNameBeginPos) {
        int lookupNameEndPos;
        int dotlookupNameEndPos = expression.indexOf(".", lookupNameBeginPos);
        int squareBracketlookupNameEndPos = expression.indexOf("[\"", lookupNameBeginPos);
        if (dotlookupNameEndPos >= lookupNameBeginPos && squareBracketlookupNameEndPos >= lookupNameBeginPos) {
            lookupNameEndPos = Math.min(dotlookupNameEndPos, squareBracketlookupNameEndPos);
        } else if (dotlookupNameEndPos >= lookupNameBeginPos) {
            lookupNameEndPos = dotlookupNameEndPos;
        } else if (squareBracketlookupNameEndPos >= lookupNameBeginPos) {
            lookupNameEndPos = squareBracketlookupNameEndPos;
        } else {
            lookupNameEndPos = -1;
        }
        return lookupNameEndPos;
    }


    public Object getProperty(Object object, String expression) {
        if (expression == null) {
            return object;
        }
        if (expression.trim().equals("")) {
            return object;
        }
        NextTokens nextTokens = getNextTokens(expression);
        List<Class<?>> lookupClasses = new ArrayList<>();
        lookupClasses.add(object.getClass());
        lookupClasses.add(object.getClass().getSuperclass());
        lookupClasses.addAll(Arrays.asList(object.getClass().getInterfaces()));
        for (Class<?> lookupClass : lookupClasses) {
            HardcodedPropertyAccessor propertyAccessor = propertyAccessors.get(lookupClass.getName());
            if (propertyAccessor != null) {
                Object result = propertyAccessor.getProperty(object, nextTokens.propertyName, nextTokens.leftoverExpression);
                if (!HardcodedPropertyAccessor.PROPERTY_NOT_FOUND_MARKER.equals(result)) {
                    return result;
                }
            }
        }
        logger.warn("Couldn't find any property access for class {} and expression {}", object.getClass().getName(), expression);
        return HardcodedPropertyAccessor.PROPERTY_NOT_FOUND_MARKER;
    }
}