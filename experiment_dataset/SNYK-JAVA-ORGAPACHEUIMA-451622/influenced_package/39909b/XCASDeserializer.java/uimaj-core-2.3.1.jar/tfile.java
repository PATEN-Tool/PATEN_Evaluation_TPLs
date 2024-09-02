// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.uima.cas.impl;

import org.apache.uima.internal.util.StringUtils;
import java.util.Map;
import java.util.Iterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SofaFS;
import org.xml.sax.SAXParseException;
import org.xml.sax.Attributes;
import java.util.ArrayList;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.Type;
import java.util.List;
import org.apache.uima.internal.util.rb_trees.RedBlackTree;
import org.xml.sax.Locator;
import org.apache.uima.internal.util.IntVector;
import org.xml.sax.ContentHandler;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import java.io.IOException;
import org.xml.sax.SAXException;
import java.io.InputStream;
import org.xml.sax.helpers.DefaultHandler;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.UimaContext;

public class XCASDeserializer
{
    private TypeSystemImpl ts;
    private UimaContext uimaContext;
    private String docTypeName;
    
    public XCASDeserializer(final TypeSystem ts, final UimaContext uimaContext) {
        this.docTypeName = "uima.tcas.Document";
        this.ts = (TypeSystemImpl)ts;
        this.uimaContext = uimaContext;
    }
    
    public XCASDeserializer(final TypeSystem ts) {
        this(ts, null);
    }
    
    public DefaultHandler getXCASHandler(final CAS cas) {
        return this.getXCASHandler(cas, null);
    }
    
    public DefaultHandler getXCASHandler(final CAS cas, final OutOfTypeSystemData outOfTypeSystemData) {
        return new XCASDeserializerHandler((CASImpl)cas, outOfTypeSystemData);
    }
    
    public String getDocumentTypeName() {
        return this.docTypeName;
    }
    
    public void setDocumentTypeName(final String aDocTypeName) {
        this.docTypeName = aDocTypeName;
    }
    
    public static void deserialize(final InputStream aStream, final CAS aCAS) throws SAXException, IOException {
        deserialize(aStream, aCAS, false);
    }
    
    public static void deserialize(final InputStream aStream, final CAS aCAS, final boolean aLenient) throws SAXException, IOException {
        final XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        final XCASDeserializer deser = new XCASDeserializer(aCAS.getTypeSystem());
        ContentHandler handler;
        if (aLenient) {
            handler = deser.getXCASHandler(aCAS, new OutOfTypeSystemData());
        }
        else {
            handler = deser.getXCASHandler(aCAS);
        }
        xmlReader.setContentHandler(handler);
        xmlReader.parse(new InputSource(aStream));
    }
    
    private static class FSInfo
    {
        private int addr;
        private IntVector indexRep;
        
        private FSInfo(final int addr, final IntVector indexRep) {
            this.addr = addr;
            this.indexRep = indexRep;
        }
    }
    
    private class XCASDeserializerHandler extends DefaultHandler
    {
        private static final int DOC_STATE = 0;
        private static final int FS_STATE = 1;
        private static final int FEAT_STATE = 2;
        private static final int CONTENT_STATE = 3;
        private static final int FEAT_CONTENT_STATE = 4;
        private static final int ARRAY_ELE_CONTENT_STATE = 5;
        private static final int ARRAY_ELE_STATE = 6;
        private static final int DOC_TEXT_STATE = 7;
        private static final int OOTS_FEAT_STATE = 8;
        private static final int OOTS_CONTENT_STATE = 9;
        private static final String DEFAULT_CONTENT_FEATURE = "value";
        private static final String reservedAttrPrefix = "_";
        private static final String unknownXMLSource = "<unknown>";
        private Locator locator;
        private CASImpl cas;
        private RedBlackTree<FSInfo> fsTree;
        private List<FSInfo> idLess;
        private int state;
        private StringBuffer buffer;
        private int currentAddr;
        private String currentContentFeat;
        private int arrayPos;
        private OutOfTypeSystemData outOfTypeSystemData;
        private FSData currentOotsFs;
        private int sofaTypeCode;
        private Type annotBaseType;
        private List<FSIndexRepository> indexRepositories;
        private List<CAS> views;
        private IntVector sofaRefMap;
        private IntVector indexMap;
        private int nextIndex;
        
        private XCASDeserializerHandler(final CASImpl aCAS, final OutOfTypeSystemData ootsData) {
            this.currentContentFeat = "value";
            (this.cas = aCAS.getBaseCAS()).resetNoQuestions();
            this.fsTree = new RedBlackTree<FSInfo>();
            this.idLess = new ArrayList<FSInfo>();
            this.buffer = new StringBuffer();
            this.outOfTypeSystemData = ootsData;
            this.indexRepositories = new ArrayList<FSIndexRepository>();
            this.views = new ArrayList<CAS>();
            this.indexRepositories.add(this.cas.getBaseIndexRepository());
            this.indexRepositories.add(this.cas.getView("_InitialView").getIndexRepository());
            this.sofaTypeCode = this.cas.ll_getTypeSystem().ll_getCodeForType(this.cas.getTypeSystem().getType("uima.cas.Sofa"));
            this.annotBaseType = this.cas.getAnnotationType();
            this.sofaRefMap = new IntVector();
            this.indexMap = new IntVector();
            this.sofaRefMap.add(1);
            this.indexMap.add(0);
        }
        
        private final void resetBuffer() {
            this.buffer = new StringBuffer();
        }
        
        @Override
        public void startDocument() throws SAXException {
            this.state = 0;
        }
        
        @Override
        public void startElement(final String nameSpaceURI, final String localName, final String qualifiedName, final Attributes attrs) throws SAXException {
            this.resetBuffer();
            switch (this.state) {
                case 0: {
                    if (!qualifiedName.equals("CAS")) {
                        throw this.createException(0, qualifiedName);
                    }
                    this.state = 1;
                    break;
                }
                case 1: {
                    this.currentContentFeat = "value";
                    if (qualifiedName.equals(XCASDeserializer.this.getDocumentTypeName())) {
                        this.readDocument(attrs);
                        break;
                    }
                    this.readFS(qualifiedName, attrs);
                    break;
                }
                case 6: {
                    this.readArrayElement(qualifiedName, attrs);
                    break;
                }
                default: {
                    throw this.createException(1, qualifiedName);
                }
            }
        }
        
        private void readDocument(final Attributes attrs) {
            this.state = 7;
        }
        
        private void readArrayElement(final String eleName, final Attributes attrs) throws SAXParseException {
            if (!eleName.equals("i")) {
                throw this.createException(2, eleName);
            }
            if (attrs.getLength() > 0) {
                throw this.createException(3);
            }
            this.state = 5;
        }
        
        private void readFS(final String qualifiedName, final Attributes attrs) throws SAXParseException {
            final String typeName = this.getCasTypeName(qualifiedName);
            TypeImpl type = (TypeImpl)XCASDeserializer.this.ts.getType(typeName);
            if (type == null && typeName.equals("uima.cas.SofA")) {
                type = (TypeImpl)XCASDeserializer.this.ts.getType("uima.cas.Sofa");
            }
            if (type == null) {
                if (this.outOfTypeSystemData == null) {
                    throw this.createException(4, typeName);
                }
                this.addToOutOfTypeSystemData(typeName, attrs);
            }
            else {
                if (this.cas.isArrayType(type.getCode())) {
                    this.readArray(type, attrs);
                    return;
                }
                final int addr = this.cas.ll_createFS(type.getCode());
                this.readFS(addr, attrs, true);
            }
        }
        
        private void readFS(final int addr, final Attributes attrs, final boolean toIndex) throws SAXParseException {
            this.currentAddr = addr;
            int id = -1;
            final IntVector indexRep = new IntVector();
            final int heapValue = this.cas.getHeapValue(addr);
            final Type type = this.cas.ll_getTypeSystem().ll_getTypeForCode(this.cas.ll_getFSRefType(addr));
            if (this.sofaTypeCode == heapValue) {
                boolean isInitialView = false;
                String sofaID = attrs.getValue("sofaID");
                if (sofaID.equals("_DefaultTextSofaName")) {
                    sofaID = "_InitialView";
                }
                if (sofaID.equals("_InitialView")) {
                    isInitialView = true;
                }
                final String sofaNum = attrs.getValue("sofaNum");
                final int thisSofaNum = Integer.parseInt(sofaNum);
                final int sofaFsId = Integer.parseInt(attrs.getValue("_id"));
                if (this.indexMap.size() == 1) {
                    if (isInitialView) {
                        if (thisSofaNum == 2) {
                            this.indexMap.add(-1);
                            this.indexMap.add(1);
                            this.nextIndex = 2;
                        }
                        else {
                            this.indexMap.add(1);
                            this.nextIndex = 2;
                        }
                    }
                    else if (thisSofaNum > 1) {
                        this.indexMap.add(1);
                        assert thisSofaNum == 2;
                        this.indexMap.add(2);
                        this.nextIndex = 3;
                    }
                    else {
                        this.indexMap.add(2);
                        this.nextIndex = 3;
                    }
                }
                else if (isInitialView) {
                    if (this.indexMap.size() == thisSofaNum) {
                        this.indexMap.add(1);
                    }
                }
                else {
                    this.indexMap.add(this.nextIndex);
                    ++this.nextIndex;
                }
                if (this.sofaRefMap.size() == thisSofaNum) {
                    this.sofaRefMap.add(sofaFsId);
                }
                else if (this.sofaRefMap.size() > thisSofaNum) {
                    this.sofaRefMap.set(thisSofaNum, sofaFsId);
                }
                else {
                    this.sofaRefMap.setSize(thisSofaNum + 1);
                    this.sofaRefMap.set(thisSofaNum, sofaFsId);
                }
            }
            for (int i = 0; i < attrs.getLength(); ++i) {
                final String attrName = attrs.getQName(i);
                String attrValue = attrs.getValue(i);
                if (attrName.startsWith("_")) {
                    if (attrName.equals("_id")) {
                        try {
                            id = Integer.parseInt(attrValue);
                            continue;
                        }
                        catch (NumberFormatException e) {
                            throw this.createException(5, attrValue);
                        }
                    }
                    if (attrName.equals("_content")) {
                        this.currentContentFeat = attrValue;
                    }
                    else if (attrName.equals("_indexed")) {
                        final String[] arrayvals = this.parseArray(attrValue);
                        for (int s = 0; s < arrayvals.length; ++s) {
                            indexRep.add(Integer.parseInt(arrayvals[s]));
                        }
                    }
                    else {
                        this.handleFeature(type, addr, attrName, attrValue, false);
                    }
                }
                else {
                    if (this.sofaTypeCode == heapValue && attrName.equals("sofaID") && attrValue.equals("_DefaultTextSofaName")) {
                        attrValue = "_InitialView";
                    }
                    this.handleFeature(type, addr, attrName, attrValue, false);
                }
            }
            if (this.sofaTypeCode == heapValue) {
                final SofaFS sofa = (SofaFS)this.cas.createFS(addr);
                this.cas.getBaseIndexRepository().addFS(sofa);
                final CAS view = this.cas.getView(sofa);
                if (sofa.getSofaRef() == 1) {
                    this.cas.registerInitialSofa();
                }
                else {
                    this.indexRepositories.add(this.cas.getSofaIndexRepository(sofa));
                }
                ((CASImpl)view).registerView(sofa);
                this.views.add(view);
            }
            final FSInfo fsInfo = new FSInfo(addr, indexRep);
            if (id < 0) {
                this.idLess.add(fsInfo);
            }
            else {
                this.fsTree.put(id, fsInfo);
            }
            this.state = 3;
        }
        
        private void readArray(final TypeImpl type, final Attributes attrs) throws SAXParseException {
            final IntVector indexRep = new IntVector();
            int size = 0;
            int id = -1;
            for (int i = 0; i < attrs.getLength(); ++i) {
                final String attrName = attrs.getQName(i);
                final String attrVal = attrs.getValue(i);
                if (attrName.equals("_id")) {
                    try {
                        id = Integer.parseInt(attrVal);
                        continue;
                    }
                    catch (NumberFormatException e) {
                        throw this.createException(5, attrVal);
                    }
                }
                if (attrName.equals("size")) {
                    try {
                        size = Integer.parseInt(attrVal);
                        if (size < 0) {
                            throw this.createException(6, attrVal);
                        }
                        continue;
                    }
                    catch (NumberFormatException e) {
                        throw this.createException(9, attrVal);
                    }
                }
                if (!attrName.equals("_indexed")) {
                    throw this.createException(7, attrName);
                }
                final String[] arrayvals = this.parseArray(attrVal);
                for (int s = 0; s < arrayvals.length; ++s) {
                    indexRep.add(Integer.parseInt(arrayvals[s]));
                }
            }
            FeatureStructureImpl fs;
            if (this.cas.isIntArrayType(type)) {
                fs = (FeatureStructureImpl)this.cas.createIntArrayFS(size);
            }
            else if (this.cas.isFloatArrayType(type)) {
                fs = (FeatureStructureImpl)this.cas.createFloatArrayFS(size);
            }
            else if (this.cas.isStringArrayType(type)) {
                fs = (FeatureStructureImpl)this.cas.createStringArrayFS(size);
            }
            else if (this.cas.isBooleanArrayType(type)) {
                fs = (FeatureStructureImpl)this.cas.createBooleanArrayFS(size);
            }
            else if (this.cas.isByteArrayType(type)) {
                fs = (FeatureStructureImpl)this.cas.createByteArrayFS(size);
            }
            else if (this.cas.isShortArrayType(type)) {
                fs = (FeatureStructureImpl)this.cas.createShortArrayFS(size);
            }
            else if (this.cas.isLongArrayType(type)) {
                fs = (FeatureStructureImpl)this.cas.createLongArrayFS(size);
            }
            else if (this.cas.isDoubleArrayType(type)) {
                fs = (FeatureStructureImpl)this.cas.createDoubleArrayFS(size);
            }
            else {
                fs = (FeatureStructureImpl)this.cas.createArrayFS(size);
            }
            final int addr = fs.getAddress();
            final FSInfo fsInfo = new FSInfo(addr, indexRep);
            if (id >= 0) {
                this.fsTree.put(id, fsInfo);
            }
            else {
                this.idLess.add(fsInfo);
            }
            this.currentAddr = addr;
            this.arrayPos = 0;
            this.state = 6;
        }
        
        private final boolean emptyVal(final String val) {
            return val == null || val.length() == 0;
        }
        
        private void handleFeature(final int addr, final String featName, final String featVal, final boolean lenient) throws SAXParseException {
            final int typeCode = this.cas.ll_getFSRefType(addr);
            final Type type = this.cas.ll_getTypeSystem().ll_getTypeForCode(typeCode);
            this.handleFeature(type, addr, featName, featVal, lenient);
        }
        
        private void handleFeature(final Type type, final int addr, final String featName, String featVal, final boolean lenient) throws SAXParseException {
            if (featName.equals("sofa") && XCASDeserializer.this.ts.subsumes(this.annotBaseType, type)) {
                featVal = Integer.toString(this.sofaRefMap.get(Integer.parseInt(featVal)));
            }
            if (featName.equals("sofaID") && this.sofaTypeCode == this.cas.getHeapValue(addr)) {
                final Type sofaType = XCASDeserializer.this.ts.ll_getTypeForCode(this.sofaTypeCode);
                final FeatureImpl sofaNumFeat = (FeatureImpl)sofaType.getFeatureByBaseName("sofaNum");
                final int sofaNum = this.cas.getFeatureValue(addr, sofaNumFeat.getCode());
                this.cas.setFeatureValue(addr, sofaNumFeat.getCode(), this.indexMap.get(sofaNum));
            }
            String realFeatName;
            if (featName.startsWith("_ref_")) {
                realFeatName = featName.substring("_ref_".length());
            }
            else {
                realFeatName = featName;
            }
            final FeatureImpl feat = (FeatureImpl)type.getFeatureByBaseName(realFeatName);
            if (feat == null) {
                if (this.outOfTypeSystemData != null) {
                    final Integer addrInteger = addr;
                    List<String[]> ootsAttrs = this.outOfTypeSystemData.extraFeatureValues.get(addrInteger);
                    if (ootsAttrs == null) {
                        ootsAttrs = new ArrayList<String[]>();
                        this.outOfTypeSystemData.extraFeatureValues.put(addrInteger, ootsAttrs);
                    }
                    ootsAttrs.add(new String[] { featName, featVal });
                }
                else if (!lenient) {
                    throw this.createException(8, featName);
                }
            }
            else if (this.cas.ll_isRefType(XCASDeserializer.this.ts.range(feat.getCode()))) {
                this.cas.setFeatureValue(addr, feat.getCode(), Integer.parseInt(featVal));
            }
            else {
                this.cas.setFeatureValueFromString(addr, feat.getCode(), featVal);
            }
        }
        
        @Override
        public void characters(final char[] chars, final int start, final int length) throws SAXException {
            switch (this.state) {
                case 3:
                case 4:
                case 5:
                case 7:
                case 9: {
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
                case 2: {
                    this.state = 1;
                    break;
                }
                case 3: {
                    if (!this.isAllWhitespace(this.buffer)) {
                        try {
                            this.handleFeature(this.currentAddr, this.currentContentFeat, this.buffer.toString(), true);
                        }
                        catch (XCASParsingException ex) {}
                    }
                    this.state = 1;
                    break;
                }
                case 4: {
                    this.handleFeature(this.currentAddr, qualifiedName, this.buffer.toString(), false);
                    this.state = 2;
                    break;
                }
                case 5: {
                    this.addArrayElement(this.buffer.toString());
                    this.state = 6;
                    break;
                }
                case 6: {
                    this.state = 1;
                    break;
                }
                case 7: {
                    final SofaFS newSofa = this.cas.createInitialSofa("text");
                    final CASImpl tcas = (CASImpl)this.cas.getInitialView();
                    tcas.registerView(newSofa);
                    tcas.setDocTextFromDeserializtion(this.buffer.toString());
                    final int addr = 1;
                    final int id = 1;
                    this.sofaRefMap.add(id);
                    final FSInfo fsInfo = new FSInfo(addr, new IntVector());
                    this.fsTree.put(id, fsInfo);
                    this.state = 1;
                    break;
                }
                case 9: {
                    if (!this.isAllWhitespace(this.buffer)) {
                        this.currentOotsFs.featVals.put(this.currentContentFeat, this.buffer.toString());
                    }
                    this.state = 1;
                    break;
                }
                case 8: {
                    this.state = 1;
                    break;
                }
            }
        }
        
        private void addArrayElement(final String content) throws SAXParseException {
            if (this.arrayPos >= this.cas.ll_getArraySize(this.currentAddr)) {
                throw this.createException(11);
            }
            try {
                if (!this.emptyVal(content)) {
                    if (this.cas.isArrayType(this.cas.getHeap().heap[this.currentAddr])) {
                        this.cas.setArrayValueFromString(this.currentAddr, this.arrayPos, content);
                    }
                    else {
                        System.out.println(" not a known array type ");
                    }
                }
            }
            catch (NumberFormatException e) {
                throw this.createException(9, content);
            }
            ++this.arrayPos;
        }
        
        @Override
        public void endDocument() throws SAXException {
            for (final FSInfo fsInfo : this.fsTree) {
                this.finalizeFS(fsInfo);
            }
            for (int i = 0; i < this.idLess.size(); ++i) {
                this.finalizeFS(this.idLess.get(i));
            }
            if (this.outOfTypeSystemData != null) {
                for (final FSData fsData : this.outOfTypeSystemData.fsList) {
                    this.finalizeOutOfTypeSystemFS(fsData);
                }
                this.finalizeOutOfTypeSystemFeatures();
            }
            for (int i = 0; i < this.views.size(); ++i) {
                this.views.get(i).updateDocumentAnnotation();
            }
        }
        
        private void finalizeFS(final FSInfo fsInfo) {
            final int addr = fsInfo.addr;
            if (fsInfo.indexRep.size() >= 0) {
                for (int i = 0; i < fsInfo.indexRep.size(); ++i) {
                    if (this.indexMap.size() == 1) {
                        this.indexRepositories.get(fsInfo.indexRep.get(i)).addFS(addr);
                    }
                    else {
                        this.indexRepositories.get(this.indexMap.get(fsInfo.indexRep.get(i))).addFS(addr);
                    }
                }
            }
            final int type = this.cas.getHeapValue(addr);
            if (this.cas.isArrayType(type)) {
                this.finalizeArray(type, addr, fsInfo);
                return;
            }
            final int[] feats = this.cas.getTypeSystemImpl().ll_getAppropriateFeatures(type);
            for (int j = 0; j < feats.length; ++j) {
                final int feat = feats[j];
                if (this.cas.ll_isRefType(XCASDeserializer.this.ts.range(feats[j]))) {
                    final int featVal = this.cas.getFeatureValue(addr, feat);
                    final FSInfo fsValInfo = this.fsTree.get(featVal);
                    if (fsValInfo == null) {
                        this.cas.setFeatureValue(addr, feat, 0);
                        if (featVal != 0 && this.outOfTypeSystemData != null) {
                            final Integer addrInteger = addr;
                            List<String[]> ootsAttrs = this.outOfTypeSystemData.extraFeatureValues.get(addrInteger);
                            if (ootsAttrs == null) {
                                ootsAttrs = new ArrayList<String[]>();
                                this.outOfTypeSystemData.extraFeatureValues.put(addrInteger, ootsAttrs);
                            }
                            final String featFullName = XCASDeserializer.this.ts.ll_getFeatureForCode(feat).getName();
                            final int separatorOffset = featFullName.indexOf(58);
                            final String featName = "_ref_" + featFullName.substring(separatorOffset + 1);
                            ootsAttrs.add(new String[] { featName, Integer.toString(featVal) });
                        }
                    }
                    else {
                        this.cas.setFeatureValue(addr, feat, fsValInfo.addr);
                    }
                }
            }
        }
        
        private void finalizeArray(final int type, final int addr, final FSInfo fsInfo) {
            if (!this.cas.isFSArrayType(type)) {
                return;
            }
            for (int size = this.cas.ll_getArraySize(addr), i = 0; i < size; ++i) {
                final int arrayVal = this.cas.getArrayValue(addr, i);
                final FSInfo fsValInfo = this.fsTree.get(arrayVal);
                if (fsValInfo == null) {
                    this.cas.setArrayValue(addr, i, 0);
                    if (arrayVal != 0 && this.outOfTypeSystemData != null) {
                        final Integer arrayAddrInteger = addr;
                        List<ArrayElement> ootsElements = this.outOfTypeSystemData.arrayElements.get(arrayAddrInteger);
                        if (ootsElements == null) {
                            ootsElements = new ArrayList<ArrayElement>();
                            this.outOfTypeSystemData.arrayElements.put(arrayAddrInteger, ootsElements);
                        }
                        final ArrayElement ootsElem = new ArrayElement(i, "a" + Integer.toString(arrayVal));
                        ootsElements.add(ootsElem);
                    }
                }
                else {
                    this.cas.setArrayValue(addr, i, fsValInfo.addr);
                }
            }
        }
        
        private void finalizeOutOfTypeSystemFS(final FSData aFS) {
            aFS.id = 'a' + aFS.id;
            for (final Map.Entry<String, String> entry : aFS.featVals.entrySet()) {
                final String attrName = entry.getKey();
                if (attrName.startsWith("_ref_")) {
                    final int val = Integer.parseInt(entry.getValue());
                    if (val < 0) {
                        continue;
                    }
                    final FSInfo fsValInfo = this.fsTree.get(val);
                    if (fsValInfo != null) {
                        entry.setValue(Integer.toString(fsValInfo.addr));
                    }
                    else {
                        entry.setValue("a" + val);
                    }
                }
            }
        }
        
        private void finalizeOutOfTypeSystemFeatures() {
            for (final List<String[]> attrs : this.outOfTypeSystemData.extraFeatureValues.values()) {
                for (final String[] attr : attrs) {
                    if (attr[0].startsWith("_ref_")) {
                        final int val = Integer.parseInt(attr[1]);
                        if (val < 0) {
                            continue;
                        }
                        final FSInfo fsValInfo = this.fsTree.get(val);
                        if (fsValInfo != null) {
                            attr[1] = Integer.toString(fsValInfo.addr);
                        }
                        else {
                            attr[1] = "a" + val;
                        }
                    }
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
        
        private void addToOutOfTypeSystemData(final String typeName, final Attributes attrs) throws XCASParsingException {
            if (this.outOfTypeSystemData != null) {
                final FSData fs = new FSData();
                fs.type = typeName;
                fs.indexRep = null;
                for (int i = 0; i < attrs.getLength(); ++i) {
                    final String attrName = attrs.getQName(i);
                    final String attrValue = attrs.getValue(i);
                    if (attrName.startsWith("_")) {
                        if (attrName.equals("_id")) {
                            fs.id = attrValue;
                        }
                        else if (attrName.equals("_content")) {
                            this.currentContentFeat = attrValue;
                        }
                        else if (attrName.equals("_indexed")) {
                            fs.indexRep = attrValue;
                        }
                        else {
                            fs.featVals.put(attrName, attrValue);
                        }
                    }
                    else {
                        fs.featVals.put(attrName, attrValue);
                    }
                }
                this.outOfTypeSystemData.fsList.add(fs);
                this.currentOotsFs = fs;
                this.state = 9;
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
        
        private String getCasTypeName(final String aTagName) {
            if (aTagName.indexOf(58) == -1 && aTagName.indexOf(45) == -1) {
                return aTagName;
            }
            return StringUtils.replaceAll(StringUtils.replaceAll(aTagName, ":", "_colon_"), "-", "_dash_");
        }
    }
}
