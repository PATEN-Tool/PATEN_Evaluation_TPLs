// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.solr.schema;

import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.w3c.dom.Node;
import javax.xml.xpath.XPath;
import org.w3c.dom.Document;
import org.apache.solr.core.SolrResourceLoader;
import java.util.Collections;
import javax.xml.xpath.XPathExpressionException;
import org.xml.sax.SAXException;
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.NodeList;
import java.util.Locale;
import javax.xml.xpath.XPathFactory;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.util.SafeXMLParsing;
import org.apache.solr.common.SolrException;
import java.util.HashMap;
import org.slf4j.Logger;
import org.apache.commons.lang3.math.NumberUtils;
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

public abstract class AbstractEnumField extends PrimitiveFieldType
{
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
        if (field.multiValued()) {
            final MultiValueSelector selector = field.type.getDefaultMultiValueSelectorForSort(field, top);
            if (null != selector) {
                final SortField result = FieldType.getSortedSetSortField(field, selector.getSortedSetSelectorType(), top, SortField.STRING_FIRST, SortField.STRING_LAST);
                if (null == result.getMissingValue()) {
                    result.setMissingValue(SortField.STRING_FIRST);
                }
                return result;
            }
        }
        final SortField result2 = FieldType.getSortField(field, SortField.Type.INT, top, Integer.MIN_VALUE, Integer.MAX_VALUE);
        if (null == result2.getMissingValue()) {
            result2.setMissingValue((Object)Integer.MIN_VALUE);
        }
        return result2;
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
    
    @Override
    public Object toNativeType(final Object val) {
        if (val instanceof CharSequence) {
            final String str = val.toString();
            final Integer entry = this.enumMapping.enumStringToIntMap.get(str);
            if (entry != null) {
                return new EnumFieldValue(entry, str);
            }
            if (NumberUtils.isCreatable(str)) {
                final int num = Integer.parseInt(str);
                return new EnumFieldValue(Integer.valueOf(num), (String)this.enumMapping.enumIntToStringMap.get(num));
            }
        }
        else if (val instanceof Number) {
            final int num2 = ((Number)val).intValue();
            return new EnumFieldValue(Integer.valueOf(num2), (String)this.enumMapping.enumIntToStringMap.get(num2));
        }
        return super.toNativeType(val);
    }
    
    public static final class EnumMapping
    {
        private static final Logger log;
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
            final SolrResourceLoader loader = schema.getResourceLoader();
            try {
                EnumMapping.log.debug("Reloading enums config file from {}", (Object)this.enumsConfigFile);
                final Document doc = SafeXMLParsing.parseConfigXML(EnumMapping.log, (ResourceLoader)loader, this.enumsConfigFile);
                final XPathFactory xpathFactory = XPathFactory.newInstance();
                final XPath xpath = xpathFactory.newXPath();
                final String xpathStr = String.format(Locale.ROOT, "/enumsConfig/enum[@name='%s']", this.enumName);
                final NodeList nodes = (NodeList)xpath.evaluate(xpathStr, doc, XPathConstants.NODESET);
                final int nodesLength = nodes.getLength();
                if (nodesLength == 0) {
                    final String exceptionMessage = String.format(Locale.ENGLISH, "%s: No enum configuration found for enum '%s' in %s.", ftName, this.enumName, this.enumsConfigFile);
                    throw new SolrException(SolrException.ErrorCode.NOT_FOUND, exceptionMessage);
                }
                if (nodesLength > 1) {
                    EnumMapping.log.warn("{}: More than one enum configuration found for enum '{}' in {}. The last one was taken.", new Object[] { ftName, this.enumName, this.enumsConfigFile });
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
            catch (IOException | SAXException | XPathExpressionException ex2) {
                final Exception ex;
                final Exception e = ex;
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, ftName + ": Error while parsing enums config.", (Throwable)e);
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
            log = LoggerFactory.getLogger((Class)MethodHandles.lookup().lookupClass());
            DEFAULT_VALUE = -1;
        }
    }
}
