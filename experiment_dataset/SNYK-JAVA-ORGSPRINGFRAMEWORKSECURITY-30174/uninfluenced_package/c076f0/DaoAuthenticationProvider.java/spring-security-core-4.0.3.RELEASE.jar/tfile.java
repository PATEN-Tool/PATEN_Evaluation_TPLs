// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.security.authentication.dao;

import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.encoding.PlaintextPasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.encoding.PasswordEncoder;

public class DaoAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider
{
    private static final String USER_NOT_FOUND_PASSWORD = "userNotFoundPassword";
    private PasswordEncoder passwordEncoder;
    private String userNotFoundEncodedPassword;
    private SaltSource saltSource;
    private UserDetailsService userDetailsService;
    
    public DaoAuthenticationProvider() {
        this.setPasswordEncoder(new PlaintextPasswordEncoder());
    }
    
    @Override
    protected void additionalAuthenticationChecks(final UserDetails userDetails, final UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        Object salt = null;
        if (this.saltSource != null) {
            salt = this.saltSource.getSalt(userDetails);
        }
        if (authentication.getCredentials() == null) {
            this.logger.debug((Object)"Authentication failed: no credentials provided");
            throw new BadCredentialsException(this.messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
        }
        final String presentedPassword = authentication.getCredentials().toString();
        if (!this.passwordEncoder.isPasswordValid(userDetails.getPassword(), presentedPassword, salt)) {
            this.logger.debug((Object)"Authentication failed: password does not match stored value");
            throw new BadCredentialsException(this.messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
        }
    }
    
    @Override
    protected void doAfterPropertiesSet() throws Exception {
        Assert.notNull((Object)this.userDetailsService, "A UserDetailsService must be set");
    }
    
    @Override
    protected final UserDetails retrieveUser(final String username, final UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        UserDetails loadedUser;
        try {
            loadedUser = this.getUserDetailsService().loadUserByUsername(username);
        }
        catch (UsernameNotFoundException notFound) {
            if (authentication.getCredentials() != null) {
                final String presentedPassword = authentication.getCredentials().toString();
                this.passwordEncoder.isPasswordValid(this.userNotFoundEncodedPassword, presentedPassword, null);
            }
            throw notFound;
        }
        catch (Exception repositoryProblem) {
            throw new InternalAuthenticationServiceException(repositoryProblem.getMessage(), repositoryProblem);
        }
        if (loadedUser == null) {
            throw new InternalAuthenticationServiceException("UserDetailsService returned null, which is an interface contract violation");
        }
        return loadedUser;
    }
    
    public void setPasswordEncoder(final Object passwordEncoder) {
        Assert.notNull(passwordEncoder, "passwordEncoder cannot be null");
        if (passwordEncoder instanceof PasswordEncoder) {
            this.setPasswordEncoder((PasswordEncoder)passwordEncoder);
            return;
        }
        if (passwordEncoder instanceof org.springframework.security.crypto.password.PasswordEncoder) {
            final org.springframework.security.crypto.password.PasswordEncoder delegate = (org.springframework.security.crypto.password.PasswordEncoder)passwordEncoder;
            this.setPasswordEncoder(new PasswordEncoder() {
                public String encodePassword(final String rawPass, final Object salt) {
                    this.checkSalt(salt);
                    return delegate.encode(rawPass);
                }
                
                public boolean isPasswordValid(final String encPass, final String rawPass, final Object salt) {
                    this.checkSalt(salt);
                    return delegate.matches(rawPass, encPass);
                }
                
                private void checkSalt(final Object salt) {
                    Assert.isNull(salt, "Salt value must be null when used with crypto module PasswordEncoder");
                }
            });
            return;
        }
        throw new IllegalArgumentException("passwordEncoder must be a PasswordEncoder instance");
    }
    
    private void setPasswordEncoder(final PasswordEncoder passwordEncoder) {
        Assert.notNull((Object)passwordEncoder, "passwordEncoder cannot be null");
        this.userNotFoundEncodedPassword = passwordEncoder.encodePassword("userNotFoundPassword", null);
        this.passwordEncoder = passwordEncoder;
    }
    
    protected PasswordEncoder getPasswordEncoder() {
        return this.passwordEncoder;
    }
    
    public void setSaltSource(final SaltSource saltSource) {
        this.saltSource = saltSource;
    }
    
    protected SaltSource getSaltSource() {
        return this.saltSource;
    }
    
    public void setUserDetailsService(final UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }
    
    protected UserDetailsService getUserDetailsService() {
        return this.userDetailsService;
    }
}
