// 
// Decompiled by Procyon v0.5.36
// 

package flex.messaging.config;

import java.util.Locale;
import flex.messaging.util.LocaleUtils;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Document;

public abstract class ServerConfigurationParser extends AbstractConfigurationParser
{
    private boolean verifyAdvancedMessagingSupport;
    private boolean advancedMessagingSupportRegistered;
    
    public ServerConfigurationParser() {
        this.verifyAdvancedMessagingSupport = false;
        this.advancedMessagingSupportRegistered = false;
    }
    
    protected void parseTopLevelConfig(final Document doc) {
        final Node root = this.selectSingleNode((Node)doc, "/services-config");
        if (root == null) {
            final ConfigurationException e = new ConfigurationException();
            e.setMessage(10103, new Object[] { "services-config" });
            throw e;
        }
        this.allowedChildElements(root, ServerConfigurationParser.SERVICES_CONFIG_CHILDREN);
        this.securitySection(root);
        this.serversSection(root);
        this.channelsSection(root);
        this.services(root);
        this.clusters(root);
        this.logging(root);
        this.system(root);
        this.flexClient(root);
        this.factories(root);
        this.messageFilters(root);
        this.validators(root);
        if (this.verifyAdvancedMessagingSupport && !this.advancedMessagingSupportRegistered) {
            final ConfigurationException e = new ConfigurationException();
            e.setMessage(11129);
            throw e;
        }
    }
    
    private void clusters(final Node root) {
        final Node clusteringNode = this.selectSingleNode(root, "clusters");
        if (clusteringNode != null) {
            this.allowedAttributesOrElements(clusteringNode, ServerConfigurationParser.CLUSTERING_CHILDREN);
            final NodeList clusters = this.selectNodeList(clusteringNode, "cluster");
            for (int i = 0; i < clusters.getLength(); ++i) {
                final Node cluster = clusters.item(i);
                this.requiredAttributesOrElements(cluster, ServerConfigurationParser.CLUSTER_DEFINITION_CHILDREN);
                final String clusterName = this.getAttributeOrChildElement(cluster, "id");
                if (isValidID(clusterName)) {
                    final String propsFileName = this.getAttributeOrChildElement(cluster, "properties");
                    final ClusterSettings clusterSettings = new ClusterSettings();
                    clusterSettings.setClusterName(clusterName);
                    clusterSettings.setPropsFileName(propsFileName);
                    final String className = this.getAttributeOrChildElement(cluster, "class");
                    if (className != null && className.length() > 0) {
                        clusterSettings.setImplementationClass(className);
                    }
                    final String defaultValue = this.getAttributeOrChildElement(cluster, "default");
                    if (defaultValue != null && defaultValue.length() > 0) {
                        if (defaultValue.equalsIgnoreCase("true")) {
                            clusterSettings.setDefault(true);
                        }
                        else if (!defaultValue.equalsIgnoreCase("false")) {
                            final ConfigurationException e = new ConfigurationException();
                            e.setMessage(10215, new Object[] { clusterName, defaultValue });
                            throw e;
                        }
                    }
                    final String ulb = this.getAttributeOrChildElement(cluster, "url-load-balancing");
                    if (ulb != null && ulb.length() > 0) {
                        if (ulb.equalsIgnoreCase("false")) {
                            clusterSettings.setURLLoadBalancing(false);
                        }
                        else if (!ulb.equalsIgnoreCase("true")) {
                            final ConfigurationException e2 = new ConfigurationException();
                            e2.setMessage(10216, new Object[] { clusterName, ulb });
                            throw e2;
                        }
                    }
                    final NodeList properties = this.selectNodeList(cluster, "properties/*");
                    if (properties.getLength() > 0) {
                        final ConfigMap map = this.properties(properties, this.getSourceFileOf(cluster));
                        clusterSettings.addProperties(map);
                    }
                    ((MessagingConfiguration)this.config).addClusterSettings(clusterSettings);
                }
            }
        }
    }
    
    private void securitySection(final Node root) {
        final Node security = this.selectSingleNode(root, "security");
        if (security == null) {
            return;
        }
        this.allowedChildElements(security, ServerConfigurationParser.SECURITY_CHILDREN);
        NodeList list = this.selectNodeList(security, "security-constraint");
        for (int i = 0; i < list.getLength(); ++i) {
            final Node constraint = list.item(i);
            this.securityConstraint(constraint, false);
        }
        list = this.selectNodeList(security, "constraint-include");
        for (int i = 0; i < list.getLength(); ++i) {
            final Node include = list.item(i);
            this.securityConstraintInclude(include);
        }
        list = this.selectNodeList(security, "login-command");
        for (int i = 0; i < list.getLength(); ++i) {
            final Node login = list.item(i);
            final LoginCommandSettings loginCommandSettings = new LoginCommandSettings();
            this.requiredAttributesOrElements(login, ServerConfigurationParser.LOGIN_COMMAND_REQ_CHILDREN);
            this.allowedAttributesOrElements(login, ServerConfigurationParser.LOGIN_COMMAND_CHILDREN);
            final String server = this.getAttributeOrChildElement(login, "server");
            if (server.length() == 0) {
                final ConfigurationException e = new ConfigurationException();
                e.setMessage(10105, new Object[] { "server", "login-command" });
                throw e;
            }
            loginCommandSettings.setServer(server);
            final String loginClass = this.getAttributeOrChildElement(login, "class");
            if (loginClass.length() == 0) {
                final ConfigurationException e2 = new ConfigurationException();
                e2.setMessage(10105, new Object[] { "class", "login-command" });
                throw e2;
            }
            loginCommandSettings.setClassName(loginClass);
            final boolean isPerClientAuth = Boolean.valueOf(this.getAttributeOrChildElement(login, "per-client-authentication"));
            loginCommandSettings.setPerClientAuthentication(isPerClientAuth);
            ((MessagingConfiguration)this.config).getSecuritySettings().addLoginCommandSettings(loginCommandSettings);
        }
        final boolean recreateHttpSessionAfterLogin = Boolean.valueOf(this.getAttributeOrChildElement(security, "recreate-httpsession-after-login"));
        ((MessagingConfiguration)this.config).getSecuritySettings().setRecreateHttpSessionAfterLogin(recreateHttpSessionAfterLogin);
    }
    
    private SecurityConstraint securityConstraint(final Node constraint, final boolean inline) {
        this.allowedAttributesOrElements(constraint, ServerConfigurationParser.SECURITY_CONSTRAINT_DEFINITION_CHILDREN);
        final String ref = this.getAttributeOrChildElement(constraint, "ref");
        SecurityConstraint sc;
        if (ref.length() > 0) {
            this.allowedAttributesOrElements(constraint, new String[] { "ref" });
            sc = ((MessagingConfiguration)this.config).getSecuritySettings().getConstraint(ref);
            if (sc == null) {
                final ConfigurationException e = new ConfigurationException();
                e.setMessage(10109, new Object[] { "security-constraint", ref });
                throw e;
            }
        }
        else {
            final String id = this.getAttributeOrChildElement(constraint, "id");
            if (inline) {
                sc = new SecurityConstraint("");
            }
            else {
                if (!isValidID(id)) {
                    final ConfigurationException ex = new ConfigurationException();
                    ex.setMessage(10110, new Object[] { "security-constraint", id });
                    ex.setDetails(10110);
                    throw ex;
                }
                sc = new SecurityConstraint(id);
                ((MessagingConfiguration)this.config).getSecuritySettings().addConstraint(sc);
            }
            final String method = this.getAttributeOrChildElement(constraint, "auth-method");
            sc.setMethod(method);
            final Node rolesNode = this.selectSingleNode(constraint, "roles");
            if (rolesNode != null) {
                this.allowedChildElements(rolesNode, ServerConfigurationParser.ROLES_CHILDREN);
                final NodeList roles = this.selectNodeList(rolesNode, "role");
                for (int r = 0; r < roles.getLength(); ++r) {
                    final Node roleNode = roles.item(r);
                    final String role = this.evaluateExpression(roleNode, ".").toString().trim();
                    if (role.length() > 0) {
                        sc.addRole(role);
                    }
                }
            }
        }
        return sc;
    }
    
    private void securityConstraintInclude(final Node constraintInclude) {
        this.allowedAttributesOrElements(constraintInclude, ServerConfigurationParser.CONSTRAINT_INCLUDE_CHILDREN);
        final String src = this.getAttributeOrChildElement(constraintInclude, "file-path");
        final String dir = this.getAttributeOrChildElement(constraintInclude, "directory-path");
        if (src.length() > 0) {
            this.constraintIncludeFile(src);
        }
        else {
            if (dir.length() <= 0) {
                final ConfigurationException ex = new ConfigurationException();
                ex.setMessage(10118, new Object[] { constraintInclude.getNodeName(), "file-path", "directory-path" });
                throw ex;
            }
            this.constraintIncludeDirectory(dir);
        }
    }
    
    private void constraintIncludeFile(final String src) {
        final Document doc = this.loadDocument(src, this.fileResolver.getIncludedFile(src));
        doc.getDocumentElement().normalize();
        final Node servicesNode = this.selectSingleNode((Node)doc, "security-constraints");
        if (servicesNode != null) {
            this.allowedChildElements(servicesNode, ServerConfigurationParser.SECURITY_CONSTRAINTS_CHILDREN);
            final NodeList constraints = this.selectNodeList(servicesNode, "security-constraint");
            for (int a = 0; a < constraints.getLength(); ++a) {
                final Node constraint = constraints.item(a);
                this.securityConstraint(constraint, false);
            }
            this.fileResolver.popIncludedFile();
        }
        else {
            final Node constraint2 = this.selectSingleNode((Node)doc, "/security-constraint");
            if (constraint2 == null) {
                final ConfigurationException ex = new ConfigurationException();
                ex.setMessage(10112, new Object[] { "constraint-include", src, "security-constraints", "security-constraint" });
                throw ex;
            }
            this.securityConstraint(constraint2, false);
            this.fileResolver.popIncludedFile();
        }
    }
    
    private void constraintIncludeDirectory(final String dir) {
        final List files = this.fileResolver.getFiles(dir);
        for (int i = 0; i < files.size(); ++i) {
            final String src = files.get(i);
            this.constraintIncludeFile(src);
        }
    }
    
    private void serversSection(final Node root) {
        if (!(this.config instanceof MessagingConfiguration)) {
            return;
        }
        final Node serversNode = this.selectSingleNode(root, "servers");
        if (serversNode != null) {
            this.allowedAttributesOrElements(serversNode, ServerConfigurationParser.SERVERS_CHILDREN);
            final NodeList servers = this.selectNodeList(serversNode, "server");
            for (int i = 0; i < servers.getLength(); ++i) {
                final Node server = servers.item(i);
                this.serverDefinition(server);
            }
        }
    }
    
    private void serverDefinition(final Node server) {
        this.requiredAttributesOrElements(server, ServerConfigurationParser.SERVER_REQ_CHILDREN);
        this.allowedAttributesOrElements(server, ServerConfigurationParser.SERVER_CHILDREN);
        final String id = this.getAttributeOrChildElement(server, "id");
        if (isValidID(id)) {
            final SharedServerSettings settings = new SharedServerSettings();
            settings.setId(id);
            settings.setSourceFile(this.getSourceFileOf(server));
            final String className = this.getAttributeOrChildElement(server, "class");
            if (className.length() <= 0) {
                final ConfigurationException ex = new ConfigurationException();
                ex.setMessage(10114, new Object[] { "server", id });
                throw ex;
            }
            settings.setClassName(className);
            final NodeList properties = this.selectNodeList(server, "properties/*");
            if (properties.getLength() > 0) {
                final ConfigMap map = this.properties(properties, this.getSourceFileOf(server));
                settings.addProperties(map);
            }
            ((MessagingConfiguration)this.config).addSharedServerSettings(settings);
        }
    }
    
    private void channelsSection(final Node root) {
        final Node channelsNode = this.selectSingleNode(root, "channels");
        if (channelsNode != null) {
            this.allowedAttributesOrElements(channelsNode, ServerConfigurationParser.CHANNELS_CHILDREN);
            final NodeList channels = this.selectNodeList(channelsNode, "channel-definition");
            for (int i = 0; i < channels.getLength(); ++i) {
                final Node channel = channels.item(i);
                this.channelDefinition(channel);
            }
            final NodeList includes = this.selectNodeList(channelsNode, "channel-include");
            for (int j = 0; j < includes.getLength(); ++j) {
                final Node include = includes.item(j);
                this.channelInclude(include);
            }
        }
    }
    
    private void channelDefinition(final Node channel) {
        this.requiredAttributesOrElements(channel, ServerConfigurationParser.CHANNEL_DEFINITION_REQ_CHILDREN);
        this.allowedAttributesOrElements(channel, ServerConfigurationParser.CHANNEL_DEFINITION_CHILDREN);
        final String id = this.getAttributeOrChildElement(channel, "id");
        if (!isValidID(id)) {
            final ConfigurationException ex = new ConfigurationException();
            ex.setMessage(10110, new Object[] { "channel-definition", id });
            ex.setDetails(10110);
            throw ex;
        }
        if (this.config.getChannelSettings(id) != null) {
            final ConfigurationException e = new ConfigurationException();
            e.setMessage(11127, new Object[] { id });
            throw e;
        }
        final ChannelSettings channelSettings = new ChannelSettings(id);
        channelSettings.setSourceFile(this.getSourceFileOf(channel));
        final String remote = this.getAttributeOrChildElement(channel, "remote");
        channelSettings.setRemote((boolean)Boolean.valueOf(remote));
        final Node endpoint = this.selectSingleNode(channel, "endpoint");
        if (endpoint != null) {
            this.allowedAttributesOrElements(endpoint, ServerConfigurationParser.ENDPOINT_CHILDREN);
            final String type = this.getAttributeOrChildElement(endpoint, "class");
            channelSettings.setEndpointType(type);
            String uri = this.getAttributeOrChildElement(endpoint, "url");
            if (uri == null || "".equals(uri)) {
                uri = this.getAttributeOrChildElement(endpoint, "uri");
            }
            channelSettings.setUri(uri);
            this.config.addChannelSettings(id, channelSettings);
        }
        this.channelServerOnlyAttribute(channel, channelSettings);
        final Node server = this.selectSingleNode(channel, "server");
        if (server != null) {
            this.requiredAttributesOrElements(server, ServerConfigurationParser.CHANNEL_DEFINITION_SERVER_REQ_CHILDREN);
            final String serverId = this.getAttributeOrChildElement(server, "ref");
            channelSettings.setServerId(serverId);
        }
        final NodeList properties = this.selectNodeList(channel, "properties/*");
        if (properties.getLength() > 0) {
            final ConfigMap map = this.properties(properties, this.getSourceFileOf(channel));
            channelSettings.addProperties(map);
            if (!this.verifyAdvancedMessagingSupport) {
                final ConfigMap outboundQueueProcessor = map.getPropertyAsMap("flex-client-outbound-queue-processor", (ConfigMap)null);
                if (outboundQueueProcessor != null) {
                    final ConfigMap queueProcessorProperties = outboundQueueProcessor.getPropertyAsMap("properties", (ConfigMap)null);
                    if (queueProcessorProperties != null) {
                        final boolean adaptiveFrequency = queueProcessorProperties.getPropertyAsBoolean("adaptive-frequency", false);
                        if (adaptiveFrequency) {
                            this.verifyAdvancedMessagingSupport = true;
                        }
                    }
                }
            }
        }
        final String ref = this.evaluateExpression(channel, "@security-constraint").toString().trim();
        if (ref.length() > 0) {
            final SecurityConstraint sc = ((MessagingConfiguration)this.config).getSecuritySettings().getConstraint(ref);
            if (sc == null) {
                final ConfigurationException ex2 = new ConfigurationException();
                ex2.setMessage(10132, new Object[] { "security-constraint", ref, id });
                throw ex2;
            }
            channelSettings.setConstraint(sc);
        }
        else {
            final Node security = this.selectSingleNode(channel, "security");
            if (security != null) {
                this.allowedChildElements(security, ServerConfigurationParser.EMBEDDED_SECURITY_CHILDREN);
                final Node constraint = this.selectSingleNode(security, "security-constraint");
                if (constraint != null) {
                    final SecurityConstraint sc2 = this.securityConstraint(constraint, true);
                    channelSettings.setConstraint(sc2);
                }
            }
        }
    }
    
    private void channelServerOnlyAttribute(final Node channel, final ChannelSettings channelSettings) {
        String clientType = this.getAttributeOrChildElement(channel, "class");
        clientType = ((clientType.length() > 0) ? clientType : null);
        final String serverOnlyString = this.getAttributeOrChildElement(channel, "server-only");
        final boolean serverOnly = serverOnlyString.length() > 0 && Boolean.valueOf(serverOnlyString);
        if (clientType == null && !serverOnly) {
            final String url = channelSettings.getUri();
            final boolean serverOnlyProtocol = url.startsWith("samfsocket") || url.startsWith("amfsocket") || url.startsWith("ws");
            if (!serverOnlyProtocol) {
                final ConfigurationException ce = new ConfigurationException();
                ce.setMessage(11139, new Object[] { channelSettings.getId() });
                throw ce;
            }
            channelSettings.setServerOnly(true);
        }
        else {
            if (clientType != null && serverOnly) {
                final ConfigurationException ce2 = new ConfigurationException();
                ce2.setMessage(11140, new Object[] { channelSettings.getId() });
                throw ce2;
            }
            if (serverOnly) {
                channelSettings.setServerOnly(true);
            }
            else {
                channelSettings.setClientType(clientType);
            }
        }
    }
    
    private void channelInclude(final Node channelInclude) {
        this.allowedAttributesOrElements(channelInclude, ServerConfigurationParser.CHANNEL_INCLUDE_CHILDREN);
        final String src = this.getAttributeOrChildElement(channelInclude, "file-path");
        final String dir = this.getAttributeOrChildElement(channelInclude, "directory-path");
        if (src.length() > 0) {
            this.channelIncludeFile(src);
        }
        else {
            if (dir.length() <= 0) {
                final ConfigurationException ex = new ConfigurationException();
                ex.setMessage(10118, new Object[] { channelInclude.getNodeName(), "file-path", "directory-path" });
                throw ex;
            }
            this.channelIncludeDirectory(dir);
        }
    }
    
    private void channelIncludeFile(final String src) {
        final Document doc = this.loadDocument(src, this.fileResolver.getIncludedFile(src));
        doc.getDocumentElement().normalize();
        final Node channelsNode = this.selectSingleNode((Node)doc, "channels");
        if (channelsNode != null) {
            this.allowedChildElements(channelsNode, ServerConfigurationParser.CHANNELS_CHILDREN);
            final NodeList channels = this.selectNodeList(channelsNode, "channel-definition");
            for (int a = 0; a < channels.getLength(); ++a) {
                final Node service = channels.item(a);
                this.channelDefinition(service);
            }
            this.fileResolver.popIncludedFile();
        }
        else {
            final Node channel = this.selectSingleNode((Node)doc, "/channel-definition");
            if (channel == null) {
                final ConfigurationException ex = new ConfigurationException();
                ex.setMessage(10112, new Object[] { "channel-include", src, "channels", "channel-definition" });
                throw ex;
            }
            this.channelDefinition(channel);
            this.fileResolver.popIncludedFile();
        }
    }
    
    private void channelIncludeDirectory(final String dir) {
        final List files = this.fileResolver.getFiles(dir);
        for (int i = 0; i < files.size(); ++i) {
            final String src = files.get(i);
            this.channelIncludeFile(src);
        }
    }
    
    private void services(final Node root) {
        final Node servicesNode = this.selectSingleNode(root, "services");
        if (servicesNode != null) {
            this.allowedChildElements(servicesNode, ServerConfigurationParser.SERVICES_CHILDREN);
            final Node defaultChannels = this.selectSingleNode(servicesNode, "default-channels");
            if (defaultChannels != null) {
                this.allowedChildElements(defaultChannels, ServerConfigurationParser.DEFAULT_CHANNELS_CHILDREN);
                final NodeList channels = this.selectNodeList(defaultChannels, "channel");
                for (int c = 0; c < channels.getLength(); ++c) {
                    final Node chan = channels.item(c);
                    this.allowedAttributes(chan, new String[] { "ref" });
                    this.defaultChannel(chan);
                }
            }
            NodeList services = this.selectNodeList(servicesNode, "service-include");
            for (int i = 0; i < services.getLength(); ++i) {
                final Node service = services.item(i);
                this.serviceInclude(service);
            }
            services = this.selectNodeList(servicesNode, "service");
            for (int i = 0; i < services.getLength(); ++i) {
                final Node service = services.item(i);
                this.service(service);
            }
        }
    }
    
    private void serviceInclude(final Node serviceInclude) {
        this.allowedAttributesOrElements(serviceInclude, ServerConfigurationParser.SERVICE_INCLUDE_CHILDREN);
        final String src = this.getAttributeOrChildElement(serviceInclude, "file-path");
        final String dir = this.getAttributeOrChildElement(serviceInclude, "directory-path");
        if (src.length() > 0) {
            this.serviceIncludeFile(src);
        }
        else {
            if (dir.length() <= 0) {
                final ConfigurationException ex = new ConfigurationException();
                ex.setMessage(10118, new Object[] { serviceInclude.getNodeName(), "file-path", "directory-path" });
                throw ex;
            }
            this.serviceIncludeDirectory(dir);
        }
    }
    
    private void serviceIncludeFile(final String src) {
        final Document doc = this.loadDocument(src, this.fileResolver.getIncludedFile(src));
        doc.getDocumentElement().normalize();
        final Node servicesNode = this.selectSingleNode((Node)doc, "services");
        if (servicesNode != null) {
            this.allowedChildElements(servicesNode, ServerConfigurationParser.SERVICES_CHILDREN);
            final NodeList services = this.selectNodeList(servicesNode, "service");
            for (int a = 0; a < services.getLength(); ++a) {
                final Node service = services.item(a);
                this.service(service);
            }
            this.fileResolver.popIncludedFile();
        }
        else {
            final Node service2 = this.selectSingleNode((Node)doc, "/service");
            if (service2 == null) {
                final ConfigurationException ex = new ConfigurationException();
                ex.setMessage(10112, new Object[] { "service-include", src, "services", "service" });
                throw ex;
            }
            this.service(service2);
            this.fileResolver.popIncludedFile();
        }
    }
    
    private void serviceIncludeDirectory(final String dir) {
        final List files = this.fileResolver.getFiles(dir);
        for (int i = 0; i < files.size(); ++i) {
            final String src = files.get(i);
            this.serviceIncludeFile(src);
        }
    }
    
    private void service(final Node service) {
        this.requiredAttributesOrElements(service, ServerConfigurationParser.SERVICE_REQ_CHILDREN);
        this.allowedAttributesOrElements(service, ServerConfigurationParser.SERVICE_CHILDREN);
        final String id = this.getAttributeOrChildElement(service, "id");
        if (!isValidID(id)) {
            final ConfigurationException ex = new ConfigurationException();
            ex.setMessage(10110, new Object[] { "service", id });
            throw ex;
        }
        ServiceSettings serviceSettings = this.config.getServiceSettings(id);
        if (serviceSettings != null) {
            final ConfigurationException e = new ConfigurationException();
            e.setMessage(10113, new Object[] { id });
            throw e;
        }
        serviceSettings = new ServiceSettings(id);
        serviceSettings.setSourceFile(this.getSourceFileOf(service));
        this.config.addServiceSettings(serviceSettings);
        final String className = this.getAttributeOrChildElement(service, "class");
        if (className.length() > 0) {
            serviceSettings.setClassName(className);
            if (className.equals("flex.messaging.services.AdvancedMessagingSupport")) {
                this.advancedMessagingSupportRegistered = true;
            }
            final NodeList properties = this.selectNodeList(service, "properties/*");
            if (properties.getLength() > 0) {
                final ConfigMap map = this.properties(properties, this.getSourceFileOf(service));
                serviceSettings.addProperties(map);
            }
            final Node defaultChannels = this.selectSingleNode(service, "default-channels");
            if (defaultChannels != null) {
                this.allowedChildElements(defaultChannels, ServerConfigurationParser.DEFAULT_CHANNELS_CHILDREN);
                final NodeList channels = this.selectNodeList(defaultChannels, "channel");
                for (int c = 0; c < channels.getLength(); ++c) {
                    final Node chan = channels.item(c);
                    this.allowedAttributes(chan, new String[] { "ref" });
                    this.defaultChannel(chan, serviceSettings);
                }
            }
            else if (this.config.getDefaultChannels().size() > 0) {
                for (final String channelId : this.config.getDefaultChannels()) {
                    final ChannelSettings channel = this.config.getChannelSettings(channelId);
                    serviceSettings.addDefaultChannel(channel);
                }
            }
            final Node defaultSecurityConstraint = this.selectSingleNode(service, "default-security-constraint");
            if (defaultSecurityConstraint != null) {
                this.requiredAttributesOrElements(defaultSecurityConstraint, new String[] { "ref" });
                this.allowedAttributesOrElements(defaultSecurityConstraint, new String[] { "ref" });
                final String ref = this.getAttributeOrChildElement(defaultSecurityConstraint, "ref");
                if (ref.length() <= 0) {
                    final ConfigurationException ex2 = new ConfigurationException();
                    ex2.setMessage(11124, new Object[] { ref, id });
                    throw ex2;
                }
                final SecurityConstraint sc = ((MessagingConfiguration)this.config).getSecuritySettings().getConstraint(ref);
                if (sc == null) {
                    final ConfigurationException e2 = new ConfigurationException();
                    e2.setMessage(10109, new Object[] { "security-constraint", ref });
                    throw e2;
                }
                serviceSettings.setConstraint(sc);
            }
            final Node adapters = this.selectSingleNode(service, "adapters");
            if (adapters != null) {
                this.allowedChildElements(adapters, ServerConfigurationParser.ADAPTERS_CHILDREN);
                final NodeList serverAdapters = this.selectNodeList(adapters, "adapter-definition");
                for (int a = 0; a < serverAdapters.getLength(); ++a) {
                    final Node adapter = serverAdapters.item(a);
                    this.adapterDefinition(adapter, serviceSettings);
                }
                final NodeList adapterIncludes = this.selectNodeList(adapters, "adapter-include");
                for (int a2 = 0; a2 < adapterIncludes.getLength(); ++a2) {
                    final Node include = adapterIncludes.item(a2);
                    this.adapterInclude(include, serviceSettings);
                }
            }
            NodeList list = this.selectNodeList(service, "destination");
            for (int i = 0; i < list.getLength(); ++i) {
                final Node dest = list.item(i);
                this.destination(dest, serviceSettings);
            }
            list = this.selectNodeList(service, "destination-include");
            for (int i = 0; i < list.getLength(); ++i) {
                final Node dest = list.item(i);
                this.destinationInclude(dest, serviceSettings);
            }
            return;
        }
        final ConfigurationException ex3 = new ConfigurationException();
        ex3.setMessage(10114, new Object[] { "service", id });
        throw ex3;
    }
    
    private void defaultChannel(final Node chan) {
        final String ref = this.getAttributeOrChildElement(chan, "ref");
        if (ref.length() <= 0) {
            final ConfigurationException ex = new ConfigurationException();
            ex.setMessage(10116, new Object[] { "MessageBroker" });
            throw ex;
        }
        final ChannelSettings channel = this.config.getChannelSettings(ref);
        if (channel != null) {
            this.config.addDefaultChannel(channel.getId());
            return;
        }
        final ConfigurationException e = new ConfigurationException();
        e.setMessage(10109, new Object[] { "channel", ref });
        throw e;
    }
    
    private void defaultChannel(final Node chan, final ServiceSettings serviceSettings) {
        final String ref = this.getAttributeOrChildElement(chan, "ref");
        if (ref.length() <= 0) {
            final ConfigurationException ex = new ConfigurationException();
            ex.setMessage(10116, new Object[] { serviceSettings.getId() });
            throw ex;
        }
        final ChannelSettings channel = this.config.getChannelSettings(ref);
        if (channel != null) {
            serviceSettings.addDefaultChannel(channel);
            return;
        }
        final ConfigurationException e = new ConfigurationException();
        e.setMessage(10109, new Object[] { "channel", ref });
        throw e;
    }
    
    private void adapterDefinition(final Node adapter, final ServiceSettings serviceSettings) {
        this.requiredAttributesOrElements(adapter, ServerConfigurationParser.ADAPTER_DEFINITION_REQ_CHILDREN);
        this.allowedChildElements(adapter, ServerConfigurationParser.ADAPTER_DEFINITION_CHILDREN);
        final String serviceId = serviceSettings.getId();
        final String id = this.getAttributeOrChildElement(adapter, "id");
        if (!isValidID(id)) {
            final ConfigurationException ex = new ConfigurationException();
            ex.setMessage(10119, new Object[] { "adapter-definition", id, serviceId });
            throw ex;
        }
        final AdapterSettings adapterSettings = new AdapterSettings(id);
        adapterSettings.setSourceFile(this.getSourceFileOf(adapter));
        final String className = this.getAttributeOrChildElement(adapter, "class");
        if (className.length() > 0) {
            adapterSettings.setClassName(className);
            final boolean isDefault = Boolean.valueOf(this.getAttributeOrChildElement(adapter, "default"));
            if (isDefault) {
                adapterSettings.setDefault(isDefault);
                final AdapterSettings defaultAdapter = serviceSettings.getDefaultAdapter();
                if (defaultAdapter != null) {
                    final ConfigurationException ex2 = new ConfigurationException();
                    ex2.setMessage(10117, new Object[] { id, serviceId, defaultAdapter.getId() });
                    throw ex2;
                }
            }
            serviceSettings.addAdapterSettings(adapterSettings);
            final NodeList properties = this.selectNodeList(adapter, "properties/*");
            if (properties.getLength() > 0) {
                final ConfigMap map = this.properties(properties, this.getSourceFileOf(adapter));
                adapterSettings.addProperties(map);
            }
            return;
        }
        final ConfigurationException ex3 = new ConfigurationException();
        ex3.setMessage(10114, new Object[] { "adapter-definition", id });
        throw ex3;
    }
    
    private void adapterInclude(final Node adapterInclude, final ServiceSettings serviceSettings) {
        this.allowedAttributesOrElements(adapterInclude, ServerConfigurationParser.ADAPTER_INCLUDE_CHILDREN);
        final String src = this.getAttributeOrChildElement(adapterInclude, "file-path");
        final String dir = this.getAttributeOrChildElement(adapterInclude, "directory-path");
        if (src.length() > 0) {
            this.adapterIncludeFile(serviceSettings, src);
        }
        else {
            if (dir.length() <= 0) {
                final ConfigurationException ex = new ConfigurationException();
                ex.setMessage(10118, new Object[] { adapterInclude.getNodeName(), "file-path", "directory-path" });
                throw ex;
            }
            this.adapterIncludeDirectory(serviceSettings, dir);
        }
    }
    
    private void adapterIncludeDirectory(final ServiceSettings serviceSettings, final String dir) {
        final List files = this.fileResolver.getFiles(dir);
        for (int i = 0; i < files.size(); ++i) {
            final String src = files.get(i);
            this.adapterIncludeFile(serviceSettings, src);
        }
    }
    
    private void adapterIncludeFile(final ServiceSettings serviceSettings, final String src) {
        final Document doc = this.loadDocument(src, this.fileResolver.getIncludedFile(src));
        doc.getDocumentElement().normalize();
        final Node adaptersNode = this.selectSingleNode((Node)doc, "adapters");
        if (adaptersNode != null) {
            this.allowedChildElements(adaptersNode, ServerConfigurationParser.ADAPTERS_CHILDREN);
            final NodeList adapters = this.selectNodeList(adaptersNode, "adapter-definition");
            for (int a = 0; a < adapters.getLength(); ++a) {
                final Node adapter = adapters.item(a);
                this.adapterDefinition(adapter, serviceSettings);
            }
            this.fileResolver.popIncludedFile();
        }
        else {
            final Node adapter2 = this.selectSingleNode((Node)doc, "/adapter-definition");
            if (adapter2 == null) {
                final ConfigurationException ex = new ConfigurationException();
                ex.setMessage(10112, new Object[] { "adapter-include", src, "adapters", "adapter-definition" });
                throw ex;
            }
            this.adapterDefinition(adapter2, serviceSettings);
            this.fileResolver.popIncludedFile();
        }
    }
    
    private void destinationInclude(final Node destInclude, final ServiceSettings serviceSettings) {
        this.allowedAttributesOrElements(destInclude, ServerConfigurationParser.DESTINATION_INCLUDE_CHILDREN);
        final String src = this.getAttributeOrChildElement(destInclude, "file-path");
        final String dir = this.getAttributeOrChildElement(destInclude, "directory-path");
        if (src.length() > 0) {
            this.destinationIncludeFile(serviceSettings, src);
        }
        else {
            if (dir.length() <= 0) {
                final ConfigurationException ex = new ConfigurationException();
                ex.setMessage(10118, new Object[] { destInclude.getNodeName(), "file-path", "directory-path" });
                throw ex;
            }
            this.destinationIncludeDirectory(serviceSettings, dir);
        }
    }
    
    private void destinationIncludeDirectory(final ServiceSettings serviceSettings, final String dir) {
        final List files = this.fileResolver.getFiles(dir);
        for (int i = 0; i < files.size(); ++i) {
            final String src = files.get(i);
            this.destinationIncludeFile(serviceSettings, src);
        }
    }
    
    private void destinationIncludeFile(final ServiceSettings serviceSettings, final String src) {
        final Document doc = this.loadDocument(src, this.fileResolver.getIncludedFile(src));
        doc.getDocumentElement().normalize();
        final Node destinationsNode = this.selectSingleNode((Node)doc, "destinations");
        if (destinationsNode != null) {
            this.allowedChildElements(destinationsNode, ServerConfigurationParser.DESTINATIONS_CHILDREN);
            final NodeList destinations = this.selectNodeList(destinationsNode, "destination");
            for (int a = 0; a < destinations.getLength(); ++a) {
                final Node dest = destinations.item(a);
                this.destination(dest, serviceSettings);
            }
            this.fileResolver.popIncludedFile();
        }
        else {
            final Node dest2 = this.selectSingleNode((Node)doc, "/destination");
            if (dest2 == null) {
                final ConfigurationException ex = new ConfigurationException();
                ex.setMessage(10112, new Object[] { "destination-include", src, "destinations", "destination" });
                throw ex;
            }
            this.destination(dest2, serviceSettings);
            this.fileResolver.popIncludedFile();
        }
    }
    
    private void destination(final Node dest, final ServiceSettings serviceSettings) {
        this.requiredAttributesOrElements(dest, ServerConfigurationParser.DESTINATION_REQ_CHILDREN);
        this.allowedAttributes(dest, ServerConfigurationParser.DESTINATION_ATTR);
        this.allowedChildElements(dest, ServerConfigurationParser.DESTINATION_CHILDREN);
        final String serviceId = serviceSettings.getId();
        final String id = this.getAttributeOrChildElement(dest, "id");
        if (!isValidID(id)) {
            final ConfigurationException ex = new ConfigurationException();
            ex.setMessage(10119, new Object[] { "destination", id, serviceId });
            throw ex;
        }
        DestinationSettings destinationSettings = serviceSettings.getDestinationSettings().get(id);
        if (destinationSettings != null) {
            final ConfigurationException e = new ConfigurationException();
            e.setMessage(10122, new Object[] { id, serviceId });
            throw e;
        }
        destinationSettings = new DestinationSettings(id);
        destinationSettings.setSourceFile(this.getSourceFileOf(dest));
        serviceSettings.addDestinationSettings(destinationSettings);
        final NodeList properties = this.selectNodeList(dest, "properties/*");
        if (properties.getLength() > 0) {
            final ConfigMap map = this.properties(properties, this.getSourceFileOf(dest));
            destinationSettings.addProperties(map);
            if (!this.verifyAdvancedMessagingSupport) {
                final ConfigMap networkSettings = map.getPropertyAsMap("network", (ConfigMap)null);
                if (networkSettings != null) {
                    final String reliable = networkSettings.getPropertyAsString("reliable", (String)null);
                    if (reliable != null && Boolean.valueOf(reliable)) {
                        this.verifyAdvancedMessagingSupport = true;
                    }
                    else {
                        final ConfigMap inbound = networkSettings.getPropertyAsMap("throttle-inbound", (ConfigMap)null);
                        if (inbound != null) {
                            final String policy = inbound.getPropertyAsString("policy", (String)null);
                            if (policy != null && (ThrottleSettings.Policy.BUFFER.toString().equalsIgnoreCase(policy) || ThrottleSettings.Policy.CONFLATE.toString().equalsIgnoreCase(policy))) {
                                this.verifyAdvancedMessagingSupport = true;
                            }
                        }
                        if (!this.verifyAdvancedMessagingSupport) {
                            final ConfigMap outbound = networkSettings.getPropertyAsMap("throttle-outbound", (ConfigMap)null);
                            if (outbound != null) {
                                final String policy2 = outbound.getPropertyAsString("policy", (String)null);
                                if (policy2 != null && (ThrottleSettings.Policy.BUFFER.toString().equalsIgnoreCase(policy2) || ThrottleSettings.Policy.CONFLATE.toString().equalsIgnoreCase(policy2))) {
                                    this.verifyAdvancedMessagingSupport = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        this.destinationChannels(dest, destinationSettings, serviceSettings);
        this.destinationSecurity(dest, destinationSettings, serviceSettings);
        this.destinationAdapter(dest, destinationSettings, serviceSettings);
    }
    
    private void destinationChannels(final Node dest, final DestinationSettings destinationSettings, final ServiceSettings serviceSettings) {
        final String destId = destinationSettings.getId();
        final String channelsList = this.evaluateExpression(dest, "@channels").toString().trim();
        if (channelsList.length() > 0) {
            final StringTokenizer st = new StringTokenizer(channelsList, ",;:");
            while (st.hasMoreTokens()) {
                final String ref = st.nextToken().trim();
                final ChannelSettings channel = this.config.getChannelSettings(ref);
                if (channel == null) {
                    final ConfigurationException ex = new ConfigurationException();
                    ex.setMessage(10120, new Object[] { "channel", ref, destId });
                    throw ex;
                }
                destinationSettings.addChannelSettings(channel);
            }
        }
        else {
            final Node channelsNode = this.selectSingleNode(dest, "channels");
            if (channelsNode != null) {
                this.allowedChildElements(channelsNode, ServerConfigurationParser.DESTINATION_CHANNELS_CHILDREN);
                final NodeList channels = this.selectNodeList(channelsNode, "channel");
                for (int c = 0; c < channels.getLength(); ++c) {
                    final Node chan = channels.item(c);
                    this.requiredAttributesOrElements(chan, ServerConfigurationParser.DESTINATION_CHANNEL_REQ_CHILDREN);
                    final String ref2 = this.getAttributeOrChildElement(chan, "ref");
                    if (ref2.length() <= 0) {
                        final ConfigurationException ex2 = new ConfigurationException();
                        ex2.setMessage(10121, new Object[] { "channel", ref2, destId });
                        throw ex2;
                    }
                    final ChannelSettings channel2 = this.config.getChannelSettings(ref2);
                    if (channel2 == null) {
                        final ConfigurationException ex3 = new ConfigurationException();
                        ex3.setMessage(10120, new Object[] { "channel", ref2, destId });
                        throw ex3;
                    }
                    destinationSettings.addChannelSettings(channel2);
                }
            }
            else {
                final List defaultChannels = serviceSettings.getDefaultChannels();
                for (final ChannelSettings channel3 : defaultChannels) {
                    destinationSettings.addChannelSettings(channel3);
                }
            }
        }
        if (destinationSettings.getChannelSettings().size() <= 0) {
            final ConfigurationException ex4 = new ConfigurationException();
            ex4.setMessage(10123, new Object[] { destId });
            throw ex4;
        }
    }
    
    private void destinationSecurity(final Node dest, final DestinationSettings destinationSettings, final ServiceSettings serviceSettings) {
        final String destId = destinationSettings.getId();
        final String ref = this.evaluateExpression(dest, "@security-constraint").toString().trim();
        if (ref.length() > 0) {
            final SecurityConstraint sc = ((MessagingConfiguration)this.config).getSecuritySettings().getConstraint(ref);
            if (sc == null) {
                final ConfigurationException ex = new ConfigurationException();
                ex.setMessage(10120, new Object[] { "security-constraint", ref, destId });
                throw ex;
            }
            destinationSettings.setConstraint(sc);
        }
        else {
            final Node security = this.selectSingleNode(dest, "security");
            if (security != null) {
                this.allowedChildElements(security, ServerConfigurationParser.EMBEDDED_SECURITY_CHILDREN);
                final Node constraint = this.selectSingleNode(security, "security-constraint");
                if (constraint != null) {
                    final SecurityConstraint sc2 = this.securityConstraint(constraint, true);
                    destinationSettings.setConstraint(sc2);
                }
            }
            else {
                final SecurityConstraint sc3 = serviceSettings.getConstraint();
                if (sc3 != null) {
                    destinationSettings.setConstraint(sc3);
                }
            }
        }
    }
    
    private void destinationAdapter(final Node dest, final DestinationSettings destinationSettings, final ServiceSettings serviceSettings) {
        final String destId = destinationSettings.getId();
        String ref = this.evaluateExpression(dest, "@adapter").toString().trim();
        if (ref.length() > 0) {
            this.adapterReference(ref, destinationSettings, serviceSettings);
        }
        else {
            final Node adapter = this.selectSingleNode(dest, "adapter");
            if (adapter != null) {
                this.allowedAttributesOrElements(adapter, ServerConfigurationParser.DESTINATION_ADAPTER_CHILDREN);
                ref = this.getAttributeOrChildElement(adapter, "ref");
                this.adapterReference(ref, destinationSettings, serviceSettings);
            }
            else {
                final AdapterSettings adapterSettings = serviceSettings.getDefaultAdapter();
                if (adapterSettings != null) {
                    destinationSettings.setAdapterSettings(adapterSettings);
                }
            }
        }
        if (destinationSettings.getAdapterSettings() == null) {
            final ConfigurationException ex = new ConfigurationException();
            ex.setMessage(10127, new Object[] { destId });
            throw ex;
        }
    }
    
    private void adapterReference(final String ref, final DestinationSettings destinationSettings, final ServiceSettings serviceSettings) {
        final String destId = destinationSettings.getId();
        if (ref.length() <= 0) {
            final ConfigurationException ex = new ConfigurationException();
            ex.setMessage(10121, new Object[] { "adapter", ref, destId });
            throw ex;
        }
        final AdapterSettings adapterSettings = serviceSettings.getAdapterSettings(ref);
        if (adapterSettings != null) {
            destinationSettings.setAdapterSettings(adapterSettings);
            return;
        }
        final ConfigurationException ex2 = new ConfigurationException();
        ex2.setMessage(10120, new Object[] { "adapter", ref, destId });
        throw ex2;
    }
    
    private void logging(final Node root) {
        final Node logging = this.selectSingleNode(root, "logging");
        if (logging != null) {
            this.allowedAttributesOrElements(logging, ServerConfigurationParser.LOGGING_CHILDREN);
            final LoggingSettings settings = new LoggingSettings();
            NodeList properties = this.selectNodeList(logging, "properties/*");
            if (properties.getLength() > 0) {
                final ConfigMap map = this.properties(properties, this.getSourceFileOf(logging));
                settings.addProperties(map);
            }
            final NodeList targets = this.selectNodeList(logging, "target");
            for (int i = 0; i < targets.getLength(); ++i) {
                final Node targetNode = targets.item(i);
                this.requiredAttributesOrElements(targetNode, ServerConfigurationParser.TARGET_REQ_CHILDREN);
                this.allowedAttributesOrElements(targetNode, ServerConfigurationParser.TARGET_CHILDREN);
                final String className = this.getAttributeOrChildElement(targetNode, "class");
                if (className.length() > 0) {
                    final TargetSettings targetSettings = new TargetSettings(className);
                    final String targetLevel = this.getAttributeOrChildElement(targetNode, "level");
                    if (targetLevel.length() > 0) {
                        targetSettings.setLevel(targetLevel);
                    }
                    final Node filtersNode = this.selectSingleNode(targetNode, "filters");
                    if (filtersNode != null) {
                        this.allowedChildElements(filtersNode, ServerConfigurationParser.FILTERS_CHILDREN);
                        final NodeList filters = this.selectNodeList(filtersNode, "pattern");
                        for (int f = 0; f < filters.getLength(); ++f) {
                            final Node pattern = filters.item(f);
                            final String filter = this.evaluateExpression(pattern, ".").toString().trim();
                            targetSettings.addFilter(filter);
                        }
                    }
                    properties = this.selectNodeList(targetNode, "properties/*");
                    if (properties.getLength() > 0) {
                        final ConfigMap map2 = this.properties(properties, this.getSourceFileOf(targetNode));
                        targetSettings.addProperties(map2);
                    }
                    settings.addTarget(targetSettings);
                }
            }
            this.config.setLoggingSettings(settings);
        }
    }
    
    private void system(final Node root) {
        final Node system = this.selectSingleNode(root, "system");
        if (system == null) {
            ((MessagingConfiguration)this.config).setSystemSettings(new SystemSettings());
            return;
        }
        this.allowedAttributesOrElements(system, ServerConfigurationParser.SYSTEM_CHILDREN);
        final SystemSettings settings = new SystemSettings();
        settings.setEnforceEndpointValidation(this.getAttributeOrChildElement(system, "enforce-endpoint-validation"));
        this.locale(system, settings);
        settings.setManageable(this.getAttributeOrChildElement(system, "manageable"));
        settings.setDotNetFrameworkVersion(this.getAttributeOrChildElement(system, "dotnet-framework-version"));
        this.redeploy(system, settings);
        this.uuidGenerator(system, settings);
        ((MessagingConfiguration)this.config).setSystemSettings(settings);
    }
    
    private void redeploy(final Node system, final SystemSettings settings) {
        final Node redeployNode = this.selectSingleNode(system, "redeploy");
        if (redeployNode == null) {
            return;
        }
        this.allowedAttributesOrElements(redeployNode, ServerConfigurationParser.REDEPLOY_CHILDREN);
        final String enabled = this.getAttributeOrChildElement(redeployNode, "enabled");
        settings.setRedeployEnabled(enabled);
        final String interval = this.getAttributeOrChildElement(redeployNode, "watch-interval");
        if (interval.length() > 0) {
            settings.setWatchInterval(interval);
        }
        final NodeList watches = this.selectNodeList(redeployNode, "watch-file");
        for (int i = 0; i < watches.getLength(); ++i) {
            final Node watchNode = watches.item(i);
            final String watch = this.evaluateExpression(watchNode, ".").toString().trim();
            if (watch.length() > 0) {
                settings.addWatchFile(watch);
            }
        }
        final NodeList touches = this.selectNodeList(redeployNode, "touch-file");
        for (int j = 0; j < touches.getLength(); ++j) {
            final Node touchNode = touches.item(j);
            final String touch = this.evaluateExpression(touchNode, ".").toString().trim();
            if (touch.length() > 0) {
                settings.addTouchFile(touch);
            }
        }
    }
    
    private void locale(final Node system, final SystemSettings settings) {
        final Node localeNode = this.selectSingleNode(system, "locale");
        if (localeNode == null) {
            return;
        }
        this.allowedAttributesOrElements(localeNode, ServerConfigurationParser.LOCALE_CHILDREN);
        final String defaultLocaleString = this.getAttributeOrChildElement(localeNode, "default-locale");
        final Locale defaultLocale = (defaultLocaleString.length() > 0) ? LocaleUtils.buildLocale(defaultLocaleString) : LocaleUtils.buildLocale((String)null);
        settings.setDefaultLocale(defaultLocale);
    }
    
    private void uuidGenerator(final Node system, final SystemSettings settings) {
        final Node uuidGenerator = this.selectSingleNode(system, "uuid-generator");
        if (uuidGenerator == null) {
            return;
        }
        this.requiredAttributesOrElements(uuidGenerator, ServerConfigurationParser.UUID_GENERATOR_REQ_CHILDREN);
        final String className = this.getAttributeOrChildElement(uuidGenerator, "class");
        if (className.length() == 0) {
            final ConfigurationException ex = new ConfigurationException();
            ex.setMessage(10114, new Object[] { "uuid-generator", "" });
            throw ex;
        }
        settings.setUUIDGeneratorClassName(className);
    }
    
    private void flexClient(final Node root) {
        final Node flexClient = this.selectSingleNode(root, "flex-client");
        if (flexClient != null) {
            this.allowedChildElements(flexClient, ServerConfigurationParser.FLEX_CLIENT_CHILDREN);
            final FlexClientSettings flexClientSettings = new FlexClientSettings();
            final String timeout = this.getAttributeOrChildElement(flexClient, "timeout-minutes");
            Label_0135: {
                if (timeout.length() > 0) {
                    try {
                        final long timeoutMinutes = Long.parseLong(timeout);
                        if (timeoutMinutes < 0L) {
                            final ConfigurationException e = new ConfigurationException();
                            e.setMessage(11123, new Object[] { timeout });
                            throw e;
                        }
                        flexClientSettings.setTimeoutMinutes(timeoutMinutes);
                        break Label_0135;
                    }
                    catch (NumberFormatException nfe) {
                        final ConfigurationException e2 = new ConfigurationException();
                        e2.setMessage(11123, new Object[] { timeout });
                        throw e2;
                    }
                }
                flexClientSettings.setTimeoutMinutes(0L);
            }
            final Node outboundQueueProcessor = this.selectSingleNode(flexClient, "flex-client-outbound-queue-processor");
            if (outboundQueueProcessor != null) {
                this.requiredAttributesOrElements(outboundQueueProcessor, ServerConfigurationParser.FLEX_CLIENT_OUTBOUND_QUEUE_PROCESSOR_REQ_CHILDREN);
                final String outboundQueueProcessClass = this.getAttributeOrChildElement(outboundQueueProcessor, "class");
                if (outboundQueueProcessClass.length() <= 0) {
                    final ConfigurationException ex = new ConfigurationException();
                    ex.setMessage(10114, new Object[] { "flex-client-outbound-queue-processor", "" });
                    throw ex;
                }
                flexClientSettings.setFlexClientOutboundQueueProcessorClassName(outboundQueueProcessClass);
                final NodeList properties = this.selectNodeList(outboundQueueProcessor, "properties/*");
                if (properties.getLength() > 0) {
                    final ConfigMap map = this.properties(properties, this.getSourceFileOf(outboundQueueProcessor));
                    flexClientSettings.setFlexClientOutboundQueueProcessorProperties(map);
                    final boolean adaptiveFrequency = map.getPropertyAsBoolean("adaptive-frequency", false);
                    if (adaptiveFrequency) {
                        this.verifyAdvancedMessagingSupport = true;
                    }
                }
            }
            ((MessagingConfiguration)this.config).setFlexClientSettings(flexClientSettings);
        }
    }
    
    private void factories(final Node root) {
        final Node factories = this.selectSingleNode(root, "factories");
        if (factories != null) {
            this.allowedAttributesOrElements(factories, ServerConfigurationParser.FACTORIES_CHILDREN);
            final NodeList factoryList = this.selectNodeList(factories, "factory");
            for (int i = 0; i < factoryList.getLength(); ++i) {
                final Node factory = factoryList.item(i);
                this.factory(factory);
            }
        }
    }
    
    private void factory(final Node factory) {
        this.requiredAttributesOrElements(factory, ServerConfigurationParser.FACTORY_REQ_CHILDREN);
        final String id = this.getAttributeOrChildElement(factory, "id");
        final String className = this.getAttributeOrChildElement(factory, "class");
        if (isValidID(id)) {
            final FactorySettings factorySettings = new FactorySettings(id, className);
            final NodeList properties = this.selectNodeList(factory, "properties/*");
            if (properties.getLength() > 0) {
                final ConfigMap map = this.properties(properties, this.getSourceFileOf(factory));
                factorySettings.addProperties(map);
            }
            ((MessagingConfiguration)this.config).addFactorySettings(id, factorySettings);
            return;
        }
        final ConfigurationException ex = new ConfigurationException();
        ex.setMessage(10110, new Object[] { "factory", id });
        ex.setDetails(10110);
        throw ex;
    }
    
    private void messageFilters(final Node root) {
        this.typedMessageFilters(root, "async-message-filters", ServerConfigurationParser.ASYNC_MESSAGE_FILTERS_ELEMENT_CHILDREN);
        this.typedMessageFilters(root, "sync-message-filters", ServerConfigurationParser.SYNC_MESSAGE_FILTERS_ELEMENT_CHILDREN);
    }
    
    private void typedMessageFilters(final Node root, final String filterTypeElement, final String[] childrenElements) {
        final Node messageFiltersNode = this.selectSingleNode(root, filterTypeElement);
        if (messageFiltersNode == null) {
            return;
        }
        this.allowedChildElements(messageFiltersNode, childrenElements);
        final NodeList messageFilters = this.selectNodeList(messageFiltersNode, "filter");
        for (int i = 0; i < messageFilters.getLength(); ++i) {
            final Node messageFilter = messageFilters.item(i);
            this.messageFilter(messageFilter, filterTypeElement);
        }
    }
    
    private void messageFilter(final Node messageFilter, final String filterType) {
        this.requiredAttributesOrElements(messageFilter, ServerConfigurationParser.FILTER_REQ_CHILDREN);
        this.allowedAttributesOrElements(messageFilter, ServerConfigurationParser.FILTER_CHILDREN);
        final String id = this.getAttributeOrChildElement(messageFilter, "id");
        if (isValidID(id)) {
            final String className = this.getAttributeOrChildElement(messageFilter, "class");
            if (className.length() <= 0) {
                final ConfigurationException ex = new ConfigurationException();
                ex.setMessage(10114, new Object[] { "filter", id });
                throw ex;
            }
            final MessageFilterSettings messageFilterSettings = new MessageFilterSettings();
            messageFilterSettings.setId(id);
            messageFilterSettings.setClassName(className);
            final MessageFilterSettings.FilterType type = filterType.equals("async-message-filters") ? MessageFilterSettings.FilterType.ASYNC : MessageFilterSettings.FilterType.SYNC;
            messageFilterSettings.setFilterType(type);
            final NodeList properties = this.selectNodeList(messageFilter, "properties/*");
            if (properties.getLength() > 0) {
                final ConfigMap map = this.properties(properties, this.getSourceFileOf(messageFilter));
                messageFilterSettings.addProperties(map);
            }
            ((MessagingConfiguration)this.config).addMessageFilterSettings(messageFilterSettings);
        }
    }
    
    private void validators(final Node root) {
        final Node validatorsNode = this.selectSingleNode(root, "validators");
        if (validatorsNode == null) {
            return;
        }
        this.allowedChildElements(validatorsNode, ServerConfigurationParser.VALIDATORS_CHILDREN);
        final NodeList validators = this.selectNodeList(validatorsNode, "validator");
        for (int i = 0; i < validators.getLength(); ++i) {
            final Node validator = validators.item(i);
            this.validator(validator);
        }
    }
    
    private void validator(final Node validator) {
        this.requiredAttributesOrElements(validator, ServerConfigurationParser.VALIDATOR_REQ_CHILDREN);
        this.allowedAttributesOrElements(validator, ServerConfigurationParser.VALIDATOR_CHILDREN);
        final ValidatorSettings validatorSettings = new ValidatorSettings();
        final String className = this.getAttributeOrChildElement(validator, "class");
        if (className.length() > 0) {
            validatorSettings.setClassName(className);
            final String type = this.getAttributeOrChildElement(validator, "type");
            if (type.length() > 0) {
                validatorSettings.setType(type);
            }
            final NodeList properties = this.selectNodeList(validator, "properties/*");
            if (properties.getLength() > 0) {
                final ConfigMap map = this.properties(properties, this.getSourceFileOf(validator));
                validatorSettings.addProperties(map);
            }
            ((MessagingConfiguration)this.config).addValidatorSettings(validatorSettings);
            return;
        }
        final ConfigurationException ex = new ConfigurationException();
        ex.setMessage(10114, new Object[] { "validator", "" });
        throw ex;
    }
}
