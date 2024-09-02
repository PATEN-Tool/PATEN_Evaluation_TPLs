// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.http.converter.xml;

import org.springframework.util.xml.StaxUtils;
import javax.xml.transform.Result;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.HttpOutputMessage;
import java.util.LinkedHashSet;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.ArrayList;
import java.util.List;
import org.springframework.util.ReflectionUtils;
import java.io.IOException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import org.springframework.http.converter.HttpMessageConversionException;
import javax.xml.bind.UnmarshalException;
import javax.xml.stream.XMLStreamException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpInputMessage;
import javax.xml.transform.Source;
import org.springframework.http.HttpHeaders;
import javax.xml.bind.annotation.XmlType;
import java.lang.annotation.Annotation;
import javax.xml.bind.annotation.XmlRootElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import org.springframework.lang.Nullable;
import org.springframework.http.MediaType;
import javax.xml.stream.XMLInputFactory;
import org.springframework.http.converter.GenericHttpMessageConverter;
import java.util.Collection;

public class Jaxb2CollectionHttpMessageConverter<T extends Collection> extends AbstractJaxb2HttpMessageConverter<T> implements GenericHttpMessageConverter<T>
{
    private final XMLInputFactory inputFactory;
    
    public Jaxb2CollectionHttpMessageConverter() {
        this.inputFactory = this.createXmlInputFactory();
    }
    
    @Override
    public boolean canRead(final Class<?> clazz, @Nullable final MediaType mediaType) {
        return false;
    }
    
    @Override
    public boolean canRead(final Type type, @Nullable final Class<?> contextClass, @Nullable final MediaType mediaType) {
        if (!(type instanceof ParameterizedType)) {
            return false;
        }
        final ParameterizedType parameterizedType = (ParameterizedType)type;
        if (!(parameterizedType.getRawType() instanceof Class)) {
            return false;
        }
        final Class<?> rawType = (Class<?>)parameterizedType.getRawType();
        if (!Collection.class.isAssignableFrom(rawType)) {
            return false;
        }
        if (parameterizedType.getActualTypeArguments().length != 1) {
            return false;
        }
        final Type typeArgument = parameterizedType.getActualTypeArguments()[0];
        if (!(typeArgument instanceof Class)) {
            return false;
        }
        final Class<?> typeArgumentClass = (Class<?>)typeArgument;
        return (typeArgumentClass.isAnnotationPresent(XmlRootElement.class) || typeArgumentClass.isAnnotationPresent(XmlType.class)) && this.canRead(mediaType);
    }
    
    @Override
    public boolean canWrite(final Class<?> clazz, @Nullable final MediaType mediaType) {
        return false;
    }
    
    @Override
    public boolean canWrite(@Nullable final Type type, @Nullable final Class<?> clazz, @Nullable final MediaType mediaType) {
        return false;
    }
    
    @Override
    protected boolean supports(final Class<?> clazz) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    protected T readFromSource(final Class<? extends T> clazz, final HttpHeaders headers, final Source source) throws Exception {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public T read(final Type type, @Nullable final Class<?> contextClass, final HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        final ParameterizedType parameterizedType = (ParameterizedType)type;
        final T result = this.createCollection((Class<?>)parameterizedType.getRawType());
        final Class<?> elementClass = (Class<?>)parameterizedType.getActualTypeArguments()[0];
        try {
            final Unmarshaller unmarshaller = this.createUnmarshaller(elementClass);
            final XMLStreamReader streamReader = this.inputFactory.createXMLStreamReader(inputMessage.getBody());
            for (int event = this.moveToFirstChildOfRootElement(streamReader); event != 8; event = this.moveToNextElement(streamReader)) {
                if (elementClass.isAnnotationPresent(XmlRootElement.class)) {
                    result.add(unmarshaller.unmarshal(streamReader));
                }
                else {
                    if (!elementClass.isAnnotationPresent(XmlType.class)) {
                        throw new HttpMessageNotReadableException("Cannot unmarshal to [" + elementClass + "]", inputMessage);
                    }
                    result.add(unmarshaller.unmarshal(streamReader, elementClass).getValue());
                }
            }
            return result;
        }
        catch (XMLStreamException ex) {
            throw new HttpMessageNotReadableException("Failed to read XML stream: " + ex.getMessage(), ex, inputMessage);
        }
        catch (UnmarshalException ex2) {
            throw new HttpMessageNotReadableException("Could not unmarshal to [" + elementClass + "]: " + ex2.getMessage(), ex2, inputMessage);
        }
        catch (JAXBException ex3) {
            throw new HttpMessageConversionException("Invalid JAXB setup: " + ex3.getMessage(), ex3);
        }
    }
    
    protected T createCollection(final Class<?> collectionClass) {
        if (!collectionClass.isInterface()) {
            try {
                return ReflectionUtils.accessibleConstructor((Class)collectionClass, new Class[0]).newInstance(new Object[0]);
            }
            catch (Throwable ex) {
                throw new IllegalArgumentException("Could not instantiate collection class: " + collectionClass.getName(), ex);
            }
        }
        if (List.class == collectionClass) {
            return (T)new ArrayList();
        }
        if (SortedSet.class == collectionClass) {
            return (T)new TreeSet();
        }
        return (T)new LinkedHashSet();
    }
    
    private int moveToFirstChildOfRootElement(final XMLStreamReader streamReader) throws XMLStreamException {
        for (int event = streamReader.next(); event != 1; event = streamReader.next()) {}
        int event;
        for (event = streamReader.next(); event != 1 && event != 8; event = streamReader.next()) {}
        return event;
    }
    
    private int moveToNextElement(final XMLStreamReader streamReader) throws XMLStreamException {
        int event;
        for (event = streamReader.getEventType(); event != 1 && event != 8; event = streamReader.next()) {}
        return event;
    }
    
    @Override
    public void write(final T t, @Nullable final Type type, @Nullable final MediaType contentType, final HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        throw new UnsupportedOperationException();
    }
    
    @Override
    protected void writeToResult(final T t, final HttpHeaders headers, final Result result) throws Exception {
        throw new UnsupportedOperationException();
    }
    
    protected XMLInputFactory createXmlInputFactory() {
        return StaxUtils.createDefensiveInputFactory();
    }
}
