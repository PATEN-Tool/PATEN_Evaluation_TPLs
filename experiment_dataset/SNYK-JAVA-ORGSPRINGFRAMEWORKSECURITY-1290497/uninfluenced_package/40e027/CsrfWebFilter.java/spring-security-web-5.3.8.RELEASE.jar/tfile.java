// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.security.web.server.csrf;

import java.util.Collection;
import java.util.HashSet;
import java.util.Arrays;
import java.util.function.Predicate;
import org.springframework.http.HttpMethod;
import java.util.Set;
import org.springframework.util.MultiValueMap;
import org.reactivestreams.Publisher;
import org.springframework.security.crypto.codec.Utf8;
import java.security.MessageDigest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import java.util.function.Function;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.MediaType;
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
    public static final ServerWebExchangeMatcher DEFAULT_CSRF_MATCHER;
    private static final String SHOULD_NOT_FILTER;
    private ServerWebExchangeMatcher requireCsrfProtectionMatcher;
    private ServerCsrfTokenRepository csrfTokenRepository;
    private ServerAccessDeniedHandler accessDeniedHandler;
    private boolean isTokenFromMultipartDataEnabled;
    
    public CsrfWebFilter() {
        this.requireCsrfProtectionMatcher = CsrfWebFilter.DEFAULT_CSRF_MATCHER;
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
    
    public void setTokenFromMultipartDataEnabled(final boolean tokenFromMultipartDataEnabled) {
        this.isTokenFromMultipartDataEnabled = tokenFromMultipartDataEnabled;
    }
    
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        if (Boolean.TRUE.equals(exchange.getAttribute(CsrfWebFilter.SHOULD_NOT_FILTER))) {
            return (Mono<Void>)chain.filter(exchange).then(Mono.empty());
        }
        return (Mono<Void>)this.requireCsrfProtectionMatcher.matches(exchange).filter(ServerWebExchangeMatcher.MatchResult::isMatch).filter(matchResult -> !exchange.getAttributes().containsKey(CsrfToken.class.getName())).flatMap(m -> this.validateToken(exchange)).flatMap(m -> this.continueFilterChain(exchange, chain)).switchIfEmpty(this.continueFilterChain(exchange, chain).then(Mono.empty())).onErrorResume((Class)CsrfException.class, ex -> this.accessDeniedHandler.handle(exchange, ex));
    }
    
    public static void skipExchange(final ServerWebExchange exchange) {
        exchange.getAttributes().put(CsrfWebFilter.SHOULD_NOT_FILTER, Boolean.TRUE);
    }
    
    private Mono<Void> validateToken(final ServerWebExchange exchange) {
        return (Mono<Void>)this.csrfTokenRepository.loadToken(exchange).switchIfEmpty(Mono.defer(() -> Mono.error((Throwable)new CsrfException("An expected CSRF token cannot be found")))).filterWhen(expected -> this.containsValidCsrfToken(exchange, expected)).switchIfEmpty(Mono.defer(() -> Mono.error((Throwable)new CsrfException("Invalid CSRF Token")))).then();
    }
    
    private Mono<Boolean> containsValidCsrfToken(final ServerWebExchange exchange, final CsrfToken expected) {
        return (Mono<Boolean>)exchange.getFormData().flatMap(data -> Mono.justOrEmpty(data.getFirst((Object)expected.getParameterName()))).switchIfEmpty(Mono.justOrEmpty((Object)exchange.getRequest().getHeaders().getFirst(expected.getHeaderName()))).switchIfEmpty((Mono)this.tokenFromMultipartData(exchange, expected)).map(actual -> equalsConstantTime(actual, expected.getToken()));
    }
    
    private Mono<String> tokenFromMultipartData(final ServerWebExchange exchange, final CsrfToken expected) {
        if (!this.isTokenFromMultipartDataEnabled) {
            return (Mono<String>)Mono.empty();
        }
        final ServerHttpRequest request = exchange.getRequest();
        final HttpHeaders headers = request.getHeaders();
        final MediaType contentType = headers.getContentType();
        if (!contentType.includes(MediaType.MULTIPART_FORM_DATA)) {
            return (Mono<String>)Mono.empty();
        }
        return (Mono<String>)exchange.getMultipartData().map(d -> (Part)d.getFirst((Object)expected.getParameterName())).cast((Class)FormFieldPart.class).map((Function)FormFieldPart::value);
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
    
    private static boolean equalsConstantTime(final String expected, final String actual) {
        final byte[] expectedBytes = bytesUtf8(expected);
        final byte[] actualBytes = bytesUtf8(actual);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }
    
    private static byte[] bytesUtf8(final String s) {
        return (byte[])((s != null) ? Utf8.encode((CharSequence)s) : null);
    }
    
    private Mono<CsrfToken> generateToken(final ServerWebExchange exchange) {
        return (Mono<CsrfToken>)this.csrfTokenRepository.generateToken(exchange).delayUntil(token -> this.csrfTokenRepository.saveToken(exchange, token));
    }
    
    static {
        DEFAULT_CSRF_MATCHER = new DefaultRequireCsrfProtectionMatcher();
        SHOULD_NOT_FILTER = "SHOULD_NOT_FILTER" + CsrfWebFilter.class.getName();
    }
    
    private static class DefaultRequireCsrfProtectionMatcher implements ServerWebExchangeMatcher
    {
        private static final Set<HttpMethod> ALLOWED_METHODS;
        
        @Override
        public Mono<MatchResult> matches(final ServerWebExchange exchange) {
            return (Mono<MatchResult>)Mono.just((Object)exchange.getRequest()).flatMap(r -> Mono.justOrEmpty((Object)r.getMethod())).filter((Predicate)DefaultRequireCsrfProtectionMatcher.ALLOWED_METHODS::contains).flatMap(m -> MatchResult.notMatch()).switchIfEmpty((Mono)MatchResult.match());
        }
        
        static {
            ALLOWED_METHODS = new HashSet<HttpMethod>(Arrays.asList(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.TRACE, HttpMethod.OPTIONS));
        }
    }
}
