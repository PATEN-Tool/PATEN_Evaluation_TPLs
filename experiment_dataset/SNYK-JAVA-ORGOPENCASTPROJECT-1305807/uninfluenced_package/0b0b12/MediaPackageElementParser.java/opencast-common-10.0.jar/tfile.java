// 
// Decompiled by Procyon v0.5.36
// 

package org.opencastproject.mediapackage;

import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.Collection;
import javax.xml.bind.Unmarshaller;
import org.xml.sax.SAXException;
import java.io.IOException;
import org.w3c.dom.Node;
import org.opencastproject.util.XmlSafeParser;
import org.apache.commons.io.IOUtils;
import org.opencastproject.util.data.Function;
import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBException;
import java.io.Writer;
import java.io.StringWriter;

public final class MediaPackageElementParser
{
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
            return (MediaPackageElement)m.unmarshal(XmlSafeParser.parse(IOUtils.toInputStream(xml)));
        }
        catch (JAXBException e) {
            throw new MediaPackageException((e.getLinkedException() != null) ? e.getLinkedException() : e);
        }
        catch (IOException | SAXException ex2) {
            final Exception ex;
            final Exception e2 = ex;
            throw new MediaPackageException(e2);
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
}
