// 
// Decompiled by Procyon v0.5.36
// 

package org.jboss.as.security.service;

import org.jboss.security.SecurityRolesAssociation;
import org.jboss.security.audit.AuditEvent;
import org.jboss.security.identity.plugins.SimpleIdentity;
import org.jboss.security.audit.AuditManager;
import org.jboss.security.AuthenticationManager;
import org.jboss.security.identity.Identity;
import org.jboss.security.SubjectInfo;
import org.jboss.remoting3.security.UserInfo;
import org.jboss.remoting3.Connection;
import org.jboss.security.SecurityContextUtil;
import org.jboss.security.RunAs;
import org.jboss.as.security.remoting.RemotingConnectionCredential;
import org.jboss.as.security.remoting.RemotingConnectionPrincipal;
import org.jboss.security.SimplePrincipal;
import org.jboss.as.domain.management.security.PasswordCredential;
import org.jboss.as.core.security.SubjectUserInfo;
import org.jboss.security.RunAsIdentity;
import org.jboss.security.identity.RoleGroup;
import org.jboss.security.identity.plugins.SimpleRoleGroup;
import java.lang.reflect.Method;
import java.security.CodeSource;
import org.jboss.security.javaee.AbstractEJBAuthorizationHelper;
import org.jboss.security.authorization.Resource;
import org.jboss.security.javaee.SecurityHelperFactory;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.security.javaee.SecurityRoleRef;
import java.util.HashSet;
import org.jboss.security.authorization.resources.EJBResource;
import java.util.HashMap;
import javax.security.jacc.PolicyContext;
import java.util.Collection;
import java.util.Map;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.security.acl.Group;
import javax.security.auth.Subject;
import java.security.AccessController;
import java.security.Principal;
import org.jboss.as.security.logging.SecurityLogger;
import org.jboss.security.SecurityContextFactory;
import org.jboss.security.SecurityContextAssociation;
import java.security.PrivilegedAction;
import org.jboss.security.ISecurityManagement;
import org.jboss.security.SecurityContext;
import org.jboss.as.core.security.ServerSecurityManager;

public class SimpleSecurityManager implements ServerSecurityManager
{
    private ThreadLocalStack<SecurityContext> contexts;
    private boolean propagate;
    private ISecurityManagement securityManagement;
    
    public SimpleSecurityManager() {
        this.contexts = new ThreadLocalStack<SecurityContext>();
        this.propagate = true;
        this.securityManagement = null;
    }
    
    public SimpleSecurityManager(final SimpleSecurityManager delegate) {
        this.contexts = new ThreadLocalStack<SecurityContext>();
        this.propagate = true;
        this.securityManagement = null;
        this.securityManagement = delegate.securityManagement;
        this.propagate = false;
    }
    
    private PrivilegedAction<SecurityContext> securityContext() {
        return new PrivilegedAction<SecurityContext>() {
            @Override
            public SecurityContext run() {
                return SecurityContextAssociation.getSecurityContext();
            }
        };
    }
    
    private SecurityContext establishSecurityContext(final String securityDomain) {
        try {
            final SecurityContext securityContext = SecurityContextFactory.createSecurityContext(securityDomain);
            if (this.securityManagement == null) {
                throw SecurityLogger.ROOT_LOGGER.securityManagementNotInjected();
            }
            securityContext.setSecurityManagement(this.securityManagement);
            SecurityContextAssociation.setSecurityContext(securityContext);
            return securityContext;
        }
        catch (Exception e) {
            throw SecurityLogger.ROOT_LOGGER.securityException(e);
        }
    }
    
    public void setSecurityManagement(final ISecurityManagement iSecurityManagement) {
        this.securityManagement = iSecurityManagement;
    }
    
    public Principal getCallerPrincipal() {
        final SecurityContext securityContext = AccessController.doPrivileged(this.securityContext());
        if (securityContext == null) {
            return this.getUnauthenticatedIdentity().asPrincipal();
        }
        Principal principal = (Principal)securityContext.getIncomingRunAs();
        if (principal == null) {
            principal = this.getPrincipal(this.getSubjectInfo(securityContext).getAuthenticatedSubject());
        }
        if (principal == null) {
            return this.getUnauthenticatedIdentity().asPrincipal();
        }
        return principal;
    }
    
    public Subject getSubject() {
        final SecurityContext securityContext = AccessController.doPrivileged(this.securityContext());
        if (securityContext != null) {
            return this.getSubjectInfo(securityContext).getAuthenticatedSubject();
        }
        return null;
    }
    
    private Principal getPrincipal(final Subject subject) {
        Principal principal = null;
        Principal callerPrincipal = null;
        if (subject != null) {
            final Set<Principal> principals = subject.getPrincipals();
            if (principals != null && !principals.isEmpty()) {
                for (final Principal p : principals) {
                    if (!(p instanceof Group) && principal == null) {
                        principal = p;
                    }
                    if (p instanceof Group) {
                        final Group g = Group.class.cast(p);
                        if (!g.getName().equals("CallerPrincipal") || callerPrincipal != null) {
                            continue;
                        }
                        final Enumeration<? extends Principal> e = g.members();
                        if (!e.hasMoreElements()) {
                            continue;
                        }
                        callerPrincipal = (Principal)e.nextElement();
                    }
                }
            }
        }
        return (callerPrincipal == null) ? principal : callerPrincipal;
    }
    
    public boolean isCallerInRole(final String ejbName, final Object mappedRoles, final Map<String, Collection<String>> roleLinks, final String... roleNames) {
        return this.isCallerInRole(ejbName, PolicyContext.getContextID(), mappedRoles, roleLinks, roleNames);
    }
    
    public boolean isCallerInRole(final String ejbName, final String policyContextID, final Object incommingMappedRoles, final Map<String, Collection<String>> roleLinks, final String... roleNames) {
        final SecurityContext securityContext = AccessController.doPrivileged(this.securityContext());
        if (securityContext == null) {
            return false;
        }
        final EJBResource resource = new EJBResource((Map)new HashMap());
        resource.setEjbName(ejbName);
        resource.setPolicyContextID(policyContextID);
        resource.setCallerRunAsIdentity(securityContext.getIncomingRunAs());
        resource.setCallerSubject(securityContext.getUtil().getSubject());
        final Principal userPrincipal = securityContext.getUtil().getUserPrincipal();
        resource.setPrincipal(userPrincipal);
        if (roleLinks != null) {
            final Set<SecurityRoleRef> roleRefs = new HashSet<SecurityRoleRef>();
            for (final String key : roleLinks.keySet()) {
                final Collection<String> values = roleLinks.get(key);
                if (values != null) {
                    for (final String value : values) {
                        roleRefs.add(new SecurityRoleRef(key, value));
                    }
                }
            }
            resource.setSecurityRoleReferences((Set)roleRefs);
        }
        Map<String, Set<String>> previousRolesAssociationMap = null;
        try {
            if (incommingMappedRoles != null) {
                final SecurityRolesMetaData rolesMetaData = (SecurityRolesMetaData)incommingMappedRoles;
                previousRolesAssociationMap = this.setSecurityRolesAssociation(rolesMetaData.getPrincipalVersusRolesMap());
            }
            final AbstractEJBAuthorizationHelper helper = SecurityHelperFactory.getEJBAuthorizationHelper(securityContext);
            for (final String roleName : roleNames) {
                if (helper.isCallerInRole((Resource)resource, roleName)) {
                    return true;
                }
            }
            return false;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            if (incommingMappedRoles != null) {
                this.setSecurityRolesAssociation(previousRolesAssociationMap);
            }
        }
    }
    
    public boolean authorize(final String ejbName, final CodeSource ejbCodeSource, final String ejbMethodIntf, final Method ejbMethod, final Set<Principal> methodRoles, final String contextID) {
        final SecurityContext securityContext = AccessController.doPrivileged(this.securityContext());
        if (securityContext == null) {
            return false;
        }
        final EJBResource resource = new EJBResource((Map)new HashMap());
        resource.setEjbName(ejbName);
        resource.setEjbMethod(ejbMethod);
        resource.setEjbMethodInterface(ejbMethodIntf);
        resource.setEjbMethodRoles((RoleGroup)new SimpleRoleGroup((Set)methodRoles));
        resource.setCodeSource(ejbCodeSource);
        resource.setPolicyContextID(contextID);
        resource.setCallerRunAsIdentity(securityContext.getIncomingRunAs());
        resource.setCallerSubject(securityContext.getUtil().getSubject());
        final Principal userPrincipal = securityContext.getUtil().getUserPrincipal();
        resource.setPrincipal(userPrincipal);
        try {
            final AbstractEJBAuthorizationHelper helper = SecurityHelperFactory.getEJBAuthorizationHelper(securityContext);
            return helper.authorize((Resource)resource);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public void push(final String securityDomain) {
        final SecurityContext previous = SecurityContextAssociation.getSecurityContext();
        this.contexts.push(previous);
        final SecurityContext current = this.establishSecurityContext(securityDomain);
        if (this.propagate && previous != null) {
            current.setSubjectInfo(this.getSubjectInfo(previous));
            current.setIncomingRunAs(previous.getOutgoingRunAs());
        }
        final RunAs currentRunAs = current.getIncomingRunAs();
        final boolean trusted = currentRunAs != null && currentRunAs instanceof RunAsIdentity;
        if (!trusted && SecurityActions.remotingContextIsSet()) {
            final SecurityContextUtil util = current.getUtil();
            final Connection connection = SecurityActions.remotingContextGetConnection();
            final UserInfo userInfo = connection.getUserInfo();
            Principal p = null;
            Object credential = null;
            if (userInfo instanceof SubjectUserInfo) {
                final SubjectUserInfo sinfo = (SubjectUserInfo)userInfo;
                final Subject subject = sinfo.getSubject();
                final Set<PasswordCredential> pcSet = subject.getPrivateCredentials(PasswordCredential.class);
                if (pcSet.size() > 0) {
                    final PasswordCredential pc = pcSet.iterator().next();
                    p = (Principal)new SimplePrincipal(pc.getUserName());
                    credential = new String(pc.getCredential());
                }
            }
            if (p == null || credential == null) {
                p = new RemotingConnectionPrincipal(connection);
                credential = new RemotingConnectionCredential(connection);
            }
            SecurityActions.remotingContextClear();
            util.createSubjectInfo(p, credential, (Subject)null);
        }
    }
    
    public void push(final String securityDomain, final String userName, final char[] password, final Subject subject) {
        final SecurityContext previous = SecurityContextAssociation.getSecurityContext();
        this.contexts.push(previous);
        final SecurityContext current = this.establishSecurityContext(securityDomain);
        if (this.propagate && previous != null) {
            current.setSubjectInfo(this.getSubjectInfo(previous));
            current.setIncomingRunAs(previous.getOutgoingRunAs());
        }
        final RunAs currentRunAs = current.getIncomingRunAs();
        final boolean trusted = currentRunAs != null && currentRunAs instanceof RunAsIdentity;
        if (!trusted) {
            final SecurityContextUtil util = current.getUtil();
            util.createSubjectInfo((Principal)new SimplePrincipal(userName), (Object)new String(password), subject);
        }
    }
    
    public void authenticate() {
        this.authenticate(null, null, null);
    }
    
    public void authenticate(final String runAs, final String runAsPrincipal, final Set<String> extraRoles) {
        final SecurityContext current = SecurityContextAssociation.getSecurityContext();
        final SecurityContext previous = this.contexts.peek();
        final boolean skipReauthentication = current.getSubjectInfo() != null && current.getSubjectInfo().getAuthenticatedSubject() != null && previous != null && current.getSecurityDomain().equals(previous.getSecurityDomain());
        if (!skipReauthentication) {
            final SecurityContextUtil util = current.getUtil();
            final Object credential = util.getCredential();
            Subject subject = null;
            if (credential instanceof RemotingConnectionCredential) {
                subject = ((RemotingConnectionCredential)credential).getSubject();
            }
            if (!this.authenticate(current, subject)) {
                throw SecurityLogger.ROOT_LOGGER.invalidUserException();
            }
        }
        if (runAs != null) {
            final RunAs runAsIdentity = (RunAs)new RunAsIdentity(runAs, runAsPrincipal, (Set)extraRoles);
            current.setOutgoingRunAs(runAsIdentity);
        }
        else if (this.propagate && previous != null && previous.getOutgoingRunAs() != null) {
            current.setOutgoingRunAs(previous.getOutgoingRunAs());
        }
    }
    
    private boolean authenticate(final SecurityContext context, Subject subject) {
        final SecurityContextUtil util = context.getUtil();
        final SubjectInfo subjectInfo = this.getSubjectInfo(context);
        if (subject == null) {
            subject = new Subject();
        }
        Principal auditPrincipal;
        final Principal principal = auditPrincipal = util.getUserPrincipal();
        final Object credential = util.getCredential();
        Identity unauthenticatedIdentity = null;
        boolean authenticated = false;
        if (principal == null) {
            unauthenticatedIdentity = this.getUnauthenticatedIdentity();
            subjectInfo.addIdentity(unauthenticatedIdentity);
            auditPrincipal = unauthenticatedIdentity.asPrincipal();
            subject.getPrincipals().add(auditPrincipal);
            authenticated = true;
        }
        else {
            subject.getPrincipals().add(principal);
        }
        if (!authenticated) {
            final AuthenticationManager authenticationManager = context.getAuthenticationManager();
            authenticated = authenticationManager.isValid(principal, credential, subject);
        }
        if (authenticated) {
            subjectInfo.setAuthenticatedSubject(subject);
        }
        final AuditManager auditManager = context.getAuditManager();
        if (auditManager != null) {
            this.audit(authenticated ? "Success" : "Failure", auditManager, auditPrincipal);
        }
        return authenticated;
    }
    
    private Identity getUnauthenticatedIdentity() {
        return (Identity)new SimpleIdentity("anonymous");
    }
    
    public void pop() {
        final SecurityContext sc = this.contexts.pop();
        SecurityContextAssociation.setSecurityContext(sc);
    }
    
    private void audit(final String level, final AuditManager auditManager, final Principal userPrincipal) {
        final AuditEvent auditEvent = new AuditEvent(level);
        final Map<String, Object> ctxMap = new HashMap<String, Object>();
        ctxMap.put("principal", (userPrincipal != null) ? userPrincipal.getName() : "null");
        ctxMap.put("Source", this.getClass().getCanonicalName());
        ctxMap.put("Action", "authentication");
        auditEvent.setContextMap((Map)ctxMap);
        auditManager.audit(auditEvent);
    }
    
    private SubjectInfo getSubjectInfo(final SecurityContext context) {
        if (System.getSecurityManager() == null) {
            return context.getSubjectInfo();
        }
        return AccessController.doPrivileged((PrivilegedAction<SubjectInfo>)new PrivilegedAction<SubjectInfo>() {
            @Override
            public SubjectInfo run() {
                return context.getSubjectInfo();
            }
        });
    }
    
    private Map<String, Set<String>> setSecurityRolesAssociation(final Map<String, Set<String>> mappedRoles) {
        if (System.getSecurityManager() == null) {
            final Map<String, Set<String>> previousMappedRoles = (Map<String, Set<String>>)SecurityRolesAssociation.getSecurityRoles();
            SecurityRolesAssociation.setSecurityRoles((Map)mappedRoles);
            return previousMappedRoles;
        }
        final SetSecurityRolesAssociationAction action = new SetSecurityRolesAssociationAction(mappedRoles);
        return AccessController.doPrivileged((PrivilegedAction<Map<String, Set<String>>>)action);
    }
    
    private static class SetSecurityRolesAssociationAction implements PrivilegedAction<Map<String, Set<String>>>
    {
        private final Map<String, Set<String>> mappedRoles;
        
        SetSecurityRolesAssociationAction(final Map<String, Set<String>> mappedRoles) {
            this.mappedRoles = mappedRoles;
        }
        
        @Override
        public Map<String, Set<String>> run() {
            final Map<String, Set<String>> previousMappedRoles = (Map<String, Set<String>>)SecurityRolesAssociation.getSecurityRoles();
            SecurityRolesAssociation.setSecurityRoles((Map)this.mappedRoles);
            return previousMappedRoles;
        }
    }
}
