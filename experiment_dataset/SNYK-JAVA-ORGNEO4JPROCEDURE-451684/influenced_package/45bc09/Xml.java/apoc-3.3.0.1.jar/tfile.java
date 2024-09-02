// 
// Decompiled by Procyon v0.5.36
// 

package apoc.load;

import java.util.Collection;
import java.util.Arrays;
import org.w3c.dom.CharacterData;
import org.w3c.dom.NamedNodeMap;
import java.util.Iterator;
import org.w3c.dom.Node;
import java.util.LinkedHashMap;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import apoc.util.Util;
import java.util.Deque;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPath;
import org.w3c.dom.Document;
import java.net.URLConnection;
import javax.xml.parsers.DocumentBuilder;
import java.util.List;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.NodeList;
import org.apache.commons.lang3.StringUtils;
import javax.xml.xpath.XPathFactory;
import java.net.URL;
import apoc.export.util.FileUtils;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.Collections;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Procedure;
import apoc.result.MapResult;
import java.util.stream.Stream;
import java.util.Map;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Context;
import org.neo4j.graphdb.GraphDatabaseService;
import javax.xml.stream.XMLInputFactory;

public class Xml
{
    public static final XMLInputFactory FACTORY;
    @Context
    public GraphDatabaseService db;
    
    @Procedure
    @Description("apoc.load.xml('http://example.com/test.xml', 'xPath',config, false) YIELD value as doc CREATE (p:Person) SET p.name = doc.name load from XML URL (e.g. web-api) to import XML as single nested map with attributes and _type, _text and _childrenx fields.")
    public Stream<MapResult> xml(@Name("url") final String url, @Name(value = "path", defaultValue = "/") final String path, @Name(value = "config", defaultValue = "{}") final Map<String, Object> config, @Name(value = "simple", defaultValue = "false") final boolean simpleMode) throws Exception {
        return this.xmlXpathToMapResult(url, simpleMode, path, config);
    }
    
    @Procedure(deprecatedBy = "apoc.load.xml")
    @Deprecated
    @Description("apoc.load.xmlSimple('http://example.com/test.xml') YIELD value as doc CREATE (p:Person) SET p.name = doc.name load from XML URL (e.g. web-api) to import XML as single nested map with attributes and _type, _text and _children fields. This method does intentionally not work with XML mixed content.")
    public Stream<MapResult> xmlSimple(@Name("url") final String url) throws Exception {
        return this.xmlToMapResult(url, true);
    }
    
    private Stream<MapResult> xmlXpathToMapResult(@Name("url") final String url, final boolean simpleMode, String path, Map<String, Object> config) throws Exception {
        if (config == null) {
            config = Collections.emptyMap();
        }
        final boolean failOnError = config.getOrDefault("failOnError", true);
        final List<MapResult> result = new ArrayList<MapResult>();
        try {
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilderFactory.setIgnoringElementContentWhitespace(true);
            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            FileUtils.checkReadAllowed(url);
            final URLConnection urlConnection = new URL(url).openConnection();
            final Document doc = documentBuilder.parse(urlConnection.getInputStream());
            final XPathFactory xPathFactory = XPathFactory.newInstance();
            final XPath xPath = xPathFactory.newXPath();
            path = (StringUtils.isEmpty((CharSequence)path) ? "/" : path);
            final XPathExpression xPathExpression = xPath.compile(path);
            final NodeList nodeList = (NodeList)xPathExpression.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); ++i) {
                final Deque<Map<String, Object>> stack = new LinkedList<Map<String, Object>>();
                this.handleNode(stack, nodeList.item(i), simpleMode);
                for (int index = 0; index < stack.size(); ++index) {
                    result.add(new MapResult(stack.pollFirst()));
                }
            }
        }
        catch (FileNotFoundException e) {
            if (!failOnError) {
                return Stream.of(new MapResult(Collections.emptyMap()));
            }
            throw new FileNotFoundException(e.getMessage());
        }
        catch (Exception e2) {
            if (!failOnError) {
                return Stream.of(new MapResult(Collections.emptyMap()));
            }
            throw new Exception(e2);
        }
        return result.stream();
    }
    
    private Stream<MapResult> xmlToMapResult(@Name("url") final String url, final boolean simpleMode) {
        try {
            final XMLStreamReader reader = this.getXMLStreamReaderFromUrl(url);
            final Deque<Map<String, Object>> stack = new LinkedList<Map<String, Object>>();
            do {
                this.handleXmlEvent(stack, reader, simpleMode);
            } while (this.proceedReader(reader));
            return Stream.of(new MapResult(stack.getFirst()));
        }
        catch (IOException | XMLStreamException ex2) {
            final Exception ex;
            final Exception e = ex;
            throw new RuntimeException("Can't read url " + Util.cleanUrl(url) + " as XML", e);
        }
    }
    
    private XMLStreamReader getXMLStreamReaderFromUrl(@Name("url") final String url) throws IOException, XMLStreamException {
        FileUtils.checkReadAllowed(url);
        final URLConnection urlConnection = new URL(url).openConnection();
        Xml.FACTORY.setProperty("javax.xml.stream.isCoalescing", true);
        return Xml.FACTORY.createXMLStreamReader(urlConnection.getInputStream());
    }
    
    private boolean proceedReader(final XMLStreamReader reader) throws XMLStreamException {
        if (reader.hasNext()) {
            do {
                reader.next();
            } while (reader.isWhiteSpace());
            return true;
        }
        return false;
    }
    
    private void handleXmlEvent(final Deque<Map<String, Object>> stack, final XMLStreamReader reader, final boolean simpleMode) throws XMLStreamException {
        switch (reader.getEventType()) {
            case 7:
            case 8: {
                break;
            }
            case 1: {
                final int attributes = reader.getAttributeCount();
                final Map<String, Object> elementMap = new LinkedHashMap<String, Object>(attributes + 3);
                elementMap.put("_type", reader.getLocalName());
                for (int a = 0; a < attributes; ++a) {
                    elementMap.put(reader.getAttributeLocalName(a), reader.getAttributeValue(a));
                }
                if (!stack.isEmpty()) {
                    final Map<String, Object> last = stack.getLast();
                    final String key = simpleMode ? ("_" + reader.getLocalName()) : "_children";
                    this.amendToList(last, key, elementMap);
                }
                stack.addLast(elementMap);
                break;
            }
            case 2: {
                final Map<String, Object> elementMap = (stack.size() > 1) ? stack.removeLast() : stack.getLast();
                final Object children = elementMap.get("_children");
                if (children != null && (children instanceof String || this.collectionIsAllStrings(children))) {
                    elementMap.put("_text", children);
                    elementMap.remove("_children");
                    break;
                }
                break;
            }
            case 4: {
                final String text = reader.getText().trim();
                if (!text.isEmpty()) {
                    final Map<String, Object> map = stack.getLast();
                    this.amendToList(map, "_children", text);
                    break;
                }
                break;
            }
            default: {
                throw new RuntimeException("dunno know how to handle xml event type " + reader.getEventType());
            }
        }
    }
    
    private void handleNode(final Deque<Map<String, Object>> stack, final Node node, final boolean simpleMode) {
        if (node.getNodeType() == 9) {
            final NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); ++i) {
                if (children.item(i).getLocalName() != null) {
                    this.handleNode(stack, children.item(i), simpleMode);
                    return;
                }
            }
        }
        final Map<String, Object> elementMap = new LinkedHashMap<String, Object>();
        this.handleTypeAndAttributes(node, elementMap);
        final NodeList children2 = node.getChildNodes();
        int count = 0;
        for (int j = 0; j < children2.getLength(); ++j) {
            final Node child = children2.item(j);
            if (child.getNodeType() != 3 && child.getNodeType() != 4) {
                this.handleNode(stack, child, simpleMode);
                ++count;
            }
            else {
                this.handleTextNode(child, elementMap);
            }
        }
        if (children2.getLength() > 1 && !stack.isEmpty()) {
            final List<Object> nodeChildren = new ArrayList<Object>();
            for (int k = 0; k < count; ++k) {
                nodeChildren.add(stack.pollLast());
            }
            final String key = simpleMode ? ("_" + node.getLocalName()) : "_children";
            Collections.reverse(nodeChildren);
            if (nodeChildren.size() > 0) {
                final Object text = elementMap.get("_text");
                if (text instanceof List) {
                    for (final Object element : (List)text) {
                        nodeChildren.add(element);
                    }
                    elementMap.remove("_text");
                }
                elementMap.put(key, nodeChildren);
            }
        }
        if (!elementMap.isEmpty()) {
            stack.addLast(elementMap);
        }
    }
    
    private void handleTypeAndAttributes(final Node node, final Map<String, Object> elementMap) {
        if (node.getLocalName() != null) {
            elementMap.put("_type", node.getLocalName());
        }
        if (node.getAttributes() != null) {
            final NamedNodeMap attributeMap = node.getAttributes();
            for (int i = 0; i < attributeMap.getLength(); ++i) {
                final Node attribute = attributeMap.item(i);
                elementMap.put(attribute.getNodeName(), attribute.getNodeValue());
            }
        }
    }
    
    private void handleTextNode(final Node node, final Map<String, Object> elementMap) {
        Object text = "";
        final int nodeType = node.getNodeType();
        switch (nodeType) {
            case 3: {
                text = this.normalizeText(node.getNodeValue());
                break;
            }
            case 4: {
                text = this.normalizeText(((CharacterData)node).getData());
                break;
            }
        }
        if (!StringUtils.isEmpty((CharSequence)text.toString())) {
            final Object previousText = elementMap.get("_text");
            if (previousText != null) {
                text = Arrays.asList(previousText.toString(), text);
            }
            elementMap.put("_text", text);
        }
    }
    
    private String normalizeText(final String text) {
        final String[] tokens = StringUtils.split(text, "\n");
        for (int i = 0; i < tokens.length; ++i) {
            tokens[i] = tokens[i].trim();
        }
        return StringUtils.join((Object[])tokens, " ").trim();
    }
    
    private boolean collectionIsAllStrings(final Object collection) {
        return collection instanceof Collection && ((Collection)collection).stream().allMatch(o -> o instanceof String);
    }
    
    private void amendToList(final Map<String, Object> map, final String key, final Object value) {
        final Object element = map.get(key);
        if (element == null) {
            map.put(key, value);
        }
        else if (element instanceof List) {
            ((List)element).add(value);
        }
        else {
            final List<Object> list = new LinkedList<Object>();
            list.add(element);
            list.add(value);
            map.put(key, list);
        }
    }
    
    static {
        FACTORY = XMLInputFactory.newFactory();
    }
}
