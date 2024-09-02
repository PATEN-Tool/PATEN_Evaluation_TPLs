// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.security.crypto.bcrypt;

import java.util.regex.Matcher;
import org.apache.commons.logging.LogFactory;
import java.security.SecureRandom;
import org.apache.commons.logging.Log;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;

public class BCryptPasswordEncoder implements PasswordEncoder
{
    private Pattern BCRYPT_PATTERN;
    private final Log logger;
    private final int strength;
    private final BCryptVersion version;
    private final SecureRandom random;
    
    public BCryptPasswordEncoder() {
        this(-1);
    }
    
    public BCryptPasswordEncoder(final int strength) {
        this(strength, null);
    }
    
    public BCryptPasswordEncoder(final BCryptVersion version) {
        this(version, null);
    }
    
    public BCryptPasswordEncoder(final BCryptVersion version, final SecureRandom random) {
        this(version, -1, random);
    }
    
    public BCryptPasswordEncoder(final int strength, final SecureRandom random) {
        this(BCryptVersion.$2A, strength, random);
    }
    
    public BCryptPasswordEncoder(final BCryptVersion version, final int strength) {
        this(version, strength, null);
    }
    
    public BCryptPasswordEncoder(final BCryptVersion version, final int strength, final SecureRandom random) {
        this.BCRYPT_PATTERN = Pattern.compile("\\A\\$2(a|y|b)?\\$(\\d\\d)\\$[./0-9A-Za-z]{53}");
        this.logger = LogFactory.getLog((Class)this.getClass());
        if (strength != -1 && (strength < 4 || strength > 31)) {
            throw new IllegalArgumentException("Bad strength");
        }
        this.version = version;
        this.strength = ((strength == -1) ? 10 : strength);
        this.random = random;
    }
    
    @Override
    public String encode(final CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }
        final String salt = this.getSalt();
        return BCrypt.hashpw(rawPassword.toString(), salt);
    }
    
    private String getSalt() {
        if (this.random != null) {
            return BCrypt.gensalt(this.version.getVersion(), this.strength, this.random);
        }
        return BCrypt.gensalt(this.version.getVersion(), this.strength);
    }
    
    @Override
    public boolean matches(final CharSequence rawPassword, final String encodedPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }
        if (encodedPassword == null || encodedPassword.length() == 0) {
            this.logger.warn((Object)"Empty encoded password");
            return false;
        }
        if (!this.BCRYPT_PATTERN.matcher(encodedPassword).matches()) {
            this.logger.warn((Object)"Encoded password does not look like BCrypt");
            return false;
        }
        return BCrypt.checkpw(rawPassword.toString(), encodedPassword);
    }
    
    @Override
    public boolean upgradeEncoding(final String encodedPassword) {
        if (encodedPassword == null || encodedPassword.length() == 0) {
            this.logger.warn((Object)"Empty encoded password");
            return false;
        }
        final Matcher matcher = this.BCRYPT_PATTERN.matcher(encodedPassword);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Encoded password does not look like BCrypt: " + encodedPassword);
        }
        final int strength = Integer.parseInt(matcher.group(2));
        return strength < this.strength;
    }
    
    public enum BCryptVersion
    {
        $2A("$2a"), 
        $2Y("$2y"), 
        $2B("$2b");
        
        private final String version;
        
        private BCryptVersion(final String version) {
            this.version = version;
        }
        
        public String getVersion() {
            return this.version;
        }
    }
}
