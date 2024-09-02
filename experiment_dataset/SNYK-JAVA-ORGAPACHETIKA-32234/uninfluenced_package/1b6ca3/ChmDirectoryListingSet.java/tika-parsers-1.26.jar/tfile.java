// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.tika.parser.chm.accessor;

import org.slf4j.LoggerFactory;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.apache.tika.parser.chm.exception.ChmParsingException;
import java.util.HashSet;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.chm.core.ChmCommons;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

public class ChmDirectoryListingSet
{
    private static final Logger LOG;
    private List<DirectoryListingEntry> dlel;
    private byte[] data;
    private int placeHolder;
    private long dataOffset;
    private int controlDataIndex;
    private int resetTableIndex;
    private boolean isNotControlDataFound;
    private boolean isNotResetTableFound;
    private ChmPmglHeader PMGLheader;
    
    public ChmDirectoryListingSet(final byte[] data, final ChmItsfHeader chmItsHeader, final ChmItspHeader chmItspHeader) throws TikaException {
        this.placeHolder = -1;
        this.dataOffset = -1L;
        this.controlDataIndex = -1;
        this.resetTableIndex = -1;
        this.isNotControlDataFound = true;
        this.isNotResetTableFound = true;
        this.setDirectoryListingEntryList(new ArrayList<DirectoryListingEntry>());
        ChmCommons.assertByteArrayNotNull(data);
        this.setData(data);
        this.enumerateChmDirectoryListingList(chmItsHeader, chmItspHeader);
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("list:=" + this.getDirectoryListingEntryList().toString() + System.getProperty("line.separator"));
        sb.append("number of list items:=" + this.getDirectoryListingEntryList().size());
        return sb.toString();
    }
    
    public int getControlDataIndex() {
        return this.controlDataIndex;
    }
    
    protected void setControlDataIndex(final int controlDataIndex) {
        this.controlDataIndex = controlDataIndex;
    }
    
    public int getResetTableIndex() {
        return this.resetTableIndex;
    }
    
    protected void setResetTableIndex(final int resetTableIndex) {
        this.resetTableIndex = resetTableIndex;
    }
    
    private void setPlaceHolder(final int placeHolder) {
        this.placeHolder = placeHolder;
    }
    
    private void enumerateChmDirectoryListingList(final ChmItsfHeader chmItsHeader, final ChmItspHeader chmItspHeader) throws TikaException {
        try {
            final int startPmgl = chmItspHeader.getIndex_head();
            final int stopPmgl = chmItspHeader.getUnknown_0024();
            final int dir_offset = (int)(chmItsHeader.getDirOffset() + chmItspHeader.getHeader_len());
            this.setDataOffset(chmItsHeader.getDataOffset());
            byte[] dir_chunk = null;
            final Set<Integer> processed = new HashSet<Integer>();
            int nextBlock;
            for (int i = startPmgl; i >= 0; i = nextBlock, dir_chunk = null) {
                dir_chunk = new byte[(int)chmItspHeader.getBlock_len()];
                final int start = i * (int)chmItspHeader.getBlock_len() + dir_offset;
                dir_chunk = ChmCommons.copyOfRange(this.getData(), start, start + (int)chmItspHeader.getBlock_len());
                (this.PMGLheader = new ChmPmglHeader()).parse(dir_chunk, this.PMGLheader);
                this.enumerateOneSegment(dir_chunk);
                nextBlock = this.PMGLheader.getBlockNext();
                processed.add(i);
                if (processed.contains(nextBlock)) {
                    throw new ChmParsingException("already processed block; avoiding cycle");
                }
            }
        }
        catch (ChmParsingException e) {
            ChmDirectoryListingSet.LOG.warn("Chm parse exception", (Throwable)e);
        }
        finally {
            this.setData(null);
        }
    }
    
    private void checkControlData(final DirectoryListingEntry dle) {
        if (this.isNotControlDataFound && dle.getName().contains("ControlData")) {
            this.setControlDataIndex(this.getDirectoryListingEntryList().size());
            this.isNotControlDataFound = false;
        }
    }
    
    private void checkResetTable(final DirectoryListingEntry dle) {
        if (this.isNotResetTableFound && dle.getName().contains("ResetTable")) {
            this.setResetTableIndex(this.getDirectoryListingEntryList().size());
            this.isNotResetTableFound = false;
        }
    }
    
    public static final boolean startsWith(final byte[] data, final String prefix) {
        for (int i = 0; i < prefix.length(); ++i) {
            if (data[i] != prefix.charAt(i)) {
                return false;
            }
        }
        return true;
    }
    
    private void enumerateOneSegment(final byte[] dir_chunk) throws ChmParsingException, TikaException {
        if (dir_chunk != null) {
            if (startsWith(dir_chunk, "PMGI")) {
                final int header_len = 8;
                return;
            }
            if (!startsWith(dir_chunk, "PMGL")) {
                throw new ChmParsingException("Bad dir entry block.");
            }
            final int header_len = 20;
            this.placeHolder = header_len;
            while (this.placeHolder > 0 && this.placeHolder < dir_chunk.length - this.PMGLheader.getFreeSpace()) {
                int strlen = 0;
                byte temp;
                while ((temp = dir_chunk[this.placeHolder++]) >= 128) {
                    strlen <<= 7;
                    strlen += (temp & 0x7F);
                }
                strlen = ((strlen << 7) + temp & 0x7F);
                if (strlen > dir_chunk.length) {
                    throw new ChmParsingException("Bad data of a string length.");
                }
                final DirectoryListingEntry dle = new DirectoryListingEntry();
                dle.setNameLength(strlen);
                dle.setName(new String(ChmCommons.copyOfRange(dir_chunk, this.placeHolder, this.placeHolder + dle.getNameLength()), StandardCharsets.UTF_8));
                this.checkControlData(dle);
                this.checkResetTable(dle);
                this.setPlaceHolder(this.placeHolder + dle.getNameLength());
                if (this.placeHolder < dir_chunk.length && dir_chunk[this.placeHolder] == 0) {
                    dle.setEntryType(ChmCommons.EntryType.UNCOMPRESSED);
                }
                else {
                    dle.setEntryType(ChmCommons.EntryType.COMPRESSED);
                }
                this.setPlaceHolder(this.placeHolder + 1);
                dle.setOffset(this.getEncint(dir_chunk));
                dle.setLength(this.getEncint(dir_chunk));
                this.getDirectoryListingEntryList().add(dle);
            }
        }
    }
    
    private int getEncint(final byte[] data_chunk) {
        BigInteger bi = BigInteger.ZERO;
        final byte[] nb = { 0 };
        if (this.placeHolder < data_chunk.length) {
            byte ob;
            while ((ob = data_chunk[this.placeHolder]) < 0) {
                nb[0] = (byte)(ob & 0x7F);
                bi = bi.shiftLeft(7).add(new BigInteger(nb));
                this.setPlaceHolder(this.placeHolder + 1);
            }
            nb[0] = (byte)(ob & 0x7F);
            bi = bi.shiftLeft(7).add(new BigInteger(nb));
            this.setPlaceHolder(this.placeHolder + 1);
        }
        return bi.intValue();
    }
    
    public void setDirectoryListingEntryList(final List<DirectoryListingEntry> dlel) {
        this.dlel = dlel;
    }
    
    public List<DirectoryListingEntry> getDirectoryListingEntryList() {
        return this.dlel;
    }
    
    private void setData(final byte[] data) {
        this.data = data;
    }
    
    private byte[] getData() {
        return this.data;
    }
    
    private void setDataOffset(final long dataOffset) {
        this.dataOffset = dataOffset;
    }
    
    public long getDataOffset() {
        return this.dataOffset;
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)ChmDirectoryListingSet.class);
    }
}
