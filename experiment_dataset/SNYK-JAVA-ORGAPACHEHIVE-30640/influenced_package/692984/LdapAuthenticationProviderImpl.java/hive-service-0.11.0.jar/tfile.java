// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.hive.service.auth;

import javax.naming.directory.DirContext;
import javax.naming.NamingException;
import javax.security.sasl.AuthenticationException;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import org.apache.hadoop.hive.conf.HiveConf;

public class LdapAuthenticationProviderImpl implements PasswdAuthenticationProvider
{
    String ldapURL;
    String baseDN;
    
    LdapAuthenticationProviderImpl() {
        final HiveConf conf = new HiveConf();
        this.ldapURL = conf.getVar(HiveConf.ConfVars.HIVE_SERVER2_PLAIN_LDAP_URL);
        this.baseDN = conf.getVar(HiveConf.ConfVars.HIVE_SERVER2_PLAIN_LDAP_BASEDN);
    }
    
    @Override
    public void Authenticate(final String user, final String password) throws AuthenticationException {
        final Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
        env.put("java.naming.provider.url", this.ldapURL);
        String bindDN;
        if (this.baseDN != null) {
            bindDN = "uid=" + user + "," + this.baseDN;
        }
        else {
            bindDN = user;
        }
        env.put("java.naming.security.authentication", "simple");
        env.put("java.naming.security.principal", bindDN);
        env.put("java.naming.security.credentials", password);
        try {
            final DirContext ctx = new InitialDirContext(env);
            ctx.close();
        }
        catch (NamingException e) {
            throw new AuthenticationException("Error validating LDAP user");
        }
    }
}
