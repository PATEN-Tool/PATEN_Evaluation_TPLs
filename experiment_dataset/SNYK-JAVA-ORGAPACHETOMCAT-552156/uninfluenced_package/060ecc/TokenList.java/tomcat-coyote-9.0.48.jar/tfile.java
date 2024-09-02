// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.tomcat.util.http.parser;

import java.util.Locale;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Enumeration;

public class TokenList
{
    private TokenList() {
    }
    
    public static boolean parseTokenList(final Enumeration<String> inputs, final Collection<String> collection) throws IOException {
        boolean result = true;
        while (inputs.hasMoreElements()) {
            final String nextHeaderValue = inputs.nextElement();
            if (nextHeaderValue != null && !parseTokenList(new StringReader(nextHeaderValue), collection)) {
                result = false;
            }
        }
        return result;
    }
    
    public static boolean parseTokenList(final Reader input, final Collection<String> collection) throws IOException {
        boolean invalid = false;
        boolean valid = false;
        while (true) {
            final String fieldName = HttpParser.readToken(input);
            if (fieldName == null) {
                invalid = true;
                HttpParser.skipUntil(input, 0, ',');
            }
            else {
                if (fieldName.length() == 0) {
                    invalid = true;
                    break;
                }
                final SkipResult skipResult = HttpParser.skipConstant(input, ",");
                if (skipResult == SkipResult.EOF) {
                    valid = true;
                    collection.add(fieldName.toLowerCase(Locale.ENGLISH));
                    break;
                }
                if (skipResult == SkipResult.FOUND) {
                    valid = true;
                    collection.add(fieldName.toLowerCase(Locale.ENGLISH));
                }
                else {
                    invalid = true;
                    HttpParser.skipUntil(input, 0, ',');
                }
            }
        }
        return valid && !invalid;
    }
}
