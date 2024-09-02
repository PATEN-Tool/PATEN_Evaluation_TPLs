// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.security.web.server.csrf;

import java.util.Collection;
import java.util.HashSet;
import java.util.Arrays;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.HttpMethod;
import java.util.Set;
import org.springframework.util.MultiValueMap;
import org.reactivestreams.Publisher;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Mono;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.util.Assert;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.WebFilter;

public class CsrfWebFilter implements WebFilter
{
    private ServerWebExchangeMatcher requireCsrfProtectionMatcher;
    private ServerCsrfTokenRepository csrfTokenRepository;
    private ServerAccessDeniedHandler accessDeniedHandler;
    
    public CsrfWebFilter() {
        this.requireCsrfProtectionMatcher = new DefaultRequireCsrfProtectionMatcher();
        this.csrfTokenRepository = new WebSessionServerCsrfTokenRepository();
        this.accessDeniedHandler = new HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN);
    }
    
    public void setAccessDeniedHandler(final ServerAccessDeniedHandler accessDeniedHandler) {
        Assert.notNull((Object)accessDeniedHandler, "accessDeniedHandler");
        this.accessDeniedHandler = accessDeniedHandler;
    }
    
    public void setCsrfTokenRepository(final ServerCsrfTokenRepository csrfTokenRepository) {
        Assert.notNull((Object)csrfTokenRepository, "csrfTokenRepository cannot be null");
        this.csrfTokenRepository = csrfTokenRepository;
    }
    
    public void setRequireCsrfProtectionMatcher(final ServerWebExchangeMatcher requireCsrfProtectionMatcher) {
        Assert.notNull((Object)requireCsrfProtectionMatcher, "requireCsrfProtectionMatcher cannot be null");
        this.requireCsrfProtectionMatcher = requireCsrfProtectionMatcher;
    }
    
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        return (Mono<Void>)this.requireCsrfProtectionMatcher.matches(exchange).filter(matchResult -> matchResult.isMatch()).filter(matchResult -> !exchange.getAttributes().containsKey(CsrfToken.class.getName())).flatMap(m -> this.validateToken(exchange)).flatMap(m -> this.continueFilterChain(exchange, chain)).switchIfEmpty(this.continueFilterChain(exchange, chain).then(Mono.empty())).onErrorResume((Class)CsrfException.class, e -> this.accessDeniedHandler.handle(exchange, e));
    }
    
    private Mono<Void> validateToken(final ServerWebExchange exchange) {
        return (Mono<Void>)this.csrfTokenRepository.loadToken(exchange).switchIfEmpty(Mono.defer(() -> Mono.error((Throwable)new CsrfException("CSRF Token has been associated to this client")))).filterWhen(expected -> this.containsValidCsrfToken(exchange, expected)).switchIfEmpty(Mono.defer(() -> Mono.error((Throwable)new CsrfException("Invalid CSRF Token")))).then();
    }
    
    private Mono<Boolean> containsValidCsrfToken(final ServerWebExchange exchange, final CsrfToken expected) {
        return (Mono<Boolean>)exchange.getFormData().flatMap(data -> Mono.justOrEmpty(data.getFirst((Object)expected.getParameterName()))).switchIfEmpty(Mono.justOrEmpty((Object)exchange.getRequest().getHeaders().getFirst(expected.getHeaderName()))).map(actual -> actual.equals(expected.getToken()));
    }
    
    private Mono<Void> continueFilterChain(final ServerWebExchange exchange, final WebFilterChain chain) {
        final Mono<CsrfToken> csrfToken;
        return (Mono<Void>)Mono.defer(() -> {
            csrfToken = this.csrfToken(exchange);
            exchange.getAttributes().put(CsrfToken.class.getName(), csrfToken);
            return chain.filter(exchange);
        });
    }
    
    private Mono<CsrfToken> csrfToken(final ServerWebExchange exchange) {
        return (Mono<CsrfToken>)this.csrfTokenRepository.loadToken(exchange).switchIfEmpty((Mono)this.generateToken(exchange));
    }
    
    private Mono<CsrfToken> generateToken(final ServerWebExchange exchange) {
        return (Mono<CsrfToken>)this.csrfTokenRepository.generateToken(exchange).delayUntil(token -> this.csrfTokenRepository.saveToken(exchange, token));
    }
    
    private static class DefaultRequireCsrfProtectionMatcher implements ServerWebExchangeMatcher
    {
        private static final Set<HttpMethod> ALLOWED_METHODS;
        
        @Override
        public Mono<MatchResult> matches(final ServerWebExchange exchange) {
            return (Mono<MatchResult>)Mono.just((Object)exchange.getRequest()).flatMap(r -> Mono.justOrEmpty((Object)r.getMethod())).filter(m -> DefaultRequireCsrfProtectionMatcher.ALLOWED_METHODS.contains(m)).flatMap(m -> MatchResult.notMatch()).switchIfEmpty((Mono)MatchResult.match());
        }
        
        static {
            ALLOWED_METHODS = new HashSet<HttpMethod>(Arrays.asList(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.TRACE, HttpMethod.OPTIONS));
        }
    }
}
