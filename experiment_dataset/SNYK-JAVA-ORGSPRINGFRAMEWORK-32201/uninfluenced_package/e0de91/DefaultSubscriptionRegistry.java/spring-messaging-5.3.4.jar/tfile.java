// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.messaging.simp.broker;

import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.util.Assert;
import java.util.function.BiConsumer;
import java.util.concurrent.ConcurrentMap;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.expression.PropertyAccessor;
import java.util.List;
import org.springframework.expression.spel.SpelEvaluationException;
import java.util.Iterator;
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
    private int cacheLimit;
    @Nullable
    private String selectorHeaderName;
    private volatile boolean selectorHeaderInUse;
    private final ExpressionParser expressionParser;
    private final DestinationCache destinationCache;
    private final SessionRegistry sessionRegistry;
    
    public DefaultSubscriptionRegistry() {
        this.pathMatcher = (PathMatcher)new AntPathMatcher();
        this.cacheLimit = 1024;
        this.selectorHeaderName = "selector";
        this.expressionParser = (ExpressionParser)new SpelExpressionParser();
        this.destinationCache = new DestinationCache();
        this.sessionRegistry = new SessionRegistry();
    }
    
    public void setPathMatcher(final PathMatcher pathMatcher) {
        this.pathMatcher = pathMatcher;
    }
    
    public PathMatcher getPathMatcher() {
        return this.pathMatcher;
    }
    
    public void setCacheLimit(final int cacheLimit) {
        this.cacheLimit = cacheLimit;
        this.destinationCache.ensureCacheLimit();
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
    protected void addSubscriptionInternal(final String sessionId, final String subscriptionId, final String destination, final Message<?> message) {
        final boolean isPattern = this.pathMatcher.isPattern(destination);
        final Expression expression = this.getSelectorExpression(message.getHeaders());
        final Subscription subscription = new Subscription(subscriptionId, destination, isPattern, expression);
        this.sessionRegistry.addSubscription(sessionId, subscription);
        this.destinationCache.updateAfterNewSubscription(sessionId, subscription);
    }
    
    @Nullable
    private Expression getSelectorExpression(final MessageHeaders headers) {
        if (this.getSelectorHeaderName() == null) {
            return null;
        }
        final String selector = NativeMessageHeaderAccessor.getFirstNativeHeader(this.getSelectorHeaderName(), headers);
        if (selector == null) {
            return null;
        }
        Expression expression = null;
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
        return expression;
    }
    
    @Override
    protected void removeSubscriptionInternal(final String sessionId, final String subscriptionId, final Message<?> message) {
        final SessionInfo info = this.sessionRegistry.getSession(sessionId);
        if (info != null) {
            final Subscription subscription = info.removeSubscription(subscriptionId);
            if (subscription != null) {
                this.destinationCache.updateAfterRemovedSubscription(sessionId, subscription);
            }
        }
    }
    
    @Override
    public void unregisterAllSubscriptions(final String sessionId) {
        final SessionInfo info = this.sessionRegistry.removeSubscriptions(sessionId);
        if (info != null) {
            this.destinationCache.updateAfterRemovedSession(sessionId, info);
        }
    }
    
    @Override
    protected MultiValueMap<String, String> findSubscriptionsInternal(final String destination, final Message<?> message) {
        final MultiValueMap<String, String> allMatches = (MultiValueMap<String, String>)this.destinationCache.getSubscriptions(destination);
        if (!this.selectorHeaderInUse) {
            return allMatches;
        }
        final MultiValueMap<String, String> result = (MultiValueMap<String, String>)new LinkedMultiValueMap(allMatches.size());
        final SessionInfo info;
        final Iterator<String> iterator;
        String subscriptionId;
        Subscription subscription;
        final MultiValueMap multiValueMap;
        allMatches.forEach((sessionId, subscriptionIds) -> {
            info = this.sessionRegistry.getSession(sessionId);
            if (info != null) {
                subscriptionIds.iterator();
                while (iterator.hasNext()) {
                    subscriptionId = iterator.next();
                    subscription = info.getSubscription(subscriptionId);
                    if (subscription != null && this.evaluateExpression(subscription.getSelector(), message)) {
                        multiValueMap.add((Object)sessionId, (Object)subscription.getId());
                    }
                }
            }
            return;
        });
        return result;
    }
    
    private boolean evaluateExpression(@Nullable final Expression expression, final Message<?> message) {
        if (expression == null) {
            return true;
        }
        try {
            final Boolean result = (Boolean)expression.getValue(DefaultSubscriptionRegistry.messageEvalContext, (Object)message, (Class)Boolean.class);
            if (Boolean.TRUE.equals(result)) {
                return true;
            }
        }
        catch (SpelEvaluationException ex) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug((Object)("Failed to evaluate selector: " + ex.getMessage()));
            }
        }
        catch (Throwable ex2) {
            this.logger.debug((Object)"Failed to evaluate selector", ex2);
        }
        return false;
    }
    
    static {
        messageEvalContext = (EvaluationContext)SimpleEvaluationContext.forPropertyAccessors(new PropertyAccessor[] { (PropertyAccessor)new SimpMessageHeaderPropertyAccessor() }).build();
    }
    
    private final class DestinationCache
    {
        private final Map<String, LinkedMultiValueMap<String, String>> destinationCache;
        private final AtomicInteger cacheSize;
        private final Queue<String> cacheEvictionPolicy;
        
        private DestinationCache() {
            this.destinationCache = new ConcurrentHashMap<String, LinkedMultiValueMap<String, String>>(1024);
            this.cacheSize = new AtomicInteger();
            this.cacheEvictionPolicy = new ConcurrentLinkedQueue<String>();
        }
        
        public LinkedMultiValueMap<String, String> getSubscriptions(final String destination) {
            LinkedMultiValueMap<String, String> sessionIdToSubscriptionIds = this.destinationCache.get(destination);
            if (sessionIdToSubscriptionIds == null) {
                final LinkedMultiValueMap<String, String> matches;
                sessionIdToSubscriptionIds = this.destinationCache.computeIfAbsent(destination, _destination -> {
                    matches = this.computeMatchingSubscriptions(destination);
                    this.cacheEvictionPolicy.add(destination);
                    this.cacheSize.incrementAndGet();
                    return matches;
                });
                this.ensureCacheLimit();
            }
            return sessionIdToSubscriptionIds;
        }
        
        private LinkedMultiValueMap<String, String> computeMatchingSubscriptions(final String destination) {
            final LinkedMultiValueMap<String, String> sessionIdToSubscriptionIds = (LinkedMultiValueMap<String, String>)new LinkedMultiValueMap();
            final LinkedMultiValueMap<String, String> linkedMultiValueMap;
            DefaultSubscriptionRegistry.this.sessionRegistry.forEachSubscription((sessionId, subscription) -> {
                if (subscription.isPattern()) {
                    if (DefaultSubscriptionRegistry.this.pathMatcher.match(subscription.getDestination(), destination)) {
                        this.addMatchedSubscriptionId(linkedMultiValueMap, sessionId, subscription.getId());
                    }
                }
                else if (destination.equals(subscription.getDestination())) {
                    this.addMatchedSubscriptionId(linkedMultiValueMap, sessionId, subscription.getId());
                }
                return;
            });
            return sessionIdToSubscriptionIds;
        }
        
        private void addMatchedSubscriptionId(final LinkedMultiValueMap<String, String> sessionIdToSubscriptionIds, final String sessionId, final String subscriptionId) {
            List<String> result;
            sessionIdToSubscriptionIds.compute((Object)sessionId, (_sessionId, subscriptionIds) -> {
                if (subscriptionIds == null) {
                    return Collections.singletonList(subscriptionId);
                }
                else {
                    result = new ArrayList<String>(subscriptionIds.size() + 1);
                    result.addAll(subscriptionIds);
                    result.add(subscriptionId);
                    return result;
                }
            });
        }
        
        private void ensureCacheLimit() {
            int size = this.cacheSize.get();
            if (size > DefaultSubscriptionRegistry.this.cacheLimit) {
                do {
                    if (this.cacheSize.compareAndSet(size, size - 1)) {
                        final String head = this.cacheEvictionPolicy.remove();
                        this.destinationCache.remove(head);
                    }
                } while ((size = this.cacheSize.get()) > DefaultSubscriptionRegistry.this.cacheLimit);
            }
        }
        
        public void updateAfterNewSubscription(final String sessionId, final Subscription subscription) {
            if (subscription.isPattern()) {
                for (final String cachedDestination : this.destinationCache.keySet()) {
                    if (DefaultSubscriptionRegistry.this.pathMatcher.match(subscription.getDestination(), cachedDestination)) {
                        this.addToDestination(cachedDestination, sessionId, subscription.getId());
                    }
                }
            }
            else {
                this.addToDestination(subscription.getDestination(), sessionId, subscription.getId());
            }
        }
        
        private void addToDestination(final String destination, final String sessionId, final String subscriptionId) {
            this.destinationCache.computeIfPresent(destination, (_destination, sessionIdToSubscriptionIds) -> {
                sessionIdToSubscriptionIds = (LinkedMultiValueMap<String, String>)sessionIdToSubscriptionIds.clone();
                this.addMatchedSubscriptionId(sessionIdToSubscriptionIds, sessionId, subscriptionId);
                return sessionIdToSubscriptionIds;
            });
        }
        
        public void updateAfterRemovedSubscription(final String sessionId, final Subscription subscription) {
            if (subscription.isPattern()) {
                final String subscriptionId = subscription.getId();
                final List subscriptionIds;
                final String subscriptionId2;
                this.destinationCache.forEach((destination, sessionIdToSubscriptionIds) -> {
                    subscriptionIds = sessionIdToSubscriptionIds.get((Object)sessionId);
                    if (subscriptionIds != null && subscriptionIds.contains(subscriptionId2)) {
                        this.removeInternal(destination, sessionId, subscriptionId2);
                    }
                });
            }
            else {
                this.removeInternal(subscription.getDestination(), sessionId, subscription.getId());
            }
        }
        
        private void removeInternal(final String destination, final String sessionId, final String subscriptionId) {
            this.destinationCache.computeIfPresent(destination, (_destination, sessionIdToSubscriptionIds) -> {
                sessionIdToSubscriptionIds = (List<Object>)((LinkedMultiValueMap)sessionIdToSubscriptionIds).clone();
                ((LinkedMultiValueMap)sessionIdToSubscriptionIds).computeIfPresent((Object)sessionId, (_sessionId, subscriptionIds) -> {
                    if (subscriptionIds.size() == 1 && subscriptionId.equals(subscriptionIds.get(0))) {
                        return null;
                    }
                    else {
                        subscriptionIds = new ArrayList<Object>(subscriptionIds);
                        subscriptionIds.remove(subscriptionId);
                        return subscriptionIds.isEmpty() ? null : subscriptionIds;
                    }
                });
                return sessionIdToSubscriptionIds;
            });
        }
        
        public void updateAfterRemovedSession(final String sessionId, final SessionInfo info) {
            for (final Subscription subscription : info.getSubscriptions()) {
                this.updateAfterRemovedSubscription(sessionId, subscription);
            }
        }
    }
    
    private static final class SessionRegistry
    {
        private final ConcurrentMap<String, SessionInfo> sessions;
        
        private SessionRegistry() {
            this.sessions = new ConcurrentHashMap<String, SessionInfo>();
        }
        
        @Nullable
        public SessionInfo getSession(final String sessionId) {
            return this.sessions.get(sessionId);
        }
        
        public void forEachSubscription(final BiConsumer<String, Subscription> consumer) {
            this.sessions.forEach((sessionId, info) -> info.getSubscriptions().forEach(subscription -> consumer.accept(sessionId, subscription)));
        }
        
        public void addSubscription(final String sessionId, final Subscription subscription) {
            final SessionInfo info = this.sessions.computeIfAbsent(sessionId, _sessionId -> new SessionInfo());
            info.addSubscription(subscription);
        }
        
        @Nullable
        public SessionInfo removeSubscriptions(final String sessionId) {
            return this.sessions.remove(sessionId);
        }
    }
    
    private static final class SessionInfo
    {
        private final Map<String, Subscription> subscriptionMap;
        
        private SessionInfo() {
            this.subscriptionMap = new ConcurrentHashMap<String, Subscription>();
        }
        
        public Collection<Subscription> getSubscriptions() {
            return this.subscriptionMap.values();
        }
        
        @Nullable
        public Subscription getSubscription(final String subscriptionId) {
            return this.subscriptionMap.get(subscriptionId);
        }
        
        public void addSubscription(final Subscription subscription) {
            this.subscriptionMap.putIfAbsent(subscription.getId(), subscription);
        }
        
        @Nullable
        public Subscription removeSubscription(final String subscriptionId) {
            return this.subscriptionMap.remove(subscriptionId);
        }
    }
    
    private static final class Subscription
    {
        private final String id;
        private final String destination;
        private final boolean isPattern;
        @Nullable
        private final Expression selector;
        
        public Subscription(final String id, final String destination, final boolean isPattern, @Nullable final Expression selector) {
            Assert.notNull((Object)id, "Subscription id must not be null");
            Assert.notNull((Object)destination, "Subscription destination must not be null");
            this.id = id;
            this.selector = selector;
            this.destination = destination;
            this.isPattern = isPattern;
        }
        
        public String getId() {
            return this.id;
        }
        
        public String getDestination() {
            return this.destination;
        }
        
        public boolean isPattern() {
            return this.isPattern;
        }
        
        @Nullable
        public Expression getSelector() {
            return this.selector;
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
