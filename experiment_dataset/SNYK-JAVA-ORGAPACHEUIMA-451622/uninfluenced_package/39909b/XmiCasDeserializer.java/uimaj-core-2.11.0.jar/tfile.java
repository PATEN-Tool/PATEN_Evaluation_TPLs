// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.uima.cas.impl;

import org.apache.uima.internal.util.IntListIterator;
import java.util.Collections;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.internal.util.I18nUtil;
import java.util.Locale;
import org.apache.uima.cas.FeatureStructure;
import org.xml.sax.SAXParseException;
import org.apache.uima.internal.util.PositiveIntSet;
import java.util.NoSuchElementException;
import java.util.Arrays;
import org.apache.uima.cas.Type;
import org.apache.uima.internal.util.XmlElementNameAndContents;
import org.apache.uima.internal.util.XmlAttribute;
import org.apache.uima.internal.util.XmlElementName;
import org.apache.uima.cas.CASRuntimeException;
import org.xml.sax.Attributes;
import org.apache.uima.cas.Feature;
import java.util.Iterator;
import org.apache.uima.cas.FSIterator;
import org.xml.sax.ErrorHandler;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.SofaFS;
import java.util.ArrayList;
import java.util.TreeMap;
import org.apache.uima.internal.util.rb_trees.IntRedBlackTree;
import org.apache.uima.cas.FSIndexRepository;
import java.util.List;
import org.apache.uima.internal.util.IntVector;
import org.xml.sax.Locator;
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

public class XmiCasDeserializer
{
    private static final boolean IS_NEW_FS = true;
    private static final boolean IS_EXISTING_FS = false;
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
        return new XmiCasDeserializerHandler((CASImpl)cas, lenient, (XmiSerializationSharedData)null, -1, AllowPreexistingFS.ignore);
    }
    
    public DefaultHandler getXmiCasHandler(final CAS cas, final boolean lenient, final XmiSerializationSharedData sharedData) {
        return new XmiCasDeserializerHandler((CASImpl)cas, lenient, sharedData, -1, AllowPreexistingFS.ignore);
    }
    
    public DefaultHandler getXmiCasHandler(final CAS cas, final boolean lenient, final XmiSerializationSharedData sharedData, final int mergePoint) {
        return new XmiCasDeserializerHandler((CASImpl)cas, lenient, sharedData, mergePoint, AllowPreexistingFS.ignore);
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
    
    public class XmiCasDeserializerHandler extends DefaultHandler
    {
        private static final int DOC_STATE = 0;
        private static final int FS_STATE = 1;
        private static final int FEAT_STATE = 2;
        private static final int FEAT_CONTENT_STATE = 3;
        private static final int IGNORING_XMI_ELEMENTS_STATE = 4;
        private static final int REF_FEAT_STATE = 5;
        private static final String unknownXMLSource = "<unknown>";
        private String ID_ATTR_NAME;
        private Locator locator;
        private CASImpl casBeingFilled;
        private IntVector deserializedFsAddrs;
        private IntVector fsListNodesFromMultivaluedProperties;
        private int state;
        private StringBuffer buffer;
        private int currentAddr;
        private TypeImpl currentType;
        private int currentArrayId;
        private List<String> currentArrayElements;
        private Map<String, List<String>> multiValuedFeatures;
        private int sofaTypeCode;
        private int sofaNumFeatCode;
        private int sofaFeatCode;
        private List<FSIndexRepository> indexRepositories;
        private List<CAS> views;
        private ListUtils listUtils;
        private int[] featureType;
        boolean lenient;
        private int ignoreDepth;
        private Map<String, String> nsPrefixToUriMap;
        private XmiSerializationSharedData sharedData;
        private int nextSofaNum;
        private int mergePoint;
        private XmiSerializationSharedData.OotsElementData outOfTypeSystemElement;
        private IntRedBlackTree localXmiIdToFsAddrMap;
        AllowPreexistingFS allowPreexistingFS;
        IntVector featsSeen;
        boolean disallowedViewMemberEncountered;
        private final DeferredIndexUpdates toBeAdded;
        private final DeferredIndexUpdates toBeRemoved;
        
        private XmiCasDeserializerHandler(final CASImpl aCAS, final boolean lenient, final XmiSerializationSharedData sharedData, final int mergePoint, final AllowPreexistingFS allowPreexistingFS) {
            this.ID_ATTR_NAME = "xmi:id";
            this.multiValuedFeatures = new TreeMap<String, List<String>>();
            this.ignoreDepth = 0;
            this.nsPrefixToUriMap = new HashMap<String, String>();
            this.outOfTypeSystemElement = null;
            this.localXmiIdToFsAddrMap = new IntRedBlackTree();
            this.featsSeen = null;
            this.toBeAdded = new DeferredIndexUpdates();
            this.toBeRemoved = new DeferredIndexUpdates();
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
                this.nextSofaNum = this.casBeingFilled.getBaseSofaCount() + 1;
            }
            this.deserializedFsAddrs = new IntVector();
            this.fsListNodesFromMultivaluedProperties = new IntVector();
            this.buffer = new StringBuffer();
            this.indexRepositories = new ArrayList<FSIndexRepository>();
            this.views = new ArrayList<CAS>();
            this.indexRepositories.add(this.casBeingFilled.getBaseIndexRepository());
            this.indexRepositories.add(this.casBeingFilled.getView("_InitialView").getIndexRepository());
            final FSIterator<SofaFS> sofaIter = this.casBeingFilled.getSofaIterator();
            while (sofaIter.hasNext()) {
                final SofaFS sofa = sofaIter.next();
                if (sofa.getSofaRef() == 1) {
                    this.casBeingFilled.registerInitialSofa();
                }
                else {
                    this.indexRepositories.add(this.casBeingFilled.getSofaIndexRepository(sofa));
                }
            }
            final TypeSystemImpl tsOfReceivingCas = this.casBeingFilled.getTypeSystemImpl();
            this.sofaTypeCode = tsOfReceivingCas.ll_getCodeForTypeName("uima.cas.Sofa");
            this.sofaNumFeatCode = tsOfReceivingCas.ll_getCodeForFeatureName("uima.cas.Sofa:sofaNum");
            this.sofaFeatCode = tsOfReceivingCas.ll_getCodeForFeatureName("uima.cas.AnnotationBase:sofa");
            this.listUtils = new ListUtils(this.casBeingFilled, UIMAFramework.getLogger(XmiCasDeserializer.class), null);
            this.featureType = new int[tsOfReceivingCas.getNumberOfFeatures() + 1];
            final Iterator<Feature> it = tsOfReceivingCas.getFeatures();
            while (it.hasNext()) {
                final FeatureImpl feat = it.next();
                this.featureType[feat.getCode()] = this.classifyType(tsOfReceivingCas.range(feat.getCode()));
            }
        }
        
        private final void resetBuffer() {
            this.buffer = new StringBuffer();
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
                        final String id = attrs.getValue(this.ID_ATTR_NAME);
                        if (id != null) {
                            final int idInt = Integer.parseInt(id);
                            if (idInt > 0 && !this.isNewFS(idInt)) {
                                if (this.allowPreexistingFS == AllowPreexistingFS.ignore) {
                                    this.state = 4;
                                    ++this.ignoreDepth;
                                    return;
                                }
                                if (this.allowPreexistingFS == AllowPreexistingFS.disallow) {
                                    final CASRuntimeException e = new CASRuntimeException("DELTA_CAS_PREEXISTING_FS_DISALLOWED", new String[] { this.ID_ATTR_NAME + "=" + id, nameSpaceURI, localName, qualifiedName });
                                    throw e;
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
                            List<String> valueList = this.multiValuedFeatures.get(qualifiedName);
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
            this.currentType = (TypeImpl)XmiCasDeserializer.this.ts.getType(typeName);
            if (this.currentType != null) {
                if (this.casBeingFilled.isArrayType(this.currentType)) {
                    final String idStr = attrs.getValue(this.ID_ATTR_NAME);
                    this.currentArrayId = ((idStr == null) ? -1 : Integer.parseInt(idStr));
                    final String elements = attrs.getValue("elements");
                    if (this.casBeingFilled.isByteArrayType(this.currentType)) {
                        this.createByteArray(elements, this.currentArrayId, 0);
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
                    final String idStr = attrs.getValue(this.ID_ATTR_NAME);
                    final int xmiId = (idStr == null) ? -1 : Integer.parseInt(idStr);
                    if (this.isNewFS(xmiId)) {
                        final int addr = this.casBeingFilled.ll_createFS(this.currentType.getCode());
                        this.readFS(addr, attrs, true);
                    }
                    else {
                        if (this.allowPreexistingFS == AllowPreexistingFS.disallow) {
                            final CASRuntimeException e = new CASRuntimeException("DELTA_CAS_PREEXISTING_FS_DISALLOWED", new String[] { this.ID_ATTR_NAME + "=" + idStr, nameSpaceURI, localName, qualifiedName });
                            throw e;
                        }
                        if (this.allowPreexistingFS == AllowPreexistingFS.allow) {
                            final int addr = this.getFsAddrForXmiId(xmiId);
                            this.readFS(addr, attrs, false);
                        }
                    }
                }
                return;
            }
            if ("uima.cas.NULL".equals(typeName)) {
                return;
            }
            if ("uima.cas.View".equals(typeName)) {
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
        
        private void processView(final String sofa, final String membersString) throws SAXParseException {
            if (membersString != null) {
                final int sofaXmiId = (sofa == null) ? 1 : Integer.parseInt(sofa);
                final FSIndexRepositoryImpl indexRep = this.getIndexRepo(sofa, sofaXmiId);
                final boolean newview = sofa != null && this.isNewFS(sofaXmiId);
                final PositiveIntSet todo = this.toBeAdded.getTodos(indexRep);
                final String[] members = this.parseArray(membersString);
                for (int i = 0; i < members.length; ++i) {
                    final int id = Integer.parseInt(members[i]);
                    if (!newview && !this.isNewFS(id)) {
                        if (this.allowPreexistingFS == AllowPreexistingFS.ignore) {
                            continue;
                        }
                        if (this.allowPreexistingFS == AllowPreexistingFS.disallow) {
                            this.disallowedViewMemberEncountered = true;
                            continue;
                        }
                    }
                    try {
                        final int addr = this.getFsAddrForXmiId(id);
                        todo.add(addr);
                    }
                    catch (NoSuchElementException e) {
                        if (!this.lenient) {
                            throw this.createException(12, Integer.toString(id));
                        }
                        this.sharedData.addOutOfTypeSystemViewMember(sofa, members[i]);
                    }
                }
            }
        }
        
        private FSIndexRepositoryImpl getIndexRepo(final String sofa, final int sofaXmiId) throws XCASParsingException {
            if (sofa == null) {
                return this.indexRepositories.get(1);
            }
            int sofaAddr;
            try {
                sofaAddr = this.getFsAddrForXmiId(sofaXmiId);
            }
            catch (NoSuchElementException e) {
                throw this.createException(12, Integer.toString(sofaXmiId));
            }
            final int sofaNum = this.casBeingFilled.getFeatureValue(sofaAddr, this.sofaNumFeatCode);
            return this.indexRepositories.get(sofaNum);
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
                final PositiveIntSet localRemoves = this.toBeRemoved.getTodos(indexRep);
                final String[] members = this.parseArray(delmemberString);
                for (int i = 0; i < members.length; ++i) {
                    final int id = Integer.parseInt(members[i]);
                    if (!this.isNewFS(id)) {
                        if (this.allowPreexistingFS == AllowPreexistingFS.disallow) {
                            this.disallowedViewMemberEncountered = true;
                            continue;
                        }
                        if (this.allowPreexistingFS == AllowPreexistingFS.ignore) {
                            continue;
                        }
                    }
                    try {
                        final int addr = this.getFsAddrForXmiId(id);
                        localRemoves.add(addr);
                    }
                    catch (NoSuchElementException e) {
                        if (!this.lenient) {
                            throw this.createException(12, Integer.toString(id));
                        }
                        this.sharedData.addOutOfTypeSystemViewMember(sofa, members[i]);
                    }
                }
            }
        }
        
        private void readFS(final int fsAddr, final Attributes attrs, final boolean isNewFs) throws SAXException {
            this.currentAddr = fsAddr;
            int id = -1;
            final int typeCode = this.casBeingFilled.getHeapValue(fsAddr);
            final Type type = this.casBeingFilled.getTypeSystemImpl().ll_getTypeForCode(typeCode);
            int thisSofaNum = 0;
            if (this.sofaTypeCode == typeCode) {
                final String sofaID = attrs.getValue("sofaID");
                if (sofaID.equals("_InitialView") || sofaID.equals("_DefaultTextSofaName")) {
                    thisSofaNum = 1;
                }
                else if (isNewFs) {
                    thisSofaNum = this.nextSofaNum++;
                }
                else {
                    thisSofaNum = Integer.parseInt(attrs.getValue("sofaNum"));
                }
            }
            this.featsSeen = null;
            try {
                if (!isNewFs) {
                    this.casBeingFilled.removeFromCorruptableIndexAnyViewSetCache(fsAddr, this.casBeingFilled.getAddbackSingle());
                }
                for (int i = 0; i < attrs.getLength(); ++i) {
                    final String attrName = attrs.getQName(i);
                    String attrValue = attrs.getValue(i);
                    if (attrName.equals(this.ID_ATTR_NAME)) {
                        try {
                            id = Integer.parseInt(attrValue);
                            if (this.sofaTypeCode != typeCode && !isNewFs) {
                                this.featsSeen = new IntVector(attrs.getLength());
                            }
                            else {
                                this.featsSeen = null;
                            }
                            continue;
                        }
                        catch (NumberFormatException e) {
                            throw this.createException(5, attrValue);
                        }
                    }
                    if (this.sofaTypeCode == typeCode && attrName.equals("sofaID")) {
                        if (attrValue.equals("_DefaultTextSofaName")) {
                            attrValue = "_InitialView";
                        }
                    }
                    else if (this.sofaTypeCode == typeCode && attrName.equals("sofaNum")) {
                        attrValue = Integer.toString(thisSofaNum);
                    }
                    final int featCode = this.handleFeature(type, fsAddr, attrName, attrValue, isNewFs);
                    if (this.featsSeen != null && !isNewFs && featCode != -1) {
                        this.featsSeen.add(featCode);
                    }
                }
            }
            finally {
                if (!isNewFs) {
                    this.casBeingFilled.addbackSingle(fsAddr);
                }
            }
            if (this.sofaTypeCode == typeCode && isNewFs) {
                final SofaFS sofa = this.casBeingFilled.createFS(fsAddr);
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
            this.deserializedFsAddrs.add(fsAddr);
            this.addFsAddrXmiIdMapping(fsAddr, id);
        }
        
        private final boolean emptyVal(final String val) {
            return val == null || val.length() == 0;
        }
        
        private int handleFeature(final Type type, final int fsAddr, final String featName, final String featVal, final boolean isNewFS) throws SAXException {
            final FeatureImpl feat = (FeatureImpl)type.getFeatureByBaseName(featName);
            if (feat != null) {
                if (this.sofaTypeCode == this.casBeingFilled.getHeapValue(fsAddr) && !isNewFS) {
                    if (featName.equals("sofaID") || featName.equals("sofaNum")) {
                        return feat.getCode();
                    }
                    if (featName.equals("sofaString") || featName.equals("sofaURI") || featName.equals("sofaArray")) {
                        final int currVal = this.casBeingFilled.getFeatureValue(fsAddr, feat.getCode());
                        if (currVal != 0) {
                            return feat.getCode();
                        }
                    }
                }
                this.handleFeature(fsAddr, feat.getCode(), featVal);
                return feat.getCode();
            }
            if (!this.lenient) {
                throw this.createException(8, featName);
            }
            this.sharedData.addOutOfTypeSystemAttribute(fsAddr, featName, featVal);
            return -1;
        }
        
        private int handleFeature(final Type type, final int addr, final String featName, final List<String> featVals) throws SAXException {
            final FeatureImpl feat = (FeatureImpl)type.getFeatureByBaseName(featName);
            if (feat != null) {
                this.handleFeature(addr, feat.getCode(), featVals);
                return feat.getCode();
            }
            if (!this.lenient) {
                throw this.createException(8, featName);
            }
            this.sharedData.addOutOfTypeSystemChildElements(addr, featName, featVals);
            return -1;
        }
        
        private void handleFeature(final int addr, final int featCode, final String featVal) throws SAXException {
            switch (this.featureType[featCode]) {
                case 1: {
                    try {
                        if (!this.emptyVal(featVal)) {
                            if (featCode == this.sofaFeatCode) {
                                final int sofaXmiId = Integer.parseInt(featVal);
                                final int sofaAddr = this.getFsAddrForXmiId(sofaXmiId);
                                final int sofaNum = this.casBeingFilled.getFeatureValue(sofaAddr, this.sofaNumFeatCode);
                                this.casBeingFilled.setFeatureValue(addr, featCode, sofaNum);
                            }
                            else {
                                this.casBeingFilled.setFeatureValue(addr, featCode, Integer.parseInt(featVal));
                            }
                        }
                        break;
                    }
                    catch (NumberFormatException e) {
                        throw this.createException(9, featVal);
                    }
                }
                case 2:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13: {
                    try {
                        if (!this.emptyVal(featVal)) {
                            this.casBeingFilled.setFeatureValueFromString(addr, featCode, featVal);
                        }
                        break;
                    }
                    catch (NumberFormatException e) {
                        throw this.createException(10, featVal);
                    }
                }
                case 3: {
                    if (featVal != null) {
                        final String origValue = this.casBeingFilled.getStringValue(addr, featCode);
                        if (origValue == null || !featVal.equals(origValue)) {
                            this.casBeingFilled.setStringValue(addr, featCode, featVal);
                        }
                        break;
                    }
                    break;
                }
                case 8: {
                    try {
                        if (!this.emptyVal(featVal)) {
                            this.casBeingFilled.setFeatureValue(addr, featCode, Integer.parseInt(featVal));
                        }
                        break;
                    }
                    catch (NumberFormatException e) {
                        throw this.createException(9, featVal);
                    }
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
                    if (XmiCasDeserializer.this.ts.ll_getFeatureForCode(featCode).isMultipleReferencesAllowed()) {
                        try {
                            if (!this.emptyVal(featVal)) {
                                this.casBeingFilled.setFeatureValue(addr, featCode, Integer.parseInt(featVal));
                            }
                            break;
                        }
                        catch (NumberFormatException e) {
                            throw this.createException(9, featVal);
                        }
                    }
                    if (this.featureType[featCode] == 15) {
                        final int currFeatVal = this.casBeingFilled.getFeatureValue(addr, featCode);
                        int casArray = 0;
                        casArray = this.createByteArray(featVal, -1, currFeatVal);
                        if (casArray != currFeatVal) {
                            this.casBeingFilled.setFeatureValue(addr, featCode, casArray);
                        }
                        break;
                    }
                    final String[] arrayVals = this.parseArray(featVal);
                    this.handleFeature(addr, featCode, Arrays.asList(arrayVals));
                    break;
                }
                case 101:
                case 102:
                case 103:
                case 104: {
                    if (XmiCasDeserializer.this.ts.ll_getFeatureForCode(featCode).isMultipleReferencesAllowed()) {
                        try {
                            if (!this.emptyVal(featVal)) {
                                this.casBeingFilled.setFeatureValue(addr, featCode, Integer.parseInt(featVal));
                            }
                            break;
                        }
                        catch (NumberFormatException e) {
                            throw this.createException(9, featVal);
                        }
                    }
                    final String[] arrayVals = this.parseArray(featVal);
                    this.handleFeature(addr, featCode, Arrays.asList(arrayVals));
                    break;
                }
                default: {
                    assert false;
                    break;
                }
            }
        }
        
        private String[] parseArray(String val) {
            val = val.trim();
            String[] arrayVals;
            if (this.emptyVal(val)) {
                arrayVals = new String[0];
            }
            else {
                arrayVals = val.split("\\s+");
            }
            return arrayVals;
        }
        
        private void handleFeature(final int addr, final int featCode, final List<String> featVals) throws SAXException {
            switch (this.featureType[featCode]) {
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
                        throw new SAXParseException(I18nUtil.localizeMessage("org.apache.uima.UIMAException_Messages", Locale.getDefault(), "multiple_values_unexpected", new Object[] { XmiCasDeserializer.this.ts.ll_getFeatureForCode(featCode).getName() }), this.locator);
                    }
                    this.handleFeature(addr, featCode, featVals.get(0));
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
                    int casArray = 0;
                    final int currVal = this.casBeingFilled.getFeatureValue(addr, featCode);
                    casArray = this.createArray(this.casBeingFilled.getTypeSystemImpl().range(featCode), featVals, -1, currVal);
                    if (currVal != casArray) {
                        this.casBeingFilled.setFeatureValue(addr, featCode, casArray);
                    }
                    if (!XmiCasDeserializer.this.ts.ll_getFeatureForCode(featCode).isMultipleReferencesAllowed()) {
                        this.addNonsharedFSToEncompassingFSMapping(casArray, addr);
                        break;
                    }
                    break;
                }
                case 101: {
                    int listFS = this.casBeingFilled.getFeatureValue(addr, featCode);
                    if (listFS == 0) {
                        listFS = this.listUtils.createIntList(featVals);
                        this.casBeingFilled.setFeatureValue(addr, featCode, listFS);
                    }
                    else {
                        this.listUtils.updateIntList(listFS, featVals);
                    }
                    if (!XmiCasDeserializer.this.ts.ll_getFeatureForCode(featCode).isMultipleReferencesAllowed()) {
                        this.addNonsharedFSToEncompassingFSMapping(listFS, addr);
                        break;
                    }
                    break;
                }
                case 102: {
                    int listFS = this.casBeingFilled.getFeatureValue(addr, featCode);
                    if (listFS == 0) {
                        listFS = this.listUtils.createFloatList(featVals);
                        this.casBeingFilled.setFeatureValue(addr, featCode, listFS);
                    }
                    else {
                        this.listUtils.updateFloatList(listFS, featVals);
                    }
                    if (!XmiCasDeserializer.this.ts.ll_getFeatureForCode(featCode).isMultipleReferencesAllowed()) {
                        this.addNonsharedFSToEncompassingFSMapping(listFS, addr);
                        break;
                    }
                    break;
                }
                case 103: {
                    int listFS = this.casBeingFilled.getFeatureValue(addr, featCode);
                    if (listFS == 0) {
                        listFS = this.listUtils.createStringList(featVals);
                        this.casBeingFilled.setFeatureValue(addr, featCode, listFS);
                    }
                    else {
                        this.listUtils.updateStringList(listFS, featVals);
                    }
                    if (!XmiCasDeserializer.this.ts.ll_getFeatureForCode(featCode).isMultipleReferencesAllowed()) {
                        this.addNonsharedFSToEncompassingFSMapping(listFS, addr);
                        break;
                    }
                    break;
                }
                case 104: {
                    int listFS = this.casBeingFilled.getFeatureValue(addr, featCode);
                    final IntVector fslistnodes = new IntVector();
                    if (listFS == 0) {
                        listFS = this.listUtils.createFsList(featVals, fslistnodes);
                        this.casBeingFilled.setFeatureValue(addr, featCode, listFS);
                    }
                    else {
                        this.listUtils.updateFsList(listFS, featVals, fslistnodes);
                    }
                    for (int i = 0; i < fslistnodes.size(); ++i) {
                        this.fsListNodesFromMultivaluedProperties.add(fslistnodes.get(i));
                    }
                    if (!XmiCasDeserializer.this.ts.ll_getFeatureForCode(featCode).isMultipleReferencesAllowed()) {
                        for (int i = 0; i < fslistnodes.size(); ++i) {
                            this.addNonsharedFSToEncompassingFSMapping(fslistnodes.get(i), addr);
                        }
                        break;
                    }
                    break;
                }
                default: {
                    assert false;
                    break;
                }
            }
        }
        
        private int createArray(final int arrayType, final List<String> values, final int xmiId, final int addr) {
            int casArray = -1;
            if (addr > 0) {
                if (values.size() == this.casBeingFilled.getLowLevelCAS().ll_getArraySize(addr)) {
                    casArray = addr;
                    this.updateExistingArray(arrayType, values, casArray);
                }
                else {
                    casArray = this.createNewArray(arrayType, values);
                }
            }
            else if (xmiId == -1) {
                casArray = this.createNewArray(arrayType, values);
            }
            else if (this.isNewFS(xmiId)) {
                casArray = this.createNewArray(arrayType, values);
            }
            else {
                casArray = this.getFsAddrForXmiId(xmiId);
                if (values.size() == this.casBeingFilled.getLowLevelCAS().ll_getArraySize(casArray)) {
                    this.updateExistingArray(arrayType, values, casArray);
                }
                else {
                    casArray = this.createNewArray(arrayType, values);
                }
            }
            this.deserializedFsAddrs.add(casArray);
            this.addFsAddrXmiIdMapping(casArray, xmiId);
            return casArray;
        }
        
        private int createNewArray(final int arrayType, final List<String> values) {
            int casArray = -1;
            FeatureStructureImpl fs;
            if (this.casBeingFilled.isIntArrayType(arrayType)) {
                fs = (FeatureStructureImpl)this.casBeingFilled.createIntArrayFS(values.size());
            }
            else if (this.casBeingFilled.isFloatArrayType(arrayType)) {
                fs = (FeatureStructureImpl)this.casBeingFilled.createFloatArrayFS(values.size());
            }
            else if (this.casBeingFilled.isStringArrayType(arrayType)) {
                fs = (FeatureStructureImpl)this.casBeingFilled.createStringArrayFS(values.size());
            }
            else if (this.casBeingFilled.isBooleanArrayType(arrayType)) {
                fs = (FeatureStructureImpl)this.casBeingFilled.createBooleanArrayFS(values.size());
            }
            else if (this.casBeingFilled.isByteArrayType(arrayType)) {
                fs = (FeatureStructureImpl)this.casBeingFilled.createByteArrayFS(values.size());
            }
            else if (this.casBeingFilled.isShortArrayType(arrayType)) {
                fs = (FeatureStructureImpl)this.casBeingFilled.createShortArrayFS(values.size());
            }
            else if (this.casBeingFilled.isLongArrayType(arrayType)) {
                fs = (FeatureStructureImpl)this.casBeingFilled.createLongArrayFS(values.size());
            }
            else if (this.casBeingFilled.isDoubleArrayType(arrayType)) {
                fs = (FeatureStructureImpl)this.casBeingFilled.createDoubleArrayFS(values.size());
            }
            else {
                fs = (FeatureStructureImpl)this.casBeingFilled.createArrayFS(values.size());
            }
            casArray = fs.getAddress();
            for (int i = 0; i < values.size(); ++i) {
                final String stringVal = values.get(i);
                this.casBeingFilled.setArrayValueFromString(casArray, i, stringVal);
            }
            return casArray;
        }
        
        private void updateExistingArray(final int arrayType, final List<String> values, final int casArray) {
            for (int i = 0; i < values.size(); ++i) {
                final String stringVal = values.get(i);
                if (this.casBeingFilled.isStringArrayType(arrayType)) {
                    final String currVal = this.casBeingFilled.getLowLevelCAS().ll_getStringArrayValue(casArray, i);
                    if (currVal != null && currVal.equals(stringVal)) {
                        continue;
                    }
                }
                this.casBeingFilled.setArrayValueFromString(casArray, i, stringVal);
            }
        }
        
        private int createByteArray(final String hexString, final int xmiId, int addr) {
            final int arrayLen = hexString.length() / 2;
            ByteArrayFS fs = null;
            if (addr > 0) {
                fs = this.casBeingFilled.createFS(addr);
                if (fs.size() != arrayLen) {
                    fs = this.casBeingFilled.createByteArrayFS(arrayLen);
                }
            }
            else if (xmiId == -1) {
                fs = this.casBeingFilled.createByteArrayFS(arrayLen);
            }
            else if (this.isNewFS(xmiId)) {
                fs = this.casBeingFilled.createByteArrayFS(arrayLen);
            }
            else {
                addr = this.getFsAddrForXmiId(xmiId);
                fs = this.casBeingFilled.createFS(addr);
                if (fs.size() != arrayLen) {
                    fs = this.casBeingFilled.createByteArrayFS(arrayLen);
                }
            }
            for (int i = 0; i < arrayLen; ++i) {
                final byte high = this.hexCharToByte(hexString.charAt(i * 2));
                final byte low = this.hexCharToByte(hexString.charAt(i * 2 + 1));
                final byte b = (byte)(high << 4 | low);
                fs.set(i, b);
            }
            final int arrayAddr = ((FeatureStructureImpl)fs).getAddress();
            this.deserializedFsAddrs.add(arrayAddr);
            this.addFsAddrXmiIdMapping(arrayAddr, xmiId);
            return arrayAddr;
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
        
        boolean isAllWhitespace(final StringBuffer b) {
            for (int len = b.length(), i = 0; i < len; ++i) {
                if (!Character.isWhitespace(b.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
        
        @Override
        public void endElement(final String nsURI, final String localName, final String qualifiedName) throws SAXException {
            switch (this.state) {
                case 1: {
                    this.state = 0;
                    break;
                }
                case 3: {
                    List<String> valueList = this.multiValuedFeatures.get(qualifiedName);
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
                    if (this.outOfTypeSystemElement != null) {
                        if (!this.multiValuedFeatures.isEmpty()) {
                            for (final Map.Entry<String, List<String>> entry : this.multiValuedFeatures.entrySet()) {
                                final String featName = entry.getKey();
                                final List<String> featVals = entry.getValue();
                                this.addOutOfTypeSystemFeature(this.outOfTypeSystemElement, featName, featVals);
                            }
                        }
                        this.outOfTypeSystemElement = null;
                    }
                    else if (this.currentType != null) {
                        if (this.casBeingFilled.isArrayType(this.currentType) && !this.casBeingFilled.isByteArrayType(this.currentType)) {
                            if (this.currentArrayElements == null) {
                                this.currentArrayElements = this.multiValuedFeatures.get("elements");
                                if (this.currentArrayElements == null) {
                                    this.currentArrayElements = Collections.emptyList();
                                }
                            }
                            this.createArray(this.currentType.getCode(), this.currentArrayElements, this.currentArrayId, 0);
                        }
                        else if (!this.multiValuedFeatures.isEmpty()) {
                            for (final Map.Entry<String, List<String>> entry : this.multiValuedFeatures.entrySet()) {
                                final String featName = entry.getKey();
                                final List<String> featVals = entry.getValue();
                                final int featcode = this.handleFeature(this.currentType, this.currentAddr, featName, featVals);
                                if (featcode != -1 && this.featsSeen != null) {
                                    this.featsSeen.add(featcode);
                                }
                            }
                        }
                        if (this.sofaTypeCode != this.currentType.getCode() && this.featsSeen != null) {
                            final int[] feats = this.casBeingFilled.getTypeSystemImpl().ll_getAppropriateFeatures(this.currentType.getCode());
                            for (int i = 0; i < feats.length; ++i) {
                                if (!this.featsSeen.contains(feats[i])) {
                                    this.casBeingFilled.setFeatureValue(this.currentAddr, feats[i], 0);
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
            for (int i = 0; i < this.deserializedFsAddrs.size(); ++i) {
                this.finalizeFS(this.deserializedFsAddrs.get(i));
            }
            for (final Map.Entry<FSIndexRepositoryImpl, PositiveIntSet> e : this.toBeAdded.entrySet()) {
                final FSIndexRepositoryImpl indexRep = e.getKey();
                final PositiveIntSet todo = e.getValue();
                final IntListIterator it = todo.iterator();
                while (it.hasNext()) {
                    indexRep.addFS(it.next());
                }
            }
            for (final Map.Entry<FSIndexRepositoryImpl, PositiveIntSet> e : this.toBeRemoved.entrySet()) {
                final FSIndexRepositoryImpl indexRep = e.getKey();
                final PositiveIntSet todo = e.getValue();
                final IntListIterator it = todo.iterator();
                while (it.hasNext()) {
                    indexRep.removeFS(it.next());
                }
            }
            for (int i = 0; i < this.fsListNodesFromMultivaluedProperties.size(); ++i) {
                this.remapFSListHeads(this.fsListNodesFromMultivaluedProperties.get(i));
            }
            for (final CAS view : this.views) {
                ((CASImpl)view).updateDocumentAnnotation();
            }
            if (this.disallowedViewMemberEncountered) {
                final CASRuntimeException e2 = new CASRuntimeException("DELTA_CAS_PREEXISTING_FS_DISALLOWED", new String[] { "Preexisting FS view member encountered." });
                throw e2;
            }
        }
        
        private void finalizeFS(final int addr) throws SAXParseException {
            final int type = this.casBeingFilled.getHeapValue(addr);
            if (this.casBeingFilled.isArrayType(type)) {
                this.finalizeArray(type, addr);
                return;
            }
            final int[] feats = this.casBeingFilled.getTypeSystemImpl().ll_getAppropriateFeatures(type);
            for (int i = 0; i < feats.length; ++i) {
                final Feature feat = XmiCasDeserializer.this.ts.ll_getFeatureForCode(feats[i]);
                final int typeCode = XmiCasDeserializer.this.ts.ll_getRangeType(feats[i]);
                if (this.casBeingFilled.ll_isRefType(typeCode) && (this.featureType[feats[i]] == 8 || feat.isMultipleReferencesAllowed())) {
                    final int featVal = this.casBeingFilled.getFeatureValue(addr, feats[i]);
                    if (featVal != 0) {
                        int fsValAddr = 0;
                        try {
                            fsValAddr = this.getFsAddrForXmiId(featVal);
                        }
                        catch (NoSuchElementException e) {
                            if (!this.lenient) {
                                throw this.createException(12, Integer.toString(featVal));
                            }
                            this.sharedData.addOutOfTypeSystemAttribute(addr, feat.getShortName(), Integer.toString(featVal));
                        }
                        this.casBeingFilled.setFeatureValue(addr, feats[i], fsValAddr);
                    }
                }
            }
        }
        
        private void remapFSListHeads(final int addr) throws SAXParseException {
            final int type = this.casBeingFilled.getHeapValue(addr);
            if (!this.listUtils.isFsListType(type)) {
                return;
            }
            final int[] feats = this.casBeingFilled.getTypeSystemImpl().ll_getAppropriateFeatures(type);
            if (feats.length == 0) {
                return;
            }
            final int headFeat = feats[0];
            final int featVal = this.casBeingFilled.getFeatureValue(addr, headFeat);
            if (featVal != 0) {
                int fsValAddr = 0;
                try {
                    fsValAddr = this.getFsAddrForXmiId(featVal);
                }
                catch (NoSuchElementException e) {
                    if (!this.lenient) {
                        throw this.createException(12, Integer.toString(featVal));
                    }
                    this.sharedData.addOutOfTypeSystemAttribute(addr, "head", Integer.toString(featVal));
                }
                this.casBeingFilled.setFeatureValue(addr, headFeat, fsValAddr);
            }
        }
        
        private void finalizeArray(final int type, final int addr) throws SAXParseException {
            if (!this.casBeingFilled.isFSArrayType(type)) {
                return;
            }
            for (int size = this.casBeingFilled.ll_getArraySize(addr), i = 0; i < size; ++i) {
                final int arrayVal = this.casBeingFilled.getArrayValue(addr, i);
                if (arrayVal != 0) {
                    int arrayValAddr = 0;
                    try {
                        arrayValAddr = this.getFsAddrForXmiId(arrayVal);
                    }
                    catch (NoSuchElementException e) {
                        if (!this.lenient) {
                            throw this.createException(12, Integer.toString(arrayVal));
                        }
                        this.sharedData.addOutOfTypeSystemArrayElement(addr, i, arrayVal);
                    }
                    this.casBeingFilled.setArrayValue(addr, i, arrayValAddr);
                }
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
        
        private final int classifyType(final int type) {
            if (this.listUtils.isIntListType(type)) {
                return 101;
            }
            if (this.listUtils.isFloatListType(type)) {
                return 102;
            }
            if (this.listUtils.isStringListType(type)) {
                return 103;
            }
            if (this.listUtils.isFsListType(type)) {
                return 104;
            }
            return this.casBeingFilled.ll_getTypeClass(type);
        }
        
        private void addFsAddrXmiIdMapping(final int fsAddr, final int xmiId) {
            if (xmiId > 0) {
                if (this.mergePoint < 0) {
                    this.sharedData.addIdMapping(fsAddr, xmiId);
                }
                else {
                    this.localXmiIdToFsAddrMap.put(xmiId, fsAddr);
                }
            }
        }
        
        private int getFsAddrForXmiId(final int xmiId) {
            if (this.mergePoint >= 0 && this.isNewFS(xmiId)) {
                return this.localXmiIdToFsAddrMap.get(xmiId);
            }
            final int addr = this.sharedData.getFsAddrForXmiId(xmiId);
            if (addr > 0) {
                return addr;
            }
            throw new NoSuchElementException();
        }
        
        private void addToOutOfTypeSystemData(final XmlElementName xmlElementName, final Attributes attrs) throws XCASParsingException {
            this.outOfTypeSystemElement = new XmiSerializationSharedData.OotsElementData();
            this.outOfTypeSystemElement.elementName = xmlElementName;
            for (int i = 0; i < attrs.getLength(); ++i) {
                final String attrName = attrs.getQName(i);
                final String attrValue = attrs.getValue(i);
                if (attrName.equals(this.ID_ATTR_NAME)) {
                    this.outOfTypeSystemElement.xmiId = attrValue;
                }
                else {
                    this.outOfTypeSystemElement.attributes.add(new XmlAttribute(attrName, attrValue));
                }
            }
            this.sharedData.addOutOfTypeSystemElement(this.outOfTypeSystemElement);
        }
        
        private void addOutOfTypeSystemFeature(final XmiSerializationSharedData.OotsElementData ootsElem, final String featName, final List<String> featVals) {
            final Iterator<String> iter = featVals.iterator();
            final XmlElementName elemName = new XmlElementName("", featName, featName);
            while (iter.hasNext()) {
                ootsElem.childElements.add(new XmlElementNameAndContents(elemName, iter.next()));
            }
        }
        
        private boolean isNewFS(final int id) {
            return id > this.mergePoint;
        }
        
        private void addNonsharedFSToEncompassingFSMapping(final int nonsharedFS, final int encompassingFS) {
            this.sharedData.addNonsharedRefToFSMapping(nonsharedFS, encompassingFS);
        }
    }
}
