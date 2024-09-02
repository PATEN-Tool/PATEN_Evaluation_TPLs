// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.util;

import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.Locale;
import java.util.List;
import java.util.Collection;
import java.util.LinkedList;

public abstract class StringUtils
{
    private static final String FOLDER_SEPARATOR = "/";
    private static final String WINDOWS_FOLDER_SEPARATOR = "\\";
    private static final String TOP_PATH = "..";
    private static final String CURRENT_PATH = ".";
    private static final char EXTENSION_SEPARATOR = '.';
    
    public static boolean isEmpty(final Object str) {
        return str == null || "".equals(str);
    }
    
    public static boolean hasLength(final CharSequence str) {
        return str != null && str.length() > 0;
    }
    
    public static boolean hasLength(final String str) {
        return hasLength((CharSequence)str);
    }
    
    public static boolean hasText(final CharSequence str) {
        if (!hasLength(str)) {
            return false;
        }
        for (int strLen = str.length(), i = 0; i < strLen; ++i) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean hasText(final String str) {
        return hasText((CharSequence)str);
    }
    
    public static boolean containsWhitespace(final CharSequence str) {
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
    
    public static boolean containsWhitespace(final String str) {
        return containsWhitespace((CharSequence)str);
    }
    
    public static String trimWhitespace(final String str) {
        if (!hasLength(str)) {
            return str;
        }
        final StringBuilder sb = new StringBuilder(str);
        while (sb.length() > 0 && Character.isWhitespace(sb.charAt(0))) {
            sb.deleteCharAt(0);
        }
        while (sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }
    
    public static String trimAllWhitespace(final String str) {
        if (!hasLength(str)) {
            return str;
        }
        final StringBuilder sb = new StringBuilder(str);
        int index = 0;
        while (sb.length() > index) {
            if (Character.isWhitespace(sb.charAt(index))) {
                sb.deleteCharAt(index);
            }
            else {
                ++index;
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
    
    public static boolean startsWithIgnoreCase(final String str, final String prefix) {
        if (str == null || prefix == null) {
            return false;
        }
        if (str.startsWith(prefix)) {
            return true;
        }
        if (str.length() < prefix.length()) {
            return false;
        }
        final String lcStr = str.substring(0, prefix.length()).toLowerCase();
        final String lcPrefix = prefix.toLowerCase();
        return lcStr.equals(lcPrefix);
    }
    
    public static boolean endsWithIgnoreCase(final String str, final String suffix) {
        if (str == null || suffix == null) {
            return false;
        }
        if (str.endsWith(suffix)) {
            return true;
        }
        if (str.length() < suffix.length()) {
            return false;
        }
        final String lcStr = str.substring(str.length() - suffix.length()).toLowerCase();
        final String lcSuffix = suffix.toLowerCase();
        return lcStr.equals(lcSuffix);
    }
    
    public static boolean substringMatch(final CharSequence str, final int index, final CharSequence substring) {
        for (int j = 0; j < substring.length(); ++j) {
            final int i = index + j;
            if (i >= str.length() || str.charAt(i) != substring.charAt(j)) {
                return false;
            }
        }
        return true;
    }
    
    public static int countOccurrencesOf(final String str, final String sub) {
        if (str == null || sub == null || str.length() == 0 || sub.length() == 0) {
            return 0;
        }
        int count = 0;
        int idx;
        for (int pos = 0; (idx = str.indexOf(sub, pos)) != -1; pos = idx + sub.length()) {
            ++count;
        }
        return count;
    }
    
    public static String replace(final String inString, final String oldPattern, final String newPattern) {
        if (!hasLength(inString) || !hasLength(oldPattern) || newPattern == null) {
            return inString;
        }
        final StringBuilder sb = new StringBuilder();
        int pos = 0;
        int index = inString.indexOf(oldPattern);
        final int patLen = oldPattern.length();
        while (index >= 0) {
            sb.append(inString.substring(pos, index));
            sb.append(newPattern);
            pos = index + patLen;
            index = inString.indexOf(oldPattern, pos);
        }
        sb.append(inString.substring(pos));
        return sb.toString();
    }
    
    public static String delete(final String inString, final String pattern) {
        return replace(inString, pattern, "");
    }
    
    public static String deleteAny(final String inString, final String charsToDelete) {
        if (!hasLength(inString) || !hasLength(charsToDelete)) {
            return inString;
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < inString.length(); ++i) {
            final char c = inString.charAt(i);
            if (charsToDelete.indexOf(c) == -1) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    public static String quote(final String str) {
        return (str != null) ? ("'" + str + "'") : null;
    }
    
    public static Object quoteIfString(final Object obj) {
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
        if (str == null || str.length() == 0) {
            return str;
        }
        final StringBuilder sb = new StringBuilder(str.length());
        if (capitalize) {
            sb.append(Character.toUpperCase(str.charAt(0)));
        }
        else {
            sb.append(Character.toLowerCase(str.charAt(0)));
        }
        sb.append(str.substring(1));
        return sb.toString();
    }
    
    public static String getFilename(final String path) {
        if (path == null) {
            return null;
        }
        final int separatorIndex = path.lastIndexOf("/");
        return (separatorIndex != -1) ? path.substring(separatorIndex + 1) : path;
    }
    
    public static String getFilenameExtension(final String path) {
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
        if (path == null) {
            return null;
        }
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
        if (path == null) {
            return null;
        }
        String pathToUse = replace(path, "\\", "/");
        final int prefixIndex = pathToUse.indexOf(":");
        String prefix = "";
        if (prefixIndex != -1) {
            prefix = pathToUse.substring(0, prefixIndex + 1);
            pathToUse = pathToUse.substring(prefixIndex + 1);
        }
        if (pathToUse.startsWith("/")) {
            prefix += "/";
            pathToUse = pathToUse.substring(1);
        }
        final String[] pathArray = delimitedListToStringArray(pathToUse, "/");
        final List<String> pathElements = new LinkedList<String>();
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
                    pathElements.add(0, element);
                }
            }
        }
        for (int i = 0; i < tops; ++i) {
            pathElements.add(0, "..");
        }
        return prefix + collectionToDelimitedString(pathElements, "/");
    }
    
    public static boolean pathEquals(final String path1, final String path2) {
        return cleanPath(path1).equals(cleanPath(path2));
    }
    
    public static Locale parseLocaleString(final String localeString) {
        final String[] parts = tokenizeToStringArray(localeString, "_ ", false, false);
        final String language = (parts.length > 0) ? parts[0] : "";
        final String country = (parts.length > 1) ? parts[1] : "";
        validateLocalePart(language);
        validateLocalePart(country);
        String variant = "";
        if (parts.length > 2) {
            final int endIndexOfCountryCode = localeString.lastIndexOf(country) + country.length();
            variant = trimLeadingWhitespace(localeString.substring(endIndexOfCountryCode));
            if (variant.startsWith("_")) {
                variant = trimLeadingCharacter(variant, '_');
            }
        }
        return (language.length() > 0) ? new Locale(language, country, variant) : null;
    }
    
    private static void validateLocalePart(final String localePart) {
        for (int i = 0; i < localePart.length(); ++i) {
            final char ch = localePart.charAt(i);
            if (ch != '_' && ch != ' ' && !Character.isLetterOrDigit(ch)) {
                throw new IllegalArgumentException("Locale part \"" + localePart + "\" contains invalid characters");
            }
        }
    }
    
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
    
    public static String[] addStringToArray(final String[] array, final String str) {
        if (ObjectUtils.isEmpty(array)) {
            return new String[] { str };
        }
        final String[] newArr = new String[array.length + 1];
        System.arraycopy(array, 0, newArr, 0, array.length);
        newArr[array.length] = str;
        return newArr;
    }
    
    public static String[] concatenateStringArrays(final String[] array1, final String[] array2) {
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
    
    public static String[] mergeStringArrays(final String[] array1, final String[] array2) {
        if (ObjectUtils.isEmpty(array1)) {
            return array2;
        }
        if (ObjectUtils.isEmpty(array2)) {
            return array1;
        }
        final List<String> result = new ArrayList<String>();
        result.addAll(Arrays.asList(array1));
        for (final String str : array2) {
            if (!result.contains(str)) {
                result.add(str);
            }
        }
        return toStringArray(result);
    }
    
    public static String[] sortStringArray(final String[] array) {
        if (ObjectUtils.isEmpty(array)) {
            return new String[0];
        }
        Arrays.sort(array);
        return array;
    }
    
    public static String[] toStringArray(final Collection<String> collection) {
        if (collection == null) {
            return null;
        }
        return collection.toArray(new String[collection.size()]);
    }
    
    public static String[] toStringArray(final Enumeration<String> enumeration) {
        if (enumeration == null) {
            return null;
        }
        final List<String> list = Collections.list(enumeration);
        return list.toArray(new String[list.size()]);
    }
    
    public static String[] trimArrayElements(final String[] array) {
        if (ObjectUtils.isEmpty(array)) {
            return new String[0];
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
        final Set<String> set = new TreeSet<String>();
        for (final String element : array) {
            set.add(element);
        }
        return toStringArray(set);
    }
    
    public static String[] split(final String toSplit, final String delimiter) {
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
    
    public static Properties splitArrayElementsIntoProperties(final String[] array, final String delimiter) {
        return splitArrayElementsIntoProperties(array, delimiter, null);
    }
    
    public static Properties splitArrayElementsIntoProperties(final String[] array, final String delimiter, final String charsToDelete) {
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
    
    public static String[] tokenizeToStringArray(final String str, final String delimiters) {
        return tokenizeToStringArray(str, delimiters, true, true);
    }
    
    public static String[] tokenizeToStringArray(final String str, final String delimiters, final boolean trimTokens, final boolean ignoreEmptyTokens) {
        if (str == null) {
            return null;
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
    
    public static String[] delimitedListToStringArray(final String str, final String delimiter) {
        return delimitedListToStringArray(str, delimiter, null);
    }
    
    public static String[] delimitedListToStringArray(final String str, final String delimiter, final String charsToDelete) {
        if (str == null) {
            return new String[0];
        }
        if (delimiter == null) {
            return new String[] { str };
        }
        final List<String> result = new ArrayList<String>();
        if ("".equals(delimiter)) {
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
    
    public static String[] commaDelimitedListToStringArray(final String str) {
        return delimitedListToStringArray(str, ",");
    }
    
    public static Set<String> commaDelimitedListToSet(final String str) {
        final Set<String> set = new TreeSet<String>();
        final String[] commaDelimitedListToStringArray;
        final String[] tokens = commaDelimitedListToStringArray = commaDelimitedListToStringArray(str);
        for (final String token : commaDelimitedListToStringArray) {
            set.add(token);
        }
        return set;
    }
    
    public static String collectionToDelimitedString(final Collection<?> coll, final String delim, final String prefix, final String suffix) {
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
    
    public static String collectionToDelimitedString(final Collection<?> coll, final String delim) {
        return collectionToDelimitedString(coll, delim, "", "");
    }
    
    public static String collectionToCommaDelimitedString(final Collection<?> coll) {
        return collectionToDelimitedString(coll, ",");
    }
    
    public static String arrayToDelimitedString(final Object[] arr, final String delim) {
        if (ObjectUtils.isEmpty(arr)) {
            return "";
        }
        if (arr.length == 1) {
            return ObjectUtils.nullSafeToString(arr[0]);
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; ++i) {
            if (i > 0) {
                sb.append(delim);
            }
            sb.append(arr[i]);
        }
        return sb.toString();
    }
    
    public static String arrayToCommaDelimitedString(final Object[] arr) {
        return arrayToDelimitedString(arr, ",");
    }
}
