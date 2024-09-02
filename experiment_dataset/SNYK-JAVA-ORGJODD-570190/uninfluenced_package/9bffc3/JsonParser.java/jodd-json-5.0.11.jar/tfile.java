// 
// Decompiled by Procyon v0.5.36
// 

package jodd.json;

import jodd.util.CharArraySequence;
import jodd.introspector.PropertyDescriptor;
import jodd.json.meta.TypeData;
import jodd.introspector.ClassDescriptor;
import jodd.introspector.ClassIntrospector;
import java.util.Collection;
import jodd.util.CharUtil;
import java.util.function.Supplier;
import java.util.List;
import jodd.util.UnsafeUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.math.BigInteger;
import java.util.Map;
import jodd.json.meta.JsonAnnotationManager;

public class JsonParser extends JsonParserBase
{
    private static final char[] T_RUE;
    private static final char[] F_ALSE;
    private static final char[] N_ULL;
    public static final String KEYS = "keys";
    public static final String VALUES = "values";
    protected int ndx;
    protected char[] input;
    protected int total;
    protected Path path;
    protected boolean useAltPaths;
    protected boolean lazy;
    protected boolean looseMode;
    protected Class rootType;
    protected MapToBean mapToBean;
    private boolean notFirstObject;
    private final JsonAnnotationManager jsonAnnotationManager;
    protected Map<Path, Class> mappings;
    protected Map<Path, ValueConverter> convs;
    protected String classMetadataName;
    protected char[] text;
    protected int textLen;
    private static final char[] UNQUOTED_DELIMETERS;
    private static final BigInteger MAX_LONG;
    private static final BigInteger MIN_LONG;
    
    public static JsonParser create() {
        return new JsonParser();
    }
    
    public static JsonParser createLazyOne() {
        return new JsonParser().lazy(true);
    }
    
    public JsonParser() {
        super(Defaults.strictTypes);
        this.ndx = 0;
        this.useAltPaths = Defaults.useAltPathsByParser;
        this.lazy = Defaults.lazy;
        this.looseMode = Defaults.loose;
        this.classMetadataName = Defaults.classMetadataName;
        this.text = new char[512];
        this.jsonAnnotationManager = JsonAnnotationManager.get();
    }
    
    protected void reset() {
        this.ndx = 0;
        this.textLen = 0;
        this.path = new Path();
        this.notFirstObject = false;
        if (this.useAltPaths) {
            this.path.altPath = new Path();
        }
        if (this.classMetadataName != null) {
            this.mapToBean = this.createMapToBean(this.classMetadataName);
        }
    }
    
    public JsonParser useAltPaths() {
        this.useAltPaths = true;
        return this;
    }
    
    public JsonParser looseMode(final boolean looseMode) {
        this.looseMode = looseMode;
        return this;
    }
    
    public JsonParser strictTypes(final boolean strictTypes) {
        this.strictTypes = strictTypes;
        return this;
    }
    
    public JsonParser lazy(final boolean lazy) {
        this.lazy = lazy;
        this.mapSupplier = (lazy ? JsonParser.LAZYMAP_SUPPLIER : JsonParser.HASHMAP_SUPPLIER);
        this.listSupplier = (lazy ? JsonParser.LAZYLIST_SUPPLIER : JsonParser.ARRAYLIST_SUPPLIER);
        return this;
    }
    
    public JsonParser map(final Class target) {
        this.rootType = target;
        return this;
    }
    
    public JsonParser map(final String path, final Class target) {
        if (path == null) {
            this.rootType = target;
            return this;
        }
        if (this.mappings == null) {
            this.mappings = new HashMap<Path, Class>();
        }
        this.mappings.put(Path.parse(path), target);
        return this;
    }
    
    protected Class replaceWithMappedTypeForPath(final Class target) {
        if (this.mappings == null) {
            return target;
        }
        final Path altPath = this.path.getAltPath();
        if (altPath != null && !altPath.equals(this.path)) {
            final Class newType = this.mappings.get(altPath);
            if (newType != null) {
                return newType;
            }
        }
        final Class newType = this.mappings.get(this.path);
        if (newType != null) {
            return newType;
        }
        return target;
    }
    
    public JsonParser withValueConverter(final String path, final ValueConverter valueConverter) {
        if (this.convs == null) {
            this.convs = new HashMap<Path, ValueConverter>();
        }
        this.convs.put(Path.parse(path), valueConverter);
        return this;
    }
    
    protected ValueConverter lookupValueConverter() {
        if (this.convs == null) {
            return null;
        }
        return this.convs.get(this.path);
    }
    
    public JsonParser setClassMetadataName(final String name) {
        this.classMetadataName = name;
        return this;
    }
    
    public JsonParser withClassMetadata(final boolean useMetadata) {
        if (useMetadata) {
            this.classMetadataName = "__class";
        }
        else {
            this.classMetadataName = null;
        }
        return this;
    }
    
    public JsonParser allowClass(final String classPattern) {
        if (super.classnameWhitelist == null) {
            super.classnameWhitelist = new ArrayList<String>();
        }
        this.classnameWhitelist.add(classPattern);
        return this;
    }
    
    public JsonParser allowAllClasses() {
        this.classnameWhitelist = null;
        return this;
    }
    
    public <T> T parse(final String input, final Class<T> targetType) {
        this.rootType = targetType;
        return this._parse(UnsafeUtil.getChars(input));
    }
    
    public JsonObject parseAsJsonObject(final String input) {
        return new JsonObject(this.parse(input));
    }
    
    public JsonArray parseAsJsonArray(final String input) {
        return new JsonArray(this.parse(input));
    }
    
    public <T> List<T> parseAsList(final String string, final Class<T> componentType) {
        return new JsonParser().map("values", componentType).parse(string);
    }
    
    public <K, V> Map<K, V> parseAsMap(final String string, final Class<K> keyType, final Class<V> valueType) {
        return new JsonParser().map("keys", keyType).map("values", valueType).parse(string);
    }
    
    public <T> T parse(final String input) {
        return this._parse(UnsafeUtil.getChars(input));
    }
    
    public <T> T parse(final char[] input, final Class<T> targetType) {
        this.rootType = targetType;
        return this._parse(input);
    }
    
    public <T> T parse(final char[] input) {
        return (T)this._parse(input);
    }
    
    private <T> T _parse(final char[] input) {
        this.input = input;
        this.total = input.length;
        this.reset();
        this.skipWhiteSpaces();
        Object value;
        try {
            value = this.parseValue(this.rootType, null, null);
        }
        catch (IndexOutOfBoundsException iofbex) {
            this.syntaxError("End of JSON");
            return null;
        }
        this.skipWhiteSpaces();
        if (this.ndx != this.total) {
            this.syntaxError("Trailing chars");
            return null;
        }
        if (this.lazy) {
            value = this.resolveLazyValue(value);
        }
        if (this.classMetadataName != null && this.rootType == null && value instanceof Map) {
            final Map map = (Map)value;
            value = this.mapToBean.map2bean(map, null);
        }
        return (T)value;
    }
    
    protected Object parseValue(final Class targetType, final Class keyType, final Class componentType) {
        final char c = this.input[this.ndx];
        switch (c) {
            case '\'': {
                if (!this.looseMode) {
                    break;
                }
            }
            case '\"': {
                ++this.ndx;
                Object string = this.parseStringContent(c);
                final ValueConverter valueConverter = this.lookupValueConverter();
                if (valueConverter != null) {
                    return valueConverter.convert(string);
                }
                if (targetType != null && targetType != String.class) {
                    string = this.convertType(string, targetType);
                }
                return string;
            }
            case '{': {
                ++this.ndx;
                if (this.lazy) {
                    if (this.notFirstObject) {
                        final Object value = new ObjectParser(this, targetType, keyType, componentType);
                        this.skipObject();
                        return value;
                    }
                    this.notFirstObject = true;
                }
                return this.parseObjectContent(targetType, keyType, componentType);
            }
            case '[': {
                ++this.ndx;
                return this.parseArrayContent(targetType, componentType);
            }
            case '-':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9': {
                Object number = this.parseNumber();
                final ValueConverter valueConverter = this.lookupValueConverter();
                if (valueConverter != null) {
                    return valueConverter.convert(number);
                }
                if (targetType != null) {
                    number = this.convertType(number, targetType);
                }
                return number;
            }
            case 'n': {
                ++this.ndx;
                if (!this.match(JsonParser.N_ULL)) {
                    break;
                }
                final ValueConverter valueConverter = this.lookupValueConverter();
                if (valueConverter != null) {
                    return valueConverter.convert(null);
                }
                return null;
            }
            case 't': {
                ++this.ndx;
                if (!this.match(JsonParser.T_RUE)) {
                    break;
                }
                Object value2 = Boolean.TRUE;
                final ValueConverter valueConverter = this.lookupValueConverter();
                if (valueConverter != null) {
                    return valueConverter.convert(value2);
                }
                if (targetType != null) {
                    value2 = this.convertType(value2, targetType);
                }
                return value2;
            }
            case 'f': {
                ++this.ndx;
                if (!this.match(JsonParser.F_ALSE)) {
                    break;
                }
                Object value2 = Boolean.FALSE;
                final ValueConverter valueConverter = this.lookupValueConverter();
                if (valueConverter != null) {
                    return valueConverter.convert(value2);
                }
                if (targetType != null) {
                    value2 = this.convertType(value2, targetType);
                }
                return value2;
            }
        }
        if (!this.looseMode) {
            this.syntaxError("Invalid char: " + this.input[this.ndx]);
            return null;
        }
        Object string = this.parseUnquotedStringContent();
        final ValueConverter valueConverter = this.lookupValueConverter();
        if (valueConverter != null) {
            return valueConverter.convert(string);
        }
        if (targetType != null && targetType != String.class) {
            string = this.convertType(string, targetType);
        }
        return string;
    }
    
    private Object resolveLazyValue(Object value) {
        if (value instanceof Supplier) {
            value = ((Supplier)value).get();
        }
        return value;
    }
    
    private void skipObject() {
        int bracketCount = 1;
        boolean insideString = false;
        while (this.ndx < this.total) {
            final char c = this.input[this.ndx];
            if (insideString) {
                if (c == '\"' && this.notPrecededByEvenNumberOfBackslashes()) {
                    insideString = false;
                }
            }
            else if (c == '\"') {
                insideString = true;
            }
            else if (c == '{') {
                ++bracketCount;
            }
            else if (c == '}' && --bracketCount == 0) {
                ++this.ndx;
                return;
            }
            ++this.ndx;
        }
    }
    
    private boolean notPrecededByEvenNumberOfBackslashes() {
        int pos = this.ndx;
        int count = 0;
        while (pos > 0 && this.input[pos - 1] == '\\') {
            ++count;
            --pos;
        }
        return count % 2 == 0;
    }
    
    protected String parseString() {
        char quote = '\"';
        if (this.looseMode) {
            quote = this.consumeOneOf('\"', '\'');
            if (quote == '\0') {
                return this.parseUnquotedStringContent();
            }
        }
        else {
            this.consume(quote);
        }
        return this.parseStringContent(quote);
    }
    
    protected String parseStringContent(final char quote) {
        final int startNdx = this.ndx;
        while (true) {
            char c = this.input[this.ndx];
            if (c == quote) {
                ++this.ndx;
                return new String(this.input, startNdx, this.ndx - 1 - startNdx);
            }
            if (c == '\\') {
                this.textLen = this.ndx - startNdx;
                this.growEmpty();
                System.arraycopy(this.input, startNdx, this.text, 0, this.textLen);
                while (true) {
                    c = this.input[this.ndx];
                    if (c == quote) {
                        break;
                    }
                    if (c == '\\') {
                        ++this.ndx;
                        c = this.input[this.ndx];
                        switch (c) {
                            case '\"': {
                                c = '\"';
                                break;
                            }
                            case '\\': {
                                c = '\\';
                                break;
                            }
                            case '/': {
                                c = '/';
                                break;
                            }
                            case 'b': {
                                c = '\b';
                                break;
                            }
                            case 'f': {
                                c = '\f';
                                break;
                            }
                            case 'n': {
                                c = '\n';
                                break;
                            }
                            case 'r': {
                                c = '\r';
                                break;
                            }
                            case 't': {
                                c = '\t';
                                break;
                            }
                            case 'u': {
                                ++this.ndx;
                                c = this.parseUnicode();
                                break;
                            }
                            default: {
                                if (!this.looseMode) {
                                    this.syntaxError("Invalid escape char: " + c);
                                    break;
                                }
                                if (c != '\'') {
                                    c = '\\';
                                    --this.ndx;
                                    break;
                                }
                                break;
                            }
                        }
                    }
                    this.text[this.textLen] = c;
                    ++this.textLen;
                    this.growAndCopy();
                    ++this.ndx;
                }
                ++this.ndx;
                final String str = new String(this.text, 0, this.textLen);
                this.textLen = 0;
                return str;
            }
            ++this.ndx;
        }
    }
    
    protected void growEmpty() {
        if (this.textLen >= this.text.length) {
            final int newSize = this.textLen << 1;
            this.text = new char[newSize];
        }
    }
    
    protected void growAndCopy() {
        if (this.textLen == this.text.length) {
            final int newSize = this.text.length << 1;
            final char[] newText = new char[newSize];
            if (this.textLen > 0) {
                System.arraycopy(this.text, 0, newText, 0, this.textLen);
            }
            this.text = newText;
        }
    }
    
    protected char parseUnicode() {
        final int i0 = CharUtil.hex2int(this.input[this.ndx++]);
        final int i2 = CharUtil.hex2int(this.input[this.ndx++]);
        final int i3 = CharUtil.hex2int(this.input[this.ndx++]);
        final int i4 = CharUtil.hex2int(this.input[this.ndx]);
        return (char)((i0 << 12) + (i2 << 8) + (i3 << 4) + i4);
    }
    
    protected String parseUnquotedStringContent() {
        final int startNdx = this.ndx;
        while (true) {
            final char c = this.input[this.ndx];
            if (c <= ' ' || CharUtil.equalsOne(c, JsonParser.UNQUOTED_DELIMETERS)) {
                break;
            }
            ++this.ndx;
        }
        final int currentNdx = this.ndx;
        this.skipWhiteSpaces();
        return new String(this.input, startNdx, currentNdx - startNdx);
    }
    
    protected Number parseNumber() {
        final int startIndex = this.ndx;
        char c = this.input[this.ndx];
        boolean isDouble = false;
        boolean isExp = false;
        if (c == '-') {
            ++this.ndx;
        }
        while (true) {
            while (!this.isEOF()) {
                c = this.input[this.ndx];
                if (c >= '0' && c <= '9') {
                    ++this.ndx;
                }
                else {
                    if (c > ' ') {
                        if (c != ',' && c != '}') {
                            if (c != ']') {
                                if (c == '.') {
                                    isDouble = true;
                                }
                                else if (c == 'e' || c == 'E') {
                                    isExp = true;
                                }
                                ++this.ndx;
                                continue;
                            }
                        }
                    }
                    final String value = new String(this.input, startIndex, this.ndx - startIndex);
                    if (isDouble) {
                        return Double.valueOf(value);
                    }
                    long longNumber;
                    if (isExp) {
                        longNumber = Double.valueOf(value).longValue();
                    }
                    else if (value.length() >= 19) {
                        final BigInteger bigInteger = new BigInteger(value);
                        if (isGreaterThanLong(bigInteger)) {
                            return bigInteger;
                        }
                        longNumber = bigInteger.longValue();
                    }
                    else {
                        longNumber = Long.parseLong(value);
                    }
                    if (longNumber >= -2147483648L && longNumber <= 2147483647L) {
                        return (int)longNumber;
                    }
                    return longNumber;
                }
            }
            continue;
        }
    }
    
    private static boolean isGreaterThanLong(final BigInteger bigInteger) {
        return bigInteger.compareTo(JsonParser.MAX_LONG) > 0 || bigInteger.compareTo(JsonParser.MIN_LONG) < 0;
    }
    
    protected Object parseArrayContent(Class targetType, Class componentType) {
        if (targetType == Object.class) {
            targetType = List.class;
        }
        targetType = this.replaceWithMappedTypeForPath(targetType);
        if (componentType == null && targetType != null && targetType.isArray()) {
            componentType = targetType.getComponentType();
        }
        this.path.push("values");
        componentType = this.replaceWithMappedTypeForPath(componentType);
        final Collection<Object> target = this.newArrayInstance(targetType);
        boolean koma = false;
        while (true) {
            this.skipWhiteSpaces();
            char c = this.input[this.ndx];
            if (c == ']') {
                if (koma) {
                    this.syntaxError("Trailing comma");
                }
                ++this.ndx;
                this.path.pop();
                return target;
            }
            final Object value = this.parseValue(componentType, null, null);
            target.add(value);
            this.skipWhiteSpaces();
            c = this.input[this.ndx];
            switch (c) {
                case ']': {
                    ++this.ndx;
                    this.path.pop();
                    if (targetType != null) {
                        return this.convertType(target, targetType);
                    }
                    return target;
                }
                case ',': {
                    ++this.ndx;
                    koma = true;
                    continue;
                }
                default: {
                    this.syntaxError("Invalid char: expected ] or ,");
                    continue;
                }
            }
        }
    }
    
    protected Object parseObjectContent(Class targetType, Class valueKeyType, Class valueType) {
        if (targetType == Object.class) {
            targetType = Map.class;
        }
        targetType = (Class<Map>)this.replaceWithMappedTypeForPath(targetType);
        boolean isTargetTypeMap = true;
        boolean isTargetRealTypeMap = true;
        ClassDescriptor targetTypeClassDescriptor = null;
        TypeData typeData = null;
        if (targetType != null) {
            targetTypeClassDescriptor = ClassIntrospector.get().lookup((Class)targetType);
            isTargetRealTypeMap = targetTypeClassDescriptor.isMap();
            typeData = this.jsonAnnotationManager.lookupTypeData(targetType);
        }
        if (isTargetRealTypeMap) {
            this.path.push("keys");
            valueKeyType = this.replaceWithMappedTypeForPath(valueKeyType);
            this.path.pop();
        }
        Object target;
        if (this.classMetadataName == null) {
            target = this.newObjectInstance(targetType);
            isTargetTypeMap = isTargetRealTypeMap;
        }
        else {
            target = this.mapSupplier.get();
        }
        boolean koma = false;
    Label_0530:
        while (true) {
            this.skipWhiteSpaces();
            char c = this.input[this.ndx];
            if (c == '}') {
                if (koma) {
                    this.syntaxError("Trailing comma");
                }
                ++this.ndx;
                break;
            }
            koma = false;
            final String keyOriginal;
            String key = keyOriginal = this.parseString();
            this.skipWhiteSpaces();
            this.consume(':');
            this.skipWhiteSpaces();
            PropertyDescriptor pd = null;
            Class propertyType = null;
            Class keyType = null;
            Class componentType = null;
            if (!isTargetRealTypeMap) {
                key = this.jsonAnnotationManager.resolveRealName(targetType, key);
            }
            if (!isTargetTypeMap) {
                pd = targetTypeClassDescriptor.getPropertyDescriptor(key, true);
                if (pd != null) {
                    propertyType = pd.getType();
                    keyType = pd.resolveKeyType(true);
                    componentType = pd.resolveComponentType(true);
                }
            }
            if (!isTargetTypeMap) {
                this.path.push(key);
                Object value = this.parseValue(propertyType, keyType, componentType);
                this.path.pop();
                if (typeData.rules.match((Object)keyOriginal, !typeData.strict) && pd != null) {
                    if (this.lazy) {
                        value = this.resolveLazyValue(value);
                    }
                    this.injectValueIntoObject(target, pd, value);
                }
            }
            else {
                Object keyValue = key;
                if (valueKeyType != null) {
                    keyValue = this.convertType(key, valueKeyType);
                }
                if (isTargetRealTypeMap) {
                    this.path.push("values", key);
                    valueType = this.replaceWithMappedTypeForPath(valueType);
                }
                else {
                    this.path.push(key);
                }
                final Object value = this.parseValue(valueType, null, null);
                this.path.pop();
                ((Map)target).put(keyValue, value);
            }
            this.skipWhiteSpaces();
            c = this.input[this.ndx];
            switch (c) {
                case '}': {
                    ++this.ndx;
                    break Label_0530;
                }
                case ',': {
                    ++this.ndx;
                    koma = true;
                    continue;
                }
                default: {
                    this.syntaxError("Invalid char: expected } or ,");
                    continue;
                }
            }
        }
        if (this.classMetadataName != null) {
            target = this.mapToBean.map2bean((Map)target, targetType);
        }
        return target;
    }
    
    protected void consume(final char c) {
        if (this.input[this.ndx] != c) {
            this.syntaxError("Invalid char: expected " + c);
        }
        ++this.ndx;
    }
    
    protected char consumeOneOf(final char c1, final char c2) {
        final char c3 = this.input[this.ndx];
        if (c3 != c1 && c3 != c2) {
            return '\0';
        }
        ++this.ndx;
        return c3;
    }
    
    protected boolean isEOF() {
        return this.ndx >= this.total;
    }
    
    protected final void skipWhiteSpaces() {
        while (!this.isEOF()) {
            if (this.input[this.ndx] > ' ') {
                return;
            }
            ++this.ndx;
        }
    }
    
    protected final boolean match(final char[] target) {
        for (final char c : target) {
            if (this.input[this.ndx] != c) {
                return false;
            }
            ++this.ndx;
        }
        return true;
    }
    
    protected void syntaxError(final String message) {
        String left = "...";
        String right = "...";
        final int offset = 10;
        int from = this.ndx - offset;
        if (from < 0) {
            from = 0;
            left = "";
        }
        int to = this.ndx + offset;
        if (to > this.input.length) {
            to = this.input.length;
            right = "";
        }
        final CharSequence str = (CharSequence)CharArraySequence.of(this.input, from, to - from);
        throw new JsonException("Syntax error! " + message + "\noffset: " + this.ndx + " near: \"" + left + (Object)str + right + "\"");
    }
    
    static {
        T_RUE = new char[] { 'r', 'u', 'e' };
        F_ALSE = new char[] { 'a', 'l', 's', 'e' };
        N_ULL = new char[] { 'u', 'l', 'l' };
        UNQUOTED_DELIMETERS = ",:[]{}\\\"'".toCharArray();
        MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
        MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);
    }
    
    public static class Defaults
    {
        public static final String DEFAULT_CLASS_METADATA_NAME = "__class";
        public static boolean lazy;
        public static boolean useAltPathsByParser;
        public static boolean loose;
        public static String classMetadataName;
        public static boolean strictTypes;
        
        static {
            Defaults.lazy = false;
            Defaults.useAltPathsByParser = false;
            Defaults.loose = false;
            Defaults.classMetadataName = null;
            Defaults.strictTypes = true;
        }
    }
}
