// 
// Decompiled by Procyon v0.5.36
// 

package org.bouncycastle.asn1;

import java.io.InputStream;

abstract class LimitedInputStream extends InputStream
{
    protected final InputStream _in;
    private int _limit;
    private int _length;
    
    LimitedInputStream(final InputStream in, final int limit, final int length) {
        this._in = in;
        this._limit = limit;
        this._length = length;
    }
    
    int getLimit() {
        return this._limit;
    }
    
    int getRemaining() {
        return this._length;
    }
    
    protected void setParentEofDetect(final boolean eofOn00) {
        if (this._in instanceof IndefiniteLengthInputStream) {
            ((IndefiniteLengthInputStream)this._in).setEofOn00(eofOn00);
        }
    }
}
