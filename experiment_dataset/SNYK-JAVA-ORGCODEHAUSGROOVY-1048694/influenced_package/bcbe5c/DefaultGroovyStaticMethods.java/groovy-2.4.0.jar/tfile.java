// 
// Decompiled by Procyon v0.5.36
// 

package org.codehaus.groovy.runtime;

import java.io.IOException;
import java.io.File;
import org.codehaus.groovy.reflection.ReflectionUtils;
import java.util.ResourceBundle;
import java.util.Locale;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import java.util.regex.Matcher;
import groovy.lang.Closure;

public class DefaultGroovyStaticMethods
{
    public static Thread start(final Thread self, final Closure closure) {
        return createThread(null, false, closure);
    }
    
    public static Thread start(final Thread self, final String name, final Closure closure) {
        return createThread(name, false, closure);
    }
    
    public static Thread startDaemon(final Thread self, final Closure closure) {
        return createThread(null, true, closure);
    }
    
    public static Thread startDaemon(final Thread self, final String name, final Closure closure) {
        return createThread(name, true, closure);
    }
    
    private static Thread createThread(final String name, final boolean daemon, final Closure closure) {
        final Thread thread = (name != null) ? new Thread(closure, name) : new Thread(closure);
        if (daemon) {
            thread.setDaemon(true);
        }
        thread.start();
        return thread;
    }
    
    public static Matcher getLastMatcher(final Matcher self) {
        return RegexSupport.getLastMatcher();
    }
    
    private static void sleepImpl(final long millis, final Closure closure) {
        final long start = System.currentTimeMillis();
        long rest = millis;
        while (rest > 0L) {
            try {
                Thread.sleep(rest);
                rest = 0L;
            }
            catch (InterruptedException e) {
                if (closure != null && DefaultTypeTransformation.castToBoolean(closure.call(e))) {
                    return;
                }
                final long current = System.currentTimeMillis();
                rest = millis + start - current;
            }
        }
    }
    
    public static void sleep(final Object self, final long milliseconds) {
        sleepImpl(milliseconds, null);
    }
    
    public static void sleep(final Object self, final long milliseconds, final Closure onInterrupt) {
        sleepImpl(milliseconds, onInterrupt);
    }
    
    public static Date parse(final Date self, final String format, final String input) throws ParseException {
        return new SimpleDateFormat(format).parse(input);
    }
    
    public static Date parseToStringDate(final Date self, final String dateToString) throws ParseException {
        return new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US).parse(dateToString);
    }
    
    public static ResourceBundle getBundle(final ResourceBundle self, final String bundleName) {
        return getBundle(self, bundleName, Locale.getDefault());
    }
    
    public static ResourceBundle getBundle(final ResourceBundle self, final String bundleName, final Locale locale) {
        final Class c = ReflectionUtils.getCallingClass();
        ClassLoader targetCL = (c != null) ? c.getClassLoader() : null;
        if (targetCL == null) {
            targetCL = ClassLoader.getSystemClassLoader();
        }
        return ResourceBundle.getBundle(bundleName, locale, targetCL);
    }
    
    public static File createTempDir(final File self) throws IOException {
        return createTempDir(self, "groovy-generated-", "-tmpdir");
    }
    
    public static File createTempDir(final File self, final String prefix, final String suffix) throws IOException {
        final int MAXTRIES = 3;
        int accessDeniedCounter = 0;
        File tempFile = null;
        int i = 0;
        while (i < 3) {
            try {
                tempFile = File.createTempFile(prefix, suffix);
                tempFile.delete();
                tempFile.mkdirs();
            }
            catch (IOException ioe) {
                if (ioe.getMessage().startsWith("Access is denied")) {
                    ++accessDeniedCounter;
                    try {
                        Thread.sleep(100L);
                    }
                    catch (InterruptedException ex) {}
                }
                if (i != 2) {
                    ++i;
                    continue;
                }
                if (accessDeniedCounter == 3) {
                    final String msg = "Access is denied.\nWe tried " + accessDeniedCounter + " times to create a temporary directory" + " and failed each time. If you are on Windows" + " you are possibly victim to" + " http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6325169. " + " this is no bug in Groovy.";
                    throw new IOException(msg);
                }
                throw ioe;
            }
            break;
        }
        return tempFile;
    }
    
    public static long currentTimeSeconds(final System self) {
        return System.currentTimeMillis() / 1000L;
    }
}
