// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.http;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.time.format.DateTimeParseException;
import org.springframework.util.Assert;
import java.time.temporal.TemporalAccessor;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import org.springframework.util.ObjectUtils;
import java.time.ZonedDateTime;
import java.nio.charset.Charset;
import org.springframework.lang.Nullable;

public final class ContentDisposition
{
    private static final String INVALID_HEADER_FIELD_PARAMETER_FORMAT = "Invalid header field parameter format (as defined in RFC 5987)";
    @Nullable
    private final String type;
    @Nullable
    private final String name;
    @Nullable
    private final String filename;
    @Nullable
    private final Charset charset;
    @Nullable
    private final Long size;
    @Nullable
    private final ZonedDateTime creationDate;
    @Nullable
    private final ZonedDateTime modificationDate;
    @Nullable
    private final ZonedDateTime readDate;
    
    private ContentDisposition(@Nullable final String type, @Nullable final String name, @Nullable final String filename, @Nullable final Charset charset, @Nullable final Long size, @Nullable final ZonedDateTime creationDate, @Nullable final ZonedDateTime modificationDate, @Nullable final ZonedDateTime readDate) {
        this.type = type;
        this.name = name;
        this.filename = filename;
        this.charset = charset;
        this.size = size;
        this.creationDate = creationDate;
        this.modificationDate = modificationDate;
        this.readDate = readDate;
    }
    
    @Nullable
    public String getType() {
        return this.type;
    }
    
    @Nullable
    public String getName() {
        return this.name;
    }
    
    @Nullable
    public String getFilename() {
        return this.filename;
    }
    
    @Nullable
    public Charset getCharset() {
        return this.charset;
    }
    
    @Nullable
    public Long getSize() {
        return this.size;
    }
    
    @Nullable
    public ZonedDateTime getCreationDate() {
        return this.creationDate;
    }
    
    @Nullable
    public ZonedDateTime getModificationDate() {
        return this.modificationDate;
    }
    
    @Nullable
    public ZonedDateTime getReadDate() {
        return this.readDate;
    }
    
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ContentDisposition)) {
            return false;
        }
        final ContentDisposition otherCd = (ContentDisposition)other;
        return ObjectUtils.nullSafeEquals((Object)this.type, (Object)otherCd.type) && ObjectUtils.nullSafeEquals((Object)this.name, (Object)otherCd.name) && ObjectUtils.nullSafeEquals((Object)this.filename, (Object)otherCd.filename) && ObjectUtils.nullSafeEquals((Object)this.charset, (Object)otherCd.charset) && ObjectUtils.nullSafeEquals((Object)this.size, (Object)otherCd.size) && ObjectUtils.nullSafeEquals((Object)this.creationDate, (Object)otherCd.creationDate) && ObjectUtils.nullSafeEquals((Object)this.modificationDate, (Object)otherCd.modificationDate) && ObjectUtils.nullSafeEquals((Object)this.readDate, (Object)otherCd.readDate);
    }
    
    @Override
    public int hashCode() {
        int result = ObjectUtils.nullSafeHashCode((Object)this.type);
        result = 31 * result + ObjectUtils.nullSafeHashCode((Object)this.name);
        result = 31 * result + ObjectUtils.nullSafeHashCode((Object)this.filename);
        result = 31 * result + ObjectUtils.nullSafeHashCode((Object)this.charset);
        result = 31 * result + ObjectUtils.nullSafeHashCode((Object)this.size);
        result = 31 * result + ((this.creationDate != null) ? this.creationDate.hashCode() : 0);
        result = 31 * result + ((this.modificationDate != null) ? this.modificationDate.hashCode() : 0);
        result = 31 * result + ((this.readDate != null) ? this.readDate.hashCode() : 0);
        return result;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (this.type != null) {
            sb.append(this.type);
        }
        if (this.name != null) {
            sb.append("; name=\"");
            sb.append(this.name).append('\"');
        }
        if (this.filename != null) {
            if (this.charset == null || StandardCharsets.US_ASCII.equals(this.charset)) {
                sb.append("; filename=\"");
                sb.append(escapeQuotationsInFilename(this.filename)).append('\"');
            }
            else {
                sb.append("; filename*=");
                sb.append(encodeFilename(this.filename, this.charset));
            }
        }
        if (this.size != null) {
            sb.append("; size=");
            sb.append(this.size);
        }
        if (this.creationDate != null) {
            sb.append("; creation-date=\"");
            sb.append(DateTimeFormatter.RFC_1123_DATE_TIME.format(this.creationDate));
            sb.append('\"');
        }
        if (this.modificationDate != null) {
            sb.append("; modification-date=\"");
            sb.append(DateTimeFormatter.RFC_1123_DATE_TIME.format(this.modificationDate));
            sb.append('\"');
        }
        if (this.readDate != null) {
            sb.append("; read-date=\"");
            sb.append(DateTimeFormatter.RFC_1123_DATE_TIME.format(this.readDate));
            sb.append('\"');
        }
        return sb.toString();
    }
    
    public static Builder builder(final String type) {
        return new BuilderImpl(type);
    }
    
    public static ContentDisposition empty() {
        return new ContentDisposition("", null, null, null, null, null, null, null);
    }
    
    public static ContentDisposition parse(final String contentDisposition) {
        final List<String> parts = tokenize(contentDisposition);
        final String type = parts.get(0);
        String name = null;
        String filename = null;
        Charset charset = null;
        Long size = null;
        ZonedDateTime creationDate = null;
        ZonedDateTime modificationDate = null;
        ZonedDateTime readDate = null;
        for (int i = 1; i < parts.size(); ++i) {
            final String part = parts.get(i);
            final int eqIndex = part.indexOf(61);
            if (eqIndex == -1) {
                throw new IllegalArgumentException("Invalid content disposition format");
            }
            final String attribute = part.substring(0, eqIndex);
            final String value = (part.startsWith("\"", eqIndex + 1) && part.endsWith("\"")) ? part.substring(eqIndex + 2, part.length() - 1) : part.substring(eqIndex + 1);
            if (attribute.equals("name")) {
                name = value;
            }
            else if (attribute.equals("filename*")) {
                final int idx1 = value.indexOf(39);
                final int idx2 = value.indexOf(39, idx1 + 1);
                if (idx1 != -1 && idx2 != -1) {
                    charset = Charset.forName(value.substring(0, idx1).trim());
                    Assert.isTrue(StandardCharsets.UTF_8.equals(charset) || StandardCharsets.ISO_8859_1.equals(charset), "Charset should be UTF-8 or ISO-8859-1");
                    filename = decodeFilename(value.substring(idx2 + 1), charset);
                }
                else {
                    filename = decodeFilename(value, StandardCharsets.US_ASCII);
                }
            }
            else if (attribute.equals("filename") && filename == null) {
                filename = value;
            }
            else if (attribute.equals("size")) {
                size = Long.parseLong(value);
            }
            else if (attribute.equals("creation-date")) {
                try {
                    creationDate = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
                }
                catch (DateTimeParseException ex) {}
            }
            else if (attribute.equals("modification-date")) {
                try {
                    modificationDate = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
                }
                catch (DateTimeParseException ex2) {}
            }
            else if (attribute.equals("read-date")) {
                try {
                    readDate = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
                }
                catch (DateTimeParseException ex3) {}
            }
        }
        return new ContentDisposition(type, name, filename, charset, size, creationDate, modificationDate, readDate);
    }
    
    private static List<String> tokenize(final String headerValue) {
        int index = headerValue.indexOf(59);
        final String type = ((index >= 0) ? headerValue.substring(0, index) : headerValue).trim();
        if (type.isEmpty()) {
            throw new IllegalArgumentException("Content-Disposition header must not be empty");
        }
        final List<String> parts = new ArrayList<String>();
        parts.add(type);
        if (index >= 0) {
            do {
                int nextIndex = index + 1;
                boolean quoted = false;
                boolean escaped = false;
                while (nextIndex < headerValue.length()) {
                    final char ch = headerValue.charAt(nextIndex);
                    if (ch == ';') {
                        if (!quoted) {
                            break;
                        }
                    }
                    else if (!escaped && ch == '\"') {
                        quoted = !quoted;
                    }
                    escaped = (!escaped && ch == '\\');
                    ++nextIndex;
                }
                final String part = headerValue.substring(index + 1, nextIndex).trim();
                if (!part.isEmpty()) {
                    parts.add(part);
                }
                index = nextIndex;
            } while (index < headerValue.length());
        }
        return parts;
    }
    
    private static String decodeFilename(final String filename, final Charset charset) {
        Assert.notNull((Object)filename, "'input' String` should not be null");
        Assert.notNull((Object)charset, "'charset' should not be null");
        final byte[] value = filename.getBytes(charset);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int index = 0;
        while (index < value.length) {
            final byte b = value[index];
            if (isRFC5987AttrChar(b)) {
                baos.write((char)b);
                ++index;
            }
            else {
                if (b != 37 || index >= value.length - 2) {
                    throw new IllegalArgumentException("Invalid header field parameter format (as defined in RFC 5987)");
                }
                final char[] array = { (char)value[index + 1], (char)value[index + 2] };
                try {
                    baos.write(Integer.parseInt(String.valueOf(array), 16));
                }
                catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid header field parameter format (as defined in RFC 5987)", ex);
                }
                index += 3;
            }
        }
        return new String(baos.toByteArray(), charset);
    }
    
    private static boolean isRFC5987AttrChar(final byte c) {
        return (c >= 48 && c <= 57) || (c >= 97 && c <= 122) || (c >= 65 && c <= 90) || c == 33 || c == 35 || c == 36 || c == 38 || c == 43 || c == 45 || c == 46 || c == 94 || c == 95 || c == 96 || c == 124 || c == 126;
    }
    
    private static String escapeQuotationsInFilename(final String filename) {
        if (filename.indexOf(34) == -1 && filename.indexOf(92) == -1) {
            return filename;
        }
        boolean escaped = false;
        final StringBuilder sb = new StringBuilder();
        for (final char c : filename.toCharArray()) {
            sb.append((c == '\"' && !escaped) ? "\\\"" : Character.valueOf(c));
            escaped = (!escaped && c == '\\');
        }
        if (escaped) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }
    
    private static String encodeFilename(final String input, final Charset charset) {
        Assert.notNull((Object)input, "`input` is required");
        Assert.notNull((Object)charset, "`charset` is required");
        Assert.isTrue(!StandardCharsets.US_ASCII.equals(charset), "ASCII does not require encoding");
        Assert.isTrue(StandardCharsets.UTF_8.equals(charset) || StandardCharsets.ISO_8859_1.equals(charset), "Only UTF-8 and ISO-8859-1 supported.");
        final byte[] source = input.getBytes(charset);
        final int len = source.length;
        final StringBuilder sb = new StringBuilder(len << 1);
        sb.append(charset.name());
        sb.append("''");
        for (final byte b : source) {
            if (isRFC5987AttrChar(b)) {
                sb.append((char)b);
            }
            else {
                sb.append('%');
                final char hex1 = Character.toUpperCase(Character.forDigit(b >> 4 & 0xF, 16));
                final char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
                sb.append(hex1);
                sb.append(hex2);
            }
        }
        return sb.toString();
    }
    
    private static class BuilderImpl implements Builder
    {
        private final String type;
        @Nullable
        private String name;
        @Nullable
        private String filename;
        @Nullable
        private Charset charset;
        @Nullable
        private Long size;
        @Nullable
        private ZonedDateTime creationDate;
        @Nullable
        private ZonedDateTime modificationDate;
        @Nullable
        private ZonedDateTime readDate;
        
        public BuilderImpl(final String type) {
            Assert.hasText(type, "'type' must not be not empty");
            this.type = type;
        }
        
        @Override
        public Builder name(final String name) {
            this.name = name;
            return this;
        }
        
        @Override
        public Builder filename(final String filename) {
            Assert.hasText(filename, "No filename");
            this.filename = filename;
            return this;
        }
        
        @Override
        public Builder filename(final String filename, final Charset charset) {
            Assert.hasText(filename, "No filename");
            this.filename = filename;
            this.charset = charset;
            return this;
        }
        
        @Override
        public Builder size(final Long size) {
            this.size = size;
            return this;
        }
        
        @Override
        public Builder creationDate(final ZonedDateTime creationDate) {
            this.creationDate = creationDate;
            return this;
        }
        
        @Override
        public Builder modificationDate(final ZonedDateTime modificationDate) {
            this.modificationDate = modificationDate;
            return this;
        }
        
        @Override
        public Builder readDate(final ZonedDateTime readDate) {
            this.readDate = readDate;
            return this;
        }
        
        @Override
        public ContentDisposition build() {
            return new ContentDisposition(this.type, this.name, this.filename, this.charset, this.size, this.creationDate, this.modificationDate, this.readDate, null);
        }
    }
    
    public interface Builder
    {
        Builder name(final String p0);
        
        Builder filename(final String p0);
        
        Builder filename(final String p0, final Charset p1);
        
        Builder size(final Long p0);
        
        Builder creationDate(final ZonedDateTime p0);
        
        Builder modificationDate(final ZonedDateTime p0);
        
        Builder readDate(final ZonedDateTime p0);
        
        ContentDisposition build();
    }
}
