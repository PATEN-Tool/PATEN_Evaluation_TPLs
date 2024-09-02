// 
// Decompiled by Procyon v0.5.36
// 

package org.opencastproject.mediapackage;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.Collection;
import javax.xml.bind.Unmarshaller;
import org.xml.sax.InputSource;
import org.apache.commons.io.IOUtils;
import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBException;
import java.io.Writer;
import java.io.StringWriter;
import java.util.List;
import org.opencastproject.util.data.Function;

public final class MediaPackageElementParser
{
    public static final Function<String, MediaPackageElement> getFromXml;
    public static final Function<String, List<MediaPackageElement>> getArrayFromXmlFn;
    
    private MediaPackageElementParser() {
    }
    
    public static String getAsXml(final MediaPackageElement element) throws MediaPackageException {
        if (element == null) {
            throw new IllegalArgumentException("Mediapackage element must not be null");
        }
        final StringWriter writer = new StringWriter();
        Marshaller m = null;
        try {
            m = MediaPackageImpl.context.createMarshaller();
            m.setProperty("jaxb.formatted.output", false);
            m.marshal(element, writer);
            return writer.toString();
        }
        catch (JAXBException e) {
            throw new MediaPackageException((e.getLinkedException() != null) ? e.getLinkedException() : e);
        }
    }
    
    public static <A extends MediaPackageElement> Function<A, String> getAsXml() {
        return new Function.X<A, String>() {
            @Override
            protected String xapply(final MediaPackageElement elem) throws Exception {
                return MediaPackageElementParser.getAsXml(elem);
            }
        };
    }
    
    public static MediaPackageElement getFromXml(final String xml) throws MediaPackageException {
        Unmarshaller m = null;
        try {
            m = MediaPackageImpl.context.createUnmarshaller();
            return (MediaPackageElement)m.unmarshal(new InputSource(IOUtils.toInputStream(xml)));
        }
        catch (JAXBException e) {
            throw new MediaPackageException((e.getLinkedException() != null) ? e.getLinkedException() : e);
        }
    }
    
    public static String getArrayAsXml(final Collection<? extends MediaPackageElement> elements) throws MediaPackageException {
        if (elements == null || elements.isEmpty()) {
            return "";
        }
        try {
            final StringBuilder builder = new StringBuilder();
            final Iterator<? extends MediaPackageElement> it = elements.iterator();
            builder.append(getAsXml((MediaPackageElement)it.next()));
            while (it.hasNext()) {
                builder.append("###");
                builder.append(getAsXml((MediaPackageElement)it.next()));
            }
            return builder.toString();
        }
        catch (Exception e) {
            if (e instanceof MediaPackageException) {
                throw (MediaPackageException)e;
            }
            throw new MediaPackageException(e);
        }
    }
    
    public static List<? extends MediaPackageElement> getArrayFromXml(final String xml) throws MediaPackageException {
        try {
            final List<MediaPackageElement> elements = new LinkedList<MediaPackageElement>();
            final String[] split;
            final String[] xmlArray = split = xml.split("###");
            for (final String xmlElement : split) {
                if (!"".equals(xmlElement.trim())) {
                    elements.add(getFromXml(xmlElement.trim()));
                }
            }
            return elements;
        }
        catch (Exception e) {
            if (e instanceof MediaPackageException) {
                throw (MediaPackageException)e;
            }
            throw new MediaPackageException(e);
        }
    }
    
    public static List<? extends MediaPackageElement> getArrayFromXmlUnchecked(final String xml) {
        try {
            return getArrayFromXml(xml);
        }
        catch (MediaPackageException e) {
            throw new MediaPackageRuntimeException(e);
        }
    }
    
    static {
        getFromXml = new Function.X<String, MediaPackageElement>() {
            public MediaPackageElement xapply(final String s) throws Exception {
                return MediaPackageElementParser.getFromXml(s);
            }
        };
        getArrayFromXmlFn = new Function.X<String, List<MediaPackageElement>>() {
            public List<MediaPackageElement> xapply(final String xml) throws Exception {
                return (List<MediaPackageElement>)MediaPackageElementParser.getArrayFromXml(xml);
            }
        };
    }
}
