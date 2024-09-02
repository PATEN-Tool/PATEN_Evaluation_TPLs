// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.uima.cas.impl;

import java.util.function.Supplier;
import org.apache.uima.internal.util.StringUtils;
import org.apache.uima.util.impl.Constants;
import java.util.Map;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FeatureStructure;
import java.util.Iterator;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.CommonPrimitiveArray;
import org.apache.uima.internal.util.Pair;
import org.apache.uima.UimaSerializable;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.jcas.cas.Sofa;
import org.xml.sax.SAXParseException;
import org.xml.sax.Attributes;
import java.util.ArrayList;
import org.apache.uima.cas.FSIndexRepository;
import java.util.List;
import org.apache.uima.internal.util.rb_trees.RedBlackTree;
import org.xml.sax.Locator;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.jcas.cas.TOP;
import org.xml.sax.ContentHandler;
import org.xml.sax.XMLReader;
import org.apache.uima.internal.util.XMLUtils;
import org.xml.sax.InputSource;
import java.io.Reader;
import java.io.IOException;
import org.xml.sax.SAXException;
import java.io.InputStream;
import org.xml.sax.helpers.DefaultHandler;
import org.apache.uima.cas.CAS;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.TypeSystem;

public class XCASDeserializer
{
    private final TypeSystemImpl ts;
    private String docTypeName;
    
    public XCASDeserializer(final TypeSystem ts, final UimaContext uimaContext) {
        this.docTypeName = "uima.tcas.Document";
        this.ts = (TypeSystemImpl)ts;
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
    
    public static void deserialize(final Reader aReader, final CAS aCAS, final boolean aLenient) throws SAXException, IOException {
        deserialize(new InputSource(aReader), aCAS, aLenient);
    }
    
    public static void deserialize(final InputStream aStream, final CAS aCAS, final boolean aLenient) throws SAXException, IOException {
        deserialize(new InputSource(aStream), aCAS, aLenient);
    }
    
    public static void deserialize(final InputSource aSource, final CAS aCAS, final boolean aLenient) throws SAXException, IOException {
        final XMLReader xmlReader = XMLUtils.createXMLReader();
        final XCASDeserializer deser = new XCASDeserializer(aCAS.getTypeSystem());
        ContentHandler handler;
        if (aLenient) {
            handler = deser.getXCASHandler(aCAS, new OutOfTypeSystemData());
        }
        else {
            handler = deser.getXCASHandler(aCAS);
        }
        xmlReader.setContentHandler(handler);
        xmlReader.parse(aSource);
        final CASImpl casImpl = (CASImpl)aCAS.getLowLevelCAS();
        if (casImpl.is_ll_enableV2IdRefs()) {
            final TOP highest_fs = ((XCASDeserializerHandler)handler).highestIdFs;
            casImpl.setLastUsedFsId(highest_fs._id);
            casImpl.setLastFsV2Size(highest_fs._getTypeImpl().getFsSpaceReq(highest_fs));
        }
    }
    
    private static class FSInfo
    {
        private final TOP fs;
        private final IntVector indexRep;
        
        private FSInfo(final TOP fs, final IntVector indexRep) {
            this.fs = fs;
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
        private static final int sofaTypeCode = 33;
        private Locator locator;
        private final CASImpl cas;
        private final RedBlackTree<FSInfo> fsTree;
        private final List<FSInfo> idLess;
        private final List<Runnable> fixupToDos;
        private final List<Runnable> uimaSerializableFixups;
        private int state;
        private StringBuffer buffer;
        private TOP currentFs;
        private String currentContentFeat;
        private int arrayPos;
        private OutOfTypeSystemData outOfTypeSystemData;
        private FSData currentOotsFs;
        private final List<FSIndexRepository> indexRepositories;
        private final List<CAS> views;
        private final IntVector sofaRefMap;
        private final IntVector indexMap;
        private int nextIndex;
        private TOP highestIdFs;
        private int fsId;
        
        private XCASDeserializerHandler(final CASImpl aCAS, final OutOfTypeSystemData ootsData) {
            this.fixupToDos = new ArrayList<Runnable>();
            this.uimaSerializableFixups = new ArrayList<Runnable>();
            this.currentContentFeat = "value";
            this.highestIdFs = null;
            (this.cas = aCAS.getBaseCAS()).resetNoQuestions();
            this.fsTree = new RedBlackTree<FSInfo>();
            this.idLess = new ArrayList<FSInfo>();
            this.buffer = new StringBuffer();
            this.outOfTypeSystemData = ootsData;
            this.indexRepositories = new ArrayList<FSIndexRepository>();
            this.views = new ArrayList<CAS>();
            this.indexRepositories.add(this.cas.getBaseIndexRepository());
            this.indexRepositories.add(this.cas.getView("_InitialView").getIndexRepository());
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
        
        private void readFS(String qualifiedName, final Attributes attrs) throws SAXParseException {
            this.fsId = Integer.parseInt(attrs.getValue("_id"));
            if (qualifiedName.equals("uima.cas.SofA")) {
                qualifiedName = "uima.cas.Sofa";
            }
            final String typeName = this.getCasTypeName(qualifiedName);
            final TypeImpl type = XCASDeserializer.this.ts.getType(typeName);
            if (type == null) {
                if (this.outOfTypeSystemData == null) {
                    throw this.createException(4, typeName);
                }
                this.addToOutOfTypeSystemData(typeName, attrs);
            }
            else {
                if (type.isArray()) {
                    this.readArray(type, attrs);
                    return;
                }
                this.readFS(type, attrs, true);
            }
        }
        
        private void readFS(final TypeImpl type, final Attributes attrs, final boolean toIndex) throws SAXParseException {
            final int typecode = type.getCode();
            TOP fs;
            if (33 == typecode) {
                String sofaID = attrs.getValue("sofaID");
                if (sofaID.equals("_DefaultTextSofaName")) {
                    sofaID = "_InitialView";
                }
                final boolean isInitialView = sofaID.equals("_InitialView");
                final String sofaNum = attrs.getValue("sofaNum");
                final int extSofaNum = Integer.parseInt(sofaNum);
                if (this.indexMap.size() == 1) {
                    if (isInitialView) {
                        if (extSofaNum == 2) {
                            this.indexMap.add(-1);
                            this.indexMap.add(1);
                            this.nextIndex = 2;
                        }
                        else {
                            this.indexMap.add(1);
                            this.nextIndex = 2;
                        }
                    }
                    else if (extSofaNum > 1) {
                        this.indexMap.add(1);
                        assert extSofaNum == 2;
                        this.indexMap.add(2);
                        this.nextIndex = 3;
                    }
                    else {
                        this.indexMap.add(2);
                        this.nextIndex = 3;
                    }
                }
                else if (isInitialView) {
                    if (this.indexMap.size() == extSofaNum) {
                        this.indexMap.add(1);
                    }
                }
                else {
                    this.indexMap.add(this.nextIndex);
                    ++this.nextIndex;
                }
                if (this.sofaRefMap.size() == extSofaNum) {
                    this.sofaRefMap.add(this.fsId);
                }
                else if (this.sofaRefMap.size() > extSofaNum) {
                    this.sofaRefMap.set(extSofaNum, this.fsId);
                }
                else {
                    this.sofaRefMap.setSize(extSofaNum + 1);
                    this.sofaRefMap.set(extSofaNum, this.fsId);
                }
                final String sofaMimeType = attrs.getValue("mimeType");
                final String finalSofaId = sofaID;
                fs = this.maybeCreateWithV2Id(this.fsId, () -> this.cas.createSofa(this.indexMap.get(extSofaNum), finalSofaId, sofaMimeType));
            }
            else if (type.isAnnotationBaseType()) {
                final String extSofaNum2 = attrs.getValue("sofa");
                CAS casView;
                if (extSofaNum2 != null) {
                    casView = this.cas.getView((this.indexMap.size() == 1) ? 1 : this.indexMap.get(Integer.parseInt(extSofaNum2)));
                }
                else {
                    final String extSofaRefString = attrs.getValue("_ref_sofa");
                    if (null == extSofaRefString || extSofaRefString.length() == 0) {
                        throw this.createException(13);
                    }
                    casView = this.cas.getView((SofaFS)this.fsTree.get(Integer.parseInt(extSofaRefString)).fs);
                }
                if (type.getCode() == 36) {
                    fs = this.maybeCreateWithV2Id(this.fsId, () -> casView.getDocumentAnnotation());
                    this.cas.removeFromCorruptableIndexAnyView(fs, this.cas.getAddbackSingle());
                }
                else {
                    fs = this.maybeCreateWithV2Id(this.fsId, () -> casView.createFS(type));
                    if (this.currentFs instanceof UimaSerializable) {
                        final UimaSerializable ufs = (UimaSerializable)this.currentFs;
                        this.uimaSerializableFixups.add(() -> ufs._init_from_cas_data());
                    }
                }
            }
            else {
                fs = this.maybeCreateWithV2Id(this.fsId, () -> this.cas.createFS(type));
                if (this.currentFs instanceof UimaSerializable) {
                    final UimaSerializable ufs2 = (UimaSerializable)this.currentFs;
                    this.uimaSerializableFixups.add(() -> ufs2._init_from_cas_data());
                }
            }
            this.currentFs = fs;
            int extId = -1;
            final IntVector indexRep = new IntVector(1);
            for (int i = 0; i < attrs.getLength(); ++i) {
                final String attrName = attrs.getQName(i);
                String attrValue = attrs.getValue(i);
                if (attrName.startsWith("_")) {
                    if (attrName.equals("_id")) {
                        try {
                            extId = Integer.parseInt(attrValue);
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
                        this.handleFeature(type, fs, attrName, attrValue, false);
                    }
                }
                else {
                    if (33 == typecode && attrName.equals("sofaID") && attrValue.equals("_DefaultTextSofaName")) {
                        attrValue = "_InitialView";
                    }
                    if (!type.isAnnotationBaseType() || !attrName.equals("sofa")) {
                        this.handleFeature(type, fs, attrName, attrValue, false);
                    }
                }
            }
            if (type.getCode() == 36) {
                this.cas.addbackSingle(fs);
            }
            if (33 == typecode) {
                final Sofa sofa = (Sofa)fs;
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
            final FSInfo fsInfo = new FSInfo(fs, indexRep);
            if (extId < 0) {
                this.idLess.add(fsInfo);
            }
            else {
                this.fsTree.put(extId, fsInfo);
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
            final int finalSize = size;
            final TOP fs = this.maybeCreateWithV2Id(this.fsId, () -> this.cas.createArray(type, finalSize));
            final FSInfo fsInfo = new FSInfo(fs, indexRep);
            if (id >= 0) {
                this.fsTree.put(id, fsInfo);
            }
            else {
                this.idLess.add(fsInfo);
            }
            this.currentFs = fs;
            this.arrayPos = 0;
            this.state = 6;
        }
        
        private final boolean emptyVal(final String val) {
            return val == null || val.length() == 0;
        }
        
        private void handleFeature(final TOP fs, final String featName, final String featVal, final boolean lenient) throws SAXParseException {
            final Type type = fs._getTypeImpl();
            this.handleFeature(type, fs, featName, featVal, lenient);
        }
        
        private void handleFeature(final Type type, final TOP fs, final String featName, final String featValIn, final boolean lenient) throws SAXParseException {
            final String featVal = (featName.equals("sofa") && ((TypeImpl)type).isAnnotationBaseType()) ? Integer.toString(this.sofaRefMap.get(((Sofa)this.fsTree.get(Integer.parseInt(featValIn)).fs).getSofaNum())) : featValIn;
            final String realFeatName = this.getRealFeatName(featName);
            final FeatureImpl feat = (FeatureImpl)type.getFeatureByBaseName(realFeatName);
            if (feat == null) {
                if (this.outOfTypeSystemData != null) {
                    List<Pair<String, Object>> ootsAttrs = this.outOfTypeSystemData.extraFeatureValues.get(fs);
                    if (ootsAttrs == null) {
                        ootsAttrs = new ArrayList<Pair<String, Object>>();
                        this.outOfTypeSystemData.extraFeatureValues.put(fs, ootsAttrs);
                    }
                    ootsAttrs.add(new Pair<String, Object>(featName, featVal));
                }
                else if (!lenient) {
                    throw this.createException(8, featName);
                }
            }
            else if (feat.getRangeImpl().isRefType) {
                this.fixupToDos.add(() -> this.finalizeRefValue(Integer.parseInt(featVal), fs, feat));
            }
            else {
                CASImpl.setFeatureValueFromStringNoDocAnnotUpdate(fs, feat, featVal);
            }
        }
        
        private String getRealFeatName(final String featName) {
            return featName.startsWith("_ref_") ? featName.substring("_ref_".length()) : featName;
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
                            this.handleFeature(this.currentFs, this.currentContentFeat, this.buffer.toString(), true);
                        }
                        catch (XCASParsingException ex) {}
                    }
                    this.state = 1;
                    break;
                }
                case 4: {
                    this.handleFeature(this.currentFs, qualifiedName, this.buffer.toString(), false);
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
                    final Sofa newSofa = (Sofa)this.maybeCreateWithV2Id(1, () -> this.cas.createInitialSofa("text"));
                    final CASImpl initialView = this.cas.getInitialView();
                    initialView.registerView(newSofa);
                    initialView.setDocTextFromDeserializtion(this.buffer.toString());
                    final int id = 1;
                    this.sofaRefMap.add(id);
                    final FSInfo fsInfo = new FSInfo((TOP)newSofa, new IntVector());
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
            if (this.currentFs instanceof CommonPrimitiveArray) {
                final CommonPrimitiveArray fsa = (CommonPrimitiveArray)this.currentFs;
                if (this.arrayPos >= fsa.size()) {
                    throw this.createException(11);
                }
                try {
                    if (!this.emptyVal(content)) {
                        fsa.setArrayValueFromString(this.arrayPos, content);
                    }
                }
                catch (NumberFormatException e) {
                    throw this.createException(9, content);
                }
            }
            else if (content != null && content.length() > 0) {
                final FSArray fsa2 = (FSArray)this.currentFs;
                final int pos = this.arrayPos;
                final int extId = Integer.parseInt(content);
                this.fixupToDos.add(() -> this.finalizeArrayRefValue(extId, pos, fsa2));
            }
            ++this.arrayPos;
        }
        
        @Override
        public void endDocument() throws SAXException {
            for (final Runnable fixup : this.fixupToDos) {
                fixup.run();
            }
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
            for (final CAS view : this.views) {
                final AutoCloseable ac = view.protectIndexes();
                try {
                    ((CASImpl)view).updateDocumentAnnotation();
                }
                finally {
                    try {
                        ac.close();
                    }
                    catch (Exception e) {
                        Misc.internalError();
                    }
                }
            }
            for (final Runnable r : this.uimaSerializableFixups) {
                r.run();
            }
        }
        
        private void finalizeFS(final FSInfo fsInfo) {
            this.finalizeAddToIndexes(fsInfo);
        }
        
        private void finalizeRefValue(final int extId, final TOP fs, final FeatureImpl fi) {
            final FSInfo fsInfo = this.fsTree.get(extId);
            if (fsInfo == null) {
                if (extId != 0 && this.outOfTypeSystemData != null) {
                    List<Pair<String, Object>> ootsAttrs = this.outOfTypeSystemData.extraFeatureValues.get(fs);
                    if (ootsAttrs == null) {
                        ootsAttrs = new ArrayList<Pair<String, Object>>();
                        this.outOfTypeSystemData.extraFeatureValues.put(fs, ootsAttrs);
                    }
                    final String featFullName = fi.getName();
                    final int separatorOffset = featFullName.indexOf(58);
                    final String featName = "_ref_" + featFullName.substring(separatorOffset + 1);
                    ootsAttrs.add(new Pair<String, Object>(featName, Integer.toString(extId)));
                }
                CASImpl.setFeatureValueMaybeSofa(fs, fi, null);
            }
            else if (fi.getCode() != 15) {
                if (fs instanceof Sofa) {
                    final Sofa sofa = (Sofa)fs;
                    switch (fi.getRangeImpl().getCode()) {
                        case 12: {
                            sofa.setLocalSofaData(fsInfo.fs);
                        }
                        default: {
                            throw new CASRuntimeException("INTERNAL_ERROR", new Object[0]);
                        }
                    }
                }
                else {
                    XCASDeserializer.this.ts.fixupFSArrayTypes(fi.getRangeImpl(), fsInfo.fs);
                    CASImpl.setFeatureValueMaybeSofa(fs, fi, fsInfo.fs);
                }
            }
        }
        
        private void finalizeArrayRefValue(final int extId, final int pos, final FSArray fs) {
            final FSInfo fsInfo = this.fsTree.get(extId);
            if (fsInfo == null) {
                if (extId != 0 && this.outOfTypeSystemData != null) {
                    List<ArrayElement> ootsElements = this.outOfTypeSystemData.arrayElements.get(fs);
                    if (ootsElements == null) {
                        ootsElements = new ArrayList<ArrayElement>();
                        this.outOfTypeSystemData.arrayElements.put(fs, ootsElements);
                    }
                    final ArrayElement ootsElem = new ArrayElement(pos, "a" + Integer.toString(extId));
                    ootsElements.add(ootsElem);
                }
                fs.set(pos, null);
            }
            else {
                fs.set(pos, fsInfo.fs);
            }
        }
        
        private void finalizeAddToIndexes(final FSInfo fsInfo) {
            if (fsInfo.indexRep.size() >= 0) {
                for (int i = 0; i < fsInfo.indexRep.size(); ++i) {
                    if (this.indexMap.size() == 1) {
                        this.indexRepositories.get(fsInfo.indexRep.get(i)).addFS(fsInfo.fs);
                    }
                    else {
                        this.indexRepositories.get(this.indexMap.get(fsInfo.indexRep.get(i))).addFS(fsInfo.fs);
                    }
                }
            }
        }
        
        private void finalizeOutOfTypeSystemFS(final FSData aFS) {
            aFS.id = 'a' + aFS.id;
            for (final Map.Entry<String, Object> entry : aFS.featVals.entrySet()) {
                final String attrName = entry.getKey();
                if (attrName.startsWith("_ref_")) {
                    final int val = Integer.parseInt(entry.getValue());
                    if (val < 0) {
                        continue;
                    }
                    final FSInfo fsValInfo = this.fsTree.get(val);
                    if (fsValInfo != null) {
                        entry.setValue(fsValInfo.fs);
                    }
                    else {
                        entry.setValue("a" + val);
                    }
                }
            }
        }
        
        private void finalizeOutOfTypeSystemFeatures() {
            for (final List<Pair<String, Object>> attrs : this.outOfTypeSystemData.extraFeatureValues.values()) {
                for (final Pair<String, Object> p : attrs) {
                    final String sv = (String)((p.u instanceof String) ? p.u : "");
                    if (p.t.startsWith("_ref_")) {
                        final int val = Integer.parseInt(sv);
                        if (val < 0) {
                            continue;
                        }
                        final FSInfo fsValInfo = this.fsTree.get(val);
                        if (fsValInfo != null) {
                            p.u = fsValInfo.fs;
                        }
                        else {
                            p.u = "a" + val;
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
                final FSData fsData = new FSData();
                fsData.type = typeName;
                fsData.indexRep = null;
                for (int i = 0; i < attrs.getLength(); ++i) {
                    final String attrName = attrs.getQName(i);
                    final String attrValue = attrs.getValue(i);
                    if (attrName.startsWith("_")) {
                        if (attrName.equals("_id")) {
                            fsData.id = attrValue;
                        }
                        else if (attrName.equals("_content")) {
                            this.currentContentFeat = attrValue;
                        }
                        else if (attrName.equals("_indexed")) {
                            fsData.indexRep = attrValue;
                        }
                        else {
                            fsData.featVals.put(attrName, attrValue);
                        }
                    }
                    else {
                        fsData.featVals.put(attrName, attrValue);
                    }
                }
                this.outOfTypeSystemData.fsList.add(fsData);
                this.currentOotsFs = fsData;
                this.state = 9;
            }
        }
        
        private String[] parseArray(String val) {
            val = val.trim();
            String[] arrayVals;
            if (this.emptyVal(val)) {
                arrayVals = Constants.EMPTY_STRING_ARRAY;
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
        
        TOP maybeCreateWithV2Id(final int id, final Supplier<TOP> create) {
            if (this.cas.is_ll_enableV2IdRefs()) {
                this.cas.set_reuseId(id);
                try {
                    final TOP fs = create.get();
                    if (this.highestIdFs == null) {
                        this.highestIdFs = fs;
                    }
                    else if (this.highestIdFs._id < fs._id) {
                        this.highestIdFs = fs;
                    }
                    return fs;
                }
                finally {
                    this.cas.set_reuseId(0);
                }
            }
            return create.get();
        }
    }
}
