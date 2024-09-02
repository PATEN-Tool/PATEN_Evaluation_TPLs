// 
// Decompiled by Procyon v0.5.36
// 

package cn.fxbin.bubble.plugin.token;

import cn.fxbin.bubble.plugin.token.model.Tokens;
import cn.fxbin.bubble.plugin.token.exception.InvalidClaimException;
import cn.fxbin.bubble.plugin.token.exception.TokenExpiredException;
import java.util.Date;
import cn.fxbin.bubble.fireworks.core.util.StringUtils;
import io.jsonwebtoken.Jwts;
import cn.fxbin.bubble.fireworks.core.util.time.DateUtils;
import cn.fxbin.bubble.fireworks.core.util.SystemClock;
import cn.fxbin.bubble.fireworks.core.util.ObjectUtils;
import cn.fxbin.bubble.fireworks.core.util.CollectionUtils;
import cn.fxbin.bubble.fireworks.core.util.BeanUtils;
import java.util.Map;
import cn.fxbin.bubble.plugin.token.model.TokenPayload;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;
import java.security.Key;
import io.jsonwebtoken.SignatureAlgorithm;

public class DoubleJwt
{
    private long accessExpire;
    private long refreshExpire;
    private SignatureAlgorithm algorithm;
    private Key key;
    private String secret;
    
    public DoubleJwt(final long accessExpire, final long refreshExpire, final SignatureAlgorithm algorithm, final Key key, final String secret) {
        this.accessExpire = accessExpire;
        this.refreshExpire = refreshExpire;
        this.algorithm = algorithm;
        this.key = key;
        this.secret = secret;
    }
    
    public DoubleJwt(final long accessExpire, final long refreshExpire, final SignatureAlgorithm algorithm, final String secret) {
        this.accessExpire = accessExpire;
        this.refreshExpire = refreshExpire;
        this.algorithm = algorithm;
        this.key = Keys.hmacShaKeyFor((byte[])Decoders.BASE64.decode((Object)secret));
    }
    
    public DoubleJwt(final long accessExpire, final long refreshExpire, final String secret) {
        this.accessExpire = accessExpire;
        this.refreshExpire = refreshExpire;
        this.algorithm = SignatureAlgorithm.HS512;
        this.key = Keys.hmacShaKeyFor((byte[])Decoders.BASE64.decode((Object)secret));
    }
    
    public String generateToken(final String tokenType, final String identity, final String scope, final long expire) {
        final TokenPayload claims = TokenPayload.builder().type(tokenType).identity(identity).scope(scope).build();
        return this.generateToken(claims, expire);
    }
    
    public String generateToken(final String tokenType, final long identity, final String scope, final long expire) {
        final TokenPayload claims = TokenPayload.builder().type(tokenType).identity(String.valueOf(identity)).scope(scope).build();
        return this.generateToken(claims, expire);
    }
    
    public String generateToken(final String tokenType, final long identity, final String scope, final long expire, final Map<String, Object> extra) {
        return this.generateToken(tokenType, String.valueOf(identity), scope, expire, extra);
    }
    
    public String generateToken(final String tokenType, final String identity, final String scope, final long expire, final Map<String, Object> extra) {
        final TokenPayload claims = TokenPayload.builder().type(tokenType).identity(identity).scope(scope).extra(extra).build();
        return this.generateToken(claims, expire);
    }
    
    public String generateToken(final TokenPayload tokenPayload, final long expire) {
        final Map<String, Object> extra = tokenPayload.getExtra();
        tokenPayload.setExtra(null);
        final Map<String, Object> claims = (Map<String, Object>)BeanUtils.object2Map((Object)tokenPayload);
        if (CollectionUtils.isNotEmpty((Map)extra)) {
            extra.keySet().stream().filter(key -> ObjectUtils.isNotEmpty(extra.get(key))).forEach(key -> claims.put(key, extra.get(key)));
        }
        final Date now = DateUtils.toDate(SystemClock.INSTANCE.currentTimeMillis());
        final Date expireDate = DateUtils.toDate(now.getTime() + expire * 1000L);
        return Jwts.builder().setId(StringUtils.getUUID()).setHeaderParam("typ", (Object)"JWT").setSubject(String.valueOf(tokenPayload.getIdentity())).setClaims((Map)claims).setIssuedAt(now).setExpiration(expireDate).signWith(this.key, this.algorithm).compact();
    }
    
    public TokenPayload parseToken(final String token) {
        final Map<String, Object> mapObj = (Map<String, Object>)Jwts.parserBuilder().setSigningKey(this.key).build().parse(token).getBody();
        final TokenPayload payload = (TokenPayload)BeanUtils.map2Object((Map)mapObj, (Class)TokenPayload.class);
        this.checkTokenExpired(payload.getExp());
        return payload;
    }
    
    public TokenPayload parseAccessToken(final String token) {
        return this.parseAccessToken(token, "bubble-fireworks");
    }
    
    public TokenPayload parseAccessToken(final String token, final String scope) {
        final TokenPayload payload = this.parseToken(token);
        this.checkTokenScope(payload.getScope(), scope);
        this.checkTokenType(payload.getType(), "access");
        return payload;
    }
    
    public TokenPayload parseRefreshToken(final String token) {
        return this.parseRefreshToken(token, "bubble-fireworks");
    }
    
    public TokenPayload parseRefreshToken(final String token, final String scope) {
        final TokenPayload payload = this.parseToken(token);
        this.checkTokenScope(payload.getScope(), scope);
        this.checkTokenType(payload.getType(), "refresh");
        return payload;
    }
    
    private void checkTokenExpired(final Integer exp) {
        final long nowSeconds = SystemClock.INSTANCE.currentTimeMillis() / 1000L;
        if (nowSeconds > exp) {
            throw new TokenExpiredException("token is expired");
        }
    }
    
    private void checkTokenScope(final String scope, final String certScope) {
        if (scope == null || !scope.equals(certScope)) {
            throw new InvalidClaimException("token scope is invalid");
        }
    }
    
    private void checkTokenType(final String type, final String accessType) {
        if (type == null || !type.equals(accessType)) {
            throw new InvalidClaimException("token type is invalid");
        }
    }
    
    public String generateAccessToken(final long identity) {
        return this.generateToken("access", identity, "bubble-fireworks", this.accessExpire);
    }
    
    public String generateAccessToken(final String identity) {
        return this.generateToken("access", identity, "bubble-fireworks", this.accessExpire);
    }
    
    public String generateRefreshToken(final long identity) {
        return this.generateToken("refresh", identity, "bubble-fireworks", this.refreshExpire);
    }
    
    public String generateRefreshToken(final String identity) {
        return this.generateToken("refresh", identity, "bubble-fireworks", this.refreshExpire);
    }
    
    public Tokens generateTokens(final long identity) {
        return this.generateTokens(String.valueOf(identity));
    }
    
    public Tokens generateTokens(final String identity) {
        return this.generateTokens(identity, "bubble-fireworks");
    }
    
    public Tokens generateTokens(final long identity, final String scope) {
        return this.generateTokens(String.valueOf(identity), scope, null);
    }
    
    public Tokens generateTokens(final String identity, final String scope) {
        return this.generateTokens(identity, scope, null);
    }
    
    public Tokens generateTokens(final long identity, final Map<String, Object> extra) {
        return this.generateTokens(String.valueOf(identity), "bubble-fireworks", extra);
    }
    
    public Tokens generateTokens(final String identity, final Map<String, Object> extra) {
        return this.generateTokens(identity, "bubble-fireworks", extra);
    }
    
    public Tokens generateTokens(final String identity, final String scope, final Map<String, Object> extra) {
        final String access = this.generateToken("access", identity, scope, this.accessExpire, extra);
        final String refresh = this.generateToken("refresh", identity, scope, this.refreshExpire, extra);
        return new Tokens(access, refresh);
    }
    
    public SignatureAlgorithm getAlgorithm() {
        return this.algorithm;
    }
    
    public Key getKey() {
        return this.key;
    }
}
