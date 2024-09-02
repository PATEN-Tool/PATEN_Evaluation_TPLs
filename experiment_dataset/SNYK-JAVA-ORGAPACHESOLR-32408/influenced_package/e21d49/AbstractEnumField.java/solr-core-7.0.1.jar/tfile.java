// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.solr.schema;

import org.w3c.dom.Node;
import javax.xml.xpath.XPath;
import org.w3c.dom.Document;
import java.io.InputStream;
import java.util.Collections;
import org.xml.sax.SAXException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.NodeList;
import java.util.Locale;
import javax.xml.xpath.XPathFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.solr.common.SolrException;
import java.util.HashMap;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.apache.lucene.util.BytesRefBuilder;
import java.io.IOException;
import org.apache.solr.response.TextResponseWriter;
import org.apache.lucene.queries.function.valuesource.EnumFieldSource;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.solr.search.QParser;
import org.apache.lucene.search.SortField;
import org.apache.solr.common.EnumFieldValue;
import org.apache.lucene.index.IndexableField;
import java.util.Map;
import org.slf4j.Logger;

public abstract class AbstractEnumField extends PrimitiveFieldType
{
    private static final Logger log;
    protected EnumMapping enumMapping;
    
    @Override
    protected void init(final IndexSchema schema, final Map<String, String> args) {
        super.init(schema, args);
        this.enumMapping = new EnumMapping(schema, this, args);
    }
    
    public EnumMapping getEnumMapping() {
        return this.enumMapping;
    }
    
    @Override
    public EnumFieldValue toObject(final IndexableField f) {
        Integer intValue = null;
        String stringValue = null;
        final Number val = f.numericValue();
        if (val != null) {
            intValue = val.intValue();
            stringValue = this.enumMapping.intValueToStringValue(intValue);
        }
        return new EnumFieldValue(intValue, stringValue);
    }
    
    @Override
    public SortField getSortField(final SchemaField field, final boolean top) {
        field.checkSortability();
        final Object missingValue = Integer.MIN_VALUE;
        final SortField sf = new SortField(field.getName(), SortField.Type.INT, top);
        sf.setMissingValue(missingValue);
        return sf;
    }
    
    @Override
    public ValueSource getValueSource(final SchemaField field, final QParser qparser) {
        field.checkFieldCacheSource();
        return (ValueSource)new EnumFieldSource(field.getName(), (Map)this.enumMapping.enumIntToStringMap, (Map)this.enumMapping.enumStringToIntMap);
    }
    
    @Override
    public void write(final TextResponseWriter writer, final String name, final IndexableField f) throws IOException {
        final Number val = f.numericValue();
        if (val == null) {
            writer.writeNull(name);
            return;
        }
        final String readableValue = this.enumMapping.intValueToStringValue(val.intValue());
        writer.writeStr(name, readableValue, true);
    }
    
    @Override
    public boolean isTokenized() {
        return false;
    }
    
    @Override
    public NumberType getNumberType() {
        return NumberType.INTEGER;
    }
    
    @Override
    public String readableToIndexed(final String val) {
        if (val == null) {
            return null;
        }
        final BytesRefBuilder bytes = new BytesRefBuilder();
        this.readableToIndexed(val, bytes);
        return bytes.get().utf8ToString();
    }
    
    @Override
    public String toInternal(final String val) {
        return this.readableToIndexed(val);
    }
    
    @Override
    public String toExternal(final IndexableField f) {
        final Number val = f.numericValue();
        if (val == null) {
            return null;
        }
        return this.enumMapping.intValueToStringValue(val.intValue());
    }
    
    static {
        log = LoggerFactory.getLogger((Class)MethodHandles.lookup().lookupClass());
    }
    
    public static final class EnumMapping
    {
        public static final String PARAM_ENUMS_CONFIG = "enumsConfig";
        public static final String PARAM_ENUM_NAME = "enumName";
        public static final Integer DEFAULT_VALUE;
        public final Map<String, Integer> enumStringToIntMap;
        public final Map<Integer, String> enumIntToStringMap;
        protected final String enumsConfigFile;
        protected final String enumName;
        
        public EnumMapping(final IndexSchema schema, final FieldType fieldType, final Map<String, String> args) {
            final String ftName = fieldType.getTypeName();
            final Map<String, Integer> enumStringToIntMap = new HashMap<String, Integer>();
            final Map<Integer, String> enumIntToStringMap = new HashMap<Integer, String>();
            this.enumsConfigFile = args.get("enumsConfig");
            if (this.enumsConfigFile == null) {
                throw new SolrException(SolrException.ErrorCode.NOT_FOUND, ftName + ": No enums config file was configured.");
            }
            this.enumName = args.get("enumName");
            if (this.enumName == null) {
                throw new SolrException(SolrException.ErrorCode.NOT_FOUND, ftName + ": No enum name was configured.");
            }
            InputStream is = null;
            try {
                is = schema.getResourceLoader().openResource(this.enumsConfigFile);
                final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                try {
                    final Document doc = dbf.newDocumentBuilder().parse(is);
                    final XPathFactory xpathFactory = XPathFactory.newInstance();
                    final XPath xpath = xpathFactory.newXPath();
                    final String xpathStr = String.format(Locale.ROOT, "/enumsConfig/enum[@name='%s']", this.enumName);
                    final NodeList nodes = (NodeList)xpath.evaluate(xpathStr, doc, XPathConstants.NODESET);
                    final int nodesLength = nodes.getLength();
                    if (nodesLength == 0) {
                        final String exceptionMessage = String.format(Locale.ENGLISH, "%s: No enum configuration found for enum '%s' in %s.", ftName, this.enumName, this.enumsConfigFile);
                        throw new SolrException(SolrException.ErrorCode.NOT_FOUND, exceptionMessage);
                    }
                    if (nodesLength > 1 && AbstractEnumField.log.isWarnEnabled()) {
                        AbstractEnumField.log.warn("{}: More than one enum configuration found for enum '{}' in {}. The last one was taken.", new Object[] { ftName, this.enumName, this.enumsConfigFile });
                    }
                    final Node enumNode = nodes.item(nodesLength - 1);
                    final NodeList valueNodes = (NodeList)xpath.evaluate("value", enumNode, XPathConstants.NODESET);
                    for (int i = 0; i < valueNodes.getLength(); ++i) {
                        final Node valueNode = valueNodes.item(i);
                        final String valueStr = valueNode.getTextContent();
                        if (valueStr == null || valueStr.length() == 0) {
                            final String exceptionMessage2 = String.format(Locale.ENGLISH, "%s: A value was defined with an no value in enum '%s' in %s.", ftName, this.enumName, this.enumsConfigFile);
                            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, exceptionMessage2);
                        }
                        if (enumStringToIntMap.containsKey(valueStr)) {
                            final String exceptionMessage2 = String.format(Locale.ENGLISH, "%s: A duplicated definition was found for value '%s' in enum '%s' in %s.", ftName, valueStr, this.enumName, this.enumsConfigFile);
                            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, exceptionMessage2);
                        }
                        enumIntToStringMap.put(i, valueStr);
                        enumStringToIntMap.put(valueStr, i);
                    }
                }
                catch (ParserConfigurationException | XPathExpressionException | SAXException ex2) {
                    final Exception ex;
                    final Exception e = ex;
                    throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, ftName + ": Error parsing enums config.", (Throwable)e);
                }
            }
            catch (IOException e2) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, ftName + ": Error while opening enums config.", (Throwable)e2);
            }
            finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                }
                catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
            if (enumStringToIntMap.size() == 0 || enumIntToStringMap.size() == 0) {
                final String exceptionMessage3 = String.format(Locale.ENGLISH, "%s: Invalid configuration was defined for enum '%s' in %s.", ftName, this.enumName, this.enumsConfigFile);
                throw new SolrException(SolrException.ErrorCode.NOT_FOUND, exceptionMessage3);
            }
            this.enumStringToIntMap = Collections.unmodifiableMap((Map<? extends String, ? extends Integer>)enumStringToIntMap);
            this.enumIntToStringMap = Collections.unmodifiableMap((Map<? extends Integer, ? extends String>)enumIntToStringMap);
            args.remove("enumsConfig");
            args.remove("enumName");
        }
        
        public String intValueToStringValue(final Integer intVal) {
            if (intVal == null) {
                return null;
            }
            final String enumString = this.enumIntToStringMap.get(intVal);
            if (enumString != null) {
                return enumString;
            }
            return EnumMapping.DEFAULT_VALUE.toString();
        }
        
        public Integer stringValueToIntValue(final String stringVal) {
            if (stringVal == null) {
                return null;
            }
            final Integer enumInt = this.enumStringToIntMap.get(stringVal);
            if (enumInt != null) {
                return enumInt;
            }
            Integer intValue = tryParseInt(stringVal);
            if (intValue == null) {
                intValue = EnumMapping.DEFAULT_VALUE;
            }
            final String enumString = this.enumIntToStringMap.get(intValue);
            if (enumString != null) {
                return intValue;
            }
            return EnumMapping.DEFAULT_VALUE;
        }
        
        private static Integer tryParseInt(final String valueStr) {
            Integer intValue = null;
            try {
                intValue = Integer.parseInt(valueStr);
            }
            catch (NumberFormatException ex) {}
            return intValue;
        }
        
        static {
            DEFAULT_VALUE = -1;
        }
    }
}
