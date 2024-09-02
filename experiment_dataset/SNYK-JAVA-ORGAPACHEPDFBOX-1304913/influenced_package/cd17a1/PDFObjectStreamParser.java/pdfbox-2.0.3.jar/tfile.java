// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.pdfbox.pdfparser;

import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSBase;
import java.util.ArrayList;
import java.io.IOException;
import java.io.InputStream;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSObject;
import java.util.List;
import org.apache.commons.logging.Log;

public class PDFObjectStreamParser extends BaseParser
{
    private static final Log LOG;
    private List<COSObject> streamObjects;
    private final COSStream stream;
    
    public PDFObjectStreamParser(final COSStream stream, final COSDocument document) throws IOException {
        super(new InputStreamSource(stream.createInputStream()));
        this.streamObjects = null;
        this.stream = stream;
        this.document = document;
    }
    
    public void parse() throws IOException {
        try {
            final int numberOfObjects = this.stream.getInt("N");
            final List<Long> objectNumbers = new ArrayList<Long>(numberOfObjects);
            this.streamObjects = new ArrayList<COSObject>(numberOfObjects);
            for (int i = 0; i < numberOfObjects; ++i) {
                final long objectNumber = this.readObjectNumber();
                this.readLong();
                objectNumbers.add(objectNumber);
            }
            int objectCounter = 0;
            COSBase cosObject;
            while ((cosObject = this.parseDirObject()) != null) {
                final COSObject object = new COSObject(cosObject);
                object.setGenerationNumber(0);
                if (objectCounter >= objectNumbers.size()) {
                    PDFObjectStreamParser.LOG.error((Object)("/ObjStm (object stream) has more objects than /N " + numberOfObjects));
                    break;
                }
                object.setObjectNumber(objectNumbers.get(objectCounter));
                this.streamObjects.add(object);
                if (PDFObjectStreamParser.LOG.isDebugEnabled()) {
                    PDFObjectStreamParser.LOG.debug((Object)("parsed=" + object));
                }
                if (!this.seqSource.isEOF() && this.seqSource.peek() == 101) {
                    this.readLine();
                }
                ++objectCounter;
            }
        }
        finally {
            this.seqSource.close();
        }
    }
    
    public List<COSObject> getObjects() {
        return this.streamObjects;
    }
    
    static {
        LOG = LogFactory.getLog((Class)PDFObjectStreamParser.class);
    }
}
