// 
// Decompiled by Procyon v0.5.36
// 

package com.ctc.wstx.sr;

final class Element
{
    protected String mLocalName;
    protected String mPrefix;
    protected String mNamespaceURI;
    protected String mDefaultNsURI;
    protected int mNsOffset;
    protected Element mParent;
    protected int mChildCount;
    
    public Element(final Element parent, final int nsOffset, final String prefix, final String ln) {
        this.mParent = parent;
        this.mNsOffset = nsOffset;
        this.mPrefix = prefix;
        this.mLocalName = ln;
    }
    
    public void reset(final Element parent, final int nsOffset, final String prefix, final String ln) {
        this.mParent = parent;
        this.mNsOffset = nsOffset;
        this.mPrefix = prefix;
        this.mLocalName = ln;
        this.mChildCount = 0;
    }
    
    public void relink(final Element next) {
        this.mParent = next;
    }
}
