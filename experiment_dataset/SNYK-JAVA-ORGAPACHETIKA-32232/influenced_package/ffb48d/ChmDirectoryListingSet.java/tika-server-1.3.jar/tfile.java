// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.tika.parser.chm.accessor;

import java.math.BigInteger;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.chm.core.ChmCommons;
import java.util.ArrayList;
import java.util.List;

public class ChmDirectoryListingSet
{
    private List<DirectoryListingEntry> dlel;
    private byte[] data;
    private int placeHolder;
    private long dataOffset;
    private int controlDataIndex;
    private int resetTableIndex;
    private boolean isNotControlDataFound;
    private boolean isNotResetTableFound;
    
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
    
    private int getPlaceHolder() {
        return this.placeHolder;
    }
    
    private void setPlaceHolder(final int placeHolder) {
        this.placeHolder = placeHolder;
    }
    
    private void enumerateChmDirectoryListingList(final ChmItsfHeader chmItsHeader, final ChmItspHeader chmItspHeader) {
        try {
            final int startPmgl = chmItspHeader.getIndex_head();
            final int stopPmgl = chmItspHeader.getUnknown_0024();
            final int dir_offset = (int)(chmItsHeader.getDirOffset() + chmItspHeader.getHeader_len());
            this.setDataOffset(chmItsHeader.getDataOffset());
            int previous_index = 0;
            byte[] dir_chunk = null;
            for (int i = startPmgl; i <= stopPmgl; ++i) {
                final int data_copied = (1 + i) * (int)chmItspHeader.getBlock_len() + dir_offset;
                if (i == 0) {
                    dir_chunk = new byte[(int)chmItspHeader.getBlock_len()];
                    dir_chunk = ChmCommons.copyOfRange(this.getData(), dir_offset, (1 + i) * (int)chmItspHeader.getBlock_len() + dir_offset);
                    previous_index = data_copied;
                }
                else {
                    dir_chunk = new byte[(int)chmItspHeader.getBlock_len()];
                    dir_chunk = ChmCommons.copyOfRange(this.getData(), previous_index, (1 + i) * (int)chmItspHeader.getBlock_len() + dir_offset);
                    previous_index = data_copied;
                }
                this.enumerateOneSegment(dir_chunk);
                dir_chunk = null;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
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
    
    private void enumerateOneSegment(final byte[] dir_chunk) {
        try {
            if (dir_chunk != null) {
                final int indexWorkData = ChmCommons.indexOf(dir_chunk, "::".getBytes());
                final int indexUserData = ChmCommons.indexOf(dir_chunk, "/".getBytes());
                if (indexUserData < indexWorkData) {
                    this.setPlaceHolder(indexUserData);
                }
                else {
                    this.setPlaceHolder(indexWorkData);
                }
                if (this.getPlaceHolder() > 0 && dir_chunk[this.getPlaceHolder() - 1] != 115) {
                    do {
                        if (dir_chunk[this.getPlaceHolder() - 1] > 0) {
                            final DirectoryListingEntry dle = new DirectoryListingEntry();
                            this.doNameCheck(dir_chunk, dle);
                            dle.setName(new String(ChmCommons.copyOfRange(dir_chunk, this.getPlaceHolder(), this.getPlaceHolder() + dle.getNameLength())));
                            this.checkControlData(dle);
                            this.checkResetTable(dle);
                            this.setPlaceHolder(this.getPlaceHolder() + dle.getNameLength());
                            if (this.getPlaceHolder() < dir_chunk.length && dir_chunk[this.getPlaceHolder()] == 0) {
                                dle.setEntryType(ChmCommons.EntryType.UNCOMPRESSED);
                            }
                            else {
                                dle.setEntryType(ChmCommons.EntryType.COMPRESSED);
                            }
                            this.setPlaceHolder(this.getPlaceHolder() + 1);
                            dle.setOffset(this.getEncint(dir_chunk));
                            dle.setLength(this.getEncint(dir_chunk));
                            this.getDirectoryListingEntryList().add(dle);
                        }
                        else {
                            this.setPlaceHolder(this.getPlaceHolder() + 1);
                        }
                    } while (this.hasNext(dir_chunk));
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void doNameCheck(final byte[] dir_chunk, final DirectoryListingEntry dle) {
        if (dir_chunk[this.getPlaceHolder() - 1] == 115) {
            dle.setNameLength(dir_chunk[this.getPlaceHolder() - 1] & 0x21);
        }
        else if (dir_chunk[this.getPlaceHolder() + 1] == 47) {
            dle.setNameLength(dir_chunk[this.getPlaceHolder()]);
            this.setPlaceHolder(this.getPlaceHolder() + 1);
        }
        else {
            dle.setNameLength(dir_chunk[this.getPlaceHolder() - 1]);
        }
    }
    
    private boolean hasNext(final byte[] dir_chunk) {
        while (this.getPlaceHolder() < dir_chunk.length) {
            if (dir_chunk[this.getPlaceHolder()] == 47 && dir_chunk[this.getPlaceHolder() + 1] != 58) {
                this.setPlaceHolder(this.getPlaceHolder());
                return true;
            }
            if (dir_chunk[this.getPlaceHolder()] == 58 && dir_chunk[this.getPlaceHolder() + 1] == 58) {
                this.setPlaceHolder(this.getPlaceHolder());
                return true;
            }
            this.setPlaceHolder(this.getPlaceHolder() + 1);
        }
        return false;
    }
    
    private int getEncint(final byte[] data_chunk) {
        BigInteger bi = BigInteger.ZERO;
        final byte[] nb = { 0 };
        if (this.getPlaceHolder() < data_chunk.length) {
            byte ob;
            while ((ob = data_chunk[this.getPlaceHolder()]) < 0) {
                nb[0] = (byte)(ob & 0x7F);
                bi = bi.shiftLeft(7).add(new BigInteger(nb));
                this.setPlaceHolder(this.getPlaceHolder() + 1);
            }
            nb[0] = (byte)(ob & 0x7F);
            bi = bi.shiftLeft(7).add(new BigInteger(nb));
            this.setPlaceHolder(this.getPlaceHolder() + 1);
        }
        return bi.intValue();
    }
    
    public static void main(final String[] args) {
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
}
