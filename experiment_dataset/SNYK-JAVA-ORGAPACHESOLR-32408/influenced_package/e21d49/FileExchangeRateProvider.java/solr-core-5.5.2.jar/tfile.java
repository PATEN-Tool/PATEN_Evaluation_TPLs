// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.solr.schema;

import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import javax.xml.xpath.XPath;
import org.w3c.dom.Document;
import java.io.InputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.xml.sax.SAXException;
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.NodeList;
import javax.xml.xpath.XPathFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import org.apache.solr.common.SolrException;
import java.util.HashMap;
import org.apache.lucene.analysis.util.ResourceLoader;
import java.util.Map;
import org.slf4j.Logger;

class FileExchangeRateProvider implements ExchangeRateProvider
{
    private static final Logger log;
    protected static final String PARAM_CURRENCY_CONFIG = "currencyConfig";
    private Map<String, Map<String, Double>> rates;
    private String currencyConfigFile;
    private ResourceLoader loader;
    
    FileExchangeRateProvider() {
        this.rates = new HashMap<String, Map<String, Double>>();
    }
    
    @Override
    public double getExchangeRate(final String sourceCurrencyCode, final String targetCurrencyCode) {
        if (sourceCurrencyCode == null || targetCurrencyCode == null) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Cannot get exchange rate; currency was null.");
        }
        if (sourceCurrencyCode.equals(targetCurrencyCode)) {
            return 1.0;
        }
        final Double directRate = this.lookupRate(sourceCurrencyCode, targetCurrencyCode);
        if (directRate != null) {
            return directRate;
        }
        final Double symmetricRate = this.lookupRate(targetCurrencyCode, sourceCurrencyCode);
        if (symmetricRate != null) {
            return 1.0 / symmetricRate;
        }
        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "No available conversion rate between " + sourceCurrencyCode + " to " + targetCurrencyCode);
    }
    
    private Double lookupRate(final String sourceCurrencyCode, final String targetCurrencyCode) {
        final Map<String, Double> rhs = this.rates.get(sourceCurrencyCode);
        if (rhs != null) {
            return rhs.get(targetCurrencyCode);
        }
        return null;
    }
    
    private void addRate(final Map<String, Map<String, Double>> ratesMap, final String sourceCurrencyCode, final String targetCurrencyCode, final double rate) {
        Map<String, Double> rhs = ratesMap.get(sourceCurrencyCode);
        if (rhs == null) {
            rhs = new HashMap<String, Double>();
            ratesMap.put(sourceCurrencyCode, rhs);
        }
        rhs.put(targetCurrencyCode, rate);
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final FileExchangeRateProvider that = (FileExchangeRateProvider)o;
        if (this.rates != null) {
            if (!this.rates.equals(that.rates)) {
                return false;
            }
        }
        else if (that.rates != null) {
            return false;
        }
        return true;
        b = false;
        return b;
    }
    
    @Override
    public int hashCode() {
        return (this.rates != null) ? this.rates.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "[" + this.getClass().getName() + " : " + this.rates.size() + " rates.]";
    }
    
    @Override
    public Set<String> listAvailableCurrencies() {
        final Set<String> currencies = new HashSet<String>();
        for (final String from : this.rates.keySet()) {
            currencies.add(from);
            for (final String to : this.rates.get(from).keySet()) {
                currencies.add(to);
            }
        }
        return currencies;
    }
    
    @Override
    public boolean reload() throws SolrException {
        InputStream is = null;
        final Map<String, Map<String, Double>> tmpRates = new HashMap<String, Map<String, Double>>();
        try {
            FileExchangeRateProvider.log.info("Reloading exchange rates from file " + this.currencyConfigFile);
            is = this.loader.openResource(this.currencyConfigFile);
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            try {
                dbf.setXIncludeAware(true);
                dbf.setNamespaceAware(true);
            }
            catch (UnsupportedOperationException e) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "XML parser doesn't support XInclude option", (Throwable)e);
            }
            try {
                final Document doc = dbf.newDocumentBuilder().parse(is);
                final XPathFactory xpathFactory = XPathFactory.newInstance();
                final XPath xpath = xpathFactory.newXPath();
                final NodeList nodes = (NodeList)xpath.evaluate("/currencyConfig/rates/rate", doc, XPathConstants.NODESET);
                for (int i = 0; i < nodes.getLength(); ++i) {
                    final Node rateNode = nodes.item(i);
                    final NamedNodeMap attributes = rateNode.getAttributes();
                    final Node from = attributes.getNamedItem("from");
                    final Node to = attributes.getNamedItem("to");
                    final Node rate = attributes.getNamedItem("rate");
                    if (from == null || to == null || rate == null) {
                        throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Exchange rate missing attributes (required: from, to, rate) " + rateNode);
                    }
                    final String fromCurrency = from.getNodeValue();
                    final String toCurrency = to.getNodeValue();
                    if (null == CurrencyField.getCurrency(fromCurrency)) {
                        throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Specified 'from' currency not supported in this JVM: " + fromCurrency);
                    }
                    if (null == CurrencyField.getCurrency(toCurrency)) {
                        throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Specified 'to' currency not supported in this JVM: " + toCurrency);
                    }
                    Double exchangeRate;
                    try {
                        exchangeRate = Double.parseDouble(rate.getNodeValue());
                    }
                    catch (NumberFormatException e2) {
                        throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Could not parse exchange rate: " + rateNode, (Throwable)e2);
                    }
                    this.addRate(tmpRates, fromCurrency, toCurrency, exchangeRate);
                }
            }
            catch (SAXException | XPathExpressionException | ParserConfigurationException | IOException ex2) {
                final Exception ex;
                final Exception e3 = ex;
                throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Error parsing currency config.", (Throwable)e3);
            }
        }
        catch (IOException e4) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Error while opening Currency configuration file " + this.currencyConfigFile, (Throwable)e4);
        }
        finally {
            try {
                if (is != null) {
                    is.close();
                }
            }
            catch (IOException e5) {
                e5.printStackTrace();
            }
        }
        this.rates = tmpRates;
        return true;
    }
    
    @Override
    public void init(final Map<String, String> params) throws SolrException {
        this.currencyConfigFile = params.get("currencyConfig");
        if (this.currencyConfigFile == null) {
            throw new SolrException(SolrException.ErrorCode.NOT_FOUND, "Missing required configuration currencyConfig");
        }
        params.remove("currencyConfig");
    }
    
    @Override
    public void inform(final ResourceLoader loader) throws SolrException {
        if (loader == null) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Needs ResourceLoader in order to load config file");
        }
        this.loader = loader;
        this.reload();
    }
    
    static {
        log = LoggerFactory.getLogger((Class)MethodHandles.lookup().lookupClass());
    }
}
