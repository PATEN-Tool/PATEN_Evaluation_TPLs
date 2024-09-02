// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.pdfbox.pdfparser;

import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSInteger;
import java.util.ArrayList;
import java.io.IOException;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSObject;
import java.util.List;
import org.apache.commons.logging.Log;

public class PDFObjectStreamParser extends BaseParser
{
    private static final Log LOG;
    private List<COSObject> streamObjects;
    private List<Integer> objectNumbers;
    private COSStream stream;
    
    public PDFObjectStreamParser(final COSStream strm, final COSDocument doc, final boolean forceParsing) throws IOException {
        super(strm.getUnfilteredStream(), forceParsing);
        this.streamObjects = null;
        this.objectNumbers = null;
        this.setDocument(doc);
        this.stream = strm;
    }
    
    public PDFObjectStreamParser(final COSStream strm, final COSDocument doc) throws IOException {
        this(strm, doc, PDFObjectStreamParser.FORCE_PARSING);
    }
    
    public void parse() throws IOException {
        try {
            final int numberOfObjects = this.stream.getInt("N");
            this.objectNumbers = new ArrayList<Integer>(numberOfObjects);
            this.streamObjects = new ArrayList<COSObject>(numberOfObjects);
            for (int i = 0; i < numberOfObjects; ++i) {
                final int objectNumber = this.readInt();
                final int offset = this.readInt();
                this.objectNumbers.add(new Integer(objectNumber));
            }
            COSObject object = null;
            COSBase cosObject = null;
            int objectCounter = 0;
            while ((cosObject = this.parseDirObject()) != null) {
                object = new COSObject(cosObject);
                object.setGenerationNumber(COSInteger.ZERO);
                final COSInteger objNum = COSInteger.get(this.objectNumbers.get(objectCounter));
                object.setObjectNumber(objNum);
                this.streamObjects.add(object);
                if (PDFObjectStreamParser.LOG.isDebugEnabled()) {
                    PDFObjectStreamParser.LOG.debug((Object)("parsed=" + object));
                }
                ++objectCounter;
            }
        }
        finally {
            this.pdfSource.close();
        }
    }
    
    public List<COSObject> getObjects() {
        return this.streamObjects;
    }
    
    static {
        LOG = LogFactory.getLog((Class)PDFObjectStreamParser.class);
    }
}
