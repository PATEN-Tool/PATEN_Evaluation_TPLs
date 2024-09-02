// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.uima.pear.util;

import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Enumeration;
import java.io.PrintWriter;
import java.util.Properties;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import java.io.CharConversionException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

public class XMLUtil
{
    public static final String XML_HEADER_BEG = "<?xml version=\"1.0\"";
    public static final String XML_ENCODING_TAG = "encoding";
    public static final String XML_HEADER_END = "?>";
    public static final String CDATA_SECTION_BEG = "<![CDATA[";
    public static final String CDATA_SECTION_END = "]]>";
    public static final String NAMESPACES_FEATURE_ID = "http://xml.org/sax/features/namespaces";
    public static final String NAMESPACE_PREFIXES_FEATURE_ID = "http://xml.org/sax/features/namespace-prefixes";
    public static final String VALIDATION_FEATURE_ID = "http://xml.org/sax/features/validation";
    public static final String SCHEMA_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/schema";
    public static final String SCHEMA_FULL_CHECKING_FEATURE_ID = "http://apache.org/xml/features/validation/schema-full-checking";
    public static final String DYNAMIC_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/dynamic";
    protected static final boolean DEFAULT_NAMESPACES = true;
    protected static final boolean DEFAULT_NAMESPACE_PREFIXES = false;
    protected static final boolean DEFAULT_VALIDATION = false;
    protected static final boolean DEFAULT_SCHEMA_VALIDATION = false;
    protected static final boolean DEFAULT_SCHEMA_FULL_CHECKING = false;
    protected static final boolean DEFAULT_DYNAMIC_VALIDATION = false;
    protected static final String FIRST_XML_CHARS = "<?";
    
    public static SAXParser createSAXParser() throws SAXException {
        SAXParser parser = null;
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature("http://xml.org/sax/features/namespaces", true);
            factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            parser = factory.newSAXParser();
        }
        catch (ParserConfigurationException exc) {
            throw new SAXException(exc);
        }
        return parser;
    }
    
    public static String detectXmlFileEncoding(final File xmlFile) throws IOException {
        String encoding = null;
        FileInputStream iStream = null;
        BufferedReader fReader = null;
        try {
            if (!isValidXmlFile(xmlFile)) {
                return null;
            }
            iStream = new FileInputStream(xmlFile);
            int byteCounter = 0;
            int nextByte = 0;
            final int[] prefix = new int[16];
            do {
                nextByte = iStream.read();
                if (byteCounter < 16) {
                    prefix[byteCounter] = nextByte;
                }
                ++byteCounter;
                if (nextByte < 0) {
                    throw new IOException("cannot read file");
                }
            } while (nextByte == 239 || nextByte == 187 || nextByte == 191 || nextByte == 254 || nextByte == 255 || nextByte == 0);
            final int prefixLength = (byteCounter < 17) ? (byteCounter - 1) : 16;
            final String utfSignature = (prefixLength > 0) ? FileUtil.identifyUtfSignature(prefix, prefixLength) : null;
            boolean utf8Signature = false;
            boolean utf16Signature = false;
            boolean utf32Signature = false;
            if (utfSignature != null) {
                if (utfSignature.startsWith("UTF-8")) {
                    utf8Signature = true;
                }
                else if (utfSignature.startsWith("UTF-16")) {
                    utf16Signature = true;
                }
                else if (utfSignature.startsWith("UTF-32")) {
                    utf32Signature = true;
                }
            }
            byte[] buffer = null;
            int bytes2put = 0;
            if (utf16Signature) {
                bytes2put = 14;
                buffer = new byte[prefixLength + bytes2put];
                for (int i = 0; i < prefixLength; ++i) {
                    buffer[i] = (byte)prefix[i];
                }
                byteCounter = prefixLength;
            }
            else if (utf32Signature) {
                bytes2put = 28;
                buffer = new byte[prefixLength + bytes2put];
                for (int i = 0; i < prefixLength; ++i) {
                    buffer[i] = (byte)prefix[i];
                }
                byteCounter = prefixLength;
            }
            else {
                bytes2put = 7;
                buffer = new byte[bytes2put];
                byteCounter = 0;
            }
            buffer[byteCounter++] = (byte)nextByte;
            int offset;
            int bytesRead;
            for (offset = 0; offset < bytes2put - 1; offset += bytesRead) {
                bytesRead = iStream.read(buffer, offset + byteCounter, bytes2put - 1 - offset);
                if (bytesRead == -1) {
                    break;
                }
            }
            if (offset != bytes2put - 1) {
                throw new IOException("cannot read file");
            }
            final byte[] buffer2 = new byte[6];
            System.arraycopy(buffer, 0, buffer2, 0, 6);
            if (utf8Signature) {
                final String test = new String(buffer, "UTF-8");
                if (test.startsWith("<?")) {
                    encoding = "UTF-8";
                }
            }
            else if (utf16Signature) {
                final String test = new String(buffer2, "UTF-16");
                if (test.startsWith("<?")) {
                    encoding = "UTF-16";
                }
            }
            else if (!utf32Signature) {
                String test = new String(buffer, "UTF-8");
                if (test.startsWith("<?")) {
                    encoding = "UTF-8";
                }
                else {
                    test = new String(buffer2, "UTF-16LE");
                    if (test.startsWith("<?")) {
                        encoding = "UTF-16LE";
                    }
                    else {
                        test = new String(buffer2, "UTF-16BE");
                        if (test.startsWith("<?")) {
                            encoding = "UTF-16BE";
                        }
                    }
                }
            }
            iStream.close();
            if (encoding == null) {
                fReader = new BufferedReader(new InputStreamReader(new FileInputStream(xmlFile), "UTF-8"));
                String line = null;
                try {
                    while ((line = fReader.readLine()) != null) {
                        final String xmlLine = line.trim();
                        if (xmlLine.length() > 0) {
                            if (xmlLine.charAt(0) == '<') {
                                encoding = "UTF-8";
                                break;
                            }
                            break;
                        }
                    }
                }
                catch (CharConversionException ex) {}
                fReader.close();
                if (encoding == null) {
                    fReader = new BufferedReader(new InputStreamReader(new FileInputStream(xmlFile), "UTF-16"));
                    try {
                        while ((line = fReader.readLine()) != null) {
                            final String xmlLine = line.trim();
                            if (xmlLine.length() > 0) {
                                if (xmlLine.charAt(0) == '<') {
                                    encoding = "UTF-16";
                                    break;
                                }
                                break;
                            }
                        }
                    }
                    catch (CharConversionException ex2) {}
                    fReader.close();
                }
            }
        }
        catch (IOException exc) {
            throw exc;
        }
        catch (Throwable err) {
            throw new IOException(err.toString());
        }
        finally {
            if (iStream != null) {
                try {
                    iStream.close();
                }
                catch (Exception ex3) {}
            }
            if (fReader != null) {
                try {
                    fReader.close();
                }
                catch (Exception ex4) {}
            }
        }
        return encoding;
    }
    
    public static boolean isValidXmlFile(final File xmlFile) throws IOException {
        boolean isValid = true;
        InputStream iStream = null;
        try {
            iStream = new FileInputStream(xmlFile);
            final SAXParser parser = createSAXParser();
            parser.parse(iStream, new DefaultHandler());
        }
        catch (IOException exc) {
            throw exc;
        }
        catch (SAXException err) {
            isValid = false;
        }
        finally {
            if (iStream != null) {
                try {
                    iStream.close();
                }
                catch (Exception ex) {}
            }
        }
        return isValid;
    }
    
    public static void printError(final String type, final SAXParseException ex) {
        System.err.print("[");
        System.err.print(type);
        System.err.print("] ");
        if (ex == null) {
            System.err.print("SAX Parse Exception was null! Therefore, no further details are available.");
        }
        else {
            String systemId = ex.getSystemId();
            if (systemId != null) {
                final int index = systemId.lastIndexOf(47);
                if (index != -1) {
                    systemId = systemId.substring(index + 1);
                }
                System.err.print(systemId);
            }
            System.err.print(':');
            System.err.print(ex.getLineNumber());
            System.err.print(':');
            System.err.print(ex.getColumnNumber());
            System.err.print(": ");
            System.err.print(ex.getMessage());
        }
        System.err.println();
        System.err.flush();
    }
    
    public static void printAllXMLElements(final Properties elements, final PrintWriter oWriter, final int level) throws IOException {
        printAllXMLElements(elements, null, oWriter, level);
    }
    
    public static void printAllXMLElements(final Properties elements, final String valueDelimiter, final String[] tagOrder, final PrintWriter oWriter, final int level) throws IOException {
        final boolean multiValue = valueDelimiter != null && valueDelimiter.length() > 0;
        if (elements != null) {
            if (tagOrder != null) {
                for (int i = 0; i < tagOrder.length; ++i) {
                    final String tag = tagOrder[i];
                    final String eValue = elements.getProperty(tag);
                    if (eValue != null) {
                        if (multiValue) {
                            printXMLElements(tag, eValue, valueDelimiter, oWriter, level);
                        }
                        else {
                            printXMLElement(tag, eValue, oWriter, level);
                            oWriter.println();
                        }
                    }
                }
            }
            final Enumeration<Object> keys = ((Hashtable<Object, V>)elements).keys();
            while (keys.hasMoreElements()) {
                final String tag = keys.nextElement();
                boolean done = false;
                if (tagOrder != null) {
                    for (int j = 0; j < tagOrder.length; ++j) {
                        if (tag.equals(tagOrder[j])) {
                            done = true;
                            break;
                        }
                    }
                }
                if (!done) {
                    final String eValue2 = elements.getProperty(tag);
                    if (multiValue) {
                        printXMLElements(tag, eValue2, valueDelimiter, oWriter, level);
                    }
                    else {
                        printXMLElement(tag, eValue2, oWriter, level);
                        oWriter.println();
                    }
                }
            }
        }
    }
    
    public static void printAllXMLElements(final Properties elements, final String[] tagOrder, final PrintWriter oWriter, final int level) throws IOException {
        printAllXMLElements(elements, null, tagOrder, oWriter, level);
    }
    
    public static void printXMLElement(final String tag, final Properties attributes, final PrintWriter oWriter, final int level) throws IOException {
        printXMLElement(tag, attributes, null, oWriter, level);
    }
    
    public static void printXMLElement(final String tag, final Properties attributes, final String elemValue, final PrintWriter oWriter, final int level) throws IOException {
        printXMLElement(tag, attributes, elemValue, false, oWriter, level, false);
    }
    
    public static void printXMLElement(final String tag, final Properties attributes, final String elemValue, final boolean putInCdataSection, final PrintWriter oWriter, final int level) throws IOException {
        printXMLElement(tag, attributes, elemValue, putInCdataSection, oWriter, level, false);
    }
    
    public static void printXMLElement(final String tag, final Properties attributes, final String elemValue, final boolean putInCdataSection, final PrintWriter oWriter, final int level, final boolean useNewLine4Value) throws IOException {
        printXMLTag(tag, attributes, oWriter, false, level);
        if (useNewLine4Value) {
            oWriter.println();
            printXMLElementValue(elemValue, putInCdataSection, oWriter, level);
            oWriter.println();
            printXMLTag(tag, oWriter, true, level);
        }
        else {
            printXMLElementValue(elemValue, putInCdataSection, oWriter, 0);
            printXMLTag(tag, oWriter, true, 0);
        }
    }
    
    public static void printXMLElement(final String tag, final String elemValue, final PrintWriter oWriter, final int level) throws IOException {
        printXMLElement(tag, null, elemValue, oWriter, level);
    }
    
    public static void printXMLElement(final String tag, final String elemValue, final boolean putInCdataSection, final PrintWriter oWriter, final int level) throws IOException {
        printXMLElement(tag, null, elemValue, putInCdataSection, oWriter, level);
    }
    
    public static void printXMLElements(final String tag, final String elemValue, final String valueDelimiter, final PrintWriter oWriter, final int level) throws IOException {
        if (elemValue != null) {
            final StringTokenizer elemTokens = new StringTokenizer(elemValue, valueDelimiter);
            while (elemTokens.hasMoreTokens()) {
                final String elemToken = elemTokens.nextToken();
                printXMLElement(tag, elemToken, oWriter, level);
                oWriter.println();
            }
        }
    }
    
    public static void printXMLElementValue(final String elemValue, final PrintWriter oWriter, final int level) throws IOException {
        printXMLElementValue(elemValue, false, oWriter, level);
    }
    
    public static void printXMLElementValue(final String elemValue, final boolean putInCdataSection, final PrintWriter oWriter, final int level) throws IOException {
        for (int l = 0; l < level; ++l) {
            oWriter.print('\t');
        }
        if (elemValue != null) {
            if (putInCdataSection) {
                oWriter.print("<![CDATA[");
            }
            oWriter.print(elemValue.trim());
            if (putInCdataSection) {
                oWriter.print("]]>");
            }
            oWriter.flush();
        }
    }
    
    public static void printXMLHeader(final String encoding, final PrintWriter oWriter) throws IOException {
        oWriter.print("<?xml version=\"1.0\"");
        if (encoding != null && encoding.length() > 0) {
            oWriter.print(" encoding=\"" + encoding + "\"");
        }
        oWriter.println("?>");
    }
    
    public static void printXMLTag(final String tag, final PrintWriter oWriter, final boolean tagEnd, final int level) throws IOException {
        printXMLTag(tag, null, oWriter, tagEnd, level);
    }
    
    public static void printXMLTag(final String tag, final Properties attributes, final PrintWriter oWriter, final boolean tagEnd, final int level) throws IOException {
        for (int l = 0; l < level; ++l) {
            oWriter.print('\t');
        }
        if (tagEnd) {
            oWriter.print("</");
        }
        else {
            oWriter.print('<');
        }
        oWriter.print(tag);
        if (!tagEnd && attributes != null) {
            final Enumeration<Object> attrNames = ((Hashtable<Object, V>)attributes).keys();
            while (attrNames.hasMoreElements()) {
                final String name = attrNames.nextElement();
                final String value = attributes.getProperty(name);
                oWriter.print(' ');
                oWriter.print(name);
                oWriter.print("=\"");
                oWriter.print(value);
                oWriter.print('\"');
            }
        }
        oWriter.print('>');
        oWriter.flush();
    }
}
