package cn.fxbin.bubble.plugin.token;


import cn.fxbin.bubble.fireworks.core.util.*;
import cn.fxbin.bubble.fireworks.core.util.time.DateUtils;
import cn.fxbin.bubble.plugin.token.exception.TokenExpiredException;
import cn.fxbin.bubble.plugin.token.model.TokenPayload;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jdk.nashorn.internal.parser.Token;

import java.security.Key;
import java.util.Date;
import java.util.Map;

/**
 * SingleJWT ?????
 *
 * @author fxbin
 * @version v1.0
 * @since 2020/11/11 16:43
 */
public class SingleJwt {

    /**
     * ????
     */
    private long expire;

    /**
     * ????
     */
    private SignatureAlgorithm algorithm;

    /**
     * ???????
     */
    private Key key;


    public SingleJwt(long expire, SignatureAlgorithm algorithm, Key key) {
        this.expire = expire;
        this.algorithm = algorithm;
        this.key = key;
    }

    public SingleJwt(long expire) {
        this.expire = expire;
        this.algorithm = SignatureAlgorithm.HS512;
        this.key = Keys.secretKeyFor(this.algorithm);
    }

    public SingleJwt() {
        this.algorithm = SignatureAlgorithm.HS512;
        this.key = Keys.secretKeyFor(this.algorithm);
    }

    /**
     * generateToken
     *
     * @author fxbin
     * @since 2020/11/14 18:32
     * @param tokenType ??token type
     * @param identity ?????ex: userId
     * @param scope ?????
     * @param expire ?????????
     * @return java.lang.String
     */
    public String generateToken(String tokenType, String identity, String scope, long expire) {
        this.expire = expire;
        TokenPayload claims = TokenPayload.builder()
                .type(tokenType)
                .identity(identity)
                .scope(scope)
                .build();
        return generateToken(claims);
    }

    /**
     * generateToken
     *
     * @author fxbin
     * @since 2020/11/14 18:32
     * @param tokenType ??token type
     * @param identity ?????ex: userId
     * @param scope ?????
     * @param expire ?????????
     * @return java.lang.String
     */
    public String generateToken(String tokenType, long identity, String scope, long expire) {
        this.expire = expire;
        TokenPayload claims = TokenPayload.builder()
                .type(tokenType)
                .identity(String.valueOf(identity))
                .scope(scope)
                .build();
        return generateToken(claims);
    }

    /**
     * generateToken
     *
     * @author fxbin
     * @since 2020/11/14 18:31
     * @param tokenType ??token type
     * @param identity ?????ex: userId
     * @param scope ?????
     * @param expire ?????????
     * @param extra ???????
     * @return java.lang.String
     */
    public String generateToken(String tokenType, long identity, String scope, long expire, Map<String, Object> extra) {
        this.expire = expire;
        TokenPayload claims = TokenPayload.builder()
                .type(tokenType)
                .identity(String.valueOf(identity))
                .scope(scope)
                .extra(extra)
                .build();
        return generateToken(claims);
    }

    /**
     * generateToken
     *
     * @author fxbin
     * @since 2020/11/14 18:23
     * @param tokenPayload {@link TokenPayload}
     * @return java.lang.String
     */
    @SuppressWarnings("DuplicatedCode")
    public String generateToken(TokenPayload tokenPayload) {
        Map<String, Object> extra = tokenPayload.getExtra();
        tokenPayload.setExtra(null);
        Map<String, Object> claims = BeanUtils.object2Map(tokenPayload);
        if (CollectionUtils.isNotEmpty(extra)) {
            extra.keySet().stream().filter(key -> ObjectUtils.isNotEmpty(extra.get(key)))
                    .forEach(key -> claims.put(key, extra.get(key)));
        }
        Date now = DateUtils.toDate(SystemClock.INSTANCE.currentTimeMillis());
        Date expireDate = DateUtils.toDate(now.getTime() + expire * 1000);

        return Jwts.builder()
                .setId(StringUtils.getUUID())
                .setHeaderParam("typ", "JWT")
                .setSubject(String.valueOf(tokenPayload.getIdentity()))
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(key)
                .compact();
    }

    /**
     * parseToken
     *
     * <p>
     *     Jwt Token ??, jjwt-impl ?????????
     *     {@link io.jsonwebtoken.impl.JwtMap#setDateAsSeconds(java.lang.String, java.util.Date)}
     * </p>
     *
     * @author fxbin
     * @since 2020/11/13 16:45
     * @param token jwt token
     * @return java.util.Map<java.lang.String,java.lang.Object>
     */
    @SuppressWarnings("unchecked")
    public TokenPayload parseToken(String token) {
        Map<String, Object> mapObj = (Map<String, Object>) Jwts.parserBuilder()
                .setSigningKey(key).build()
                .parseClaimsJws(token)
                .getBody();

        TokenPayload payload = BeanUtils.map2Object(mapObj, TokenPayload.class);
        long nowSeconds = SystemClock.INSTANCE.currentTimeMillis() / 1000;
        if (nowSeconds > payload.getExp().longValue()) {
            throw new TokenExpiredException("token is expired");
        }
        return payload;
    }

    /**
     * getSignatureAlgorithm
     *
     * @author fxbin
     * @since 2020/11/11 16:56
     * @return io.jsonwebtoken.SignatureAlgorithm
     */
    public SignatureAlgorithm getSignatureAlgorithm() {
        return algorithm;
    }

    /**
     * getExpire
     *
     * @author fxbin
     * @since 2020/11/11 16:56
     * @return java.lang.Long
     */
    public Long getExpire() {
        return expire;
    }
}
