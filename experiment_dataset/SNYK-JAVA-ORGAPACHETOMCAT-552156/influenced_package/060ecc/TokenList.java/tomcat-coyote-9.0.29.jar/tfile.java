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
    
    public static void parseTokenList(final Enumeration<String> inputs, final Collection<String> result) throws IOException {
        while (inputs.hasMoreElements()) {
            final String nextHeaderValue = inputs.nextElement();
            if (nextHeaderValue != null) {
                parseTokenList(new StringReader(nextHeaderValue), result);
            }
        }
    }
    
    public static void parseTokenList(final Reader input, final Collection<String> result) throws IOException {
        while (true) {
            final String fieldName = HttpParser.readToken(input);
            if (fieldName == null) {
                HttpParser.skipUntil(input, 0, ',');
            }
            else {
                if (fieldName.length() == 0) {
                    break;
                }
                final SkipResult skipResult = HttpParser.skipConstant(input, ",");
                if (skipResult == SkipResult.EOF) {
                    result.add(fieldName.toLowerCase(Locale.ENGLISH));
                    break;
                }
                if (skipResult == SkipResult.FOUND) {
                    result.add(fieldName.toLowerCase(Locale.ENGLISH));
                }
                else {
                    HttpParser.skipUntil(input, 0, ',');
                }
            }
        }
    }
}
