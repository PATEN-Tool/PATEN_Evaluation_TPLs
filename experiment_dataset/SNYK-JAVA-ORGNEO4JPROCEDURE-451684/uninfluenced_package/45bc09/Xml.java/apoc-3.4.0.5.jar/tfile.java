// 
// Decompiled by Procyon v0.5.36
// 

package apoc.load;

import java.util.ArrayDeque;
import org.apache.commons.lang3.BooleanUtils;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.neo4j.procedure.Mode;
import javax.xml.namespace.QName;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Label;
import apoc.result.NodeResult;
import java.util.Collection;
import java.util.Arrays;
import org.w3c.dom.CharacterData;
import org.w3c.dom.NamedNodeMap;
import java.util.Iterator;
import org.w3c.dom.Node;
import java.util.LinkedHashMap;
import java.net.URLConnection;
import java.net.URL;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Deque;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPath;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import java.util.List;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.NodeList;
import org.apache.commons.lang3.StringUtils;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import apoc.util.Util;
import apoc.util.FileUtils;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.Collections;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Procedure;
import apoc.result.MapResult;
import java.util.stream.Stream;
import java.util.Map;
import org.neo4j.procedure.Name;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.graphdb.GraphDatabaseService;
import javax.xml.stream.XMLInputFactory;

public class Xml
{
    public static final XMLInputFactory FACTORY;
    @Context
    public GraphDatabaseService db;
    @Context
    public Log log;
    
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
    
    private Stream<MapResult> xmlXpathToMapResult(@Name("url") String url, final boolean simpleMode, String path, Map<String, Object> config) throws Exception {
        if (config == null) {
            config = Collections.emptyMap();
        }
        final boolean failOnError = config.getOrDefault("failOnError", true);
        final List<MapResult> result = new ArrayList<MapResult>();
        try {
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilderFactory.setIgnoringElementContentWhitespace(true);
            documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            FileUtils.checkReadAllowed(url);
            url = FileUtils.changeFileUrlIfImportDirectoryConstrained(url);
            final Map<String, Object> headers = config.getOrDefault("headers", Collections.emptyMap());
            final Document doc = documentBuilder.parse(Util.openInputStream(url, headers, null));
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
            final XMLStreamReader reader = this.getXMLStreamReaderFromUrl(url, new XmlImportConfig(Collections.EMPTY_MAP));
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
    
    private XMLStreamReader getXMLStreamReaderFromUrl(String url, final XmlImportConfig config) throws IOException, XMLStreamException {
        FileUtils.checkReadAllowed(url);
        url = FileUtils.changeFileUrlIfImportDirectoryConstrained(url);
        final URLConnection urlConnection = new URL(url).openConnection();
        Xml.FACTORY.setProperty("javax.xml.stream.isCoalescing", true);
        InputStream inputStream = urlConnection.getInputStream();
        if (config.isFilterLeadingWhitespace()) {
            inputStream = new SkipWhitespaceInputStream(inputStream);
        }
        return Xml.FACTORY.createXMLStreamReader(inputStream);
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
        if (children2.getLength() > 0 && !stack.isEmpty()) {
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
    
    @Procedure(mode = Mode.WRITE, value = "apoc.xml.import")
    public Stream<NodeResult> importToGraph(@Name("url") final String url, @Name(value = "config", defaultValue = "{}") final Map<String, Object> config) throws IOException, XMLStreamException {
        final XmlImportConfig importConfig = new XmlImportConfig(config);
        final XMLStreamReader xml = this.getXMLStreamReaderFromUrl(url, importConfig);
        final org.neo4j.graphdb.Node root = this.db.createNode(new Label[] { Label.label("XmlDocument") });
        this.setPropertyIfNotNull(root, "_xmlVersion", xml.getVersion());
        this.setPropertyIfNotNull(root, "_xmlEncoding", xml.getEncoding());
        root.setProperty("url", (Object)url);
        final ImportState state = new ImportState(root);
        state.push(new ParentAndChildPair(root));
        while (xml.hasNext()) {
            xml.next();
            switch (xml.getEventType()) {
                case 7: {
                    continue;
                }
                case 3: {
                    final org.neo4j.graphdb.Node pi = this.db.createNode(new Label[] { Label.label("XmlProcessingInstruction") });
                    pi.setProperty("_piData", (Object)xml.getPIData());
                    pi.setProperty("_piTarget", (Object)xml.getPITarget());
                    state.updateLast(pi);
                    continue;
                }
                case 1: {
                    final QName qName = xml.getName();
                    final org.neo4j.graphdb.Node tag = this.db.createNode(new Label[] { Label.label("XmlTag") });
                    tag.setProperty("_name", (Object)qName.getLocalPart());
                    for (int i = 0; i < xml.getAttributeCount(); ++i) {
                        tag.setProperty(xml.getAttributeLocalName(i), (Object)xml.getAttributeValue(i));
                    }
                    state.updateLast(tag);
                    state.push(new ParentAndChildPair(tag));
                    continue;
                }
                case 4: {
                    final List<String> words = this.parseTextIntoPartsAndDelimiters(xml.getText(), importConfig.getDelimiter());
                    for (final String currentWord : words) {
                        this.createCharactersNode(currentWord, state, importConfig);
                    }
                    continue;
                }
                case 2: {
                    final String charactersForTag = importConfig.getCharactersForTag().get(xml.getName().getLocalPart());
                    if (charactersForTag != null) {
                        this.createCharactersNode(charactersForTag, state, importConfig);
                    }
                    final ParentAndChildPair parent = state.pop();
                    if (parent.getPreviousChild() != null) {
                        parent.getPreviousChild().createRelationshipTo(parent.getParent(), RelationshipType.withName("LAST_CHILD_OF"));
                        continue;
                    }
                    continue;
                }
                case 8: {
                    state.pop();
                    continue;
                }
                case 5:
                case 6: {
                    continue;
                }
                default: {
                    this.log.warn("xml file contains a {} type structure - ignoring this.", new Object[] { xml.getEventType() });
                    continue;
                }
            }
        }
        if (!state.isEmpty()) {
            throw new IllegalStateException("non empty parents, this indicates a bug");
        }
        return Stream.of(new NodeResult(root));
    }
    
    private void createCharactersNode(final String currentWord, final ImportState state, final XmlImportConfig importConfig) {
        final org.neo4j.graphdb.Node word = this.db.createNode(new Label[] { importConfig.getLabel() });
        word.setProperty("text", (Object)currentWord);
        word.setProperty("startIndex", (Object)state.getCurrentCharacterIndex());
        state.addCurrentCharacterIndex(currentWord.length());
        word.setProperty("endIndex", (Object)(state.getCurrentCharacterIndex() - 1));
        state.updateLast(word);
        if (importConfig.isConnectCharacters()) {
            state.getLastWord().createRelationshipTo(word, importConfig.getRelType());
            state.setLastWord(word);
        }
    }
    
    List<String> parseTextIntoPartsAndDelimiters(final String sourceString, final Pattern delimiterPattern) {
        final Matcher matcher = delimiterPattern.matcher(sourceString);
        final ArrayList<String> result = new ArrayList<String>();
        int prevEndIndex = 0;
        final int length = sourceString.length();
        while (matcher.find()) {
            final int start = matcher.start();
            final int end = matcher.end();
            if (prevEndIndex != start) {
                result.add(sourceString.substring(prevEndIndex, start));
            }
            result.add(sourceString.substring(start, end));
            prevEndIndex = end;
        }
        if (prevEndIndex != length) {
            result.add(sourceString.substring(prevEndIndex, length));
        }
        return result;
    }
    
    private void setPropertyIfNotNull(final org.neo4j.graphdb.Node root, final String propertyKey, final Object value) {
        if (value != null) {
            root.setProperty(propertyKey, value);
        }
    }
    
    static {
        FACTORY = XMLInputFactory.newFactory();
    }
    
    public static class ParentAndChildPair
    {
        private org.neo4j.graphdb.Node parent;
        private org.neo4j.graphdb.Node previousChild;
        
        public ParentAndChildPair(final org.neo4j.graphdb.Node parent) {
            this.previousChild = null;
            this.parent = parent;
        }
        
        public org.neo4j.graphdb.Node getParent() {
            return this.parent;
        }
        
        public void setParent(final org.neo4j.graphdb.Node parent) {
            this.parent = parent;
        }
        
        public org.neo4j.graphdb.Node getPreviousChild() {
            return this.previousChild;
        }
        
        public void setPreviousChild(final org.neo4j.graphdb.Node previousChild) {
            this.previousChild = previousChild;
        }
        
        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            final ParentAndChildPair that = (ParentAndChildPair)o;
            return this.parent.equals(that.parent);
        }
        
        @Override
        public int hashCode() {
            return this.parent.hashCode();
        }
    }
    
    private static class XmlImportConfig
    {
        private boolean connectCharacters;
        private Pattern delimiter;
        private Label label;
        private RelationshipType relType;
        private Map<String, String> charactersForTag;
        private final boolean filterLeadingWhitespace;
        
        public XmlImportConfig(final Map<String, Object> config) {
            this.label = Label.label("XmlCharacters");
            this.relType = RelationshipType.withName("NE");
            this.charactersForTag = new HashMap<String, String>();
            this.connectCharacters = BooleanUtils.toBoolean(Boolean.valueOf(config.get("connectCharacters")));
            this.filterLeadingWhitespace = BooleanUtils.toBoolean(Boolean.valueOf(config.get("filterLeadingWhitespace")));
            final String _delimiter = config.get("delimiter");
            if (_delimiter != null) {
                this.connectCharacters = true;
            }
            this.delimiter = Pattern.compile((_delimiter == null) ? "\\s" : _delimiter);
            final String _label = config.get("label");
            if (_label != null) {
                this.label = Label.label(_label);
                this.connectCharacters = true;
            }
            final String _relType = config.get("relType");
            if (_relType != null) {
                this.relType = RelationshipType.withName(_relType);
                this.connectCharacters = true;
            }
            final Map<String, String> _charactersForTag = config.get("charactersForTag");
            if (_charactersForTag != null) {
                this.charactersForTag = _charactersForTag;
            }
            final Boolean createNextWordRelationship = config.get("createNextWordRelationships");
            if (createNextWordRelationship != null) {
                this.relType = RelationshipType.withName("NEXT_WORD");
                this.label = Label.label("XmlWord");
                this.connectCharacters = true;
            }
        }
        
        public Pattern getDelimiter() {
            return this.delimiter;
        }
        
        public Label getLabel() {
            return this.label;
        }
        
        public RelationshipType getRelType() {
            return this.relType;
        }
        
        public boolean isConnectCharacters() {
            return this.connectCharacters;
        }
        
        public Map<String, String> getCharactersForTag() {
            return this.charactersForTag;
        }
        
        public boolean isFilterLeadingWhitespace() {
            return this.filterLeadingWhitespace;
        }
    }
    
    private static class ImportState
    {
        private final Deque<ParentAndChildPair> parents;
        private org.neo4j.graphdb.Node last;
        private org.neo4j.graphdb.Node lastWord;
        private int currentCharacterIndex;
        
        public ImportState(final org.neo4j.graphdb.Node initialNode) {
            this.parents = new ArrayDeque<ParentAndChildPair>();
            this.currentCharacterIndex = 0;
            this.last = initialNode;
            this.lastWord = initialNode;
        }
        
        public void push(final ParentAndChildPair parentAndChildPair) {
            this.parents.push(parentAndChildPair);
        }
        
        public org.neo4j.graphdb.Node getLastWord() {
            return this.lastWord;
        }
        
        public void setLastWord(final org.neo4j.graphdb.Node lastWord) {
            this.lastWord = lastWord;
        }
        
        public int getCurrentCharacterIndex() {
            return this.currentCharacterIndex;
        }
        
        public ParentAndChildPair pop() {
            return this.parents.pop();
        }
        
        public boolean isEmpty() {
            return this.parents.isEmpty();
        }
        
        public void updateLast(final org.neo4j.graphdb.Node thisNode) {
            final ParentAndChildPair parentAndChildPair = this.parents.peek();
            final org.neo4j.graphdb.Node parent = parentAndChildPair.getParent();
            final org.neo4j.graphdb.Node previousChild = parentAndChildPair.getPreviousChild();
            this.last.createRelationshipTo(thisNode, RelationshipType.withName("NEXT"));
            thisNode.createRelationshipTo(parent, RelationshipType.withName("IS_CHILD_OF"));
            if (previousChild == null) {
                thisNode.createRelationshipTo(parent, RelationshipType.withName("FIRST_CHILD_OF"));
            }
            else {
                previousChild.createRelationshipTo(thisNode, RelationshipType.withName("NEXT_SIBLING"));
            }
            parentAndChildPair.setPreviousChild(thisNode);
            this.last = thisNode;
        }
        
        public void addCurrentCharacterIndex(final int length) {
            this.currentCharacterIndex += length;
        }
    }
}
