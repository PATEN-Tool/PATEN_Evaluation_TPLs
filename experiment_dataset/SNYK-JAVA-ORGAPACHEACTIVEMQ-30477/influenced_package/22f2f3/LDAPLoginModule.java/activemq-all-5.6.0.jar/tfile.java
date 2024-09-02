// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.activemq.jaas;

import org.slf4j.LoggerFactory;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import javax.naming.directory.Attribute;
import javax.naming.AuthenticationException;
import javax.naming.directory.Attributes;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import java.util.List;
import javax.naming.CommunicationException;
import javax.naming.directory.SearchResult;
import java.util.ArrayList;
import javax.naming.directory.SearchControls;
import java.text.MessageFormat;
import javax.naming.NamingException;
import javax.security.auth.login.FailedLoginException;
import java.util.Iterator;
import java.security.Principal;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import javax.security.auth.login.LoginException;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.Callback;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.Subject;
import javax.naming.directory.DirContext;
import org.slf4j.Logger;
import javax.security.auth.spi.LoginModule;

public class LDAPLoginModule implements LoginModule
{
    private static final String INITIAL_CONTEXT_FACTORY = "initialContextFactory";
    private static final String CONNECTION_URL = "connectionURL";
    private static final String CONNECTION_USERNAME = "connectionUsername";
    private static final String CONNECTION_PASSWORD = "connectionPassword";
    private static final String CONNECTION_PROTOCOL = "connectionProtocol";
    private static final String AUTHENTICATION = "authentication";
    private static final String USER_BASE = "userBase";
    private static final String USER_SEARCH_MATCHING = "userSearchMatching";
    private static final String USER_SEARCH_SUBTREE = "userSearchSubtree";
    private static final String ROLE_BASE = "roleBase";
    private static final String ROLE_NAME = "roleName";
    private static final String ROLE_SEARCH_MATCHING = "roleSearchMatching";
    private static final String ROLE_SEARCH_SUBTREE = "roleSearchSubtree";
    private static final String USER_ROLE_NAME = "userRoleName";
    private static Logger log;
    protected DirContext context;
    private Subject subject;
    private CallbackHandler handler;
    private LDAPLoginProperty[] config;
    private String username;
    private Set<GroupPrincipal> groups;
    
    public LDAPLoginModule() {
        this.groups = new HashSet<GroupPrincipal>();
    }
    
    @Override
    public void initialize(final Subject subject, final CallbackHandler callbackHandler, final Map sharedState, final Map options) {
        this.subject = subject;
        this.handler = callbackHandler;
        this.config = new LDAPLoginProperty[] { new LDAPLoginProperty("initialContextFactory", options.get("initialContextFactory")), new LDAPLoginProperty("connectionURL", options.get("connectionURL")), new LDAPLoginProperty("connectionUsername", options.get("connectionUsername")), new LDAPLoginProperty("connectionPassword", options.get("connectionPassword")), new LDAPLoginProperty("connectionProtocol", options.get("connectionProtocol")), new LDAPLoginProperty("authentication", options.get("authentication")), new LDAPLoginProperty("userBase", options.get("userBase")), new LDAPLoginProperty("userSearchMatching", options.get("userSearchMatching")), new LDAPLoginProperty("userSearchSubtree", options.get("userSearchSubtree")), new LDAPLoginProperty("roleBase", options.get("roleBase")), new LDAPLoginProperty("roleName", options.get("roleName")), new LDAPLoginProperty("roleSearchMatching", options.get("roleSearchMatching")), new LDAPLoginProperty("roleSearchSubtree", options.get("roleSearchSubtree")), new LDAPLoginProperty("userRoleName", options.get("userRoleName")) };
    }
    
    @Override
    public boolean login() throws LoginException {
        final Callback[] callbacks = { new NameCallback("User name"), new PasswordCallback("Password", false) };
        try {
            this.handler.handle(callbacks);
        }
        catch (IOException ioe) {
            throw (LoginException)new LoginException().initCause(ioe);
        }
        catch (UnsupportedCallbackException uce) {
            throw (LoginException)new LoginException().initCause(uce);
        }
        this.username = ((NameCallback)callbacks[0]).getName();
        if (this.username == null) {
            return false;
        }
        String password;
        if (((PasswordCallback)callbacks[1]).getPassword() != null) {
            password = new String(((PasswordCallback)callbacks[1]).getPassword());
        }
        else {
            password = "";
        }
        this.authenticate(this.username, password);
        return true;
    }
    
    @Override
    public boolean logout() throws LoginException {
        this.username = null;
        return true;
    }
    
    @Override
    public boolean commit() throws LoginException {
        final Set<Principal> principals = this.subject.getPrincipals();
        principals.add(new UserPrincipal(this.username));
        for (final GroupPrincipal gp : this.groups) {
            principals.add(gp);
        }
        return true;
    }
    
    @Override
    public boolean abort() throws LoginException {
        this.username = null;
        return true;
    }
    
    protected void close(final DirContext context) {
        try {
            context.close();
        }
        catch (Exception e) {
            LDAPLoginModule.log.error(e.toString());
        }
    }
    
    protected boolean authenticate(final String username, final String password) throws LoginException {
        DirContext context = null;
        if (LDAPLoginModule.log.isDebugEnabled()) {
            LDAPLoginModule.log.debug("Create the LDAP initial context.");
        }
        try {
            context = this.open();
        }
        catch (NamingException ne) {
            final FailedLoginException ex = new FailedLoginException("Error opening LDAP connection");
            ex.initCause(ne);
            throw ex;
        }
        if (!this.isLoginPropertySet("userSearchMatching")) {
            return false;
        }
        final MessageFormat userSearchMatchingFormat = new MessageFormat(this.getLDAPPropertyValue("userSearchMatching"));
        final boolean userSearchSubtreeBool = Boolean.valueOf(this.getLDAPPropertyValue("userSearchSubtree"));
        try {
            final String filter = userSearchMatchingFormat.format(new String[] { username });
            final SearchControls constraints = new SearchControls();
            if (userSearchSubtreeBool) {
                constraints.setSearchScope(2);
            }
            else {
                constraints.setSearchScope(1);
            }
            final List<String> list = new ArrayList<String>();
            if (this.isLoginPropertySet("userRoleName")) {
                list.add(this.getLDAPPropertyValue("userRoleName"));
            }
            final String[] attribs = new String[list.size()];
            list.toArray(attribs);
            constraints.setReturningAttributes(attribs);
            if (LDAPLoginModule.log.isDebugEnabled()) {
                LDAPLoginModule.log.debug("Get the user DN.");
                LDAPLoginModule.log.debug("Looking for the user in LDAP with ");
                LDAPLoginModule.log.debug("  base DN: " + this.getLDAPPropertyValue("userBase"));
                LDAPLoginModule.log.debug("  filter: " + filter);
            }
            final NamingEnumeration<SearchResult> results = context.search(this.getLDAPPropertyValue("userBase"), filter, constraints);
            if (results == null || !results.hasMore()) {
                LDAPLoginModule.log.warn("User " + username + " not found in LDAP.");
                throw new FailedLoginException("User " + username + " not found in LDAP.");
            }
            final SearchResult result = results.next();
            if (results.hasMore()) {}
            final NameParser parser = context.getNameParser("");
            final Name contextName = parser.parse(context.getNameInNamespace());
            final Name baseName = parser.parse(this.getLDAPPropertyValue("userBase"));
            final Name entryName = parser.parse(result.getName());
            Name name = contextName.addAll(baseName);
            name = name.addAll(entryName);
            final String dn = name.toString();
            final Attributes attrs = result.getAttributes();
            if (attrs == null) {
                throw new FailedLoginException("User found, but LDAP entry malformed: " + username);
            }
            List<String> roles = null;
            if (this.isLoginPropertySet("userRoleName")) {
                roles = this.addAttributeValues(this.getLDAPPropertyValue("userRoleName"), attrs, roles);
            }
            if (!this.bindUser(context, dn, password)) {
                throw new FailedLoginException("Password does not match for user: " + username);
            }
            roles = this.getRoles(context, dn, username, roles);
            if (LDAPLoginModule.log.isDebugEnabled()) {
                LDAPLoginModule.log.debug("Roles " + roles + " for user " + username);
            }
            for (int i = 0; i < roles.size(); ++i) {
                this.groups.add(new GroupPrincipal(roles.get(i)));
            }
        }
        catch (CommunicationException e) {
            final FailedLoginException ex = new FailedLoginException("Error contacting LDAP");
            ex.initCause(e);
            throw ex;
        }
        catch (NamingException e2) {
            if (context != null) {
                this.close(context);
            }
            final FailedLoginException ex = new FailedLoginException("Error contacting LDAP");
            ex.initCause(e2);
            throw ex;
        }
        return true;
    }
    
    protected List<String> getRoles(final DirContext context, final String dn, final String username, final List<String> currentRoles) throws NamingException {
        List<String> list = currentRoles;
        final MessageFormat roleSearchMatchingFormat = new MessageFormat(this.getLDAPPropertyValue("roleSearchMatching"));
        final boolean roleSearchSubtreeBool = Boolean.valueOf(this.getLDAPPropertyValue("roleSearchSubtree"));
        if (list == null) {
            list = new ArrayList<String>();
        }
        if (!this.isLoginPropertySet("roleName")) {
            return list;
        }
        final String filter = roleSearchMatchingFormat.format(new String[] { this.doRFC2254Encoding(dn), username });
        final SearchControls constraints = new SearchControls();
        if (roleSearchSubtreeBool) {
            constraints.setSearchScope(2);
        }
        else {
            constraints.setSearchScope(1);
        }
        if (LDAPLoginModule.log.isDebugEnabled()) {
            LDAPLoginModule.log.debug("Get user roles.");
            LDAPLoginModule.log.debug("Looking for the user roles in LDAP with ");
            LDAPLoginModule.log.debug("  base DN: " + this.getLDAPPropertyValue("roleBase"));
            LDAPLoginModule.log.debug("  filter: " + filter);
        }
        final NamingEnumeration<SearchResult> results = context.search(this.getLDAPPropertyValue("roleBase"), filter, constraints);
        while (results.hasMore()) {
            final SearchResult result = results.next();
            final Attributes attrs = result.getAttributes();
            if (attrs == null) {
                continue;
            }
            list = this.addAttributeValues(this.getLDAPPropertyValue("roleName"), attrs, list);
        }
        return list;
    }
    
    protected String doRFC2254Encoding(final String inputString) {
        final StringBuffer buf = new StringBuffer(inputString.length());
        for (int i = 0; i < inputString.length(); ++i) {
            final char c = inputString.charAt(i);
            switch (c) {
                case '\\': {
                    buf.append("\\5c");
                    break;
                }
                case '*': {
                    buf.append("\\2a");
                    break;
                }
                case '(': {
                    buf.append("\\28");
                    break;
                }
                case ')': {
                    buf.append("\\29");
                    break;
                }
                case '\0': {
                    buf.append("\\00");
                    break;
                }
                default: {
                    buf.append(c);
                    break;
                }
            }
        }
        return buf.toString();
    }
    
    protected boolean bindUser(final DirContext context, final String dn, final String password) throws NamingException {
        boolean isValid = false;
        if (LDAPLoginModule.log.isDebugEnabled()) {
            LDAPLoginModule.log.debug("Binding the user.");
        }
        context.addToEnvironment("java.naming.security.principal", dn);
        context.addToEnvironment("java.naming.security.credentials", password);
        try {
            context.getAttributes("", null);
            isValid = true;
            if (LDAPLoginModule.log.isDebugEnabled()) {
                LDAPLoginModule.log.debug("User " + dn + " successfully bound.");
            }
        }
        catch (AuthenticationException e) {
            isValid = false;
            if (LDAPLoginModule.log.isDebugEnabled()) {
                LDAPLoginModule.log.debug("Authentication failed for dn=" + dn);
            }
        }
        if (this.isLoginPropertySet("connectionUsername")) {
            context.addToEnvironment("java.naming.security.principal", this.getLDAPPropertyValue("connectionUsername"));
        }
        else {
            context.removeFromEnvironment("java.naming.security.principal");
        }
        if (this.isLoginPropertySet("connectionPassword")) {
            context.addToEnvironment("java.naming.security.credentials", this.getLDAPPropertyValue("connectionPassword"));
        }
        else {
            context.removeFromEnvironment("java.naming.security.credentials");
        }
        return isValid;
    }
    
    private List<String> addAttributeValues(final String attrId, final Attributes attrs, List<String> values) throws NamingException {
        if (attrId == null || attrs == null) {
            return values;
        }
        if (values == null) {
            values = new ArrayList<String>();
        }
        final Attribute attr = attrs.get(attrId);
        if (attr == null) {
            return values;
        }
        final NamingEnumeration<?> e = attr.getAll();
        while (e.hasMore()) {
            final String value = (String)e.next();
            values.add(value);
        }
        return values;
    }
    
    protected DirContext open() throws NamingException {
        try {
            final Hashtable<String, String> env = new Hashtable<String, String>();
            env.put("java.naming.factory.initial", this.getLDAPPropertyValue("initialContextFactory"));
            if (this.isLoginPropertySet("connectionUsername")) {
                env.put("java.naming.security.principal", this.getLDAPPropertyValue("connectionUsername"));
            }
            if (this.isLoginPropertySet("connectionPassword")) {
                env.put("java.naming.security.credentials", this.getLDAPPropertyValue("connectionPassword"));
            }
            env.put("java.naming.security.protocol", this.getLDAPPropertyValue("connectionProtocol"));
            env.put("java.naming.provider.url", this.getLDAPPropertyValue("connectionURL"));
            env.put("java.naming.security.authentication", this.getLDAPPropertyValue("authentication"));
            this.context = new InitialDirContext(env);
        }
        catch (NamingException e) {
            LDAPLoginModule.log.error(e.toString());
            throw e;
        }
        return this.context;
    }
    
    private String getLDAPPropertyValue(final String propertyName) {
        for (int i = 0; i < this.config.length; ++i) {
            if (this.config[i].getPropertyName() == propertyName) {
                return this.config[i].getPropertyValue();
            }
        }
        return null;
    }
    
    private boolean isLoginPropertySet(final String propertyName) {
        for (int i = 0; i < this.config.length; ++i) {
            if (this.config[i].getPropertyName() == propertyName && this.config[i].getPropertyValue() != null) {
                return true;
            }
        }
        return false;
    }
    
    static {
        LDAPLoginModule.log = LoggerFactory.getLogger(LDAPLoginModule.class);
    }
}
