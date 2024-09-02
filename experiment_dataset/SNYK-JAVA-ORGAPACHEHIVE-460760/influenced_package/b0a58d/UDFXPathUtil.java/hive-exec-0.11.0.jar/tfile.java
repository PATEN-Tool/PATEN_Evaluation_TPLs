// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.hadoop.hive.ql.udf.xml;

import java.io.IOException;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.namespace.QName;
import java.io.Reader;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpression;
import org.xml.sax.InputSource;
import javax.xml.xpath.XPath;

public class UDFXPathUtil
{
    private XPath xpath;
    private ReusableStringReader reader;
    private InputSource inputSource;
    private XPathExpression expression;
    private String oldPath;
    
    public UDFXPathUtil() {
        this.xpath = XPathFactory.newInstance().newXPath();
        this.reader = new ReusableStringReader();
        this.inputSource = new InputSource(this.reader);
        this.expression = null;
        this.oldPath = null;
    }
    
    public Object eval(final String xml, final String path, final QName qname) {
        if (xml == null || path == null || qname == null) {
            return null;
        }
        if (xml.length() == 0 || path.length() == 0) {
            return null;
        }
        if (!path.equals(this.oldPath)) {
            try {
                this.expression = this.xpath.compile(path);
            }
            catch (XPathExpressionException e) {
                this.expression = null;
            }
            this.oldPath = path;
        }
        if (this.expression == null) {
            return null;
        }
        this.reader.set(xml);
        try {
            return this.expression.evaluate(this.inputSource, qname);
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException("Invalid expression '" + this.oldPath + "'");
        }
    }
    
    public Boolean evalBoolean(final String xml, final String path) {
        return (Boolean)this.eval(xml, path, XPathConstants.BOOLEAN);
    }
    
    public String evalString(final String xml, final String path) {
        return (String)this.eval(xml, path, XPathConstants.STRING);
    }
    
    public Double evalNumber(final String xml, final String path) {
        return (Double)this.eval(xml, path, XPathConstants.NUMBER);
    }
    
    public Node evalNode(final String xml, final String path) {
        return (Node)this.eval(xml, path, XPathConstants.NODE);
    }
    
    public NodeList evalNodeList(final String xml, final String path) {
        return (NodeList)this.eval(xml, path, XPathConstants.NODESET);
    }
    
    public static class ReusableStringReader extends Reader
    {
        private String str;
        private int length;
        private int next;
        private int mark;
        
        public ReusableStringReader() {
            this.str = null;
            this.length = -1;
            this.next = 0;
            this.mark = 0;
        }
        
        public void set(final String s) {
            this.str = s;
            this.length = s.length();
            this.mark = 0;
            this.next = 0;
        }
        
        private void ensureOpen() throws IOException {
            if (this.str == null) {
                throw new IOException("Stream closed");
            }
        }
        
        @Override
        public int read() throws IOException {
            this.ensureOpen();
            if (this.next >= this.length) {
                return -1;
            }
            return this.str.charAt(this.next++);
        }
        
        @Override
        public int read(final char[] cbuf, final int off, final int len) throws IOException {
            this.ensureOpen();
            if (off < 0 || off > cbuf.length || len < 0 || off + len > cbuf.length || off + len < 0) {
                throw new IndexOutOfBoundsException();
            }
            if (len == 0) {
                return 0;
            }
            if (this.next >= this.length) {
                return -1;
            }
            final int n = Math.min(this.length - this.next, len);
            this.str.getChars(this.next, this.next + n, cbuf, off);
            this.next += n;
            return n;
        }
        
        @Override
        public long skip(final long ns) throws IOException {
            this.ensureOpen();
            if (this.next >= this.length) {
                return 0L;
            }
            long n = Math.min(this.length - this.next, ns);
            n = Math.max(-this.next, n);
            this.next += (int)n;
            return n;
        }
        
        @Override
        public boolean ready() throws IOException {
            this.ensureOpen();
            return true;
        }
        
        @Override
        public boolean markSupported() {
            return true;
        }
        
        @Override
        public void mark(final int readAheadLimit) throws IOException {
            if (readAheadLimit < 0) {
                throw new IllegalArgumentException("Read-ahead limit < 0");
            }
            this.ensureOpen();
            this.mark = this.next;
        }
        
        @Override
        public void reset() throws IOException {
            this.ensureOpen();
            this.next = this.mark;
        }
        
        @Override
        public void close() {
            this.str = null;
        }
    }
}
