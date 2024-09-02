// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.oozie.tools;

import java.util.concurrent.Callable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.Collections;
import java.util.Date;
import java.text.SimpleDateFormat;
import org.apache.hadoop.conf.Configuration;
import java.net.URI;
import java.util.Collection;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.fs.FileSystem;
import org.apache.oozie.service.HadoopAccessorService;
import org.apache.oozie.service.WorkflowAppService;
import org.apache.oozie.service.Services;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileUtil;
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
    public static final String FS_OPT = "fs";
    public static final String CONCURRENCY_OPT = "concurrency";
    public static final String OOZIE_HOME = "oozie.home.dir";
    public static final String SHARE_LIB_PREFIX = "lib_";
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
        return options;
    }
    
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
            if (!srcFile.exists()) {
                throw new IOException(srcPath + " cannot be found");
            }
            if (threadPoolSize > 1) {
                this.concurrentCopyFromLocal(fs, threadPoolSize, srcFile, dstPath);
            }
            else {
                fs.copyFromLocalFile(false, srcPath, dstPath);
            }
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
        catch (Exception ex2) {
            this.logError(ex2.getMessage(), ex2);
            return 1;
        }
    }
    
    private void logError(final String errorMessage, final Throwable ex) {
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
    
    private void concurrentCopyFromLocal(final FileSystem fs, final int threadPoolSize, final File srcFile, final Path dstPath) throws IOException {
        List<Future<Void>> futures = Collections.emptyList();
        final ExecutorService threadPool = Executors.newFixedThreadPool(threadPoolSize);
        try {
            futures = this.copyFolderRecursively(fs, threadPool, srcFile, dstPath);
            System.out.println("Running " + futures.size() + " copy tasks on " + threadPoolSize + " threads");
        }
        finally {
            try {
                threadPool.shutdown();
            }
            finally {
                this.checkCopyResults(futures);
            }
        }
    }
    
    private void checkCopyResults(final List<Future<Void>> futures) throws IOException {
        Throwable t = null;
        for (final Future<Void> future : futures) {
            try {
                future.get();
            }
            catch (CancellationException ce) {
                t = ce;
                this.logError("Copy task was cancelled", ce);
            }
            catch (ExecutionException ee) {
                t = ee.getCause();
                this.logError("Copy task failed with exception", t);
            }
            catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        if (t != null) {
            throw new IOException("At least one copy task failed with exception", t);
        }
    }
    
    private List<Future<Void>> copyFolderRecursively(final FileSystem fs, final ExecutorService threadPool, final File srcFile, final Path dstPath) throws IOException {
        final List<Future<Void>> taskList = new ArrayList<Future<Void>>();
        final File[] files = srcFile.listFiles();
        if (files != null) {
            for (final File file : files) {
                final Path trgName = new Path(dstPath, file.getName());
                if (file.isDirectory()) {
                    taskList.addAll(this.copyFolderRecursively(fs, threadPool, file, trgName));
                }
                else {
                    taskList.add(threadPool.submit((Callable<Void>)new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            fs.copyFromLocalFile(new Path(file.toURI()), trgName);
                            return null;
                        }
                    }));
                }
            }
        }
        else {
            System.out.println("WARNING: directory listing of " + srcFile.getAbsolutePath().toString() + " returned null");
        }
        return taskList;
    }
    
    static {
        HELP_INFO = new String[] { "", "OozieSharelibCLI creates or upgrade sharelib for oozie" };
    }
}
