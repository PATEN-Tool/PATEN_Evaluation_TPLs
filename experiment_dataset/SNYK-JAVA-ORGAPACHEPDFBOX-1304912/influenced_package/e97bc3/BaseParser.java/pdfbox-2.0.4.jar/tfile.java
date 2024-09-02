// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.pdfbox.pdfparser;

import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSNull;
import org.apache.pdfbox.cos.COSBoolean;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSArray;
import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.cos.COSString;
import java.util.Arrays;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.util.Charsets;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSObjectKey;
import java.io.IOException;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.commons.logging.Log;

public abstract class BaseParser
{
    private static final long OBJECT_NUMBER_THRESHOLD = 10000000000L;
    private static final long GENERATION_NUMBER_THRESHOLD = 65535L;
    static final int MAX_LENGTH_LONG;
    private static final Log LOG;
    protected static final int E = 101;
    protected static final int N = 110;
    protected static final int D = 100;
    protected static final int S = 115;
    protected static final int T = 116;
    protected static final int R = 114;
    protected static final int A = 97;
    protected static final int M = 109;
    protected static final int O = 111;
    protected static final int B = 98;
    protected static final int J = 106;
    public static final String DEF = "def";
    protected static final String ENDOBJ_STRING = "endobj";
    protected static final String ENDSTREAM_STRING = "endstream";
    protected static final String STREAM_STRING = "stream";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String NULL = "null";
    protected static final byte ASCII_LF = 10;
    protected static final byte ASCII_CR = 13;
    private static final byte ASCII_ZERO = 48;
    private static final byte ASCII_NINE = 57;
    private static final byte ASCII_SPACE = 32;
    protected final SequentialSource seqSource;
    protected COSDocument document;
    
    public BaseParser(final SequentialSource pdfSource) {
        this.seqSource = pdfSource;
    }
    
    private static boolean isHexDigit(final char ch) {
        return isDigit(ch) || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
    }
    
    private COSBase parseCOSDictionaryValue() throws IOException {
        final long numOffset = this.seqSource.getPosition();
        final COSBase number = this.parseDirObject();
        this.skipSpaces();
        if (!this.isDigit()) {
            return number;
        }
        final long genOffset = this.seqSource.getPosition();
        final COSBase generationNumber = this.parseDirObject();
        this.skipSpaces();
        this.readExpectedChar('R');
        if (!(number instanceof COSInteger)) {
            throw new IOException("expected number, actual=" + number + " at offset " + numOffset);
        }
        if (!(generationNumber instanceof COSInteger)) {
            throw new IOException("expected number, actual=" + number + " at offset " + genOffset);
        }
        final COSObjectKey key = new COSObjectKey(((COSInteger)number).longValue(), ((COSInteger)generationNumber).intValue());
        return this.getObjectFromPool(key);
    }
    
    private COSBase getObjectFromPool(final COSObjectKey key) throws IOException {
        if (this.document == null) {
            throw new IOException("object reference " + key + " at offset " + this.seqSource.getPosition() + " in content stream");
        }
        return this.document.getObjectFromPool(key);
    }
    
    protected COSDictionary parseCOSDictionary() throws IOException {
        this.readExpectedChar('<');
        this.readExpectedChar('<');
        this.skipSpaces();
        final COSDictionary obj = new COSDictionary();
        boolean done = false;
        while (!done) {
            this.skipSpaces();
            final char c = (char)this.seqSource.peek();
            if (c == '>') {
                done = true;
            }
            else if (c == '/') {
                this.parseCOSDictionaryNameValuePair(obj);
            }
            else {
                BaseParser.LOG.warn((Object)("Invalid dictionary, found: '" + c + "' but expected: '/' at offset " + this.seqSource.getPosition()));
                if (this.readUntilEndOfCOSDictionary()) {
                    return obj;
                }
                continue;
            }
        }
        this.readExpectedChar('>');
        this.readExpectedChar('>');
        return obj;
    }
    
    private boolean readUntilEndOfCOSDictionary() throws IOException {
        int c;
        for (c = this.seqSource.read(); c != -1 && c != 47 && c != 62; c = this.seqSource.read()) {
            if (c == 101) {
                c = this.seqSource.read();
                if (c == 110) {
                    c = this.seqSource.read();
                    if (c == 100) {
                        c = this.seqSource.read();
                        final boolean isStream = c == 115 && this.seqSource.read() == 116 && this.seqSource.read() == 114 && this.seqSource.read() == 101 && this.seqSource.read() == 97 && this.seqSource.read() == 109;
                        final boolean isObj = !isStream && c == 111 && this.seqSource.read() == 98 && this.seqSource.read() == 106;
                        if (isStream || isObj) {
                            return true;
                        }
                    }
                }
            }
        }
        if (c == -1) {
            return true;
        }
        this.seqSource.unread(c);
        return false;
    }
    
    private void parseCOSDictionaryNameValuePair(final COSDictionary obj) throws IOException {
        final COSName key = this.parseCOSName();
        final COSBase value = this.parseCOSDictionaryValue();
        this.skipSpaces();
        if ((char)this.seqSource.peek() == 'd') {
            final String potentialDEF = this.readString();
            if (!potentialDEF.equals("def")) {
                this.seqSource.unread(potentialDEF.getBytes(Charsets.ISO_8859_1));
            }
            else {
                this.skipSpaces();
            }
        }
        if (value == null) {
            BaseParser.LOG.warn((Object)("Bad Dictionary Declaration " + this.seqSource));
        }
        else {
            value.setDirect(true);
            obj.setItem(key, value);
        }
    }
    
    protected void skipWhiteSpaces() throws IOException {
        int whitespace;
        for (whitespace = this.seqSource.read(); 32 == whitespace; whitespace = this.seqSource.read()) {}
        if (13 == whitespace) {
            whitespace = this.seqSource.read();
            if (10 != whitespace) {
                this.seqSource.unread(whitespace);
            }
        }
        else if (10 != whitespace) {
            this.seqSource.unread(whitespace);
        }
    }
    
    private int checkForMissingCloseParen(final int bracesParameter) throws IOException {
        int braces = bracesParameter;
        final byte[] nextThreeBytes = new byte[3];
        final int amountRead = this.seqSource.read(nextThreeBytes);
        if (amountRead == 3 && ((nextThreeBytes[0] == 13 && nextThreeBytes[1] == 10 && nextThreeBytes[2] == 47) || (nextThreeBytes[0] == 13 && nextThreeBytes[1] == 47))) {
            braces = 0;
        }
        if (amountRead > 0) {
            this.seqSource.unread(Arrays.copyOfRange(nextThreeBytes, 0, amountRead));
        }
        return braces;
    }
    
    protected COSString parseCOSString() throws IOException {
        final char nextChar = (char)this.seqSource.read();
        if (nextChar == '(') {
            final char openBrace = '(';
            final char closeBrace = ')';
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            int braces = 1;
            int c = this.seqSource.read();
            while (braces > 0 && c != -1) {
                final char ch = (char)c;
                int nextc = -2;
                if (ch == closeBrace) {
                    --braces;
                    braces = this.checkForMissingCloseParen(braces);
                    if (braces != 0) {
                        out.write(ch);
                    }
                }
                else if (ch == openBrace) {
                    ++braces;
                    out.write(ch);
                }
                else if (ch == '\\') {
                    final char next = (char)this.seqSource.read();
                    switch (next) {
                        case 'n': {
                            out.write(10);
                            break;
                        }
                        case 'r': {
                            out.write(13);
                            break;
                        }
                        case 't': {
                            out.write(9);
                            break;
                        }
                        case 'b': {
                            out.write(8);
                            break;
                        }
                        case 'f': {
                            out.write(12);
                            break;
                        }
                        case ')': {
                            braces = this.checkForMissingCloseParen(braces);
                            if (braces != 0) {
                                out.write(next);
                                break;
                            }
                            out.write(92);
                            break;
                        }
                        case '(':
                        case '\\': {
                            out.write(next);
                            break;
                        }
                        case '\n':
                        case '\r': {
                            for (c = this.seqSource.read(); this.isEOL(c) && c != -1; c = this.seqSource.read()) {}
                            nextc = c;
                            break;
                        }
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7': {
                            final StringBuilder octal = new StringBuilder();
                            octal.append(next);
                            c = this.seqSource.read();
                            char digit = (char)c;
                            if (digit >= '0' && digit <= '7') {
                                octal.append(digit);
                                c = this.seqSource.read();
                                digit = (char)c;
                                if (digit >= '0' && digit <= '7') {
                                    octal.append(digit);
                                }
                                else {
                                    nextc = c;
                                }
                            }
                            else {
                                nextc = c;
                            }
                            int character = 0;
                            try {
                                character = Integer.parseInt(octal.toString(), 8);
                            }
                            catch (NumberFormatException e) {
                                throw new IOException("Error: Expected octal character, actual='" + (Object)octal + "'", e);
                            }
                            out.write(character);
                            break;
                        }
                        default: {
                            out.write(next);
                            break;
                        }
                    }
                }
                else {
                    out.write(ch);
                }
                if (nextc != -2) {
                    c = nextc;
                }
                else {
                    c = this.seqSource.read();
                }
            }
            if (c != -1) {
                this.seqSource.unread(c);
            }
            return new COSString(out.toByteArray());
        }
        if (nextChar == '<') {
            return this.parseCOSHexString();
        }
        throw new IOException("parseCOSString string should start with '(' or '<' and not '" + nextChar + "' " + this.seqSource);
    }
    
    private COSString parseCOSHexString() throws IOException {
        final StringBuilder sBuf = new StringBuilder();
        while (true) {
            int c = this.seqSource.read();
            if (isHexDigit((char)c)) {
                sBuf.append((char)c);
            }
            else {
                if (c == 62) {
                    break;
                }
                if (c < 0) {
                    throw new IOException("Missing closing bracket for hex string. Reached EOS.");
                }
                if (c == 32 || c == 10 || c == 9 || c == 13 || c == 8) {
                    continue;
                }
                if (c == 12) {
                    continue;
                }
                if (sBuf.length() % 2 != 0) {
                    sBuf.deleteCharAt(sBuf.length() - 1);
                }
                do {
                    c = this.seqSource.read();
                } while (c != 62 && c >= 0);
                if (c < 0) {
                    throw new IOException("Missing closing bracket for hex string. Reached EOS.");
                }
                break;
            }
        }
        return COSString.parseHex(sBuf.toString());
    }
    
    protected COSArray parseCOSArray() throws IOException {
        this.readExpectedChar('[');
        final COSArray po = new COSArray();
        this.skipSpaces();
        int i;
        while ((i = this.seqSource.peek()) > 0 && (char)i != ']') {
            COSBase pbo = this.parseDirObject();
            if (pbo instanceof COSObject) {
                if (po.get(po.size() - 1) instanceof COSInteger) {
                    final COSInteger genNumber = (COSInteger)po.remove(po.size() - 1);
                    if (po.get(po.size() - 1) instanceof COSInteger) {
                        final COSInteger number = (COSInteger)po.remove(po.size() - 1);
                        final COSObjectKey key = new COSObjectKey(number.longValue(), genNumber.intValue());
                        pbo = this.getObjectFromPool(key);
                    }
                    else {
                        pbo = null;
                    }
                }
                else {
                    pbo = null;
                }
            }
            if (pbo != null) {
                po.add(pbo);
            }
            else {
                BaseParser.LOG.warn((Object)("Corrupt object reference at offset " + this.seqSource.getPosition()));
                final String isThisTheEnd = this.readString();
                this.seqSource.unread(isThisTheEnd.getBytes(Charsets.ISO_8859_1));
                if ("endobj".equals(isThisTheEnd) || "endstream".equals(isThisTheEnd)) {
                    return po;
                }
            }
            this.skipSpaces();
        }
        this.seqSource.read();
        this.skipSpaces();
        return po;
    }
    
    protected boolean isEndOfName(final int ch) {
        return ch == 32 || ch == 13 || ch == 10 || ch == 9 || ch == 62 || ch == 60 || ch == 91 || ch == 47 || ch == 93 || ch == 41 || ch == 40 || ch == 0 || ch == 12;
    }
    
    protected COSName parseCOSName() throws IOException {
        this.readExpectedChar('/');
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int c = this.seqSource.read();
        while (c != -1) {
            final int ch = c;
            if (ch == 35) {
                final int ch2 = this.seqSource.read();
                final int ch3 = this.seqSource.read();
                if (isHexDigit((char)ch2) && isHexDigit((char)ch3)) {
                    final String hex = "" + (char)ch2 + (char)ch3;
                    try {
                        buffer.write(Integer.parseInt(hex, 16));
                    }
                    catch (NumberFormatException e) {
                        throw new IOException("Error: expected hex digit, actual='" + hex + "'", e);
                    }
                    c = this.seqSource.read();
                }
                else {
                    if (ch3 == -1 || ch2 == -1) {
                        BaseParser.LOG.error((Object)"Premature EOF in BaseParser#parseCOSName");
                        c = -1;
                        break;
                    }
                    this.seqSource.unread(ch3);
                    c = ch2;
                    buffer.write(ch);
                }
            }
            else {
                if (this.isEndOfName(ch)) {
                    break;
                }
                buffer.write(ch);
                c = this.seqSource.read();
            }
        }
        if (c != -1) {
            this.seqSource.unread(c);
        }
        final String string = new String(buffer.toByteArray(), Charsets.UTF_8);
        return COSName.getPDFName(string);
    }
    
    protected COSBoolean parseBoolean() throws IOException {
        COSBoolean retval = null;
        final char c = (char)this.seqSource.peek();
        if (c == 't') {
            final String trueString = new String(this.seqSource.readFully(4), Charsets.ISO_8859_1);
            if (!trueString.equals("true")) {
                throw new IOException("Error parsing boolean: expected='true' actual='" + trueString + "' at offset " + this.seqSource.getPosition());
            }
            retval = COSBoolean.TRUE;
        }
        else {
            if (c != 'f') {
                throw new IOException("Error parsing boolean expected='t or f' actual='" + c + "' at offset " + this.seqSource.getPosition());
            }
            final String falseString = new String(this.seqSource.readFully(5), Charsets.ISO_8859_1);
            if (!falseString.equals("false")) {
                throw new IOException("Error parsing boolean: expected='true' actual='" + falseString + "' at offset " + this.seqSource.getPosition());
            }
            retval = COSBoolean.FALSE;
        }
        return retval;
    }
    
    protected COSBase parseDirObject() throws IOException {
        COSBase retval = null;
        this.skipSpaces();
        final int nextByte = this.seqSource.peek();
        char c = (char)nextByte;
        switch (c) {
            case '<': {
                final int leftBracket = this.seqSource.read();
                c = (char)this.seqSource.peek();
                this.seqSource.unread(leftBracket);
                if (c == '<') {
                    retval = this.parseCOSDictionary();
                    this.skipSpaces();
                    break;
                }
                retval = this.parseCOSString();
                break;
            }
            case '[': {
                retval = this.parseCOSArray();
                break;
            }
            case '(': {
                retval = this.parseCOSString();
                break;
            }
            case '/': {
                retval = this.parseCOSName();
                break;
            }
            case 'n': {
                this.readExpectedString("null");
                retval = COSNull.NULL;
                break;
            }
            case 't': {
                final String trueString = new String(this.seqSource.readFully(4), Charsets.ISO_8859_1);
                if (trueString.equals("true")) {
                    retval = COSBoolean.TRUE;
                    break;
                }
                throw new IOException("expected true actual='" + trueString + "' " + this.seqSource + "' at offset " + this.seqSource.getPosition());
            }
            case 'f': {
                final String falseString = new String(this.seqSource.readFully(5), Charsets.ISO_8859_1);
                if (falseString.equals("false")) {
                    retval = COSBoolean.FALSE;
                    break;
                }
                throw new IOException("expected false actual='" + falseString + "' " + this.seqSource + "' at offset " + this.seqSource.getPosition());
            }
            case 'R': {
                this.seqSource.read();
                retval = new COSObject(null);
                break;
            }
            case '\uffff': {
                return null;
            }
            default: {
                if (Character.isDigit(c) || c == '-' || c == '+' || c == '.') {
                    final StringBuilder buf = new StringBuilder();
                    int ic;
                    for (ic = this.seqSource.read(), c = (char)ic; Character.isDigit(c) || c == '-' || c == '+' || c == '.' || c == 'E' || c == 'e'; c = (char)ic) {
                        buf.append(c);
                        ic = this.seqSource.read();
                    }
                    if (ic != -1) {
                        this.seqSource.unread(ic);
                    }
                    retval = COSNumber.get(buf.toString());
                    break;
                }
                final String badString = this.readString();
                if (badString == null || badString.length() == 0) {
                    final int peek = this.seqSource.peek();
                    throw new IOException("Unknown dir object c='" + c + "' cInt=" + (int)c + " peek='" + (char)peek + "' peekInt=" + peek + " at offset " + this.seqSource.getPosition());
                }
                if ("endobj".equals(badString) || "endstream".equals(badString)) {
                    this.seqSource.unread(badString.getBytes(Charsets.ISO_8859_1));
                    break;
                }
                break;
            }
        }
        return retval;
    }
    
    protected String readString() throws IOException {
        this.skipSpaces();
        final StringBuilder buffer = new StringBuilder();
        int c;
        for (c = this.seqSource.read(); !this.isEndOfName((char)c) && c != -1; c = this.seqSource.read()) {
            buffer.append((char)c);
        }
        if (c != -1) {
            this.seqSource.unread(c);
        }
        return buffer.toString();
    }
    
    protected void readExpectedString(final String expectedString) throws IOException {
        this.readExpectedString(expectedString.toCharArray(), false);
    }
    
    protected final void readExpectedString(final char[] expectedString, final boolean skipSpaces) throws IOException {
        this.skipSpaces();
        for (final char c : expectedString) {
            if (this.seqSource.read() != c) {
                throw new IOException("Expected string '" + new String(expectedString) + "' but missed at character '" + c + "' at offset " + this.seqSource.getPosition());
            }
        }
        this.skipSpaces();
    }
    
    protected void readExpectedChar(final char ec) throws IOException {
        final char c = (char)this.seqSource.read();
        if (c != ec) {
            throw new IOException("expected='" + ec + "' actual='" + c + "' at offset " + this.seqSource.getPosition());
        }
    }
    
    protected String readString(final int length) throws IOException {
        this.skipSpaces();
        int c;
        StringBuilder buffer;
        for (c = this.seqSource.read(), buffer = new StringBuilder(length); !this.isWhitespace(c) && !this.isClosing(c) && c != -1 && buffer.length() < length && c != 91 && c != 60 && c != 40 && c != 47; c = this.seqSource.read()) {
            buffer.append((char)c);
        }
        if (c != -1) {
            this.seqSource.unread(c);
        }
        return buffer.toString();
    }
    
    protected boolean isClosing() throws IOException {
        return this.isClosing(this.seqSource.peek());
    }
    
    protected boolean isClosing(final int c) {
        return c == 93;
    }
    
    protected String readLine() throws IOException {
        if (this.seqSource.isEOF()) {
            throw new IOException("Error: End-of-File, expected line");
        }
        final StringBuilder buffer = new StringBuilder(11);
        int c;
        while ((c = this.seqSource.read()) != -1 && !this.isEOL(c)) {
            buffer.append((char)c);
        }
        if (this.isCR(c) && this.isLF(this.seqSource.peek())) {
            this.seqSource.read();
        }
        return buffer.toString();
    }
    
    protected boolean isEOL() throws IOException {
        return this.isEOL(this.seqSource.peek());
    }
    
    protected boolean isEOL(final int c) {
        return this.isLF(c) || this.isCR(c);
    }
    
    private boolean isLF(final int c) {
        return 10 == c;
    }
    
    private boolean isCR(final int c) {
        return 13 == c;
    }
    
    protected boolean isWhitespace() throws IOException {
        return this.isWhitespace(this.seqSource.peek());
    }
    
    protected boolean isWhitespace(final int c) {
        return c == 0 || c == 9 || c == 12 || c == 10 || c == 13 || c == 32;
    }
    
    protected boolean isSpace() throws IOException {
        return this.isSpace(this.seqSource.peek());
    }
    
    protected boolean isSpace(final int c) {
        return 32 == c;
    }
    
    protected boolean isDigit() throws IOException {
        return isDigit(this.seqSource.peek());
    }
    
    protected static boolean isDigit(final int c) {
        return c >= 48 && c <= 57;
    }
    
    protected void skipSpaces() throws IOException {
        int c;
        for (c = this.seqSource.read(); this.isWhitespace(c) || c == 37; c = this.seqSource.read()) {
            if (c == 37) {
                for (c = this.seqSource.read(); !this.isEOL(c) && c != -1; c = this.seqSource.read()) {}
            }
            else {}
        }
        if (c != -1) {
            this.seqSource.unread(c);
        }
    }
    
    protected long readObjectNumber() throws IOException {
        final long retval = this.readLong();
        if (retval < 0L || retval >= 10000000000L) {
            throw new IOException("Object Number '" + retval + "' has more than 10 digits or is negative");
        }
        return retval;
    }
    
    protected int readGenerationNumber() throws IOException {
        final int retval = this.readInt();
        if (retval < 0 || retval > 65535L) {
            throw new IOException("Generation Number '" + retval + "' has more than 5 digits");
        }
        return retval;
    }
    
    protected int readInt() throws IOException {
        this.skipSpaces();
        int retval = 0;
        final StringBuilder intBuffer = this.readStringNumber();
        try {
            retval = Integer.parseInt(intBuffer.toString());
        }
        catch (NumberFormatException e) {
            this.seqSource.unread(intBuffer.toString().getBytes(Charsets.ISO_8859_1));
            throw new IOException("Error: Expected an integer type at offset " + this.seqSource.getPosition(), e);
        }
        return retval;
    }
    
    protected long readLong() throws IOException {
        this.skipSpaces();
        long retval = 0L;
        final StringBuilder longBuffer = this.readStringNumber();
        try {
            retval = Long.parseLong(longBuffer.toString());
        }
        catch (NumberFormatException e) {
            this.seqSource.unread(longBuffer.toString().getBytes(Charsets.ISO_8859_1));
            throw new IOException("Error: Expected a long type at offset " + this.seqSource.getPosition() + ", instead got '" + (Object)longBuffer + "'", e);
        }
        return retval;
    }
    
    protected final StringBuilder readStringNumber() throws IOException {
        int lastByte = 0;
        final StringBuilder buffer = new StringBuilder();
        while ((lastByte = this.seqSource.read()) != 32 && lastByte != 10 && lastByte != 13 && lastByte != 60 && lastByte != 91 && lastByte != 40 && lastByte != 0 && lastByte != -1) {
            buffer.append((char)lastByte);
            if (buffer.length() > BaseParser.MAX_LENGTH_LONG) {
                throw new IOException("Number '" + (Object)buffer + "' is getting too long, stop reading at offset " + this.seqSource.getPosition());
            }
        }
        if (lastByte != -1) {
            this.seqSource.unread(lastByte);
        }
        return buffer;
    }
    
    static {
        MAX_LENGTH_LONG = Long.toString(Long.MAX_VALUE).length();
        LOG = LogFactory.getLog((Class)BaseParser.class);
    }
}
