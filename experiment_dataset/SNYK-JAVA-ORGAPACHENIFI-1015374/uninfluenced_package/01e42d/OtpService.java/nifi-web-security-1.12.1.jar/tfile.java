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
import org.apache.nifi.web.security.util.CacheKey;
import org.apache.nifi.web.security.token.OtpAuthenticationToken;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

public class OtpService
{
    private static final Logger logger;
    private static final String HMAC_SHA256 = "HmacSHA256";
    protected static final int MAX_CACHE_SOFT_LIMIT = 100;
    private final TokenCache downloadTokens;
    private final TokenCache uiExtensionTokens;
    
    public OtpService() {
        this(5, TimeUnit.MINUTES);
    }
    
    public OtpService(final int duration, final TimeUnit units) {
        this.downloadTokens = new TokenCache("download tokens", duration, units);
        this.uiExtensionTokens = new TokenCache("UI extension tokens", duration, units);
    }
    
    public String generateDownloadToken(final OtpAuthenticationToken authenticationToken) {
        return this.generateToken(this.downloadTokens, authenticationToken);
    }
    
    public String getAuthenticationFromDownloadToken(final String token) throws OtpAuthenticationException {
        return this.getAuthenticationFromToken(this.downloadTokens, token);
    }
    
    public String generateUiExtensionToken(final OtpAuthenticationToken authenticationToken) {
        return this.generateToken(this.uiExtensionTokens, authenticationToken);
    }
    
    public String getAuthenticationFromUiExtensionToken(final String token) throws OtpAuthenticationException {
        return this.getAuthenticationFromToken(this.uiExtensionTokens, token);
    }
    
    private String generateToken(final TokenCache tokenCache, final OtpAuthenticationToken authenticationToken) {
        final String userId = (String)authenticationToken.getPrincipal();
        if (tokenCache.containsValue(userId)) {
            return tokenCache.getKeyForValue(userId).getKey();
        }
        if (tokenCache.size() >= 100L) {
            throw new IllegalStateException("The maximum number of single use tokens have been issued.");
        }
        final CacheKey cacheKey = new CacheKey(this.hash(authenticationToken));
        tokenCache.put(cacheKey, userId);
        return cacheKey.getKey();
    }
    
    private String getAuthenticationFromToken(final TokenCache tokenCache, final String token) throws OtpAuthenticationException {
        final CacheKey cacheKey = new CacheKey(token);
        final String authenticatedUser = tokenCache.getIfPresent(cacheKey);
        if (authenticatedUser == null) {
            throw new OtpAuthenticationException("Unable to validate the access token.");
        }
        tokenCache.invalidate(cacheKey);
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
