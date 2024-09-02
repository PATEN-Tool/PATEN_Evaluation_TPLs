// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.uima.cas.impl;

import java.util.NoSuchElementException;
import java.util.Iterator;
import java.util.Collections;
import org.apache.uima.jcas.cas.NonEmptyStringList;
import org.apache.uima.jcas.cas.NonEmptyFloatList;
import org.apache.uima.jcas.cas.NonEmptyIntegerList;
import org.apache.uima.jcas.cas.StringList;
import org.apache.uima.jcas.cas.FloatList;
import org.apache.uima.jcas.cas.IntegerList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.EmptyList;
import org.apache.uima.jcas.cas.CommonPrimitiveArray;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.NonEmptyList;
import org.apache.uima.jcas.cas.CommonList;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.internal.util.I18nUtil;
import java.util.Locale;
import org.apache.uima.util.impl.Constants;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.xml.sax.SAXParseException;
import org.apache.uima.UimaSerializable;
import java.util.Arrays;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.cas.Type;
import org.apache.uima.internal.util.XmlElementNameAndContents;
import org.apache.uima.internal.util.XmlElementName;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.cas.Sofa;
import java.util.TreeMap;
import org.apache.uima.internal.util.function.Runnable_withSaxException;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.cas.FSIndexRepository;
import java.util.ArrayList;
import java.util.List;
import org.apache.uima.jcas.cas.TOP;
import org.xml.sax.Locator;
import org.apache.uima.internal.util.XmlAttribute;
import org.xml.sax.Attributes;
import java.net.URISyntaxException;
import java.net.URI;
import org.xml.sax.ContentHandler;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.apache.uima.internal.util.XMLUtils;
import java.io.IOException;
import org.xml.sax.SAXException;
import java.io.InputStream;
import org.xml.sax.helpers.DefaultHandler;
import org.apache.uima.cas.CAS;
import java.util.HashMap;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.TypeSystem;
import java.util.Map;
import java.util.regex.Pattern;

public class XmiCasDeserializer
{
    private static final boolean IS_NEW_FS = true;
    private static final boolean IS_EXISTING_FS = false;
    private static final int sofaTypeCode = 33;
    private static final Pattern whiteSpace;
    private static final String ID_ATTR_NAME = "xmi:id";
    private TypeSystemImpl ts;
    private Map<String, String> xmiNamespaceToUimaNamespaceMap;
    
    public XmiCasDeserializer(final TypeSystem ts, final UimaContext uimaContext) {
        this.xmiNamespaceToUimaNamespaceMap = new HashMap<String, String>();
        this.ts = (TypeSystemImpl)ts;
    }
    
    public XmiCasDeserializer(final TypeSystem ts) {
        this(ts, null);
    }
    
    public DefaultHandler getXmiCasHandler(final CAS cas) {
        return this.getXmiCasHandler(cas, false);
    }
    
    public DefaultHandler getXmiCasHandler(final CAS cas, final boolean lenient) {
        return this.getXmiCasHandler(cas, lenient, null);
    }
    
    public DefaultHandler getXmiCasHandler(final CAS cas, final boolean lenient, final XmiSerializationSharedData sharedData) {
        return this.getXmiCasHandler(cas, lenient, sharedData, -1);
    }
    
    public DefaultHandler getXmiCasHandler(final CAS cas, final boolean lenient, final XmiSerializationSharedData sharedData, final int mergePoint) {
        return this.getXmiCasHandler(cas, lenient, sharedData, mergePoint, AllowPreexistingFS.ignore);
    }
    
    public DefaultHandler getXmiCasHandler(final CAS cas, final boolean lenient, final XmiSerializationSharedData sharedData, final int mergePoint, final AllowPreexistingFS allow) {
        return new XmiCasDeserializerHandler((CASImpl)cas, lenient, sharedData, mergePoint, allow);
    }
    
    public static void deserialize(final InputStream aStream, final CAS aCAS) throws SAXException, IOException {
        deserialize(aStream, aCAS, false, null, -1);
    }
    
    public static void deserialize(final InputStream aStream, final CAS aCAS, final boolean aLenient) throws SAXException, IOException {
        deserialize(aStream, aCAS, aLenient, null, -1);
    }
    
    public static void deserialize(final InputStream aStream, final CAS aCAS, final boolean aLenient, final XmiSerializationSharedData aSharedData) throws SAXException, IOException {
        deserialize(aStream, aCAS, aLenient, aSharedData, -1);
    }
    
    public static void deserialize(final InputStream aStream, final CAS aCAS, final boolean aLenient, final XmiSerializationSharedData aSharedData, final int aMergePoint) throws SAXException, IOException {
        final XMLReader xmlReader = XMLUtils.createXMLReader();
        final XmiCasDeserializer deser = new XmiCasDeserializer(aCAS.getTypeSystem());
        final ContentHandler handler = deser.getXmiCasHandler(aCAS, aLenient, aSharedData, aMergePoint);
        xmlReader.setContentHandler(handler);
        xmlReader.parse(new InputSource(aStream));
    }
    
    public static void deserialize(final InputStream aStream, final CAS aCAS, final boolean aLenient, final XmiSerializationSharedData aSharedData, final int aMergePoint, final AllowPreexistingFS allowPreexistingFS) throws SAXException, IOException {
        final XMLReader xmlReader = XMLUtils.createXMLReader();
        final XmiCasDeserializer deser = new XmiCasDeserializer(aCAS.getTypeSystem());
        final ContentHandler handler = deser.getXmiCasHandler(aCAS, aLenient, aSharedData, aMergePoint, allowPreexistingFS);
        xmlReader.setContentHandler(handler);
        xmlReader.parse(new InputSource(aStream));
    }
    
    private String xmiElementName2uimaTypeName(final String nsUri, final String localName) throws SAXException {
        String uimaNamespace = this.xmiNamespaceToUimaNamespaceMap.get(nsUri);
        if (uimaNamespace == null) {
            if ("http:///uima/noNamespace.ecore".equals(nsUri)) {
                uimaNamespace = "";
            }
            else {
                URI uri;
                try {
                    uri = new URI(nsUri);
                }
                catch (URISyntaxException e) {
                    throw new SAXException(e);
                }
                String path;
                for (path = uri.getPath(); path.startsWith("/"); path = path.substring(1)) {}
                if (path.endsWith(".ecore")) {
                    path = path.substring(0, path.length() - 6);
                }
                uimaNamespace = path.replace('/', '.') + '.';
            }
            this.xmiNamespaceToUimaNamespaceMap.put(nsUri, uimaNamespace);
        }
        return uimaNamespace + localName;
    }
    
    private void addOutOfTypeSystemAttributes(final XmiSerializationSharedData.OotsElementData ootsElem, final Attributes attrs) {
        for (int i = 0; i < attrs.getLength(); ++i) {
            final String attrName = attrs.getQName(i);
            final String attrValue = attrs.getValue(i);
            if (!attrName.equals("xmi:id")) {
                ootsElem.attributes.add(new XmlAttribute(attrName, attrValue));
            }
        }
    }
    
    private void report0xmiId() {
        final Throwable t = new Throwable();
        System.err.println("Debug 0 xmiId encountered where not expected");
        t.printStackTrace();
    }
    
    static {
        whiteSpace = Pattern.compile("\\s+");
    }
    
    public class XmiCasDeserializerHandler extends DefaultHandler
    {
        private static final int DOC_STATE = 0;
        private static final int FS_STATE = 1;
        private static final int FEAT_STATE = 2;
        private static final int FEAT_CONTENT_STATE = 3;
        private static final int IGNORING_XMI_ELEMENTS_STATE = 4;
        private static final int REF_FEAT_STATE = 5;
        private static final String unknownXMLSource = "<unknown>";
        private Locator locator;
        private CASImpl casBeingFilled;
        private int state;
        private StringBuilder buffer;
        private TOP currentFs;
        private TypeImpl currentType;
        private int currentArrayId;
        private List<String> currentArrayElements;
        private Map<String, ArrayList<String>> multiValuedFeatures;
        private List<FSIndexRepository> indexRepositories;
        private List<CAS> views;
        boolean lenient;
        private int ignoreDepth;
        private Map<String, String> nsPrefixToUriMap;
        private XmiSerializationSharedData sharedData;
        private int nextSofaNum;
        private int mergePoint;
        private XmiSerializationSharedData.OotsElementData outOfTypeSystemElement;
        private List<XmiSerializationSharedData.OotsElementData> deferredFSs;
        private XmiSerializationSharedData.OotsElementData deferredFsElement;
        private boolean processingDeferredFSs;
        private boolean isDoingDeferredChildElements;
        private Map<Integer, TOP> localXmiIdToFs;
        AllowPreexistingFS allowPreexistingFS;
        IntVector featsSeen;
        boolean disallowedViewMemberEncountered;
        private final DeferredIndexUpdates toBeAdded;
        private final DeferredIndexUpdates toBeRemoved;
        private final List<Runnable_withSaxException> fixupToDos;
        private final List<Runnable> uimaSerializableFixups;
        
        private XmiCasDeserializerHandler(final CASImpl aCAS, final boolean lenient, final XmiSerializationSharedData sharedData, final int mergePoint, final AllowPreexistingFS allowPreexistingFS) {
            this.multiValuedFeatures = new TreeMap<String, ArrayList<String>>();
            this.ignoreDepth = 0;
            this.nsPrefixToUriMap = new HashMap<String, String>();
            this.outOfTypeSystemElement = null;
            this.deferredFSs = null;
            this.deferredFsElement = null;
            this.processingDeferredFSs = false;
            this.isDoingDeferredChildElements = false;
            this.localXmiIdToFs = new HashMap<Integer, TOP>();
            this.featsSeen = null;
            this.toBeAdded = new DeferredIndexUpdates();
            this.toBeRemoved = new DeferredIndexUpdates();
            this.fixupToDos = new ArrayList<Runnable_withSaxException>();
            this.uimaSerializableFixups = new ArrayList<Runnable>();
            this.casBeingFilled = aCAS.getBaseCAS();
            this.lenient = lenient;
            this.sharedData = ((sharedData != null) ? sharedData : new XmiSerializationSharedData());
            this.mergePoint = mergePoint;
            this.allowPreexistingFS = allowPreexistingFS;
            this.featsSeen = null;
            this.disallowedViewMemberEncountered = false;
            if (mergePoint < 0) {
                this.casBeingFilled.resetNoQuestions();
                this.sharedData.clearIdMap();
                this.nextSofaNum = 2;
            }
            else {
                this.nextSofaNum = this.casBeingFilled.getViewCount() + 1;
            }
            this.buffer = new StringBuilder();
            this.indexRepositories = new ArrayList<FSIndexRepository>();
            this.views = new ArrayList<CAS>();
            this.indexRepositories.add(this.casBeingFilled.getBaseIndexRepository());
            this.indexRepositories.add(this.casBeingFilled.getView("_InitialView").getIndexRepository());
            final FSIterator<Sofa> sofaIter = this.casBeingFilled.getSofaIterator();
            while (sofaIter.hasNext()) {
                final SofaFS sofa = sofaIter.next();
                if (sofa.getSofaRef() == 1) {
                    this.casBeingFilled.registerInitialSofa();
                }
                else {
                    Misc.setWithExpand(this.indexRepositories, sofa.getSofaRef(), this.casBeingFilled.getSofaIndexRepository(sofa));
                }
            }
        }
        
        private final void resetBuffer() {
            this.buffer.setLength(0);
        }
        
        @Override
        public void startDocument() throws SAXException {
            this.state = 0;
        }
        
        @Override
        public void startElement(String nameSpaceURI, String localName, final String qualifiedName, final Attributes attrs) throws SAXException {
            this.resetBuffer();
            switch (this.state) {
                case 0: {
                    if (attrs != null) {
                        for (int i = 0; i < attrs.getLength(); ++i) {
                            final String attrName = attrs.getQName(i);
                            if (attrName.startsWith("xmlns:")) {
                                final String prefix = attrName.substring(6);
                                final String uri = attrs.getValue(i);
                                this.nsPrefixToUriMap.put(prefix, uri);
                            }
                        }
                    }
                    this.state = 1;
                    break;
                }
                case 1: {
                    if (qualifiedName.startsWith("xmi")) {
                        this.state = 4;
                        ++this.ignoreDepth;
                        return;
                    }
                    if (this.mergePoint >= 0) {
                        final String id = attrs.getValue("xmi:id");
                        if (id != null) {
                            final int idInt = Integer.parseInt(id);
                            if (idInt > 0 && !this.isNewFS(idInt)) {
                                if (this.allowPreexistingFS == AllowPreexistingFS.ignore) {
                                    this.state = 4;
                                    ++this.ignoreDepth;
                                    return;
                                }
                                if (this.allowPreexistingFS == AllowPreexistingFS.disallow) {
                                    throw new CASRuntimeException("DELTA_CAS_PREEXISTING_FS_DISALLOWED", new Object[] { "xmi:id=" + id, nameSpaceURI, localName, qualifiedName });
                                }
                            }
                        }
                    }
                    if (nameSpaceURI == null || nameSpaceURI.length() == 0) {
                        final int colonIndex = qualifiedName.indexOf(58);
                        if (colonIndex != -1) {
                            final String prefix2 = qualifiedName.substring(0, colonIndex);
                            nameSpaceURI = this.nsPrefixToUriMap.get(prefix2);
                            if (nameSpaceURI == null) {
                                nameSpaceURI = "http:///" + prefix2 + ".ecore";
                            }
                            localName = qualifiedName.substring(colonIndex + 1);
                        }
                        else {
                            nameSpaceURI = "http:///uima/noNamespace.ecore";
                        }
                    }
                    this.readFS(nameSpaceURI, localName, qualifiedName, attrs);
                    this.multiValuedFeatures.clear();
                    this.state = 2;
                    break;
                }
                case 2: {
                    final String href = attrs.getValue("href");
                    if (href != null && href.startsWith("#")) {
                        if (this.outOfTypeSystemElement != null) {
                            final XmlElementName elemName = new XmlElementName(nameSpaceURI, localName, qualifiedName);
                            final List<XmlAttribute> ootsAttrs = new ArrayList<XmlAttribute>();
                            ootsAttrs.add(new XmlAttribute("href", href));
                            final XmlElementNameAndContents elemWithContents = new XmlElementNameAndContents(elemName, null, ootsAttrs);
                            this.outOfTypeSystemElement.childElements.add(elemWithContents);
                        }
                        else {
                            ArrayList<String> valueList = this.multiValuedFeatures.get(qualifiedName);
                            if (valueList == null) {
                                valueList = new ArrayList<String>();
                                this.multiValuedFeatures.put(qualifiedName, valueList);
                            }
                            valueList.add(href.substring(1));
                        }
                        this.state = 5;
                        break;
                    }
                    this.state = 3;
                    break;
                }
                case 4: {
                    ++this.ignoreDepth;
                    break;
                }
                default: {
                    throw this.createException(1, qualifiedName);
                }
            }
        }
        
        private void readFS(final String nameSpaceURI, final String localName, final String qualifiedName, final Attributes attrs) throws SAXException {
            final String typeName = XmiCasDeserializer.this.xmiElementName2uimaTypeName(nameSpaceURI, localName);
            this.currentType = XmiCasDeserializer.this.ts.getType(typeName);
            if (this.currentType != null) {
                if (this.currentType.isArray()) {
                    final String idStr = attrs.getValue("xmi:id");
                    this.currentArrayId = ((idStr == null) ? -1 : Integer.parseInt(idStr));
                    final String elements = attrs.getValue("elements");
                    if (this.casBeingFilled.isByteArrayType(this.currentType)) {
                        this.createOrUpdateByteArray(elements, this.currentArrayId, null);
                    }
                    else if (elements != null) {
                        final String[] parsedElements = this.parseArray(elements);
                        this.currentArrayElements = Arrays.asList(parsedElements);
                    }
                    else {
                        this.currentArrayElements = null;
                    }
                }
                else {
                    final String idStr = attrs.getValue("xmi:id");
                    final int xmiId = (idStr == null) ? -1 : Integer.parseInt(idStr);
                    if (this.isNewFS(xmiId)) {
                        TOP fs;
                        if (33 == this.currentType.getCode()) {
                            String sofaID = attrs.getValue("sofaID");
                            int nextSofaNum;
                            if (sofaID.equals("_InitialView") || sofaID.equals("_DefaultTextSofaName")) {
                                nextSofaNum = 1;
                            }
                            else {
                                this.nextSofaNum = (nextSofaNum = this.nextSofaNum) + 1;
                            }
                            final int thisSofaNum = nextSofaNum;
                            final String sofaMimeType = attrs.getValue("mimeType");
                            if (sofaID.equals("_DefaultTextSofaName")) {
                                sofaID = "_InitialView";
                            }
                            fs = this.casBeingFilled.createSofa(thisSofaNum, sofaID, sofaMimeType);
                        }
                        else if (this.currentType.isAnnotationBaseType()) {
                            final String extSofaRef = attrs.getValue("sofa");
                            CAS casView = null;
                            if (extSofaRef == null || extSofaRef.length() == 0) {
                                this.doDeferFsOrThrow(idStr, nameSpaceURI, localName, qualifiedName, attrs);
                                return;
                            }
                            final Sofa sofa = (Sofa)this.maybeGetFsForXmiId(Integer.parseInt(extSofaRef));
                            if (null != sofa) {
                                casView = this.casBeingFilled.getView(sofa);
                            }
                            if (casView == null) {
                                this.doDeferFsOrThrow(idStr, nameSpaceURI, localName, qualifiedName, attrs);
                                return;
                            }
                            if (this.currentType.getCode() == 36) {
                                fs = casView.getDocumentAnnotation();
                            }
                            else {
                                fs = casView.createFS(this.currentType);
                                if (this.currentFs instanceof UimaSerializable) {
                                    final UimaSerializable ufs = (UimaSerializable)this.currentFs;
                                    this.uimaSerializableFixups.add(() -> ufs._init_from_cas_data());
                                }
                            }
                        }
                        else {
                            fs = this.casBeingFilled.createFS(this.currentType);
                            if (this.currentFs instanceof UimaSerializable) {
                                final UimaSerializable ufs2 = (UimaSerializable)this.currentFs;
                                this.uimaSerializableFixups.add(() -> ufs2._init_from_cas_data());
                            }
                        }
                        this.readFS(fs, attrs, true);
                    }
                    else {
                        if (this.allowPreexistingFS == AllowPreexistingFS.disallow) {
                            throw new CASRuntimeException("DELTA_CAS_PREEXISTING_FS_DISALLOWED", new Object[] { "xmi:id=" + idStr, nameSpaceURI, localName, qualifiedName });
                        }
                        if (this.allowPreexistingFS == AllowPreexistingFS.allow) {
                            final TOP fs = this.getFsForXmiId(xmiId);
                            this.readFS(fs, attrs, false);
                        }
                    }
                }
                return;
            }
            if ("uima.cas.NULL".equals(typeName)) {
                return;
            }
            if ("uima.cas.View".equals(typeName)) {
                this.processDeferredFSs();
                this.processView(attrs.getValue("sofa"), attrs.getValue("members"));
                final String added = attrs.getValue("added_members");
                final String deleted = attrs.getValue("deleted_members");
                final String reindexed = attrs.getValue("reindexed_members");
                this.processView(attrs.getValue("sofa"), added, deleted, reindexed);
                return;
            }
            if (!this.lenient) {
                throw this.createException(4, typeName);
            }
            this.addToOutOfTypeSystemData(new XmlElementName(nameSpaceURI, localName, qualifiedName), attrs);
        }
        
        private void doDeferFsOrThrow(final String idStr, final String nameSpaceURI, final String localName, final String qualifiedName, final Attributes attrs) throws XCASParsingException {
            if (this.processingDeferredFSs) {
                throw this.createException(13);
            }
            if (this.deferredFSs == null) {
                this.deferredFSs = new ArrayList<XmiSerializationSharedData.OotsElementData>();
            }
            this.deferredFsElement = new XmiSerializationSharedData.OotsElementData(idStr, new XmlElementName(nameSpaceURI, localName, qualifiedName), (this.locator == null) ? 0 : this.locator.getLineNumber(), (this.locator == null) ? 0 : this.locator.getColumnNumber());
            this.deferredFSs.add(this.deferredFsElement);
            XmiCasDeserializer.this.addOutOfTypeSystemAttributes(this.deferredFsElement, attrs);
        }
        
        private void processView(final String sofa, final String membersString) throws SAXParseException {
            if (membersString != null) {
                final int sofaXmiId = (sofa == null) ? 1 : Integer.parseInt(sofa);
                final FSIndexRepositoryImpl indexRep = this.getIndexRepo(sofa, sofaXmiId);
                final boolean newview = sofa != null && this.isNewFS(sofaXmiId);
                final List<TOP> todo = this.toBeAdded.getTodos(indexRep);
                final String[] members = this.parseArray(membersString);
                for (int i = 0; i < members.length; ++i) {
                    final int xmiId = Integer.parseInt(members[i]);
                    if (!newview && !this.isNewFS(xmiId)) {
                        if (this.allowPreexistingFS == AllowPreexistingFS.ignore) {
                            continue;
                        }
                        if (this.allowPreexistingFS == AllowPreexistingFS.disallow) {
                            this.disallowedViewMemberEncountered = true;
                            continue;
                        }
                    }
                    final TOP fs = this.maybeGetFsForXmiId(xmiId);
                    if (fs != null) {
                        todo.add(fs);
                    }
                    else {
                        if (!this.lenient) {
                            if (xmiId == 0) {
                                XmiCasDeserializer.this.report0xmiId();
                            }
                            throw this.createException(12, Integer.toString(xmiId));
                        }
                        this.sharedData.addOutOfTypeSystemViewMember(sofa, members[i]);
                    }
                }
            }
        }
        
        private FSIndexRepositoryImpl getIndexRepo(final String sofaXmiIdAsString, final int sofaXmiId) throws XCASParsingException {
            if (sofaXmiIdAsString == null) {
                return this.indexRepositories.get(1);
            }
            final Sofa sofa = (Sofa)this.maybeGetFsForXmiId(sofaXmiId);
            if (null == sofa) {
                if (sofaXmiId == 0) {
                    XmiCasDeserializer.this.report0xmiId();
                }
                throw this.createException(12, Integer.toString(sofaXmiId));
            }
            return this.indexRepositories.get(sofa.getSofaNum());
        }
        
        private void processView(final String sofa, final String addmembersString, final String delmemberString, final String reindexmemberString) throws SAXParseException {
            if (addmembersString != null) {
                this.processView(sofa, addmembersString);
            }
            if (delmemberString == null && reindexmemberString == null) {
                return;
            }
            final int sofaXmiId = (sofa == null) ? 1 : Integer.parseInt(sofa);
            final FSIndexRepositoryImpl indexRep = this.getIndexRepo(sofa, sofaXmiId);
            if (delmemberString != null) {
                final List<TOP> localRemoves = this.toBeRemoved.getTodos(indexRep);
                final String[] members = this.parseArray(delmemberString);
                for (int i = 0; i < members.length; ++i) {
                    final int xmiId = Integer.parseInt(members[i]);
                    if (!this.isNewFS(xmiId)) {
                        if (this.allowPreexistingFS == AllowPreexistingFS.disallow) {
                            this.disallowedViewMemberEncountered = true;
                            continue;
                        }
                        if (this.allowPreexistingFS == AllowPreexistingFS.ignore) {
                            continue;
                        }
                    }
                    final TOP fs = this.maybeGetFsForXmiId(xmiId);
                    if (fs != null) {
                        localRemoves.add(fs);
                    }
                    else {
                        if (!this.lenient) {
                            if (xmiId == 0) {
                                XmiCasDeserializer.this.report0xmiId();
                            }
                            throw this.createException(12, Integer.toString(xmiId));
                        }
                        this.sharedData.addOutOfTypeSystemViewMember(sofa, members[i]);
                    }
                }
            }
        }
        
        private void readFS(final TOP fs, final Attributes attrs, final boolean isNewFs) throws SAXException {
            this.currentFs = fs;
            final TypeImpl type = fs._getTypeImpl();
            final int typeCode = type.getCode();
            this.featsSeen = null;
            try {
                if (!isNewFs || fs._getTypeCode() == 36) {
                    this.casBeingFilled.removeFromCorruptableIndexAnyView(fs, this.casBeingFilled.getAddbackSingle());
                }
                final String idStr = attrs.getValue("xmi:id");
                int extId;
                try {
                    extId = Integer.parseInt(idStr);
                }
                catch (NumberFormatException e) {
                    throw this.createException(5, idStr);
                }
                this.addFsToXmiId(fs, extId);
                this.featsSeen = ((33 != typeCode && !isNewFs) ? new IntVector(attrs.getLength()) : null);
                for (int i = 0; i < attrs.getLength(); ++i) {
                    final String attrName = attrs.getQName(i);
                    final String attrValue = attrs.getValue(i);
                    if (!attrName.equals("xmi:id")) {
                        final int featCode = this.handleFeatureFromName(type, fs, attrName, attrValue, isNewFs);
                        if (this.featsSeen != null && featCode != -1) {
                            this.featsSeen.add(featCode);
                        }
                    }
                }
            }
            finally {
                if (!isNewFs || fs._getTypeCode() == 36) {
                    this.casBeingFilled.addbackSingle(fs);
                }
            }
            if (33 == typeCode && isNewFs) {
                final Sofa sofa = (Sofa)fs;
                this.casBeingFilled.getBaseIndexRepository().addFS(sofa);
                final CAS view = this.casBeingFilled.getView(sofa);
                if (sofa.getSofaRef() == 1) {
                    this.casBeingFilled.registerInitialSofa();
                }
                else {
                    this.indexRepositories.add(this.casBeingFilled.getSofaIndexRepository(sofa));
                }
                ((CASImpl)view).registerView(sofa);
                this.views.add(view);
            }
        }
        
        private final boolean emptyVal(final String val) {
            return val == null || val.length() == 0;
        }
        
        private int handleFeatureFromName(final TypeImpl type, final TOP fs, final String featName, final String featVal, final boolean isNewFS) throws SAXException {
            final FeatureImpl feat = type.getFeatureByBaseName(featName);
            if (feat != null) {
                if (fs instanceof Sofa && !isNewFS) {
                    if (featName.equals("sofaID") || featName.equals("sofaNum")) {
                        return feat.getCode();
                    }
                    if (((Sofa)fs).isSofaDataSet()) {
                        return feat.getCode();
                    }
                }
                this.handleFeatSingleValue(fs, feat, featVal);
                return feat.getCode();
            }
            if (!this.lenient) {
                throw this.createException(8, featName);
            }
            if (this.isDoingDeferredChildElements) {
                final ArrayList<String> featValAsArrayList = new ArrayList<String>(1);
                featValAsArrayList.add(featVal);
                this.sharedData.addOutOfTypeSystemChildElements(fs, featName, featValAsArrayList);
            }
            else {
                this.sharedData.addOutOfTypeSystemAttribute(fs, featName, featVal);
            }
            return -1;
        }
        
        private int handleFeatMultiValueFromName(final Type type, final TOP fs, final String featName, final ArrayList<String> featVals) throws SAXException {
            final FeatureImpl feat = (FeatureImpl)type.getFeatureByBaseName(featName);
            if (feat != null) {
                this.handleFeatMultiValue(fs, feat, featVals);
                return feat.getCode();
            }
            if (!this.lenient) {
                throw this.createException(8, featName);
            }
            this.sharedData.addOutOfTypeSystemChildElements(fs, featName, featVals);
            return -1;
        }
        
        private void handleFeatSingleValue(final TOP fs, final FeatureImpl fi, final String featVal) throws SAXException {
            if (fs instanceof AnnotationBase && fi.getCode() == 15) {
                return;
            }
            final int rangeClass = fi.rangeTypeClass;
            switch (rangeClass) {
                case 1:
                case 2:
                case 3:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13: {
                    CASImpl.setFeatureValueFromStringNoDocAnnotUpdate(fs, fi, featVal);
                    break;
                }
                case 8: {
                    this.deserializeFsRef(featVal, fi, fs);
                    break;
                }
                case 4:
                case 5:
                case 6:
                case 7:
                case 14:
                case 15:
                case 16:
                case 17:
                case 18: {
                    if (fi.isMultipleReferencesAllowed()) {
                        this.deserializeFsRef(featVal, fi, fs);
                        break;
                    }
                    if (rangeClass == 15) {
                        final ByteArray existingByteArray = (ByteArray)fs.getFeatureValue((Feature)fi);
                        final ByteArray byteArray = this.createOrUpdateByteArray(featVal, -1, existingByteArray);
                        if (byteArray != existingByteArray) {
                            CASImpl.setFeatureValueMaybeSofa(fs, fi, byteArray);
                        }
                        break;
                    }
                    final String[] arrayVals = this.parseArray(featVal);
                    this.handleFeatMultiValue(fs, fi, Arrays.asList(arrayVals));
                    break;
                }
                case 101:
                case 102:
                case 103:
                case 104: {
                    if (fi.isMultipleReferencesAllowed()) {
                        this.deserializeFsRef(featVal, fi, fs);
                        break;
                    }
                    final String[] arrayVals = this.parseArray(featVal);
                    this.handleFeatMultiValue(fs, fi, Arrays.asList(arrayVals));
                    break;
                }
                default: {
                    Misc.internalError();
                    break;
                }
            }
        }
        
        private void deserializeFsRef(final String featVal, final FeatureImpl fi, final TOP fs) {
            if (featVal == null || featVal.length() == 0) {
                CASImpl.setFeatureValueMaybeSofa(fs, fi, null);
            }
            else {
                final int xmiId = Integer.parseInt(featVal);
                final TOP tgtFs = this.maybeGetFsForXmiId(xmiId);
                if (null == tgtFs) {
                    this.fixupToDos.add(() -> this.finalizeRefValue(xmiId, fs, fi));
                }
                else {
                    CASImpl.setFeatureValueMaybeSofa(fs, fi, tgtFs);
                    XmiCasDeserializer.this.ts.fixupFSArrayTypes(fi.getRangeImpl(), tgtFs);
                }
            }
        }
        
        private String[] parseArray(String val) {
            val = val.trim();
            String[] arrayVals;
            if (this.emptyVal(val)) {
                arrayVals = Constants.EMPTY_STRING_ARRAY;
            }
            else {
                arrayVals = XmiCasDeserializer.whiteSpace.split(val);
            }
            return arrayVals;
        }
        
        private void handleFeatMultiValue(final TOP fs, final FeatureImpl fi, final List<String> featVals) throws SAXException {
            final int rangeCode = fi.rangeTypeClass;
            switch (rangeCode) {
                case 1:
                case 2:
                case 3:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13: {
                    if (featVals.size() != 1) {
                        throw new SAXParseException(I18nUtil.localizeMessage("org.apache.uima.UIMAException_Messages", Locale.getDefault(), "multiple_values_unexpected", new Object[] { fi.getName() }), this.locator);
                    }
                    this.handleFeatSingleValue(fs, fi, featVals.get(0));
                    break;
                }
                case 4:
                case 5:
                case 6:
                case 7:
                case 14:
                case 15:
                case 16:
                case 17:
                case 18: {
                    final CommonArrayFS existingArray = (CommonArrayFS)fs.getFeatureValue((Feature)fi);
                    final CommonArrayFS casArray = this.createOrUpdateArray(fi.getRangeImpl(), featVals, -1, existingArray);
                    if (existingArray != casArray) {
                        CASImpl.setFeatureValueMaybeSofa(fs, fi, (TOP)casArray);
                    }
                    if (!fi.isMultipleReferencesAllowed()) {
                        this.addNonsharedFSToEncompassingFSMapping((TOP)casArray, fs);
                        break;
                    }
                    break;
                }
                case 101:
                case 102:
                case 103:
                case 104: {
                    if (featVals == null) {
                        fs.setFeatureValue(fi, null);
                        break;
                    }
                    if (featVals.size() == 0) {
                        fs.setFeatureValue(fi, this.casBeingFilled.emptyList(rangeCode));
                        break;
                    }
                    final CommonList existingList = (CommonList)fs.getFeatureValue((Feature)fi);
                    final CommonList theList = this.createOrUpdateList(fi.getRangeImpl(), featVals, -1, existingList);
                    if (existingList != theList) {
                        fs.setFeatureValue(fi, theList);
                    }
                    if (!fi.isMultipleReferencesAllowed()) {
                        for (CommonList node = theList; node != null && node instanceof NonEmptyList; node = node.getCommonTail()) {
                            this.addNonsharedFSToEncompassingFSMapping((TOP)node, fs);
                        }
                    }
                    break;
                }
                default: {
                    assert false;
                    break;
                }
            }
        }
        
        private CommonList createOrUpdateList(final TypeImpl listType, final List<String> values, final int xmiId, final CommonList existingList) {
            if (existingList != null) {
                this.updateExistingList(values, existingList);
                return existingList;
            }
            return this.createListFromStringValues(values, this.casBeingFilled.emptyListFromTypeCode(listType.getCode()));
        }
        
        private CommonArrayFS createOrUpdateArray(final TypeImpl arrayType, final List<String> values, final int xmiId, CommonArrayFS existingArray) throws XCASParsingException {
            if (values == null) {
                return null;
            }
            final int arrayLen = values.size();
            CommonArrayFS resultArray;
            if (existingArray != null) {
                if (arrayLen == 0) {
                    resultArray = (CommonArrayFS)((existingArray.size() == 0) ? existingArray : this.casBeingFilled.createArray(arrayType, 0));
                }
                else if (existingArray.size() == arrayLen) {
                    this.updateExistingArray(values, existingArray);
                    resultArray = existingArray;
                }
                else {
                    resultArray = this.createNewArray(arrayType, values);
                }
            }
            else if (xmiId == -1 || this.isNewFS(xmiId)) {
                resultArray = this.createNewArray(arrayType, values);
            }
            else {
                existingArray = (CommonArrayFS)this.getFsForXmiId(xmiId);
                if (existingArray.size() == arrayLen) {
                    this.updateExistingArray(values, existingArray);
                    resultArray = existingArray;
                }
                else {
                    resultArray = this.createNewArray(arrayType, values);
                }
            }
            final TOP newOrUpdated = (TOP)resultArray;
            this.addFsToXmiId(newOrUpdated, xmiId);
            return resultArray;
        }
        
        private CommonArrayFS createNewArray(final TypeImpl type, final List<String> values) {
            final int sz = values.size();
            final CommonArrayFS fs = (CommonArrayFS)((sz == 0) ? this.casBeingFilled.emptyArray(type) : this.casBeingFilled.createArray(type, sz));
            if (fs instanceof FSArray) {
                final FSArray fsArray = (FSArray)fs;
                for (int i = 0; i < sz; ++i) {
                    this.maybeSetFsArrayElement(values, i, fsArray);
                }
            }
            else {
                final CommonPrimitiveArray fsp = (CommonPrimitiveArray)fs;
                for (int i = 0; i < sz; ++i) {
                    fsp.setArrayValueFromString(i, values.get(i));
                }
            }
            return fs;
        }
        
        private void updateExistingArray(final List<String> values, final CommonArrayFS existingArray) {
            final int sz = values.size();
            if (existingArray instanceof FSArray) {
                final FSArray fsArray = (FSArray)existingArray;
                for (int i = 0; i < sz; ++i) {
                    final String featVal = values.get(i);
                    if (this.emptyVal(featVal)) {
                        fsArray.set(i, null);
                    }
                    else {
                        this.maybeSetFsArrayElement(values, i, fsArray);
                        final int xmiId = Integer.parseInt(featVal);
                        final int pos = i;
                        final TOP tgtFs = this.maybeGetFsForXmiId(xmiId);
                        if (null == tgtFs) {
                            this.fixupToDos.add(() -> this.finalizeFSArrayRefValue(xmiId, fsArray, pos));
                        }
                        else {
                            fsArray.set(i, tgtFs);
                        }
                    }
                }
                return;
            }
            final CommonPrimitiveArray existingPrimitiveArray = (CommonPrimitiveArray)existingArray;
            for (int i = 0; i < sz; ++i) {
                existingPrimitiveArray.setArrayValueFromString(i, values.get(i));
            }
        }
        
        private CommonList updateExistingList(final List<String> values, final CommonList existingList) {
            if (values == null || values.size() == 0) {
                if (existingList instanceof EmptyList) {
                    return existingList;
                }
                return existingList.emptyList();
            }
            else {
                final int valLen = values.size();
                if (existingList instanceof EmptyList) {
                    return this.createListFromStringValues(values, (EmptyList)existingList);
                }
                if (existingList instanceof FSList) {
                    FSList node = (FSList)existingList;
                    NonEmptyFSList prevNode = null;
                    for (int i = 0; i < valLen; ++i) {
                        if (node instanceof EmptyList) {
                            prevNode.setTail(this.createListFromStringValues(values, i, (EmptyList)node));
                            return existingList;
                        }
                        final NonEmptyFSList neNode = (NonEmptyFSList)node;
                        this.maybeSetFsListHead(values.get(i), neNode);
                        prevNode = (NonEmptyFSList)node;
                        node = prevNode.getTail();
                    }
                    prevNode.setTail(existingList.emptyList());
                    return existingList;
                }
                CommonList node2 = existingList;
                CommonList prevNode2 = null;
                for (int i = 0; i < valLen; ++i) {
                    if (node2 instanceof EmptyList) {
                        prevNode2.setTail(this.createListFromStringValues(values, i, (EmptyList)node2));
                        return existingList;
                    }
                    node2.set_headFromString(values.get(i));
                    prevNode2 = node2;
                    node2 = node2.getCommonTail();
                }
                prevNode2.setTail(existingList.emptyList());
                return existingList;
            }
        }
        
        private void maybeSetFsArrayElement(final List<String> values, final int i, final FSArray fsArray) {
            final String featVal = values.get(i);
            if (this.emptyVal(featVal)) {
                fsArray.set(i, null);
            }
            else {
                final int xmiId = Integer.parseInt(featVal);
                final int pos = i;
                final TOP tgtFs = this.maybeGetFsForXmiId(xmiId);
                if (null == tgtFs) {
                    this.fixupToDos.add(() -> this.finalizeFSArrayRefValue(xmiId, fsArray, pos));
                }
                else {
                    fsArray.set(i, tgtFs);
                }
            }
        }
        
        private void maybeSetFsListHead(final String featVal, final NonEmptyFSList neNode) {
            if (this.emptyVal(featVal)) {
                neNode.setHead(null);
            }
            else {
                final int xmiId = Integer.parseInt(featVal);
                final TOP tgtFs = this.maybeGetFsForXmiId(xmiId);
                if (null == tgtFs) {
                    this.fixupToDos.add(() -> this.finalizeFSListRefValue(xmiId, neNode));
                }
                else {
                    neNode.setHead(tgtFs);
                }
            }
        }
        
        CommonList createListFromStringValues(final List<String> stringValues, final EmptyList emptyNode) {
            return this.createListFromStringValues(stringValues, 0, emptyNode);
        }
        
        private CommonList createListFromStringValues(final List<String> stringValues, final int startPos, final EmptyList emptyNode) {
            if (emptyNode instanceof FSList) {
                FSList n = (FSList)emptyNode;
                for (int i = stringValues.size() - 1; i >= startPos; --i) {
                    final String v = stringValues.get(i);
                    final NonEmptyFSList nn = n.pushNode();
                    this.maybeSetFsListHead(v, nn);
                    n = nn;
                }
                return n;
            }
            if (emptyNode instanceof IntegerList) {
                IntegerList n2 = (IntegerList)emptyNode;
                for (int i = stringValues.size() - 1; i >= startPos; --i) {
                    final String v = stringValues.get(i);
                    final NonEmptyIntegerList nn2 = (NonEmptyIntegerList)(n2 = n2.push(Integer.parseInt(v)));
                }
                return n2;
            }
            if (emptyNode instanceof FloatList) {
                FloatList n3 = (FloatList)emptyNode;
                for (int i = stringValues.size() - 1; i >= startPos; --i) {
                    final String v = stringValues.get(i);
                    final NonEmptyFloatList nn3 = (NonEmptyFloatList)(n3 = n3.push(Float.parseFloat(v)));
                }
                return n3;
            }
            StringList n4 = (StringList)emptyNode;
            for (int i = stringValues.size() - 1; i >= startPos; --i) {
                final String v = stringValues.get(i);
                final NonEmptyStringList nn4 = (NonEmptyStringList)(n4 = n4.push(v));
            }
            return n4;
        }
        
        private ByteArray createOrUpdateByteArray(final String hexString, final int xmiId, ByteArray existingArray) throws XCASParsingException {
            if (hexString == null) {
                return null;
            }
            if ((hexString.length() & 0x1) != 0x0) {
                throw this.createException(14);
            }
            final int arrayLen = hexString.length() / 2;
            ByteArray fs;
            if (existingArray != null) {
                if (arrayLen == 0) {
                    return (ByteArray)((existingArray.size() == 0) ? existingArray : this.casBeingFilled.createByteArrayFS(0));
                }
                fs = (ByteArray)((existingArray.size() == arrayLen) ? existingArray : this.casBeingFilled.createByteArrayFS(arrayLen));
            }
            else if (xmiId == -1 || this.isNewFS(xmiId)) {
                fs = (ByteArray)this.casBeingFilled.createByteArrayFS(arrayLen);
            }
            else {
                existingArray = (ByteArray)this.getFsForXmiId(xmiId);
                fs = (ByteArray)((existingArray.size() == arrayLen) ? existingArray : this.casBeingFilled.createByteArrayFS(arrayLen));
            }
            for (int i = 0; i < arrayLen; ++i) {
                final byte high = this.hexCharToByte(hexString.charAt(i * 2));
                final byte low = this.hexCharToByte(hexString.charAt(i * 2 + 1));
                final byte b = (byte)(high << 4 | low);
                fs.set(i, b);
            }
            this.addFsToXmiId(fs, xmiId);
            return fs;
        }
        
        private byte hexCharToByte(final char c) {
            if ('0' <= c && c <= '9') {
                return (byte)(c - '0');
            }
            if ('A' <= c && c <= 'F') {
                return (byte)(c - 'A' + 10);
            }
            if ('a' <= c && c <= 'f') {
                return (byte)(c - 'a' + 10);
            }
            throw new NumberFormatException("Invalid hex char: " + c);
        }
        
        @Override
        public void characters(final char[] chars, final int start, final int length) throws SAXException {
            switch (this.state) {
                case 3: {
                    this.buffer.append(chars, start, length);
                    break;
                }
            }
        }
        
        @Override
        public void endElement(final String nsURI, final String localName, final String qualifiedName) throws SAXException {
            switch (this.state) {
                case 1: {
                    this.state = 0;
                    break;
                }
                case 3: {
                    ArrayList<String> valueList = this.multiValuedFeatures.get(qualifiedName);
                    if (valueList == null) {
                        valueList = new ArrayList<String>();
                        this.multiValuedFeatures.put(qualifiedName, valueList);
                    }
                    valueList.add(this.buffer.toString());
                    this.state = 2;
                    break;
                }
                case 5: {
                    this.state = 2;
                    break;
                }
                case 2: {
                    if (this.outOfTypeSystemElement != null || this.deferredFsElement != null) {
                        if (!this.multiValuedFeatures.isEmpty()) {
                            for (final Map.Entry<String, ArrayList<String>> entry : this.multiValuedFeatures.entrySet()) {
                                final String featName = entry.getKey();
                                final ArrayList<String> featVals = entry.getValue();
                                XmiSerializationSharedData.addOutOfTypeSystemFeature((this.outOfTypeSystemElement == null) ? this.deferredFsElement : this.outOfTypeSystemElement, featName, featVals);
                            }
                        }
                        final XmiSerializationSharedData.OotsElementData ootsElementData = null;
                        this.deferredFsElement = ootsElementData;
                        this.outOfTypeSystemElement = ootsElementData;
                    }
                    else if (this.currentType != null) {
                        if (this.currentType.isArray() && this.currentType.getCode() != 29) {
                            if (this.currentArrayElements == null) {
                                this.currentArrayElements = this.multiValuedFeatures.get("elements");
                                if (this.currentArrayElements == null) {
                                    this.currentArrayElements = Collections.emptyList();
                                }
                            }
                            this.createOrUpdateArray(this.currentType, this.currentArrayElements, this.currentArrayId, null);
                        }
                        else if (!this.multiValuedFeatures.isEmpty()) {
                            for (final Map.Entry<String, ArrayList<String>> entry : this.multiValuedFeatures.entrySet()) {
                                final String featName = entry.getKey();
                                final ArrayList<String> featVals = entry.getValue();
                                final int featcode = this.handleFeatMultiValueFromName(this.currentType, this.currentFs, featName, featVals);
                                if (featcode != -1 && this.featsSeen != null) {
                                    this.featsSeen.add(featcode);
                                }
                            }
                        }
                        if (33 != this.currentType.getCode() && this.featsSeen != null) {
                            for (final FeatureImpl fi : this.currentType.getFeatureImpls()) {
                                if (!fi.getName().equals("uima.cas.AnnotationBase:sofa") && !this.featsSeen.contains(fi.getCode())) {
                                    CASImpl.setFeatureValueFromStringNoDocAnnotUpdate(this.currentFs, fi, null);
                                }
                            }
                            this.featsSeen = null;
                        }
                    }
                    this.state = 1;
                    break;
                }
                case 4: {
                    --this.ignoreDepth;
                    if (this.ignoreDepth == 0) {
                        this.state = 1;
                        break;
                    }
                    break;
                }
            }
        }
        
        @Override
        public void endDocument() throws SAXException {
            this.processDeferredFSs();
            for (final Runnable_withSaxException todo : this.fixupToDos) {
                todo.run();
            }
            for (final Map.Entry<FSIndexRepositoryImpl, List<TOP>> e : this.toBeAdded.entrySet()) {
                final FSIndexRepositoryImpl indexRep = e.getKey();
                final List<TOP> todo2 = e.getValue();
                for (final TOP fs : todo2) {
                    indexRep.addFS(fs);
                }
            }
            for (final Map.Entry<FSIndexRepositoryImpl, List<TOP>> e : this.toBeRemoved.entrySet()) {
                final FSIndexRepositoryImpl indexRep = e.getKey();
                final List<TOP> todo2 = e.getValue();
                for (final TOP fs : todo2) {
                    indexRep.removeFS(fs);
                }
            }
            for (final CAS view : this.views) {
                ((CASImpl)view).updateDocumentAnnotation();
            }
            if (this.disallowedViewMemberEncountered) {
                throw new CASRuntimeException("DELTA_CAS_PREEXISTING_FS_DISALLOWED", new Object[] { "Preexisting FS view member encountered." });
            }
            for (final Runnable r : this.uimaSerializableFixups) {
                r.run();
            }
        }
        
        private void finalizeRefValue(final int xmiId, final TOP fs, final FeatureImpl fi) throws XCASParsingException {
            final TOP tgtFs = this.maybeGetFsForXmiId(xmiId);
            if (null == tgtFs && xmiId != 0) {
                if (!this.lenient) {
                    throw this.createException(12, Integer.toString(xmiId));
                }
                this.sharedData.addOutOfTypeSystemAttribute(fs, fi.getShortName(), Integer.toString(xmiId));
                CASImpl.setFeatureValueMaybeSofa(fs, fi, null);
            }
            else {
                CASImpl.setFeatureValueMaybeSofa(fs, fi, tgtFs);
                XmiCasDeserializer.this.ts.fixupFSArrayTypes(fi.getRangeImpl(), tgtFs);
            }
        }
        
        private void finalizeFSListRefValue(final int xmiId, final NonEmptyFSList neNode) throws XCASParsingException {
            final TOP tgtFs = this.maybeGetFsForXmiId(xmiId);
            if (null == tgtFs && xmiId != 0) {
                if (!this.lenient) {
                    throw this.createException(12, Integer.toString(xmiId));
                }
                this.sharedData.addOutOfTypeSystemAttribute(neNode, "head", Integer.toString(xmiId));
                neNode.setHead(null);
            }
            else {
                neNode.setHead(tgtFs);
            }
        }
        
        private void finalizeFSArrayRefValue(final int xmiId, final FSArray fsArray, final int index) throws XCASParsingException {
            final TOP tgtFs = this.maybeGetFsForXmiId(xmiId);
            if (null == tgtFs && xmiId != 0) {
                if (!this.lenient) {
                    throw this.createException(12, Integer.toString(xmiId));
                }
                this.sharedData.addOutOfTypeSystemArrayElement(fsArray, index, xmiId);
                fsArray.set(index, null);
            }
            else {
                fsArray.set(index, tgtFs);
            }
        }
        
        private XCASParsingException createException(final int code) {
            final XCASParsingException e = new XCASParsingException(code);
            String source = "<unknown>";
            String line = "<unknown>";
            String col = "<unknown>";
            if (this.locator != null) {
                source = this.locator.getSystemId();
                if (source == null) {
                    source = this.locator.getPublicId();
                }
                if (source == null) {
                    source = "<unknown>";
                }
                line = Integer.toString(this.locator.getLineNumber());
                col = Integer.toString(this.locator.getColumnNumber());
            }
            e.addArgument(source);
            e.addArgument(line);
            e.addArgument(col);
            return e;
        }
        
        private XCASParsingException createException(final int code, final String arg) {
            final XCASParsingException e = this.createException(code);
            e.addArgument(arg);
            return e;
        }
        
        @Override
        public void error(final SAXParseException e) throws SAXException {
            throw e;
        }
        
        @Override
        public void fatalError(final SAXParseException e) throws SAXException {
            throw e;
        }
        
        @Override
        public void ignorableWhitespace(final char[] arg0, final int arg1, final int arg2) throws SAXException {
        }
        
        @Override
        public void setDocumentLocator(final Locator loc) {
            this.locator = loc;
        }
        
        @Override
        public void warning(final SAXParseException e) throws SAXException {
            throw e;
        }
        
        private void addFsToXmiId(final TOP fs, final int xmiId) {
            if (xmiId > 0) {
                if (this.mergePoint < 0) {
                    this.sharedData.addIdMapping(fs, xmiId);
                }
                else {
                    this.localXmiIdToFs.put(xmiId, fs);
                }
            }
        }
        
        private TOP getFsForXmiId(final int xmiId) {
            final TOP r = this.maybeGetFsForXmiId(xmiId);
            if (r == null) {
                throw new NoSuchElementException();
            }
            return r;
        }
        
        private TOP maybeGetFsForXmiId(final int xmiId) {
            if (this.mergePoint >= 0 && this.isNewFS(xmiId)) {
                return this.localXmiIdToFs.get(xmiId);
            }
            final TOP fs = this.sharedData.getFsForXmiId(xmiId);
            if (fs != null) {
                return fs;
            }
            return null;
        }
        
        private void addToOutOfTypeSystemData(final XmlElementName xmlElementName, final Attributes attrs) throws XCASParsingException {
            final String xmiId = attrs.getValue("xmi:id");
            this.outOfTypeSystemElement = new XmiSerializationSharedData.OotsElementData(xmiId, xmlElementName);
            XmiCasDeserializer.this.addOutOfTypeSystemAttributes(this.outOfTypeSystemElement, attrs);
            this.sharedData.addOutOfTypeSystemElement(this.outOfTypeSystemElement);
        }
        
        private boolean isNewFS(final int id) {
            return id > this.mergePoint;
        }
        
        private void addNonsharedFSToEncompassingFSMapping(final TOP nonsharedFS, final TOP encompassingFS) {
            this.sharedData.addNonsharedRefToFSMapping(nonsharedFS, encompassingFS);
        }
        
        private void processDeferredFSs() throws SAXException {
            if (null == this.deferredFSs) {
                return;
            }
            this.processingDeferredFSs = true;
            final List<XmiSerializationSharedData.OotsElementData> localDeferredFSs = this.deferredFSs;
            this.deferredFSs = null;
            for (final XmiSerializationSharedData.OotsElementData deferredFs : localDeferredFSs) {
                final List<XmlAttribute> attrs = deferredFs.attributes;
                for (final XmlElementNameAndContents childElement : deferredFs.childElements) {
                    if (childElement.name.qName.equals("sofa")) {
                        attrs.add(new XmlAttribute("sofa", childElement.contents));
                        break;
                    }
                }
                attrs.add(new XmlAttribute("xmi:id", deferredFs.xmiId));
                this.readFS(deferredFs.elementName.nsUri, deferredFs.elementName.localName, deferredFs.elementName.qName, deferredFs.getAttributes());
                try {
                    this.isDoingDeferredChildElements = true;
                    for (final XmiSerializationSharedData.NameMultiValue nmv : deferredFs.multiValuedFeatures) {
                        final int featcode = this.handleFeatMultiValueFromName(this.currentType, this.currentFs, nmv.name, nmv.values);
                        if (featcode != -1 && this.featsSeen != null) {
                            this.featsSeen.add(featcode);
                        }
                    }
                }
                finally {
                    this.isDoingDeferredChildElements = false;
                }
            }
        }
    }
}
