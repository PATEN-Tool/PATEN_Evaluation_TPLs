// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.tika.parser.pkg;

import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.apache.poi.xslf.usermodel.XSLFRelation;
import org.apache.poi.xssf.usermodel.XSSFRelation;
import org.apache.poi.xwpf.usermodel.XWPFRelation;
import java.util.concurrent.ConcurrentHashMap;
import org.xml.sax.helpers.DefaultHandler;
import org.apache.tika.utils.XMLReaderUtils;
import org.apache.tika.parser.ParseContext;
import java.util.Iterator;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import java.util.Set;
import java.util.Collection;
import org.apache.commons.io.IOUtils;
import java.nio.charset.StandardCharsets;
import org.apache.tika.parser.iwork.IWorkPackageParser;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.tika.io.CloseShieldInputStream;
import java.util.HashSet;
import java.io.InputStream;
import org.apache.tika.mime.MediaType;
import java.util.Map;

class StreamingZipContainerDetector extends ZipContainerDetectorBase
{
    static Map<String, MediaType> OOXML_CONTENT_TYPES;
    
    static MediaType detect(final InputStream is) {
        final Set<String> fileNames = new HashSet<String>();
        final Set<String> directoryNames = new HashSet<String>();
        try (final ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream((InputStream)new CloseShieldInputStream(is))) {
            ZipArchiveEntry zae = zipArchiveInputStream.getNextZipEntry();
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
                            return MediaType.parse(IOUtils.toString((InputStream)zipArchiveInputStream, StandardCharsets.UTF_8));
                        }
                        zae = zipArchiveInputStream.getNextZipEntry();
                    }
                }
            }
        }
        catch (SecurityException e) {
            throw e;
        }
        catch (Exception ex) {}
        final Set<String> entryNames = new HashSet<String>(fileNames);
        entryNames.addAll(fileNames);
        MediaType mt2 = ZipContainerDetectorBase.detectKmz(fileNames);
        if (mt2 != null) {
            return mt2;
        }
        mt2 = ZipContainerDetectorBase.detectJar(entryNames);
        if (mt2 != null) {
            return mt2;
        }
        mt2 = ZipContainerDetectorBase.detectIpa(entryNames);
        if (mt2 != null) {
            return mt2;
        }
        mt2 = detectIWorks(entryNames);
        if (mt2 != null) {
            return mt2;
        }
        int hits = 0;
        for (final String s : StreamingZipContainerDetector.OOXML_HINTS) {
            if (entryNames.contains(s) && ++hits > 2) {
                return StreamingZipContainerDetector.TIKA_OOXML;
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
            XMLReaderUtils.parseSAX(is, (DefaultHandler)contentTypeHandler, new ParseContext());
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
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XWPFRelation.TEMPLATE.getContentType(), StreamingZipContainerDetector.DOTX);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSSFRelation.WORKBOOK.getContentType(), StreamingZipContainerDetector.XLSX);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSSFRelation.MACROS_WORKBOOK.getContentType(), StreamingZipContainerDetector.XLSM);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSSFRelation.XLSB_BINARY_WORKBOOK.getContentType(), StreamingZipContainerDetector.XLSB);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSLFRelation.MAIN.getContentType(), StreamingZipContainerDetector.PPTX);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSLFRelation.MACRO.getContentType(), StreamingZipContainerDetector.PPSM);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSLFRelation.MACRO_TEMPLATE.getContentType(), StreamingZipContainerDetector.POTM);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSLFRelation.PRESENTATIONML_TEMPLATE.getContentType(), StreamingZipContainerDetector.PPTM);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSLFRelation.PRESENTATIONML.getContentType(), StreamingZipContainerDetector.PPSX);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSLFRelation.PRESENTATION_MACRO.getContentType(), StreamingZipContainerDetector.PPTM);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSLFRelation.PRESENTATIONML_TEMPLATE.getContentType(), StreamingZipContainerDetector.POTX);
        StreamingZipContainerDetector.OOXML_CONTENT_TYPES.put(XSLFRelation.THEME_MANAGER.getContentType(), StreamingZipContainerDetector.THMX);
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
    
    private static class StoppingEarlyException extends SAXException
    {
    }
}
