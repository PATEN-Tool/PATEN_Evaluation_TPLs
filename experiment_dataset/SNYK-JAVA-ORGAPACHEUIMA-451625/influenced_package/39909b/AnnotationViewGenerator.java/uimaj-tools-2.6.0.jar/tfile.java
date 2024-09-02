// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.uima.tools.util.htmlview;

import java.io.FileWriter;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import java.util.Iterator;
import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.resource.metadata.Capability;
import java.util.ArrayList;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Transformer;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import javax.xml.transform.TransformerConfigurationException;
import org.apache.uima.UIMARuntimeException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerFactory;

public class AnnotationViewGenerator
{
    private TransformerFactory mTFactory;
    private Templates mStyleMapToCss;
    private Templates mStyleMapToLegend;
    private Templates mStyleMapToDocFrameXsl;
    private File mOutputDir;
    
    public AnnotationViewGenerator(final File aOutputDir) {
        this.mOutputDir = aOutputDir;
        this.mTFactory = TransformerFactory.newInstance();
        this.mStyleMapToCss = this.getTemplates("styleMapToCss.xsl");
        this.mStyleMapToLegend = this.getTemplates("styleMapToLegend.xsl");
        this.mStyleMapToDocFrameXsl = this.getTemplates("styleMapToDocFrameXsl.xsl");
    }
    
    private Templates getTemplates(final String filename) {
        final InputStream is = AnnotationViewGenerator.class.getResourceAsStream(filename);
        Templates templates;
        try {
            templates = this.mTFactory.newTemplates(new StreamSource(is));
        }
        catch (TransformerConfigurationException e) {
            throw new UIMARuntimeException((Throwable)e);
        }
        finally {
            try {
                is.close();
            }
            catch (IOException ex) {}
        }
        return templates;
    }
    
    private void writeToFile(final String filename, final File outputDir) {
        final File outFile = new File(outputDir, filename);
        OutputStream os;
        try {
            os = new FileOutputStream(outFile);
        }
        catch (FileNotFoundException e) {
            throw new UIMARuntimeException((Throwable)e);
        }
        final InputStream is = AnnotationViewGenerator.class.getResourceAsStream(filename);
        try {
            final byte[] buf = new byte[1024];
            int numRead;
            while ((numRead = is.read(buf)) > 0) {
                os.write(buf, 0, numRead);
            }
        }
        catch (IOException e2) {
            throw new UIMARuntimeException((Throwable)e2);
        }
        finally {
            try {
                is.close();
            }
            catch (IOException ex) {}
            try {
                os.close();
            }
            catch (IOException ex2) {}
        }
    }
    
    public void processStyleMap(final File aStyleMap) throws TransformerException {
        this.writeToFile("annotations.xsl", this.mOutputDir);
        this.writeToFile("annotationViewer.js", this.mOutputDir);
        this.writeToFile("index.html", this.mOutputDir);
        final Transformer cssTransformer = this.mStyleMapToCss.newTransformer();
        cssTransformer.transform(new StreamSource(aStyleMap), new StreamResult(new File(this.mOutputDir, "annotations.css").getAbsolutePath()));
        final Transformer legendTransformer = this.mStyleMapToLegend.newTransformer();
        legendTransformer.transform(new StreamSource(aStyleMap), new StreamResult(new File(this.mOutputDir, "legend.html").getAbsolutePath()));
        final Transformer docFrameXslTransformer = this.mStyleMapToDocFrameXsl.newTransformer();
        docFrameXslTransformer.transform(new StreamSource(aStyleMap), new StreamResult(new File(this.mOutputDir, "docFrame.xsl").getAbsolutePath()));
    }
    
    public void processDocument(final File aInlineXmlDoc) throws TransformerException {
        final Transformer docHtmlTransformer = this.mTFactory.newTransformer(new StreamSource(new File(this.mOutputDir, "docFrame.xsl")));
        docHtmlTransformer.transform(new StreamSource(aInlineXmlDoc), new StreamResult(new File(this.mOutputDir, "docView.html").getAbsolutePath()));
    }
    
    public static String autoGenerateStyleMap(final AnalysisEngineMetaData aTaeMetaData) {
        final String[] STYLES = { "color:black; background:lightblue;", "color:black; background:lightgreen;", "color:black; background:orange;", "color:black; background:yellow;", "color:black; background:pink;", "color:black; background:salmon;", "color:black; background:cyan;", "color:black; background:violet;", "color:black; background:tan;", "color:white; background:brown;", "color:white; background:blue;", "color:white; background:green;", "color:white; background:red;", "color:white; background:mediumpurple;" };
        final ArrayList outputTypes = new ArrayList();
        final Capability[] capabilities = aTaeMetaData.getCapabilities();
        for (int i = 0; i < capabilities.length; ++i) {
            final TypeOrFeature[] outputs = capabilities[i].getOutputs();
            for (int j = 0; j < outputs.length; ++j) {
                if (outputs[j].isType() && !outputTypes.contains(outputs[j].getName())) {
                    outputTypes.add(outputs[j].getName());
                }
            }
        }
        final StringBuffer buf = new StringBuffer();
        buf.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
        buf.append("<styleMap>\n");
        int k = 0;
        for (String label : outputTypes) {
            final String outputType = label;
            final int lastDot = outputType.lastIndexOf(46);
            if (lastDot > -1) {
                label = outputType.substring(lastDot + 1);
            }
            buf.append("<rule>\n");
            buf.append("<pattern>");
            buf.append(outputType);
            buf.append("</pattern>\n");
            buf.append("<label>");
            buf.append(label);
            buf.append("</label>\n");
            buf.append("<style>");
            buf.append(STYLES[k % STYLES.length]);
            buf.append("</style>\n");
            buf.append("</rule>\n");
            ++k;
        }
        buf.append("</styleMap>\n");
        return buf.toString();
    }
    
    public static String autoGenerateStyleMap(final TypeSystemDescription aTypeSystem) {
        final String[] STYLES = { "color:black; background:lightblue;", "color:black; background:lightgreen;", "color:black; background:orange;", "color:black; background:yellow;", "color:black; background:pink;", "color:black; background:salmon;", "color:black; background:cyan;", "color:black; background:violet;", "color:black; background:tan;", "color:white; background:brown;", "color:white; background:blue;", "color:white; background:green;", "color:white; background:red;", "color:white; background:mediumpurple;" };
        final TypeDescription[] types = aTypeSystem.getTypes();
        final StringBuffer buf = new StringBuffer();
        buf.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
        buf.append("<styleMap>\n");
        for (int i = 0; i < types.length; ++i) {
            String label;
            final String outputType = label = types[i].getName();
            final int lastDot = outputType.lastIndexOf(46);
            if (lastDot > -1) {
                label = outputType.substring(lastDot + 1);
            }
            buf.append("<rule>\n");
            buf.append("<pattern>");
            buf.append(outputType);
            buf.append("</pattern>\n");
            buf.append("<label>");
            buf.append(label);
            buf.append("</label>\n");
            buf.append("<style>");
            buf.append(STYLES[i % STYLES.length]);
            buf.append("</style>\n");
            buf.append("</rule>\n");
        }
        buf.append("</styleMap>\n");
        return buf.toString();
    }
    
    public void autoGenerateStyleMapFile(final AnalysisEngine aAE, final File aStyleMapFile) throws IOException {
        this.autoGenerateStyleMapFile(aAE.getAnalysisEngineMetaData(), aStyleMapFile);
    }
    
    public void autoGenerateStyleMapFile(final AnalysisEngineMetaData aMetaData, final File aStyleMapFile) throws IOException {
        final String xmlStr = autoGenerateStyleMap(aMetaData);
        FileWriter out = null;
        try {
            out = new FileWriter(aStyleMapFile);
            out.write(xmlStr);
        }
        finally {
            if (out != null) {
                out.close();
            }
        }
    }
    
    public void autoGenerateStyleMapFile(final TypeSystemDescription aTypeSystem, final File aStyleMapFile) throws IOException {
        final String xmlStr = autoGenerateStyleMap(aTypeSystem);
        FileWriter out = null;
        try {
            out = new FileWriter(aStyleMapFile);
            out.write(xmlStr);
        }
        finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
