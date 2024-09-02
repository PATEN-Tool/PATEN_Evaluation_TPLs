// 
// Decompiled by Procyon v0.5.36
// 

package org.jruby;

import org.jcodings.specific.UTF32LEEncoding;
import org.jcodings.specific.UTF32BEEncoding;
import org.jcodings.specific.UTF16LEEncoding;
import org.jruby.util.Pack;
import java.io.IOException;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.Numeric;
import org.joni.Region;
import org.joni.Matcher;
import org.joni.Regex;
import org.jruby.util.RegexpOptions;
import jnr.posix.POSIX;
import java.util.Arrays;
import org.jcodings.exception.EncodingException;
import org.jruby.util.ConvertBytes;
import org.jcodings.unicode.UnicodeEncoding;
import org.jruby.runtime.CallSite;
import org.jruby.anno.FrameField;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.Visibility;
import org.jruby.util.PerlHash;
import org.jruby.util.SipHashInline;
import org.jruby.util.Sprintf;
import java.util.Locale;
import org.jruby.runtime.JavaSites;
import org.jruby.util.TypeConverter;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Helpers;
import org.jruby.platform.Platform;
import org.jruby.runtime.ThreadContext;
import java.nio.charset.Charset;
import org.jcodings.specific.UTF16BEEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.util.ByteListHolder;
import org.jruby.util.io.EncodingUtils;
import org.jruby.util.CodeRangeSupport;
import org.jruby.util.StringSupport;
import org.jcodings.Encoding;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.util.ByteList;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.anno.JRubyClass;
import org.jruby.util.CodeRangeable;
import org.jruby.runtime.encoding.MarshalEncoding;
import org.jruby.runtime.encoding.EncodingCapable;

@JRubyClass(name = { "String" }, include = { "Enumerable", "Comparable" })
public class RubyString extends RubyObject implements EncodingCapable, MarshalEncoding, CodeRangeable
{
    public static final String DEBUG_INFO_FIELD = "@debug_created_info";
    private static final ASCIIEncoding ASCII;
    private static final UTF8Encoding UTF8;
    private static final int SHARE_LEVEL_NONE = 0;
    private static final int SHARE_LEVEL_BUFFER = 1;
    private static final int SHARE_LEVEL_BYTELIST = 2;
    private static final byte[] SCRUB_REPL_UTF8;
    private static final byte[] SCRUB_REPL_ASCII;
    private static final byte[] SCRUB_REPL_UTF16BE;
    private static final byte[] SCRUB_REPL_UTF16LE;
    private static final byte[] SCRUB_REPL_UTF32BE;
    private static final byte[] SCRUB_REPL_UTF32LE;
    private volatile int shareLevel;
    private ByteList value;
    private static final String[][] opTable19;
    private static ObjectAllocator STRING_ALLOCATOR;
    private static final ByteList EMPTY_ASCII8BIT_BYTELIST;
    private static final ByteList EMPTY_USASCII_BYTELIST;
    private static EmptyByteListHolder[] EMPTY_BYTELISTS;
    private static final ByteList SPACE_BYTELIST;
    
    public static RubyClass createStringClass(final Ruby runtime) {
        final RubyClass stringClass = runtime.defineClass("String", runtime.getObject(), RubyString.STRING_ALLOCATOR);
        runtime.setString(stringClass);
        stringClass.setClassIndex(ClassIndex.STRING);
        stringClass.setReifiedClass(RubyString.class);
        stringClass.kindOf = new RubyModule.JavaClassKindOf(RubyString.class);
        stringClass.includeModule(runtime.getComparable());
        stringClass.defineAnnotatedMethods(RubyString.class);
        return stringClass;
    }
    
    @Override
    public Encoding getEncoding() {
        return this.value.getEncoding();
    }
    
    @Override
    public void setEncoding(final Encoding encoding) {
        this.value.setEncoding(encoding);
    }
    
    @Override
    public boolean shouldMarshalEncoding() {
        return this.getEncoding() != ASCIIEncoding.INSTANCE;
    }
    
    @Override
    public Encoding getMarshalEncoding() {
        return this.getEncoding();
    }
    
    public void associateEncoding(final Encoding enc) {
        StringSupport.associateEncoding(this, enc);
    }
    
    public final void setEncodingAndCodeRange(final Encoding enc, final int cr) {
        this.value.setEncoding(enc);
        this.setCodeRange(cr);
    }
    
    public final Encoding toEncoding(final Ruby runtime) {
        return runtime.getEncodingService().findEncoding(this);
    }
    
    @Override
    public final int getCodeRange() {
        return this.flags & 0x30;
    }
    
    @Override
    public final void setCodeRange(final int codeRange) {
        this.clearCodeRange();
        this.flags |= (codeRange & 0x30);
    }
    
    @Override
    public final void clearCodeRange() {
        this.flags &= 0xFFFFFFCF;
    }
    
    @Override
    public final void keepCodeRange() {
        if (this.getCodeRange() == 48) {
            this.clearCodeRange();
        }
    }
    
    public final boolean isCodeRangeAsciiOnly() {
        return CodeRangeSupport.isCodeRangeAsciiOnly(this);
    }
    
    public final boolean isAsciiOnly() {
        return StringSupport.isAsciiOnly(this);
    }
    
    @Override
    public final boolean isCodeRangeValid() {
        return (this.flags & 0x30) == 0x20;
    }
    
    public final boolean isCodeRangeBroken() {
        return (this.flags & 0x30) == 0x30;
    }
    
    public final boolean isBrokenString() {
        return this.scanForCodeRange() == 48;
    }
    
    private void copyCodeRangeForSubstr(final RubyString from, final Encoding enc) {
        if (this.value.getRealSize() == 0) {
            this.setCodeRange(enc.isAsciiCompatible() ? 16 : 32);
        }
        else {
            final int fromCr = from.getCodeRange();
            if (fromCr == 16) {
                this.setCodeRange(fromCr);
            }
            else {
                this.setCodeRange(0);
            }
        }
    }
    
    @Override
    public final int scanForCodeRange() {
        int cr = this.getCodeRange();
        if (cr == 0) {
            final Encoding enc = this.getEncoding();
            if (enc.minLength() > 1 && enc.isDummy()) {
                cr = 48;
            }
            else {
                cr = StringSupport.codeRangeScan(EncodingUtils.getActualEncoding(this.getEncoding(), this.value), this.value);
            }
            this.setCodeRange(cr);
        }
        return cr;
    }
    
    final boolean singleByteOptimizable() {
        return StringSupport.isSingleByteOptimizable(this, EncodingUtils.STR_ENC_GET(this));
    }
    
    final boolean singleByteOptimizable(final Encoding enc) {
        return StringSupport.isSingleByteOptimizable(this, enc);
    }
    
    final Encoding isCompatibleWith(final EncodingCapable other) {
        if (other instanceof RubyString) {
            return this.checkEncoding((RubyString)other);
        }
        final Encoding enc1 = this.value.getEncoding();
        final Encoding enc2 = other.getEncoding();
        if (enc1 == enc2) {
            return enc1;
        }
        if (this.value.getRealSize() == 0) {
            return enc2;
        }
        if (!enc1.isAsciiCompatible() || !enc2.isAsciiCompatible()) {
            return null;
        }
        if (enc2 instanceof USASCIIEncoding) {
            return enc1;
        }
        if (this.scanForCodeRange() == 16) {
            return enc2;
        }
        return null;
    }
    
    public final Encoding checkEncoding(final RubyString other) {
        return this.checkEncoding((CodeRangeable)other);
    }
    
    final Encoding checkEncoding(final EncodingCapable other) {
        final Encoding enc = this.isCompatibleWith(other);
        if (enc == null) {
            throw this.getRuntime().newEncodingCompatibilityError("incompatible character encodings: " + this.value.getEncoding() + " and " + other.getEncoding());
        }
        return enc;
    }
    
    @Override
    public final Encoding checkEncoding(final CodeRangeable other) {
        final Encoding enc = StringSupport.areCompatible(this, other);
        if (enc == null) {
            throw this.getRuntime().newEncodingCompatibilityError("incompatible character encodings: " + this.value.getEncoding() + " and " + other.getByteList().getEncoding());
        }
        return enc;
    }
    
    private Encoding checkDummyEncoding() {
        final Encoding enc = this.value.getEncoding();
        if (enc.isDummy()) {
            throw this.getRuntime().newEncodingCompatibilityError("incompatible encoding with this operation: " + enc);
        }
        return enc;
    }
    
    public final int strLength() {
        return StringSupport.strLengthFromRubyString(this);
    }
    
    final int subLength(final int pos) {
        if (pos < 0 || this.singleByteOptimizable()) {
            return pos;
        }
        return StringSupport.strLength(this.value.getEncoding(), this.value.getUnsafeBytes(), this.value.getBegin(), this.value.getBegin() + pos);
    }
    
    @Override
    public final boolean eql(final IRubyObject other) {
        final RubyClass metaclass = this.getMetaClass();
        final Ruby runtime = metaclass.getClassRuntime();
        if (metaclass != runtime.getString() || metaclass != other.getMetaClass()) {
            return super.eql(other);
        }
        return this.eql19(other);
    }
    
    private boolean eql19(final IRubyObject other) {
        final RubyString otherString = (RubyString)other;
        return StringSupport.areComparable(this, otherString) && this.value.equal(otherString.value);
    }
    
    public RubyString(final Ruby runtime, final RubyClass rubyClass) {
        this(runtime, rubyClass, ByteList.NULL_ARRAY);
    }
    
    public RubyString(final Ruby runtime, final RubyClass rubyClass, final CharSequence value) {
        this(runtime, rubyClass, value, null);
    }
    
    public RubyString(final Ruby runtime, final RubyClass rubyClass, final CharSequence value, Encoding enc) {
        super(runtime, rubyClass);
        this.shareLevel = 0;
        assert value != null;
        if (enc == null) {
            enc = (Encoding)RubyString.UTF8;
        }
        this.value = encodeBytelist(value, enc);
    }
    
    public RubyString(final Ruby runtime, final RubyClass rubyClass, final byte[] value) {
        super(runtime, rubyClass);
        this.shareLevel = 0;
        assert value != null;
        this.value = new ByteList(value);
    }
    
    public RubyString(final Ruby runtime, final RubyClass rubyClass, final ByteList value) {
        super(runtime, rubyClass);
        this.shareLevel = 0;
        assert value != null;
        this.value = value;
    }
    
    public RubyString(final Ruby runtime, final RubyClass rubyClass, final ByteList value, final boolean objectSpace) {
        super(runtime, rubyClass, objectSpace);
        this.shareLevel = 0;
        assert value != null;
        this.value = value;
    }
    
    public RubyString(final Ruby runtime, final RubyClass rubyClass, final ByteList value, final Encoding encoding, final boolean objectSpace) {
        this(runtime, rubyClass, value, objectSpace);
        value.setEncoding(encoding);
    }
    
    protected RubyString(final Ruby runtime, final RubyClass rubyClass, final ByteList value, final Encoding enc, final int cr) {
        this(runtime, rubyClass, value);
        value.setEncoding(enc);
        this.flags |= cr;
    }
    
    protected RubyString(final Ruby runtime, final RubyClass rubyClass, final ByteList value, final Encoding enc) {
        this(runtime, rubyClass, value);
        value.setEncoding(enc);
    }
    
    protected RubyString(final Ruby runtime, final RubyClass rubyClass, final ByteList value, final int cr) {
        this(runtime, rubyClass, value);
        this.flags |= cr;
    }
    
    @Deprecated
    public RubyString newString(final CharSequence s) {
        return new RubyString(this.getRuntime(), this.getType(), s);
    }
    
    @Deprecated
    public RubyString newString(final ByteList s) {
        return new RubyString(this.getRuntime(), this.getMetaClass(), s);
    }
    
    @Deprecated
    public static RubyString newString(final Ruby runtime, final RubyClass clazz, final CharSequence str) {
        return new RubyString(runtime, clazz, str);
    }
    
    public static RubyString newStringLight(final Ruby runtime, final ByteList bytes) {
        return new RubyString(runtime, runtime.getString(), bytes, false);
    }
    
    public static RubyString newStringLight(final Ruby runtime, final int size) {
        return new RubyString(runtime, runtime.getString(), new ByteList(size), false);
    }
    
    public static RubyString newStringLight(final Ruby runtime, final int size, final Encoding encoding) {
        return new RubyString(runtime, runtime.getString(), new ByteList(size), encoding, false);
    }
    
    public static RubyString newString(final Ruby runtime, final CharSequence str) {
        return new RubyString(runtime, runtime.getString(), str);
    }
    
    public static RubyString newString(final Ruby runtime, final String str) {
        return new RubyString(runtime, runtime.getString(), str);
    }
    
    public static RubyString newString(final Ruby runtime, final String str, final Encoding encoding) {
        return new RubyString(runtime, runtime.getString(), str, encoding);
    }
    
    public static RubyString newUSASCIIString(final Ruby runtime, final String str) {
        return new RubyString(runtime, runtime.getString(), str, (Encoding)USASCIIEncoding.INSTANCE);
    }
    
    public static RubyString newString(final Ruby runtime, final byte[] bytes) {
        return new RubyString(runtime, runtime.getString(), bytes);
    }
    
    public static RubyString newString(final Ruby runtime, final byte[] bytes, final int start, final int length) {
        return newString(runtime, bytes, start, length, (Encoding)ASCIIEncoding.INSTANCE);
    }
    
    public static RubyString newString(final Ruby runtime, final byte[] bytes, final int start, final int length, final Encoding encoding) {
        final byte[] copy = new byte[length];
        System.arraycopy(bytes, start, copy, 0, length);
        return new RubyString(runtime, runtime.getString(), new ByteList(copy, encoding, false));
    }
    
    public static RubyString newString(final Ruby runtime, final ByteList bytes) {
        return new RubyString(runtime, runtime.getString(), bytes);
    }
    
    public static RubyString newString(final Ruby runtime, final ByteList bytes, final int coderange) {
        return new RubyString(runtime, runtime.getString(), bytes, coderange);
    }
    
    public static RubyString newString(final Ruby runtime, final ByteList bytes, final Encoding encoding) {
        return new RubyString(runtime, runtime.getString(), bytes, encoding);
    }
    
    public static RubyString newUnicodeString(final Ruby runtime, final String str) {
        final Encoding defaultInternal = runtime.getDefaultInternalEncoding();
        if (defaultInternal == UTF16BEEncoding.INSTANCE) {
            return newUTF16String(runtime, str);
        }
        return newUTF8String(runtime, str);
    }
    
    public static RubyString newUTF8String(final Ruby runtime, final String str) {
        final ByteList byteList = new ByteList(RubyEncoding.encodeUTF8(str), (Encoding)UTF8Encoding.INSTANCE, false);
        return new RubyString(runtime, runtime.getString(), byteList);
    }
    
    public static RubyString newUTF16String(final Ruby runtime, final String str) {
        final ByteList byteList = new ByteList(RubyEncoding.encodeUTF16(str), (Encoding)UTF16BEEncoding.INSTANCE, false);
        return new RubyString(runtime, runtime.getString(), byteList);
    }
    
    public static RubyString newUnicodeString(final Ruby runtime, final CharSequence str) {
        final Encoding defaultInternal = runtime.getDefaultInternalEncoding();
        if (defaultInternal == UTF16BEEncoding.INSTANCE) {
            return newUTF16String(runtime, str);
        }
        return newUTF8String(runtime, str);
    }
    
    public static RubyString newUTF8String(final Ruby runtime, final CharSequence str) {
        final ByteList byteList = new ByteList(RubyEncoding.encodeUTF8(str), (Encoding)UTF8Encoding.INSTANCE, false);
        return new RubyString(runtime, runtime.getString(), byteList);
    }
    
    public static RubyString newUTF16String(final Ruby runtime, final CharSequence str) {
        final ByteList byteList = new ByteList(RubyEncoding.encodeUTF16(str), (Encoding)UTF16BEEncoding.INSTANCE, false);
        return new RubyString(runtime, runtime.getString(), byteList);
    }
    
    public static RubyString newInternalFromJavaExternal(final Ruby runtime, final String str) {
        final Encoding internal = runtime.getDefaultInternalEncoding();
        Charset rubyInt = null;
        if (internal != null) {
            rubyInt = internal.getCharset();
        }
        if (rubyInt == null) {
            final Encoding javaExtEncoding = runtime.getEncodingService().getJavaDefault();
            return newString(runtime, new ByteList(str.getBytes(), javaExtEncoding));
        }
        return newString(runtime, new ByteList(RubyEncoding.encode(str, rubyInt), internal));
    }
    
    public static RubyString newStringShared(final Ruby runtime, final RubyString orig) {
        orig.shareLevel = 2;
        final RubyString str = new RubyString(runtime, runtime.getString(), orig.value);
        str.shareLevel = 2;
        return str;
    }
    
    public static RubyString newStringShared(final Ruby runtime, final ByteList bytes) {
        return newStringShared(runtime, runtime.getString(), bytes);
    }
    
    public static RubyString newStringShared(final Ruby runtime, final ByteList bytes, final Encoding encoding) {
        return newStringShared(runtime, runtime.getString(), bytes, encoding);
    }
    
    public static RubyString newStringShared(final Ruby runtime, final ByteList bytes, final int codeRange) {
        final RubyString str = new RubyString(runtime, runtime.getString(), bytes, codeRange);
        str.shareLevel = 2;
        return str;
    }
    
    public static RubyString newStringShared(final Ruby runtime, final RubyClass clazz, final ByteList bytes) {
        final RubyString str = new RubyString(runtime, clazz, bytes);
        str.shareLevel = 2;
        return str;
    }
    
    public static RubyString newStringShared(final Ruby runtime, final RubyClass clazz, final ByteList bytes, final Encoding encoding) {
        final RubyString str = new RubyString(runtime, clazz, bytes, encoding);
        str.shareLevel = 2;
        return str;
    }
    
    public static RubyString newStringShared(final Ruby runtime, final byte[] bytes) {
        return newStringShared(runtime, new ByteList(bytes, false));
    }
    
    public static RubyString newStringShared(final Ruby runtime, final byte[] bytes, final Encoding encoding) {
        return newStringShared(runtime, new ByteList(bytes, encoding, false));
    }
    
    public static RubyString newStringShared(final Ruby runtime, final byte[] bytes, final int start, final int length) {
        return newStringShared(runtime, new ByteList(bytes, start, length, false));
    }
    
    public static RubyString newStringShared(final Ruby runtime, final byte[] bytes, final int start, final int length, final Encoding encoding) {
        return newStringShared(runtime, new ByteList(bytes, start, length, encoding, false));
    }
    
    public static RubyString newEmptyString(final Ruby runtime) {
        return newEmptyString(runtime, runtime.getString());
    }
    
    public static RubyString newAllocatedString(final Ruby runtime, final RubyClass metaClass) {
        final RubyString empty = new RubyString(runtime, metaClass, RubyString.EMPTY_ASCII8BIT_BYTELIST);
        empty.shareLevel = 2;
        return empty;
    }
    
    public static RubyString newEmptyString(final Ruby runtime, final RubyClass metaClass) {
        final RubyString empty = new RubyString(runtime, metaClass, RubyString.EMPTY_USASCII_BYTELIST);
        empty.shareLevel = 2;
        return empty;
    }
    
    public static RubyString newStringNoCopy(final Ruby runtime, final ByteList bytes) {
        return newStringNoCopy(runtime, runtime.getString(), bytes);
    }
    
    public static RubyString newStringNoCopy(final Ruby runtime, final RubyClass clazz, final ByteList bytes) {
        return new RubyString(runtime, clazz, bytes);
    }
    
    public static RubyString newStringNoCopy(final Ruby runtime, final byte[] bytes, final int start, final int length) {
        return newStringNoCopy(runtime, new ByteList(bytes, start, length, false));
    }
    
    public static RubyString newStringNoCopy(final Ruby runtime, final byte[] bytes) {
        return newStringNoCopy(runtime, new ByteList(bytes, false));
    }
    
    public final boolean independent() {
        return this.shareLevel == 0;
    }
    
    public final RubyString makeIndependent() {
        final RubyClass klass = this.metaClass;
        final RubyString str = this.strDup(klass.getClassRuntime(), klass);
        str.modify();
        str.setFrozen(true);
        str.infectBy(this);
        return str;
    }
    
    public final RubyString makeIndependent(final int length) {
        final RubyClass klass = this.metaClass;
        final RubyString str = this.strDup(klass.getClassRuntime(), klass);
        str.modify(length);
        str.setFrozen(true);
        str.infectBy(this);
        return str;
    }
    
    public RubyString export(final ThreadContext context) {
        if (Platform.IS_WINDOWS) {
            return EncodingUtils.strConvEncOpts(context, this, null, (Encoding)UTF8Encoding.INSTANCE, 0, context.nil);
        }
        return this;
    }
    
    static EmptyByteListHolder getEmptyByteList(Encoding enc) {
        if (enc == null) {
            enc = (Encoding)ASCIIEncoding.INSTANCE;
        }
        final int index = enc.getIndex();
        final EmptyByteListHolder bytes;
        if (index < RubyString.EMPTY_BYTELISTS.length && (bytes = RubyString.EMPTY_BYTELISTS[index]) != null) {
            return bytes;
        }
        return prepareEmptyByteList(enc);
    }
    
    private static EmptyByteListHolder prepareEmptyByteList(Encoding enc) {
        if (enc == null) {
            enc = (Encoding)ASCIIEncoding.INSTANCE;
        }
        final int index = enc.getIndex();
        if (index >= RubyString.EMPTY_BYTELISTS.length) {
            final EmptyByteListHolder[] tmp = new EmptyByteListHolder[index + 4];
            System.arraycopy(RubyString.EMPTY_BYTELISTS, 0, tmp, 0, RubyString.EMPTY_BYTELISTS.length);
            RubyString.EMPTY_BYTELISTS = tmp;
        }
        return RubyString.EMPTY_BYTELISTS[index] = new EmptyByteListHolder(enc);
    }
    
    public static RubyString newEmptyString(final Ruby runtime, final RubyClass metaClass, final Encoding enc) {
        final EmptyByteListHolder holder = getEmptyByteList(enc);
        final RubyString empty = new RubyString(runtime, metaClass, holder.bytes, holder.cr);
        empty.shareLevel = 2;
        return empty;
    }
    
    public static RubyString newEmptyString(final Ruby runtime, final Encoding enc) {
        return newEmptyString(runtime, runtime.getString(), enc);
    }
    
    public static RubyString newStringNoCopy(final Ruby runtime, final RubyClass clazz, final ByteList bytes, final Encoding enc, final int cr) {
        return new RubyString(runtime, clazz, bytes, enc, cr);
    }
    
    public static RubyString newStringNoCopy(final Ruby runtime, final ByteList bytes, final Encoding enc, final int cr) {
        return newStringNoCopy(runtime, runtime.getString(), bytes, enc, cr);
    }
    
    public static RubyString newUsAsciiStringNoCopy(final Ruby runtime, final ByteList bytes) {
        return newStringNoCopy(runtime, bytes, (Encoding)USASCIIEncoding.INSTANCE, 16);
    }
    
    public static RubyString newUsAsciiStringShared(final Ruby runtime, final ByteList bytes) {
        final RubyString str = newUsAsciiStringNoCopy(runtime, bytes);
        str.shareLevel = 2;
        return str;
    }
    
    public static RubyString newUsAsciiStringShared(final Ruby runtime, final byte[] bytes, final int start, final int length) {
        final byte[] copy = new byte[length];
        System.arraycopy(bytes, start, copy, 0, length);
        return newUsAsciiStringShared(runtime, new ByteList(copy, false));
    }
    
    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.STRING;
    }
    
    @Override
    public Class getJavaClass() {
        return String.class;
    }
    
    @Override
    public RubyString convertToString() {
        return this;
    }
    
    @Override
    public String toString() {
        return this.decodeString();
    }
    
    public String decodeString() {
        return Helpers.decodeByteList(this.getRuntime(), this.value);
    }
    
    @Override
    public IRubyObject dup() {
        final RubyClass mc = this.metaClass.getRealClass();
        if (mc.getClassIndex() != ClassIndex.STRING) {
            return super.dup();
        }
        return this.strDup(mc.getClassRuntime(), mc.getRealClass());
    }
    
    public IRubyObject dupFrozen() {
        final RubyString dup = (RubyString)this.dup();
        dup.setFrozen(true);
        return dup;
    }
    
    public final RubyString strDup(final Ruby runtime) {
        return this.strDup(runtime, this.getMetaClass().getRealClass());
    }
    
    final RubyString strDup(final Ruby runtime, final RubyClass clazz) {
        this.shareLevel = 2;
        final RubyString dup = new RubyString(runtime, clazz, this.value);
        dup.shareLevel = 2;
        final RubyString rubyString = dup;
        rubyString.flags |= (this.flags & (0x30 | RubyString.TAINTED_F));
        return dup;
    }
    
    public final RubyString makeSharedString(final Ruby runtime, final int index, final int len) {
        return this.makeShared19(runtime, runtime.getString(), index, len);
    }
    
    public RubyString makeSharedString19(final Ruby runtime, final int index, final int len) {
        return this.makeShared19(runtime, runtime.getString(), this.value, index, len);
    }
    
    public final RubyString makeShared(final Ruby runtime, final int index, final int len) {
        return this.makeShared19(runtime, this.getType(), index, len);
    }
    
    public final RubyString makeShared(final Ruby runtime, final RubyClass meta, final int index, final int len) {
        RubyString shared;
        if (len == 0) {
            shared = newEmptyString(runtime, meta);
        }
        else if (len == 1) {
            shared = newStringShared(runtime, meta, RubyInteger.SINGLE_CHAR_BYTELISTS[this.value.getUnsafeBytes()[this.value.getBegin() + index] & 0xFF]);
        }
        else {
            if (this.shareLevel == 0) {
                this.shareLevel = 1;
            }
            shared = new RubyString(runtime, meta, this.value.makeShared(index, len));
            shared.shareLevel = 1;
        }
        shared.infectBy(this);
        return shared;
    }
    
    public final RubyString makeShared19(final Ruby runtime, final int index, final int len) {
        return this.makeShared19(runtime, this.value, index, len);
    }
    
    public final RubyString makeShared19(final Ruby runtime, final RubyClass meta, final int index, final int len) {
        return this.makeShared19(runtime, meta, this.value, index, len);
    }
    
    private RubyString makeShared19(final Ruby runtime, final ByteList value, final int index, final int len) {
        return this.makeShared19(runtime, this.getType(), value, index, len);
    }
    
    private RubyString makeShared19(final Ruby runtime, final RubyClass meta, final ByteList value, final int index, final int len) {
        final Encoding enc = value.getEncoding();
        RubyString shared;
        if (len == 0) {
            shared = newEmptyString(runtime, meta, enc);
        }
        else if (len == 1) {
            final ByteList bytes = new ByteList(new byte[] { (byte)value.get(index) }, enc);
            shared = new RubyString(runtime, meta, bytes, enc);
        }
        else {
            if (this.shareLevel == 0) {
                this.shareLevel = 1;
            }
            shared = new RubyString(runtime, meta, value.makeShared(index, len));
            shared.shareLevel = 1;
        }
        shared.copyCodeRangeForSubstr(this, enc);
        shared.infectBy(this);
        return shared;
    }
    
    public final void setByteListShared() {
        if (this.shareLevel != 2) {
            this.shareLevel = 2;
        }
    }
    
    public final void modifyCheck() {
        this.frozenCheck();
    }
    
    public void modifyCheck(final byte[] b, final int len) {
        if (this.value.getUnsafeBytes() != b || this.value.getRealSize() != len) {
            throw this.getRuntime().newRuntimeError("string modified");
        }
    }
    
    private void modifyCheck(final byte[] b, final int len, final Encoding enc) {
        if (this.value.getUnsafeBytes() != b || this.value.getRealSize() != len || this.value.getEncoding() != enc) {
            throw this.getRuntime().newRuntimeError("string modified");
        }
    }
    
    private void frozenCheck() {
        this.frozenCheck(false);
    }
    
    private void frozenCheck(final boolean runtimeError) {
        if (this.isFrozen()) {
            if (this.getRuntime().getInstanceConfig().isDebuggingFrozenStringLiteral()) {
                final IRubyObject obj = this.getInstanceVariable("@debug_created_info");
                if (obj != null && obj instanceof RubyArray) {
                    final RubyArray info = (RubyArray)obj;
                    if (info.getLength() == 2) {
                        throw this.getRuntime().newRaiseException(this.getRuntime().getRuntimeError(), "can't modify frozen String, created at " + info.eltInternal(0) + ":" + info.eltInternal(1));
                    }
                }
            }
            throw this.getRuntime().newFrozenError("String", runtimeError);
        }
    }
    
    @Override
    public final void modify() {
        this.modifyCheck();
        if (this.shareLevel != 0) {
            if (this.shareLevel == 2) {
                this.value = this.value.dup();
            }
            else {
                this.value.unshare();
            }
            this.shareLevel = 0;
        }
        this.value.invalidate();
    }
    
    public final void modify19() {
        this.modify();
        this.clearCodeRange();
    }
    
    @Override
    public void modifyAndKeepCodeRange() {
        this.modify();
        this.keepCodeRange();
    }
    
    @Override
    public final void modify(final int length) {
        this.modifyCheck();
        if (this.shareLevel != 0) {
            if (this.shareLevel == 2) {
                this.value = this.value.dup(length);
            }
            else {
                this.value.unshare(length);
            }
            this.shareLevel = 0;
        }
        else {
            this.value.ensure(length);
        }
        this.value.invalidate();
    }
    
    public final void modifyExpand(final int length) {
        this.modify(length);
        this.clearCodeRange();
    }
    
    public void setReadLength(final int length) {
        if (this.size() != length) {
            this.modify();
            this.value.setRealSize(length);
        }
    }
    
    public RubyString newFrozen() {
        if (this.isFrozen()) {
            return this;
        }
        final RubyClass klass = this.getMetaClass();
        final RubyString str = this.strDup(klass.getClassRuntime());
        str.setCodeRange(this.getCodeRange());
        str.setFrozen(true);
        return str;
    }
    
    public final void resize(final int length) {
        if (this.value.getRealSize() > length) {
            this.modify();
            this.value.setRealSize(length);
        }
        else if (this.value.length() < length) {
            this.modify();
            this.value.length(length);
        }
    }
    
    public final void view(final ByteList bytes) {
        this.modifyCheck();
        this.value = bytes;
        this.shareLevel = 0;
    }
    
    private void view(final byte[] bytes, final boolean copy) {
        this.modifyCheck();
        this.value = new ByteList(bytes, copy);
        this.shareLevel = 0;
        this.value.invalidate();
    }
    
    private void view(final int index, final int len) {
        this.modifyCheck();
        if (this.shareLevel != 0) {
            if (this.shareLevel == 2) {
                this.value = this.value.makeShared(index, len);
                this.shareLevel = 1;
            }
            else {
                this.value.view(index, len);
            }
        }
        else {
            this.value.view(index, len);
            this.shareLevel = 1;
        }
        this.value.invalidate();
    }
    
    public static String bytesToString(final byte[] bytes, final int beg, final int len) {
        return new String(ByteList.plain(bytes, beg, len));
    }
    
    public static String byteListToString(final ByteList bytes) {
        return bytesToString(bytes.getUnsafeBytes(), bytes.begin(), bytes.length());
    }
    
    public static String bytesToString(final byte[] bytes) {
        return bytesToString(bytes, 0, bytes.length);
    }
    
    public static byte[] stringToBytes(final String string) {
        return ByteList.plain((CharSequence)string);
    }
    
    @Override
    public RubyString asString() {
        return this;
    }
    
    @Override
    public IRubyObject checkStringType() {
        return this;
    }
    
    @Override
    public IRubyObject checkStringType19() {
        return this;
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject try_convert(final ThreadContext context, final IRubyObject recv, final IRubyObject str) {
        return str.checkStringType();
    }
    
    @JRubyMethod(name = { "to_s", "to_str" })
    @Override
    public IRubyObject to_s() {
        final Ruby runtime = this.getRuntime();
        if (this.getMetaClass().getRealClass() != runtime.getString()) {
            return this.strDup(runtime, runtime.getString());
        }
        return this;
    }
    
    @Override
    public final int compareTo(final IRubyObject other) {
        return (int)this.op_cmp(this.getRuntime().getCurrentContext(), other).convertToInteger().getLongValue();
    }
    
    @JRubyMethod(name = { "<=>" })
    @Override
    public IRubyObject op_cmp(final ThreadContext context, final IRubyObject other) {
        final Ruby runtime = context.runtime;
        if (other instanceof RubyString) {
            return runtime.newFixnum(this.op_cmp((RubyString)other));
        }
        final JavaSites.CheckedSites sites = sites(context).to_str_checked;
        if (!sites.respond_to_X.respondsTo(context, this, other)) {
            return RubyComparable.invcmp(context, sites(context).recursive_cmp, this, other);
        }
        final IRubyObject tmp = TypeConverter.checkStringType(context, sites, other);
        if (tmp instanceof RubyString) {
            return runtime.newFixnum(this.op_cmp((RubyString)tmp));
        }
        return runtime.getNil();
    }
    
    @Override
    public IRubyObject op_equal(final ThreadContext context, final IRubyObject other) {
        return this.op_equal19(context, other);
    }
    
    @JRubyMethod(name = { "==", "===" })
    public IRubyObject op_equal19(final ThreadContext context, final IRubyObject other) {
        final Ruby runtime = context.runtime;
        if (this == other) {
            return runtime.getTrue();
        }
        if (other instanceof RubyString) {
            final RubyString otherString = (RubyString)other;
            return (StringSupport.areComparable(this, otherString) && this.value.equal(otherString.value)) ? runtime.getTrue() : runtime.getFalse();
        }
        return this.op_equalCommon(context, other);
    }
    
    private IRubyObject op_equalCommon(final ThreadContext context, final IRubyObject other) {
        final Ruby runtime = context.runtime;
        if (!sites(context).respond_to_to_str.respondsTo(context, this, other)) {
            return runtime.getFalse();
        }
        return sites(context).equals.call(context, this, other, this).isTrue() ? runtime.getTrue() : runtime.getFalse();
    }
    
    @JRubyMethod(name = { "-@" })
    public final IRubyObject minus_at() {
        return this.isFrozen() ? this : this.dupFrozen();
    }
    
    @JRubyMethod(name = { "+@" })
    public final IRubyObject plus_at() {
        return this.isFrozen() ? this.dup() : this;
    }
    
    public IRubyObject op_plus(final ThreadContext context, final IRubyObject arg) {
        return this.op_plus19(context, arg);
    }
    
    @JRubyMethod(name = { "+" }, required = 1)
    public IRubyObject op_plus19(final ThreadContext context, final IRubyObject arg) {
        final RubyString str = arg.convertToString();
        final Encoding enc = this.checkEncoding(str);
        final RubyString resultStr = newStringNoCopy(context.runtime, StringSupport.addByteLists(this.value, str.value), enc, CodeRangeSupport.codeRangeAnd(this.getCodeRange(), str.getCodeRange()));
        resultStr.infectBy(this.flags | str.flags);
        return resultStr;
    }
    
    public IRubyObject op_mul(final ThreadContext context, final IRubyObject other) {
        return this.op_mul19(context, other);
    }
    
    @JRubyMethod(name = { "*" }, required = 1)
    public IRubyObject op_mul19(final ThreadContext context, final IRubyObject other) {
        final RubyString result = this.multiplyByteList(context, other);
        result.value.setEncoding(this.value.getEncoding());
        result.copyCodeRangeForSubstr(this, this.value.getEncoding());
        return result;
    }
    
    private RubyString multiplyByteList(final ThreadContext context, final IRubyObject arg) {
        int len = RubyNumeric.num2int(arg);
        if (len < 0) {
            throw context.runtime.newArgumentError("negative argument");
        }
        if (len > 0 && Integer.MAX_VALUE / len < this.value.getRealSize()) {
            throw context.runtime.newArgumentError("argument too big");
        }
        final ByteList bytes = new ByteList(len *= this.value.getRealSize());
        if (len > 0) {
            bytes.setRealSize(len);
            int n = this.value.getRealSize();
            System.arraycopy(this.value.getUnsafeBytes(), this.value.getBegin(), bytes.getUnsafeBytes(), 0, n);
            while (n <= len >> 1) {
                System.arraycopy(bytes.getUnsafeBytes(), 0, bytes.getUnsafeBytes(), n, n);
                n <<= 1;
            }
            System.arraycopy(bytes.getUnsafeBytes(), 0, bytes.getUnsafeBytes(), n, len - n);
        }
        final RubyString result = new RubyString(context.runtime, this.getMetaClass(), bytes);
        result.infectBy(this);
        return result;
    }
    
    @JRubyMethod(name = { "%" }, required = 1)
    public RubyString op_format(final ThreadContext context, final IRubyObject arg) {
        return this.opFormatCommon(context, arg);
    }
    
    private RubyString opFormatCommon(final ThreadContext context, final IRubyObject arg) {
        IRubyObject tmp;
        if (arg instanceof RubyHash) {
            tmp = arg;
        }
        else {
            tmp = arg.checkArrayType();
            if (tmp.isNil()) {
                tmp = arg;
            }
        }
        final ByteList out = new ByteList(this.value.getRealSize());
        out.setEncoding(this.value.getEncoding());
        final boolean tainted = Sprintf.sprintf1_9(out, Locale.US, (CharSequence)this.value, tmp);
        final RubyString str = newString(context.runtime, out);
        str.setTaint(tainted || this.isTaint());
        return str;
    }
    
    @JRubyMethod
    @Override
    public RubyFixnum hash() {
        final Ruby runtime = this.getRuntime();
        return RubyFixnum.newFixnum(runtime, this.strHashCode(runtime));
    }
    
    @Override
    public int hashCode() {
        return this.strHashCode(this.getRuntime());
    }
    
    public int strHashCode(final Ruby runtime) {
        long hash = runtime.isSiphashEnabled() ? SipHashInline.hash24(runtime.getHashSeedK0(), runtime.getHashSeedK1(), this.value.getUnsafeBytes(), this.value.getBegin(), this.value.getRealSize()) : PerlHash.hash(runtime.getHashSeedK0(), this.value.getUnsafeBytes(), this.value.getBegin(), this.value.getRealSize());
        hash ^= ((this.value.getEncoding().isAsciiCompatible() && this.scanForCodeRange() == 16) ? 0 : this.value.getEncoding().getIndex());
        return (int)hash;
    }
    
    public int unseededStrHashCode(final Ruby runtime) {
        long hash = runtime.isSiphashEnabled() ? SipHashInline.hash24(0L, 0L, this.value.getUnsafeBytes(), this.value.getBegin(), this.value.getRealSize()) : PerlHash.hash(0L, this.value.getUnsafeBytes(), this.value.getBegin(), this.value.getRealSize());
        hash ^= ((this.value.getEncoding().isAsciiCompatible() && this.scanForCodeRange() == 16) ? 0 : this.value.getEncoding().getIndex());
        return (int)hash;
    }
    
    @Override
    public boolean equals(final Object other) {
        return this == other || (other instanceof RubyString && ((RubyString)other).value.equal(this.value));
    }
    
    public static RubyString objAsString(final ThreadContext context, final IRubyObject obj) {
        if (obj instanceof RubyString) {
            return (RubyString)obj;
        }
        final IRubyObject str = sites(context).to_s.call(context, obj, obj);
        if (!(str instanceof RubyString)) {
            return (RubyString)obj.anyToString();
        }
        if (obj.isTaint() && !str.isTaint() && !str.isFrozen()) {
            str.setTaint(true);
        }
        return (RubyString)str;
    }
    
    public final int op_cmp(final RubyString other) {
        final int ret = this.value.cmp(other.value);
        if (ret == 0 && !StringSupport.areComparable(this, other)) {
            return (this.value.getEncoding().getIndex() > other.value.getEncoding().getIndex()) ? 1 : -1;
        }
        return ret;
    }
    
    @Override
    public String asJavaString() {
        return this.toString();
    }
    
    public IRubyObject doClone() {
        return newString(this.getRuntime(), this.value.dup());
    }
    
    public final RubyString cat(final byte[] str) {
        this.modify(this.value.getRealSize() + str.length);
        System.arraycopy(str, 0, this.value.getUnsafeBytes(), this.value.getBegin() + this.value.getRealSize(), str.length);
        this.value.setRealSize(this.value.getRealSize() + str.length);
        return this;
    }
    
    public final RubyString cat(final byte[] str, final int beg, final int len) {
        this.modify(this.value.getRealSize() + len);
        if (len == 0) {
            return this;
        }
        System.arraycopy(str, beg, this.value.getUnsafeBytes(), this.value.getBegin() + this.value.getRealSize(), len);
        this.value.setRealSize(this.value.getRealSize() + len);
        return this;
    }
    
    public final RubyString cat19(final RubyString str2) {
        final int str2_cr = this.cat19(str2.getByteList(), str2.getCodeRange());
        this.infectBy(str2);
        str2.setCodeRange(str2_cr);
        return this;
    }
    
    public final int cat19(final ByteList other, final int codeRange) {
        final int[] ptr_cr_ret = { codeRange };
        EncodingUtils.encCrStrBufCat(this.getRuntime(), this, other, other.getEncoding(), codeRange, ptr_cr_ret);
        return ptr_cr_ret[0];
    }
    
    public final RubyString cat(final RubyString str) {
        return this.cat(str.getByteList());
    }
    
    public final RubyString cat(final ByteList str) {
        this.modify(this.value.getRealSize() + str.getRealSize());
        System.arraycopy(str.getUnsafeBytes(), str.getBegin(), this.value.getUnsafeBytes(), this.value.getBegin() + this.value.getRealSize(), str.getRealSize());
        this.value.setRealSize(this.value.getRealSize() + str.getRealSize());
        return this;
    }
    
    public final RubyString cat(final byte ch) {
        this.modify(this.value.getRealSize() + 1);
        this.value.getUnsafeBytes()[this.value.getBegin() + this.value.getRealSize()] = ch;
        this.value.setRealSize(this.value.getRealSize() + 1);
        return this;
    }
    
    public final RubyString cat(final int ch) {
        return this.cat((byte)ch);
    }
    
    public final RubyString cat(final int code, final Encoding enc) {
        final int n = StringSupport.codeLength(enc, code);
        this.modify(this.value.getRealSize() + n);
        enc.codeToMbc(code, this.value.getUnsafeBytes(), this.value.getBegin() + this.value.getRealSize());
        this.value.setRealSize(this.value.getRealSize() + n);
        return this;
    }
    
    public final int cat(final byte[] bytes, final int p, final int len, final Encoding enc) {
        final int[] ptr_cr_ret = { 0 };
        EncodingUtils.encCrStrBufCat(this.getRuntime(), this, new ByteList(bytes, p, len), enc, 0, ptr_cr_ret);
        return ptr_cr_ret[0];
    }
    
    public final RubyString catAscii(final byte[] bytes, int ptr, final int ptrLen) {
        final Encoding enc = this.value.getEncoding();
        if (enc.isAsciiCompatible()) {
            EncodingUtils.encCrStrBufCat(this.getRuntime(), this, new ByteList(bytes, ptr, ptrLen), enc, 16, null);
        }
        else {
            final byte[] buf = new byte[enc.maxLength()];
            for (int end = ptr + ptrLen; ptr < end; ++ptr) {
                final int c = bytes[ptr];
                final int len = StringSupport.codeLength(enc, c);
                EncodingUtils.encMbcput(c, buf, 0, enc);
                EncodingUtils.encCrStrBufCat(this.getRuntime(), this, buf, 0, len, enc, 32, null);
            }
        }
        return this;
    }
    
    public IRubyObject replace(final IRubyObject other) {
        return this.replace19(other);
    }
    
    @JRubyMethod(name = { "initialize_copy" }, required = 1, visibility = Visibility.PRIVATE)
    @Override
    public RubyString initialize_copy(final IRubyObject other) {
        return this.replace19(other);
    }
    
    @JRubyMethod(name = { "replace" }, required = 1)
    public RubyString replace19(final IRubyObject other) {
        this.modifyCheck();
        if (this == other) {
            return this;
        }
        this.setCodeRange(this.replaceCommon(other).getCodeRange());
        return this;
    }
    
    private RubyString replaceCommon(final IRubyObject other) {
        this.modifyCheck();
        final RubyString convertToString;
        final RubyString otherStr = convertToString = other.convertToString();
        final int n = 2;
        this.shareLevel = n;
        convertToString.shareLevel = n;
        this.value = otherStr.value;
        this.infectBy(otherStr);
        return otherStr;
    }
    
    @JRubyMethod
    public RubyString clear() {
        this.modifyCheck();
        final Encoding enc = this.value.getEncoding();
        final EmptyByteListHolder holder = getEmptyByteList(enc);
        this.value = holder.bytes;
        this.shareLevel = 2;
        this.setCodeRange(holder.cr);
        return this;
    }
    
    public IRubyObject reverse(final ThreadContext context) {
        return this.reverse19(context);
    }
    
    @JRubyMethod(name = { "reverse" })
    public IRubyObject reverse19(final ThreadContext context) {
        final RubyString str = this.strDup(context.runtime);
        str.reverse_bang19(context);
        return str;
    }
    
    public RubyString reverse_bang(final ThreadContext context) {
        return this.reverse_bang19(context);
    }
    
    @JRubyMethod(name = { "reverse!" })
    public RubyString reverse_bang19(final ThreadContext context) {
        this.modifyCheck();
        if (this.value.getRealSize() > 1) {
            this.modifyAndKeepCodeRange();
            final byte[] bytes = this.value.getUnsafeBytes();
            int p = this.value.getBegin();
            final int len = this.value.getRealSize();
            final int end = p + len;
            int op = len;
            int cr = this.getCodeRange();
            final Encoding enc = this.value.getEncoding();
            if (this.singleByteOptimizable()) {
                for (int i = 0; i < len >> 1; ++i) {
                    final byte b = bytes[p + i];
                    bytes[p + i] = bytes[p + len - i - 1];
                    bytes[p + len - i - 1] = b;
                }
            }
            else if (cr == 32) {
                final byte[] obytes = new byte[len];
                while (p < end) {
                    final int cl = StringSupport.encFastMBCLen(bytes, p, end, enc);
                    op -= cl;
                    System.arraycopy(bytes, p, obytes, op, cl);
                    p += cl;
                }
                this.value.setUnsafeBytes(obytes);
            }
            else {
                final byte[] obytes = new byte[len];
                cr = (enc.isAsciiCompatible() ? 16 : 32);
                while (p < end) {
                    final int cl = StringSupport.length(enc, bytes, p, end);
                    if (cl > 1 || (bytes[p] & 0x80) != 0x0) {
                        cr = 0;
                    }
                    op -= cl;
                    System.arraycopy(bytes, p, obytes, op, cl);
                    p += cl;
                }
                this.value.setUnsafeBytes(obytes);
            }
            this.setCodeRange(cr);
        }
        return this;
    }
    
    public static RubyString newInstance(final IRubyObject recv, final IRubyObject[] args, final Block block) {
        final RubyString newString = newStringShared(recv.getRuntime(), ByteList.EMPTY_BYTELIST);
        newString.setMetaClass((RubyClass)recv);
        newString.callInit(args, block);
        return newString;
    }
    
    @JRubyMethod(name = { "initialize" }, visibility = Visibility.PRIVATE)
    @Override
    public IRubyObject initialize(final ThreadContext context) {
        return this;
    }
    
    @JRubyMethod(name = { "initialize" }, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(final ThreadContext context, final IRubyObject arg0) {
        final IRubyObject tmp = ArgsUtil.getOptionsArg(context.runtime, arg0);
        if (tmp.isNil()) {
            return this.initialize(context, arg0, null);
        }
        return this.initialize(context, null, (RubyHash)tmp);
    }
    
    @JRubyMethod(name = { "initialize" }, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(final ThreadContext context, final IRubyObject arg0, final IRubyObject opts) {
        final Ruby runtime = context.runtime;
        final IRubyObject tmp = ArgsUtil.getOptionsArg(context.runtime, opts);
        if (tmp.isNil()) {
            throw runtime.newArgumentError(2, 1);
        }
        return this.initialize(context, arg0, (RubyHash)tmp);
    }
    
    private IRubyObject initialize(final ThreadContext context, final IRubyObject arg0, final RubyHash opts) {
        final Ruby runtime = context.runtime;
        if (arg0 != null) {
            this.replace19(arg0);
        }
        if (opts != null) {
            final IRubyObject encoding = opts.fastARef(context.runtime.newSymbol("encoding"));
            if (!encoding.isNil()) {
                this.modify();
                this.setEncodingAndCodeRange(runtime.getEncodingService().getEncodingFromObject(encoding), 0);
            }
        }
        return this;
    }
    
    @Deprecated
    public IRubyObject initialize19(final ThreadContext context, final IRubyObject arg0) {
        return this.initialize(context, arg0);
    }
    
    public IRubyObject casecmp(final ThreadContext context, final IRubyObject other) {
        return this.casecmp19(context, other);
    }
    
    @JRubyMethod(name = { "casecmp" })
    public IRubyObject casecmp19(final ThreadContext context, final IRubyObject other) {
        final Ruby runtime = context.runtime;
        final RubyString otherStr = other.convertToString();
        final Encoding enc = StringSupport.areCompatible(this, otherStr);
        if (enc == null) {
            return runtime.getNil();
        }
        if (this.singleByteOptimizable() && otherStr.singleByteOptimizable()) {
            return RubyFixnum.newFixnum(runtime, this.value.caseInsensitiveCmp(otherStr.value));
        }
        final int ret = StringSupport.multiByteCasecmp(enc, this.value, otherStr.value);
        if (ret < 0) {
            return RubyFixnum.minus_one(runtime);
        }
        if (ret > 0) {
            return RubyFixnum.one(runtime);
        }
        return RubyFixnum.zero(runtime);
    }
    
    @Override
    public IRubyObject op_match(final ThreadContext context, final IRubyObject other) {
        return this.op_match19(context, other);
    }
    
    @JRubyMethod(name = { "=~" }, writes = { FrameField.BACKREF })
    @Override
    public IRubyObject op_match19(final ThreadContext context, final IRubyObject other) {
        if (other instanceof RubyRegexp) {
            return ((RubyRegexp)other).op_match19(context, this);
        }
        if (other instanceof RubyString) {
            throw context.runtime.newTypeError("type mismatch: String given");
        }
        return sites(context).op_match.call(context, other, other, this);
    }
    
    public IRubyObject match(final ThreadContext context, final IRubyObject pattern) {
        return this.match19(context, pattern, Block.NULL_BLOCK);
    }
    
    @JRubyMethod(name = { "match" }, reads = { FrameField.BACKREF })
    public IRubyObject match19(final ThreadContext context, final IRubyObject pattern, final Block block) {
        final RubyRegexp coercedPattern = this.getPattern(pattern);
        final IRubyObject result = sites(context).match.call(context, coercedPattern, coercedPattern, this);
        return (block.isGiven() && !result.isNil()) ? block.yield(context, result) : result;
    }
    
    @JRubyMethod(name = { "match" }, required = 1, rest = true, reads = { FrameField.BACKREF })
    public IRubyObject match19(final ThreadContext context, final IRubyObject[] args, final Block block) {
        final RubyRegexp pattern = this.getPattern(args[0]);
        args[0] = this;
        final IRubyObject result = sites(context).match.call(context, pattern, pattern, args);
        return (block.isGiven() && !result.isNil()) ? block.yield(context, result) : result;
    }
    
    public IRubyObject capitalize(final ThreadContext context) {
        return this.capitalize19(context);
    }
    
    public IRubyObject capitalize_bang(final ThreadContext context) {
        return this.capitalize_bang19(context);
    }
    
    @JRubyMethod(name = { "capitalize" })
    public IRubyObject capitalize19(final ThreadContext context) {
        final RubyString str = this.strDup(context.runtime);
        str.capitalize_bang19(context);
        return str;
    }
    
    @JRubyMethod(name = { "capitalize!" })
    public IRubyObject capitalize_bang19(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        final Encoding enc = this.checkDummyEncoding();
        if (this.value.getRealSize() == 0) {
            this.modifyCheck();
            return runtime.getNil();
        }
        this.modifyAndKeepCodeRange();
        int s = this.value.getBegin();
        final int end = s + this.value.getRealSize();
        final byte[] bytes = this.value.getUnsafeBytes();
        boolean modify = false;
        int c = StringSupport.codePoint(runtime, enc, bytes, s, end);
        if (enc.isLower(c)) {
            enc.codeToMbc(StringSupport.toUpper(enc, c), bytes, s);
            modify = true;
        }
        for (s += StringSupport.codeLength(enc, c); s < end; s += StringSupport.codeLength(enc, c)) {
            c = StringSupport.codePoint(runtime, enc, bytes, s, end);
            if (enc.isUpper(c)) {
                enc.codeToMbc(StringSupport.toLower(enc, c), bytes, s);
                modify = true;
            }
        }
        return modify ? this : runtime.getNil();
    }
    
    public IRubyObject op_ge(final ThreadContext context, final IRubyObject other) {
        return this.op_ge19(context, other);
    }
    
    @JRubyMethod(name = { ">=" })
    public IRubyObject op_ge19(final ThreadContext context, final IRubyObject other) {
        if (other instanceof RubyString && this.cmpIsBuiltin(context)) {
            return context.runtime.newBoolean(this.op_cmp((RubyString)other) >= 0);
        }
        return RubyComparable.op_ge(context, this, other);
    }
    
    public IRubyObject op_gt(final ThreadContext context, final IRubyObject other) {
        return this.op_gt19(context, other);
    }
    
    @JRubyMethod(name = { ">" })
    public IRubyObject op_gt19(final ThreadContext context, final IRubyObject other) {
        if (other instanceof RubyString && this.cmpIsBuiltin(context)) {
            return context.runtime.newBoolean(this.op_cmp((RubyString)other) > 0);
        }
        return RubyComparable.op_gt(context, this, other);
    }
    
    public IRubyObject op_le(final ThreadContext context, final IRubyObject other) {
        return this.op_le19(context, other);
    }
    
    @JRubyMethod(name = { "<=" })
    public IRubyObject op_le19(final ThreadContext context, final IRubyObject other) {
        if (other instanceof RubyString && this.cmpIsBuiltin(context)) {
            return context.runtime.newBoolean(this.op_cmp((RubyString)other) <= 0);
        }
        return RubyComparable.op_le(context, this, other);
    }
    
    public IRubyObject op_lt(final ThreadContext context, final IRubyObject other) {
        return this.op_lt19(context, other);
    }
    
    @JRubyMethod(name = { "<" })
    public IRubyObject op_lt19(final ThreadContext context, final IRubyObject other) {
        if (other instanceof RubyString && this.cmpIsBuiltin(context)) {
            return context.runtime.newBoolean(this.op_cmp((RubyString)other) < 0);
        }
        return RubyComparable.op_lt(context, sites(context).cmp, this, other);
    }
    
    private boolean cmpIsBuiltin(final ThreadContext context) {
        return sites(context).cmp.isBuiltin(this.metaClass);
    }
    
    public IRubyObject str_eql_p(final ThreadContext context, final IRubyObject other) {
        return this.str_eql_p19(context, other);
    }
    
    @JRubyMethod(name = { "eql?" })
    public IRubyObject str_eql_p19(final ThreadContext context, final IRubyObject other) {
        final Ruby runtime = context.runtime;
        if (other instanceof RubyString) {
            final RubyString otherString = (RubyString)other;
            if (StringSupport.areComparable(this, otherString) && this.value.equal(otherString.value)) {
                return runtime.getTrue();
            }
        }
        return runtime.getFalse();
    }
    
    @Deprecated
    public RubyString upcase(final ThreadContext context) {
        return this.upcase19(context);
    }
    
    @Deprecated
    public IRubyObject upcase_bang(final ThreadContext context) {
        return this.upcase_bang19(context);
    }
    
    @JRubyMethod(name = { "upcase" })
    public RubyString upcase19(final ThreadContext context) {
        final RubyString str = this.strDup(context.runtime);
        str.upcase_bang19(context);
        return str;
    }
    
    @JRubyMethod(name = { "upcase!" })
    public IRubyObject upcase_bang19(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        final Encoding enc = this.checkDummyEncoding();
        if (this.value.getRealSize() == 0) {
            this.modifyCheck();
            return runtime.getNil();
        }
        this.modifyAndKeepCodeRange();
        final int s = this.value.getBegin();
        final int end = s + this.value.getRealSize();
        final byte[] bytes = this.value.getUnsafeBytes();
        if (this.singleByteOptimizable(enc)) {
            return this.singleByteUpcase(runtime, bytes, s, end);
        }
        return this.multiByteUpcase(runtime, enc, bytes, s, end);
    }
    
    private IRubyObject singleByteUpcase(final Ruby runtime, final byte[] bytes, final int s, final int end) {
        final boolean modify = StringSupport.singleByteUpcase(bytes, s, end);
        return modify ? this : runtime.getNil();
    }
    
    private IRubyObject multiByteUpcase(final Ruby runtime, final Encoding enc, final byte[] bytes, final int s, final int end) {
        try {
            final boolean modify = StringSupport.multiByteUpcase(enc, bytes, s, end);
            return modify ? this : runtime.getNil();
        }
        catch (IllegalArgumentException e) {
            throw runtime.newArgumentError(e.getMessage());
        }
    }
    
    @Deprecated
    public RubyString downcase(final ThreadContext context) {
        return this.downcase19(context);
    }
    
    @Deprecated
    public IRubyObject downcase_bang(final ThreadContext context) {
        return this.downcase_bang19(context);
    }
    
    @JRubyMethod(name = { "downcase" })
    public RubyString downcase19(final ThreadContext context) {
        final RubyString str = this.strDup(context.runtime);
        str.downcase_bang19(context);
        return str;
    }
    
    @JRubyMethod(name = { "downcase!" })
    public IRubyObject downcase_bang19(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        final Encoding enc = this.checkDummyEncoding();
        if (this.value.getRealSize() == 0) {
            this.modifyCheck();
            return runtime.getNil();
        }
        this.modifyAndKeepCodeRange();
        final int s = this.value.getBegin();
        final int end = s + this.value.getRealSize();
        final byte[] bytes = this.value.getUnsafeBytes();
        if (this.singleByteOptimizable(enc)) {
            return this.singleByteDowncase(runtime, bytes, s, end);
        }
        return this.multiByteDowncase(runtime, enc, bytes, s, end);
    }
    
    private IRubyObject singleByteDowncase(final Ruby runtime, final byte[] bytes, final int s, final int end) {
        final boolean modify = StringSupport.singleByteDowncase(bytes, s, end);
        return modify ? this : runtime.getNil();
    }
    
    private IRubyObject multiByteDowncase(final Ruby runtime, final Encoding enc, final byte[] bytes, final int s, final int end) {
        try {
            final boolean modify = StringSupport.multiByteDowncase(enc, bytes, s, end);
            return modify ? this : runtime.getNil();
        }
        catch (IllegalArgumentException e) {
            throw runtime.newArgumentError(e.getMessage());
        }
    }
    
    public RubyString swapcase(final ThreadContext context) {
        return this.swapcase19(context);
    }
    
    public IRubyObject swapcase_bang(final ThreadContext context) {
        return this.swapcase_bang19(context);
    }
    
    @JRubyMethod(name = { "swapcase" })
    public RubyString swapcase19(final ThreadContext context) {
        final RubyString str = this.strDup(context.runtime);
        str.swapcase_bang19(context);
        return str;
    }
    
    @JRubyMethod(name = { "swapcase!" })
    public IRubyObject swapcase_bang19(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        final Encoding enc = this.checkDummyEncoding();
        if (this.value.getRealSize() == 0) {
            this.modifyCheck();
            return runtime.getNil();
        }
        this.modifyAndKeepCodeRange();
        final int s = this.value.getBegin();
        final int end = s + this.value.getRealSize();
        final byte[] bytes = this.value.getUnsafeBytes();
        if (this.singleByteOptimizable(enc)) {
            if (StringSupport.singleByteSwapcase(bytes, s, end)) {
                return this;
            }
        }
        else if (StringSupport.multiByteSwapcase(runtime, enc, bytes, s, end)) {
            return this;
        }
        return runtime.getNil();
    }
    
    public IRubyObject dump() {
        return this.dump19();
    }
    
    @JRubyMethod(name = { "dump" })
    public IRubyObject dump19() {
        final ByteList outBytes = StringSupport.dumpCommon(this.getRuntime(), this.value);
        final RubyString result = new RubyString(this.getRuntime(), this.getMetaClass(), outBytes);
        Encoding enc = this.value.getEncoding();
        if (!enc.isAsciiCompatible()) {
            result.cat(".force_encoding(\"".getBytes());
            result.cat(enc.getName());
            result.cat((byte)34).cat((byte)41);
            enc = (Encoding)RubyString.ASCII;
        }
        result.associateEncoding(enc);
        result.setCodeRange(16);
        return result.infectBy(this);
    }
    
    public IRubyObject insert(final ThreadContext context, final IRubyObject indexArg, final IRubyObject stringArg) {
        return this.insert19(context, indexArg, stringArg);
    }
    
    @JRubyMethod(name = { "insert" })
    public IRubyObject insert19(final ThreadContext context, final IRubyObject indexArg, final IRubyObject stringArg) {
        final RubyString str = stringArg.convertToString();
        int index = RubyNumeric.num2int(indexArg);
        if (index == -1) {
            return this.append19(stringArg);
        }
        if (index < 0) {
            ++index;
        }
        this.replaceInternal19(index, 0, str);
        return this;
    }
    
    private int checkIndex(int beg, final int len) {
        if (beg > len) {
            this.raiseIndexOutOfString(beg);
        }
        if (beg < 0) {
            if (-beg > len) {
                this.raiseIndexOutOfString(beg);
            }
            beg += len;
        }
        return beg;
    }
    
    private int checkIndexForRef(int beg, final int len) {
        if (beg >= len) {
            this.raiseIndexOutOfString(beg);
        }
        if (beg < 0) {
            if (-beg > len) {
                this.raiseIndexOutOfString(beg);
            }
            beg += len;
        }
        return beg;
    }
    
    private int checkLength(final int len) {
        if (len < 0) {
            throw this.getRuntime().newIndexError("negative length " + len);
        }
        return len;
    }
    
    private void raiseIndexOutOfString(final int index) {
        throw this.getRuntime().newIndexError("index " + index + " out of string");
    }
    
    @JRubyMethod(name = { "inspect" })
    @Override
    public IRubyObject inspect() {
        return this.inspect(this.getRuntime());
    }
    
    final RubyString inspect(final Ruby runtime) {
        return (RubyString)inspect(runtime, this.value).infectBy(this);
    }
    
    @Deprecated
    public IRubyObject inspect19() {
        return this.inspect();
    }
    
    public static IRubyObject rbStrEscape(final ThreadContext context, final RubyString str) {
        final Ruby runtime = context.runtime;
        final Encoding enc = str.getEncoding();
        final ByteList strBL = str.getByteList();
        final byte[] pBytes = strBL.unsafeBytes();
        int p = strBL.begin();
        final int pend = p + strBL.realSize();
        int prev = p;
        final RubyString result = newEmptyString(runtime);
        final boolean unicode_p = enc.isUnicode();
        final boolean asciicompat = enc.isAsciiCompatible();
        while (p < pend) {
            int n = enc.length(pBytes, p, pend);
            if (!StringSupport.MBCLEN_CHARFOUND_P(n)) {
                if (p > prev) {
                    result.cat(pBytes, prev, p - prev);
                }
                n = enc.minLength();
                if (pend < p + n) {
                    n = pend - p;
                }
                while (n-- > 0) {
                    result.modify();
                    Sprintf.sprintf(runtime, result.getByteList(), "\\x%02X", pBytes[p] & 0xFF);
                    prev = ++p;
                }
            }
            else {
                n = StringSupport.MBCLEN_CHARFOUND_LEN(n);
                final int c = enc.mbcToCode(pBytes, p, pend);
                p += n;
                int cc = 0;
                switch (c) {
                    case 10: {
                        cc = 110;
                        break;
                    }
                    case 13: {
                        cc = 114;
                        break;
                    }
                    case 9: {
                        cc = 116;
                        break;
                    }
                    case 12: {
                        cc = 102;
                        break;
                    }
                    case 11: {
                        cc = 118;
                        break;
                    }
                    case 8: {
                        cc = 98;
                        break;
                    }
                    case 7: {
                        cc = 97;
                        break;
                    }
                    case 27: {
                        cc = 101;
                        break;
                    }
                    default: {
                        cc = 0;
                        break;
                    }
                }
                if (cc != 0) {
                    if (p - n > prev) {
                        result.cat(pBytes, prev, p - n - prev);
                    }
                    result.cat(92);
                    result.cat((byte)cc);
                    prev = p;
                }
                else {
                    if (asciicompat && Encoding.isAscii(c) && c < 127 && c > 31) {
                        continue;
                    }
                    if (p - n > prev) {
                        result.cat(pBytes, prev, p - n - prev);
                    }
                    result.modify();
                    Sprintf.sprintf(runtime, result.getByteList(), StringSupport.escapedCharFormat(c, unicode_p), (long)c & 0xFFFFFFFFL);
                    prev = p;
                }
            }
        }
        if (p > prev) {
            result.cat(pBytes, prev, p - prev);
        }
        result.setEncodingAndCodeRange((Encoding)USASCIIEncoding.INSTANCE, 16);
        result.infectBy(str);
        return result;
    }
    
    @Deprecated
    public static IRubyObject inspect19(final Ruby runtime, final ByteList byteList) {
        return inspect(runtime, byteList);
    }
    
    public static RubyString inspect(final Ruby runtime, final ByteList byteList) {
        Encoding enc = byteList.getEncoding();
        final byte[] bytes = byteList.getUnsafeBytes();
        int p = byteList.getBegin();
        final int end = p + byteList.getRealSize();
        final RubyString result = new RubyString(runtime, runtime.getString(), new ByteList(end - p));
        Encoding resultEnc = runtime.getDefaultInternalEncoding();
        boolean isUnicode = StringSupport.isUnicode(enc);
        final boolean asciiCompat = enc.isAsciiCompatible();
        if (resultEnc == null) {
            resultEnc = runtime.getDefaultExternalEncoding();
        }
        if (!resultEnc.isAsciiCompatible()) {
            resultEnc = (Encoding)USASCIIEncoding.INSTANCE;
        }
        result.associateEncoding(resultEnc);
        result.cat(34);
        int prev = p;
        final Encoding actEnc = EncodingUtils.getActualEncoding(enc, byteList);
        if (actEnc != enc) {
            enc = actEnc;
            if (isUnicode) {
                isUnicode = (enc instanceof UnicodeEncoding);
            }
        }
        while (p < end) {
            int n = StringSupport.preciseLength(enc, bytes, p, end);
            if (!StringSupport.MBCLEN_CHARFOUND_P(n)) {
                if (p > prev) {
                    result.cat(bytes, prev, p - prev);
                }
                n = enc.minLength();
                if (end < p + n) {
                    n = end - p;
                }
                while (n-- > 0) {
                    result.modifyExpand(result.size() + 4);
                    Sprintf.sprintf(runtime, result.getByteList(), "\\x%02X", bytes[p] & 0xFF);
                    prev = ++p;
                }
            }
            else {
                n = StringSupport.MBCLEN_CHARFOUND_LEN(n);
                final int c = enc.mbcToCode(bytes, p, end);
                int cc = 0;
                p += n;
                if ((asciiCompat || isUnicode) && (c == 34 || c == 92 || (c == 35 && p < end && StringSupport.MBCLEN_CHARFOUND_P(StringSupport.preciseLength(enc, bytes, p, end)) && ((cc = StringSupport.codePoint(runtime, enc, bytes, p, end)) == 36 || cc == 64 || cc == 123)))) {
                    if (p - n > prev) {
                        result.cat(bytes, prev, p - n - prev);
                    }
                    result.cat(92);
                    if (asciiCompat || enc == resultEnc) {
                        prev = p - n;
                        continue;
                    }
                }
                switch (c) {
                    case 10: {
                        cc = 110;
                        break;
                    }
                    case 13: {
                        cc = 114;
                        break;
                    }
                    case 9: {
                        cc = 116;
                        break;
                    }
                    case 12: {
                        cc = 102;
                        break;
                    }
                    case 11: {
                        cc = 118;
                        break;
                    }
                    case 8: {
                        cc = 98;
                        break;
                    }
                    case 7: {
                        cc = 97;
                        break;
                    }
                    case 27: {
                        cc = 101;
                        break;
                    }
                    default: {
                        cc = 0;
                        break;
                    }
                }
                if (cc != 0) {
                    if (p - n > prev) {
                        result.cat(bytes, prev, p - n - prev);
                    }
                    result.cat(92);
                    result.cat(cc);
                    prev = p;
                }
                else {
                    if (enc == resultEnc && enc.isPrint(c)) {
                        continue;
                    }
                    if (asciiCompat && c < 128 && c > 0 && enc.isPrint(c)) {
                        continue;
                    }
                    if (p - n > prev) {
                        result.cat(bytes, prev, p - n - prev);
                    }
                    Sprintf.sprintf(runtime, result.getByteList(), StringSupport.escapedCharFormat(c, isUnicode), (long)c & 0xFFFFFFFFL);
                    prev = p;
                }
            }
        }
        if (p > prev) {
            result.cat(bytes, prev, p - prev);
        }
        result.cat(34);
        return result;
    }
    
    public int size() {
        return this.value.getRealSize();
    }
    
    public RubyFixnum length() {
        return this.length19();
    }
    
    @JRubyMethod(name = { "length", "size" })
    public RubyFixnum length19() {
        return this.getRuntime().newFixnum(this.strLength());
    }
    
    @JRubyMethod(name = { "bytesize" })
    public RubyFixnum bytesize() {
        return this.getRuntime().newFixnum(this.value.getRealSize());
    }
    
    private RubyEnumerator.SizeFn eachByteSizeFn() {
        final RubyString self = this;
        return new RubyEnumerator.SizeFn() {
            @Override
            public IRubyObject size(final IRubyObject[] args) {
                return self.bytesize();
            }
        };
    }
    
    @JRubyMethod(name = { "empty?" })
    public RubyBoolean empty_p(final ThreadContext context) {
        return this.isEmpty() ? context.runtime.getTrue() : context.runtime.getFalse();
    }
    
    public boolean isEmpty() {
        return this.value.length() == 0;
    }
    
    public RubyString append(final IRubyObject other) {
        if (other instanceof RubyFixnum) {
            this.cat(ConvertBytes.longToByteList(((RubyFixnum)other).getLongValue()));
            return this;
        }
        if (other instanceof RubyFloat) {
            return this.cat((RubyString)((RubyFloat)other).to_s());
        }
        if (other instanceof RubySymbol) {
            this.cat(((RubySymbol)other).getBytes());
            return this;
        }
        final RubyString otherStr = other.convertToString();
        this.infectBy(otherStr);
        return this.cat(otherStr.value);
    }
    
    public RubyString append19(final IRubyObject other) {
        if (other instanceof RubyFixnum) {
            this.cat19(ConvertBytes.longToByteList(((RubyFixnum)other).getLongValue()), 16);
            return this;
        }
        if (other instanceof RubyFloat) {
            return this.cat19((RubyString)((RubyFloat)other).to_s());
        }
        if (other instanceof RubySymbol) {
            this.cat19(((RubySymbol)other).getBytes(), 0);
            return this;
        }
        return this.cat19(other.convertToString());
    }
    
    public RubyString concat(final IRubyObject other) {
        return this.concat19(this.getRuntime().getCurrentContext(), other);
    }
    
    @JRubyMethod(name = { "concat", "<<" })
    public RubyString concat19(final ThreadContext context, final IRubyObject other) {
        final Ruby runtime = context.runtime;
        if (other instanceof RubyFixnum) {
            final long c = RubyNumeric.num2long(other);
            if (c < 0L) {
                throw runtime.newRangeError(c + " out of char range");
            }
            return this.concatNumeric(runtime, (int)(c & -1L));
        }
        else if (other instanceof RubyBignum) {
            if (((RubyBignum)other).getBigIntegerValue().signum() < 0) {
                throw runtime.newRangeError("negative string size (or size too big)");
            }
            final long c = ((RubyBignum)other).getLongValue();
            return this.concatNumeric(runtime, (int)c);
        }
        else {
            if (other instanceof RubySymbol) {
                throw runtime.newTypeError("can't convert Symbol into String");
            }
            return this.append19(other);
        }
    }
    
    private RubyString concatNumeric(final Ruby runtime, final int c) {
        Encoding enc = this.value.getEncoding();
        int cl;
        try {
            cl = StringSupport.codeLength(enc, c);
            if (cl <= 0) {
                throw runtime.newRangeError(c + " out of char range or invalid code point");
            }
            this.modify19(this.value.getRealSize() + cl);
            if (enc == USASCIIEncoding.INSTANCE) {
                if (c > 255) {
                    throw runtime.newRangeError(c + " out of char range");
                }
                if (c > 121) {
                    this.value.setEncoding((Encoding)ASCIIEncoding.INSTANCE);
                    enc = this.value.getEncoding();
                }
            }
            enc.codeToMbc(c, this.value.getUnsafeBytes(), this.value.getBegin() + this.value.getRealSize());
        }
        catch (EncodingException e) {
            throw runtime.newRangeError(c + " out of char range");
        }
        this.value.setRealSize(this.value.getRealSize() + cl);
        return this;
    }
    
    @JRubyMethod
    public IRubyObject prepend(final ThreadContext context, final IRubyObject other) {
        return this.replace19(other.convertToString().op_plus19(context, this));
    }
    
    @JRubyMethod(name = { "crypt" })
    public RubyString crypt(final ThreadContext context, final IRubyObject other) {
        final Encoding ascii8bit = context.runtime.getEncodingService().getAscii8bitEncoding();
        final RubyString otherStr = other.convertToString().strDup(context.runtime);
        otherStr.modify();
        otherStr.associateEncoding(ascii8bit);
        final ByteList otherBL = otherStr.getByteList();
        if (otherBL.length() < 2) {
            throw context.runtime.newArgumentError("salt too short (need >=2 bytes)");
        }
        final POSIX posix = context.runtime.getPosix();
        final byte[] keyBytes = Arrays.copyOfRange(this.value.unsafeBytes(), this.value.begin(), this.value.begin() + this.value.realSize());
        final byte[] saltBytes = Arrays.copyOfRange(otherBL.unsafeBytes(), otherBL.begin(), otherBL.begin() + otherBL.realSize());
        if (saltBytes[0] == 0 || saltBytes[1] == 0) {
            throw context.runtime.newArgumentError("salt too short (need >=2 bytes)");
        }
        final byte[] cryptedString = posix.crypt(keyBytes, saltBytes);
        if (cryptedString == null) {
            throw context.runtime.newErrnoFromInt(posix.errno());
        }
        final RubyString result = newStringNoCopy(context.runtime, cryptedString, 0, cryptedString.length - 1);
        result.associateEncoding(ascii8bit);
        result.infectBy(this);
        result.infectBy(otherStr);
        return result;
    }
    
    public static RubyString stringValue(final IRubyObject object) {
        return (RubyString)((object instanceof RubyString) ? object : object.convertToString());
    }
    
    @Deprecated
    public IRubyObject sub19(final ThreadContext context, final IRubyObject arg0, final Block block) {
        return this.sub(context, arg0, block);
    }
    
    @Deprecated
    public IRubyObject sub19(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1, final Block block) {
        return this.sub(context, arg0, arg1, block);
    }
    
    @Deprecated
    public IRubyObject sub_bang19(final ThreadContext context, final IRubyObject arg0, final Block block) {
        return this.sub_bang(context, arg0, block);
    }
    
    @Deprecated
    public IRubyObject sub_bang19(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1, final Block block) {
        return this.sub_bang(context, arg0, arg1, block);
    }
    
    @JRubyMethod(name = { "sub" }, reads = { FrameField.BACKREF }, writes = { FrameField.BACKREF })
    public IRubyObject sub(final ThreadContext context, final IRubyObject arg0, final Block block) {
        final RubyString str = this.strDup(context.runtime);
        str.sub_bang(context, arg0, block);
        return str;
    }
    
    @JRubyMethod(name = { "sub" }, reads = { FrameField.BACKREF }, writes = { FrameField.BACKREF })
    public IRubyObject sub(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1, final Block block) {
        final RubyString str = this.strDup(context.runtime);
        str.sub_bang(context, arg0, arg1, block);
        return str;
    }
    
    @JRubyMethod(name = { "sub!" }, reads = { FrameField.BACKREF }, writes = { FrameField.BACKREF })
    public IRubyObject sub_bang(final ThreadContext context, final IRubyObject arg0, final Block block) {
        final Ruby runtime = context.runtime;
        this.frozenCheck();
        if (block.isGiven()) {
            return this.subBangIter(runtime, context, arg0, null, block);
        }
        throw runtime.newArgumentError(1, 2);
    }
    
    @JRubyMethod(name = { "sub!" }, reads = { FrameField.BACKREF }, writes = { FrameField.BACKREF })
    public IRubyObject sub_bang(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1, final Block block) {
        final Ruby runtime = context.runtime;
        final IRubyObject hash = TypeConverter.convertToTypeWithCheck(context, arg1, runtime.getHash(), sites(context).to_hash_checked);
        this.frozenCheck();
        if (hash.isNil()) {
            return this.subBangNoIter(runtime, context, arg0, arg1.convertToString());
        }
        return this.subBangIter(runtime, context, arg0, (RubyHash)hash, block);
    }
    
    private RubyRegexp asRegexpArg(final Ruby runtime, final IRubyObject arg0) {
        return (RubyRegexp)((arg0 instanceof RubyRegexp) ? arg0 : RubyRegexp.newRegexp(runtime, RubyRegexp.quote19(this.getStringForPattern(arg0).getByteList(), false), new RegexpOptions()));
    }
    
    private IRubyObject subBangIter(final Ruby runtime, final ThreadContext context, final IRubyObject arg0, final RubyHash hash, final Block block) {
        final RubyRegexp regexp = this.asRegexpArg(runtime, arg0);
        final Regex pattern = regexp.getPattern();
        final Regex prepared = regexp.preparePattern(this);
        final int begin = this.value.getBegin();
        final int len = this.value.getRealSize();
        final int range = begin + len;
        final byte[] bytes = this.value.getUnsafeBytes();
        final Encoding enc = this.value.getEncoding();
        final Matcher matcher = prepared.matcher(bytes, begin, range);
        if (RubyRegexp.matcherSearch(runtime, matcher, begin, range, 0) >= 0) {
            final RubyMatchData match = RubyRegexp.createMatchData19(context, this, matcher, pattern);
            match.regexp = regexp;
            context.setBackRef(match);
            final int mBeg = matcher.getBegin();
            final int mEnd = matcher.getEnd();
            final IRubyObject subStr = this.makeShared19(runtime, mBeg, mEnd - mBeg);
            int tuFlags;
            RubyString repl;
            if (hash == null) {
                tuFlags = 0;
                repl = objAsString(context, block.yield(context, subStr));
            }
            else {
                tuFlags = hash.flags;
                repl = objAsString(context, hash.op_aref(context, subStr));
            }
            this.modifyCheck(bytes, len, enc);
            return this.subBangCommon(context, mBeg, mEnd, repl, tuFlags | repl.flags);
        }
        return context.setBackRef(runtime.getNil());
    }
    
    private IRubyObject subBangNoIter(final Ruby runtime, final ThreadContext context, final IRubyObject arg0, RubyString repl) {
        final RubyRegexp regexp = this.asRegexpArg(runtime, arg0);
        final Regex pattern = regexp.getPattern();
        final Regex prepared = regexp.preparePattern(this);
        final int begin = this.value.getBegin();
        final int range = begin + this.value.getRealSize();
        final Matcher matcher = prepared.matcher(this.value.getUnsafeBytes(), begin, range);
        if (RubyRegexp.matcherSearch(runtime, matcher, begin, range, 0) >= 0) {
            repl = RubyRegexp.regsub19(context, repl, this, matcher, pattern);
            final RubyMatchData match = RubyRegexp.createMatchData19(context, this, matcher, pattern);
            match.regexp = regexp;
            context.setBackRef(match);
            return this.subBangCommon(context, matcher.getBegin(), matcher.getEnd(), repl, repl.flags);
        }
        return context.setBackRef(runtime.getNil());
    }
    
    private IRubyObject subBangCommon(final ThreadContext context, final int beg, final int end, final RubyString repl, final int tuFlags) {
        Encoding enc = StringSupport.areCompatible(this, repl);
        if (enc == null) {
            enc = this.subBangVerifyEncoding(context, repl, beg, end);
        }
        final ByteList replValue = repl.value;
        final int replSize = replValue.getRealSize();
        final int plen = end - beg;
        if (replSize > plen) {
            this.modifyExpand(this.value.getRealSize() + replSize - plen);
        }
        else {
            this.modify19();
        }
        final ByteList value = this.value;
        final int size = value.getRealSize();
        this.associateEncoding(enc);
        int cr = this.getCodeRange();
        if (cr > 0 && cr < 48) {
            final int cr2 = repl.getCodeRange();
            if (cr2 == 48 || (cr == 32 && cr2 == 16)) {
                cr = 0;
            }
            else {
                cr = cr2;
            }
        }
        if (replSize != plen) {
            final int src = value.getBegin() + beg + plen;
            final int dst = value.getBegin() + beg + replSize;
            System.arraycopy(value.getUnsafeBytes(), src, value.getUnsafeBytes(), dst, size - beg - plen);
        }
        System.arraycopy(replValue.getUnsafeBytes(), replValue.getBegin(), value.getUnsafeBytes(), value.getBegin() + beg, replSize);
        value.setRealSize(size + replSize - plen);
        this.setCodeRange(cr);
        return this.infectBy(tuFlags);
    }
    
    private Encoding subBangVerifyEncoding(final ThreadContext context, final RubyString repl, final int beg, final int end) {
        final ByteList value = this.value;
        final byte[] bytes = value.getUnsafeBytes();
        final int p = value.getBegin();
        final int len = value.getRealSize();
        final Encoding strEnc = value.getEncoding();
        if (StringSupport.codeRangeScan(strEnc, bytes, p, beg) != 16 || StringSupport.codeRangeScan(strEnc, bytes, p + end, len - end) != 16) {
            throw context.runtime.newEncodingCompatibilityError("incompatible character encodings " + strEnc + " and " + repl.value.getEncoding());
        }
        return repl.value.getEncoding();
    }
    
    public IRubyObject gsub(final ThreadContext context, final IRubyObject arg0, final Block block) {
        return this.gsub19(context, arg0, block);
    }
    
    public IRubyObject gsub(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1, final Block block) {
        return this.gsub19(context, arg0, arg1, block);
    }
    
    public IRubyObject gsub_bang(final ThreadContext context, final IRubyObject arg0, final Block block) {
        return this.gsub_bang19(context, arg0, block);
    }
    
    public IRubyObject gsub_bang(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1, final Block block) {
        return this.gsub_bang19(context, arg0, arg1, block);
    }
    
    @JRubyMethod(name = { "gsub" }, reads = { FrameField.BACKREF }, writes = { FrameField.BACKREF })
    public IRubyObject gsub19(final ThreadContext context, final IRubyObject arg0, final Block block) {
        return block.isGiven() ? this.gsubCommon19(context, block, null, null, arg0, false, 0) : RubyEnumerator.enumeratorize(context.runtime, this, "gsub", arg0);
    }
    
    @JRubyMethod(name = { "gsub" }, reads = { FrameField.BACKREF }, writes = { FrameField.BACKREF })
    public IRubyObject gsub19(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1, final Block block) {
        return this.gsub19(context, arg0, arg1, block, false);
    }
    
    @JRubyMethod(name = { "gsub!" }, reads = { FrameField.BACKREF }, writes = { FrameField.BACKREF })
    public IRubyObject gsub_bang19(final ThreadContext context, final IRubyObject arg0, final Block block) {
        this.checkFrozen();
        return block.isGiven() ? this.gsubCommon19(context, block, null, null, arg0, true, 0) : RubyEnumerator.enumeratorize(context.runtime, this, "gsub!", arg0);
    }
    
    @JRubyMethod(name = { "gsub!" }, reads = { FrameField.BACKREF }, writes = { FrameField.BACKREF })
    public IRubyObject gsub_bang19(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1, final Block block) {
        this.checkFrozen();
        return this.gsub19(context, arg0, arg1, block, true);
    }
    
    private IRubyObject gsub19(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1, final Block block, final boolean bang) {
        final Ruby runtime = context.runtime;
        final IRubyObject tryHash = TypeConverter.convertToTypeWithCheck(context, arg1, runtime.getHash(), sites(context).to_hash_checked);
        RubyHash hash;
        RubyString str;
        int tuFlags;
        if (tryHash.isNil()) {
            hash = null;
            str = arg1.convertToString();
            tuFlags = str.flags;
        }
        else {
            hash = (RubyHash)tryHash;
            str = null;
            tuFlags = (hash.flags & RubyString.TAINTED_F);
        }
        return this.gsubCommon19(context, block, str, hash, arg0, bang, tuFlags);
    }
    
    private IRubyObject gsubCommon19(final ThreadContext context, final Block block, final RubyString repl, final RubyHash hash, final IRubyObject arg0, final boolean bang, final int tuFlags) {
        return this.gsubCommon19(context, block, repl, hash, arg0, bang, tuFlags, true);
    }
    
    private IRubyObject gsubCommon19(final ThreadContext context, final Block block, final RubyString repl, final RubyHash hash, final IRubyObject arg0, final boolean bang, int tuFlags, final boolean useBackref) {
        final Ruby runtime = context.runtime;
        final RubyRegexp regexp = this.asRegexpArg(runtime, arg0);
        final Regex pattern = regexp.getPattern();
        final Regex prepared = regexp.preparePattern(this);
        final byte[] spBytes = this.value.getUnsafeBytes();
        final int spBeg = this.value.getBegin();
        final int spLen = this.value.getRealSize();
        final Matcher matcher = prepared.matcher(spBytes, spBeg, spBeg + spLen);
        int beg = RubyRegexp.matcherSearch(runtime, matcher, spBeg, spBeg + spLen, 0);
        if (beg < 0) {
            if (useBackref) {
                context.setBackRef(context.nil);
            }
            return bang ? context.nil : this.strDup(runtime);
        }
        int offset = 0;
        int cp = spBeg;
        final RubyString dest = new RubyString(runtime, this.getMetaClass(), new ByteList(spLen + 30));
        final Encoding str_enc = this.value.getEncoding();
        dest.setEncoding(str_enc);
        dest.setCodeRange(str_enc.isAsciiCompatible() ? 16 : 32);
        RubyMatchData match = null;
        do {
            final int begz = matcher.getBegin();
            final int endz = matcher.getEnd();
            RubyString val;
            if (repl != null) {
                val = RubyRegexp.regsub19(context, repl, this, matcher, pattern);
            }
            else {
                final RubyString substr = this.makeShared19(runtime, begz, endz - begz);
                if (hash != null) {
                    val = objAsString(context, hash.op_aref(context, substr));
                }
                else {
                    match = RubyRegexp.createMatchData19(context, this, matcher, pattern);
                    match.regexp = regexp;
                    if (useBackref) {
                        context.setBackRef(match);
                    }
                    val = objAsString(context, block.yield(context, substr));
                }
                this.modifyCheck(spBytes, spLen, str_enc);
                if (bang) {
                    this.frozenCheck();
                }
            }
            tuFlags |= val.flags;
            int len = beg - offset;
            if (len != 0) {
                dest.cat(spBytes, cp, len, str_enc);
            }
            dest.cat19(val);
            if (begz == (offset = endz)) {
                if (spLen <= endz) {
                    break;
                }
                len = StringSupport.encFastMBCLen(spBytes, spBeg + endz, spBeg + spLen, str_enc);
                dest.cat(spBytes, spBeg + endz, len, str_enc);
                offset = endz + len;
            }
            cp = spBeg + offset;
            if (offset > spLen) {
                break;
            }
            beg = RubyRegexp.matcherSearch(runtime, matcher, cp, spBeg + spLen, 0);
        } while (beg >= 0);
        if (spLen > offset) {
            dest.cat(spBytes, cp, spLen - offset, str_enc);
        }
        if (match != null) {
            if (useBackref) {
                context.setBackRef(match);
            }
        }
        else {
            match = RubyRegexp.createMatchData19(context, this, matcher, pattern);
            match.regexp = regexp;
            if (useBackref) {
                context.setBackRef(match);
            }
        }
        if (bang) {
            this.view(dest.value);
            this.setCodeRange(dest.getCodeRange());
            return this.infectBy(tuFlags);
        }
        return dest.infectBy(tuFlags | this.flags);
    }
    
    public IRubyObject index(final ThreadContext context, final IRubyObject arg0) {
        return this.index19(context, arg0);
    }
    
    public IRubyObject index(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1) {
        return this.index19(context, arg0, arg1);
    }
    
    @JRubyMethod(name = { "index" }, reads = { FrameField.BACKREF }, writes = { FrameField.BACKREF })
    public IRubyObject index19(final ThreadContext context, final IRubyObject arg0) {
        return this.indexCommon19(context.runtime, context, arg0, 0);
    }
    
    @JRubyMethod(name = { "index" }, reads = { FrameField.BACKREF }, writes = { FrameField.BACKREF })
    public IRubyObject index19(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1) {
        int pos = RubyNumeric.num2int(arg1);
        final Ruby runtime = context.runtime;
        if (pos < 0) {
            pos += this.strLength();
            if (pos < 0) {
                if (arg0 instanceof RubyRegexp) {
                    context.setBackRef(runtime.getNil());
                }
                return runtime.getNil();
            }
        }
        return this.indexCommon19(runtime, context, arg0, pos);
    }
    
    private IRubyObject indexCommon19(final Ruby runtime, final ThreadContext context, final IRubyObject sub, int pos) {
        if (sub instanceof RubyRegexp) {
            if (pos > this.strLength()) {
                return context.nil;
            }
            final RubyRegexp regSub = (RubyRegexp)sub;
            pos = (this.singleByteOptimizable() ? pos : (StringSupport.nth(this.checkEncoding(regSub), this.value.getUnsafeBytes(), this.value.getBegin(), this.value.getBegin() + this.value.getRealSize(), pos) - this.value.getBegin()));
            pos = regSub.adjustStartPos(this, pos, false);
            pos = regSub.search19(context, this, pos, false);
            pos = this.subLength(pos);
        }
        else if (sub instanceof RubyString) {
            pos = StringSupport.index(this, (CodeRangeable)sub, pos, this.checkEncoding((RubyString)sub));
            pos = this.subLength(pos);
        }
        else {
            final IRubyObject tmp = sub.checkStringType();
            if (tmp.isNil()) {
                throw runtime.newTypeError("type mismatch: " + sub.getMetaClass().getName() + " given");
            }
            pos = StringSupport.index(this, (CodeRangeable)tmp, pos, this.checkEncoding((RubyString)tmp));
            pos = this.subLength(pos);
        }
        return (pos == -1) ? runtime.getNil() : RubyFixnum.newFixnum(runtime, pos);
    }
    
    private int strseqIndex(final RubyString sub, int offset, final boolean inBytes) {
        final byte[] sBytes = this.value.unsafeBytes();
        final boolean single_byte = this.singleByteOptimizable();
        final Encoding enc = this.checkEncoding(sub);
        if (sub.isCodeRangeBroken()) {
            return -1;
        }
        int len = (inBytes || single_byte) ? this.value.realSize() : this.strLength();
        int slen = inBytes ? sub.value.realSize() : sub.strLength();
        if (offset < 0) {
            offset += len;
            if (offset < 0) {
                return -1;
            }
        }
        if (len - offset < slen) {
            return -1;
        }
        int s = this.value.begin();
        final int e = s + this.value.realSize();
        if (offset != 0) {
            if (!inBytes) {
                offset = StringSupport.offset(enc, sBytes, s, e, offset, single_byte);
            }
            s += offset;
        }
        if (slen == 0) {
            return offset;
        }
        final byte[] sptrBytes = sub.value.unsafeBytes();
        final int sptr = sub.value.begin();
        slen = sub.value.realSize();
        len = this.value.realSize() - offset;
        while (true) {
            final int pos = StringSupport.memsearch(sptrBytes, sptr, slen, sBytes, s, len, enc);
            if (pos < 0) {
                return pos;
            }
            final int t = enc.rightAdjustCharHead(sBytes, s, s + pos, e);
            if (t == s + pos) {
                return pos + offset;
            }
            len -= t - s;
            if (len <= 0) {
                return -1;
            }
            offset += t - s;
            s = t;
        }
    }
    
    public IRubyObject rindex(final ThreadContext context, final IRubyObject arg0) {
        return this.rindex19(context, arg0);
    }
    
    public IRubyObject rindex(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1) {
        return this.rindex19(context, arg0, arg1);
    }
    
    @JRubyMethod(name = { "rindex" }, reads = { FrameField.BACKREF }, writes = { FrameField.BACKREF })
    public IRubyObject rindex19(final ThreadContext context, final IRubyObject arg0) {
        return this.rindexCommon19(context.runtime, context, arg0, this.strLength());
    }
    
    @JRubyMethod(name = { "rindex" }, reads = { FrameField.BACKREF }, writes = { FrameField.BACKREF })
    public IRubyObject rindex19(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1) {
        int pos = RubyNumeric.num2int(arg1);
        final Ruby runtime = context.runtime;
        final int length = this.strLength();
        if (pos < 0) {
            pos += length;
            if (pos < 0) {
                if (arg0 instanceof RubyRegexp) {
                    context.setBackRef(runtime.getNil());
                }
                return runtime.getNil();
            }
        }
        if (pos > length) {
            pos = length;
        }
        return this.rindexCommon19(runtime, context, arg0, pos);
    }
    
    private IRubyObject rindexCommon19(final Ruby runtime, final ThreadContext context, final IRubyObject sub, int pos) {
        if (sub instanceof RubyRegexp) {
            final RubyRegexp regSub = (RubyRegexp)sub;
            pos = StringSupport.offset(this.value.getEncoding(), this.value.getUnsafeBytes(), this.value.getBegin(), this.value.getBegin() + this.value.getRealSize(), pos, this.singleByteOptimizable());
            if (regSub.length() > 0) {
                pos = regSub.search19(context, this, pos, true);
                pos = this.subLength(pos);
            }
        }
        else if (sub instanceof RubyString) {
            final Encoding enc = this.checkEncoding((RubyString)sub);
            pos = StringSupport.rindex(this.value, StringSupport.strLengthFromRubyString(this, enc), StringSupport.strLengthFromRubyString((CodeRangeable)sub, enc), pos, (CodeRangeable)sub, this.checkEncoding((RubyString)sub));
        }
        else {
            final IRubyObject tmp = sub.checkStringType();
            if (tmp.isNil()) {
                throw runtime.newTypeError("type mismatch: " + sub.getMetaClass().getName() + " given");
            }
            pos = StringSupport.rindex(this.value, StringSupport.strLengthFromRubyString(this, this.checkEncoding((RubyString)tmp)), StringSupport.strLengthFromRubyString((CodeRangeable)tmp, this.checkEncoding((RubyString)tmp)), pos, (CodeRangeable)tmp, this.checkEncoding((RubyString)tmp));
        }
        if (pos >= 0) {
            return RubyFixnum.newFixnum(runtime, pos);
        }
        return runtime.getNil();
    }
    
    @Deprecated
    public final IRubyObject substr(final int beg, final int len) {
        return this.substr(this.getRuntime(), beg, len);
    }
    
    public final IRubyObject substr(final Ruby runtime, int beg, final int len) {
        final int length = this.value.length();
        if (len < 0 || beg > length) {
            return runtime.getNil();
        }
        if (beg < 0) {
            beg += length;
            if (beg < 0) {
                return runtime.getNil();
            }
        }
        final int end = Math.min(length, beg + len);
        return this.makeShared19(runtime, beg, end - beg);
    }
    
    private IRubyObject byteSubstr(final Ruby runtime, int beg, int len) {
        final int length = this.value.length();
        if (len < 0 || beg > length) {
            return runtime.getNil();
        }
        if (beg < 0) {
            beg += length;
            if (beg < 0) {
                return runtime.getNil();
            }
        }
        if (beg + len > length) {
            len = length - beg;
        }
        if (len <= 0) {
            len = 0;
        }
        return this.makeShared19(runtime, beg, len);
    }
    
    private IRubyObject byteARef(final Ruby runtime, final IRubyObject idx) {
        if (idx instanceof RubyRange) {
            final int[] begLen = ((RubyRange)idx).begLenInt(this.getByteList().length(), 0);
            return (begLen == null) ? runtime.getNil() : this.byteSubstr(runtime, begLen[0], begLen[1]);
        }
        int index;
        if (idx instanceof RubyFixnum) {
            index = RubyNumeric.fix2int((RubyFixnum)idx);
        }
        else {
            final ThreadContext context = runtime.getCurrentContext();
            final JavaSites.StringSites sites = sites(context);
            if (RubyRange.isRangeLike(context, idx, sites.respond_to_begin, sites.respond_to_end)) {
                final RubyRange range = RubyRange.rangeFromRangeLike(context, idx, sites.begin, sites.end, sites.exclude_end);
                final int[] begLen2 = range.begLenInt(this.getByteList().length(), 0);
                return (begLen2 == null) ? runtime.getNil() : this.byteSubstr(runtime, begLen2[0], begLen2[1]);
            }
            index = RubyNumeric.num2int(idx);
        }
        final IRubyObject obj = this.byteSubstr(runtime, index, 1);
        if (obj.isNil() || ((RubyString)obj).getByteList().length() == 0) {
            return runtime.getNil();
        }
        return obj;
    }
    
    public final IRubyObject substr19(final Ruby runtime, int beg, int len) {
        if (len < 0) {
            return runtime.getNil();
        }
        final int length = this.value.getRealSize();
        if (length == 0) {
            len = 0;
        }
        final Encoding enc = this.value.getEncoding();
        if (!this.singleByteOptimizable(enc)) {
            if (beg + len > length) {
                len = length - beg;
            }
            return this.multibyteSubstr19(runtime, enc, len, beg, length);
        }
        if (beg > length) {
            return runtime.getNil();
        }
        if (beg < 0) {
            beg += length;
            if (beg < 0) {
                return runtime.getNil();
            }
        }
        if (beg + len > length) {
            len = length - beg;
        }
        if (len <= 0) {
            beg = (len = 0);
        }
        return this.makeShared19(runtime, beg, len);
    }
    
    private IRubyObject multibyteSubstr19(final Ruby runtime, final Encoding enc, int len, int beg, final int length) {
        final int s = this.value.getBegin();
        final int end = s + length;
        final byte[] bytes = this.value.getUnsafeBytes();
        if (beg < 0) {
            if (len > -beg) {
                len = -beg;
            }
            if (-beg * enc.maxLength() < length >>> 3) {
                beg = -beg;
                int e = end;
                while (beg-- > len && (e = enc.prevCharHead(bytes, s, e, e)) != -1) {}
                int p = e;
                if (p == -1) {
                    return runtime.getNil();
                }
                while (len-- > 0 && (p = enc.prevCharHead(bytes, s, p, e)) != -1) {}
                if (p == -1) {
                    return runtime.getNil();
                }
                return this.makeShared19(runtime, p - s, e - p);
            }
            else {
                beg += StringSupport.strLengthFromRubyString(this, enc);
                if (beg < 0) {
                    return runtime.getNil();
                }
            }
        }
        else if (beg > 0 && beg > StringSupport.strLengthFromRubyString(this, enc)) {
            return runtime.getNil();
        }
        int p;
        if (len == 0) {
            p = 0;
        }
        else if (this.isCodeRangeValid() && enc.isUTF8()) {
            p = StringSupport.utf8Nth(bytes, s, end, beg);
            len = StringSupport.utf8Offset(bytes, p, end, len);
        }
        else if (enc.isFixedWidth()) {
            final int w = enc.maxLength();
            p = s + beg * w;
            if (p > end) {
                p = end;
                len = 0;
            }
            else if (len * w > end - p) {
                len = end - p;
            }
            else {
                len *= w;
            }
        }
        else if ((p = StringSupport.nth(enc, bytes, s, end, beg)) == end) {
            len = 0;
        }
        else {
            len = StringSupport.offset(enc, bytes, p, end, len);
        }
        return this.makeShared19(runtime, p - s, len);
    }
    
    private IRubyObject replaceInternal(final int beg, final int len, final RubyString repl) {
        StringSupport.replaceInternal(beg, len, this, repl);
        return this.infectBy(repl);
    }
    
    private void replaceInternal19(final int beg, final int len, final RubyString repl) {
        StringSupport.replaceInternal19(this.getRuntime(), beg, len, this, repl);
        this.infectBy(repl);
    }
    
    public IRubyObject op_aref(final ThreadContext context, final IRubyObject arg1, final IRubyObject arg2) {
        return this.op_aref19(context, arg1, arg2);
    }
    
    public IRubyObject op_aref(final ThreadContext context, final IRubyObject arg) {
        return this.op_aref19(context, arg);
    }
    
    @JRubyMethod(name = { "[]", "slice" }, reads = { FrameField.BACKREF }, writes = { FrameField.BACKREF })
    public IRubyObject op_aref19(final ThreadContext context, final IRubyObject arg1, final IRubyObject arg2) {
        final Ruby runtime = context.runtime;
        if (arg1 instanceof RubyRegexp) {
            return this.subpat19(runtime, context, (RubyRegexp)arg1, arg2);
        }
        return this.substr19(runtime, RubyNumeric.num2int(arg1), RubyNumeric.num2int(arg2));
    }
    
    @JRubyMethod(name = { "[]", "slice" }, reads = { FrameField.BACKREF }, writes = { FrameField.BACKREF })
    public IRubyObject op_aref19(final ThreadContext context, final IRubyObject arg) {
        final Ruby runtime = context.runtime;
        if (arg instanceof RubyFixnum) {
            return this.op_aref19(runtime, RubyNumeric.fix2int((RubyFixnum)arg));
        }
        if (arg instanceof RubyRegexp) {
            return this.subpat19(runtime, context, (RubyRegexp)arg);
        }
        if (arg instanceof RubyString) {
            final RubyString str = (RubyString)arg;
            return (StringSupport.index(this, str, 0, this.checkEncoding(str)) != -1) ? str.strDup(runtime) : runtime.getNil();
        }
        if (arg instanceof RubyRange) {
            final int len = this.strLength();
            final int[] begLen = ((RubyRange)arg).begLenInt(len, 0);
            return (begLen == null) ? runtime.getNil() : this.substr19(runtime, begLen[0], begLen[1]);
        }
        final JavaSites.StringSites sites = sites(context);
        if (RubyRange.isRangeLike(context, arg, sites.respond_to_begin, sites.respond_to_end)) {
            final int len2 = this.strLength();
            final RubyRange range = RubyRange.rangeFromRangeLike(context, arg, sites.begin, sites.end, sites.exclude_end);
            final int[] begLen2 = range.begLenInt(len2, 0);
            return (begLen2 == null) ? runtime.getNil() : this.substr19(runtime, begLen2[0], begLen2[1]);
        }
        return this.op_aref19(runtime, RubyNumeric.num2int(arg));
    }
    
    @JRubyMethod
    public IRubyObject byteslice(final ThreadContext context, final IRubyObject arg1, final IRubyObject arg2) {
        return this.byteSubstr(context.runtime, RubyNumeric.num2int(arg1), RubyNumeric.num2int(arg2));
    }
    
    @JRubyMethod
    public IRubyObject byteslice(final ThreadContext context, final IRubyObject arg) {
        return this.byteARef(context.runtime, arg);
    }
    
    private IRubyObject op_aref19(final Ruby runtime, final int idx) {
        final IRubyObject str = this.substr19(runtime, idx, 1);
        return (!str.isNil() && ((RubyString)str).value.getRealSize() == 0) ? runtime.getNil() : str;
    }
    
    private int subpatSetCheck(final Ruby runtime, final int nth, final Region regs) {
        final int numRegs = (regs == null) ? 1 : regs.numRegs;
        if (nth < numRegs) {
            if (nth >= 0) {
                return nth;
            }
            if (-nth < numRegs) {
                return nth + numRegs;
            }
        }
        throw runtime.newIndexError("index " + nth + " out of regexp");
    }
    
    private void subpatSet19(final ThreadContext context, final RubyRegexp regexp, final IRubyObject backref, final IRubyObject repl) {
        final Ruby runtime = context.runtime;
        final int result = regexp.search19(context, this, 0, false);
        if (result < 0) {
            throw runtime.newIndexError("regexp not matched");
        }
        final RubyMatchData match = (RubyMatchData)context.getBackRef();
        final int nth = (backref == null) ? 0 : this.subpatSetCheck(runtime, match.backrefNumber(backref), match.regs);
        int start;
        int end;
        if (match.regs == null) {
            start = match.begin;
            end = match.end;
        }
        else {
            start = match.regs.beg[nth];
            end = match.regs.end[nth];
        }
        if (start == -1) {
            throw runtime.newIndexError("regexp group " + nth + " not matched");
        }
        final RubyString replStr = repl.convertToString();
        final Encoding enc = this.checkEncoding(replStr);
        this.replaceInternal(start, end - start, replStr);
        this.associateEncoding(enc);
    }
    
    private IRubyObject subpat19(final Ruby runtime, final ThreadContext context, final RubyRegexp regex, final IRubyObject backref) {
        final int result = regex.search19(context, this, 0, false);
        if (result >= 0) {
            final RubyMatchData match = (RubyMatchData)context.getBackRef();
            return RubyRegexp.nth_match(match.backrefNumber(backref), match);
        }
        return runtime.getNil();
    }
    
    private IRubyObject subpat19(final Ruby runtime, final ThreadContext context, final RubyRegexp regex) {
        final int result = regex.search19(context, this, 0, false);
        if (result >= 0) {
            return RubyRegexp.nth_match(0, context.getBackRef());
        }
        return runtime.getNil();
    }
    
    public IRubyObject op_aset(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1) {
        return this.op_aset19(context, arg0, arg1);
    }
    
    public IRubyObject op_aset(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1, final IRubyObject arg2) {
        return this.op_aset19(context, arg0, arg1, arg2);
    }
    
    @JRubyMethod(name = { "[]=" }, reads = { FrameField.BACKREF })
    public IRubyObject op_aset19(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1) {
        if (arg0 instanceof RubyFixnum) {
            return this.op_aset19(context, RubyNumeric.fix2int((RubyFixnum)arg0), arg1);
        }
        if (arg0 instanceof RubyRegexp) {
            this.subpatSet19(context, (RubyRegexp)arg0, null, arg1);
            return arg1;
        }
        if (arg0 instanceof RubyString) {
            final RubyString orig = (RubyString)arg0;
            int beg = StringSupport.index(this, orig, 0, this.checkEncoding(orig));
            if (beg < 0) {
                throw context.runtime.newIndexError("string not matched");
            }
            beg = this.subLength(beg);
            this.replaceInternal19(beg, orig.strLength(), arg1.convertToString());
            return arg1;
        }
        else {
            if (arg0 instanceof RubyRange) {
                final int[] begLen = ((RubyRange)arg0).begLenInt(this.strLength(), 2);
                this.replaceInternal19(begLen[0], begLen[1], arg1.convertToString());
                return arg1;
            }
            final JavaSites.StringSites sites = sites(context);
            if (RubyRange.isRangeLike(context, arg0, sites.respond_to_begin, sites.respond_to_end)) {
                final RubyRange rng = RubyRange.rangeFromRangeLike(context, arg0, sites.begin, sites.end, sites.exclude_end);
                final int[] begLen2 = rng.begLenInt(this.strLength(), 2);
                this.replaceInternal19(begLen2[0], begLen2[1], arg1.convertToString());
                return arg1;
            }
            return this.op_aset19(context, RubyNumeric.num2int(arg0), arg1);
        }
    }
    
    private IRubyObject op_aset19(final ThreadContext context, final int idx, final IRubyObject arg1) {
        StringSupport.replaceInternal19(context.runtime, idx, 1, this, arg1.convertToString());
        return arg1;
    }
    
    @JRubyMethod(name = { "[]=" }, reads = { FrameField.BACKREF })
    public IRubyObject op_aset19(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1, final IRubyObject arg2) {
        if (arg0 instanceof RubyRegexp) {
            this.subpatSet19(context, (RubyRegexp)arg0, arg1, arg2);
        }
        else {
            final int beg = RubyNumeric.num2int(arg0);
            final int len = RubyNumeric.num2int(arg1);
            this.checkLength(len);
            final RubyString repl = arg2.convertToString();
            StringSupport.replaceInternal19(context.runtime, beg, len, this, repl);
        }
        return arg2;
    }
    
    @Deprecated
    public IRubyObject slice_bang19(final ThreadContext context, final IRubyObject arg0) {
        return this.slice_bang(context, arg0);
    }
    
    @Deprecated
    public IRubyObject slice_bang19(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1) {
        return this.slice_bang(context, arg0, arg1);
    }
    
    @JRubyMethod(name = { "slice!" }, reads = { FrameField.BACKREF }, writes = { FrameField.BACKREF })
    public IRubyObject slice_bang(final ThreadContext context, final IRubyObject arg0) {
        final IRubyObject result = this.op_aref19(context, arg0);
        if (result.isNil()) {
            this.modifyCheck();
        }
        else {
            this.op_aset19(context, arg0, newEmptyString(context.runtime));
        }
        return result;
    }
    
    @JRubyMethod(name = { "slice!" }, reads = { FrameField.BACKREF }, writes = { FrameField.BACKREF })
    public IRubyObject slice_bang(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1) {
        final IRubyObject result = this.op_aref19(context, arg0, arg1);
        if (result.isNil()) {
            this.modifyCheck();
        }
        else {
            this.op_aset19(context, arg0, arg1, newEmptyString(context.runtime));
        }
        return result;
    }
    
    @Deprecated
    public IRubyObject succ19(final ThreadContext context) {
        return this.succ(context);
    }
    
    @Deprecated
    public IRubyObject succ_bang19() {
        return this.succ_bang();
    }
    
    @JRubyMethod(name = { "succ", "next" })
    public IRubyObject succ(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        RubyString str;
        if (this.value.getRealSize() > 0) {
            str = new RubyString(runtime, this.getMetaClass(), StringSupport.succCommon(runtime, this.value));
        }
        else {
            str = newEmptyString(runtime, this.getType(), this.value.getEncoding());
        }
        return str.infectBy(this);
    }
    
    @JRubyMethod(name = { "succ!", "next!" })
    public IRubyObject succ_bang() {
        this.modifyCheck();
        if (this.value.getRealSize() > 0) {
            this.value = StringSupport.succCommon(this.getRuntime(), this.value);
            this.shareLevel = 0;
        }
        return this;
    }
    
    @Deprecated
    public final IRubyObject upto19(final ThreadContext context, final IRubyObject end, final Block block) {
        return this.upto(context, end, block);
    }
    
    @Deprecated
    public final IRubyObject upto19(final ThreadContext context, final IRubyObject end, final IRubyObject excl, final Block block) {
        return this.upto(context, end, excl, block);
    }
    
    @JRubyMethod(name = { "upto" })
    public final IRubyObject upto(final ThreadContext context, final IRubyObject end, final Block block) {
        final Ruby runtime = context.runtime;
        return block.isGiven() ? this.uptoCommon(context, end, false, block) : RubyEnumerator.enumeratorize(runtime, this, "upto", end);
    }
    
    @JRubyMethod(name = { "upto" })
    public final IRubyObject upto(final ThreadContext context, final IRubyObject end, final IRubyObject excl, final Block block) {
        return block.isGiven() ? this.uptoCommon(context, end, excl.isTrue(), block) : RubyEnumerator.enumeratorize(context.runtime, this, "upto", new IRubyObject[] { end, excl });
    }
    
    final IRubyObject uptoCommon(final ThreadContext context, final IRubyObject arg, final boolean excl, final Block block) {
        return this.uptoCommon(context, arg, excl, block, false);
    }
    
    final IRubyObject uptoCommon(final ThreadContext context, final IRubyObject arg, final boolean excl, final Block block, final boolean asSymbol) {
        final Ruby runtime = context.runtime;
        if (arg instanceof RubySymbol) {
            throw runtime.newTypeError("can't convert Symbol into String");
        }
        final RubyString end = arg.convertToString();
        final Encoding enc = this.checkEncoding(end);
        final boolean isAscii = this.scanForCodeRange() == 16 && end.scanForCodeRange() == 16;
        if (this.value.getRealSize() == 1 && end.value.getRealSize() == 1 && isAscii) {
            byte c = this.value.getUnsafeBytes()[this.value.getBegin()];
            final byte e = end.value.getUnsafeBytes()[end.value.getBegin()];
            if (c > e || (excl && c == e)) {
                return this;
            }
            do {
                final RubyString s = new RubyString(runtime, runtime.getString(), RubyInteger.SINGLE_CHAR_BYTELISTS[c & 0xFF], enc, 16);
                s.shareLevel = 2;
                block.yield(context, (IRubyObject)(asSymbol ? runtime.newSymbol(s.toString()) : s));
                if (!excl && c == e) {
                    break;
                }
                ++c;
            } while (!excl || c != e);
            return this;
        }
        else {
            if (isAscii && RubyString.ASCII.isDigit((int)this.value.getUnsafeBytes()[this.value.getBegin()]) && RubyString.ASCII.isDigit((int)end.value.getUnsafeBytes()[end.value.getBegin()])) {
                int s2 = this.value.getBegin();
                int send = s2 + this.value.getRealSize();
                byte[] bytes = this.value.getUnsafeBytes();
                while (s2 < send) {
                    if (!RubyString.ASCII.isDigit(bytes[s2] & 0xFF)) {
                        return this.uptoCommonNoDigits(context, end, excl, block, asSymbol);
                    }
                    ++s2;
                }
                s2 = end.value.getBegin();
                send = s2 + end.value.getRealSize();
                bytes = end.value.getUnsafeBytes();
                while (s2 < send) {
                    if (!RubyString.ASCII.isDigit(bytes[s2] & 0xFF)) {
                        return this.uptoCommonNoDigits(context, end, excl, block, asSymbol);
                    }
                    ++s2;
                }
                IRubyObject b = this.stringToInum19(10, false);
                final IRubyObject e2 = end.stringToInum19(10, false);
                final RubyArray argsArr = RubyArray.newArray(runtime, RubyFixnum.newFixnum(runtime, this.value.length()), context.nil);
                if (b instanceof RubyFixnum && e2 instanceof RubyFixnum) {
                    for (long bl = RubyNumeric.fix2long(b), el = RubyNumeric.fix2long(e2); bl <= el && (!excl || bl != el); ++bl) {
                        argsArr.eltSetOk(1, RubyFixnum.newFixnum(runtime, bl));
                        final ByteList to = new ByteList(this.value.length() + 5);
                        Sprintf.sprintf(to, "%.*d", argsArr);
                        final RubyString str = newStringNoCopy(runtime, to, (Encoding)USASCIIEncoding.INSTANCE, 16);
                        block.yield(context, (IRubyObject)(asSymbol ? runtime.newSymbol(str.toString()) : str));
                    }
                }
                else {
                    final JavaSites.StringSites sites = sites(context);
                    for (CallSite op = excl ? sites.op_lt : sites.op_le; op.call(context, b, b, e2).isTrue(); b = sites.succ.call(context, b, b)) {
                        argsArr.eltSetOk(1, b);
                        final ByteList to2 = new ByteList(this.value.length() + 5);
                        Sprintf.sprintf(to2, "%.*d", argsArr);
                        final RubyString str2 = newStringNoCopy(runtime, to2, (Encoding)USASCIIEncoding.INSTANCE, 16);
                        block.yield(context, (IRubyObject)(asSymbol ? runtime.newSymbol(str2.toString()) : str2));
                    }
                }
                return this;
            }
            return this.uptoCommonNoDigits(context, end, excl, block, asSymbol);
        }
    }
    
    private IRubyObject uptoCommonNoDigits(final ThreadContext context, final RubyString end, final boolean excl, final Block block, final boolean asSymbol) {
        final Ruby runtime = context.runtime;
        final int n = this.op_cmp(end);
        if (n > 0 || (excl && n == 0)) {
            return this;
        }
        final JavaSites.StringSites sites = sites(context);
        final CallSite succ = sites.succ;
        final IRubyObject afterEnd = succ.call(context, end, end);
        RubyString current = this.strDup(context.runtime);
        while (!current.op_equal19(context, afterEnd).isTrue()) {
            IRubyObject next = null;
            if (excl || !current.op_equal19(context, end).isTrue()) {
                next = succ.call(context, current, current);
            }
            block.yield(context, (IRubyObject)(asSymbol ? runtime.newSymbol(current.toString()) : current));
            if (next == null) {
                break;
            }
            current = next.convertToString();
            if (excl && current.op_equal19(context, end).isTrue()) {
                break;
            }
            if (current.getByteList().length() > end.getByteList().length()) {
                break;
            }
            if (current.getByteList().length() == 0) {
                break;
            }
        }
        return this;
    }
    
    @Deprecated
    public final RubyBoolean include_p19(final ThreadContext context, final IRubyObject obj) {
        return this.include_p(context, obj);
    }
    
    @JRubyMethod(name = { "include?" })
    public RubyBoolean include_p(final ThreadContext context, final IRubyObject obj) {
        final Ruby runtime = context.runtime;
        final RubyString coerced = obj.convertToString();
        return (StringSupport.index(this, coerced, 0, this.checkEncoding(coerced)) == -1) ? runtime.getFalse() : runtime.getTrue();
    }
    
    @JRubyMethod
    public IRubyObject chr(final ThreadContext context) {
        return this.substr19(context.runtime, 0, 1);
    }
    
    @JRubyMethod
    public IRubyObject getbyte(final ThreadContext context, final IRubyObject index) {
        final Ruby runtime = context.runtime;
        int i = RubyNumeric.num2int(index);
        if (i < 0) {
            i += this.value.getRealSize();
        }
        if (i < 0 || i >= this.value.getRealSize()) {
            return runtime.getNil();
        }
        return RubyFixnum.newFixnum(runtime, this.value.getUnsafeBytes()[this.value.getBegin() + i] & 0xFF);
    }
    
    @JRubyMethod
    public IRubyObject setbyte(final ThreadContext context, final IRubyObject index, final IRubyObject val) {
        final int i = RubyNumeric.num2int(index);
        final int b = RubyNumeric.num2int(val);
        final int normalizedIndex = this.checkIndexForRef(i, this.value.getRealSize());
        this.modify19();
        this.value.getUnsafeBytes()[normalizedIndex] = (byte)b;
        return val;
    }
    
    public IRubyObject to_i() {
        return this.to_i19();
    }
    
    public IRubyObject to_i(final IRubyObject arg0) {
        return this.to_i19(arg0);
    }
    
    @JRubyMethod(name = { "to_i" })
    public IRubyObject to_i19() {
        return this.stringToInum19(10, false);
    }
    
    @JRubyMethod(name = { "to_i" })
    public IRubyObject to_i19(final IRubyObject arg0) {
        final long base = this.checkBase(arg0);
        return this.stringToInum19((int)base, false);
    }
    
    private long checkBase(final IRubyObject arg0) {
        final long base = arg0.convertToInteger().getLongValue();
        if (base < 0L) {
            throw this.getRuntime().newArgumentError("illegal radix " + base);
        }
        return base;
    }
    
    @Deprecated
    public IRubyObject stringToInum(final int base, final boolean badcheck) {
        final ByteList s = this.value;
        return ConvertBytes.byteListToInum(this.getRuntime(), s, base, badcheck);
    }
    
    public IRubyObject stringToInum19(final int base, final boolean badcheck) {
        final ByteList s = this.value;
        if (!s.getEncoding().isAsciiCompatible()) {
            throw this.getRuntime().newEncodingCompatibilityError("ASCII incompatible encoding: " + s.getEncoding());
        }
        return ConvertBytes.byteListToInum19(this.getRuntime(), s, base, badcheck);
    }
    
    public IRubyObject oct(final ThreadContext context) {
        return this.oct19(context);
    }
    
    @JRubyMethod(name = { "oct" })
    public IRubyObject oct19(final ThreadContext context) {
        return this.stringToInum19(-8, false);
    }
    
    public IRubyObject hex(final ThreadContext context) {
        return this.hex19(context);
    }
    
    @JRubyMethod(name = { "hex" })
    public IRubyObject hex19(final ThreadContext context) {
        return this.stringToInum19(16, false);
    }
    
    public IRubyObject to_f() {
        return this.to_f19();
    }
    
    @JRubyMethod(name = { "to_f" })
    public IRubyObject to_f19() {
        return RubyNumeric.str2fnum(this.getRuntime(), this, false);
    }
    
    public RubyArray split(final ThreadContext context) {
        return this.split19(context);
    }
    
    public RubyArray split(final ThreadContext context, final IRubyObject arg0) {
        return this.split19(context, arg0);
    }
    
    public RubyArray split(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1) {
        return this.split19(context, arg0, arg1);
    }
    
    private void populateCapturesForSplit(final Ruby runtime, final RubyArray result, final RubyMatchData match) {
        for (int i = 1; i < match.numRegs(); ++i) {
            final int beg = match.begin(i);
            if (beg != -1) {
                result.append(this.makeShared19(runtime, beg, match.end(i) - beg));
            }
        }
    }
    
    @JRubyMethod(name = { "split" }, writes = { FrameField.BACKREF })
    public RubyArray split19(final ThreadContext context) {
        return this.split19(context, context.runtime.getNil());
    }
    
    @JRubyMethod(name = { "split" }, writes = { FrameField.BACKREF })
    public RubyArray split19(final ThreadContext context, final IRubyObject arg0) {
        return this.splitCommon19(arg0, false, 0, 0, context, true);
    }
    
    @JRubyMethod(name = { "split" }, writes = { FrameField.BACKREF })
    public RubyArray split19(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1) {
        final int lim = RubyNumeric.num2int(arg1);
        if (lim <= 0) {
            return this.splitCommon19(arg0, false, lim, 1, context, true);
        }
        if (lim == 1) {
            return (this.value.getRealSize() == 0) ? context.runtime.newArray() : context.runtime.newArray(this);
        }
        return this.splitCommon19(arg0, true, lim, 1, context, true);
    }
    
    public RubyArray split19(final IRubyObject spat, final ThreadContext context, final boolean useBackref) {
        return this.splitCommon19(spat, false, this.value.realSize(), 0, context, useBackref);
    }
    
    private RubyArray splitCommon19(IRubyObject spat, final boolean limit, final int lim, final int i, final ThreadContext context, final boolean useBackref) {
        RubyArray result;
        if (spat.isNil() && (spat = context.runtime.getGlobalVariables().get("$;")).isNil()) {
            result = this.awkSplit19(limit, lim, i);
        }
        else {
            spat = getPatternQuoted(context, spat, false);
            if (spat instanceof RubyString) {
                final ByteList spatValue = ((RubyString)spat).value;
                final int len = spatValue.getRealSize();
                final Encoding spatEnc = spatValue.getEncoding();
                ((RubyString)spat).mustnotBroken(context);
                if (len == 0) {
                    final RubyRegexp pattern = RubyRegexp.newRegexpFromStr(context.runtime, (RubyString)spat, 0);
                    result = this.regexSplit19(context, pattern, limit, lim, i, useBackref);
                }
                else {
                    final byte[] bytes = spatValue.getUnsafeBytes();
                    final int p = spatValue.getBegin();
                    int c;
                    if (spatEnc.isAsciiCompatible()) {
                        c = ((len == 1) ? (bytes[p] & 0xFF) : -1);
                    }
                    else {
                        c = ((len == StringSupport.preciseLength(spatEnc, bytes, p, p + len)) ? spatEnc.mbcToCode(bytes, p, p + len) : -1);
                    }
                    result = ((c == 32) ? this.awkSplit19(limit, lim, i) : this.stringSplit19(context, (RubyString)spat, limit, lim, i));
                }
            }
            else {
                result = this.regexSplit19(context, (RubyRegexp)spat, limit, lim, i, useBackref);
            }
        }
        if (!limit && lim == 0) {
            while (result.size() > 0 && ((RubyString)result.eltInternal(result.size() - 1)).value.getRealSize() == 0) {
                result.pop(context);
            }
        }
        return result;
    }
    
    private RubyArray regexSplit19(final ThreadContext context, final RubyRegexp pattern, final boolean limit, final int lim, int i, final boolean useBackref) {
        final Ruby runtime = context.runtime;
        final int ptr = this.value.getBegin();
        final int len = this.value.getRealSize();
        final byte[] bytes = this.value.getUnsafeBytes();
        final RubyArray result = runtime.newArray();
        final Encoding enc = this.value.getEncoding();
        final boolean captures = pattern.getPattern().numberOfCaptures() != 0;
        int beg = 0;
        boolean lastNull = false;
        int start = beg;
        final IRubyObject[] holder = (IRubyObject[])(useBackref ? null : new IRubyObject[] { context.nil });
        int end;
        while ((end = pattern.search19(context, this, start, false, holder)) >= 0) {
            final RubyMatchData match = (RubyMatchData)(useBackref ? context.getBackRef() : ((RubyMatchData)holder[0]));
            if (start == end && match.begin(0) == match.end(0)) {
                if (len == 0) {
                    result.append(newEmptyString(runtime, this.getMetaClass()).infectBy(this));
                    break;
                }
                if (!lastNull) {
                    if (ptr + start == ptr + len) {
                        ++start;
                    }
                    else {
                        start += StringSupport.length(enc, bytes, ptr + start, ptr + len);
                    }
                    lastNull = true;
                    continue;
                }
                result.append(this.makeShared19(runtime, beg, StringSupport.length(enc, bytes, ptr + beg, ptr + len)));
                beg = start;
            }
            else {
                result.append(this.makeShared19(runtime, beg, end - beg));
                beg = (start = match.end(0));
            }
            lastNull = false;
            if (captures) {
                this.populateCapturesForSplit(runtime, result, match);
            }
            if (limit && lim <= ++i) {
                break;
            }
        }
        if (useBackref) {
            context.setBackRef(context.nil);
        }
        else {
            holder[0] = context.nil;
        }
        if (len > 0 && (limit || len > beg || lim < 0)) {
            result.append(this.makeShared19(runtime, beg, len - beg));
        }
        return result;
    }
    
    private RubyArray awkSplit19(final boolean limit, final int lim, int i) {
        final Ruby runtime = this.getRuntime();
        final RubyArray result = runtime.newArray();
        final byte[] bytes = this.value.getUnsafeBytes();
        final int ptr;
        int p = ptr = this.value.getBegin();
        final int len = this.value.getRealSize();
        final int end = p + len;
        final Encoding enc = this.value.getEncoding();
        boolean skip = true;
        int e = 0;
        int b = 0;
        final boolean singlebyte = this.singleByteOptimizable(enc);
        while (p < end) {
            int c;
            if (singlebyte) {
                c = (bytes[p++] & 0xFF);
            }
            else {
                c = StringSupport.codePoint(runtime, enc, bytes, p, end);
                p += StringSupport.length(enc, bytes, p, end);
            }
            if (skip) {
                if (enc.isSpace(c)) {
                    b = p - ptr;
                }
                else {
                    e = p - ptr;
                    skip = false;
                    if (limit && lim <= i) {
                        break;
                    }
                    continue;
                }
            }
            else if (enc.isSpace(c)) {
                result.append(this.makeShared19(runtime, b, e - b));
                skip = true;
                b = p - ptr;
                if (!limit) {
                    continue;
                }
                ++i;
            }
            else {
                e = p - ptr;
            }
        }
        if (len > 0 && (limit || len > b || lim < 0)) {
            result.append(this.makeShared19(runtime, b, len - b));
        }
        return result;
    }
    
    private RubyArray stringSplit19(final ThreadContext context, final RubyString spat, final boolean limit, final int lim, int i) {
        final Ruby runtime = context.runtime;
        this.mustnotBroken(context);
        final RubyArray result = runtime.newArray();
        final Encoding enc = this.checkEncoding(spat);
        final ByteList pattern = spat.value;
        final byte[] patternBytes = pattern.getUnsafeBytes();
        final int patternBegin = pattern.getBegin();
        final int patternRealSize = pattern.getRealSize();
        final byte[] bytes = this.value.getUnsafeBytes();
        final int begin = this.value.getBegin();
        final int realSize = this.value.getRealSize();
        int p = 0;
        int e;
        while (p < realSize && (e = indexOf(bytes, begin, realSize, patternBytes, patternBegin, patternRealSize, p)) >= 0) {
            final int t = enc.rightAdjustCharHead(bytes, p + begin, e + begin, begin + realSize) - begin;
            if (t != e) {
                p = t;
            }
            else {
                result.append(this.makeShared19(runtime, p, e - p));
                p = e + pattern.getRealSize();
                if (limit && lim <= ++i) {
                    break;
                }
                continue;
            }
        }
        if (this.value.getRealSize() > 0 && (limit || this.value.getRealSize() > p || lim < 0)) {
            result.append(this.makeShared19(runtime, p, this.value.getRealSize() - p));
        }
        return result;
    }
    
    static int indexOf(final byte[] source, final int sourceOffset, final int sourceCount, final byte[] target, final int targetOffset, final int targetCount, int fromIndex) {
        if (fromIndex >= sourceCount) {
            return (targetCount == 0) ? sourceCount : -1;
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (targetCount == 0) {
            return fromIndex;
        }
        final byte first = target[targetOffset];
        for (int max = sourceOffset + (sourceCount - targetCount), i = sourceOffset + fromIndex; i <= max; ++i) {
            if (source[i] != first) {
                while (++i <= max && source[i] != first) {}
            }
            if (i <= max) {
                int j = i + 1;
                final int end = j + targetCount - 1;
                for (int k = targetOffset + 1; j < end && source[j] == target[k]; ++j, ++k) {}
                if (j == end) {
                    return i - sourceOffset;
                }
            }
        }
        return -1;
    }
    
    private RubyString getStringForPattern(final IRubyObject obj) {
        if (obj instanceof RubyString) {
            return (RubyString)obj;
        }
        final IRubyObject val = obj.checkStringType();
        if (val.isNil()) {
            throw this.getRuntime().newTypeError("wrong argument type " + obj.getMetaClass() + " (expected Regexp)");
        }
        return (RubyString)val;
    }
    
    private RubyRegexp getPattern(final IRubyObject obj) {
        if (obj instanceof RubyRegexp) {
            return (RubyRegexp)obj;
        }
        return RubyRegexp.newRegexpFromStr(this.getRuntime(), this.getStringForPattern(obj), 0);
    }
    
    private static IRubyObject getPatternQuoted(final ThreadContext context, IRubyObject pat, final boolean check) {
        if (pat instanceof RubyRegexp) {
            return pat;
        }
        if (!(pat instanceof RubyString)) {
            final IRubyObject val = pat.checkStringType19();
            if (val.isNil()) {
                TypeConverter.checkType(context, pat, context.runtime.getRegexp());
            }
            pat = val;
        }
        if (check && ((RubyString)pat).isBrokenString()) {
            throw context.runtime.newRegexpError("invalid byte sequence in " + ((RubyString)pat).getEncoding());
        }
        return pat;
    }
    
    public IRubyObject scan(final ThreadContext context, final IRubyObject arg, final Block block) {
        return this.scan19(context, arg, block);
    }
    
    private IRubyObject populateCapturesForScan(final Ruby runtime, final Matcher matcher, final int range, final int tuFlags, final boolean is19) {
        final Region region = matcher.getRegion();
        final RubyArray result = this.getRuntime().newArray(region.numRegs);
        for (int i = 1; i < region.numRegs; ++i) {
            final int beg = region.beg[i];
            if (beg == -1) {
                result.append(runtime.getNil());
            }
            else {
                final RubyString substr = is19 ? this.makeShared19(runtime, beg, region.end[i] - beg) : this.makeShared(runtime, beg, region.end[i] - beg);
                substr.infectBy(tuFlags);
                result.append(substr);
            }
        }
        return result;
    }
    
    @JRubyMethod(name = { "scan" }, reads = { FrameField.BACKREF }, writes = { FrameField.BACKREF })
    public IRubyObject scan19(final ThreadContext context, IRubyObject pat, final Block block) {
        final RubyString str = this;
        int last = -1;
        int prev = 0;
        final int[] startp = { 0 };
        pat = getPatternQuoted(context, pat, true);
        this.mustnotBroken(context);
        if (!block.isGiven()) {
            RubyArray ary = null;
            IRubyObject result;
            while (!(result = scanOnce(context, str, pat, startp)).isNil()) {
                last = prev;
                prev = startp[0];
                if (ary == null) {
                    ary = context.runtime.newArray(4);
                }
                ary.push(result);
            }
            if (last >= 0) {
                patternSearch(context, pat, str, last, true);
            }
            return (ary == null) ? context.runtime.newEmptyArray() : ary;
        }
        final byte[] pBytes = this.value.unsafeBytes();
        final int len = this.value.realSize();
        IRubyObject result;
        while (!(result = scanOnce(context, str, pat, startp)).isNil()) {
            last = prev;
            prev = startp[0];
            block.yieldSpecific(context, result);
            str.modifyCheck(pBytes, len);
        }
        if (last >= 0) {
            patternSearch(context, pat, str, last, true);
        }
        return this;
    }
    
    private void mustnotBroken(final ThreadContext context) {
        if (this.scanForCodeRange() == 48) {
            throw context.runtime.newArgumentError("invalid byte sequence in " + this.getEncoding());
        }
    }
    
    private static IRubyObject scanOnce(final ThreadContext context, final RubyString str, final IRubyObject pat, final int[] startp) {
        if (patternSearch(context, pat, str, startp[0], true) < 0) {
            return context.nil;
        }
        final RubyMatchData match = (RubyMatchData)context.getBackRef();
        final int matchEnd = match.end(0);
        if (match.begin(0) == matchEnd) {
            final Encoding enc = str.getEncoding();
            if (str.size() > matchEnd) {
                final ByteList strValue = str.value;
                startp[0] = matchEnd + StringSupport.encFastMBCLen(strValue.unsafeBytes(), strValue.begin() + matchEnd, strValue.begin() + strValue.realSize(), enc);
            }
            else {
                startp[0] = matchEnd + 1;
            }
        }
        else {
            startp[0] = matchEnd;
        }
        if (match.numRegs() == 1) {
            return RubyRegexp.nth_match(0, match);
        }
        final int size = match.numRegs();
        final RubyArray result = RubyArray.newBlankArray(context.runtime, size - 1);
        int index = 0;
        for (int i = 1; i < size; ++i) {
            result.store(index++, RubyRegexp.nth_match(i, match));
        }
        return result;
    }
    
    private static int patternSearch(final ThreadContext context, final IRubyObject pattern, final RubyString str, final int pos, final boolean setBackrefStr) {
        if (pattern instanceof RubyString) {
            final RubyString strPattern = (RubyString)pattern;
            final int beg = str.strseqIndex(strPattern, pos, true);
            if (setBackrefStr) {
                if (beg >= 0) {
                    setBackRefString(context, str, beg, strPattern).infectBy(pattern);
                }
                else {
                    context.setBackRef(context.nil);
                }
            }
            return beg;
        }
        return ((RubyRegexp)pattern).search19(context, str, pos, false);
    }
    
    private static RubyMatchData setBackRefString(final ThreadContext context, final RubyString str, final int pos, final RubyString pattern) {
        final IRubyObject m = context.getBackRef();
        RubyMatchData match;
        if (m == null || m.isNil() || ((RubyMatchData)m).used()) {
            match = new RubyMatchData(context.runtime);
        }
        else {
            match = (RubyMatchData)m;
        }
        match.initMatchData(context, str, pos, pattern);
        context.setBackRef(match);
        return match;
    }
    
    @JRubyMethod(name = { "start_with?" })
    public IRubyObject start_with_p(final ThreadContext context) {
        return context.runtime.getFalse();
    }
    
    @JRubyMethod(name = { "start_with?" })
    public IRubyObject start_with_p(final ThreadContext context, final IRubyObject arg) {
        return this.start_with_pCommon(arg) ? context.runtime.getTrue() : context.runtime.getFalse();
    }
    
    @JRubyMethod(name = { "start_with?" }, rest = true)
    public IRubyObject start_with_p(final ThreadContext context, final IRubyObject[] args) {
        for (int i = 0; i < args.length; ++i) {
            if (this.start_with_pCommon(args[i])) {
                return context.runtime.getTrue();
            }
        }
        return context.runtime.getFalse();
    }
    
    private boolean start_with_pCommon(final IRubyObject arg) {
        final RubyString otherString = arg.convertToString();
        this.checkEncoding(otherString);
        final int otherLength = otherString.value.getRealSize();
        return otherLength == 0 || (this.value.getRealSize() >= otherLength && this.value.startsWith(otherString.value));
    }
    
    @JRubyMethod(name = { "end_with?" })
    public IRubyObject end_with_p(final ThreadContext context) {
        return context.runtime.getFalse();
    }
    
    @JRubyMethod(name = { "end_with?" })
    public IRubyObject end_with_p(final ThreadContext context, final IRubyObject arg) {
        return this.end_with_pCommon(arg) ? context.runtime.getTrue() : context.runtime.getFalse();
    }
    
    @JRubyMethod(name = { "end_with?" }, rest = true)
    public IRubyObject end_with_p(final ThreadContext context, final IRubyObject[] args) {
        for (int i = 0; i < args.length; ++i) {
            if (this.end_with_pCommon(args[i])) {
                return context.runtime.getTrue();
            }
        }
        return context.runtime.getFalse();
    }
    
    private boolean end_with_pCommon(IRubyObject tmp) {
        tmp = tmp.convertToString();
        final ByteList tmpBL = ((RubyString)tmp).value;
        if (tmpBL.getRealSize() == 0) {
            return true;
        }
        final Encoding enc = this.checkEncoding((RubyString)tmp);
        if (this.value.realSize() < tmpBL.realSize()) {
            return false;
        }
        final int p = this.value.begin();
        final int e = p + this.value.realSize();
        final int s = e - tmpBL.realSize();
        return enc.leftAdjustCharHead(this.value.unsafeBytes(), p, s, e) == s && ByteList.memcmp(this.value.unsafeBytes(), s, tmpBL.unsafeBytes(), tmpBL.begin(), tmpBL.realSize()) == 0;
    }
    
    private IRubyObject justify19(final Ruby runtime, final IRubyObject arg0, final int jflag) {
        final RubyString result = this.justifyCommon(runtime, RubyString.SPACE_BYTELIST, 1, true, EncodingUtils.STR_ENC_GET(this), RubyNumeric.num2int(arg0), jflag);
        if (this.getCodeRange() != 48) {
            result.setCodeRange(this.getCodeRange());
        }
        return result;
    }
    
    private IRubyObject justify19(final IRubyObject arg0, final IRubyObject arg1, final int jflag) {
        final Ruby runtime = this.getRuntime();
        final RubyString padStr = arg1.convertToString();
        final ByteList pad = padStr.value;
        final Encoding enc = this.checkEncoding(padStr);
        final int padCharLen = StringSupport.strLengthFromRubyString(padStr, enc);
        if (pad.getRealSize() == 0 || padCharLen == 0) {
            throw runtime.newArgumentError("zero width padding");
        }
        final int width = RubyNumeric.num2int(arg0);
        final RubyString result = this.justifyCommon(runtime, pad, padCharLen, padStr.singleByteOptimizable(), enc, width, jflag);
        if (result.strLength() > this.strLength()) {
            result.infectBy(padStr);
        }
        final int cr = CodeRangeSupport.codeRangeAnd(this.getCodeRange(), padStr.getCodeRange());
        if (cr != 48) {
            result.setCodeRange(cr);
        }
        return result;
    }
    
    private RubyString justifyCommon(final Ruby runtime, final ByteList pad, final int padCharLen, final boolean padSinglebyte, final Encoding enc, final int width, final int jflag) {
        final int len = StringSupport.strLengthFromRubyString(this, enc);
        if (width < 0 || len >= width) {
            return this.strDup(runtime);
        }
        int n = width - len;
        int llen = (jflag == 108) ? 0 : ((jflag == 114) ? n : (n / 2));
        int rlen = n - llen;
        final int padP = pad.getBegin();
        final int padLen = pad.getRealSize();
        final byte[] padBytes = pad.getUnsafeBytes();
        final ByteList res = new ByteList(this.value.getRealSize() + n * padLen / padCharLen + 2);
        int p = res.getBegin();
        final byte[] bytes = res.getUnsafeBytes();
        while (llen > 0) {
            if (padLen <= 1) {
                bytes[p++] = padBytes[padP];
                --llen;
            }
            else {
                if (llen <= padCharLen) {
                    final int padPP = padSinglebyte ? (padP + llen) : StringSupport.nth(enc, padBytes, padP, padP + padLen, llen);
                    n = padPP - padP;
                    System.arraycopy(padBytes, padP, bytes, p, n);
                    p += n;
                    break;
                }
                System.arraycopy(padBytes, padP, bytes, p, padLen);
                p += padLen;
                llen -= padCharLen;
            }
        }
        System.arraycopy(this.value.getUnsafeBytes(), this.value.getBegin(), bytes, p, this.value.getRealSize());
        p += this.value.getRealSize();
        while (rlen > 0) {
            if (padLen <= 1) {
                bytes[p++] = padBytes[padP];
                --rlen;
            }
            else {
                if (rlen <= padCharLen) {
                    final int padPP = padSinglebyte ? (padP + rlen) : StringSupport.nth(enc, padBytes, padP, padP + padLen, rlen);
                    n = padPP - padP;
                    System.arraycopy(padBytes, padP, bytes, p, n);
                    p += n;
                    break;
                }
                System.arraycopy(padBytes, padP, bytes, p, padLen);
                p += padLen;
                rlen -= padCharLen;
            }
        }
        res.setRealSize(p);
        final RubyString result = new RubyString(runtime, this.getMetaClass(), res);
        if (result.strLength() > this.strLength()) {
            result.infectBy(this);
        }
        result.associateEncoding(enc);
        return result;
    }
    
    public IRubyObject ljust(final IRubyObject arg0) {
        return this.ljust19(arg0);
    }
    
    public IRubyObject ljust(final IRubyObject arg0, final IRubyObject arg1) {
        return this.ljust19(arg0, arg1);
    }
    
    @JRubyMethod(name = { "ljust" })
    public IRubyObject ljust19(final IRubyObject arg0) {
        return this.justify19(this.getRuntime(), arg0, 108);
    }
    
    @JRubyMethod(name = { "ljust" })
    public IRubyObject ljust19(final IRubyObject arg0, final IRubyObject arg1) {
        return this.justify19(arg0, arg1, 108);
    }
    
    public IRubyObject rjust(final IRubyObject arg0) {
        return this.rjust19(arg0);
    }
    
    public IRubyObject rjust(final IRubyObject arg0, final IRubyObject arg1) {
        return this.rjust19(arg0, arg1);
    }
    
    @JRubyMethod(name = { "rjust" })
    public IRubyObject rjust19(final IRubyObject arg0) {
        return this.justify19(this.getRuntime(), arg0, 114);
    }
    
    @JRubyMethod(name = { "rjust" })
    public IRubyObject rjust19(final IRubyObject arg0, final IRubyObject arg1) {
        return this.justify19(arg0, arg1, 114);
    }
    
    public IRubyObject center(final IRubyObject arg0) {
        return this.center19(arg0);
    }
    
    public IRubyObject center(final IRubyObject arg0, final IRubyObject arg1) {
        return this.center19(arg0, arg1);
    }
    
    @JRubyMethod(name = { "center" })
    public IRubyObject center19(final IRubyObject arg0) {
        return this.justify19(this.getRuntime(), arg0, 99);
    }
    
    @JRubyMethod(name = { "center" })
    public IRubyObject center19(final IRubyObject arg0, final IRubyObject arg1) {
        return this.justify19(arg0, arg1, 99);
    }
    
    @JRubyMethod(reads = { FrameField.BACKREF }, writes = { FrameField.BACKREF })
    public IRubyObject partition(final ThreadContext context, final Block block) {
        return RubyEnumerable.partition(context, this, block);
    }
    
    @JRubyMethod(reads = { FrameField.BACKREF }, writes = { FrameField.BACKREF })
    public IRubyObject partition(final ThreadContext context, final IRubyObject arg, final Block block) {
        final Ruby runtime = context.runtime;
        int pos;
        RubyString sep;
        if (arg instanceof RubyRegexp) {
            final RubyRegexp regex = (RubyRegexp)arg;
            pos = regex.search19(context, this, 0, false);
            if (pos < 0) {
                return this.partitionMismatch(runtime);
            }
            sep = (RubyString)this.subpat19(runtime, context, regex);
            if (pos == 0 && sep.value.getRealSize() == 0) {
                return this.partitionMismatch(runtime);
            }
        }
        else {
            final IRubyObject tmp = arg.checkStringType();
            if (tmp.isNil()) {
                throw runtime.newTypeError("type mismatch: " + arg.getMetaClass().getName() + " given");
            }
            sep = (RubyString)tmp;
            pos = StringSupport.index(this, sep, 0, this.checkEncoding(sep));
            if (pos < 0) {
                return this.partitionMismatch(runtime);
            }
        }
        return RubyArray.newArray(runtime, new IRubyObject[] { this.makeShared19(runtime, 0, pos), sep, this.makeShared19(runtime, pos + sep.value.getRealSize(), this.value.getRealSize() - pos - sep.value.getRealSize()) });
    }
    
    private IRubyObject partitionMismatch(final Ruby runtime) {
        final Encoding enc = this.getEncoding();
        return RubyArray.newArray(runtime, new IRubyObject[] { this, newEmptyString(runtime, enc), newEmptyString(runtime, enc) });
    }
    
    @JRubyMethod(name = { "rpartition" }, reads = { FrameField.BACKREF }, writes = { FrameField.BACKREF })
    public IRubyObject rpartition(final ThreadContext context, final IRubyObject arg) {
        final Ruby runtime = context.runtime;
        int pos;
        RubyString sep;
        if (arg instanceof RubyRegexp) {
            final RubyRegexp regex = (RubyRegexp)arg;
            pos = regex.search19(context, this, this.value.getRealSize(), true);
            if (pos < 0) {
                return this.rpartitionMismatch(runtime);
            }
            sep = (RubyString)RubyRegexp.nth_match(0, context.getBackRef());
        }
        else {
            final IRubyObject tmp = arg.checkStringType();
            if (tmp.isNil()) {
                throw runtime.newTypeError("type mismatch: " + arg.getMetaClass().getName() + " given");
            }
            sep = (RubyString)tmp;
            pos = StringSupport.rindex(this.value, StringSupport.strLengthFromRubyString(this, this.checkEncoding(sep)), StringSupport.strLengthFromRubyString(sep, this.checkEncoding(sep)), this.subLength(this.value.getRealSize()), sep, this.checkEncoding(sep));
            if (pos < 0) {
                return this.rpartitionMismatch(runtime);
            }
        }
        return RubyArray.newArray(runtime, new IRubyObject[] { this.substr19(runtime, 0, pos), sep, this.substr19(runtime, pos + sep.strLength(), this.value.getRealSize()) });
    }
    
    private IRubyObject rpartitionMismatch(final Ruby runtime) {
        final Encoding enc = this.getEncoding();
        return RubyArray.newArray(runtime, new IRubyObject[] { newEmptyString(runtime, enc), newEmptyString(runtime, enc), this });
    }
    
    public IRubyObject chop(final ThreadContext context) {
        return this.chop19(context);
    }
    
    public IRubyObject chop_bang(final ThreadContext context) {
        return this.chop_bang19(context);
    }
    
    @JRubyMethod(name = { "chop" })
    public IRubyObject chop19(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        if (this.value.getRealSize() == 0) {
            return newEmptyString(runtime, this.getMetaClass(), this.value.getEncoding()).infectBy(this);
        }
        return this.makeShared19(runtime, 0, StringSupport.choppedLength(this));
    }
    
    @JRubyMethod(name = { "chop!" })
    public IRubyObject chop_bang19(final ThreadContext context) {
        this.modifyAndKeepCodeRange();
        if (this.size() > 0) {
            final int len = StringSupport.choppedLength(this);
            this.value.realSize(len);
            if (this.getCodeRange() != 16) {
                this.clearCodeRange();
            }
            return this;
        }
        return context.nil;
    }
    
    public RubyString chomp(final ThreadContext context) {
        return this.chomp19(context);
    }
    
    public RubyString chomp(final ThreadContext context, final IRubyObject arg0) {
        return this.chomp19(context, arg0);
    }
    
    public IRubyObject chomp_bang(final ThreadContext context) {
        return this.chomp_bang19(context);
    }
    
    public IRubyObject chomp_bang(final ThreadContext context, final IRubyObject arg0) {
        return this.chomp_bang19(context, arg0);
    }
    
    @JRubyMethod(name = { "chomp" })
    public RubyString chomp19(final ThreadContext context) {
        final RubyString str = this.strDup(context.runtime);
        str.chomp_bang19(context);
        return str;
    }
    
    @JRubyMethod(name = { "chomp" })
    public RubyString chomp19(final ThreadContext context, final IRubyObject arg0) {
        final RubyString str = this.strDup(context.runtime);
        str.chomp_bang19(context, arg0);
        return str;
    }
    
    @JRubyMethod(name = { "chomp!" })
    public IRubyObject chomp_bang19(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        if (this.value.getRealSize() == 0) {
            return runtime.getNil();
        }
        final IRubyObject rsObj = runtime.getGlobalVariables().get("$/");
        if (rsObj == runtime.getGlobalVariables().getDefaultSeparator()) {
            return this.smartChopBangCommon19(runtime);
        }
        return this.chompBangCommon19(runtime, rsObj);
    }
    
    @JRubyMethod(name = { "chomp!" })
    public IRubyObject chomp_bang19(final ThreadContext context, final IRubyObject arg0) {
        this.modifyCheck();
        final Ruby runtime = context.runtime;
        if (this.value.getRealSize() == 0) {
            return runtime.getNil();
        }
        return this.chompBangCommon19(runtime, arg0);
    }
    
    private IRubyObject chompBangCommon19(final Ruby runtime, final IRubyObject rsObj) {
        if (rsObj.isNil()) {
            return rsObj;
        }
        final RubyString rs = rsObj.convertToString();
        final int p = this.value.getBegin();
        int len = this.value.getRealSize();
        final int end = p + len;
        final byte[] bytes = this.value.getUnsafeBytes();
        final int rslen = rs.value.getRealSize();
        if (rslen == 0) {
            while (len > 0 && bytes[p + len - 1] == 10) {
                if (--len > 0 && bytes[p + len - 1] == 13) {
                    --len;
                }
            }
            if (len < this.value.getRealSize()) {
                this.keepCodeRange();
                this.view(0, len);
                return this;
            }
            return runtime.getNil();
        }
        else {
            if (rslen > len) {
                return runtime.getNil();
            }
            final byte newline = rs.value.getUnsafeBytes()[rslen - 1];
            if (rslen == 1 && newline == 10) {
                return this.smartChopBangCommon19(runtime);
            }
            final Encoding enc = this.checkEncoding(rs);
            if (rs.scanForCodeRange() == 48) {
                return runtime.getNil();
            }
            final int pp = end - rslen;
            if ((bytes[p + len - 1] != newline || rslen > 1) && !this.value.endsWith(rs.value)) {
                return runtime.getNil();
            }
            if (enc.leftAdjustCharHead(bytes, p, pp, end) != pp) {
                return runtime.getNil();
            }
            if (this.getCodeRange() != 16) {
                this.clearCodeRange();
            }
            this.view(0, this.value.getRealSize() - rslen);
            return this;
        }
    }
    
    private IRubyObject smartChopBangCommon19(final Ruby runtime) {
        final int p = this.value.getBegin();
        int len = this.value.getRealSize();
        int end = p + len;
        final byte[] bytes = this.value.getUnsafeBytes();
        final Encoding enc = this.value.getEncoding();
        this.keepCodeRange();
        if (enc.minLength() > 1) {
            int pp = enc.leftAdjustCharHead(bytes, p, end - enc.minLength(), end);
            if (enc.isNewLine(bytes, pp, end)) {
                end = pp;
            }
            pp = end - enc.minLength();
            if (pp >= p) {
                pp = enc.leftAdjustCharHead(bytes, p, pp, end);
                if (StringSupport.preciseLength(enc, bytes, pp, end) > 0 && enc.mbcToCode(bytes, pp, end) == 13) {
                    end = pp;
                }
            }
            if (end == p + this.value.getRealSize()) {
                this.modifyCheck();
                return runtime.getNil();
            }
            len = end - p;
            this.view(0, len);
        }
        else if (bytes[p + len - 1] == 10) {
            if (--len > 0 && bytes[p + len - 1] == 13) {
                --len;
            }
            this.view(0, len);
        }
        else {
            if (bytes[p + len - 1] != 13) {
                this.modifyCheck();
                return runtime.getNil();
            }
            --len;
            this.view(0, len);
        }
        return this;
    }
    
    public IRubyObject lstrip(final ThreadContext context) {
        return this.lstrip19(context);
    }
    
    public IRubyObject lstrip_bang(final ThreadContext context) {
        return this.lstrip_bang19(context);
    }
    
    @JRubyMethod(name = { "lstrip" })
    public IRubyObject lstrip19(final ThreadContext context) {
        final RubyString str = this.strDup(context.runtime);
        str.lstrip_bang19(context);
        return str;
    }
    
    @JRubyMethod(name = { "lstrip!" })
    public IRubyObject lstrip_bang19(final ThreadContext context) {
        this.modifyCheck();
        final ByteList value = this.value;
        if (value.getRealSize() == 0) {
            return context.nil;
        }
        final int s = value.getBegin();
        final int end = s + value.getRealSize();
        final byte[] bytes = value.getUnsafeBytes();
        final Encoding enc = EncodingUtils.STR_ENC_GET(this);
        IRubyObject result;
        if (this.singleByteOptimizable(enc)) {
            result = this.singleByteLStrip(context.runtime, bytes, s, end);
        }
        else {
            result = this.multiByteLStrip(context.runtime, enc, bytes, s, end);
        }
        this.keepCodeRange();
        return result;
    }
    
    private IRubyObject singleByteLStrip(final Ruby runtime, final byte[] bytes, final int s, final int end) {
        int p;
        for (p = s; p < end && RubyString.ASCII.isSpace(bytes[p] & 0xFF); ++p) {}
        if (p > s) {
            this.view(p - s, end - p);
            return this;
        }
        return runtime.getNil();
    }
    
    private IRubyObject multiByteLStrip(final Ruby runtime, final Encoding enc, final byte[] bytes, final int s, final int end) {
        int p;
        int c;
        for (p = s; p < end; p += StringSupport.codeLength(enc, c)) {
            c = StringSupport.codePoint(runtime, enc, bytes, p, end);
            if (!RubyString.ASCII.isSpace(c)) {
                break;
            }
        }
        if (p > s) {
            this.view(p - s, end - p);
            return this;
        }
        return runtime.getNil();
    }
    
    public IRubyObject rstrip(final ThreadContext context) {
        return this.rstrip19(context);
    }
    
    public IRubyObject rstrip_bang(final ThreadContext context) {
        return this.rstrip_bang19(context);
    }
    
    @JRubyMethod(name = { "rstrip" })
    public IRubyObject rstrip19(final ThreadContext context) {
        final RubyString str = this.strDup(context.runtime);
        str.rstrip_bang19(context);
        return str;
    }
    
    @JRubyMethod(name = { "rstrip!" })
    public IRubyObject rstrip_bang19(final ThreadContext context) {
        this.modifyCheck();
        final Ruby runtime = context.runtime;
        if (this.value.getRealSize() == 0) {
            return runtime.getNil();
        }
        this.checkDummyEncoding();
        final Encoding enc = EncodingUtils.STR_ENC_GET(this);
        final IRubyObject result = this.singleByteOptimizable(enc) ? this.singleByteRStrip19(runtime) : this.multiByteRStrip19(runtime, context);
        this.keepCodeRange();
        return result;
    }
    
    private IRubyObject singleByteRStrip19(final Ruby runtime) {
        final byte[] bytes = this.value.getUnsafeBytes();
        final int start = this.value.getBegin();
        final int end = start + this.value.getRealSize();
        int endp;
        for (endp = end - 1; endp >= start && (bytes[endp] == 0 || RubyString.ASCII.isSpace(bytes[endp] & 0xFF)); --endp) {}
        if (endp < end - 1) {
            this.view(0, endp - start + 1);
            return this;
        }
        return runtime.getNil();
    }
    
    private IRubyObject multiByteRStrip19(final Ruby runtime, final ThreadContext context) {
        byte[] bytes;
        int start;
        int end;
        Encoding enc;
        int endp;
        int prev;
        int point;
        for (bytes = this.value.getUnsafeBytes(), start = this.value.getBegin(), end = start + this.value.getRealSize(), enc = EncodingUtils.STR_ENC_GET(this), endp = end; (prev = enc.prevCharHead(bytes, start, endp, end)) != -1; endp = prev) {
            point = StringSupport.codePoint(runtime, enc, bytes, prev, end);
            if (point != 0 && !RubyString.ASCII.isSpace(point)) {
                break;
            }
        }
        if (endp < end) {
            this.view(0, endp - start);
            return this;
        }
        return runtime.getNil();
    }
    
    public IRubyObject strip(final ThreadContext context) {
        return this.strip19(context);
    }
    
    public IRubyObject strip_bang(final ThreadContext context) {
        return this.strip_bang19(context);
    }
    
    @JRubyMethod(name = { "strip" })
    public IRubyObject strip19(final ThreadContext context) {
        final RubyString str = this.strDup(context.runtime);
        str.strip_bang19(context);
        return str;
    }
    
    @JRubyMethod(name = { "strip!" })
    public IRubyObject strip_bang19(final ThreadContext context) {
        this.modifyCheck();
        final IRubyObject left = this.lstrip_bang19(context);
        final IRubyObject right = this.rstrip_bang19(context);
        return (left.isNil() && right.isNil()) ? context.runtime.getNil() : this;
    }
    
    public IRubyObject count(final ThreadContext context) {
        return this.count19(context);
    }
    
    public IRubyObject count(final ThreadContext context, final IRubyObject arg) {
        return this.count19(context, arg);
    }
    
    public IRubyObject count(final ThreadContext context, final IRubyObject[] args) {
        return this.count19(context, args);
    }
    
    @JRubyMethod(name = { "count" })
    public IRubyObject count19(final ThreadContext context) {
        throw context.runtime.newArgumentError("wrong number of arguments");
    }
    
    @JRubyMethod(name = { "count" })
    public IRubyObject count19(final ThreadContext context, final IRubyObject arg) {
        final Ruby runtime = context.runtime;
        final RubyString countStr = arg.convertToString();
        final ByteList countValue = countStr.getByteList();
        final Encoding enc = this.checkEncoding(countStr);
        if (countValue.length() == 1 && enc.isAsciiCompatible()) {
            final byte[] countBytes = countValue.unsafeBytes();
            final int begin = countValue.begin();
            final int size = countValue.length();
            if (enc.isReverseMatchAllowed(countBytes, begin, begin + size) && !this.isCodeRangeBroken()) {
                if (this.value.length() == 0) {
                    return RubyFixnum.zero(runtime);
                }
                int n = 0;
                final int[] len_p = { 0 };
                final int c = EncodingUtils.encCodepointLength(runtime, countBytes, begin, begin + size, len_p, enc);
                final byte[] bytes = this.value.unsafeBytes();
                int i = this.value.begin();
                final int end = i + this.value.length();
                while (i < end) {
                    if ((bytes[i++] & 0xFF) == c) {
                        ++n;
                    }
                }
                return RubyFixnum.newFixnum(runtime, n);
            }
        }
        final boolean[] table = new boolean[257];
        final StringSupport.TrTables tables = StringSupport.trSetupTable(countValue, runtime, table, null, true, enc);
        return runtime.newFixnum(StringSupport.strCount(this.value, runtime, table, tables, enc));
    }
    
    @JRubyMethod(name = { "count" }, required = 1, rest = true)
    public IRubyObject count19(final ThreadContext context, final IRubyObject[] args) {
        final Ruby runtime = context.runtime;
        if (this.value.length() == 0) {
            return RubyFixnum.zero(runtime);
        }
        RubyString countStr = args[0].convertToString();
        Encoding enc = this.checkEncoding(countStr);
        final boolean[] table = new boolean[257];
        StringSupport.TrTables tables = StringSupport.trSetupTable(countStr.value, runtime, table, null, true, enc);
        for (int i = 1; i < args.length; ++i) {
            countStr = args[i].convertToString();
            enc = this.checkEncoding(countStr);
            tables = StringSupport.trSetupTable(countStr.value, runtime, table, tables, false, enc);
        }
        return runtime.newFixnum(StringSupport.strCount(this.value, runtime, table, tables, enc));
    }
    
    public IRubyObject delete(final ThreadContext context) {
        return this.delete19(context);
    }
    
    public IRubyObject delete(final ThreadContext context, final IRubyObject arg) {
        return this.delete19(context, arg);
    }
    
    public IRubyObject delete(final ThreadContext context, final IRubyObject[] args) {
        return this.delete19(context, args);
    }
    
    public IRubyObject delete_bang(final ThreadContext context) {
        return this.delete_bang19(context);
    }
    
    public IRubyObject delete_bang(final ThreadContext context, final IRubyObject arg) {
        return this.delete_bang19(context, arg);
    }
    
    public IRubyObject delete_bang(final ThreadContext context, final IRubyObject[] args) {
        return this.delete_bang19(context, args);
    }
    
    @JRubyMethod(name = { "delete" })
    public IRubyObject delete19(final ThreadContext context) {
        throw context.runtime.newArgumentError("wrong number of arguments");
    }
    
    @JRubyMethod(name = { "delete" })
    public IRubyObject delete19(final ThreadContext context, final IRubyObject arg) {
        final RubyString str = this.strDup(context.runtime);
        str.delete_bang19(context, arg);
        return str;
    }
    
    @JRubyMethod(name = { "delete" }, required = 1, rest = true)
    public IRubyObject delete19(final ThreadContext context, final IRubyObject[] args) {
        final RubyString str = this.strDup(context.runtime);
        str.delete_bang19(context, args);
        return str;
    }
    
    @JRubyMethod(name = { "delete!" })
    public IRubyObject delete_bang19(final ThreadContext context) {
        throw context.runtime.newArgumentError("wrong number of arguments");
    }
    
    @JRubyMethod(name = { "delete!" })
    public IRubyObject delete_bang19(final ThreadContext context, final IRubyObject arg) {
        final Ruby runtime = context.runtime;
        if (this.value.getRealSize() == 0) {
            return runtime.getNil();
        }
        final RubyString otherStr = arg.convertToString();
        final Encoding enc = this.checkEncoding(otherStr);
        final boolean[] squeeze = new boolean[257];
        final StringSupport.TrTables tables = StringSupport.trSetupTable(otherStr.value, runtime, squeeze, null, true, enc);
        if (StringSupport.delete_bangCommon19(this, runtime, squeeze, tables, enc) == null) {
            return runtime.getNil();
        }
        return this;
    }
    
    @JRubyMethod(name = { "delete!" }, required = 1, rest = true)
    public IRubyObject delete_bang19(final ThreadContext context, final IRubyObject[] args) {
        final Ruby runtime = context.runtime;
        if (this.value.getRealSize() == 0) {
            return runtime.getNil();
        }
        Encoding enc = null;
        final boolean[] squeeze = new boolean[257];
        StringSupport.TrTables tables = null;
        for (int i = 0; i < args.length; ++i) {
            final RubyString otherStr = args[i].convertToString();
            enc = this.checkEncoding(otherStr);
            tables = StringSupport.trSetupTable(otherStr.value, runtime, squeeze, tables, i == 0, enc);
        }
        if (StringSupport.delete_bangCommon19(this, runtime, squeeze, tables, enc) == null) {
            return context.nil;
        }
        return this;
    }
    
    public IRubyObject squeeze(final ThreadContext context) {
        return this.squeeze19(context);
    }
    
    public IRubyObject squeeze(final ThreadContext context, final IRubyObject arg) {
        return this.squeeze19(context, arg);
    }
    
    public IRubyObject squeeze(final ThreadContext context, final IRubyObject[] args) {
        return this.squeeze19(context, args);
    }
    
    public IRubyObject squeeze_bang(final ThreadContext context) {
        return this.squeeze_bang19(context);
    }
    
    public IRubyObject squeeze_bang(final ThreadContext context, final IRubyObject arg) {
        return this.squeeze_bang19(context, arg);
    }
    
    public IRubyObject squeeze_bang(final ThreadContext context, final IRubyObject[] args) {
        return this.squeeze_bang19(context, args);
    }
    
    @JRubyMethod(name = { "squeeze" })
    public IRubyObject squeeze19(final ThreadContext context) {
        final RubyString str = this.strDup(context.runtime);
        str.squeeze_bang19(context);
        return str;
    }
    
    @JRubyMethod(name = { "squeeze" }, required = 1)
    public IRubyObject squeeze19(final ThreadContext context, final IRubyObject arg) {
        final RubyString str = this.strDup(context.runtime);
        str.squeeze_bang19(context, arg);
        return str;
    }
    
    @JRubyMethod(name = { "squeeze" }, required = 2, rest = true)
    public IRubyObject squeeze19(final ThreadContext context, final IRubyObject[] args) {
        final RubyString str = this.strDup(context.runtime);
        str.squeeze_bang19(context, args);
        return str;
    }
    
    @JRubyMethod(name = { "squeeze!" })
    public IRubyObject squeeze_bang19(final ThreadContext context) {
        if (this.value.getRealSize() == 0) {
            this.modifyCheck();
            return context.nil;
        }
        final Ruby runtime = context.runtime;
        final boolean[] squeeze = new boolean[256];
        for (int i = 0; i < 256; ++i) {
            squeeze[i] = true;
        }
        this.modifyAndKeepCodeRange();
        if (this.singleByteOptimizable()) {
            if (!StringSupport.singleByteSqueeze(this.value, squeeze)) {
                return runtime.getNil();
            }
        }
        else if (!StringSupport.multiByteSqueeze(runtime, this.value, squeeze, null, this.value.getEncoding(), false)) {
            return runtime.getNil();
        }
        return this;
    }
    
    @JRubyMethod(name = { "squeeze!" }, required = 1)
    public IRubyObject squeeze_bang19(final ThreadContext context, final IRubyObject arg) {
        final Ruby runtime = context.runtime;
        final RubyString otherStr = arg.convertToString();
        final boolean[] squeeze = new boolean[257];
        final StringSupport.TrTables tables = StringSupport.trSetupTable(otherStr.value, runtime, squeeze, null, true, this.checkEncoding(otherStr));
        this.modifyAndKeepCodeRange();
        if (this.singleByteOptimizable() && otherStr.singleByteOptimizable()) {
            if (!StringSupport.singleByteSqueeze(this.value, squeeze)) {
                return runtime.getNil();
            }
        }
        else if (!StringSupport.multiByteSqueeze(runtime, this.value, squeeze, tables, this.value.getEncoding(), true)) {
            return runtime.getNil();
        }
        return this;
    }
    
    @JRubyMethod(name = { "squeeze!" }, rest = true, required = 2)
    public IRubyObject squeeze_bang19(final ThreadContext context, final IRubyObject[] args) {
        if (this.value.getRealSize() == 0) {
            this.modifyCheck();
            return context.nil;
        }
        final Ruby runtime = context.runtime;
        RubyString otherStr = args[0].convertToString();
        Encoding enc = this.checkEncoding(otherStr);
        final boolean[] squeeze = new boolean[257];
        StringSupport.TrTables tables = StringSupport.trSetupTable(otherStr.value, runtime, squeeze, null, true, enc);
        boolean singleByte = this.singleByteOptimizable() && otherStr.singleByteOptimizable();
        for (int i = 1; i < args.length; ++i) {
            otherStr = args[i].convertToString();
            enc = this.checkEncoding(otherStr);
            singleByte = (singleByte && otherStr.singleByteOptimizable());
            tables = StringSupport.trSetupTable(otherStr.value, runtime, squeeze, tables, false, enc);
        }
        this.modifyAndKeepCodeRange();
        if (singleByte) {
            if (!StringSupport.singleByteSqueeze(this.value, squeeze)) {
                return runtime.getNil();
            }
        }
        else if (!StringSupport.multiByteSqueeze(runtime, this.value, squeeze, tables, enc, true)) {
            return runtime.getNil();
        }
        return this;
    }
    
    public IRubyObject tr(final ThreadContext context, final IRubyObject src, final IRubyObject repl) {
        return this.tr19(context, src, repl);
    }
    
    public IRubyObject tr_bang(final ThreadContext context, final IRubyObject src, final IRubyObject repl) {
        return this.tr_bang19(context, src, repl);
    }
    
    @JRubyMethod(name = { "tr" })
    public IRubyObject tr19(final ThreadContext context, final IRubyObject src, final IRubyObject repl) {
        final RubyString str = this.strDup(context.runtime);
        str.trTrans19(context, src, repl, false);
        return str;
    }
    
    @JRubyMethod(name = { "tr!" })
    public IRubyObject tr_bang19(final ThreadContext context, final IRubyObject src, final IRubyObject repl) {
        return this.trTrans19(context, src, repl, false);
    }
    
    private IRubyObject trTrans19(final ThreadContext context, final IRubyObject src, final IRubyObject repl, final boolean sflag) {
        final Ruby runtime = context.runtime;
        final RubyString replStr = repl.convertToString();
        final ByteList replList = replStr.value;
        final RubyString srcStr = src.convertToString();
        if (this.value.getRealSize() == 0) {
            return runtime.getNil();
        }
        if (replList.getRealSize() == 0) {
            return this.delete_bang19(context, src);
        }
        final CodeRangeable ret = StringSupport.trTransHelper(runtime, this, srcStr, replStr, sflag);
        if (ret == null) {
            return runtime.getNil();
        }
        return (IRubyObject)ret;
    }
    
    public IRubyObject tr_s(final ThreadContext context, final IRubyObject src, final IRubyObject repl) {
        return this.tr_s19(context, src, repl);
    }
    
    public IRubyObject tr_s_bang(final ThreadContext context, final IRubyObject src, final IRubyObject repl) {
        return this.tr_s_bang19(context, src, repl);
    }
    
    @JRubyMethod(name = { "tr_s" })
    public IRubyObject tr_s19(final ThreadContext context, final IRubyObject src, final IRubyObject repl) {
        final RubyString str = this.strDup(context.runtime);
        str.trTrans19(context, src, repl, true);
        return str;
    }
    
    @JRubyMethod(name = { "tr_s!" })
    public IRubyObject tr_s_bang19(final ThreadContext context, final IRubyObject src, final IRubyObject repl) {
        return this.trTrans19(context, src, repl, true);
    }
    
    public IRubyObject each_line(final ThreadContext context, final Block block) {
        return this.each_lineCommon(context, context.runtime.getGlobalVariables().get("$/"), block);
    }
    
    public IRubyObject each_line(final ThreadContext context, final IRubyObject arg, final Block block) {
        return this.each_lineCommon(context, arg, block);
    }
    
    public IRubyObject each_lineCommon(final ThreadContext context, final IRubyObject sep, final Block block) {
        final Ruby runtime = context.runtime;
        if (sep.isNil()) {
            block.yield(context, this);
            return this;
        }
        final RubyString sepStr = sep.convertToString();
        final ByteList sepValue = sepStr.value;
        final int rslen = sepValue.getRealSize();
        byte newline;
        if (rslen == 0) {
            newline = 10;
        }
        else {
            newline = sepValue.getUnsafeBytes()[sepValue.getBegin() + rslen - 1];
        }
        int p = this.value.getBegin();
        final int end = p + this.value.getRealSize();
        final int ptr = p;
        int s = p;
        final int len = this.value.getRealSize();
        final byte[] bytes = this.value.getUnsafeBytes();
        for (p += rslen; p < end; ++p) {
            if (rslen == 0 && bytes[p] == 10) {
                if (++p == end) {
                    continue;
                }
                if (bytes[p] != 10) {
                    continue;
                }
                while (p < end && bytes[p] == 10) {
                    ++p;
                }
            }
            if (ptr < p && bytes[p - 1] == newline && (rslen <= 1 || ByteList.memcmp(sepValue.getUnsafeBytes(), sepValue.getBegin(), rslen, bytes, p - rslen, rslen) == 0)) {
                block.yield(context, this.makeShared(runtime, s - ptr, p - s).infectBy(this));
                this.modifyCheck(bytes, len);
                s = p;
            }
        }
        if (s != end) {
            if (p > end) {
                p = end;
            }
            block.yield(context, this.makeShared(runtime, s - ptr, p - s).infectBy(this));
        }
        return this;
    }
    
    @JRubyMethod(name = { "each_line" })
    public IRubyObject each_line19(final ThreadContext context, final Block block) {
        return StringSupport.rbStrEnumerateLines(this, context, "each_line", context.runtime.getGlobalVariables().get("$/"), block, false);
    }
    
    @JRubyMethod(name = { "each_line" })
    public IRubyObject each_line19(final ThreadContext context, final IRubyObject arg, final Block block) {
        return StringSupport.rbStrEnumerateLines(this, context, "each_line", arg, block, false);
    }
    
    public IRubyObject lines(final ThreadContext context, final Block block) {
        return this.lines20(context, block);
    }
    
    public IRubyObject lines(final ThreadContext context, final IRubyObject arg, final Block block) {
        return this.lines20(context, arg, block);
    }
    
    @JRubyMethod(name = { "lines" })
    public IRubyObject lines20(final ThreadContext context, final Block block) {
        return StringSupport.rbStrEnumerateLines(this, context, "lines", context.runtime.getGlobalVariables().get("$/"), block, true);
    }
    
    @JRubyMethod(name = { "lines" })
    public IRubyObject lines20(final ThreadContext context, final IRubyObject arg, final Block block) {
        return StringSupport.rbStrEnumerateLines(this, context, "lines", arg, block, true);
    }
    
    public RubyString each_byte(final ThreadContext context, final Block block) {
        final Ruby runtime = context.runtime;
        for (int i = 0; i < this.value.length(); ++i) {
            block.yield(context, runtime.newFixnum(this.value.get(i) & 0xFF));
        }
        return this;
    }
    
    @JRubyMethod(name = { "each_byte" })
    public IRubyObject each_byte19(final ThreadContext context, final Block block) {
        return this.enumerateBytes(context, "each_byte", block, false);
    }
    
    @JRubyMethod
    public IRubyObject bytes(final ThreadContext context, final Block block) {
        return this.enumerateBytes(context, "bytes", block, true);
    }
    
    @JRubyMethod(name = { "each_char" })
    public IRubyObject each_char19(final ThreadContext context, final Block block) {
        return this.enumerateChars(context, "each_char", block, false);
    }
    
    @JRubyMethod(name = { "chars" })
    public IRubyObject chars19(final ThreadContext context, final Block block) {
        return this.enumerateChars(context, "chars", block, true);
    }
    
    private RubyEnumerator.SizeFn eachCharSizeFn() {
        final RubyString self = this;
        return new RubyEnumerator.SizeFn() {
            @Override
            public IRubyObject size(final IRubyObject[] args) {
                return self.length();
            }
        };
    }
    
    @JRubyMethod
    public IRubyObject each_codepoint(final ThreadContext context, final Block block) {
        return this.enumerateCodepoints(context, "each_codepoint", block, false);
    }
    
    @JRubyMethod
    public IRubyObject codepoints(final ThreadContext context, final Block block) {
        return this.enumerateCodepoints(context, "codepoints", block, true);
    }
    
    private IRubyObject enumerateChars(final ThreadContext context, final String name, final Block block, boolean wantarray) {
        final Ruby runtime = context.runtime;
        final IRubyObject orig;
        RubyString str = (RubyString)(orig = this);
        RubyArray ary = null;
        str = this.strDup(runtime);
        final ByteList strByteList = str.getByteList();
        final byte[] ptrBytes = strByteList.unsafeBytes();
        final int ptr = strByteList.begin();
        final int len = strByteList.getRealSize();
        final Encoding enc = str.getEncoding();
        if (block.isGiven()) {
            if (wantarray) {
                runtime.getWarnings().warning("passing a block to String#chars is deprecated");
                wantarray = false;
            }
        }
        else {
            if (!wantarray) {
                return RubyEnumerator.enumeratorizeWithSize(context, this, name, this.eachCharSizeFn());
            }
            ary = RubyArray.newArray(runtime, str.length().getLongValue());
        }
        switch (this.getCodeRange()) {
            case 16:
            case 32: {
                int n;
                for (int i = 0; i < len; i += n) {
                    n = StringSupport.encFastMBCLen(ptrBytes, ptr + i, ptr + len, enc);
                    final IRubyObject substr = str.substr(runtime, i, n);
                    if (wantarray) {
                        ary.push(substr);
                    }
                    else {
                        block.yield(context, substr);
                    }
                }
                break;
            }
            default: {
                int n;
                for (int i = 0; i < len; i += n) {
                    n = StringSupport.length(enc, ptrBytes, ptr + i, ptr + len);
                    final IRubyObject substr = str.substr(runtime, i, n);
                    if (wantarray) {
                        ary.push(substr);
                    }
                    else {
                        block.yield(context, substr);
                    }
                }
                break;
            }
        }
        if (wantarray) {
            return ary;
        }
        return orig;
    }
    
    private IRubyObject enumerateCodepoints(final ThreadContext context, final String name, final Block block, boolean wantarray) {
        final Ruby runtime = context.runtime;
        final IRubyObject orig;
        RubyString str = (RubyString)(orig = this);
        RubyArray ary = null;
        if (this.singleByteOptimizable()) {
            return this.enumerateBytes(context, name, block, wantarray);
        }
        str = newString(runtime, str.getByteList().dup());
        final ByteList strByteList = str.getByteList();
        final byte[] ptrBytes = strByteList.unsafeBytes();
        int ptr = strByteList.begin();
        final int end = ptr + strByteList.getRealSize();
        final Encoding enc = EncodingUtils.STR_ENC_GET(str);
        if (block.isGiven()) {
            if (wantarray) {
                runtime.getWarnings().warning("passing a block to String#codepoints is deprecated");
                wantarray = false;
            }
        }
        else {
            if (!wantarray) {
                return RubyEnumerator.enumeratorizeWithSize(context, str, name, this.eachCodepointSizeFn());
            }
            ary = RubyArray.newArray(runtime, str.length().getLongValue());
        }
        while (ptr < end) {
            final int c = StringSupport.codePoint(runtime, enc, ptrBytes, ptr, end);
            final int n = StringSupport.codeLength(enc, c);
            if (wantarray) {
                ary.push(RubyFixnum.newFixnum(runtime, c));
            }
            else {
                block.yield(context, RubyFixnum.newFixnum(runtime, c));
            }
            ptr += n;
        }
        if (wantarray) {
            return ary;
        }
        return orig;
    }
    
    private IRubyObject enumerateBytes(final ThreadContext context, final String name, final Block block, boolean wantarray) {
        final Ruby runtime = context.runtime;
        final RubyString str = this;
        RubyArray ary = null;
        if (block.isGiven()) {
            if (wantarray) {
                runtime.getWarnings().warning("passing a block to String#bytes is deprecated");
                wantarray = false;
            }
        }
        else {
            if (!wantarray) {
                return RubyEnumerator.enumeratorizeWithSize(context, str, name, this.eachByteSizeFn());
            }
            ary = RubyArray.newBlankArray(runtime, str.size());
        }
        for (int i = 0; i < str.size(); ++i) {
            final RubyFixnum bite = RubyFixnum.newFixnum(runtime, str.getByteList().get(i) & 0xFF);
            if (wantarray) {
                ary.store(i, bite);
            }
            else {
                block.yield(context, bite);
            }
        }
        if (wantarray) {
            return ary;
        }
        return str;
    }
    
    private RubyEnumerator.SizeFn eachCodepointSizeFn() {
        final RubyString self = this;
        return new RubyEnumerator.SizeFn() {
            @Override
            public IRubyObject size(final IRubyObject[] args) {
                return self.length();
            }
        };
    }
    
    private RubySymbol to_sym() {
        final RubySymbol specialCaseIntern = this.checkSpecialCasesIntern(this.value);
        if (specialCaseIntern != null) {
            return specialCaseIntern;
        }
        final RubySymbol symbol = this.getRuntime().getSymbolTable().getSymbol(this.value);
        if (symbol.getBytes() == this.value) {
            this.shareLevel = 2;
        }
        return symbol;
    }
    
    private RubySymbol checkSpecialCasesIntern(final ByteList value) {
        final String[][] opTable = RubyString.opTable19;
        for (int i = 0; i < opTable.length; ++i) {
            final String op = opTable[i][1];
            if (value.toString().equals(op)) {
                return this.getRuntime().getSymbolTable().getSymbol(opTable[i][0]);
            }
        }
        return null;
    }
    
    public RubySymbol intern() {
        return this.intern19();
    }
    
    @JRubyMethod(name = { "to_sym", "intern" })
    public RubySymbol intern19() {
        return this.to_sym();
    }
    
    @JRubyMethod
    public IRubyObject ord(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        return RubyFixnum.newFixnum(runtime, StringSupport.codePoint(runtime, EncodingUtils.STR_ENC_GET(this), this.value.getUnsafeBytes(), this.value.getBegin(), this.value.getBegin() + this.value.getRealSize()));
    }
    
    @JRubyMethod
    public IRubyObject sum(final ThreadContext context) {
        return this.sumCommon(context, 16L);
    }
    
    @JRubyMethod
    public IRubyObject sum(final ThreadContext context, final IRubyObject arg) {
        return this.sumCommon(context, RubyNumeric.num2long(arg));
    }
    
    public IRubyObject sumCommon(final ThreadContext context, final long bits) {
        final Ruby runtime = context.runtime;
        final byte[] bytes = this.value.getUnsafeBytes();
        int p = this.value.getBegin();
        final int len = this.value.getRealSize();
        final int end = p + len;
        if (bits >= 64L) {
            final IRubyObject one = RubyFixnum.one(runtime);
            IRubyObject sum = RubyFixnum.zero(runtime);
            final JavaSites.StringSites sites = sites(context);
            for (CallSite op_plus = sites.op_plus; p < end; sum = op_plus.call(context, sum, sum, RubyFixnum.newFixnum(runtime, bytes[p++] & 0xFF))) {
                this.modifyCheck(bytes, len);
            }
            if (bits != 0L) {
                final IRubyObject mod = sites.op_lshift.call(context, one, one, RubyFixnum.newFixnum(runtime, bits));
                sum = sites.op_and.call(context, sum, sum, sites.op_minus.call(context, mod, mod, one));
            }
            return sum;
        }
        long sum2;
        for (sum2 = 0L; p < end; sum2 += (bytes[p++] & 0xFF)) {
            this.modifyCheck(bytes, len);
        }
        return RubyFixnum.newFixnum(runtime, (bits == 0L) ? sum2 : (sum2 & (1L << (int)bits) - 1L));
    }
    
    @JRubyMethod
    public IRubyObject to_c(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        final RubyString underscore = runtime.newString(new ByteList(new byte[] { 95 }));
        final RubyRegexp underscore_pattern = RubyRegexp.newDummyRegexp(runtime, Numeric.ComplexPatterns.underscores_pat);
        final IRubyObject s = this.gsubCommon19(context, null, underscore, null, underscore_pattern, false, 0, false);
        final RubyArray a = RubyComplex.str_to_c_internal(context, s);
        final IRubyObject first = a.eltInternal(0);
        if (!first.isNil()) {
            return first;
        }
        return RubyComplex.newComplexCanonicalize(context, RubyFixnum.zero(runtime));
    }
    
    @JRubyMethod
    public IRubyObject to_r(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        final RubyString underscore = runtime.newString(new ByteList(new byte[] { 95 }));
        final RubyRegexp underscore_pattern = RubyRegexp.newDummyRegexp(runtime, Numeric.ComplexPatterns.underscores_pat);
        final IRubyObject s = this.gsubCommon19(context, null, underscore, null, underscore_pattern, false, 0, false);
        final RubyArray a = RubyRational.str_to_r_internal(context, s);
        final IRubyObject first = a.eltInternal(0);
        if (!first.isNil()) {
            return first;
        }
        return RubyRational.newRationalCanonicalize(context, RubyFixnum.zero(runtime));
    }
    
    public static RubyString unmarshalFrom(final UnmarshalStream input) throws IOException {
        final RubyString result = newString(input.getRuntime(), input.unmarshalString());
        input.registerLinkTarget(result);
        return result;
    }
    
    @JRubyMethod
    public RubyArray unpack(final IRubyObject obj) {
        return Pack.unpack(this.getRuntime(), this.value, stringValue(obj).value);
    }
    
    public void empty() {
        this.value = ByteList.EMPTY_BYTELIST;
        this.shareLevel = 2;
    }
    
    @JRubyMethod
    public IRubyObject encoding(final ThreadContext context) {
        return context.runtime.getEncodingService().getEncoding(this.value.getEncoding());
    }
    
    public IRubyObject encode_bang(final ThreadContext context, final IRubyObject arg0) {
        return this.encode_bang(context, new IRubyObject[] { arg0 });
    }
    
    public IRubyObject encode_bang(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1) {
        return this.encode_bang(context, new IRubyObject[] { arg0, arg1 });
    }
    
    public IRubyObject encode_bang(final ThreadContext context, final IRubyObject arg0, final IRubyObject arg1, final IRubyObject arg2) {
        return this.encode_bang(context, new IRubyObject[] { arg0, arg1, arg2 });
    }
    
    @JRubyMethod(name = { "encode!" }, optional = 3)
    public IRubyObject encode_bang(final ThreadContext context, final IRubyObject[] args) {
        this.modify19();
        final IRubyObject[] newstr_p = { this };
        final Encoding encindex = EncodingUtils.strTranscode(context, args, newstr_p);
        if (encindex == null) {
            return this;
        }
        if (newstr_p[0] == this) {
            this.setEncoding(encindex);
            return this;
        }
        this.replace(newstr_p[0]);
        this.setEncoding(encindex);
        return this;
    }
    
    @JRubyMethod
    public IRubyObject encode(final ThreadContext context) {
        return EncodingUtils.strEncode(context, this, new IRubyObject[0]);
    }
    
    @JRubyMethod
    public IRubyObject encode(final ThreadContext context, final IRubyObject arg) {
        return EncodingUtils.strEncode(context, this, arg);
    }
    
    @JRubyMethod
    public IRubyObject encode(final ThreadContext context, final IRubyObject toEncoding, final IRubyObject arg) {
        return EncodingUtils.strEncode(context, this, toEncoding, arg);
    }
    
    @JRubyMethod
    public IRubyObject encode(final ThreadContext context, final IRubyObject toEncoding, final IRubyObject forcedEncoding, final IRubyObject opts) {
        return EncodingUtils.strEncode(context, this, toEncoding, forcedEncoding, opts);
    }
    
    @JRubyMethod
    public IRubyObject force_encoding(final ThreadContext context, final IRubyObject enc) {
        return this.force_encoding(EncodingUtils.rbToEncoding(context, enc));
    }
    
    private IRubyObject force_encoding(final Encoding encoding) {
        this.modifyCheck();
        this.modify19();
        this.associateEncoding(encoding);
        this.clearCodeRange();
        return this;
    }
    
    @JRubyMethod(name = { "valid_encoding?" })
    public IRubyObject valid_encoding_p(final ThreadContext context) {
        return context.runtime.newBoolean(this.scanForCodeRange() != 48);
    }
    
    @JRubyMethod(name = { "ascii_only?" })
    public IRubyObject ascii_only_p(final ThreadContext context) {
        return context.runtime.newBoolean(this.scanForCodeRange() == 16);
    }
    
    @JRubyMethod
    public IRubyObject b(final ThreadContext context) {
        final Encoding encoding = (Encoding)ASCIIEncoding.INSTANCE;
        final RubyString dup = this.strDup(context.runtime);
        dup.modify19();
        dup.setEncoding(encoding);
        return dup;
    }
    
    @JRubyMethod
    public IRubyObject scrub(final ThreadContext context, final Block block) {
        return this.scrub(context, context.nil, block);
    }
    
    @JRubyMethod
    public IRubyObject scrub(final ThreadContext context, final IRubyObject repl, final Block block) {
        final IRubyObject newStr = this.strScrub(context, repl, block);
        if (newStr.isNil()) {
            return this.strDup(context.runtime);
        }
        return newStr;
    }
    
    @JRubyMethod(name = { "scrub!" })
    public IRubyObject scrub_bang(final ThreadContext context, final Block block) {
        return this.scrub_bang(context, context.nil, block);
    }
    
    @JRubyMethod(name = { "scrub!" })
    public IRubyObject scrub_bang(final ThreadContext context, final IRubyObject repl, final Block block) {
        final IRubyObject newStr = this.strScrub(context, repl, block);
        if (!newStr.isNil()) {
            return this.replace(newStr);
        }
        return this;
    }
    
    @JRubyMethod
    @Override
    public IRubyObject freeze(final ThreadContext context) {
        if (this.isFrozen()) {
            return this;
        }
        this.resize(this.size());
        return super.freeze(context);
    }
    
    @Deprecated
    public void setValue(final CharSequence value) {
        this.view(ByteList.plain(value), false);
    }
    
    public void setValue(final ByteList value) {
        this.view(value);
    }
    
    public CharSequence getValue() {
        return this.toString();
    }
    
    public byte[] getBytes() {
        return this.value.bytes();
    }
    
    @Override
    public ByteList getByteList() {
        return this.value;
    }
    
    public String getUnicodeValue() {
        return RubyEncoding.decodeUTF8(this.value.getUnsafeBytes(), this.value.getBegin(), this.value.getRealSize());
    }
    
    public static ByteList encodeBytelist(final CharSequence value, final Encoding encoding) {
        final Charset charset = encoding.getCharset();
        if (charset == null) {
            return EncodingUtils.transcodeString(value.toString(), encoding, 0);
        }
        byte[] bytes;
        if (charset == RubyEncoding.UTF8) {
            bytes = RubyEncoding.encodeUTF8(value);
        }
        else if (charset == RubyEncoding.UTF16) {
            bytes = RubyEncoding.encodeUTF16(value);
        }
        else {
            bytes = RubyEncoding.encode(value, charset);
        }
        return new ByteList(bytes, encoding, false);
    }
    
    @Override
    public Object toJava(final Class target) {
        if (target.isAssignableFrom(String.class)) {
            return this.decodeString();
        }
        if (target.isAssignableFrom(ByteList.class)) {
            return this.value;
        }
        if (target != Character.class && target != Character.TYPE) {
            return super.toJava(target);
        }
        if (this.strLength() != 1) {
            throw this.getRuntime().newArgumentError("could not coerce string of length " + this.strLength() + " (!= 1) into a char");
        }
        return this.decodeString().charAt(0);
    }
    
    public IRubyObject strScrub(final ThreadContext context, IRubyObject repl, final Block block) {
        final Ruby runtime = context.runtime;
        int cr = this.getCodeRange();
        IRubyObject buf = context.nil;
        boolean tainted = false;
        if (cr == 16 || cr == 32) {
            return context.nil;
        }
        final Encoding enc = EncodingUtils.STR_ENC_GET(this);
        if (!repl.isNil()) {
            repl = EncodingUtils.strCompatAndValid(context, repl, enc);
            tainted |= repl.isTaint();
        }
        if (enc.isDummy()) {
            return context.nil;
        }
        final Encoding encidx = enc;
        if (enc.isAsciiCompatible()) {
            final byte[] pBytes = this.value.unsafeBytes();
            int p = this.value.begin();
            final int e = p + this.value.getRealSize();
            int p2 = p;
            byte[] repBytes;
            int rep;
            int replen;
            boolean rep7bit_p;
            if (block.isGiven()) {
                repBytes = null;
                rep = 0;
                replen = 0;
                rep7bit_p = false;
            }
            else if (!repl.isNil()) {
                repBytes = ((RubyString)repl).value.unsafeBytes();
                rep = ((RubyString)repl).value.begin();
                replen = ((RubyString)repl).value.getRealSize();
                rep7bit_p = (((RubyString)repl).getCodeRange() == 16);
            }
            else if (encidx == UTF8Encoding.INSTANCE) {
                repBytes = RubyString.SCRUB_REPL_UTF8;
                rep = 0;
                replen = repBytes.length;
                rep7bit_p = false;
            }
            else {
                repBytes = RubyString.SCRUB_REPL_ASCII;
                rep = 0;
                replen = repBytes.length;
                rep7bit_p = false;
            }
            cr = 16;
            p = StringSupport.searchNonAscii(pBytes, p, e);
            if (p == -1) {
                p = e;
            }
            while (p < e) {
                int ret = enc.length(pBytes, p, e);
                if (StringSupport.MBCLEN_NEEDMORE_P(ret)) {
                    break;
                }
                if (StringSupport.MBCLEN_CHARFOUND_P(ret)) {
                    cr = 32;
                    p += StringSupport.MBCLEN_CHARFOUND_LEN(ret);
                }
                else {
                    if (!StringSupport.MBCLEN_INVALID_P(ret)) {
                        continue;
                    }
                    int clen = enc.maxLength();
                    if (buf.isNil()) {
                        buf = newStringLight(runtime, this.value.getRealSize());
                    }
                    if (p > p2) {
                        ((RubyString)buf).cat(pBytes, p2, p - p2);
                    }
                    if (e - p < clen) {
                        clen = e - p;
                    }
                    if (clen <= 2) {
                        clen = 1;
                    }
                    else {
                        final int q = p;
                        --clen;
                        while (clen > 1) {
                            ret = enc.length(pBytes, q, q + clen);
                            if (StringSupport.MBCLEN_NEEDMORE_P(ret)) {
                                break;
                            }
                            if (StringSupport.MBCLEN_INVALID_P(ret)) {}
                            --clen;
                        }
                    }
                    if (repBytes != null) {
                        ((RubyString)buf).cat(repBytes, rep, replen);
                        if (!rep7bit_p) {
                            cr = 32;
                        }
                    }
                    else {
                        repl = block.yieldSpecific(context, newString(runtime, pBytes, p, clen, enc));
                        repl = EncodingUtils.strCompatAndValid(context, repl, enc);
                        tainted |= repl.isTaint();
                        ((RubyString)buf).cat((RubyString)repl);
                        if (((RubyString)repl).getCodeRange() == 32) {
                            cr = 32;
                        }
                    }
                    p = (p2 = p + clen);
                    p = StringSupport.searchNonAscii(pBytes, p, e);
                    if (p == -1) {
                        p = e;
                        break;
                    }
                    continue;
                }
            }
            if (buf.isNil()) {
                if (p == e) {
                    this.setCodeRange(cr);
                    return context.nil;
                }
                buf = newStringLight(runtime, this.value.getRealSize());
            }
            if (p2 < p) {
                ((RubyString)buf).cat(pBytes, p2, p - p2);
            }
            if (p < e) {
                if (repBytes != null) {
                    ((RubyString)buf).cat(repBytes, rep, replen);
                    if (!rep7bit_p) {
                        cr = 32;
                    }
                }
                else {
                    repl = block.yieldSpecific(context, newString(runtime, pBytes, p, e - p, enc));
                    repl = EncodingUtils.strCompatAndValid(context, repl, enc);
                    tainted |= repl.isTaint();
                    ((RubyString)buf).cat((RubyString)repl);
                    if (((RubyString)repl).getCodeRange() == 32) {
                        cr = 32;
                    }
                }
            }
        }
        else {
            final byte[] pBytes = this.value.unsafeBytes();
            int p = this.value.begin();
            final int e = p + this.value.getRealSize();
            int p2 = p;
            final int mbminlen = enc.minLength();
            byte[] repBytes;
            int rep;
            int replen;
            if (!repl.isNil()) {
                repBytes = ((RubyString)repl).value.unsafeBytes();
                rep = ((RubyString)repl).value.begin();
                replen = ((RubyString)repl).value.getRealSize();
            }
            else if (encidx == UTF16BEEncoding.INSTANCE) {
                repBytes = RubyString.SCRUB_REPL_UTF16BE;
                rep = 0;
                replen = repBytes.length;
            }
            else if (encidx == UTF16LEEncoding.INSTANCE) {
                repBytes = RubyString.SCRUB_REPL_UTF16LE;
                rep = 0;
                replen = repBytes.length;
            }
            else if (encidx == UTF32BEEncoding.INSTANCE) {
                repBytes = RubyString.SCRUB_REPL_UTF32BE;
                rep = 0;
                replen = repBytes.length;
            }
            else if (encidx == UTF32LEEncoding.INSTANCE) {
                repBytes = RubyString.SCRUB_REPL_UTF32LE;
                rep = 0;
                replen = repBytes.length;
            }
            else {
                repBytes = RubyString.SCRUB_REPL_ASCII;
                rep = 0;
                replen = repBytes.length;
            }
            while (p < e) {
                int ret = StringSupport.preciseLength(enc, pBytes, p, e);
                if (StringSupport.MBCLEN_NEEDMORE_P(ret)) {
                    break;
                }
                if (StringSupport.MBCLEN_CHARFOUND_P(ret)) {
                    p += StringSupport.MBCLEN_CHARFOUND_LEN(ret);
                }
                else {
                    if (!StringSupport.MBCLEN_INVALID_P(ret)) {
                        continue;
                    }
                    final int q2 = p;
                    int clen2 = enc.maxLength();
                    if (buf.isNil()) {
                        buf = newStringLight(runtime, this.value.getRealSize());
                    }
                    if (p > p2) {
                        ((RubyString)buf).cat(pBytes, p2, p - p2);
                    }
                    if (e - p < clen2) {
                        clen2 = e - p;
                    }
                    if (clen2 <= mbminlen * 2) {
                        clen2 = mbminlen;
                    }
                    else {
                        for (clen2 -= mbminlen; clen2 > mbminlen; clen2 -= mbminlen) {
                            ret = enc.length(pBytes, q2, q2 + clen2);
                            if (StringSupport.MBCLEN_NEEDMORE_P(ret)) {
                                break;
                            }
                            if (StringSupport.MBCLEN_INVALID_P(ret)) {}
                        }
                    }
                    if (repBytes != null) {
                        ((RubyString)buf).cat(repBytes, rep, replen);
                    }
                    else {
                        repl = block.yieldSpecific(context, newString(runtime, pBytes, p, e - p, enc));
                        repl = EncodingUtils.strCompatAndValid(context, repl, enc);
                        tainted |= repl.isTaint();
                        ((RubyString)buf).cat((RubyString)repl);
                    }
                    p = (p2 = p + clen2);
                }
            }
            if (buf.isNil()) {
                if (p == e) {
                    this.setCodeRange(32);
                    return context.nil;
                }
                buf = newStringLight(runtime, this.value.getRealSize());
            }
            if (p2 < p) {
                ((RubyString)buf).cat(pBytes, p2, p - p2);
            }
            if (p < e) {
                if (repBytes != null) {
                    ((RubyString)buf).cat(repBytes, rep, replen);
                }
                else {
                    repl = block.yieldSpecific(context, newString(runtime, pBytes, p, e - p, enc));
                    repl = EncodingUtils.strCompatAndValid(context, repl, enc);
                    tainted |= repl.isTaint();
                    ((RubyString)buf).cat((RubyString)repl);
                }
            }
            cr = 32;
        }
        buf.setTaint(tainted | this.isTaint());
        ((RubyString)buf).setEncodingAndCodeRange(enc, cr);
        return buf;
    }
    
    public int rbStrOffset(final int pos) {
        return this.strOffset(pos, StringSupport.isSingleByteOptimizable(this, this.getEncoding()));
    }
    
    private int strOffset(final int nth, final boolean singlebyte) {
        final int p = this.value.begin();
        final int size = this.value.realSize();
        final int e = p + size;
        final int pp = StringSupport.nth(this.value.getEncoding(), this.value.unsafeBytes(), p, e, nth, singlebyte);
        if (pp == -1) {
            return size;
        }
        return pp - p;
    }
    
    private static JavaSites.StringSites sites(final ThreadContext context) {
        return context.sites.String;
    }
    
    @Deprecated
    public final RubyString strDup() {
        return this.strDup(this.getRuntime(), this.getMetaClass().getRealClass());
    }
    
    @Deprecated
    final RubyString strDup(final RubyClass clazz) {
        return this.strDup(this.getRuntime(), this.getMetaClass());
    }
    
    @Deprecated
    public final void modify19(final int length) {
        this.modifyExpand(length);
    }
    
    @Deprecated
    public RubyArray split19(final ThreadContext context, final IRubyObject arg0, final boolean useBackref) {
        return this.splitCommon19(arg0, useBackref, this.flags, this.flags, context, useBackref);
    }
    
    static {
        ASCII = ASCIIEncoding.INSTANCE;
        UTF8 = UTF8Encoding.INSTANCE;
        SCRUB_REPL_UTF8 = new byte[] { -17, -65, -67 };
        SCRUB_REPL_ASCII = new byte[] { 63 };
        SCRUB_REPL_UTF16BE = new byte[] { -1, -3 };
        SCRUB_REPL_UTF16LE = new byte[] { -3, -1 };
        SCRUB_REPL_UTF32BE = new byte[] { 0, 0, -1, -3 };
        SCRUB_REPL_UTF32LE = new byte[] { -3, -1, 0, 0 };
        opTable19 = new String[][] { { "+", "+(binary)" }, { "-", "-(binary)" } };
        RubyString.STRING_ALLOCATOR = new ObjectAllocator() {
            @Override
            public IRubyObject allocate(final Ruby runtime, final RubyClass klass) {
                return RubyString.newAllocatedString(runtime, klass);
            }
        };
        EMPTY_ASCII8BIT_BYTELIST = new ByteList(ByteList.NULL_ARRAY, (Encoding)ASCIIEncoding.INSTANCE);
        EMPTY_USASCII_BYTELIST = new ByteList(ByteList.NULL_ARRAY, (Encoding)USASCIIEncoding.INSTANCE);
        RubyString.EMPTY_BYTELISTS = new EmptyByteListHolder[4];
        SPACE_BYTELIST = new ByteList(new byte[] { 32 }, false);
    }
    
    private static final class EmptyByteListHolder
    {
        final ByteList bytes;
        final int cr;
        
        EmptyByteListHolder(final Encoding enc) {
            this.bytes = new ByteList(ByteList.NULL_ARRAY, enc);
            this.cr = (this.bytes.getEncoding().isAsciiCompatible() ? 16 : 32);
        }
    }
}
