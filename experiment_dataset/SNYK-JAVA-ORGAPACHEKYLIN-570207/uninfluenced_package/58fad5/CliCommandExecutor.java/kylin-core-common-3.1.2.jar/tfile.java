// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.kylin.common.util;

import org.slf4j.LoggerFactory;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;

public class CliCommandExecutor
{
    private static final Logger logger;
    private String remoteHost;
    private int port;
    private String remoteUser;
    private String remotePwd;
    private int remoteTimeoutSeconds;
    public static final String COMMAND_BLOCK_LIST = "[ &`>|{}()$;\\-#~!+*\\\\]+";
    public static final String COMMAND_WHITE_LIST = "[^\\w%,@/:=?.\"\\[\\]]";
    public static final String HIVE_BLOCK_LIST = "[ <>()$;\\-#!+*\"'/=%@]+";
    
    public CliCommandExecutor() {
        this.remoteTimeoutSeconds = 3600;
    }
    
    public void setRunAtRemote(final String host, final int port, final String user, final String pwd) {
        this.remoteHost = host;
        this.port = port;
        this.remoteUser = user;
        this.remotePwd = pwd;
    }
    
    public void setRunAtLocal() {
        this.remoteHost = null;
        this.remoteUser = null;
        this.remotePwd = null;
    }
    
    public void copyFile(final String localFile, final String destDir) throws IOException {
        if (this.remoteHost == null) {
            this.copyNative(localFile, destDir);
        }
        else {
            this.copyRemote(localFile, destDir);
        }
    }
    
    private void copyNative(final String localFile, final String destDir) throws IOException {
        final File src = new File(localFile);
        final File dest = new File(destDir, src.getName());
        FileUtils.copyFile(src, dest);
    }
    
    private void copyRemote(final String localFile, final String destDir) throws IOException {
        final SSHClient ssh = new SSHClient(this.remoteHost, this.port, this.remoteUser, this.remotePwd);
        try {
            ssh.scpFileToRemote(localFile, destDir);
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e2) {
            throw new IOException(e2.getMessage(), e2);
        }
    }
    
    public Pair<Integer, String> execute(final String command) throws IOException {
        return this.execute(command, new SoutLogger());
    }
    
    public Pair<Integer, String> execute(final String command, final org.apache.kylin.common.util.Logger logAppender) throws IOException {
        Pair<Integer, String> r;
        if (this.remoteHost == null) {
            r = this.runNativeCommand(command, logAppender);
        }
        else {
            r = this.runRemoteCommand(command, logAppender);
        }
        if (r.getFirst() != 0) {
            throw new IOException("OS command error exit with return code: " + r.getFirst() + ", error message: " + r.getSecond() + "The command is: \n" + command + ((this.remoteHost == null) ? "" : (" (remoteHost:" + this.remoteHost + ")")));
        }
        return r;
    }
    
    private Pair<Integer, String> runRemoteCommand(final String command, final org.apache.kylin.common.util.Logger logAppender) throws IOException {
        final SSHClient ssh = new SSHClient(this.remoteHost, this.port, this.remoteUser, this.remotePwd);
        try {
            final SSHClientOutput sshOutput = ssh.execCommand(command, this.remoteTimeoutSeconds, logAppender);
            final int exitCode = sshOutput.getExitCode();
            final String output = sshOutput.getText();
            return Pair.newPair(exitCode, output);
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e2) {
            throw new IOException(e2.getMessage(), e2);
        }
    }
    
    private Pair<Integer, String> runNativeCommand(final String command, final org.apache.kylin.common.util.Logger logAppender) throws IOException {
        final String[] cmd = new String[3];
        final String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows")) {
            cmd[0] = "cmd.exe";
            cmd[1] = "/C";
        }
        else {
            cmd[0] = "/bin/bash";
            cmd[1] = "-c";
        }
        cmd[2] = command;
        final ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.redirectErrorStream(true);
        final Process proc = builder.start();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8));
        final StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
            result.append(line).append('\n');
            if (logAppender != null) {
                logAppender.log(line);
            }
        }
        if (Thread.interrupted()) {
            CliCommandExecutor.logger.info("CliCommandExecutor is interruppted by other, kill the sub process: " + command);
            proc.destroy();
            try {
                Thread.sleep(1000L);
            }
            catch (InterruptedException ex) {}
            return Pair.newPair(1, "Killed");
        }
        try {
            final int exitCode = proc.waitFor();
            return Pair.newPair(exitCode, result.toString());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }
    
    public static String checkParameter(final String commandParameter) {
        return checkParameter(commandParameter, "[ &`>|{}()$;\\-#~!+*\\\\]+");
    }
    
    public static String checkParameterWhiteList(final String commandParameter) {
        return checkParameter(commandParameter, "[^\\w%,@/:=?.\"\\[\\]]");
    }
    
    public static String checkHiveProperty(final String hiveProperty) {
        return checkParameter(hiveProperty, "[ <>()$;\\-#!+*\"'/=%@]+");
    }
    
    private static String checkParameter(final String commandParameter, final String rex) {
        final String repaired = commandParameter.replaceAll(rex, "");
        if (repaired.length() != commandParameter.length()) {
            CliCommandExecutor.logger.warn("Detected illegal character in command {} by {} , replace it to {}.", new Object[] { commandParameter, rex, repaired });
        }
        return repaired;
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)CliCommandExecutor.class);
    }
}
