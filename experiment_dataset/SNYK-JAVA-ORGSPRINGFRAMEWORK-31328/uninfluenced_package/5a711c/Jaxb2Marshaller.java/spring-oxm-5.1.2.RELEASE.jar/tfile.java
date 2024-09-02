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
import javax.xml.bind.UnmarshalException;
import org.springframework.oxm.MarshallingFailureException;
import javax.xml.bind.MarshalException;
import org.springframework.oxm.ValidationFailureException;
import javax.xml.bind.ValidationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMSource;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import org.springframework.oxm.UnmarshallingFailureException;
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
import javax.xml.validation.Schema;
import javax.xml.bind.JAXBContext;
import org.w3c.dom.ls.LSResourceResolver;
import org.springframework.core.io.Resource;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Marshaller;
import java.util.Map;
import org.springframework.lang.Nullable;
import org.apache.commons.logging.Log;
import org.xml.sax.EntityResolver;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.oxm.GenericUnmarshaller;
import org.springframework.oxm.GenericMarshaller;
import org.springframework.oxm.mime.MimeUnmarshaller;
import org.springframework.oxm.mime.MimeMarshaller;

public class Jaxb2Marshaller implements MimeMarshaller, MimeUnmarshaller, GenericMarshaller, GenericUnmarshaller, BeanClassLoaderAware, InitializingBean
{
    private static final String CID = "cid:";
    private static final EntityResolver NO_OP_ENTITY_RESOLVER;
    protected final Log logger;
    @Nullable
    private String contextPath;
    @Nullable
    private Class<?>[] classesToBeBound;
    @Nullable
    private String[] packagesToScan;
    @Nullable
    private Map<String, ?> jaxbContextProperties;
    @Nullable
    private Map<String, ?> marshallerProperties;
    @Nullable
    private Map<String, ?> unmarshallerProperties;
    @Nullable
    private javax.xml.bind.Marshaller.Listener marshallerListener;
    @Nullable
    private javax.xml.bind.Unmarshaller.Listener unmarshallerListener;
    @Nullable
    private ValidationEventHandler validationEventHandler;
    @Nullable
    private XmlAdapter<?, ?>[] adapters;
    @Nullable
    private Resource[] schemaResources;
    private String schemaLanguage;
    @Nullable
    private LSResourceResolver schemaResourceResolver;
    private boolean lazyInit;
    private boolean mtomEnabled;
    private boolean supportJaxbElementClass;
    private boolean checkForXmlRootElement;
    @Nullable
    private Class<?> mappedClass;
    @Nullable
    private ClassLoader beanClassLoader;
    private final Object jaxbContextMonitor;
    @Nullable
    private volatile JAXBContext jaxbContext;
    @Nullable
    private Schema schema;
    private boolean supportDtd;
    private boolean processExternalEntities;
    
    public Jaxb2Marshaller() {
        this.logger = LogFactory.getLog((Class)this.getClass());
        this.schemaLanguage = "http://www.w3.org/2001/XMLSchema";
        this.lazyInit = false;
        this.mtomEnabled = false;
        this.supportJaxbElementClass = false;
        this.checkForXmlRootElement = true;
        this.jaxbContextMonitor = new Object();
        this.supportDtd = false;
        this.processExternalEntities = false;
    }
    
    public void setContextPaths(final String... contextPaths) {
        Assert.notEmpty((Object[])contextPaths, "'contextPaths' must not be empty");
        this.contextPath = StringUtils.arrayToDelimitedString((Object[])contextPaths, ":");
    }
    
    public void setContextPath(@Nullable final String contextPath) {
        this.contextPath = contextPath;
    }
    
    @Nullable
    public String getContextPath() {
        return this.contextPath;
    }
    
    public void setClassesToBeBound(@Nullable final Class<?>... classesToBeBound) {
        this.classesToBeBound = classesToBeBound;
    }
    
    @Nullable
    public Class<?>[] getClassesToBeBound() {
        return this.classesToBeBound;
    }
    
    public void setPackagesToScan(@Nullable final String... packagesToScan) {
        this.packagesToScan = packagesToScan;
    }
    
    @Nullable
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
    
    public void setSupportDtd(final boolean supportDtd) {
        this.supportDtd = supportDtd;
    }
    
    public boolean isSupportDtd() {
        return this.supportDtd;
    }
    
    public void setProcessExternalEntities(final boolean processExternalEntities) {
        this.processExternalEntities = processExternalEntities;
        if (processExternalEntities) {
            this.setSupportDtd(true);
        }
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
        JAXBContext context = this.jaxbContext;
        if (context != null) {
            return context;
        }
        synchronized (this.jaxbContextMonitor) {
            context = this.jaxbContext;
            if (context == null) {
                try {
                    if (StringUtils.hasLength(this.contextPath)) {
                        context = this.createJaxbContextFromContextPath(this.contextPath);
                    }
                    else if (!ObjectUtils.isEmpty((Object[])this.classesToBeBound)) {
                        context = this.createJaxbContextFromClasses(this.classesToBeBound);
                    }
                    else if (!ObjectUtils.isEmpty((Object[])this.packagesToScan)) {
                        context = this.createJaxbContextFromPackages(this.packagesToScan);
                    }
                    else {
                        context = JAXBContext.newInstance(new Class[0]);
                    }
                    this.jaxbContext = context;
                }
                catch (JAXBException ex) {
                    throw this.convertJaxbException(ex);
                }
            }
            return context;
        }
    }
    
    private JAXBContext createJaxbContextFromContextPath(final String contextPath) throws JAXBException {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug((Object)("Creating JAXBContext with context path [" + this.contextPath + "]"));
        }
        if (this.jaxbContextProperties != null) {
            if (this.beanClassLoader != null) {
                return JAXBContext.newInstance(contextPath, this.beanClassLoader, this.jaxbContextProperties);
            }
            return JAXBContext.newInstance(contextPath, Thread.currentThread().getContextClassLoader(), this.jaxbContextProperties);
        }
        else {
            if (this.beanClassLoader != null) {
                return JAXBContext.newInstance(contextPath, this.beanClassLoader);
            }
            return JAXBContext.newInstance(contextPath);
        }
    }
    
    private JAXBContext createJaxbContextFromClasses(final Class<?>... classesToBeBound) throws JAXBException {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug((Object)("Creating JAXBContext with classes to be bound [" + StringUtils.arrayToCommaDelimitedString((Object[])classesToBeBound) + "]"));
        }
        if (this.jaxbContextProperties != null) {
            return JAXBContext.newInstance(classesToBeBound, this.jaxbContextProperties);
        }
        return JAXBContext.newInstance((Class[])classesToBeBound);
    }
    
    private JAXBContext createJaxbContextFromPackages(final String... packagesToScan) throws JAXBException {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug((Object)("Creating JAXBContext by scanning packages [" + StringUtils.arrayToCommaDelimitedString((Object[])packagesToScan) + "]"));
        }
        final ClassPathJaxb2TypeScanner scanner = new ClassPathJaxb2TypeScanner(this.beanClassLoader, packagesToScan);
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
            final Resource resource = resources[i];
            Assert.isTrue(resource != null && resource.exists(), () -> "Resource does not exist: " + resource);
            final InputSource inputSource = SaxResourceUtils.createInputSource(resource);
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
    
    @Override
    public boolean supports(final Type genericType) {
        if (genericType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType)genericType;
            if (JAXBElement.class == parameterizedType.getRawType() && parameterizedType.getActualTypeArguments().length == 1) {
                final Type typeArgument = parameterizedType.getActualTypeArguments()[0];
                if (typeArgument instanceof Class) {
                    final Class<?> classArgument = (Class<?>)typeArgument;
                    return (classArgument.isArray() && Byte.TYPE == classArgument.getComponentType()) || this.isPrimitiveWrapper(classArgument) || this.isStandardClass(classArgument) || this.supportsInternal(classArgument, false);
                }
                if (typeArgument instanceof GenericArrayType) {
                    final GenericArrayType arrayType = (GenericArrayType)typeArgument;
                    return Byte.TYPE == arrayType.getGenericComponentType();
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
            final String[] tokenizeToStringArray;
            final String[] contextPaths = tokenizeToStringArray = StringUtils.tokenizeToStringArray(this.contextPath, ":");
            for (final String contextPath : tokenizeToStringArray) {
                if (contextPath.equals(packageName)) {
                    return true;
                }
            }
            return false;
        }
        return !ObjectUtils.isEmpty((Object[])this.classesToBeBound) && Arrays.asList(this.classesToBeBound).contains(clazz);
    }
    
    private boolean isPrimitiveWrapper(final Class<?> clazz) {
        return Boolean.class == clazz || Byte.class == clazz || Short.class == clazz || Integer.class == clazz || Long.class == clazz || Float.class == clazz || Double.class == clazz;
    }
    
    private boolean isStandardClass(final Class<?> clazz) {
        return String.class == clazz || BigInteger.class.isAssignableFrom(clazz) || BigDecimal.class.isAssignableFrom(clazz) || Calendar.class.isAssignableFrom(clazz) || Date.class.isAssignableFrom(clazz) || QName.class.isAssignableFrom(clazz) || URI.class == clazz || XMLGregorianCalendar.class.isAssignableFrom(clazz) || Duration.class.isAssignableFrom(clazz) || Image.class == clazz || DataHandler.class == clazz || UUID.class == clazz;
    }
    
    public void marshal(final Object graph, final Result result) throws XmlMappingException {
        this.marshal(graph, result, null);
    }
    
    @Override
    public void marshal(final Object graph, final Result result, @Nullable final MimeContainer mimeContainer) throws XmlMappingException {
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
    
    @Override
    public Object unmarshal(Source source, @Nullable final MimeContainer mimeContainer) throws XmlMappingException {
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
        catch (NullPointerException ex) {
            if (!this.isSupportDtd()) {
                throw new UnmarshallingFailureException("NPE while unmarshalling: This can happen due to the presence of DTD declarations which are disabled.", ex);
            }
            throw ex;
        }
        catch (JAXBException ex2) {
            throw this.convertJaxbException(ex2);
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
            xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !this.isSupportDtd());
            final String name = "http://xml.org/sax/features/external-general-entities";
            xmlReader.setFeature(name, this.isProcessExternalEntities());
            if (!this.isProcessExternalEntities()) {
                xmlReader.setEntityResolver(Jaxb2Marshaller.NO_OP_ENTITY_RESOLVER);
            }
            return new SAXSource(xmlReader, inputSource);
        }
        catch (SAXException ex) {
            this.logger.info((Object)"Processing of external entities could not be disabled", (Throwable)ex);
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
        final InputSource inputSource;
        NO_OP_ENTITY_RESOLVER = ((publicId, systemId) -> {
            new InputSource(new StringReader(""));
            return inputSource;
        });
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
            catch (URISyntaxException ex) {
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
            final DataHandler dataHandler = this.mimeContainer.getAttachment(contentId);
            if (dataHandler == null) {
                throw new IllegalArgumentException("No attachment found for " + contentId);
            }
            return dataHandler;
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
        
        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(this.data, this.offset, this.length);
        }
        
        @Override
        public OutputStream getOutputStream() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public String getContentType() {
            return this.contentType;
        }
        
        @Override
        public String getName() {
            return "ByteArrayDataSource";
        }
    }
}
