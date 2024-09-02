// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.oxm.jaxb;

import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import org.springframework.util.FileCopyUtils;
import java.net.URISyntaxException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.activation.DataSource;
import java.io.Reader;
import java.io.StringReader;
import org.springframework.oxm.UncategorizedMappingException;
import org.springframework.oxm.UnmarshallingFailureException;
import javax.xml.bind.UnmarshalException;
import org.springframework.oxm.MarshallingFailureException;
import javax.xml.bind.MarshalException;
import org.springframework.oxm.ValidationFailureException;
import javax.xml.bind.ValidationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMSource;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import java.util.Iterator;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamWriter;
import org.springframework.util.xml.StaxUtils;
import javax.xml.bind.attachment.AttachmentMarshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.mime.MimeContainer;
import javax.xml.transform.Result;
import java.util.UUID;
import javax.activation.DataHandler;
import java.awt.Image;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URI;
import javax.xml.namespace.QName;
import java.util.Date;
import java.util.Calendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import org.springframework.util.ClassUtils;
import org.springframework.core.annotation.AnnotationUtils;
import javax.xml.bind.annotation.XmlRootElement;
import java.lang.reflect.GenericArrayType;
import org.springframework.core.JdkVersion;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import javax.xml.bind.JAXBElement;
import org.xml.sax.SAXException;
import java.io.IOException;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import javax.xml.validation.SchemaFactory;
import javax.xml.transform.sax.SAXSource;
import org.springframework.oxm.support.SaxResourceUtils;
import org.xml.sax.helpers.XMLReaderFactory;
import javax.xml.transform.Source;
import javax.xml.bind.JAXBException;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.Assert;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.EntityResolver;
import javax.xml.validation.Schema;
import javax.xml.bind.JAXBContext;
import org.w3c.dom.ls.LSResourceResolver;
import org.springframework.core.io.Resource;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Marshaller;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.oxm.GenericUnmarshaller;
import org.springframework.oxm.GenericMarshaller;
import org.springframework.oxm.mime.MimeUnmarshaller;
import org.springframework.oxm.mime.MimeMarshaller;

public class Jaxb2Marshaller implements MimeMarshaller, MimeUnmarshaller, GenericMarshaller, GenericUnmarshaller, BeanClassLoaderAware, InitializingBean
{
    private static final String CID = "cid:";
    protected final Log logger;
    private String contextPath;
    private Class<?>[] classesToBeBound;
    private String[] packagesToScan;
    private Map<String, ?> jaxbContextProperties;
    private Map<String, ?> marshallerProperties;
    private Map<String, ?> unmarshallerProperties;
    private javax.xml.bind.Marshaller.Listener marshallerListener;
    private javax.xml.bind.Unmarshaller.Listener unmarshallerListener;
    private ValidationEventHandler validationEventHandler;
    private XmlAdapter<?, ?>[] adapters;
    private Resource[] schemaResources;
    private String schemaLanguage;
    private LSResourceResolver schemaResourceResolver;
    private boolean lazyInit;
    private boolean mtomEnabled;
    private boolean supportJaxbElementClass;
    private boolean checkForXmlRootElement;
    private Class<?> mappedClass;
    private ClassLoader beanClassLoader;
    private final Object jaxbContextMonitor;
    private volatile JAXBContext jaxbContext;
    private Schema schema;
    private boolean processExternalEntities;
    private static final EntityResolver NO_OP_ENTITY_RESOLVER;
    
    public Jaxb2Marshaller() {
        this.logger = LogFactory.getLog((Class)this.getClass());
        this.schemaLanguage = "http://www.w3.org/2001/XMLSchema";
        this.lazyInit = false;
        this.mtomEnabled = false;
        this.supportJaxbElementClass = false;
        this.checkForXmlRootElement = true;
        this.jaxbContextMonitor = new Object();
        this.processExternalEntities = false;
    }
    
    public void setContextPaths(final String... contextPaths) {
        Assert.notEmpty((Object[])contextPaths, "'contextPaths' must not be empty");
        this.contextPath = StringUtils.arrayToDelimitedString((Object[])contextPaths, ":");
    }
    
    public void setContextPath(final String contextPath) {
        Assert.hasText(contextPath, "'contextPath' must not be null");
        this.contextPath = contextPath;
    }
    
    public String getContextPath() {
        return this.contextPath;
    }
    
    public void setClassesToBeBound(final Class<?>... classesToBeBound) {
        Assert.notEmpty((Object[])classesToBeBound, "'classesToBeBound' must not be empty");
        this.classesToBeBound = classesToBeBound;
    }
    
    public Class<?>[] getClassesToBeBound() {
        return this.classesToBeBound;
    }
    
    public void setPackagesToScan(final String... packagesToScan) {
        this.packagesToScan = packagesToScan;
    }
    
    public String[] getPackagesToScan() {
        return this.packagesToScan;
    }
    
    public void setJaxbContextProperties(final Map<String, ?> jaxbContextProperties) {
        this.jaxbContextProperties = jaxbContextProperties;
    }
    
    public void setMarshallerProperties(final Map<String, ?> properties) {
        this.marshallerProperties = properties;
    }
    
    public void setUnmarshallerProperties(final Map<String, ?> properties) {
        this.unmarshallerProperties = properties;
    }
    
    public void setMarshallerListener(final javax.xml.bind.Marshaller.Listener marshallerListener) {
        this.marshallerListener = marshallerListener;
    }
    
    public void setUnmarshallerListener(final javax.xml.bind.Unmarshaller.Listener unmarshallerListener) {
        this.unmarshallerListener = unmarshallerListener;
    }
    
    public void setValidationEventHandler(final ValidationEventHandler validationEventHandler) {
        this.validationEventHandler = validationEventHandler;
    }
    
    public void setAdapters(final XmlAdapter<?, ?>... adapters) {
        this.adapters = adapters;
    }
    
    public void setSchema(final Resource schemaResource) {
        this.schemaResources = new Resource[] { schemaResource };
    }
    
    public void setSchemas(final Resource... schemaResources) {
        this.schemaResources = schemaResources;
    }
    
    public void setSchemaLanguage(final String schemaLanguage) {
        this.schemaLanguage = schemaLanguage;
    }
    
    public void setSchemaResourceResolver(final LSResourceResolver schemaResourceResolver) {
        this.schemaResourceResolver = schemaResourceResolver;
    }
    
    public void setLazyInit(final boolean lazyInit) {
        this.lazyInit = lazyInit;
    }
    
    public void setMtomEnabled(final boolean mtomEnabled) {
        this.mtomEnabled = mtomEnabled;
    }
    
    public void setSupportJaxbElementClass(final boolean supportJaxbElementClass) {
        this.supportJaxbElementClass = supportJaxbElementClass;
    }
    
    public void setCheckForXmlRootElement(final boolean checkForXmlRootElement) {
        this.checkForXmlRootElement = checkForXmlRootElement;
    }
    
    public void setMappedClass(final Class<?> mappedClass) {
        this.mappedClass = mappedClass;
    }
    
    public void setProcessExternalEntities(final boolean processExternalEntities) {
        this.processExternalEntities = processExternalEntities;
    }
    
    public boolean isProcessExternalEntities() {
        return this.processExternalEntities;
    }
    
    public void setBeanClassLoader(final ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }
    
    public void afterPropertiesSet() throws Exception {
        final boolean hasContextPath = StringUtils.hasLength(this.contextPath);
        final boolean hasClassesToBeBound = !ObjectUtils.isEmpty((Object[])this.classesToBeBound);
        final boolean hasPackagesToScan = !ObjectUtils.isEmpty((Object[])this.packagesToScan);
        if ((hasContextPath && (hasClassesToBeBound || hasPackagesToScan)) || (hasClassesToBeBound && hasPackagesToScan)) {
            throw new IllegalArgumentException("Specify either 'contextPath', 'classesToBeBound', or 'packagesToScan'");
        }
        if (!hasContextPath && !hasClassesToBeBound && !hasPackagesToScan) {
            throw new IllegalArgumentException("Setting either 'contextPath', 'classesToBeBound', or 'packagesToScan' is required");
        }
        if (!this.lazyInit) {
            this.getJaxbContext();
        }
        if (!ObjectUtils.isEmpty((Object[])this.schemaResources)) {
            this.schema = this.loadSchema(this.schemaResources, this.schemaLanguage);
        }
    }
    
    public JAXBContext getJaxbContext() {
        if (this.jaxbContext != null) {
            return this.jaxbContext;
        }
        synchronized (this.jaxbContextMonitor) {
            if (this.jaxbContext == null) {
                try {
                    if (StringUtils.hasLength(this.contextPath)) {
                        this.jaxbContext = this.createJaxbContextFromContextPath();
                    }
                    else if (!ObjectUtils.isEmpty((Object[])this.classesToBeBound)) {
                        this.jaxbContext = this.createJaxbContextFromClasses();
                    }
                    else if (!ObjectUtils.isEmpty((Object[])this.packagesToScan)) {
                        this.jaxbContext = this.createJaxbContextFromPackages();
                    }
                }
                catch (JAXBException ex) {
                    throw this.convertJaxbException(ex);
                }
            }
            return this.jaxbContext;
        }
    }
    
    private JAXBContext createJaxbContextFromContextPath() throws JAXBException {
        if (this.logger.isInfoEnabled()) {
            this.logger.info((Object)("Creating JAXBContext with context path [" + this.contextPath + "]"));
        }
        if (this.jaxbContextProperties != null) {
            if (this.beanClassLoader != null) {
                return JAXBContext.newInstance(this.contextPath, this.beanClassLoader, this.jaxbContextProperties);
            }
            return JAXBContext.newInstance(this.contextPath, Thread.currentThread().getContextClassLoader(), this.jaxbContextProperties);
        }
        else {
            if (this.beanClassLoader != null) {
                return JAXBContext.newInstance(this.contextPath, this.beanClassLoader);
            }
            return JAXBContext.newInstance(this.contextPath);
        }
    }
    
    private JAXBContext createJaxbContextFromClasses() throws JAXBException {
        if (this.logger.isInfoEnabled()) {
            this.logger.info((Object)("Creating JAXBContext with classes to be bound [" + StringUtils.arrayToCommaDelimitedString((Object[])this.classesToBeBound) + "]"));
        }
        if (this.jaxbContextProperties != null) {
            return JAXBContext.newInstance(this.classesToBeBound, this.jaxbContextProperties);
        }
        return JAXBContext.newInstance((Class[])this.classesToBeBound);
    }
    
    private JAXBContext createJaxbContextFromPackages() throws JAXBException {
        if (this.logger.isInfoEnabled()) {
            this.logger.info((Object)("Creating JAXBContext by scanning packages [" + StringUtils.arrayToCommaDelimitedString((Object[])this.packagesToScan) + "]"));
        }
        final ClassPathJaxb2TypeScanner scanner = new ClassPathJaxb2TypeScanner(this.beanClassLoader, this.packagesToScan);
        final Class<?>[] jaxb2Classes = scanner.scanPackages();
        if (this.logger.isDebugEnabled()) {
            this.logger.debug((Object)("Found JAXB2 classes: [" + StringUtils.arrayToCommaDelimitedString((Object[])jaxb2Classes) + "]"));
        }
        this.classesToBeBound = jaxb2Classes;
        if (this.jaxbContextProperties != null) {
            return JAXBContext.newInstance(jaxb2Classes, this.jaxbContextProperties);
        }
        return JAXBContext.newInstance((Class[])jaxb2Classes);
    }
    
    private Schema loadSchema(final Resource[] resources, final String schemaLanguage) throws IOException, SAXException {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug((Object)("Setting validation schema to " + StringUtils.arrayToCommaDelimitedString((Object[])this.schemaResources)));
        }
        Assert.notEmpty((Object[])resources, "No resources given");
        Assert.hasLength(schemaLanguage, "No schema language provided");
        final Source[] schemaSources = new Source[resources.length];
        final XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
        for (int i = 0; i < resources.length; ++i) {
            Assert.notNull((Object)resources[i], "Resource is null");
            Assert.isTrue(resources[i].exists(), "Resource " + resources[i] + " does not exist");
            final InputSource inputSource = SaxResourceUtils.createInputSource(resources[i]);
            schemaSources[i] = new SAXSource(xmlReader, inputSource);
        }
        final SchemaFactory schemaFactory = SchemaFactory.newInstance(schemaLanguage);
        if (this.schemaResourceResolver != null) {
            schemaFactory.setResourceResolver(this.schemaResourceResolver);
        }
        return schemaFactory.newSchema(schemaSources);
    }
    
    public boolean supports(final Class<?> clazz) {
        return (this.supportJaxbElementClass && JAXBElement.class.isAssignableFrom(clazz)) || this.supportsInternal(clazz, this.checkForXmlRootElement);
    }
    
    public boolean supports(final Type genericType) {
        if (genericType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType)genericType;
            if (JAXBElement.class.equals(parameterizedType.getRawType()) && parameterizedType.getActualTypeArguments().length == 1) {
                final Type typeArgument = parameterizedType.getActualTypeArguments()[0];
                if (typeArgument instanceof Class) {
                    final Class<?> classArgument = (Class<?>)typeArgument;
                    if (JdkVersion.getMajorJavaVersion() >= 4 && classArgument.isArray()) {
                        return classArgument.getComponentType().equals(Byte.TYPE);
                    }
                    return this.isPrimitiveWrapper(classArgument) || this.isStandardClass(classArgument) || this.supportsInternal(classArgument, false);
                }
                else if (JdkVersion.getMajorJavaVersion() <= 3 && typeArgument instanceof GenericArrayType) {
                    final GenericArrayType arrayType = (GenericArrayType)typeArgument;
                    return arrayType.getGenericComponentType().equals(Byte.TYPE);
                }
            }
        }
        else if (genericType instanceof Class) {
            final Class<?> clazz = (Class<?>)genericType;
            return this.supportsInternal(clazz, this.checkForXmlRootElement);
        }
        return false;
    }
    
    private boolean supportsInternal(final Class<?> clazz, final boolean checkForXmlRootElement) {
        if (checkForXmlRootElement && AnnotationUtils.findAnnotation((Class)clazz, (Class)XmlRootElement.class) == null) {
            return false;
        }
        if (StringUtils.hasLength(this.contextPath)) {
            final String packageName = ClassUtils.getPackageName((Class)clazz);
            final String[] arr$;
            final String[] contextPaths = arr$ = StringUtils.tokenizeToStringArray(this.contextPath, ":");
            for (final String contextPath : arr$) {
                if (contextPath.equals(packageName)) {
                    return true;
                }
            }
            return false;
        }
        return !ObjectUtils.isEmpty((Object[])this.classesToBeBound) && Arrays.asList(this.classesToBeBound).contains(clazz);
    }
    
    private boolean isPrimitiveWrapper(final Class<?> clazz) {
        return Boolean.class.equals(clazz) || Byte.class.equals(clazz) || Short.class.equals(clazz) || Integer.class.equals(clazz) || Long.class.equals(clazz) || Float.class.equals(clazz) || Double.class.equals(clazz);
    }
    
    private boolean isStandardClass(final Class<?> clazz) {
        return String.class.equals(clazz) || BigInteger.class.isAssignableFrom(clazz) || BigDecimal.class.isAssignableFrom(clazz) || Calendar.class.isAssignableFrom(clazz) || Date.class.isAssignableFrom(clazz) || QName.class.isAssignableFrom(clazz) || URI.class.equals(clazz) || XMLGregorianCalendar.class.isAssignableFrom(clazz) || Duration.class.isAssignableFrom(clazz) || Image.class.equals(clazz) || DataHandler.class.equals(clazz) || UUID.class.equals(clazz);
    }
    
    public void marshal(final Object graph, final Result result) throws XmlMappingException {
        this.marshal(graph, result, null);
    }
    
    public void marshal(final Object graph, final Result result, final MimeContainer mimeContainer) throws XmlMappingException {
        try {
            final javax.xml.bind.Marshaller marshaller = this.createMarshaller();
            if (this.mtomEnabled && mimeContainer != null) {
                marshaller.setAttachmentMarshaller(new Jaxb2AttachmentMarshaller(mimeContainer));
            }
            if (StaxUtils.isStaxResult(result)) {
                this.marshalStaxResult(marshaller, graph, result);
            }
            else {
                marshaller.marshal(graph, result);
            }
        }
        catch (JAXBException ex) {
            throw this.convertJaxbException(ex);
        }
    }
    
    private void marshalStaxResult(final javax.xml.bind.Marshaller jaxbMarshaller, final Object graph, final Result staxResult) throws JAXBException {
        final XMLStreamWriter streamWriter = StaxUtils.getXMLStreamWriter(staxResult);
        if (streamWriter != null) {
            jaxbMarshaller.marshal(graph, streamWriter);
        }
        else {
            final XMLEventWriter eventWriter = StaxUtils.getXMLEventWriter(staxResult);
            if (eventWriter == null) {
                throw new IllegalArgumentException("StAX Result contains neither XMLStreamWriter nor XMLEventConsumer");
            }
            jaxbMarshaller.marshal(graph, eventWriter);
        }
    }
    
    protected javax.xml.bind.Marshaller createMarshaller() {
        try {
            final javax.xml.bind.Marshaller marshaller = this.getJaxbContext().createMarshaller();
            this.initJaxbMarshaller(marshaller);
            return marshaller;
        }
        catch (JAXBException ex) {
            throw this.convertJaxbException(ex);
        }
    }
    
    protected void initJaxbMarshaller(final javax.xml.bind.Marshaller marshaller) throws JAXBException {
        if (this.marshallerProperties != null) {
            for (final String name : this.marshallerProperties.keySet()) {
                marshaller.setProperty(name, this.marshallerProperties.get(name));
            }
        }
        if (this.marshallerListener != null) {
            marshaller.setListener(this.marshallerListener);
        }
        if (this.validationEventHandler != null) {
            marshaller.setEventHandler(this.validationEventHandler);
        }
        if (this.adapters != null) {
            for (final XmlAdapter<?, ?> adapter : this.adapters) {
                marshaller.setAdapter(adapter);
            }
        }
        if (this.schema != null) {
            marshaller.setSchema(this.schema);
        }
    }
    
    public Object unmarshal(final Source source) throws XmlMappingException {
        return this.unmarshal(source, null);
    }
    
    public Object unmarshal(Source source, final MimeContainer mimeContainer) throws XmlMappingException {
        source = this.processSource(source);
        try {
            final javax.xml.bind.Unmarshaller unmarshaller = this.createUnmarshaller();
            if (this.mtomEnabled && mimeContainer != null) {
                unmarshaller.setAttachmentUnmarshaller(new Jaxb2AttachmentUnmarshaller(mimeContainer));
            }
            if (StaxUtils.isStaxSource(source)) {
                return this.unmarshalStaxSource(unmarshaller, source);
            }
            if (this.mappedClass != null) {
                return unmarshaller.unmarshal(source, this.mappedClass).getValue();
            }
            return unmarshaller.unmarshal(source);
        }
        catch (JAXBException ex) {
            throw this.convertJaxbException(ex);
        }
    }
    
    protected Object unmarshalStaxSource(final javax.xml.bind.Unmarshaller jaxbUnmarshaller, final Source staxSource) throws JAXBException {
        final XMLStreamReader streamReader = StaxUtils.getXMLStreamReader(staxSource);
        if (streamReader != null) {
            return (this.mappedClass != null) ? jaxbUnmarshaller.unmarshal(streamReader, this.mappedClass).getValue() : jaxbUnmarshaller.unmarshal(streamReader);
        }
        final XMLEventReader eventReader = StaxUtils.getXMLEventReader(staxSource);
        if (eventReader != null) {
            return (this.mappedClass != null) ? jaxbUnmarshaller.unmarshal(eventReader, this.mappedClass).getValue() : jaxbUnmarshaller.unmarshal(eventReader);
        }
        throw new IllegalArgumentException("StaxSource contains neither XMLStreamReader nor XMLEventReader");
    }
    
    private Source processSource(final Source source) {
        if (StaxUtils.isStaxSource(source) || source instanceof DOMSource) {
            return source;
        }
        XMLReader xmlReader = null;
        InputSource inputSource = null;
        if (source instanceof SAXSource) {
            final SAXSource saxSource = (SAXSource)source;
            xmlReader = saxSource.getXMLReader();
            inputSource = saxSource.getInputSource();
        }
        else if (source instanceof StreamSource) {
            final StreamSource streamSource = (StreamSource)source;
            if (streamSource.getInputStream() != null) {
                inputSource = new InputSource(streamSource.getInputStream());
            }
            else if (streamSource.getReader() != null) {
                inputSource = new InputSource(streamSource.getReader());
            }
            else {
                inputSource = new InputSource(streamSource.getSystemId());
            }
        }
        try {
            if (xmlReader == null) {
                xmlReader = XMLReaderFactory.createXMLReader();
            }
            final String name = "http://xml.org/sax/features/external-general-entities";
            xmlReader.setFeature(name, this.isProcessExternalEntities());
            if (!this.isProcessExternalEntities()) {
                xmlReader.setEntityResolver(Jaxb2Marshaller.NO_OP_ENTITY_RESOLVER);
            }
            return new SAXSource(xmlReader, inputSource);
        }
        catch (SAXException ex) {
            this.logger.warn((Object)"Processing of external entities could not be disabled", (Throwable)ex);
            return source;
        }
    }
    
    protected javax.xml.bind.Unmarshaller createUnmarshaller() {
        try {
            final javax.xml.bind.Unmarshaller unmarshaller = this.getJaxbContext().createUnmarshaller();
            this.initJaxbUnmarshaller(unmarshaller);
            return unmarshaller;
        }
        catch (JAXBException ex) {
            throw this.convertJaxbException(ex);
        }
    }
    
    protected void initJaxbUnmarshaller(final javax.xml.bind.Unmarshaller unmarshaller) throws JAXBException {
        if (this.unmarshallerProperties != null) {
            for (final String name : this.unmarshallerProperties.keySet()) {
                unmarshaller.setProperty(name, this.unmarshallerProperties.get(name));
            }
        }
        if (this.unmarshallerListener != null) {
            unmarshaller.setListener(this.unmarshallerListener);
        }
        if (this.validationEventHandler != null) {
            unmarshaller.setEventHandler(this.validationEventHandler);
        }
        if (this.adapters != null) {
            for (final XmlAdapter<?, ?> adapter : this.adapters) {
                unmarshaller.setAdapter(adapter);
            }
        }
        if (this.schema != null) {
            unmarshaller.setSchema(this.schema);
        }
    }
    
    protected XmlMappingException convertJaxbException(final JAXBException ex) {
        if (ex instanceof ValidationException) {
            return new ValidationFailureException("JAXB validation exception", ex);
        }
        if (ex instanceof MarshalException) {
            return new MarshallingFailureException("JAXB marshalling exception", ex);
        }
        if (ex instanceof UnmarshalException) {
            return new UnmarshallingFailureException("JAXB unmarshalling exception", ex);
        }
        return new UncategorizedMappingException("Unknown JAXB exception", ex);
    }
    
    static {
        NO_OP_ENTITY_RESOLVER = new EntityResolver() {
            public InputSource resolveEntity(final String publicId, final String systemId) {
                return new InputSource(new StringReader(""));
            }
        };
    }
    
    private static class Jaxb2AttachmentMarshaller extends AttachmentMarshaller
    {
        private final MimeContainer mimeContainer;
        
        public Jaxb2AttachmentMarshaller(final MimeContainer mimeContainer) {
            this.mimeContainer = mimeContainer;
        }
        
        @Override
        public String addMtomAttachment(final byte[] data, final int offset, final int length, final String mimeType, final String elementNamespace, final String elementLocalName) {
            final ByteArrayDataSource dataSource = new ByteArrayDataSource(mimeType, data, offset, length);
            return this.addMtomAttachment(new DataHandler(dataSource), elementNamespace, elementLocalName);
        }
        
        @Override
        public String addMtomAttachment(final DataHandler dataHandler, final String elementNamespace, final String elementLocalName) {
            final String host = this.getHost(elementNamespace, dataHandler);
            String contentId = UUID.randomUUID() + "@" + host;
            this.mimeContainer.addAttachment("<" + contentId + ">", dataHandler);
            try {
                contentId = URLEncoder.encode(contentId, "UTF-8");
            }
            catch (UnsupportedEncodingException ex) {}
            return "cid:" + contentId;
        }
        
        private String getHost(final String elementNamespace, final DataHandler dataHandler) {
            try {
                final URI uri = new URI(elementNamespace);
                return uri.getHost();
            }
            catch (URISyntaxException e) {
                return dataHandler.getName();
            }
        }
        
        @Override
        public String addSwaRefAttachment(final DataHandler dataHandler) {
            final String contentId = UUID.randomUUID() + "@" + dataHandler.getName();
            this.mimeContainer.addAttachment(contentId, dataHandler);
            return contentId;
        }
        
        @Override
        public boolean isXOPPackage() {
            return this.mimeContainer.convertToXopPackage();
        }
    }
    
    private static class Jaxb2AttachmentUnmarshaller extends AttachmentUnmarshaller
    {
        private final MimeContainer mimeContainer;
        
        public Jaxb2AttachmentUnmarshaller(final MimeContainer mimeContainer) {
            this.mimeContainer = mimeContainer;
        }
        
        @Override
        public byte[] getAttachmentAsByteArray(final String cid) {
            try {
                final DataHandler dataHandler = this.getAttachmentAsDataHandler(cid);
                return FileCopyUtils.copyToByteArray(dataHandler.getInputStream());
            }
            catch (IOException ex) {
                throw new UnmarshallingFailureException("Couldn't read attachment", ex);
            }
        }
        
        @Override
        public DataHandler getAttachmentAsDataHandler(String contentId) {
            if (contentId.startsWith("cid:")) {
                contentId = contentId.substring("cid:".length());
                try {
                    contentId = URLDecoder.decode(contentId, "UTF-8");
                }
                catch (UnsupportedEncodingException ex) {}
                contentId = '<' + contentId + '>';
            }
            return this.mimeContainer.getAttachment(contentId);
        }
        
        @Override
        public boolean isXOPPackage() {
            return this.mimeContainer.isXopPackage();
        }
    }
    
    private static class ByteArrayDataSource implements DataSource
    {
        private final byte[] data;
        private final String contentType;
        private final int offset;
        private final int length;
        
        public ByteArrayDataSource(final String contentType, final byte[] data, final int offset, final int length) {
            this.contentType = contentType;
            this.data = data;
            this.offset = offset;
            this.length = length;
        }
        
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(this.data, this.offset, this.length);
        }
        
        public OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException();
        }
        
        public String getContentType() {
            return this.contentType;
        }
        
        public String getName() {
            return "ByteArrayDataSource";
        }
    }
}
