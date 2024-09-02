// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.hive.service.auth;

import com.google.common.collect.ImmutableList;
import org.apache.hive.service.auth.ldap.ChainFilterFactory;
import org.apache.hive.service.auth.ldap.GroupFilterFactory;
import org.apache.hive.service.auth.ldap.UserFilterFactory;
import org.apache.hive.service.auth.ldap.UserSearchFilterFactory;
import org.apache.hive.service.auth.ldap.CustomQueryFilterFactory;
import org.slf4j.LoggerFactory;
import java.util.Iterator;
import org.apache.hive.service.auth.ldap.LdapUtils;
import org.apache.commons.lang.StringUtils;
import javax.security.sasl.AuthenticationException;
import org.apache.hive.service.auth.ldap.DirSearch;
import org.apache.hive.service.ServiceUtils;
import java.io.Closeable;
import com.google.common.annotations.VisibleForTesting;
import org.apache.hive.service.auth.ldap.LdapSearchFactory;
import org.apache.hive.service.auth.ldap.DirSearchFactory;
import org.apache.hive.service.auth.ldap.Filter;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hive.service.auth.ldap.FilterFactory;
import java.util.List;
import org.slf4j.Logger;

public class LdapAuthenticationProviderImpl implements PasswdAuthenticationProvider
{
    private static final Logger LOG;
    private static final List<FilterFactory> FILTER_FACTORIES;
    private final HiveConf conf;
    private final Filter filter;
    private final DirSearchFactory searchFactory;
    
    public LdapAuthenticationProviderImpl(final HiveConf conf) {
        this(conf, new LdapSearchFactory());
    }
    
    @VisibleForTesting
    LdapAuthenticationProviderImpl(final HiveConf conf, final DirSearchFactory searchFactory) {
        this.conf = conf;
        this.searchFactory = searchFactory;
        this.filter = resolveFilter(conf);
    }
    
    @Override
    public void Authenticate(final String user, final String password) throws AuthenticationException {
        DirSearch search = null;
        try {
            search = this.createDirSearch(user, password);
            this.applyFilter(search, user);
        }
        finally {
            ServiceUtils.cleanup(LdapAuthenticationProviderImpl.LOG, search);
        }
    }
    
    private DirSearch createDirSearch(final String user, final String password) throws AuthenticationException {
        if (StringUtils.isBlank(user)) {
            throw new AuthenticationException("Error validating LDAP user: a null or blank user name has been provided");
        }
        if (StringUtils.isBlank(password) || password.getBytes()[0] == 0) {
            throw new AuthenticationException("Error validating LDAP user: a null or blank password has been provided");
        }
        final List<String> principals = LdapUtils.createCandidatePrincipals(this.conf, user);
        final Iterator<String> iterator = principals.iterator();
        while (iterator.hasNext()) {
            final String principal = iterator.next();
            try {
                return this.searchFactory.getInstance(this.conf, principal, password);
            }
            catch (AuthenticationException ex) {
                if (!iterator.hasNext()) {
                    throw ex;
                }
                continue;
            }
            break;
        }
        throw new AuthenticationException(String.format("No candidate principals for %s was found.", user));
    }
    
    private static Filter resolveFilter(final HiveConf conf) {
        for (final FilterFactory filterProvider : LdapAuthenticationProviderImpl.FILTER_FACTORIES) {
            final Filter filter = filterProvider.getInstance(conf);
            if (filter != null) {
                return filter;
            }
        }
        return null;
    }
    
    private void applyFilter(final DirSearch client, final String user) throws AuthenticationException {
        if (this.filter != null) {
            if (LdapUtils.hasDomain(user)) {
                this.filter.apply(client, LdapUtils.extractUserName(user));
            }
            else {
                this.filter.apply(client, user);
            }
        }
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)LdapAuthenticationProviderImpl.class);
        FILTER_FACTORIES = (List)ImmutableList.of((Object)new CustomQueryFilterFactory(), (Object)new ChainFilterFactory(new FilterFactory[] { new UserSearchFilterFactory(), new UserFilterFactory(), new GroupFilterFactory() }));
    }
}
