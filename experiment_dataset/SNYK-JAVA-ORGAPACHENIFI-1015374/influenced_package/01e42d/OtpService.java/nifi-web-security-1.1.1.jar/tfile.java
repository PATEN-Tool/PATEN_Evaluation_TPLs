// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.nifi.web.security.otp;

import org.slf4j.LoggerFactory;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Base64;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentMap;
import org.apache.nifi.web.security.token.OtpAuthenticationToken;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import org.apache.nifi.web.security.util.CacheKey;
import com.google.common.cache.Cache;
import org.slf4j.Logger;

public class OtpService
{
    private static final Logger logger;
    private static final String HMAC_SHA256 = "HmacSHA256";
    protected static final int MAX_CACHE_SOFT_LIMIT = 100;
    private final Cache<CacheKey, String> downloadTokenCache;
    private final Cache<CacheKey, String> uiExtensionCache;
    
    public OtpService() {
        this(5, TimeUnit.MINUTES);
    }
    
    public OtpService(final int duration, final TimeUnit units) {
        this.downloadTokenCache = (Cache<CacheKey, String>)CacheBuilder.newBuilder().expireAfterWrite((long)duration, units).build();
        this.uiExtensionCache = (Cache<CacheKey, String>)CacheBuilder.newBuilder().expireAfterWrite((long)duration, units).build();
    }
    
    public String generateDownloadToken(final OtpAuthenticationToken authenticationToken) {
        return this.generateToken(this.downloadTokenCache.asMap(), authenticationToken);
    }
    
    public String getAuthenticationFromDownloadToken(final String token) throws OtpAuthenticationException {
        return this.getAuthenticationFromToken(this.downloadTokenCache.asMap(), token);
    }
    
    public String generateUiExtensionToken(final OtpAuthenticationToken authenticationToken) {
        return this.generateToken(this.uiExtensionCache.asMap(), authenticationToken);
    }
    
    public String getAuthenticationFromUiExtensionToken(final String token) throws OtpAuthenticationException {
        return this.getAuthenticationFromToken(this.uiExtensionCache.asMap(), token);
    }
    
    private String generateToken(final ConcurrentMap<CacheKey, String> cache, final OtpAuthenticationToken authenticationToken) {
        if (cache.size() >= 100) {
            throw new IllegalStateException("The maximum number of single use tokens have been issued.");
        }
        final CacheKey cacheKey = new CacheKey(this.hash(authenticationToken));
        cache.putIfAbsent(cacheKey, authenticationToken.getName());
        return cacheKey.getKey();
    }
    
    private String getAuthenticationFromToken(final ConcurrentMap<CacheKey, String> cache, final String token) throws OtpAuthenticationException {
        final String authenticatedUser = cache.remove(new CacheKey(token));
        if (authenticatedUser == null) {
            throw new OtpAuthenticationException("Unable to validate the access token.");
        }
        return authenticatedUser;
    }
    
    private String hash(final OtpAuthenticationToken authenticationToken) {
        try {
            final String input = authenticationToken.getName() + "-" + System.nanoTime();
            final SecureRandom secureRandom = new SecureRandom();
            final byte[] randomBytes = new byte[32];
            secureRandom.nextBytes(randomBytes);
            final SecretKeySpec secret = new SecretKeySpec(randomBytes, "HmacSHA256");
            final Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            hmacSha256.init(secret);
            final byte[] output = hmacSha256.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeBase64URLSafeString(output);
        }
        catch (NoSuchAlgorithmException | InvalidKeyException ex2) {
            final GeneralSecurityException ex;
            final GeneralSecurityException e = ex;
            final String errorMessage = "There was an error generating the OTP";
            OtpService.logger.error("There was an error generating the OTP", (Throwable)e);
            throw new IllegalStateException("Unable to generate single use token.");
        }
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)OtpService.class);
    }
}
