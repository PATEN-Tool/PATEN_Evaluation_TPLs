// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.oozie.tools;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.CancellationException;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.Executors;
import org.eclipse.jetty.util.ConcurrentHashSet;
import java.util.concurrent.ExecutorService;
import org.apache.hadoop.fs.FSDataOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import com.google.common.base.Preconditions;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import org.apache.commons.io.FilenameUtils;
import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.hadoop.conf.Configuration;
import java.net.URI;
import java.util.Map;
import java.util.Collection;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.fs.FileSystem;
import org.apache.oozie.service.HadoopAccessorService;
import org.apache.oozie.service.WorkflowAppService;
import org.apache.oozie.service.Services;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileUtil;
import java.util.HashMap;
import java.io.IOException;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import java.io.File;
import org.apache.oozie.cli.CLIParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class OozieSharelibCLI
{
    public static final String[] HELP_INFO;
    public static final String HELP_CMD = "help";
    public static final String CREATE_CMD = "create";
    public static final String UPGRADE_CMD = "upgrade";
    public static final String LIB_OPT = "locallib";
    public static final String EXTRALIBS = "extralib";
    public static final String FS_OPT = "fs";
    public static final String CONCURRENCY_OPT = "concurrency";
    public static final String OOZIE_HOME = "oozie.home.dir";
    public static final String SHARE_LIB_PREFIX = "lib_";
    public static final String NEW_LINE;
    public static final String EXTRALIBS_USAGE;
    public static final String EXTRALIBS_PATH_SEPARATOR = ",";
    public static final String EXTRALIBS_SHARELIB_KEY_VALUE_SEPARATOR = "=";
    private boolean used;
    
    public static void main(final String[] args) throws Exception {
        System.exit(new OozieSharelibCLI().run(args));
    }
    
    public OozieSharelibCLI() {
        this.used = false;
    }
    
    protected Options createUpgradeOptions(final String subCommand) {
        final Option sharelib = new Option("locallib", true, "Local share library directory");
        final Option uri = new Option("fs", true, "URI of the fileSystem to " + subCommand + " oozie share library");
        final Option concurrency = new Option("concurrency", true, "Number of threads to be used for copy operations. (default=1)");
        final Options options = new Options();
        options.addOption(sharelib);
        options.addOption(uri);
        options.addOption(concurrency);
        final Option addLibsOption = new Option("extralib", true, OozieSharelibCLI.EXTRALIBS_USAGE);
        options.addOption(addLibsOption);
        return options;
    }
    
    @SuppressFBWarnings(value = { "PATH_TRAVERSAL_IN" }, justification = "False positive")
    public synchronized int run(final String[] args) throws Exception {
        if (this.used) {
            throw new IllegalStateException("CLI instance already used");
        }
        this.used = true;
        final CLIParser parser = new CLIParser("oozie-setup.sh", OozieSharelibCLI.HELP_INFO);
        final String oozieHome = System.getProperty("oozie.home.dir");
        parser.addCommand("help", "", "display usage for all commands or specified command", new Options(), false);
        parser.addCommand("create", "", "create a new timestamped version of oozie sharelib", this.createUpgradeOptions("create"), false);
        parser.addCommand("upgrade", "", "[deprecated][use command \"create\" to create new version]   upgrade oozie sharelib \n", this.createUpgradeOptions("upgrade"), false);
        try {
            final CLIParser.Command command = parser.parse(args);
            final String sharelibAction = command.getName();
            if (sharelibAction.equals("help")) {
                parser.showHelp(command.getCommandLine());
                return 0;
            }
            if (!command.getCommandLine().hasOption("fs")) {
                throw new Exception("-fs option must be specified");
            }
            final int threadPoolSize = Integer.valueOf(command.getCommandLine().getOptionValue("concurrency", "1"));
            File srcFile = null;
            if (command.getCommandLine().hasOption("locallib")) {
                srcFile = new File(command.getCommandLine().getOptionValue("locallib"));
            }
            else {
                final Collection<File> files = (Collection<File>)FileUtils.listFiles(new File(oozieHome), (IOFileFilter)new WildcardFileFilter("oozie-sharelib*.tar.gz"), (IOFileFilter)null);
                if (files.size() > 1) {
                    throw new IOException("more than one sharelib tar found at " + oozieHome);
                }
                if (files.isEmpty()) {
                    throw new IOException("default sharelib tar not found in oozie home dir: " + oozieHome);
                }
                srcFile = files.iterator().next();
            }
            Map<String, String> extraLibs = new HashMap<String, String>();
            if (command.getCommandLine().hasOption("extralib")) {
                final String[] param = command.getCommandLine().getOptionValues("extralib");
                extraLibs = getExtraLibs(param);
            }
            final File temp = File.createTempFile("oozie", ".dir");
            temp.delete();
            temp.mkdir();
            temp.deleteOnExit();
            if (!srcFile.isDirectory()) {
                FileUtil.unTar(srcFile, temp);
                srcFile = new File(temp.toString() + "/share/lib");
            }
            else {
                srcFile = new File(srcFile, "lib");
            }
            final String hdfsUri = command.getCommandLine().getOptionValue("fs");
            final Path srcPath = new Path(srcFile.toString());
            final Services services = new Services();
            services.getConf().set("oozie.services", "org.apache.oozie.service.LiteWorkflowAppService, org.apache.oozie.service.HadoopAccessorService");
            services.getConf().set("oozie.services.ext", "");
            services.init();
            final WorkflowAppService lwas = (WorkflowAppService)services.get((Class)WorkflowAppService.class);
            final HadoopAccessorService has = (HadoopAccessorService)services.get((Class)HadoopAccessorService.class);
            Path dstPath = lwas.getSystemLibPath();
            final URI uri = new Path(hdfsUri).toUri();
            final Configuration fsConf = has.createConfiguration(uri.getAuthority());
            final FileSystem fs = FileSystem.get(uri, fsConf);
            if (!fs.exists(dstPath)) {
                fs.mkdirs(dstPath);
            }
            ECPolicyDisabler.tryDisableECPolicyForPath(fs, dstPath);
            if (sharelibAction.equals("create") || sharelibAction.equals("upgrade")) {
                dstPath = new Path(dstPath.toString() + "/" + "lib_" + this.getTimestampDirectory());
            }
            System.out.println("the destination path for sharelib is: " + dstPath);
            this.checkIfSourceFilesExist(srcFile);
            this.copyToSharelib(threadPoolSize, srcFile, srcPath, dstPath, fs);
            this.copyExtraLibs(threadPoolSize, extraLibs, dstPath, fs);
            services.destroy();
            FileUtils.deleteDirectory(temp);
            return 0;
        }
        catch (ParseException ex) {
            System.err.println("Invalid sub-command: " + ex.getMessage());
            System.err.println();
            System.err.println(parser.shortHelp());
            return 1;
        }
        catch (NumberFormatException ex2) {
            logError("Invalid configuration value: ", ex2);
            return 1;
        }
        catch (Exception ex3) {
            logError(ex3.getMessage(), ex3);
            return 1;
        }
    }
    
    @VisibleForTesting
    static Map<String, String> getExtraLibs(final String[] param) {
        final Map<String, String> extraLibs = new HashMap<String, String>();
        for (final String lib : param) {
            final String[] addLibParts = lib.split("=");
            if (addLibParts.length != 2) {
                printExtraSharelibUsage();
                throw new IllegalArgumentException(String.format("Argument of extralibs '%s' is in a wrong format. Exiting.", (Object[])param));
            }
            final String sharelibName = addLibParts[0];
            final String sharelibPaths = addLibParts[1];
            if (extraLibs.containsKey(sharelibName)) {
                printExtraSharelibUsage();
                throw new IllegalArgumentException(String.format("Extra sharelib, '%s', has been specified multiple times. Exiting.", (Object[])param));
            }
            extraLibs.put(sharelibName, sharelibPaths);
        }
        return extraLibs;
    }
    
    private static void printExtraSharelibUsage() {
        System.err.println(OozieSharelibCLI.EXTRALIBS_USAGE);
    }
    
    @VisibleForTesting
    @SuppressFBWarnings(value = { "PATH_TRAVERSAL_IN" }, justification = "FilenameUtils is used to filter user input. JDK8+ is used.")
    void copyExtraLibs(final int threadPoolSize, final Map<String, String> extraLibs, final Path dstPath, final FileSystem fs) throws IOException {
        for (final Map.Entry<String, String> sharelib : extraLibs.entrySet()) {
            final Path libDestPath = new Path(dstPath.toString() + "/" + sharelib.getKey());
            for (final String libPath : sharelib.getValue().split(",")) {
                final File srcFile = new File(FilenameUtils.getFullPath(libPath) + FilenameUtils.getName(libPath));
                final Path srcPath = new Path(FilenameUtils.getFullPath(libPath) + FilenameUtils.getName(libPath));
                this.checkIfSourceFilesExist(srcFile);
                this.copyToSharelib(threadPoolSize, srcFile, srcPath, libDestPath, fs);
            }
        }
    }
    
    @VisibleForTesting
    protected void copyToSharelib(final int threadPoolSize, final File srcFile, final Path srcPath, final Path dstPath, final FileSystem fs) throws IOException {
        if (threadPoolSize > 1) {
            final long fsLimitsMinBlockSize = fs.getConf().getLong("dfs.namenode.fs-limits.min-block-size", 1048576L);
            final long bytesPerChecksum = fs.getConf().getLong("dfs.bytes-per-checksum", 512L);
            new ConcurrentCopyFromLocal(threadPoolSize, fsLimitsMinBlockSize, bytesPerChecksum).concurrentCopyFromLocal(fs, srcFile, dstPath);
        }
        else {
            fs.copyFromLocalFile(false, srcPath, dstPath);
        }
    }
    
    @VisibleForTesting
    protected void checkIfSourceFilesExist(final File srcFile) throws IOException {
        if (!srcFile.exists()) {
            throw new IOException(srcFile + " cannot be found");
        }
    }
    
    private static void logError(final String errorMessage, final Throwable ex) {
        System.err.println();
        System.err.println("Error: " + errorMessage);
        System.err.println();
        System.err.println("Stack trace for the error was (for debug purposes):");
        System.err.println("--------------------------------------");
        ex.printStackTrace(System.err);
        System.err.println("--------------------------------------");
        System.err.println();
    }
    
    public String getTimestampDirectory() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        final Date date = new Date();
        return dateFormat.format(date).toString();
    }
    
    static {
        HELP_INFO = new String[] { "", "OozieSharelibCLI creates or upgrade sharelib for oozie" };
        NEW_LINE = System.lineSeparator();
        EXTRALIBS_USAGE = "Extra sharelib resources. This option requires a pair of sharelibname and coma-separated list of pathnames in the following format:" + OozieSharelibCLI.NEW_LINE + "\"sharelib_name=pathname[,pathname...]\"" + OozieSharelibCLI.NEW_LINE + "Caveats:" + OozieSharelibCLI.NEW_LINE + "* Each pathname is either a directory or a regular file (compressed files are not extracted prior to the upload operation)." + OozieSharelibCLI.NEW_LINE + "* Sharelibname shall be specified only once." + OozieSharelibCLI.NEW_LINE + OozieSharelibCLI.NEW_LINE + "* Do not upload multiple conflicting library versions for an extra sharelib directory as it may cause runtime issues." + OozieSharelibCLI.NEW_LINE + "This option can be present multiple times, in case of more than one sharelib" + OozieSharelibCLI.NEW_LINE + "Example command:" + OozieSharelibCLI.NEW_LINE + OozieSharelibCLI.NEW_LINE + "$ oozie-setup.sh sharelib create -fs hdfs://localhost:9000 -locallib oozie-sharelib.tar.gz -extralib share2=dir2,file2 -extralib share3=file3";
    }
    
    @VisibleForTesting
    static final class CopyTaskConfiguration
    {
        private final FileSystem fs;
        private final File srcFile;
        private final Path dstPath;
        
        CopyTaskConfiguration(final FileSystem fs, final File srcFile, final Path dstPath) {
            this.fs = fs;
            this.srcFile = srcFile;
            this.dstPath = dstPath;
        }
        
        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            final CopyTaskConfiguration that = (CopyTaskConfiguration)o;
            return this.srcFile.equals(that.srcFile) && this.dstPath.equals((Object)that.dstPath);
        }
        
        @Override
        public int hashCode() {
            int result = this.srcFile.hashCode();
            result = 31 * result + this.dstPath.hashCode();
            return result;
        }
    }
    
    @VisibleForTesting
    static final class BlockSizeCalculator
    {
        protected static long getValidBlockSize(final long fileLenght, final long fsLimitsMinBlockSize, final long bytesPerChecksum) {
            if (fsLimitsMinBlockSize > fileLenght) {
                return fsLimitsMinBlockSize;
            }
            if (fileLenght % bytesPerChecksum == 0L) {
                return fileLenght;
            }
            final long ratio = fileLenght / bytesPerChecksum;
            return (ratio + 1L) * bytesPerChecksum;
        }
    }
    
    @VisibleForTesting
    static final class CopyTaskCallable implements Callable<CopyTaskConfiguration>
    {
        private static final short REPLICATION_FACTOR = 3;
        private final FileSystem fileSystem;
        private final File file;
        private final Path destinationPath;
        private final Path targetName;
        private final long blockSize;
        private final Set<CopyTaskConfiguration> failedCopyTasks;
        
        CopyTaskCallable(final CopyTaskConfiguration copyTask, final File file, final Path trgName, final long blockSize, final Set<CopyTaskConfiguration> failedCopyTasks) {
            Preconditions.checkNotNull((Object)copyTask);
            Preconditions.checkNotNull((Object)file);
            Preconditions.checkNotNull((Object)trgName);
            Preconditions.checkNotNull((Object)failedCopyTasks);
            Preconditions.checkNotNull((Object)copyTask.dstPath);
            Preconditions.checkNotNull((Object)copyTask.fs);
            this.file = file;
            this.destinationPath = copyTask.dstPath;
            this.failedCopyTasks = failedCopyTasks;
            this.fileSystem = copyTask.fs;
            this.blockSize = blockSize;
            this.targetName = trgName;
        }
        
        @Override
        public CopyTaskConfiguration call() throws Exception {
            final CopyTaskConfiguration cp = new CopyTaskConfiguration(this.fileSystem, this.file, this.targetName);
            this.failedCopyTasks.add(cp);
            final Path destinationFilePath = new Path(this.destinationPath + File.separator + this.file.getName());
            final boolean overwrite = true;
            final int bufferSize = 4096;
            try (final FSDataOutputStream out = this.fileSystem.create(destinationFilePath, true, 4096, (short)3, this.blockSize)) {
                Files.copy(this.file.toPath(), (OutputStream)out);
            }
            return cp;
        }
    }
    
    @VisibleForTesting
    static final class ConcurrentCopyFromLocal
    {
        private static final int DEFAULT_RETRY_COUNT = 5;
        private static final int STARTING_RETRY_DELAY_IN_MS = 1000;
        private int retryCount;
        private int retryDelayInMs;
        private long fsLimitsMinBlockSize;
        private long bytesPerChecksum;
        private final int threadPoolSize;
        private final ExecutorService threadPool;
        private final Set<CopyTaskConfiguration> failedCopyTasks;
        
        public ConcurrentCopyFromLocal(final int threadPoolSize, final long fsLimitsMinBlockSize, final long bytesPerChecksum) {
            this.failedCopyTasks = (Set<CopyTaskConfiguration>)new ConcurrentHashSet();
            Preconditions.checkArgument(threadPoolSize > 0, (Object)"Thread Pool size must be greater than 0");
            Preconditions.checkArgument(fsLimitsMinBlockSize > 0L, (Object)"Minimun block size must be greater than 0");
            Preconditions.checkArgument(bytesPerChecksum > 0L, (Object)"Bytes per checksum must be greater than 0");
            this.bytesPerChecksum = bytesPerChecksum;
            this.fsLimitsMinBlockSize = fsLimitsMinBlockSize;
            this.threadPoolSize = threadPoolSize;
            this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
            this.retryCount = 5;
            this.retryDelayInMs = 1000;
        }
        
        @VisibleForTesting
        void concurrentCopyFromLocal(final FileSystem fs, final File srcFile, final Path dstPath) throws IOException {
            List<Future<CopyTaskConfiguration>> futures = Collections.emptyList();
            final CopyTaskConfiguration copyTask = new CopyTaskConfiguration(fs, srcFile, dstPath);
            try {
                futures = this.copyFolderRecursively(copyTask);
                System.out.println("Running " + futures.size() + " copy tasks on " + this.threadPoolSize + " threads");
            }
            finally {
                this.checkCopyResults(futures);
                System.out.println("Copy tasks are done");
                this.threadPool.shutdown();
            }
        }
        
        private List<Future<CopyTaskConfiguration>> copyFolderRecursively(final CopyTaskConfiguration copyTask) {
            final List<Future<CopyTaskConfiguration>> taskList = new ArrayList<Future<CopyTaskConfiguration>>();
            final File[] fileList = copyTask.srcFile.listFiles();
            if (fileList != null) {
                for (final File file : fileList) {
                    final Path trgName = new Path(copyTask.dstPath, file.getName());
                    if (file.isDirectory()) {
                        taskList.addAll(this.copyFolderRecursively(new CopyTaskConfiguration(copyTask.fs, file, trgName)));
                    }
                    else {
                        final long blockSize = BlockSizeCalculator.getValidBlockSize(file.length(), this.fsLimitsMinBlockSize, this.bytesPerChecksum);
                        taskList.add(this.threadPool.submit((Callable<CopyTaskConfiguration>)new CopyTaskCallable(copyTask, file, trgName, blockSize, this.failedCopyTasks)));
                    }
                }
            }
            return taskList;
        }
        
        private void checkCopyResults(final List<Future<CopyTaskConfiguration>> futures) throws IOException {
            boolean exceptionOccurred = false;
            for (final Future<CopyTaskConfiguration> future : futures) {
                try {
                    final CopyTaskConfiguration cp = future.get();
                    if (cp == null) {
                        continue;
                    }
                    this.failedCopyTasks.remove(cp);
                }
                catch (CancellationException ce) {
                    exceptionOccurred = true;
                    logError("Copy task was cancelled", ce);
                }
                catch (ExecutionException ee) {
                    exceptionOccurred = true;
                    logError("Copy task failed with exception", ee.getCause());
                }
                catch (InterruptedException ie) {
                    exceptionOccurred = true;
                    Thread.currentThread().interrupt();
                }
            }
            if (exceptionOccurred) {
                System.err.println("At least one copy task failed with exception. Retrying failed copy tasks.");
                this.retryFailedCopyTasks();
                if (!this.failedCopyTasks.isEmpty() && this.retryCount == 0) {
                    throw new IOException("At least one copy task failed with exception");
                }
            }
        }
        
        private void retryFailedCopyTasks() throws IOException {
            while (this.retryCount > 0 && !this.failedCopyTasks.isEmpty()) {
                try {
                    System.err.println("Waiting " + this.retryDelayInMs + " ms before retrying failed copy tasks.");
                    Thread.sleep(this.retryDelayInMs);
                    this.retryDelayInMs *= 2;
                }
                catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                }
                for (final CopyTaskConfiguration cp : this.failedCopyTasks) {
                    System.err.println("Retrying to copy " + cp.srcFile + " to " + cp.dstPath);
                    try {
                        this.copyFromLocalFile(cp);
                        this.failedCopyTasks.remove(cp);
                    }
                    catch (IOException e2) {
                        System.err.printf("Copying [%s] to [%s] failed with exception: [%s]%n. Proceed to next file.%n", cp.srcFile, cp.dstPath, e2.getMessage());
                    }
                }
                --this.retryCount;
            }
            if (!this.failedCopyTasks.isEmpty() && this.retryCount == 0) {
                throw new IOException("Could not install Oozie ShareLib properly.");
            }
        }
        
        private void copyFromLocalFile(final CopyTaskConfiguration cp) throws IOException {
            final FileSystem fs = cp.fs;
            fs.delete(cp.dstPath, false);
            fs.copyFromLocalFile(false, new Path(cp.srcFile.toURI()), cp.dstPath);
        }
    }
}
