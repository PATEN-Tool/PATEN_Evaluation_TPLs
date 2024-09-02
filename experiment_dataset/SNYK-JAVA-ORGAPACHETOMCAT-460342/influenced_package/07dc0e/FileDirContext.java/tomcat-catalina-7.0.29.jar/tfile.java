// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.naming.resources;

import java.util.Date;
import java.io.FileInputStream;
import org.apache.juli.logging.LogFactory;
import java.util.Arrays;
import java.util.ArrayList;
import org.apache.tomcat.util.http.RequestUtil;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.io.FileOutputStream;
import javax.naming.directory.DirContext;
import java.io.InputStream;
import javax.naming.NameAlreadyBoundException;
import javax.naming.directory.ModificationItem;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attributes;
import org.apache.naming.NamingEntry;
import java.util.List;
import org.apache.naming.NamingContextEnumeration;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NameNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.io.File;
import org.apache.juli.logging.Log;

public class FileDirContext extends BaseDirContext
{
    private static final Log log;
    protected static final int BUFFER_SIZE = 2048;
    protected File base;
    protected String absoluteBase;
    protected boolean allowLinking;
    
    public FileDirContext() {
        this.base = null;
        this.absoluteBase = null;
        this.allowLinking = false;
    }
    
    public FileDirContext(final Hashtable<String, Object> env) {
        super(env);
        this.base = null;
        this.absoluteBase = null;
        this.allowLinking = false;
    }
    
    @Override
    public void setDocBase(final String docBase) {
        if (docBase == null) {
            throw new IllegalArgumentException(FileDirContext.sm.getString("resources.null"));
        }
        this.base = new File(docBase);
        try {
            this.base = this.base.getCanonicalFile();
        }
        catch (IOException ex) {}
        if (!this.base.exists() || !this.base.isDirectory() || !this.base.canRead()) {
            throw new IllegalArgumentException(FileDirContext.sm.getString("fileResources.base", docBase));
        }
        this.absoluteBase = this.base.getAbsolutePath();
        super.setDocBase(docBase);
    }
    
    public void setAllowLinking(final boolean allowLinking) {
        this.allowLinking = allowLinking;
    }
    
    public boolean getAllowLinking() {
        return this.allowLinking;
    }
    
    @Override
    public void release() {
        super.release();
    }
    
    @Override
    protected String doGetRealPath(final String path) {
        final File file = new File(this.getDocBase(), path);
        return file.getAbsolutePath();
    }
    
    @Override
    protected Object doLookup(final String name) {
        Object result = null;
        final File file = this.file(name);
        if (file == null) {
            return null;
        }
        if (file.isDirectory()) {
            final FileDirContext tempContext = new FileDirContext(this.env);
            tempContext.setDocBase(file.getPath());
            tempContext.setAllowLinking(this.getAllowLinking());
            result = tempContext;
        }
        else {
            result = new FileResource(file);
        }
        return result;
    }
    
    @Override
    public void unbind(final String name) throws NamingException {
        final File file = this.file(name);
        if (file == null) {
            throw new NameNotFoundException(FileDirContext.sm.getString("resources.notFound", name));
        }
        if (!file.delete()) {
            throw new NamingException(FileDirContext.sm.getString("resources.unbindFailed", name));
        }
    }
    
    @Override
    public void rename(final String oldName, final String newName) throws NamingException {
        final File file = this.file(oldName);
        if (file == null) {
            throw new NameNotFoundException(FileDirContext.sm.getString("resources.notFound", oldName));
        }
        final File newFile = new File(this.base, newName);
        if (!file.renameTo(newFile)) {
            throw new NamingException(FileDirContext.sm.getString("resources.renameFail", oldName, newName));
        }
    }
    
    @Override
    public NamingEnumeration<NameClassPair> list(final String name) throws NamingException {
        final File file = this.file(name);
        if (file == null) {
            throw new NameNotFoundException(FileDirContext.sm.getString("resources.notFound", name));
        }
        return new NamingContextEnumeration(this.list(file).iterator());
    }
    
    @Override
    protected List<NamingEntry> doListBindings(final String name) throws NamingException {
        final File file = this.file(name);
        if (file == null) {
            return null;
        }
        return this.list(file);
    }
    
    @Override
    public void destroySubcontext(final String name) throws NamingException {
        this.unbind(name);
    }
    
    @Override
    public Object lookupLink(final String name) throws NamingException {
        return this.lookup(name);
    }
    
    @Override
    public String getNameInNamespace() throws NamingException {
        return this.docBase;
    }
    
    @Override
    protected Attributes doGetAttributes(final String name, final String[] attrIds) throws NamingException {
        final File file = this.file(name);
        if (file == null) {
            return null;
        }
        return new FileResourceAttributes(file);
    }
    
    @Override
    public void modifyAttributes(final String name, final int mod_op, final Attributes attrs) throws NamingException {
        throw new OperationNotSupportedException();
    }
    
    @Override
    public void modifyAttributes(final String name, final ModificationItem[] mods) throws NamingException {
        throw new OperationNotSupportedException();
    }
    
    @Override
    public void bind(final String name, final Object obj, final Attributes attrs) throws NamingException {
        final File file = new File(this.base, name);
        if (file.exists()) {
            throw new NameAlreadyBoundException(FileDirContext.sm.getString("resources.alreadyBound", name));
        }
        this.rebind(name, obj, attrs);
    }
    
    @Override
    public void rebind(final String name, final Object obj, final Attributes attrs) throws NamingException {
        final File file = new File(this.base, name);
        InputStream is = null;
        if (obj instanceof Resource) {
            try {
                is = ((Resource)obj).streamContent();
            }
            catch (IOException e) {}
        }
        else if (obj instanceof InputStream) {
            is = (InputStream)obj;
        }
        else if (obj instanceof DirContext) {
            if (file.exists() && !file.delete()) {
                throw new NamingException(FileDirContext.sm.getString("resources.bindFailed", name));
            }
            if (!file.mkdir()) {
                throw new NamingException(FileDirContext.sm.getString("resources.bindFailed", name));
            }
        }
        if (is == null) {
            throw new NamingException(FileDirContext.sm.getString("resources.bindFailed", name));
        }
        try {
            FileOutputStream os = null;
            final byte[] buffer = new byte[2048];
            int len = -1;
            try {
                os = new FileOutputStream(file);
                while (true) {
                    len = is.read(buffer);
                    if (len == -1) {
                        break;
                    }
                    os.write(buffer, 0, len);
                }
            }
            finally {
                if (os != null) {
                    os.close();
                }
                is.close();
            }
        }
        catch (IOException e) {
            final NamingException ne = new NamingException(FileDirContext.sm.getString("resources.bindFailed", e));
            ne.initCause(e);
            throw ne;
        }
    }
    
    @Override
    public DirContext createSubcontext(final String name, final Attributes attrs) throws NamingException {
        final File file = new File(this.base, name);
        if (file.exists()) {
            throw new NameAlreadyBoundException(FileDirContext.sm.getString("resources.alreadyBound", name));
        }
        if (!file.mkdir()) {
            throw new NamingException(FileDirContext.sm.getString("resources.bindFailed", name));
        }
        return (DirContext)this.lookup(name);
    }
    
    @Override
    public DirContext getSchema(final String name) throws NamingException {
        throw new OperationNotSupportedException();
    }
    
    @Override
    public DirContext getSchemaClassDefinition(final String name) throws NamingException {
        throw new OperationNotSupportedException();
    }
    
    @Override
    public NamingEnumeration<SearchResult> search(final String name, final Attributes matchingAttributes, final String[] attributesToReturn) throws NamingException {
        return null;
    }
    
    @Override
    public NamingEnumeration<SearchResult> search(final String name, final Attributes matchingAttributes) throws NamingException {
        return null;
    }
    
    @Override
    public NamingEnumeration<SearchResult> search(final String name, final String filter, final SearchControls cons) throws NamingException {
        return null;
    }
    
    @Override
    public NamingEnumeration<SearchResult> search(final String name, final String filterExpr, final Object[] filterArgs, final SearchControls cons) throws NamingException {
        return null;
    }
    
    protected String normalize(final String path) {
        return RequestUtil.normalize(path, File.separatorChar == '\\');
    }
    
    protected File file(final String name) {
        final File file = new File(this.base, name);
        if (!file.exists() || !file.canRead()) {
            return null;
        }
        if (this.allowLinking) {
            return file;
        }
        String canPath = null;
        try {
            canPath = file.getCanonicalPath();
        }
        catch (IOException ex) {}
        if (canPath == null) {
            return null;
        }
        if (!canPath.startsWith(this.absoluteBase)) {
            return null;
        }
        String fileAbsPath = file.getAbsolutePath();
        if (fileAbsPath.endsWith(".")) {
            fileAbsPath += "/";
        }
        String absPath = this.normalize(fileAbsPath);
        canPath = this.normalize(canPath);
        if (this.absoluteBase.length() < absPath.length() && this.absoluteBase.length() < canPath.length()) {
            absPath = absPath.substring(this.absoluteBase.length() + 1);
            if (absPath == null) {
                return null;
            }
            if (absPath.equals("")) {
                absPath = "/";
            }
            canPath = canPath.substring(this.absoluteBase.length() + 1);
            if (canPath.equals("")) {
                canPath = "/";
            }
            if (!canPath.equals(absPath)) {
                return null;
            }
        }
        return file;
    }
    
    protected List<NamingEntry> list(final File file) {
        final List<NamingEntry> entries = new ArrayList<NamingEntry>();
        if (!file.isDirectory()) {
            return entries;
        }
        final String[] names = file.list();
        if (names == null) {
            FileDirContext.log.warn((Object)FileDirContext.sm.getString("fileResources.listingNull", file.getAbsolutePath()));
            return entries;
        }
        Arrays.sort(names);
        NamingEntry entry = null;
        for (int i = 0; i < names.length; ++i) {
            final File currentFile = new File(file, names[i]);
            Object object = null;
            if (currentFile.isDirectory()) {
                final FileDirContext tempContext = new FileDirContext(this.env);
                tempContext.setDocBase(file.getPath());
                tempContext.setAllowLinking(this.getAllowLinking());
                object = tempContext;
            }
            else {
                object = new FileResource(currentFile);
            }
            entry = new NamingEntry(names[i], object, 0);
            entries.add(entry);
        }
        return entries;
    }
    
    static {
        log = LogFactory.getLog((Class)FileDirContext.class);
    }
    
    protected static class FileResource extends Resource
    {
        protected File file;
        
        public FileResource(final File file) {
            this.file = file;
        }
        
        @Override
        public InputStream streamContent() throws IOException {
            if (this.binaryContent == null) {
                final FileInputStream fis = new FileInputStream(this.file);
                return this.inputStream = fis;
            }
            return super.streamContent();
        }
    }
    
    protected static class FileResourceAttributes extends ResourceAttributes
    {
        private static final long serialVersionUID = 1L;
        protected File file;
        protected boolean accessed;
        protected String canonicalPath;
        
        public FileResourceAttributes(final File file) {
            this.accessed = false;
            this.canonicalPath = null;
            this.file = file;
            this.getCreation();
            this.getLastModified();
        }
        
        @Override
        public boolean isCollection() {
            if (!this.accessed) {
                this.collection = this.file.isDirectory();
                this.accessed = true;
            }
            return super.isCollection();
        }
        
        @Override
        public long getContentLength() {
            if (this.contentLength != -1L) {
                return this.contentLength;
            }
            return this.contentLength = this.file.length();
        }
        
        @Override
        public long getCreation() {
            if (this.creation != -1L) {
                return this.creation;
            }
            return this.creation = this.getLastModified();
        }
        
        @Override
        public Date getCreationDate() {
            if (this.creation == -1L) {
                this.creation = this.getCreation();
            }
            return super.getCreationDate();
        }
        
        @Override
        public long getLastModified() {
            if (this.lastModified != -1L) {
                return this.lastModified;
            }
            return this.lastModified = this.file.lastModified();
        }
        
        @Override
        public Date getLastModifiedDate() {
            if (this.lastModified == -1L) {
                this.lastModified = this.getLastModified();
            }
            return super.getLastModifiedDate();
        }
        
        @Override
        public String getName() {
            if (this.name == null) {
                this.name = this.file.getName();
            }
            return this.name;
        }
        
        @Override
        public String getResourceType() {
            if (!this.accessed) {
                this.collection = this.file.isDirectory();
                this.accessed = true;
            }
            return super.getResourceType();
        }
        
        @Override
        public String getCanonicalPath() {
            if (this.canonicalPath == null) {
                try {
                    this.canonicalPath = this.file.getCanonicalPath();
                }
                catch (IOException ex) {}
            }
            return this.canonicalPath;
        }
    }
}
