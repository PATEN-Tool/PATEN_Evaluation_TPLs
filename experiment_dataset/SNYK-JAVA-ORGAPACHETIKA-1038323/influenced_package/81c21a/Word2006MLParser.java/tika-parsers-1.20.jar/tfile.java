// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.tika.parser.microsoft.ooxml.xwpf.ml2006;

import java.util.Collections;
import java.io.IOException;
import org.xml.sax.SAXException;
import org.apache.tika.exception.TikaException;
import org.xml.sax.helpers.DefaultHandler;
import org.apache.tika.utils.XMLReaderUtils;
import org.apache.tika.sax.OfflineContentHandler;
import org.apache.tika.sax.EmbeddedContentHandler;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.tika.sax.XHTMLContentHandler;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.ContentHandler;
import java.io.InputStream;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.mime.MediaType;
import java.util.Set;
import org.apache.tika.parser.microsoft.AbstractOfficeParser;

public class Word2006MLParser extends AbstractOfficeParser
{
    protected static final Set<MediaType> SUPPORTED_TYPES;
    
    public Set<MediaType> getSupportedTypes(final ParseContext context) {
        return Word2006MLParser.SUPPORTED_TYPES;
    }
    
    public void parse(final InputStream stream, final ContentHandler handler, final Metadata metadata, final ParseContext context) throws IOException, SAXException, TikaException {
        this.configure(context);
        final XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metadata);
        xhtml.startDocument();
        try {
            XMLReaderUtils.parseSAX((InputStream)new CloseShieldInputStream(stream), (DefaultHandler)new OfflineContentHandler((ContentHandler)new EmbeddedContentHandler((ContentHandler)new Word2006MLDocHandler(xhtml, metadata, context))), context);
        }
        catch (SAXException e) {
            throw new TikaException("XML parse error", (Throwable)e);
        }
        xhtml.endDocument();
    }
    
    static {
        SUPPORTED_TYPES = Collections.singleton(MediaType.application("vnd.ms-word2006ml"));
    }
}
