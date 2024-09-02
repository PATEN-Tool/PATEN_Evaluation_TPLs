// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.tika.parser.microsoft.xml;

import org.xml.sax.helpers.AttributesImpl;
import org.apache.tika.sax.TeeContentHandler;
import org.apache.tika.metadata.OfficeOpenXMLExtended;
import org.apache.tika.metadata.OfficeOpenXMLCore;
import org.apache.tika.metadata.Office;
import org.apache.tika.metadata.TikaCoreProperties;
import java.io.IOException;
import org.xml.sax.SAXException;
import org.apache.tika.exception.TikaException;
import org.xml.sax.helpers.DefaultHandler;
import org.apache.tika.sax.OfflineContentHandler;
import org.apache.tika.sax.EmbeddedContentHandler;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.tika.utils.XMLReaderUtils;
import org.apache.tika.sax.TaggedContentHandler;
import org.apache.tika.sax.XHTMLContentHandler;
import org.apache.tika.parser.ParseContext;
import java.io.InputStream;
import org.apache.tika.parser.xml.ElementMetadataHandler;
import org.xml.sax.ContentHandler;
import org.apache.tika.metadata.Property;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.Attributes;
import org.apache.tika.parser.AbstractParser;

public abstract class AbstractXML2003Parser extends AbstractParser
{
    static final String MS_OFFICE_PROPERTIES_URN = "urn:schemas-microsoft-com:office:office";
    static final String MS_DOC_PROPERTIES_URN = "urn:schemas-microsoft-com:office:office";
    static final String MS_SPREADSHEET_URN = "urn:schemas-microsoft-com:office:spreadsheet";
    static final String MS_VML_URN = "urn:schemas-microsoft-com:vml";
    static final String WORD_ML_URL = "http://schemas.microsoft.com/office/word/2003/wordml";
    static final Attributes EMPTY_ATTRS;
    static final String DOCUMENT_PROPERTIES = "DocumentProperties";
    static final String PICT = "pict";
    static final String BIN_DATA = "binData";
    static final String A = "a";
    static final String BODY = "body";
    static final String BR = "br";
    static final String CDATA = "cdata";
    static final String DIV = "div";
    static final String HREF = "href";
    static final String IMG = "img";
    static final String P = "p";
    static final String TD = "td";
    static final String TR = "tr";
    static final String TABLE = "table";
    static final String TBODY = "tbody";
    static final String HLINK = "hlink";
    static final String HLINK_DEST = "dest";
    static final String NAME_ATTR = "name";
    static final char[] NEWLINE;
    
    private static ContentHandler getMSPropertiesHandler(final Metadata metadata, final Property property, final String element) {
        return new ElementMetadataHandler("urn:schemas-microsoft-com:office:office", element, metadata, property);
    }
    
    public void parse(final InputStream stream, final ContentHandler handler, final Metadata metadata, final ParseContext context) throws IOException, SAXException, TikaException {
        this.setContentType(metadata);
        final XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metadata);
        xhtml.startDocument();
        final TaggedContentHandler tagged = new TaggedContentHandler((ContentHandler)xhtml);
        try {
            XMLReaderUtils.getSAXParser().parse((InputStream)new CloseShieldInputStream(stream), (DefaultHandler)new OfflineContentHandler((ContentHandler)new EmbeddedContentHandler(this.getContentHandler((ContentHandler)tagged, metadata, context))));
        }
        catch (SAXException e) {
            tagged.throwIfCauseOf((Exception)e);
            throw new TikaException("XML parse error", (Throwable)e);
        }
        finally {
            xhtml.endDocument();
        }
    }
    
    protected ContentHandler getContentHandler(final ContentHandler ch, final Metadata md, final ParseContext context) {
        return (ContentHandler)new TeeContentHandler(new ContentHandler[] { getMSPropertiesHandler(md, TikaCoreProperties.TITLE, "Title"), getMSPropertiesHandler(md, TikaCoreProperties.CREATOR, "Author"), getMSPropertiesHandler(md, Office.LAST_AUTHOR, "LastAuthor"), getMSPropertiesHandler(md, OfficeOpenXMLCore.REVISION, "Revision"), getMSPropertiesHandler(md, OfficeOpenXMLExtended.TOTAL_TIME, "TotalTime"), getMSPropertiesHandler(md, TikaCoreProperties.CREATED, "Created"), getMSPropertiesHandler(md, Office.SAVE_DATE, "LastSaved"), getMSPropertiesHandler(md, Office.PAGE_COUNT, "Pages"), getMSPropertiesHandler(md, Office.WORD_COUNT, "Words"), getMSPropertiesHandler(md, Office.CHARACTER_COUNT, "Characters"), getMSPropertiesHandler(md, Office.CHARACTER_COUNT_WITH_SPACES, "CharactersWithSpaces"), getMSPropertiesHandler(md, OfficeOpenXMLExtended.COMPANY, "Company"), getMSPropertiesHandler(md, Office.LINE_COUNT, "Lines"), getMSPropertiesHandler(md, Office.PARAGRAPH_COUNT, "Paragraphs"), getMSPropertiesHandler(md, OfficeOpenXMLCore.VERSION, "Version") });
    }
    
    protected abstract void setContentType(final Metadata p0);
    
    static {
        EMPTY_ATTRS = new AttributesImpl();
        NEWLINE = new char[] { '\n' };
    }
}
