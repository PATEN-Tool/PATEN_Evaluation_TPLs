// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.util;

import java.util.StringJoiner;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Properties;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.TimeZone;
import java.util.Locale;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Deque;
import java.util.Collection;
import java.util.ArrayDeque;
import org.springframework.lang.Nullable;

public abstract class StringUtils
{
    private static final String[] EMPTY_STRING_ARRAY;
    private static final String FOLDER_SEPARATOR = "/";
    private static final String WINDOWS_FOLDER_SEPARATOR = "\\";
    private static final String TOP_PATH = "..";
    private static final String CURRENT_PATH = ".";
    private static final char EXTENSION_SEPARATOR = '.';
    
    public static boolean isEmpty(@Nullable final Object str) {
        return str == null || "".equals(str);
    }
    
    public static boolean hasLength(@Nullable final CharSequence str) {
        return str != null && str.length() > 0;
    }
    
    public static boolean hasLength(@Nullable final String str) {
        return str != null && !str.isEmpty();
    }
    
    public static boolean hasText(@Nullable final CharSequence str) {
        return str != null && str.length() > 0 && containsText(str);
    }
    
    public static boolean hasText(@Nullable final String str) {
        return str != null && !str.isEmpty() && containsText(str);
    }
    
    private static boolean containsText(final CharSequence str) {
        for (int strLen = str.length(), i = 0; i < strLen; ++i) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean containsWhitespace(@Nullable final CharSequence str) {
        if (!hasLength(str)) {
            return false;
        }
        for (int strLen = str.length(), i = 0; i < strLen; ++i) {
            if (Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean containsWhitespace(@Nullable final String str) {
        return containsWhitespace((CharSequence)str);
    }
    
    public static String trimWhitespace(final String str) {
        if (!hasLength(str)) {
            return str;
        }
        int beginIndex;
        int endIndex;
        for (beginIndex = 0, endIndex = str.length() - 1; beginIndex <= endIndex && Character.isWhitespace(str.charAt(beginIndex)); ++beginIndex) {}
        while (endIndex > beginIndex && Character.isWhitespace(str.charAt(endIndex))) {
            --endIndex;
        }
        return str.substring(beginIndex, endIndex + 1);
    }
    
    public static String trimAllWhitespace(final String str) {
        if (!hasLength(str)) {
            return str;
        }
        final int len = str.length();
        final StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < len; ++i) {
            final char c = str.charAt(i);
            if (!Character.isWhitespace(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    public static String trimLeadingWhitespace(final String str) {
        if (!hasLength(str)) {
            return str;
        }
        final StringBuilder sb = new StringBuilder(str);
        while (sb.length() > 0 && Character.isWhitespace(sb.charAt(0))) {
            sb.deleteCharAt(0);
        }
        return sb.toString();
    }
    
    public static String trimTrailingWhitespace(final String str) {
        if (!hasLength(str)) {
            return str;
        }
        final StringBuilder sb = new StringBuilder(str);
        while (sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }
    
    public static String trimLeadingCharacter(final String str, final char leadingCharacter) {
        if (!hasLength(str)) {
            return str;
        }
        final StringBuilder sb = new StringBuilder(str);
        while (sb.length() > 0 && sb.charAt(0) == leadingCharacter) {
            sb.deleteCharAt(0);
        }
        return sb.toString();
    }
    
    public static String trimTrailingCharacter(final String str, final char trailingCharacter) {
        if (!hasLength(str)) {
            return str;
        }
        final StringBuilder sb = new StringBuilder(str);
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == trailingCharacter) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }
    
    public static boolean matchesCharacter(@Nullable final String str, final char singleCharacter) {
        return str != null && str.length() == 1 && str.charAt(0) == singleCharacter;
    }
    
    public static boolean startsWithIgnoreCase(@Nullable final String str, @Nullable final String prefix) {
        return str != null && prefix != null && str.length() >= prefix.length() && str.regionMatches(true, 0, prefix, 0, prefix.length());
    }
    
    public static boolean endsWithIgnoreCase(@Nullable final String str, @Nullable final String suffix) {
        return str != null && suffix != null && str.length() >= suffix.length() && str.regionMatches(true, str.length() - suffix.length(), suffix, 0, suffix.length());
    }
    
    public static boolean substringMatch(final CharSequence str, final int index, final CharSequence substring) {
        if (index + substring.length() > str.length()) {
            return false;
        }
        for (int i = 0; i < substring.length(); ++i) {
            if (str.charAt(index + i) != substring.charAt(i)) {
                return false;
            }
        }
        return true;
    }
    
    public static int countOccurrencesOf(final String str, final String sub) {
        if (!hasLength(str) || !hasLength(sub)) {
            return 0;
        }
        int count = 0;
        int idx;
        for (int pos = 0; (idx = str.indexOf(sub, pos)) != -1; pos = idx + sub.length()) {
            ++count;
        }
        return count;
    }
    
    public static String replace(final String inString, final String oldPattern, @Nullable final String newPattern) {
        if (!hasLength(inString) || !hasLength(oldPattern) || newPattern == null) {
            return inString;
        }
        int index = inString.indexOf(oldPattern);
        if (index == -1) {
            return inString;
        }
        int capacity = inString.length();
        if (newPattern.length() > oldPattern.length()) {
            capacity += 16;
        }
        final StringBuilder sb = new StringBuilder(capacity);
        int pos = 0;
        final int patLen = oldPattern.length();
        while (index >= 0) {
            sb.append(inString, pos, index);
            sb.append(newPattern);
            pos = index + patLen;
            index = inString.indexOf(oldPattern, pos);
        }
        sb.append(inString, pos, inString.length());
        return sb.toString();
    }
    
    public static String delete(final String inString, final String pattern) {
        return replace(inString, pattern, "");
    }
    
    public static String deleteAny(final String inString, @Nullable final String charsToDelete) {
        if (!hasLength(inString) || !hasLength(charsToDelete)) {
            return inString;
        }
        int lastCharIndex = 0;
        final char[] result = new char[inString.length()];
        for (int i = 0; i < inString.length(); ++i) {
            final char c = inString.charAt(i);
            if (charsToDelete.indexOf(c) == -1) {
                result[lastCharIndex++] = c;
            }
        }
        if (lastCharIndex == inString.length()) {
            return inString;
        }
        return new String(result, 0, lastCharIndex);
    }
    
    @Nullable
    public static String quote(@Nullable final String str) {
        return (str != null) ? ("'" + str + "'") : null;
    }
    
    @Nullable
    public static Object quoteIfString(@Nullable final Object obj) {
        return (obj instanceof String) ? quote((String)obj) : obj;
    }
    
    public static String unqualify(final String qualifiedName) {
        return unqualify(qualifiedName, '.');
    }
    
    public static String unqualify(final String qualifiedName, final char separator) {
        return qualifiedName.substring(qualifiedName.lastIndexOf(separator) + 1);
    }
    
    public static String capitalize(final String str) {
        return changeFirstCharacterCase(str, true);
    }
    
    public static String uncapitalize(final String str) {
        return changeFirstCharacterCase(str, false);
    }
    
    private static String changeFirstCharacterCase(final String str, final boolean capitalize) {
        if (!hasLength(str)) {
            return str;
        }
        final char baseChar = str.charAt(0);
        char updatedChar;
        if (capitalize) {
            updatedChar = Character.toUpperCase(baseChar);
        }
        else {
            updatedChar = Character.toLowerCase(baseChar);
        }
        if (baseChar == updatedChar) {
            return str;
        }
        final char[] chars = str.toCharArray();
        chars[0] = updatedChar;
        return new String(chars);
    }
    
    @Nullable
    public static String getFilename(@Nullable final String path) {
        if (path == null) {
            return null;
        }
        final int separatorIndex = path.lastIndexOf("/");
        return (separatorIndex != -1) ? path.substring(separatorIndex + 1) : path;
    }
    
    @Nullable
    public static String getFilenameExtension(@Nullable final String path) {
        if (path == null) {
            return null;
        }
        final int extIndex = path.lastIndexOf(46);
        if (extIndex == -1) {
            return null;
        }
        final int folderIndex = path.lastIndexOf("/");
        if (folderIndex > extIndex) {
            return null;
        }
        return path.substring(extIndex + 1);
    }
    
    public static String stripFilenameExtension(final String path) {
        final int extIndex = path.lastIndexOf(46);
        if (extIndex == -1) {
            return path;
        }
        final int folderIndex = path.lastIndexOf("/");
        if (folderIndex > extIndex) {
            return path;
        }
        return path.substring(0, extIndex);
    }
    
    public static String applyRelativePath(final String path, final String relativePath) {
        final int separatorIndex = path.lastIndexOf("/");
        if (separatorIndex != -1) {
            String newPath = path.substring(0, separatorIndex);
            if (!relativePath.startsWith("/")) {
                newPath += "/";
            }
            return newPath + relativePath;
        }
        return relativePath;
    }
    
    public static String cleanPath(final String path) {
        if (!hasLength(path)) {
            return path;
        }
        String pathToUse = replace(path, "\\", "/");
        if (pathToUse.indexOf(46) == -1) {
            return pathToUse;
        }
        final int prefixIndex = pathToUse.indexOf(58);
        String prefix = "";
        if (prefixIndex != -1) {
            prefix = pathToUse.substring(0, prefixIndex + 1);
            if (prefix.contains("/")) {
                prefix = "";
            }
            else {
                pathToUse = pathToUse.substring(prefixIndex + 1);
            }
        }
        if (pathToUse.startsWith("/")) {
            prefix += "/";
            pathToUse = pathToUse.substring(1);
        }
        final String[] pathArray = delimitedListToStringArray(pathToUse, "/");
        final Deque<String> pathElements = new ArrayDeque<String>();
        int tops = 0;
        for (int i = pathArray.length - 1; i >= 0; --i) {
            final String element = pathArray[i];
            if (!".".equals(element)) {
                if ("..".equals(element)) {
                    ++tops;
                }
                else if (tops > 0) {
                    --tops;
                }
                else {
                    pathElements.addFirst(element);
                }
            }
        }
        if (pathArray.length == pathElements.size()) {
            return prefix + pathToUse;
        }
        for (int i = 0; i < tops; ++i) {
            pathElements.addFirst("..");
        }
        if (pathElements.size() == 1 && pathElements.getLast().isEmpty() && !prefix.endsWith("/")) {
            pathElements.addFirst(".");
        }
        return prefix + collectionToDelimitedString(pathElements, "/");
    }
    
    public static boolean pathEquals(final String path1, final String path2) {
        return cleanPath(path1).equals(cleanPath(path2));
    }
    
    public static String uriDecode(final String source, final Charset charset) {
        final int length = source.length();
        if (length == 0) {
            return source;
        }
        Assert.notNull(charset, "Charset must not be null");
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(length);
        boolean changed = false;
        for (int i = 0; i < length; ++i) {
            final int ch = source.charAt(i);
            if (ch == 37) {
                if (i + 2 >= length) {
                    throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
                }
                final char hex1 = source.charAt(i + 1);
                final char hex2 = source.charAt(i + 2);
                final int u = Character.digit(hex1, 16);
                final int l = Character.digit(hex2, 16);
                if (u == -1 || l == -1) {
                    throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
                }
                baos.write((char)((u << 4) + l));
                i += 2;
                changed = true;
            }
            else {
                baos.write(ch);
            }
        }
        return changed ? StreamUtils.copyToString(baos, charset) : source;
    }
    
    @Nullable
    public static Locale parseLocale(final String localeValue) {
        final String[] tokens = tokenizeLocaleSource(localeValue);
        if (tokens.length == 1) {
            validateLocalePart(localeValue);
            final Locale resolved = Locale.forLanguageTag(localeValue);
            if (resolved.getLanguage().length() > 0) {
                return resolved;
            }
        }
        return parseLocaleTokens(localeValue, tokens);
    }
    
    @Nullable
    public static Locale parseLocaleString(final String localeString) {
        return parseLocaleTokens(localeString, tokenizeLocaleSource(localeString));
    }
    
    private static String[] tokenizeLocaleSource(final String localeSource) {
        return tokenizeToStringArray(localeSource, "_ ", false, false);
    }
    
    @Nullable
    private static Locale parseLocaleTokens(final String localeString, final String[] tokens) {
        final String language = (tokens.length > 0) ? tokens[0] : "";
        String country = (tokens.length > 1) ? tokens[1] : "";
        validateLocalePart(language);
        validateLocalePart(country);
        String variant = "";
        if (tokens.length > 2) {
            final int endIndexOfCountryCode = localeString.indexOf(country, language.length()) + country.length();
            variant = trimLeadingWhitespace(localeString.substring(endIndexOfCountryCode));
            if (variant.startsWith("_")) {
                variant = trimLeadingCharacter(variant, '_');
            }
        }
        if (variant.isEmpty() && country.startsWith("#")) {
            variant = country;
            country = "";
        }
        return (language.length() > 0) ? new Locale(language, country, variant) : null;
    }
    
    private static void validateLocalePart(final String localePart) {
        for (int i = 0; i < localePart.length(); ++i) {
            final char ch = localePart.charAt(i);
            if (ch != ' ' && ch != '_' && ch != '-' && ch != '#' && !Character.isLetterOrDigit(ch)) {
                throw new IllegalArgumentException("Locale part \"" + localePart + "\" contains invalid characters");
            }
        }
    }
    
    @Deprecated
    public static String toLanguageTag(final Locale locale) {
        return locale.getLanguage() + (hasText(locale.getCountry()) ? ("-" + locale.getCountry()) : "");
    }
    
    public static TimeZone parseTimeZoneString(final String timeZoneString) {
        final TimeZone timeZone = TimeZone.getTimeZone(timeZoneString);
        if ("GMT".equals(timeZone.getID()) && !timeZoneString.startsWith("GMT")) {
            throw new IllegalArgumentException("Invalid time zone specification '" + timeZoneString + "'");
        }
        return timeZone;
    }
    
    public static String[] toStringArray(@Nullable final Collection<String> collection) {
        return CollectionUtils.isEmpty(collection) ? StringUtils.EMPTY_STRING_ARRAY : collection.toArray(StringUtils.EMPTY_STRING_ARRAY);
    }
    
    public static String[] toStringArray(@Nullable final Enumeration<String> enumeration) {
        return (enumeration != null) ? toStringArray(Collections.list(enumeration)) : StringUtils.EMPTY_STRING_ARRAY;
    }
    
    public static String[] addStringToArray(@Nullable final String[] array, final String str) {
        if (ObjectUtils.isEmpty(array)) {
            return new String[] { str };
        }
        final String[] newArr = new String[array.length + 1];
        System.arraycopy(array, 0, newArr, 0, array.length);
        newArr[array.length] = str;
        return newArr;
    }
    
    @Nullable
    public static String[] concatenateStringArrays(@Nullable final String[] array1, @Nullable final String[] array2) {
        if (ObjectUtils.isEmpty(array1)) {
            return array2;
        }
        if (ObjectUtils.isEmpty(array2)) {
            return array1;
        }
        final String[] newArr = new String[array1.length + array2.length];
        System.arraycopy(array1, 0, newArr, 0, array1.length);
        System.arraycopy(array2, 0, newArr, array1.length, array2.length);
        return newArr;
    }
    
    @Deprecated
    @Nullable
    public static String[] mergeStringArrays(@Nullable final String[] array1, @Nullable final String[] array2) {
        if (ObjectUtils.isEmpty(array1)) {
            return array2;
        }
        if (ObjectUtils.isEmpty(array2)) {
            return array1;
        }
        final List<String> result = new ArrayList<String>(Arrays.asList(array1));
        for (final String str : array2) {
            if (!result.contains(str)) {
                result.add(str);
            }
        }
        return toStringArray(result);
    }
    
    public static String[] sortStringArray(final String[] array) {
        if (ObjectUtils.isEmpty(array)) {
            return array;
        }
        Arrays.sort(array);
        return array;
    }
    
    public static String[] trimArrayElements(final String[] array) {
        if (ObjectUtils.isEmpty(array)) {
            return array;
        }
        final String[] result = new String[array.length];
        for (int i = 0; i < array.length; ++i) {
            final String element = array[i];
            result[i] = ((element != null) ? element.trim() : null);
        }
        return result;
    }
    
    public static String[] removeDuplicateStrings(final String[] array) {
        if (ObjectUtils.isEmpty(array)) {
            return array;
        }
        final Set<String> set = new LinkedHashSet<String>(Arrays.asList(array));
        return toStringArray(set);
    }
    
    @Nullable
    public static String[] split(@Nullable final String toSplit, @Nullable final String delimiter) {
        if (!hasLength(toSplit) || !hasLength(delimiter)) {
            return null;
        }
        final int offset = toSplit.indexOf(delimiter);
        if (offset < 0) {
            return null;
        }
        final String beforeDelimiter = toSplit.substring(0, offset);
        final String afterDelimiter = toSplit.substring(offset + delimiter.length());
        return new String[] { beforeDelimiter, afterDelimiter };
    }
    
    @Nullable
    public static Properties splitArrayElementsIntoProperties(final String[] array, final String delimiter) {
        return splitArrayElementsIntoProperties(array, delimiter, null);
    }
    
    @Nullable
    public static Properties splitArrayElementsIntoProperties(final String[] array, final String delimiter, @Nullable final String charsToDelete) {
        if (ObjectUtils.isEmpty(array)) {
            return null;
        }
        final Properties result = new Properties();
        for (String element : array) {
            if (charsToDelete != null) {
                element = deleteAny(element, charsToDelete);
            }
            final String[] splittedElement = split(element, delimiter);
            if (splittedElement != null) {
                result.setProperty(splittedElement[0].trim(), splittedElement[1].trim());
            }
        }
        return result;
    }
    
    public static String[] tokenizeToStringArray(@Nullable final String str, final String delimiters) {
        return tokenizeToStringArray(str, delimiters, true, true);
    }
    
    public static String[] tokenizeToStringArray(@Nullable final String str, final String delimiters, final boolean trimTokens, final boolean ignoreEmptyTokens) {
        if (str == null) {
            return StringUtils.EMPTY_STRING_ARRAY;
        }
        final StringTokenizer st = new StringTokenizer(str, delimiters);
        final List<String> tokens = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (trimTokens) {
                token = token.trim();
            }
            if (!ignoreEmptyTokens || token.length() > 0) {
                tokens.add(token);
            }
        }
        return toStringArray(tokens);
    }
    
    public static String[] delimitedListToStringArray(@Nullable final String str, @Nullable final String delimiter) {
        return delimitedListToStringArray(str, delimiter, null);
    }
    
    public static String[] delimitedListToStringArray(@Nullable final String str, @Nullable final String delimiter, @Nullable final String charsToDelete) {
        if (str == null) {
            return StringUtils.EMPTY_STRING_ARRAY;
        }
        if (delimiter == null) {
            return new String[] { str };
        }
        final List<String> result = new ArrayList<String>();
        if (delimiter.isEmpty()) {
            for (int i = 0; i < str.length(); ++i) {
                result.add(deleteAny(str.substring(i, i + 1), charsToDelete));
            }
        }
        else {
            int pos;
            int delPos;
            for (pos = 0; (delPos = str.indexOf(delimiter, pos)) != -1; pos = delPos + delimiter.length()) {
                result.add(deleteAny(str.substring(pos, delPos), charsToDelete));
            }
            if (str.length() > 0 && pos <= str.length()) {
                result.add(deleteAny(str.substring(pos), charsToDelete));
            }
        }
        return toStringArray(result);
    }
    
    public static String[] commaDelimitedListToStringArray(@Nullable final String str) {
        return delimitedListToStringArray(str, ",");
    }
    
    public static Set<String> commaDelimitedListToSet(@Nullable final String str) {
        final String[] tokens = commaDelimitedListToStringArray(str);
        return new LinkedHashSet<String>(Arrays.asList(tokens));
    }
    
    public static String collectionToDelimitedString(@Nullable final Collection<?> coll, final String delim, final String prefix, final String suffix) {
        if (CollectionUtils.isEmpty(coll)) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        final Iterator<?> it = coll.iterator();
        while (it.hasNext()) {
            sb.append(prefix).append(it.next()).append(suffix);
            if (it.hasNext()) {
                sb.append(delim);
            }
        }
        return sb.toString();
    }
    
    public static String collectionToDelimitedString(@Nullable final Collection<?> coll, final String delim) {
        return collectionToDelimitedString(coll, delim, "", "");
    }
    
    public static String collectionToCommaDelimitedString(@Nullable final Collection<?> coll) {
        return collectionToDelimitedString(coll, ",");
    }
    
    public static String arrayToDelimitedString(@Nullable final Object[] arr, final String delim) {
        if (ObjectUtils.isEmpty(arr)) {
            return "";
        }
        if (arr.length == 1) {
            return ObjectUtils.nullSafeToString(arr[0]);
        }
        final StringJoiner sj = new StringJoiner(delim);
        for (final Object elem : arr) {
            sj.add(String.valueOf(elem));
        }
        return sj.toString();
    }
    
    public static String arrayToCommaDelimitedString(@Nullable final Object[] arr) {
        return arrayToDelimitedString(arr, ",");
    }
    
    static {
        EMPTY_STRING_ARRAY = new String[0];
    }
}
