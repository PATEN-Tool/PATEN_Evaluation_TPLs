// 
// Decompiled by Procyon v0.5.36
// 

package org.opencastproject.caption.converters;

import org.slf4j.LoggerFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import java.util.Set;
import java.util.HashSet;
import javax.xml.transform.Transformer;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.TransformerConfigurationException;
import java.io.IOException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Node;
import javax.xml.transform.dom.DOMSource;
import org.opencastproject.util.XmlSafeParser;
import org.opencastproject.metadata.mpeg7.FreeTextAnnotationImpl;
import java.util.TimeZone;
import org.opencastproject.metadata.mpeg7.MediaLocator;
import org.opencastproject.metadata.mpeg7.MediaTimeImpl;
import java.io.OutputStream;
import org.opencastproject.caption.api.CaptionConverterException;
import org.opencastproject.caption.api.Time;
import org.opencastproject.metadata.mpeg7.MediaDuration;
import org.opencastproject.metadata.mpeg7.MediaTimePoint;
import org.opencastproject.metadata.mpeg7.MediaTime;
import org.opencastproject.metadata.mpeg7.TemporalDecomposition;
import java.util.Iterator;
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.caption.api.IllegalTimeFormatException;
import org.opencastproject.caption.impl.CaptionImpl;
import org.opencastproject.caption.impl.TimeImpl;
import java.util.Calendar;
import org.opencastproject.metadata.mpeg7.FreeTextAnnotation;
import org.opencastproject.metadata.mpeg7.TextAnnotation;
import org.opencastproject.metadata.mpeg7.AudioSegment;
import org.opencastproject.metadata.mpeg7.Audio;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogImpl;
import java.util.ArrayList;
import org.opencastproject.caption.api.Caption;
import java.util.List;
import java.io.InputStream;
import org.slf4j.Logger;
import org.opencastproject.caption.api.CaptionConverter;

public class Mpeg7CaptionConverter implements CaptionConverter
{
    private static final String EXTENSION = "xml";
    private static final Logger logger;
    
    public List<Caption> importCaption(final InputStream inputStream, final String language) throws CaptionConverterException {
        final List<Caption> captions = new ArrayList<Caption>();
        final Mpeg7Catalog catalog = (Mpeg7Catalog)new Mpeg7CatalogImpl(inputStream);
        final Iterator<Audio> audioContentIterator = (Iterator<Audio>)catalog.audioContent();
        if (audioContentIterator == null) {
            return captions;
        }
    Label_0034:
        while (true) {
            while (audioContentIterator.hasNext()) {
                final Audio audioContent = audioContentIterator.next();
                final TemporalDecomposition<AudioSegment> audioSegments = (TemporalDecomposition<AudioSegment>)audioContent.getTemporalDecomposition();
                final Iterator<AudioSegment> audioSegmentIterator = (Iterator<AudioSegment>)audioSegments.segments();
                if (audioSegmentIterator == null) {
                    continue;
                }
                while (audioSegmentIterator.hasNext()) {
                    final AudioSegment segment = audioSegmentIterator.next();
                    final Iterator<TextAnnotation> annotationIterator = (Iterator<TextAnnotation>)segment.textAnnotations();
                    if (annotationIterator == null) {
                        break;
                    }
                    while (annotationIterator.hasNext()) {
                        final TextAnnotation annotation = annotationIterator.next();
                        if (!annotation.getLanguage().equals(language)) {
                            Mpeg7CaptionConverter.logger.debug("Skipping audio content '{}' because of language mismatch", (Object)audioContent.getId());
                            continue Label_0034;
                        }
                        final List<String> captionLines = new ArrayList<String>();
                        final Iterator<FreeTextAnnotation> freeTextAnnotationIterator = (Iterator<FreeTextAnnotation>)annotation.freeTextAnnotations();
                        if (freeTextAnnotationIterator == null) {
                            continue;
                        }
                        while (freeTextAnnotationIterator.hasNext()) {
                            final FreeTextAnnotation freeTextAnnotation = freeTextAnnotationIterator.next();
                            captionLines.add(freeTextAnnotation.getText());
                        }
                        final MediaTime segmentTime = segment.getMediaTime();
                        final MediaTimePoint stp = segmentTime.getMediaTimePoint();
                        final MediaDuration d = segmentTime.getMediaDuration();
                        final Calendar startCalendar = Calendar.getInstance();
                        final int millisAtStart = (int)(stp.getTimeInMilliseconds() - ((stp.getHour() * 60 + stp.getMinutes()) * 60 + stp.getSeconds()) * 1000);
                        final int millisAtEnd = (int)(d.getDurationInMilliseconds() - ((d.getHours() * 60 + d.getMinutes()) * 60 + d.getSeconds()) * 1000);
                        startCalendar.set(10, stp.getHour());
                        startCalendar.set(12, stp.getMinutes());
                        startCalendar.set(13, stp.getSeconds());
                        startCalendar.set(14, millisAtStart);
                        startCalendar.add(10, d.getHours());
                        startCalendar.add(12, d.getMinutes());
                        startCalendar.add(13, d.getSeconds());
                        startCalendar.set(14, millisAtEnd);
                        try {
                            final Time startTime = (Time)new TimeImpl(stp.getHour(), stp.getMinutes(), stp.getSeconds(), millisAtStart);
                            final Time endTime = (Time)new TimeImpl(startCalendar.get(10), startCalendar.get(12), startCalendar.get(13), startCalendar.get(14));
                            final Caption caption = (Caption)new CaptionImpl(startTime, endTime, captionLines.toArray(new String[captionLines.size()]));
                            captions.add(caption);
                        }
                        catch (IllegalTimeFormatException e) {
                            Mpeg7CaptionConverter.logger.warn("Error setting caption time: {}", (Object)e.getMessage());
                        }
                    }
                }
            }
            break;
        }
        return captions;
    }
    
    public void exportCaption(final OutputStream outputStream, final List<Caption> captions, final String language) throws IOException {
        final Mpeg7Catalog mpeg7 = (Mpeg7Catalog)Mpeg7CatalogImpl.newInstance();
        final MediaTime mediaTime = (MediaTime)new MediaTimeImpl(0L, 0L);
        final Audio audioContent = mpeg7.addAudioContent("captions", mediaTime, (MediaLocator)null);
        final TemporalDecomposition<AudioSegment> captionDecomposition = (TemporalDecomposition<AudioSegment>)audioContent.getTemporalDecomposition();
        int segmentCount = 0;
        for (final Caption caption : captions) {
            final String[] words = caption.getCaption();
            if (words.length == 0) {
                continue;
            }
            final AudioSegment segment = (AudioSegment)captionDecomposition.createSegment("segment-" + segmentCount++);
            final Time captionST = caption.getStartTime();
            final Time captionET = caption.getStopTime();
            final Calendar startTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            startTime.setTimeInMillis(0L);
            startTime.add(11, captionST.getHours());
            startTime.add(12, captionST.getMinutes());
            startTime.add(13, captionST.getSeconds());
            startTime.add(14, captionST.getMilliseconds());
            final Calendar endTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            endTime.setTimeInMillis(0L);
            endTime.add(11, captionET.getHours());
            endTime.add(12, captionET.getMinutes());
            endTime.add(13, captionET.getSeconds());
            endTime.add(14, captionET.getMilliseconds());
            final long startTimeInMillis = startTime.getTimeInMillis();
            final long endTimeInMillis = endTime.getTimeInMillis();
            final long duration = endTimeInMillis - startTimeInMillis;
            segment.setMediaTime((MediaTime)new MediaTimeImpl(startTimeInMillis, duration));
            final TextAnnotation textAnnotation = segment.createTextAnnotation(0.0f, 0.0f, language);
            final StringBuffer captionLine = new StringBuffer();
            for (final String word : words) {
                if (captionLine.length() > 0) {
                    captionLine.append(' ');
                }
                captionLine.append(word);
            }
            textAnnotation.addFreeTextAnnotation((FreeTextAnnotation)new FreeTextAnnotationImpl(captionLine.toString()));
        }
        Transformer tf = null;
        try {
            tf = XmlSafeParser.newTransformerFactory().newTransformer();
            final DOMSource xmlSource = new DOMSource(mpeg7.toXml());
            tf.transform(xmlSource, new StreamResult(outputStream));
        }
        catch (TransformerConfigurationException e) {
            Mpeg7CaptionConverter.logger.warn("Error serializing mpeg7 captions catalog: {}", (Object)e.getMessage());
            throw new IOException(e);
        }
        catch (TransformerFactoryConfigurationError e2) {
            Mpeg7CaptionConverter.logger.warn("Error serializing mpeg7 captions catalog: {}", (Object)e2.getMessage());
            throw new IOException(e2);
        }
        catch (TransformerException e3) {
            Mpeg7CaptionConverter.logger.warn("Error serializing mpeg7 captions catalog: {}", (Object)e3.getMessage());
            throw new IOException(e3);
        }
        catch (ParserConfigurationException e4) {
            Mpeg7CaptionConverter.logger.warn("Error serializing mpeg7 captions catalog: {}", (Object)e4.getMessage());
            throw new IOException(e4);
        }
    }
    
    public String[] getLanguageList(final InputStream inputStream) throws CaptionConverterException {
        final Set<String> languages = new HashSet<String>();
        final Mpeg7Catalog catalog = (Mpeg7Catalog)new Mpeg7CatalogImpl(inputStream);
        final Iterator<Audio> audioContentIterator = (Iterator<Audio>)catalog.audioContent();
        if (audioContentIterator == null) {
            return languages.toArray(new String[languages.size()]);
        }
        while (audioContentIterator.hasNext()) {
            final Audio audioContent = audioContentIterator.next();
            final TemporalDecomposition<AudioSegment> audioSegments = (TemporalDecomposition<AudioSegment>)audioContent.getTemporalDecomposition();
            final Iterator<AudioSegment> audioSegmentIterator = (Iterator<AudioSegment>)audioSegments.segments();
            if (audioSegmentIterator == null) {
                continue;
            }
            while (audioSegmentIterator.hasNext()) {
                final AudioSegment segment = audioSegmentIterator.next();
                final Iterator<TextAnnotation> annotationIterator = (Iterator<TextAnnotation>)segment.textAnnotations();
                if (annotationIterator == null) {
                    break;
                }
                while (annotationIterator.hasNext()) {
                    final TextAnnotation annotation = annotationIterator.next();
                    final String language = annotation.getLanguage();
                    if (language != null) {
                        languages.add(language);
                    }
                }
            }
        }
        return languages.toArray(new String[languages.size()]);
    }
    
    public String getExtension() {
        return "xml";
    }
    
    public MediaPackageElement.Type getElementType() {
        return MediaPackageElement.Type.Catalog;
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)Mpeg7CaptionConverter.class);
    }
}
