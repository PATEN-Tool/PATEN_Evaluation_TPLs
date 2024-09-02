// 
// Decompiled by Procyon v0.5.36
// 

package cn.fxbin.bubble.plugin.token;

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
import java.security.Key;
import io.jsonwebtoken.SignatureAlgorithm;

public class SingleJwt
{
    private long expire;
    private SignatureAlgorithm algorithm;
    private Key key;
    
    public SingleJwt(final long expire, final SignatureAlgorithm algorithm, final Key key) {
        this.expire = expire;
        this.algorithm = algorithm;
        this.key = key;
    }
    
    public SingleJwt(final long expire) {
        this.expire = expire;
        this.algorithm = SignatureAlgorithm.HS512;
        this.key = Keys.secretKeyFor(this.algorithm);
    }
    
    public SingleJwt() {
        this.algorithm = SignatureAlgorithm.HS512;
        this.key = Keys.secretKeyFor(this.algorithm);
    }
    
    public String generateToken(final String tokenType, final String identity, final String scope, final long expire) {
        this.expire = expire;
        final TokenPayload claims = TokenPayload.builder().type(tokenType).identity(identity).scope(scope).build();
        return this.generateToken(claims);
    }
    
    public String generateToken(final String tokenType, final long identity, final String scope, final long expire) {
        this.expire = expire;
        final TokenPayload claims = TokenPayload.builder().type(tokenType).identity(String.valueOf(identity)).scope(scope).build();
        return this.generateToken(claims);
    }
    
    public String generateToken(final String tokenType, final long identity, final String scope, final long expire, final Map<String, Object> extra) {
        this.expire = expire;
        final TokenPayload claims = TokenPayload.builder().type(tokenType).identity(String.valueOf(identity)).scope(scope).extra(extra).build();
        return this.generateToken(claims);
    }
    
    public String generateToken(final TokenPayload tokenPayload) {
        final Map<String, Object> extra = tokenPayload.getExtra();
        tokenPayload.setExtra(null);
        final Map<String, Object> claims = (Map<String, Object>)BeanUtils.object2Map((Object)tokenPayload);
        if (CollectionUtils.isNotEmpty((Map)extra)) {
            extra.keySet().stream().filter(key -> ObjectUtils.isNotEmpty(extra.get(key))).forEach(key -> claims.put(key, extra.get(key)));
        }
        final Date now = DateUtils.toDate(SystemClock.INSTANCE.currentTimeMillis());
        final Date expireDate = DateUtils.toDate(now.getTime() + this.expire * 1000L);
        return Jwts.builder().setId(StringUtils.getUUID()).setHeaderParam("typ", (Object)"JWT").setSubject(String.valueOf(tokenPayload.getIdentity())).setClaims((Map)claims).setIssuedAt(now).setExpiration(expireDate).signWith(this.key).compact();
    }
    
    public TokenPayload parseToken(final String token) {
        final Map<String, Object> mapObj = (Map<String, Object>)Jwts.parserBuilder().setSigningKey(this.key).build().parseClaimsJws(token).getBody();
        final TokenPayload payload = (TokenPayload)BeanUtils.map2Object((Map)mapObj, (Class)TokenPayload.class);
        final long nowSeconds = SystemClock.INSTANCE.currentTimeMillis() / 1000L;
        if (nowSeconds > payload.getExp()) {
            throw new TokenExpiredException("token is expired");
        }
        return payload;
    }
    
    public SignatureAlgorithm getSignatureAlgorithm() {
        return this.algorithm;
    }
    
    public Long getExpire() {
        return this.expire;
    }
}
