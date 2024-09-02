// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.nifi.bootstrap;

import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.nifi.bootstrap.notification.NotificationInitializationContext;
import org.apache.nifi.controller.ControllerServiceLookup;
import org.apache.nifi.attribute.expression.language.StandardPropertyValue;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.bootstrap.notification.NotificationContext;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Collection;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.bootstrap.notification.NotificationService;
import java.util.Iterator;
import org.w3c.dom.Document;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import org.apache.nifi.components.ValidationResult;
import java.util.ArrayList;
import org.apache.nifi.bootstrap.notification.NotificationValidationContext;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.HashMap;
import org.apache.nifi.registry.VariableRegistry;
import java.util.concurrent.ScheduledExecutorService;
import java.util.List;
import org.apache.nifi.bootstrap.notification.NotificationType;
import java.util.Map;
import org.slf4j.Logger;

public class NotificationServiceManager
{
    private static final Logger logger;
    private final Map<String, ConfiguredNotificationService> servicesById;
    private final Map<NotificationType, List<ConfiguredNotificationService>> servicesByNotificationType;
    private final ScheduledExecutorService notificationExecutor;
    private int maxAttempts;
    private final VariableRegistry variableRegistry;
    
    public NotificationServiceManager() {
        this(VariableRegistry.ENVIRONMENT_SYSTEM_REGISTRY);
    }
    
    NotificationServiceManager(final VariableRegistry variableRegistry) {
        this.servicesById = new HashMap<String, ConfiguredNotificationService>();
        this.servicesByNotificationType = new HashMap<NotificationType, List<ConfiguredNotificationService>>();
        this.maxAttempts = 5;
        this.variableRegistry = variableRegistry;
        this.notificationExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable r) {
                final Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setName("Notification Service Dispatcher");
                t.setDaemon(true);
                return t;
            }
        });
    }
    
    public void setMaxNotificationAttempts(final int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
    
    public void loadNotificationServices(final File servicesFile) throws IOException, ParserConfigurationException, SAXException {
        final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(false);
        final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        final Map<String, ConfiguredNotificationService> serviceMap = new HashMap<String, ConfiguredNotificationService>();
        try (final InputStream fis = new FileInputStream(servicesFile);
             final InputStream in = new BufferedInputStream(fis)) {
            final Document doc = docBuilder.parse(new InputSource(in));
            final List<Element> serviceElements = getChildElementsByTagName(doc.getDocumentElement(), "service");
            NotificationServiceManager.logger.debug("Found {} service elements", (Object)serviceElements.size());
            for (final Element serviceElement : serviceElements) {
                final ConfiguredNotificationService config = this.createService(serviceElement);
                final NotificationService service = config.getService();
                if (service == null) {
                    continue;
                }
                final String id = service.getIdentifier();
                if (serviceMap.containsKey(id)) {
                    NotificationServiceManager.logger.error("Found two different Notification Services configured with the same ID: '{}'. Loaded the first service.", (Object)id);
                }
                else {
                    final ValidationContext validationContext = (ValidationContext)new NotificationValidationContext(this.buildNotificationContext(config), this.variableRegistry);
                    final Collection<ValidationResult> validationResults = (Collection<ValidationResult>)service.validate(validationContext);
                    final List<String> invalidReasons = new ArrayList<String>();
                    for (final ValidationResult result : validationResults) {
                        if (!result.isValid()) {
                            invalidReasons.add(result.toString());
                        }
                    }
                    if (!invalidReasons.isEmpty()) {
                        NotificationServiceManager.logger.warn("Configured Notification Service {} is not valid for the following reasons: {}", (Object)service, (Object)invalidReasons);
                    }
                    serviceMap.put(id, config);
                }
            }
        }
        NotificationServiceManager.logger.info("Successfully loaded the following {} services: {}", (Object)serviceMap.size(), (Object)serviceMap.keySet());
        this.servicesById.clear();
        this.servicesById.putAll(serviceMap);
    }
    
    public void notify(final NotificationType type, final String subject, final String message) {
        final List<ConfiguredNotificationService> configs = this.servicesByNotificationType.get(type);
        if (configs == null || configs.isEmpty()) {
            return;
        }
        for (final ConfiguredNotificationService config : configs) {
            final NotificationService service = config.getService();
            final AtomicInteger attemptCount = new AtomicInteger(0);
            this.notificationExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    final ValidationContext validationContext = (ValidationContext)new NotificationValidationContext(NotificationServiceManager.this.buildNotificationContext(config), NotificationServiceManager.this.variableRegistry);
                    final Collection<ValidationResult> validationResults = (Collection<ValidationResult>)service.validate(validationContext);
                    final List<String> invalidReasons = new ArrayList<String>();
                    for (final ValidationResult result : validationResults) {
                        if (!result.isValid()) {
                            invalidReasons.add(result.toString());
                        }
                    }
                    boolean failure = false;
                    if (invalidReasons.isEmpty()) {
                        final NotificationContext context = NotificationServiceManager.this.buildNotificationContext(config);
                        try {
                            service.notify(context, subject, message);
                            NotificationServiceManager.logger.info("Successfully sent notification of type {} to {}", (Object)type, (Object)service);
                        }
                        catch (Throwable t) {
                            NotificationServiceManager.logger.error("Failed to send notification of type {} to {} with Subject {} due to {}. Will ", new Object[] { type, (service == null) ? "Unknown Notification Service" : service.toString(), subject, t.toString() });
                            NotificationServiceManager.logger.error("", t);
                            failure = true;
                        }
                    }
                    else {
                        NotificationServiceManager.logger.warn("Notification Service {} is not valid for the following reasons: {}", (Object)service, (Object)invalidReasons);
                        failure = true;
                    }
                    final int attempts = attemptCount.incrementAndGet();
                    if (failure) {
                        if (attempts < NotificationServiceManager.this.maxAttempts) {
                            NotificationServiceManager.logger.info("After failing to send notification to {} {} times, will attempt again in 1 minute", (Object)service, (Object)attempts);
                            NotificationServiceManager.this.notificationExecutor.schedule(this, 1L, TimeUnit.MINUTES);
                        }
                        else {
                            NotificationServiceManager.logger.info("After failing to send notification of type {} to {} {} times, will no longer attempt to send notification", new Object[] { type, service, attempts });
                        }
                    }
                }
            });
            if (NotificationType.NIFI_STOPPED.equals(type)) {
                while (attemptCount.get() == 0) {
                    try {
                        Thread.sleep(1000L);
                    }
                    catch (InterruptedException ex) {}
                }
            }
        }
    }
    
    private NotificationContext buildNotificationContext(final ConfiguredNotificationService config) {
        return new NotificationContext() {
            @Override
            public PropertyValue getProperty(final PropertyDescriptor descriptor) {
                final PropertyDescriptor fullPropDescriptor = config.getService().getPropertyDescriptor(descriptor.getName());
                if (fullPropDescriptor == null) {
                    return null;
                }
                String configuredValue = config.getProperties().get(fullPropDescriptor.getName());
                if (configuredValue == null) {
                    configuredValue = fullPropDescriptor.getDefaultValue();
                }
                return (PropertyValue)new StandardPropertyValue(configuredValue, (ControllerServiceLookup)null, NotificationServiceManager.this.variableRegistry);
            }
            
            @Override
            public Map<PropertyDescriptor, String> getProperties() {
                final Map<PropertyDescriptor, String> props = new HashMap<PropertyDescriptor, String>();
                final Map<String, String> configuredProps = config.getProperties();
                final NotificationService service = config.getService();
                for (final PropertyDescriptor descriptor : service.getPropertyDescriptors()) {
                    final String configuredValue = configuredProps.get(descriptor.getName());
                    if (configuredValue == null) {
                        props.put(descriptor, descriptor.getDefaultValue());
                    }
                    else {
                        props.put(descriptor, configuredValue);
                    }
                }
                return props;
            }
        };
    }
    
    public void registerNotificationService(final NotificationType type, final String serviceId) {
        final ConfiguredNotificationService service = this.servicesById.get(serviceId);
        if (service == null) {
            throw new IllegalArgumentException("No Notification Service exists with ID " + serviceId);
        }
        List<ConfiguredNotificationService> services = this.servicesByNotificationType.get(type);
        if (services == null) {
            services = new ArrayList<ConfiguredNotificationService>();
            this.servicesByNotificationType.put(type, services);
        }
        services.add(service);
    }
    
    private ConfiguredNotificationService createService(final Element serviceElement) {
        final Element idElement = getChild(serviceElement, "id");
        if (idElement == null) {
            NotificationServiceManager.logger.error("Found configuration for Notification Service with no 'id' element; this service cannot be referenced so it will not be loaded");
            return null;
        }
        final String serviceId = idElement.getTextContent().trim();
        NotificationServiceManager.logger.debug("Loading Notification Service with ID {}", (Object)serviceId);
        final Element classElement = getChild(serviceElement, "class");
        if (classElement == null) {
            NotificationServiceManager.logger.error("Found configuration for Notification Service with no 'class' element; Service ID is '{}'. This service annot be loaded", (Object)serviceId);
            return null;
        }
        final String className = classElement.getTextContent().trim();
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        }
        catch (Exception e) {
            NotificationServiceManager.logger.error("Found configuration for Notification Service with ID '{}' and Class '{}' but could not load class.", (Object)serviceId, (Object)className);
            NotificationServiceManager.logger.error("", (Throwable)e);
            return null;
        }
        if (!NotificationService.class.isAssignableFrom(clazz)) {
            NotificationServiceManager.logger.error("Found configuration for Notification Service with ID '{}' and Class '{}' but class is not a Notification Service.", (Object)serviceId, (Object)className);
            return null;
        }
        Object serviceObject;
        try {
            serviceObject = clazz.newInstance();
        }
        catch (Exception e2) {
            NotificationServiceManager.logger.error("Found configuration for Notification Service with ID '{}' and Class '{}' but could not instantiate Notification Service.", (Object)serviceId, (Object)className);
            NotificationServiceManager.logger.error("", (Throwable)e2);
            return null;
        }
        final Map<String, String> propertyValues = new HashMap<String, String>();
        final List<Element> propertyElements = getChildElementsByTagName(serviceElement, "property");
        for (final Element propertyElement : propertyElements) {
            final String propName = propertyElement.getAttribute("name");
            if (propName == null || propName.trim().isEmpty()) {
                NotificationServiceManager.logger.warn("Found configuration for Notification Service with ID '{}' that has property value configured but no name for the property.", (Object)serviceId);
            }
            else {
                final String propValue = propertyElement.getTextContent().trim();
                propertyValues.put(propName, propValue);
            }
        }
        final NotificationService service = (NotificationService)serviceObject;
        try {
            service.initialize(new NotificationInitializationContext() {
                @Override
                public PropertyValue getProperty(final PropertyDescriptor descriptor) {
                    final String propName = descriptor.getName();
                    String value = propertyValues.get(propName);
                    if (value == null) {
                        value = descriptor.getDefaultValue();
                    }
                    return (PropertyValue)new StandardPropertyValue(value, (ControllerServiceLookup)null, NotificationServiceManager.this.variableRegistry);
                }
                
                @Override
                public String getIdentifier() {
                    return serviceId;
                }
            });
        }
        catch (Exception e3) {
            NotificationServiceManager.logger.error("Failed to load Notification Service with ID '{}'", (Object)serviceId);
            NotificationServiceManager.logger.error("", (Throwable)e3);
        }
        return new ConfiguredNotificationService(service, propertyValues);
    }
    
    public static Element getChild(final Element element, final String tagName) {
        final List<Element> children = getChildElementsByTagName(element, tagName);
        if (children.isEmpty()) {
            return null;
        }
        if (children.size() > 1) {
            return null;
        }
        return children.get(0);
    }
    
    public static List<Element> getChildElementsByTagName(final Element element, final String tagName) {
        final List<Element> matches = new ArrayList<Element>();
        final NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); ++i) {
            final Node node = nodeList.item(i);
            if (node instanceof Element) {
                final Element child = (Element)nodeList.item(i);
                if (child.getNodeName().equals(tagName)) {
                    matches.add(child);
                }
            }
        }
        return matches;
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)NotificationServiceManager.class);
    }
    
    private static class ConfiguredNotificationService
    {
        private final NotificationService service;
        private final Map<String, String> properties;
        
        public ConfiguredNotificationService(final NotificationService service, final Map<String, String> properties) {
            this.service = service;
            this.properties = properties;
        }
        
        public NotificationService getService() {
            return this.service;
        }
        
        public Map<String, String> getProperties() {
            return this.properties;
        }
    }
}
