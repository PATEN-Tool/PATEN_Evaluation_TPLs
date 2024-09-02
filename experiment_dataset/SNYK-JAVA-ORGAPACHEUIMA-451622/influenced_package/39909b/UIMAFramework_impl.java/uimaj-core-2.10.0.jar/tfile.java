// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.uima.impl;

import org.xml.sax.SAXParseException;
import org.apache.uima.util.Level;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.ResourceFactory;
import org.apache.uima.internal.util.I18nUtil;
import java.util.Locale;
import org.xml.sax.Attributes;
import org.apache.uima.util.SimpleResourceFactory;
import org.xml.sax.helpers.DefaultHandler;
import java.io.IOException;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.XMLReader;
import javax.xml.parsers.SAXParser;
import org.xml.sax.InputSource;
import org.xml.sax.ErrorHandler;
import org.xml.sax.ContentHandler;
import javax.xml.parsers.SAXParserFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.collection.CollectionProcessingEngine;
import java.util.Map;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.util.UimaTimer;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.resource.ConfigurationManager;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.collection.CollectionProcessingManager;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.util.impl.XMLParser_impl;
import java.beans.Introspector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Properties;
import org.apache.uima.util.Logger;
import org.apache.uima.util.XMLParser;
import org.apache.uima.ResourceSpecifierFactory;
import org.apache.uima.CompositeResourceFactory;
import org.apache.uima.UIMAFramework;

public class UIMAFramework_impl extends UIMAFramework
{
    public static final boolean debug = false;
    private static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";
    private static final String LOGGER_CLASS_SYSTEM_PROPERTY = "org.apache.uima.logger.class";
    private static final Class<UIMAFramework_impl> CLASS_NAME;
    private CompositeResourceFactory mResourceFactory;
    private ResourceSpecifierFactory mResourceSpecifierFactory;
    private XMLParser mXMLParser;
    private Class mLoggerClass;
    private Logger mDefaultLogger;
    private String mCpmImplClassName;
    private String mResourceManagerImplClassName;
    private String mResourceManagerPearWrapperImplClassName;
    private String mConfigurationManagerImplClassName;
    private String mUimaContextImplClassName;
    private String mCpeClassName;
    private String mTimerClassName;
    private Properties mDefaultPerformanceTuningProperties;
    private ConcurrentHashMap<String, Logger> mLoggers;
    
    @Override
    protected void _initialize() throws Exception {
        Introspector.setBeanInfoSearchPath(new String[0]);
        this.mResourceFactory = new CompositeResourceFactory_impl();
        this.mXMLParser = new XMLParser_impl();
        this.mResourceSpecifierFactory = new ResourceSpecifierFactory_impl();
        this.parseFactoryConfig();
        (this.mDefaultPerformanceTuningProperties = new Properties()).load(UIMAFramework_impl.class.getResourceAsStream("performanceTuning.properties"));
        this.mLoggers = new ConcurrentHashMap<String, Logger>(200, 1.0f);
    }
    
    public short _getMajorVersion() {
        return UimaVersion.getMajorVersion();
    }
    
    public short _getMinorVersion() {
        return UimaVersion.getMinorVersion();
    }
    
    public short _getBuildRevision() {
        return UimaVersion.getBuildRevision();
    }
    
    @Override
    protected CompositeResourceFactory _getResourceFactory() {
        return this.mResourceFactory;
    }
    
    @Override
    protected ResourceSpecifierFactory _getResourceSpecifierFactory() {
        return this.mResourceSpecifierFactory;
    }
    
    @Override
    protected XMLParser _getXMLParser() {
        return this.mXMLParser;
    }
    
    @Override
    protected CollectionProcessingManager _newCollectionProcessingManager(final ResourceManager aResourceManager) {
        try {
            final Class cpmClass = Class.forName(this.mCpmImplClassName);
            final Constructor constructor = cpmClass.getConstructor(ResourceManager.class);
            return constructor.newInstance(aResourceManager);
        }
        catch (InvocationTargetException e) {
            throw new UIMARuntimeException(e.getTargetException());
        }
        catch (Exception e2) {
            throw new UIMARuntimeException(e2);
        }
    }
    
    @Override
    protected Logger _getLogger() {
        return this.mDefaultLogger;
    }
    
    @Override
    protected Logger _getLogger(final Class component) {
        Logger o = this.mLoggers.get(component.getName());
        if (o == null) {
            final Class[] argumentTypes = { Class.class };
            final Object[] arguments = { component };
            try {
                final Method instanceMethod = this.mLoggerClass.getMethod("getInstance", (Class[])argumentTypes);
                o = (Logger)instanceMethod.invoke(null, arguments);
            }
            catch (NoSuchMethodException e) {
                throw new UIMARuntimeException(e);
            }
            catch (InvocationTargetException e2) {
                throw new UIMARuntimeException(e2);
            }
            catch (IllegalAccessException e3) {
                throw new UIMARuntimeException(e3);
            }
            this.mLoggers.put(component.getName(), o);
        }
        return o;
    }
    
    @Override
    protected Logger _newLogger() {
        try {
            final Method instanceMethod = this.mLoggerClass.getMethod("getInstance", (Class[])new Class[0]);
            return (Logger)instanceMethod.invoke(null, (Object[])new Class[0]);
        }
        catch (NoSuchMethodException e) {
            throw new UIMARuntimeException(e);
        }
        catch (InvocationTargetException e2) {
            throw new UIMARuntimeException(e2);
        }
        catch (IllegalAccessException e3) {
            throw new UIMARuntimeException(e3);
        }
    }
    
    @Override
    protected ResourceManager _newDefaultResourceManager() {
        try {
            return (ResourceManager)Class.forName(this.mResourceManagerImplClassName).newInstance();
        }
        catch (InstantiationException e) {
            throw new UIMARuntimeException(e);
        }
        catch (IllegalAccessException e2) {
            throw new UIMARuntimeException(e2);
        }
        catch (ClassNotFoundException e3) {
            throw new UIMARuntimeException(e3);
        }
    }
    
    @Override
    protected ResourceManager _newDefaultResourceManagerPearWrapper() {
        try {
            return (ResourceManager)Class.forName(this.mResourceManagerPearWrapperImplClassName).newInstance();
        }
        catch (InstantiationException e) {
            throw new UIMARuntimeException(e);
        }
        catch (IllegalAccessException e2) {
            throw new UIMARuntimeException(e2);
        }
        catch (ClassNotFoundException e3) {
            throw new UIMARuntimeException(e3);
        }
    }
    
    @Override
    protected ConfigurationManager _newConfigurationManager() {
        try {
            return (ConfigurationManager)Class.forName(this.mConfigurationManagerImplClassName).newInstance();
        }
        catch (InstantiationException e) {
            throw new UIMARuntimeException(e);
        }
        catch (IllegalAccessException e2) {
            throw new UIMARuntimeException(e2);
        }
        catch (ClassNotFoundException e3) {
            throw new UIMARuntimeException(e3);
        }
    }
    
    @Override
    protected UimaContextAdmin _newUimaContext() {
        try {
            return (UimaContextAdmin)Class.forName(this.mUimaContextImplClassName).newInstance();
        }
        catch (InstantiationException e) {
            throw new UIMARuntimeException(e);
        }
        catch (IllegalAccessException e2) {
            throw new UIMARuntimeException(e2);
        }
        catch (ClassNotFoundException e3) {
            throw new UIMARuntimeException(e3);
        }
    }
    
    @Override
    protected UimaTimer _newTimer() {
        try {
            return (UimaTimer)Class.forName(this.mTimerClassName).newInstance();
        }
        catch (InstantiationException e) {
            throw new UIMARuntimeException(e);
        }
        catch (IllegalAccessException e2) {
            throw new UIMARuntimeException(e2);
        }
        catch (ClassNotFoundException e3) {
            throw new UIMARuntimeException(e3);
        }
    }
    
    @Override
    protected CollectionProcessingEngine _produceCollectionProcessingEngine(final CpeDescription aCpeDescription, final Map<String, Object> aAdditionalParams) throws ResourceInitializationException {
        try {
            final CollectionProcessingEngine cpe = (CollectionProcessingEngine)Class.forName(this.mCpeClassName).newInstance();
            cpe.initialize(aCpeDescription, aAdditionalParams);
            return cpe;
        }
        catch (InstantiationException e) {
            throw new UIMARuntimeException(e);
        }
        catch (IllegalAccessException e2) {
            throw new UIMARuntimeException(e2);
        }
        catch (ClassNotFoundException e3) {
            throw new UIMARuntimeException(e3);
        }
    }
    
    @Override
    protected Properties _getDefaultPerformanceTuningProperties() {
        return (Properties)this.mDefaultPerformanceTuningProperties.clone();
    }
    
    protected void parseFactoryConfig() throws ParserConfigurationException, SAXException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        final FactoryConfigParseHandler handler = new FactoryConfigParseHandler();
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        final SAXParser parser = factory.newSAXParser();
        final XMLReader reader = parser.getXMLReader();
        reader.setContentHandler(handler);
        reader.setErrorHandler(handler);
        reader.parse(new InputSource(UIMAFramework_impl.class.getResourceAsStream("factoryConfig.xml")));
    }
    
    static {
        CLASS_NAME = UIMAFramework_impl.class;
    }
    
    class FactoryConfigParseHandler extends DefaultHandler
    {
        static final int CONTEXT_NONE = 0;
        static final int CONTEXT_FACTORY_CONFIG = 1;
        static final int CONTEXT_RESOURCE_SPECIFIER = 2;
        static final int CONTEXT_RESOURCE = 3;
        static final int CONTEXT_SIMPLE_FACTORY = 4;
        private int context;
        private SimpleResourceFactory simpleFactory;
        private String simpleFactorySpecifierType;
        
        @Override
        public void startDocument() throws SAXException {
            this.context = 0;
        }
        
        @Override
        public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
            if ("logger".equals(qName)) {
                if (this.context != 1) {
                    throw new SAXException(I18nUtil.localizeMessage("org.apache.uima.UIMAException_Messages", Locale.getDefault(), "element_unexpected_in_context", new Object[] { "<logger>" }));
                }
                try {
                    String loggerClass = System.getProperty("org.apache.uima.logger.class");
                    if (loggerClass == null) {
                        loggerClass = attributes.getValue("class");
                    }
                    UIMAFramework_impl.this.mLoggerClass = Class.forName(loggerClass);
                    final Method instanceMethod = UIMAFramework_impl.this.mLoggerClass.getMethod("getInstance", (Class[])new Class[0]);
                    UIMAFramework_impl.this.mDefaultLogger = (Logger)instanceMethod.invoke(null, (Object[])new Class[0]);
                    return;
                }
                catch (Exception e) {
                    throw new SAXException(e);
                }
            }
            if ("cpm".equals(qName)) {
                if (this.context != 1) {
                    throw new SAXException(I18nUtil.localizeMessage("org.apache.uima.UIMAException_Messages", Locale.getDefault(), "element_unexpected_in_context", new Object[] { "<cpm>" }));
                }
                UIMAFramework_impl.this.mCpmImplClassName = attributes.getValue("class");
            }
            else if ("resourceManager".equals(qName)) {
                if (this.context != 1) {
                    throw new SAXException(I18nUtil.localizeMessage("org.apache.uima.UIMAException_Messages", Locale.getDefault(), "element_unexpected_in_context", new Object[] { "<resourceManager>" }));
                }
                UIMAFramework_impl.this.mResourceManagerImplClassName = attributes.getValue("class");
            }
            else if ("resourceManagerPearWrapper".equals(qName)) {
                if (this.context != 1) {
                    throw new SAXException(I18nUtil.localizeMessage("org.apache.uima.UIMAException_Messages", Locale.getDefault(), "element_unexpected_in_context", new Object[] { "<resourceManagerPearWrapper>" }));
                }
                UIMAFramework_impl.this.mResourceManagerPearWrapperImplClassName = attributes.getValue("class");
            }
            else if ("configurationManager".equals(qName)) {
                if (this.context != 1) {
                    throw new SAXException(I18nUtil.localizeMessage("org.apache.uima.UIMAException_Messages", Locale.getDefault(), "element_unexpected_in_context", new Object[] { "<configurationManager>" }));
                }
                UIMAFramework_impl.this.mConfigurationManagerImplClassName = attributes.getValue("class");
            }
            else if ("uimaContext".equals(qName)) {
                if (this.context != 1) {
                    throw new SAXException(I18nUtil.localizeMessage("org.apache.uima.UIMAException_Messages", Locale.getDefault(), "element_unexpected_in_context", new Object[] { "<uimaContext>" }));
                }
                UIMAFramework_impl.this.mUimaContextImplClassName = attributes.getValue("class");
            }
            else if ("cpe".equals(qName)) {
                if (this.context != 1) {
                    throw new SAXException(I18nUtil.localizeMessage("org.apache.uima.UIMAException_Messages", Locale.getDefault(), "element_unexpected_in_context", new Object[] { "<cpe>" }));
                }
                UIMAFramework_impl.this.mCpeClassName = attributes.getValue("class");
            }
            else if ("timer".equals(qName)) {
                if (this.context != 1) {
                    throw new SAXException(I18nUtil.localizeMessage("org.apache.uima.UIMAException_Messages", Locale.getDefault(), "element_unexpected_in_context", new Object[] { "<timer>" }));
                }
                UIMAFramework_impl.this.mTimerClassName = attributes.getValue("class");
            }
            else if ("resourceSpecifier".equals(qName)) {
                if (UIMAFramework_impl.this.mLoggerClass == null) {
                    throw new SAXException(I18nUtil.localizeMessage("org.apache.uima.UIMAException_Messages", Locale.getDefault(), "expected_x_but_found_y", new Object[] { "<logger>", "<resourceSpecifier>" }));
                }
                this.context = 2;
            }
            else if ("resource".equals(qName)) {
                if (UIMAFramework_impl.this.mLoggerClass == null) {
                    throw new SAXException(I18nUtil.localizeMessage("org.apache.uima.UIMAException_Messages", Locale.getDefault(), "expected_x_but_found_y", new Object[] { "<logger>", "<resource>" }));
                }
                this.context = 3;
            }
            else if ("simpleFactory".equals(qName)) {
                if (this.context != 3) {
                    throw new SAXException(I18nUtil.localizeMessage("org.apache.uima.UIMAException_Messages", Locale.getDefault(), "element_unexpected_in_context", new Object[] { "<simpleFactory>" }));
                }
                this.simpleFactory = new SimpleResourceFactory();
                this.simpleFactorySpecifierType = attributes.getValue("specifier");
                this.context = 4;
            }
            else if ("binding".equals(qName)) {
                if (this.context == 2) {
                    try {
                        UIMAFramework_impl.this.mXMLParser.addMapping(attributes.getValue("element"), attributes.getValue("class"));
                        UIMAFramework_impl.this.mResourceSpecifierFactory.addMapping(attributes.getValue("interface"), attributes.getValue("class"));
                    }
                    catch (ClassNotFoundException e3) {}
                }
                else {
                    if (this.context != 4) {
                        throw new SAXException(I18nUtil.localizeMessage("org.apache.uima.UIMAException_Messages", Locale.getDefault(), "element_unexpected_in_context", new Object[] { "<binding>" }));
                    }
                    try {
                        this.simpleFactory.addMapping(attributes.getValue("specifier"), attributes.getValue("resource"));
                    }
                    catch (ClassNotFoundException e3) {}
                }
            }
            else {
                if ("customFactory".equals(qName)) {
                    if (this.context != 3) {
                        throw new SAXException(I18nUtil.localizeMessage("org.apache.uima.UIMAException_Messages", Locale.getDefault(), "element_unexpected_in_context", new Object[] { "<customFactory>" }));
                    }
                    try {
                        final Class specifierClass = Class.forName(attributes.getValue("specifier"));
                        final Class factoryClass = Class.forName(attributes.getValue("factoryClass"));
                        final ResourceFactory factory = factoryClass.newInstance();
                        UIMAFramework_impl.this.mResourceFactory.registerFactory(specifierClass, factory);
                        return;
                    }
                    catch (RuntimeException e2) {
                        throw e2;
                    }
                    catch (Exception e) {
                        return;
                    }
                }
                if (!"factoryConfig".equals(qName)) {
                    throw new SAXException(I18nUtil.localizeMessage("org.apache.uima.UIMAException_Messages", Locale.getDefault(), "sax_unknown_element", new Object[] { qName }));
                }
                if (this.context != 0) {
                    throw new SAXException(I18nUtil.localizeMessage("org.apache.uima.UIMAException_Messages", Locale.getDefault(), "element_unexpected_in_context", new Object[] { "<factoryConfig>" }));
                }
                this.context = 1;
            }
        }
        
        @Override
        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
            if ("factoryConfig".equals(qName)) {
                this.context = 0;
            }
            else if ("resourceSpecifier".equals(qName) || "resource".equals(qName)) {
                this.context = 1;
            }
            else if ("simpleFactory".equals(qName)) {
                try {
                    final Class specifierClass = Class.forName(this.simpleFactorySpecifierType);
                    UIMAFramework_impl.this.mResourceFactory.registerFactory(specifierClass, this.simpleFactory);
                }
                catch (ClassNotFoundException e) {
                    UIMAFramework.getLogger(UIMAFramework_impl.CLASS_NAME).logrb(Level.INFO, UIMAFramework_impl.CLASS_NAME.getName(), "endElement", "org.apache.uima.impl.log_messages", "UIMA_class_in_framework_config_not_found__INFO", e.getLocalizedMessage());
                }
                this.context = 3;
            }
        }
        
        @Override
        public void warning(final SAXParseException e) throws SAXException {
            if (UIMAFramework_impl.this._getLogger() != null) {
                UIMAFramework.getLogger(UIMAFramework_impl.CLASS_NAME).logrb(Level.WARNING, UIMAFramework_impl.CLASS_NAME.getName(), "warning", "org.apache.uima.impl.log_messages", "UIMA_factory_config_parse__WARNING", e.getLocalizedMessage());
            }
        }
        
        @Override
        public void error(final SAXParseException e) throws SAXException {
            throw new UIMARuntimeException(e);
        }
    }
}
