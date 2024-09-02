// 
// Decompiled by Procyon v0.5.36
// 

package com.manydesigns.portofino.shiro;

import org.slf4j.LoggerFactory;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.PasswordMatcher;
import com.manydesigns.elements.reflection.JavaClassAccessor;
import com.manydesigns.elements.reflection.ClassAccessor;
import java.util.LinkedHashSet;
import org.apache.shiro.authz.AuthorizationException;
import java.io.Serializable;
import java.util.HashSet;
import org.apache.shiro.authz.Permission;
import java.util.Collections;
import java.util.Collection;
import com.manydesigns.portofino.security.SecurityLogic;
import java.util.Set;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.jetbrains.annotations.NotNull;
import javax.crypto.spec.SecretKeySpec;
import io.jsonwebtoken.io.Decoders;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import io.jsonwebtoken.SignatureAlgorithm;
import org.joda.time.DateTime;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.io.ObjectStreamClass;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import org.apache.shiro.codec.Base64;
import java.util.Map;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import java.security.Key;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import io.jsonwebtoken.JwtException;
import org.apache.shiro.authc.AuthenticationException;
import io.jsonwebtoken.Jwts;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.crypto.hash.format.HashFormat;
import org.apache.shiro.crypto.hash.HashService;
import org.slf4j.Logger;
import org.apache.shiro.authc.credential.PasswordService;
import com.manydesigns.portofino.code.CodeBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.configuration2.Configuration;
import org.apache.shiro.realm.AuthorizingRealm;

public abstract class AbstractPortofinoRealm extends AuthorizingRealm implements PortofinoRealm
{
    public static final String copyright = "Copyright (C) 2005-2020 ManyDesigns srl";
    public static final String JWT_EXPIRATION_PROPERTY = "jwt.expiration";
    public static final String JWT_SECRET_PROPERTY = "jwt.secret";
    @Autowired
    protected Configuration portofinoConfiguration;
    @Autowired
    protected CodeBase codeBase;
    protected PasswordService passwordService;
    protected boolean legacyHashing;
    private static final Logger logger;
    
    protected AbstractPortofinoRealm() {
        this.legacyHashing = false;
        this.setup((HashService)new PlaintextHashService(), (HashFormat)new PlaintextHashFormat());
        this.legacyHashing = true;
    }
    
    public boolean supports(final AuthenticationToken token) {
        return token instanceof JSONWebToken || super.supports(token);
    }
    
    public AuthenticationInfo loadAuthenticationInfo(final JSONWebToken token) {
        final Key key = this.getJWTKey();
        Jws<Claims> jwt;
        try {
            jwt = (Jws<Claims>)Jwts.parser().setSigningKey(key).parseClaimsJws(token.getPrincipal());
        }
        catch (JwtException e) {
            throw new AuthenticationException((Throwable)e);
        }
        final String credentials = this.legacyHashing ? token.getCredentials() : this.encryptPassword(token.getCredentials());
        final Object principal = this.extractPrincipalFromWebToken(jwt);
        return (AuthenticationInfo)new SimpleAuthenticationInfo(principal, (Object)credentials, this.getName());
    }
    
    protected Object extractPrincipalFromWebToken(final Jws<Claims> jwt) {
        final Map<String, Object> body = (Map<String, Object>)jwt.getBody();
        final String base64Principal = body.get("serialized-principal");
        final byte[] serializedPrincipal = Base64.decode(base64Principal);
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Object principal;
        try {
            Thread.currentThread().setContextClassLoader(this.codeBase.asClassLoader());
            final ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(serializedPrincipal)) {
                @Override
                protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                    return (Class<?>)AbstractPortofinoRealm.this.codeBase.loadClass(desc.getName());
                }
            };
            principal = objectInputStream.readObject();
            objectInputStream.close();
        }
        catch (Exception e) {
            throw new AuthenticationException((Throwable)e);
        }
        finally {
            Thread.currentThread().setContextClassLoader(loader);
        }
        return principal;
    }
    
    public String generateWebToken(final Object principal) {
        final Key key = this.getJWTKey();
        final Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("principal", this.getPrincipalForWebToken(principal));
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            final ObjectOutputStream objectOutputStream = new ObjectOutputStream(bytes);
            objectOutputStream.writeObject(principal);
            objectOutputStream.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        claims.put("serialized-principal", bytes.toByteArray());
        final int expireAfterMinutes = this.portofinoConfiguration.getInt("jwt.expiration", 30);
        return Jwts.builder().setClaims((Map)claims).setExpiration(new DateTime().plusMinutes(expireAfterMinutes).toDate()).signWith(key, SignatureAlgorithm.HS512).compact();
    }
    
    protected Object getPrincipalForWebToken(final Object principal) {
        return this.cleanUserPrincipal(principal);
    }
    
    protected Object cleanUserPrincipal(final Object principal) {
        if (!(principal instanceof Map)) {
            return principal;
        }
        final Map cleanUser = new HashMap();
        final AtomicBoolean skipped = new AtomicBoolean(false);
        final AtomicBoolean atomicBoolean;
        final Map<Object, Object> map;
        ((Map)principal).forEach((k, v) -> {
            if (v instanceof List || v instanceof Map) {
                AbstractPortofinoRealm.logger.debug("Skipping {}", k);
                atomicBoolean.set(true);
            }
            else {
                map.put(k, v);
            }
            return;
        });
        if (skipped.get()) {
            AbstractPortofinoRealm.logger.debug("The user entity has potential self-references that make it unusable as a principal, because it must be serializable to JSON. Returning a non-persistent map with no references.");
            return cleanUser;
        }
        return principal;
    }
    
    @NotNull
    protected Key getJWTKey() {
        final String secret = this.portofinoConfiguration.getString("jwt.secret");
        return new SecretKeySpec((byte[])Decoders.BASE64.decode((Object)secret), SignatureAlgorithm.HS512.getJcaName());
    }
    
    public AuthorizationInfo doGetAuthorizationInfo(final PrincipalCollection principals) {
        final Object principal = principals.getPrimaryPrincipal();
        final Set<String> groups = this.getGroups(principal);
        final SimpleAuthorizationInfo info = new SimpleAuthorizationInfo((Set)groups);
        if (groups.contains(SecurityLogic.getAdministratorsGroup(this.portofinoConfiguration))) {
            info.addStringPermission("*");
        }
        final Permission permission = (Permission)new GroupPermission(groups);
        info.setObjectPermissions((Set)Collections.singleton(permission));
        return (AuthorizationInfo)info;
    }
    
    @NotNull
    public Set<String> getGroups(final Object principal) {
        final Set<String> groups = new HashSet<String>();
        groups.add(SecurityLogic.getAllGroup(this.portofinoConfiguration));
        if (principal == null) {
            groups.add(SecurityLogic.getAnonymousGroup(this.portofinoConfiguration));
        }
        else {
            if (!(principal instanceof Serializable)) {
                throw new AuthorizationException("Invalid principal: " + principal);
            }
            groups.add(SecurityLogic.getRegisteredGroup(this.portofinoConfiguration));
            groups.addAll(this.loadAuthorizationInfo((Serializable)principal));
        }
        return groups;
    }
    
    protected Collection<String> loadAuthorizationInfo(final Serializable principal) {
        return (Collection<String>)Collections.emptySet();
    }
    
    public Set<String> getGroups() {
        final Set<String> groups = new LinkedHashSet<String>();
        groups.add(SecurityLogic.getAllGroup(this.portofinoConfiguration));
        groups.add(SecurityLogic.getAnonymousGroup(this.portofinoConfiguration));
        groups.add(SecurityLogic.getRegisteredGroup(this.portofinoConfiguration));
        groups.add(SecurityLogic.getAdministratorsGroup(this.portofinoConfiguration));
        return groups;
    }
    
    public Serializable getUserById(final String encodedUserId) {
        throw new UnsupportedOperationException();
    }
    
    public Serializable getUserByEmail(final String email) {
        throw new UnsupportedOperationException();
    }
    
    public boolean supportsSelfRegistration() {
        return false;
    }
    
    public ClassAccessor getSelfRegisteredUserClassAccessor() {
        return (ClassAccessor)JavaClassAccessor.getClassAccessor((Class)User.class);
    }
    
    public String getUserPrettyName(final Serializable user) {
        return user.toString();
    }
    
    public void verifyUser(final Serializable user) {
        throw new UnsupportedOperationException();
    }
    
    public void changePassword(final Serializable user, final String oldPassword, final String newPassword) {
        throw new UnsupportedOperationException();
    }
    
    public String generateOneTimeToken(final Serializable user) {
        throw new UnsupportedOperationException();
    }
    
    public String[] saveSelfRegisteredUser(final Object user) {
        throw new UnsupportedOperationException();
    }
    
    protected void setup(final HashService hashService, final HashFormat hashFormat) {
        final PortofinoPasswordService passwordService = new PortofinoPasswordService();
        passwordService.setHashService(hashService);
        passwordService.setHashFormat(hashFormat);
        final PasswordMatcher passwordMatcher = new PasswordMatcher();
        passwordMatcher.setPasswordService((PasswordService)passwordService);
        this.setCredentialsMatcher((CredentialsMatcher)passwordMatcher);
        this.passwordService = (PasswordService)passwordService;
        this.legacyHashing = false;
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)AbstractPortofinoRealm.class);
    }
}
