// 
// Decompiled by Procyon v0.5.36
// 

package com.manydesigns.portofino.dispatcher.security.jwt;

import org.apache.commons.configuration2.Configuration;
import java.util.HashMap;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.shiro.authc.AuthenticationException;
import java.io.Serializable;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import java.security.Key;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import io.jsonwebtoken.Jwts;
import javax.crypto.spec.SecretKeySpec;
import io.jsonwebtoken.io.Decoders;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import java.util.Map;
import java.util.HashSet;
import org.apache.shiro.authz.Permission;
import java.util.Collection;
import com.manydesigns.portofino.dispatcher.security.RolesPermission;
import java.util.Set;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.realm.AuthorizingRealm;

public class JWTRealm extends AuthorizingRealm
{
    protected AuthorizationInfo doGetAuthorizationInfo(final PrincipalCollection principals) {
        final Set<String> roles = this.getRoles(principals);
        final SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo((Set)roles);
        simpleAuthorizationInfo.addObjectPermission((Permission)new RolesPermission(roles));
        return (AuthorizationInfo)simpleAuthorizationInfo;
    }
    
    protected Set<String> getRoles(final PrincipalCollection principals) {
        return this.getRoles(principals.getPrimaryPrincipal());
    }
    
    protected Set<String> getRoles(final Object principal) {
        final HashSet<String> roles = new HashSet<String>();
        if (principal instanceof Map) {
            final Object rolesList = ((Map)principal).get("roles");
            if (rolesList instanceof Collection) {
                roles.addAll((Collection<?>)rolesList);
            }
        }
        return roles;
    }
    
    protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken token) throws AuthenticationException {
        final String secret = this.getSecret();
        final Key key = new SecretKeySpec((byte[])Decoders.BASE64.decode((Object)secret), this.getSignatureAlgorithm().getJcaName());
        final Jws<Claims> jwt = (Jws<Claims>)Jwts.parser().setSigningKey(key).parseClaimsJws((String)token.getPrincipal());
        final Map<String, Serializable> principal = this.getPrincipal(jwt);
        return (AuthenticationInfo)new SimpleAuthenticationInfo((Object)principal, (Object)((String)token.getCredentials()).toCharArray(), this.getName());
    }
    
    protected SignatureAlgorithm getSignatureAlgorithm() {
        return SignatureAlgorithm.HS512;
    }
    
    protected Map<String, Serializable> getPrincipal(final Jws<Claims> jwt) {
        final Map<String, Serializable> principal = new HashMap<String, Serializable>();
        principal.put("jwt", (Serializable)jwt.getBody());
        return principal;
    }
    
    protected Configuration getConfiguration() {
        return null;
    }
    
    protected String getSecret() {
        return this.getConfiguration().getString("jwt.secret");
    }
    
    public boolean supports(final AuthenticationToken token) {
        return token instanceof JSONWebToken;
    }
}
