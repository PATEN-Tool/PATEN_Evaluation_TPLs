// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.messaging.simp.broker;

import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.expression.TypedValue;
import java.util.concurrent.CopyOnWriteArraySet;
import org.springframework.util.Assert;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.expression.PropertyAccessor;
import java.util.List;
import java.util.Iterator;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.Map;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.messaging.MessageHeaders;
import org.springframework.expression.Expression;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.AntPathMatcher;
import org.springframework.expression.ExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.PathMatcher;
import org.springframework.expression.EvaluationContext;

public class DefaultSubscriptionRegistry extends AbstractSubscriptionRegistry
{
    public static final int DEFAULT_CACHE_LIMIT = 1024;
    private static final EvaluationContext messageEvalContext;
    private PathMatcher pathMatcher;
    private volatile int cacheLimit;
    @Nullable
    private String selectorHeaderName;
    private volatile boolean selectorHeaderInUse;
    private final ExpressionParser expressionParser;
    private final DestinationCache destinationCache;
    private final SessionSubscriptionRegistry subscriptionRegistry;
    
    public DefaultSubscriptionRegistry() {
        this.pathMatcher = (PathMatcher)new AntPathMatcher();
        this.cacheLimit = 1024;
        this.selectorHeaderName = "selector";
        this.selectorHeaderInUse = false;
        this.expressionParser = (ExpressionParser)new SpelExpressionParser();
        this.destinationCache = new DestinationCache();
        this.subscriptionRegistry = new SessionSubscriptionRegistry();
    }
    
    public void setPathMatcher(final PathMatcher pathMatcher) {
        this.pathMatcher = pathMatcher;
    }
    
    public PathMatcher getPathMatcher() {
        return this.pathMatcher;
    }
    
    public void setCacheLimit(final int cacheLimit) {
        this.cacheLimit = cacheLimit;
    }
    
    public int getCacheLimit() {
        return this.cacheLimit;
    }
    
    public void setSelectorHeaderName(@Nullable final String selectorHeaderName) {
        this.selectorHeaderName = (StringUtils.hasText(selectorHeaderName) ? selectorHeaderName : null);
    }
    
    @Nullable
    public String getSelectorHeaderName() {
        return this.selectorHeaderName;
    }
    
    @Override
    protected void addSubscriptionInternal(final String sessionId, final String subsId, final String destination, final Message<?> message) {
        final Expression expression = this.getSelectorExpression(message.getHeaders());
        this.subscriptionRegistry.addSubscription(sessionId, subsId, destination, expression);
        this.destinationCache.updateAfterNewSubscription(destination, sessionId, subsId);
    }
    
    @Nullable
    private Expression getSelectorExpression(final MessageHeaders headers) {
        Expression expression = null;
        if (this.getSelectorHeaderName() != null) {
            final String selector = NativeMessageHeaderAccessor.getFirstNativeHeader(this.getSelectorHeaderName(), headers);
            if (selector != null) {
                try {
                    expression = this.expressionParser.parseExpression(selector);
                    this.selectorHeaderInUse = true;
                    if (this.logger.isTraceEnabled()) {
                        this.logger.trace((Object)("Subscription selector: [" + selector + "]"));
                    }
                }
                catch (Throwable ex) {
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug((Object)("Failed to parse selector: " + selector), ex);
                    }
                }
            }
        }
        return expression;
    }
    
    @Override
    protected void removeSubscriptionInternal(final String sessionId, final String subsId, final Message<?> message) {
        final SessionSubscriptionInfo info = this.subscriptionRegistry.getSubscriptions(sessionId);
        if (info != null) {
            final String destination = info.removeSubscription(subsId);
            if (destination != null) {
                this.destinationCache.updateAfterRemovedSubscription(sessionId, subsId);
            }
        }
    }
    
    @Override
    public void unregisterAllSubscriptions(final String sessionId) {
        final SessionSubscriptionInfo info = this.subscriptionRegistry.removeSubscriptions(sessionId);
        if (info != null) {
            this.destinationCache.updateAfterRemovedSession(info);
        }
    }
    
    @Override
    protected MultiValueMap<String, String> findSubscriptionsInternal(final String destination, final Message<?> message) {
        final MultiValueMap<String, String> result = (MultiValueMap<String, String>)this.destinationCache.getSubscriptions(destination, message);
        return this.filterSubscriptions(result, message);
    }
    
    private MultiValueMap<String, String> filterSubscriptions(final MultiValueMap<String, String> allMatches, final Message<?> message) {
        if (!this.selectorHeaderInUse) {
            return allMatches;
        }
        final MultiValueMap<String, String> result = (MultiValueMap<String, String>)new LinkedMultiValueMap(allMatches.size());
        final Iterator<String> iterator;
        String subId;
        SessionSubscriptionInfo info;
        Subscription sub;
        Expression expression;
        final MultiValueMap multiValueMap;
        allMatches.forEach((sessionId, subIds) -> {
            subIds.iterator();
            while (iterator.hasNext()) {
                subId = iterator.next();
                info = this.subscriptionRegistry.getSubscriptions(sessionId);
                if (info == null) {
                    continue;
                }
                else {
                    sub = info.getSubscription(subId);
                    if (sub == null) {
                        continue;
                    }
                    else {
                        expression = sub.getSelectorExpression();
                        if (expression == null) {
                            multiValueMap.add((Object)sessionId, (Object)subId);
                        }
                        else {
                            try {
                                if (Boolean.TRUE.equals(expression.getValue(DefaultSubscriptionRegistry.messageEvalContext, (Object)message, (Class)Boolean.class))) {
                                    multiValueMap.add((Object)sessionId, (Object)subId);
                                }
                                else {
                                    continue;
                                }
                            }
                            catch (SpelEvaluationException ex) {
                                if (this.logger.isDebugEnabled()) {
                                    this.logger.debug((Object)("Failed to evaluate selector: " + ex.getMessage()));
                                }
                                else {
                                    continue;
                                }
                            }
                            catch (Throwable ex2) {
                                this.logger.debug((Object)"Failed to evaluate selector", ex2);
                            }
                        }
                    }
                }
            }
            return;
        });
        return result;
    }
    
    @Override
    public String toString() {
        return "DefaultSubscriptionRegistry[" + this.destinationCache + ", " + this.subscriptionRegistry + "]";
    }
    
    static {
        messageEvalContext = (EvaluationContext)SimpleEvaluationContext.forPropertyAccessors(new PropertyAccessor[] { (PropertyAccessor)new SimpMessageHeaderPropertyAccessor() }).build();
    }
    
    private class DestinationCache
    {
        private final Map<String, LinkedMultiValueMap<String, String>> accessCache;
        private final Map<String, LinkedMultiValueMap<String, String>> updateCache;
        
        private DestinationCache() {
            this.accessCache = new ConcurrentHashMap<String, LinkedMultiValueMap<String, String>>(1024);
            this.updateCache = new LinkedHashMap<String, LinkedMultiValueMap<String, String>>(1024, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<String, LinkedMultiValueMap<String, String>> eldest) {
                    if (this.size() > DefaultSubscriptionRegistry.this.getCacheLimit()) {
                        DestinationCache.this.accessCache.remove(eldest.getKey());
                        return true;
                    }
                    return false;
                }
            };
        }
        
        public LinkedMultiValueMap<String, String> getSubscriptions(final String destination, final Message<?> message) {
            LinkedMultiValueMap<String, String> result = this.accessCache.get(destination);
            if (result == null) {
                synchronized (this.updateCache) {
                    result = (LinkedMultiValueMap<String, String>)new LinkedMultiValueMap();
                    for (final SessionSubscriptionInfo info : DefaultSubscriptionRegistry.this.subscriptionRegistry.getAllSubscriptions()) {
                        for (final String destinationPattern : info.getDestinations()) {
                            if (DefaultSubscriptionRegistry.this.getPathMatcher().match(destinationPattern, destination)) {
                                for (final Subscription sub : info.getSubscriptions(destinationPattern)) {
                                    result.add((Object)info.sessionId, (Object)sub.getId());
                                }
                            }
                        }
                    }
                    if (!result.isEmpty()) {
                        this.updateCache.put(destination, (LinkedMultiValueMap<String, String>)result.deepCopy());
                        this.accessCache.put(destination, result);
                    }
                }
            }
            return result;
        }
        
        public void updateAfterNewSubscription(final String destination, final String sessionId, final String subsId) {
            synchronized (this.updateCache) {
                List subsForSession;
                this.updateCache.forEach((cachedDestination, subscriptions) -> {
                    if (DefaultSubscriptionRegistry.this.getPathMatcher().match(destination, cachedDestination)) {
                        subsForSession = subscriptions.get((Object)sessionId);
                        if (subsForSession == null || !subsForSession.contains(subsId)) {
                            subscriptions.add((Object)sessionId, (Object)subsId);
                            this.accessCache.put(cachedDestination, (LinkedMultiValueMap<String, String>)subscriptions.deepCopy());
                        }
                    }
                });
            }
        }
        
        public void updateAfterRemovedSubscription(final String sessionId, final String subsId) {
            synchronized (this.updateCache) {
                final Set<String> destinationsToRemove = new HashSet<String>();
                final List<String> subscriptions;
                final Set<String> set;
                this.updateCache.forEach((destination, sessionMap) -> {
                    subscriptions = (List<String>)sessionMap.get((Object)sessionId);
                    if (subscriptions != null) {
                        subscriptions.remove(subsId);
                        if (subscriptions.isEmpty()) {
                            sessionMap.remove((Object)sessionId);
                        }
                        if (sessionMap.isEmpty()) {
                            set.add(destination);
                        }
                        else {
                            this.accessCache.put(destination, (LinkedMultiValueMap<String, String>)sessionMap.deepCopy());
                        }
                    }
                    return;
                });
                for (final String destination2 : destinationsToRemove) {
                    this.updateCache.remove(destination2);
                    this.accessCache.remove(destination2);
                }
            }
        }
        
        public void updateAfterRemovedSession(final SessionSubscriptionInfo info) {
            synchronized (this.updateCache) {
                final Set<String> destinationsToRemove = new HashSet<String>();
                final Set<String> set;
                this.updateCache.forEach((destination, sessionMap) -> {
                    if (sessionMap.remove((Object)info.getSessionId()) != null) {
                        if (sessionMap.isEmpty()) {
                            set.add(destination);
                        }
                        else {
                            this.accessCache.put(destination, (LinkedMultiValueMap<String, String>)sessionMap.deepCopy());
                        }
                    }
                    return;
                });
                for (final String destination2 : destinationsToRemove) {
                    this.updateCache.remove(destination2);
                    this.accessCache.remove(destination2);
                }
            }
        }
        
        @Override
        public String toString() {
            return "cache[" + this.accessCache.size() + " destination(s)]";
        }
    }
    
    private static class SessionSubscriptionRegistry
    {
        private final ConcurrentMap<String, SessionSubscriptionInfo> sessions;
        
        private SessionSubscriptionRegistry() {
            this.sessions = new ConcurrentHashMap<String, SessionSubscriptionInfo>();
        }
        
        @Nullable
        public SessionSubscriptionInfo getSubscriptions(final String sessionId) {
            return this.sessions.get(sessionId);
        }
        
        public Collection<SessionSubscriptionInfo> getAllSubscriptions() {
            return this.sessions.values();
        }
        
        public SessionSubscriptionInfo addSubscription(final String sessionId, final String subscriptionId, final String destination, @Nullable final Expression selectorExpression) {
            SessionSubscriptionInfo info = this.sessions.get(sessionId);
            if (info == null) {
                info = new SessionSubscriptionInfo(sessionId);
                final SessionSubscriptionInfo value = this.sessions.putIfAbsent(sessionId, info);
                if (value != null) {
                    info = value;
                }
            }
            info.addSubscription(destination, subscriptionId, selectorExpression);
            return info;
        }
        
        @Nullable
        public SessionSubscriptionInfo removeSubscriptions(final String sessionId) {
            return this.sessions.remove(sessionId);
        }
        
        @Override
        public String toString() {
            return "registry[" + this.sessions.size() + " sessions]";
        }
    }
    
    private static class SessionSubscriptionInfo
    {
        private final String sessionId;
        private final Map<String, Set<Subscription>> destinationLookup;
        
        public SessionSubscriptionInfo(final String sessionId) {
            this.destinationLookup = new ConcurrentHashMap<String, Set<Subscription>>(4);
            Assert.notNull((Object)sessionId, "'sessionId' must not be null");
            this.sessionId = sessionId;
        }
        
        public String getSessionId() {
            return this.sessionId;
        }
        
        public Set<String> getDestinations() {
            return this.destinationLookup.keySet();
        }
        
        public Set<Subscription> getSubscriptions(final String destination) {
            return this.destinationLookup.get(destination);
        }
        
        @Nullable
        public Subscription getSubscription(final String subscriptionId) {
            for (final Map.Entry<String, Set<Subscription>> destinationEntry : this.destinationLookup.entrySet()) {
                for (final Subscription sub : destinationEntry.getValue()) {
                    if (sub.getId().equalsIgnoreCase(subscriptionId)) {
                        return sub;
                    }
                }
            }
            return null;
        }
        
        public void addSubscription(final String destination, final String subscriptionId, @Nullable final Expression selectorExpression) {
            Set<Subscription> subs = this.destinationLookup.get(destination);
            if (subs == null) {
                synchronized (this.destinationLookup) {
                    subs = this.destinationLookup.get(destination);
                    if (subs == null) {
                        subs = new CopyOnWriteArraySet<Subscription>();
                        this.destinationLookup.put(destination, subs);
                    }
                }
            }
            subs.add(new Subscription(subscriptionId, selectorExpression));
        }
        
        @Nullable
        public String removeSubscription(final String subscriptionId) {
            for (final Map.Entry<String, Set<Subscription>> destinationEntry : this.destinationLookup.entrySet()) {
                final Set<Subscription> subs = destinationEntry.getValue();
                if (subs != null) {
                    for (final Subscription sub : subs) {
                        if (sub.getId().equals(subscriptionId) && subs.remove(sub)) {
                            synchronized (this.destinationLookup) {
                                if (subs.isEmpty()) {
                                    this.destinationLookup.remove(destinationEntry.getKey());
                                }
                            }
                            return destinationEntry.getKey();
                        }
                    }
                }
            }
            return null;
        }
        
        @Override
        public String toString() {
            return "[sessionId=" + this.sessionId + ", subscriptions=" + this.destinationLookup + "]";
        }
    }
    
    private static final class Subscription
    {
        private final String id;
        @Nullable
        private final Expression selectorExpression;
        
        public Subscription(final String id, @Nullable final Expression selector) {
            Assert.notNull((Object)id, "Subscription id must not be null");
            this.id = id;
            this.selectorExpression = selector;
        }
        
        public String getId() {
            return this.id;
        }
        
        @Nullable
        public Expression getSelectorExpression() {
            return this.selectorExpression;
        }
        
        @Override
        public boolean equals(@Nullable final Object other) {
            return this == other || (other instanceof Subscription && this.id.equals(((Subscription)other).id));
        }
        
        @Override
        public int hashCode() {
            return this.id.hashCode();
        }
        
        @Override
        public String toString() {
            return "subscription(id=" + this.id + ")";
        }
    }
    
    private static class SimpMessageHeaderPropertyAccessor implements PropertyAccessor
    {
        public Class<?>[] getSpecificTargetClasses() {
            return (Class<?>[])new Class[] { Message.class, MessageHeaders.class };
        }
        
        public boolean canRead(final EvaluationContext context, @Nullable final Object target, final String name) {
            return true;
        }
        
        public TypedValue read(final EvaluationContext context, @Nullable final Object target, final String name) {
            Object value;
            if (target instanceof Message) {
                value = (name.equals("headers") ? ((Message)target).getHeaders() : null);
            }
            else {
                if (!(target instanceof MessageHeaders)) {
                    throw new IllegalStateException("Expected Message or MessageHeaders.");
                }
                final MessageHeaders headers = (MessageHeaders)target;
                final SimpMessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(headers, SimpMessageHeaderAccessor.class);
                Assert.state(accessor != null, "No SimpMessageHeaderAccessor");
                if ("destination".equalsIgnoreCase(name)) {
                    value = accessor.getDestination();
                }
                else {
                    value = accessor.getFirstNativeHeader(name);
                    if (value == null) {
                        value = headers.get(name);
                    }
                }
            }
            return new TypedValue(value);
        }
        
        public boolean canWrite(final EvaluationContext context, @Nullable final Object target, final String name) {
            return false;
        }
        
        public void write(final EvaluationContext context, @Nullable final Object target, final String name, @Nullable final Object value) {
        }
    }
}
