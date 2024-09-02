// 
// Decompiled by Procyon v0.5.36
// 

package net.lingala.zip4j;

import java.util.concurrent.Executors;
import net.lingala.zip4j.io.inputstream.NumberedSplitRandomAccessFile;
import net.lingala.zip4j.model.enums.RandomAccessFileMode;
import java.io.RandomAccessFile;
import net.lingala.zip4j.headers.HeaderReader;
import net.lingala.zip4j.util.FileUtils;
import java.io.IOException;
import net.lingala.zip4j.util.UnzipUtil;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.tasks.SetCommentTask;
import net.lingala.zip4j.tasks.MergeSplitZipFileTask;
import net.lingala.zip4j.tasks.AsyncZipTask;
import net.lingala.zip4j.tasks.RenameFilesTask;
import net.lingala.zip4j.util.RawIO;
import java.util.Map;
import net.lingala.zip4j.tasks.RemoveFilesFromZipTask;
import java.util.Iterator;
import net.lingala.zip4j.headers.HeaderUtil;
import net.lingala.zip4j.tasks.ExtractFileTask;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.tasks.ExtractAllFilesTask;
import net.lingala.zip4j.tasks.AddStreamToZipTask;
import java.io.InputStream;
import net.lingala.zip4j.tasks.AddFolderToZipTask;
import java.util.Collections;
import net.lingala.zip4j.util.Zip4jUtil;
import net.lingala.zip4j.tasks.AddFilesToZipTask;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import java.util.List;
import net.lingala.zip4j.util.InternalZipConstants;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.nio.charset.Charset;
import net.lingala.zip4j.headers.HeaderWriter;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.model.ZipModel;
import java.io.File;

public class ZipFile
{
    private File zipFile;
    private ZipModel zipModel;
    private boolean isEncrypted;
    private ProgressMonitor progressMonitor;
    private boolean runInThread;
    private char[] password;
    private HeaderWriter headerWriter;
    private Charset charset;
    private ThreadFactory threadFactory;
    private ExecutorService executorService;
    
    public ZipFile(final String zipFile) {
        this(new File(zipFile), null);
    }
    
    public ZipFile(final String zipFile, final char[] password) {
        this(new File(zipFile), password);
    }
    
    public ZipFile(final File zipFile) {
        this(zipFile, null);
    }
    
    public ZipFile(final File zipFile, final char[] password) {
        this.headerWriter = new HeaderWriter();
        this.charset = InternalZipConstants.CHARSET_UTF_8;
        this.zipFile = zipFile;
        this.password = password;
        this.runInThread = false;
        this.progressMonitor = new ProgressMonitor();
    }
    
    public void createSplitZipFile(final List<File> filesToAdd, final ZipParameters parameters, final boolean splitArchive, final long splitLength) throws ZipException {
        if (this.zipFile.exists()) {
            throw new ZipException("zip file: " + this.zipFile + " already exists. To add files to existing zip file use addFile method");
        }
        if (filesToAdd == null || filesToAdd.size() == 0) {
            throw new ZipException("input file List is null, cannot create zip file");
        }
        this.createNewZipModel();
        this.zipModel.setSplitArchive(splitArchive);
        this.zipModel.setSplitLength(splitLength);
        new AddFilesToZipTask(this.zipModel, this.password, this.headerWriter, this.buildAsyncParameters()).execute(new AddFilesToZipTask.AddFilesToZipTaskParameters(filesToAdd, parameters, this.charset));
    }
    
    public void createSplitZipFileFromFolder(final File folderToAdd, final ZipParameters parameters, final boolean splitArchive, final long splitLength) throws ZipException {
        if (folderToAdd == null) {
            throw new ZipException("folderToAdd is null, cannot create zip file from folder");
        }
        if (parameters == null) {
            throw new ZipException("input parameters are null, cannot create zip file from folder");
        }
        if (this.zipFile.exists()) {
            throw new ZipException("zip file: " + this.zipFile + " already exists. To add files to existing zip file use addFolder method");
        }
        this.createNewZipModel();
        this.zipModel.setSplitArchive(splitArchive);
        if (splitArchive) {
            this.zipModel.setSplitLength(splitLength);
        }
        this.addFolder(folderToAdd, parameters, false);
    }
    
    public void addFile(final String fileToAdd) throws ZipException {
        this.addFile(fileToAdd, new ZipParameters());
    }
    
    public void addFile(final String fileToAdd, final ZipParameters zipParameters) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileToAdd)) {
            throw new ZipException("file to add is null or empty");
        }
        this.addFiles(Collections.singletonList(new File(fileToAdd)), zipParameters);
    }
    
    public void addFile(final File fileToAdd) throws ZipException {
        this.addFiles(Collections.singletonList(fileToAdd), new ZipParameters());
    }
    
    public void addFile(final File fileToAdd, final ZipParameters parameters) throws ZipException {
        this.addFiles(Collections.singletonList(fileToAdd), parameters);
    }
    
    public void addFiles(final List<File> filesToAdd) throws ZipException {
        this.addFiles(filesToAdd, new ZipParameters());
    }
    
    public void addFiles(final List<File> filesToAdd, final ZipParameters parameters) throws ZipException {
        if (filesToAdd == null || filesToAdd.size() == 0) {
            throw new ZipException("input file List is null or empty");
        }
        if (parameters == null) {
            throw new ZipException("input parameters are null");
        }
        if (this.progressMonitor.getState() == ProgressMonitor.State.BUSY) {
            throw new ZipException("invalid operation - Zip4j is in busy state");
        }
        this.readZipInfo();
        if (this.zipModel == null) {
            throw new ZipException("internal error: zip model is null");
        }
        if (this.zipFile.exists() && this.zipModel.isSplitArchive()) {
            throw new ZipException("Zip file already exists. Zip file format does not allow updating split/spanned files");
        }
        new AddFilesToZipTask(this.zipModel, this.password, this.headerWriter, this.buildAsyncParameters()).execute(new AddFilesToZipTask.AddFilesToZipTaskParameters(filesToAdd, parameters, this.charset));
    }
    
    public void addFolder(final File folderToAdd) throws ZipException {
        this.addFolder(folderToAdd, new ZipParameters());
    }
    
    public void addFolder(final File folderToAdd, final ZipParameters zipParameters) throws ZipException {
        if (folderToAdd == null) {
            throw new ZipException("input path is null, cannot add folder to zip file");
        }
        if (!folderToAdd.exists()) {
            throw new ZipException("folder does not exist");
        }
        if (!folderToAdd.isDirectory()) {
            throw new ZipException("input folder is not a directory");
        }
        if (!folderToAdd.canRead()) {
            throw new ZipException("cannot read input folder");
        }
        if (zipParameters == null) {
            throw new ZipException("input parameters are null, cannot add folder to zip file");
        }
        this.addFolder(folderToAdd, zipParameters, true);
    }
    
    private void addFolder(final File folderToAdd, final ZipParameters zipParameters, final boolean checkSplitArchive) throws ZipException {
        this.readZipInfo();
        if (this.zipModel == null) {
            throw new ZipException("internal error: zip model is null");
        }
        if (checkSplitArchive && this.zipModel.isSplitArchive()) {
            throw new ZipException("This is a split archive. Zip file format does not allow updating split/spanned files");
        }
        new AddFolderToZipTask(this.zipModel, this.password, this.headerWriter, this.buildAsyncParameters()).execute(new AddFolderToZipTask.AddFolderToZipTaskParameters(folderToAdd, zipParameters, this.charset));
    }
    
    public void addStream(final InputStream inputStream, final ZipParameters parameters) throws ZipException {
        if (inputStream == null) {
            throw new ZipException("inputstream is null, cannot add file to zip");
        }
        if (parameters == null) {
            throw new ZipException("zip parameters are null");
        }
        this.setRunInThread(false);
        this.readZipInfo();
        if (this.zipModel == null) {
            throw new ZipException("internal error: zip model is null");
        }
        if (this.zipFile.exists() && this.zipModel.isSplitArchive()) {
            throw new ZipException("Zip file already exists. Zip file format does not allow updating split/spanned files");
        }
        new AddStreamToZipTask(this.zipModel, this.password, this.headerWriter, this.buildAsyncParameters()).execute(new AddStreamToZipTask.AddStreamToZipTaskParameters(inputStream, parameters, this.charset));
    }
    
    public void extractAll(final String destinationPath) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(destinationPath)) {
            throw new ZipException("output path is null or invalid");
        }
        if (!Zip4jUtil.createDirectoryIfNotExists(new File(destinationPath))) {
            throw new ZipException("invalid output path");
        }
        if (this.zipModel == null) {
            this.readZipInfo();
        }
        if (this.zipModel == null) {
            throw new ZipException("Internal error occurred when extracting zip file");
        }
        if (this.progressMonitor.getState() == ProgressMonitor.State.BUSY) {
            throw new ZipException("invalid operation - Zip4j is in busy state");
        }
        new ExtractAllFilesTask(this.zipModel, this.password, this.buildAsyncParameters()).execute(new ExtractAllFilesTask.ExtractAllFilesTaskParameters(destinationPath, this.charset));
    }
    
    public void extractFile(final FileHeader fileHeader, final String destinationPath) throws ZipException {
        this.extractFile(fileHeader, destinationPath, null);
    }
    
    public void extractFile(final FileHeader fileHeader, final String destinationPath, final String newFileName) throws ZipException {
        if (fileHeader == null) {
            throw new ZipException("input file header is null, cannot extract file");
        }
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(destinationPath)) {
            throw new ZipException("destination path is empty or null, cannot extract file");
        }
        if (this.progressMonitor.getState() == ProgressMonitor.State.BUSY) {
            throw new ZipException("invalid operation - Zip4j is in busy state");
        }
        this.readZipInfo();
        new ExtractFileTask(this.zipModel, this.password, this.buildAsyncParameters()).execute(new ExtractFileTask.ExtractFileTaskParameters(destinationPath, fileHeader, newFileName, this.charset));
    }
    
    public void extractFile(final String fileName, final String destinationPath) throws ZipException {
        this.extractFile(fileName, destinationPath, null);
    }
    
    public void extractFile(final String fileName, final String destinationPath, final String newFileName) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
            throw new ZipException("file to extract is null or empty, cannot extract file");
        }
        this.readZipInfo();
        final FileHeader fileHeader = HeaderUtil.getFileHeader(this.zipModel, fileName);
        if (fileHeader == null) {
            throw new ZipException("No file found with name " + fileName + " in zip file", ZipException.Type.FILE_NOT_FOUND);
        }
        this.extractFile(fileHeader, destinationPath, newFileName);
    }
    
    public List<FileHeader> getFileHeaders() throws ZipException {
        this.readZipInfo();
        if (this.zipModel == null || this.zipModel.getCentralDirectory() == null) {
            return Collections.emptyList();
        }
        return this.zipModel.getCentralDirectory().getFileHeaders();
    }
    
    public FileHeader getFileHeader(final String fileName) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
            throw new ZipException("input file name is emtpy or null, cannot get FileHeader");
        }
        this.readZipInfo();
        if (this.zipModel == null || this.zipModel.getCentralDirectory() == null) {
            return null;
        }
        return HeaderUtil.getFileHeader(this.zipModel, fileName);
    }
    
    public boolean isEncrypted() throws ZipException {
        if (this.zipModel == null) {
            this.readZipInfo();
            if (this.zipModel == null) {
                throw new ZipException("Zip Model is null");
            }
        }
        if (this.zipModel.getCentralDirectory() == null || this.zipModel.getCentralDirectory().getFileHeaders() == null) {
            throw new ZipException("invalid zip file");
        }
        for (final FileHeader fileHeader : this.zipModel.getCentralDirectory().getFileHeaders()) {
            if (fileHeader != null && fileHeader.isEncrypted()) {
                this.isEncrypted = true;
                break;
            }
        }
        return this.isEncrypted;
    }
    
    public boolean isSplitArchive() throws ZipException {
        if (this.zipModel == null) {
            this.readZipInfo();
            if (this.zipModel == null) {
                throw new ZipException("Zip Model is null");
            }
        }
        return this.zipModel.isSplitArchive();
    }
    
    public void removeFile(final FileHeader fileHeader) throws ZipException {
        if (fileHeader == null) {
            throw new ZipException("input file header is null, cannot remove file");
        }
        this.removeFile(fileHeader.getFileName());
    }
    
    public void removeFile(final String fileName) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
            throw new ZipException("file name is empty or null, cannot remove file");
        }
        this.removeFiles(Collections.singletonList(fileName));
    }
    
    public void removeFiles(final List<String> fileNames) throws ZipException {
        if (fileNames == null) {
            throw new ZipException("fileNames list is null");
        }
        if (fileNames.isEmpty()) {
            return;
        }
        if (this.zipModel == null) {
            this.readZipInfo();
        }
        if (this.zipModel.isSplitArchive()) {
            throw new ZipException("Zip file format does not allow updating split/spanned files");
        }
        new RemoveFilesFromZipTask(this.zipModel, this.headerWriter, this.buildAsyncParameters()).execute(new RemoveFilesFromZipTask.RemoveFilesFromZipTaskParameters(fileNames, this.charset));
    }
    
    public void renameFile(final FileHeader fileHeader, final String newFileName) throws ZipException {
        if (fileHeader == null) {
            throw new ZipException("File header is null");
        }
        this.renameFile(fileHeader.getFileName(), newFileName);
    }
    
    public void renameFile(final String fileNameToRename, final String newFileName) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileNameToRename)) {
            throw new ZipException("file name to be changed is null or empty");
        }
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(newFileName)) {
            throw new ZipException("newFileName is null or empty");
        }
        this.renameFiles(Collections.singletonMap(fileNameToRename, newFileName));
    }
    
    public void renameFiles(final Map<String, String> fileNamesMap) throws ZipException {
        if (fileNamesMap == null) {
            throw new ZipException("fileNamesMap is null");
        }
        if (fileNamesMap.size() == 0) {
            return;
        }
        this.readZipInfo();
        if (this.zipModel.isSplitArchive()) {
            throw new ZipException("Zip file format does not allow updating split/spanned files");
        }
        final AsyncZipTask.AsyncTaskParameters asyncTaskParameters = this.buildAsyncParameters();
        new RenameFilesTask(this.zipModel, this.headerWriter, new RawIO(), this.charset, asyncTaskParameters).execute(new RenameFilesTask.RenameFilesTaskParameters(fileNamesMap));
    }
    
    public void mergeSplitFiles(final File outputZipFile) throws ZipException {
        if (outputZipFile == null) {
            throw new ZipException("outputZipFile is null, cannot merge split files");
        }
        if (outputZipFile.exists()) {
            throw new ZipException("output Zip File already exists");
        }
        this.readZipInfo();
        if (this.zipModel == null) {
            throw new ZipException("zip model is null, corrupt zip file?");
        }
        new MergeSplitZipFileTask(this.zipModel, this.buildAsyncParameters()).execute(new MergeSplitZipFileTask.MergeSplitZipFileTaskParameters(outputZipFile, this.charset));
    }
    
    public void setComment(final String comment) throws ZipException {
        if (comment == null) {
            throw new ZipException("input comment is null, cannot update zip file");
        }
        if (!this.zipFile.exists()) {
            throw new ZipException("zip file does not exist, cannot set comment for zip file");
        }
        this.readZipInfo();
        if (this.zipModel == null) {
            throw new ZipException("zipModel is null, cannot update zip file");
        }
        if (this.zipModel.getEndOfCentralDirectoryRecord() == null) {
            throw new ZipException("end of central directory is null, cannot set comment");
        }
        new SetCommentTask(this.zipModel, this.buildAsyncParameters()).execute(new SetCommentTask.SetCommentTaskTaskParameters(comment, this.charset));
    }
    
    public String getComment() throws ZipException {
        if (!this.zipFile.exists()) {
            throw new ZipException("zip file does not exist, cannot read comment");
        }
        this.readZipInfo();
        if (this.zipModel == null) {
            throw new ZipException("zip model is null, cannot read comment");
        }
        if (this.zipModel.getEndOfCentralDirectoryRecord() == null) {
            throw new ZipException("end of central directory record is null, cannot read comment");
        }
        return this.zipModel.getEndOfCentralDirectoryRecord().getComment();
    }
    
    public ZipInputStream getInputStream(final FileHeader fileHeader) throws IOException {
        if (fileHeader == null) {
            throw new ZipException("FileHeader is null, cannot get InputStream");
        }
        this.readZipInfo();
        if (this.zipModel == null) {
            throw new ZipException("zip model is null, cannot get inputstream");
        }
        return UnzipUtil.createZipInputStream(this.zipModel, fileHeader, this.password);
    }
    
    public boolean isValidZipFile() {
        if (!this.zipFile.exists()) {
            return false;
        }
        try {
            this.readZipInfo();
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
    
    public List<File> getSplitZipFiles() throws ZipException {
        this.readZipInfo();
        return FileUtils.getSplitZipFiles(this.zipModel);
    }
    
    public void setPassword(final char[] password) {
        this.password = password;
    }
    
    private void readZipInfo() throws ZipException {
        if (this.zipModel != null) {
            return;
        }
        if (!this.zipFile.exists()) {
            this.createNewZipModel();
            return;
        }
        if (!this.zipFile.canRead()) {
            throw new ZipException("no read access for the input zip file");
        }
        try (final RandomAccessFile randomAccessFile = this.initializeRandomAccessFileForHeaderReading()) {
            final HeaderReader headerReader = new HeaderReader();
            (this.zipModel = headerReader.readAllHeaders(randomAccessFile, this.charset)).setZipFile(this.zipFile);
        }
        catch (ZipException e) {
            throw e;
        }
        catch (IOException e2) {
            throw new ZipException(e2);
        }
    }
    
    private void createNewZipModel() {
        (this.zipModel = new ZipModel()).setZipFile(this.zipFile);
    }
    
    private RandomAccessFile initializeRandomAccessFileForHeaderReading() throws IOException {
        if (FileUtils.isNumberedSplitFile(this.zipFile)) {
            final File[] allSplitFiles = FileUtils.getAllSortedNumberedSplitFiles(this.zipFile);
            final NumberedSplitRandomAccessFile numberedSplitRandomAccessFile = new NumberedSplitRandomAccessFile(this.zipFile, RandomAccessFileMode.READ.getValue(), allSplitFiles);
            numberedSplitRandomAccessFile.openLastSplitFileForReading();
            return numberedSplitRandomAccessFile;
        }
        return new RandomAccessFile(this.zipFile, RandomAccessFileMode.READ.getValue());
    }
    
    private AsyncZipTask.AsyncTaskParameters buildAsyncParameters() {
        if (this.runInThread) {
            if (this.threadFactory == null) {
                this.threadFactory = Executors.defaultThreadFactory();
            }
            this.executorService = Executors.newSingleThreadExecutor(this.threadFactory);
        }
        return new AsyncZipTask.AsyncTaskParameters(this.executorService, this.runInThread, this.progressMonitor);
    }
    
    public ProgressMonitor getProgressMonitor() {
        return this.progressMonitor;
    }
    
    public boolean isRunInThread() {
        return this.runInThread;
    }
    
    public void setRunInThread(final boolean runInThread) {
        this.runInThread = runInThread;
    }
    
    public File getFile() {
        return this.zipFile;
    }
    
    public Charset getCharset() {
        return this.charset;
    }
    
    public void setCharset(final Charset charset) throws IllegalArgumentException {
        if (charset == null) {
            throw new IllegalArgumentException("charset cannot be null");
        }
        this.charset = charset;
    }
    
    public void setThreadFactory(final ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }
    
    public ExecutorService getExecutorService() {
        return this.executorService;
    }
    
    @Override
    public String toString() {
        return this.zipFile.toString();
    }
}
