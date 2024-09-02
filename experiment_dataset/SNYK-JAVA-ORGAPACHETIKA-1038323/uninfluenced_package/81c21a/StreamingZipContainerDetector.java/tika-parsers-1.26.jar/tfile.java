// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.tika.parser.pkg;

import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.apache.poi.xdgf.usermodel.XDGFRelation;
import org.apache.poi.xslf.usermodel.XSLFRelation;
import org.apache.poi.xssf.usermodel.XSSFRelation;
import org.apache.poi.xwpf.usermodel.XWPFRelation;
import java.util.concurrent.ConcurrentHashMap;
import org.xml.sax.ContentHandler;
import org.apache.tika.sax.OfflineContentHandler;
import org.xml.sax.helpers.DefaultHandler;
import org.apache.tika.utils.XMLReaderUtils;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.iwork.iwana.IWork13PackageParser;
import java.util.zip.ZipEntry;
import org.apache.tika.parser.iwork.iwana.IWork18PackageParser;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import java.io.ByteArrayOutputStream;
import org.apache.tika.parser.iwork.IWorkPackageParser;
import java.util.Iterator;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import java.util.Set;
import java.util.Collection;
import java.io.EOFException;
import org.apache.commons.compress.archivers.zip.UnsupportedZipFeatureException;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.tika.io.CloseShieldInputStream;
import java.util.HashSet;
import java.io.IOException;
import org.apache.tika.io.BoundedInputStream;
import org.apache.tika.metadata.Metadata;
import java.io.InputStream;
import org.apache.tika.mime.MediaType;
import java.util.Map;
import org.apache.tika.detect.Detector;

public class StreamingZipContainerDetector extends ZipContainerDetectorBase implements Detector
{
    private static final int MAX_MIME_TYPE = 1024;
    private static final int MAX_MANIFEST = 20971520;
    static Map<String, MediaType> OOXML_CONTENT_TYPES;
    private final int markLimit;
    
    public StreamingZipContainerDetector(final int markLimit) {
        this.markLimit = markLimit;
    }
    
    public MediaType detect(final InputStream is, final Metadata metadata) throws IOException {
        final BoundedInputStream boundedInputStream = new BoundedInputStream((long)this.markLimit, is);
        boundedInputStream.mark(this.markLimit);
        try {
            return this._detect((InputStream)boundedInputStream, metadata, false);
        }
        finally {
            boundedInputStream.reset();
        }
    }
    
    private MediaType _detect(final InputStream is, final Metadata metadata, final boolean allowStoredEntries) throws IOException {
        final Set<String> fileNames = new HashSet<String>();
        final Set<String> directoryNames = new HashSet<String>();
        MediaType mt = MediaType.APPLICATION_ZIP;
        try (final ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream((InputStream)new CloseShieldInputStream(is), "UTF8", false, allowStoredEntries)) {
            final ZipArchiveEntry zae = zipArchiveInputStream.getNextZipEntry();
            mt = this.processZAE(zae, zipArchiveInputStream, directoryNames, fileNames);
        }
        catch (UnsupportedZipFeatureException zfe) {
            if (!allowStoredEntries && zfe.getFeature() == UnsupportedZipFeatureException.Feature.DATA_DESCRIPTOR) {
                is.reset();
                mt = this._detect(is, metadata, true);
            }
        }
        catch (SecurityException e) {
            throw e;
        }
        catch (EOFException ex) {}
        catch (IOException ex2) {}
        if (mt != MediaType.APPLICATION_ZIP) {
            return mt;
        }
        final Set<String> entryNames = new HashSet<String>(fileNames);
        entryNames.addAll(directoryNames);
        mt = ZipContainerDetectorBase.detectKmz(fileNames);
        if (mt != null) {
            return mt;
        }
        mt = ZipContainerDetectorBase.detectJar(entryNames);
        if (mt != null) {
            return mt;
        }
        mt = ZipContainerDetectorBase.detectIpa(entryNames);
        if (mt != null) {
            return mt;
        }
        mt = detectIWorks(entryNames);
        if (mt != null) {
            return mt;
        }
        int hits = 0;
        for (final String s : StreamingZipContainerDetector.OOXML_HINTS) {
            if (entryNames.contains(s) && ++hits > 2) {
                return StreamingZipContainerDetector.TIKA_OOXML;
            }
        }
        return MediaType.APPLICATION_ZIP;
    }
    
    private MediaType processZAE(ZipArchiveEntry zae, final ZipArchiveInputStream zipArchiveInputStream, final Set<String> directoryNames, final Set<String> fileNames) throws IOException {
        while (zae != null) {
            final String name = zae.getName();
            if (zae.isDirectory()) {
                directoryNames.add(name);
                zae = zipArchiveInputStream.getNextZipEntry();
            }
            else {
                fileNames.add(name);
                if (name.equals("[Content_Types].xml")) {
                    final MediaType mt = parseOOXMLContentTypes((InputStream)zipArchiveInputStream);
                    if (mt != null) {
                        return mt;
                    }
                    return StreamingZipContainerDetector.TIKA_OOXML;
                }
                else {
                    if (IWorkPackageParser.IWORK_CONTENT_ENTRIES.contains(name)) {
                        final IWorkPackageParser.IWORKDocumentType type = IWorkPackageParser.IWORKDocumentType.detectType((InputStream)zipArchiveInputStream);
                        if (type != null) {
                            return type.getType();
                        }
                    }
                    else if (name.equals("mimetype")) {
                        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        final BoundedInputStream bis = new BoundedInputStream(1024L, (InputStream)zipArchiveInputStream);
                        IOUtils.copy((InputStream)bis, (OutputStream)bos);
                        if (bos.toByteArray().length > 0) {
                            return MediaType.parse(new String(bos.toByteArray(), StandardCharsets.UTF_8));
                        }
                    }
                    else if (name.equals("META-INF/manifest.xml")) {
                        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        final BoundedInputStream bis = new BoundedInputStream(20971520L, (InputStream)zipArchiveInputStream);
                        IOUtils.copy((InputStream)bis, (OutputStream)bos);
                        final MediaType mt2 = ZipContainerDetectorBase.detectStarOfficeX(new ByteArrayInputStream(bos.toByteArray()));
                        if (mt2 != null) {
                            return mt2;
                        }
                    }
                    MediaType mt = IWork18PackageParser.IWork18DocumentType.detectIfPossible((ZipEntry)zae);
                    if (mt != null) {
                        return mt;
                    }
                    mt = IWork13PackageParser.IWork13DocumentType.detectIfPossible((ZipEntry)zae);
                    if (mt != null) {
                        return mt;
                    }
                    zae = zipArchiveInputStream.getNextZipEntry();
                }
            }
        }
        return MediaType.APPLICATION_ZIP;
    }
    
    private static MediaType detectIWorks(final Set<String> entryNames) {
        if (entryNames.contains("buildVersionHistory.plist")) {
            return MediaType.application("vnd.apple.iwork");
        }
        return null;
    }
    
    public static Set<String> parseOOXMLRels(final InputStream is) {
        final RelsHandler relsHandler = new RelsHandler();
        try {
            XMLReaderUtils.parseSAX(is, (DefaultHandler)relsHandler, new ParseContext());
        }
        catch (SecurityException e) {
            throw e;
        }
        catch (Exception ex) {}
        return relsHandler.rels;
    }
    
    public static MediaType parseOOXMLContentTypes(final InputStream is) {
        final ContentTypeHandler contentTypeHandler = new ContentTypeHandler();
        try {
            XMLReaderUtils.parseSAX(is, (DefaultHandler)new OfflineContentHandler((ContentHandler)contentTypeHandler), new ParseContext());
        }
        catch (SecurityException e) {
            throw e;
        }
        catch (Exception ex) {}
        return contentTypeHandler.mediaType;
    }
    
    static {
        (StreamingZipContainerDetector.OOXML_CONTENT_TYPES = new ConcurrentHashMap<String, MediaType>()).put(XWPFRelation.DOCUMENT.getContentType(), StreamingZipContainerDetector.DOCX);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XWPFRelation.MACRO_DOCUMENT.getContentType(), StreamingZipContainerDetector.DOCM);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XWPFRelation.MACRO_TEMPLATE_DOCUMENT.getContentType(), StreamingZipContainerDetector.DOTM);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XWPFRelation.TEMPLATE.getContentType(), StreamingZipContainerDetector.DOTX);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSSFRelation.WORKBOOK.getContentType(), StreamingZipContainerDetector.XLSX);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSSFRelation.MACROS_WORKBOOK.getContentType(), StreamingZipContainerDetector.XLSM);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSSFRelation.XLSB_BINARY_WORKBOOK.getContentType(), StreamingZipContainerDetector.XLSB);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSSFRelation.TEMPLATE_WORKBOOK.getContentType(), StreamingZipContainerDetector.XLTX);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSSFRelation.MACRO_TEMPLATE_WORKBOOK.getContentType(), StreamingZipContainerDetector.XLTM);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSSFRelation.MACRO_ADDIN_WORKBOOK.getContentType(), StreamingZipContainerDetector.XLAM);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSLFRelation.MAIN.getContentType(), StreamingZipContainerDetector.PPTX);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSLFRelation.MACRO.getContentType(), StreamingZipContainerDetector.PPSM);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSLFRelation.MACRO_TEMPLATE.getContentType(), StreamingZipContainerDetector.POTM);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSLFRelation.PRESENTATIONML_TEMPLATE.getContentType(), StreamingZipContainerDetector.PPTM);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSLFRelation.PRESENTATIONML.getContentType(), StreamingZipContainerDetector.PPSX);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSLFRelation.PRESENTATION_MACRO.getContentType(), StreamingZipContainerDetector.PPTM);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSLFRelation.PRESENTATIONML_TEMPLATE.getContentType(), StreamingZipContainerDetector.POTX);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSLFRelation.THEME_MANAGER.getContentType(), StreamingZipContainerDetector.THMX);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put("application/vnd.ms-visio.drawing.macroEnabled.main+xml", MediaType.application("vnd.ms-visio.drawing.macroEnabled.12"));
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XDGFRelation.DOCUMENT.getContentType(), MediaType.application("vnd.ms-visio.drawing"));
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put("application/vnd.ms-visio.stencil.macroEnabled.main+xml", MediaType.application("vnd.ms-visio.stencil.macroenabled.12"));
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put("application/vnd.ms-visio.stencil.main+xml", MediaType.application("vnd.ms-visio.stencil"));
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put("application/vnd.ms-visio.template.macroEnabled.main+xml", MediaType.application("vnd.ms-visio.template.macroenabled.12"));
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put("application/vnd.ms-visio.template.main+xml", MediaType.application("vnd.ms-visio.template"));
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put("application/vnd.ms-package.xps-fixeddocumentsequence+xml", StreamingZipContainerDetector.XPS);
    }
    
    private static class RelsHandler extends DefaultHandler
    {
        Set<String> rels;
        private MediaType mediaType;
        
        private RelsHandler() {
            this.rels = new HashSet<String>();
            this.mediaType = null;
        }
        
        @Override
        public void startElement(final String uri, final String localName, final String name, final Attributes attrs) throws SAXException {
            for (int i = 0; i < attrs.getLength(); ++i) {
                final String attrName = attrs.getLocalName(i);
                if (attrName.equals("Type")) {
                    final String contentType = attrs.getValue(i);
                    this.rels.add(contentType);
                    if (StreamingZipContainerDetector.OOXML_CONTENT_TYPES.containsKey(contentType)) {
                        this.mediaType = StreamingZipContainerDetector.OOXML_CONTENT_TYPES.get(contentType);
                    }
                }
            }
        }
    }
    
    private static class ContentTypeHandler extends DefaultHandler
    {
        private MediaType mediaType;
        
        private ContentTypeHandler() {
            this.mediaType = null;
        }
        
        @Override
        public void startElement(final String uri, final String localName, final String name, final Attributes attrs) throws SAXException {
            for (int i = 0; i < attrs.getLength(); ++i) {
                final String attrName = attrs.getLocalName(i);
                if (attrName.equals("ContentType")) {
                    final String contentType = attrs.getValue(i);
                    if (StreamingZipContainerDetector.OOXML_CONTENT_TYPES.containsKey(contentType)) {
                        this.mediaType = StreamingZipContainerDetector.OOXML_CONTENT_TYPES.get(contentType);
                        throw new StoppingEarlyException();
                    }
                }
            }
        }
    }
}
