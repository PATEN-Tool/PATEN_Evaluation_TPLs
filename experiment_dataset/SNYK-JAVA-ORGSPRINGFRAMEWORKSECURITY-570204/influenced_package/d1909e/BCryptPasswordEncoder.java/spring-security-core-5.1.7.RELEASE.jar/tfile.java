// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.security.crypto.bcrypt;

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
    private final SecureRandom random;
    
    public BCryptPasswordEncoder() {
        this(-1);
    }
    
    public BCryptPasswordEncoder(final int strength) {
        this(strength, null);
    }
    
    public BCryptPasswordEncoder(final int strength, final SecureRandom random) {
        this.BCRYPT_PATTERN = Pattern.compile("\\A\\$2a?\\$\\d\\d\\$[./0-9A-Za-z]{53}");
        this.logger = LogFactory.getLog((Class)this.getClass());
        if (strength != -1 && (strength < 4 || strength > 31)) {
            throw new IllegalArgumentException("Bad strength");
        }
        this.strength = strength;
        this.random = random;
    }
    
    @Override
    public String encode(final CharSequence rawPassword) {
        String salt;
        if (this.strength > 0) {
            if (this.random != null) {
                salt = BCrypt.gensalt(this.strength, this.random);
            }
            else {
                salt = BCrypt.gensalt(this.strength);
            }
        }
        else {
            salt = BCrypt.gensalt();
        }
        return BCrypt.hashpw(rawPassword.toString(), salt);
    }
    
    @Override
    public boolean matches(final CharSequence rawPassword, final String encodedPassword) {
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
}
