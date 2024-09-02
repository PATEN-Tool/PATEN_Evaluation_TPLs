// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.pdfbox.pdfparser;

import org.apache.commons.logging.LogFactory;
import java.util.TreeMap;
import org.apache.pdfbox.cos.COSBase;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;
import java.io.IOException;
import org.apache.pdfbox.cos.COSName;
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
    private final int numberOfObjects;
    private final int firstObject;
    
    public PDFObjectStreamParser(final COSStream stream, final COSDocument document) throws IOException {
        super(new InputStreamSource(stream.createInputStream()));
        this.streamObjects = null;
        this.document = document;
        this.numberOfObjects = stream.getInt(COSName.N);
        if (this.numberOfObjects == -1) {
            throw new IOException("/N entry missing in object stream");
        }
        if (this.numberOfObjects < 0) {
            throw new IOException("Illegal /N entry in object stream: " + this.numberOfObjects);
        }
        this.firstObject = stream.getInt(COSName.FIRST);
        if (this.firstObject == -1) {
            throw new IOException("/First entry missing in object stream");
        }
        if (this.firstObject < 0) {
            throw new IOException("Illegal /First entry in object stream: " + this.firstObject);
        }
    }
    
    public void parse() throws IOException {
        try {
            final Map<Integer, Long> offsets = this.readOffsets();
            this.streamObjects = new ArrayList<COSObject>(this.numberOfObjects);
            for (final Map.Entry<Integer, Long> offset : offsets.entrySet()) {
                final COSBase cosObject = this.parseObject(offset.getKey());
                final COSObject object = new COSObject(cosObject);
                object.setGenerationNumber(0);
                object.setObjectNumber(offset.getValue());
                this.streamObjects.add(object);
                if (PDFObjectStreamParser.LOG.isDebugEnabled()) {
                    PDFObjectStreamParser.LOG.debug((Object)("parsed=" + object));
                }
            }
        }
        finally {
            this.seqSource.close();
        }
    }
    
    public List<COSObject> getObjects() {
        return this.streamObjects;
    }
    
    private Map<Integer, Long> readOffsets() throws IOException {
        final Map<Integer, Long> objectNumbers = new TreeMap<Integer, Long>();
        for (int i = 0; i < this.numberOfObjects; ++i) {
            final long objectNumber = this.readObjectNumber();
            final int offset = (int)this.readLong();
            objectNumbers.put(offset, objectNumber);
        }
        return objectNumbers;
    }
    
    private COSBase parseObject(final int offset) throws IOException {
        final long currentPosition = this.seqSource.getPosition();
        final int finalPosition = this.firstObject + offset;
        if (finalPosition > 0 && currentPosition < finalPosition) {
            this.seqSource.readFully(finalPosition - (int)currentPosition);
        }
        return this.parseDirObject();
    }
    
    static {
        LOG = LogFactory.getLog((Class)PDFObjectStreamParser.class);
    }
}
