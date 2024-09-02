// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.solr.core;

import java.util.concurrent.ConcurrentSkipListSet;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.apache.solr.common.SolrException;
import org.apache.commons.exec.OS;
import java.io.File;
import javax.naming.Context;
import java.nio.file.Paths;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.InitialContext;
import java.nio.file.Path;
import java.util.Set;
import org.slf4j.Logger;

public final class SolrPaths
{
    private static final Logger log;
    private static final Set<String> loggedOnce;
    
    private SolrPaths() {
    }
    
    @Deprecated
    public static Path locateSolrHome() {
        String home = null;
        try {
            final Context c = new InitialContext();
            home = (String)c.lookup("java:comp/env/solr/home");
            logOnceInfo("home_using_jndi", "Using JNDI solr.home: " + home);
        }
        catch (NoInitialContextException e) {
            SolrPaths.log.debug("JNDI not configured for solr (NoInitialContextEx)");
        }
        catch (NamingException e2) {
            SolrPaths.log.debug("No /solr/home in JNDI");
        }
        catch (RuntimeException ex) {
            SolrPaths.log.warn("Odd RuntimeException while testing for JNDI: ", (Throwable)ex);
        }
        if (home == null) {
            final String prop = "solr.solr.home";
            home = System.getProperty(prop);
            if (home != null) {
                logOnceInfo("home_using_sysprop", "Using system property " + prop + ": " + home);
            }
        }
        if (home == null) {
            home = "solr/";
            logOnceInfo("home_default", "solr home defaulted to '" + home + "' (could not find system property or JNDI)");
        }
        return Paths.get(home, new String[0]);
    }
    
    public static String normalizeDir(final String path) {
        return (path != null && !path.endsWith("/") && !path.endsWith("\\")) ? (path + File.separator) : path;
    }
    
    private static void logOnceInfo(final String key, final String msg) {
        if (!SolrPaths.loggedOnce.contains(key)) {
            SolrPaths.loggedOnce.add(key);
            SolrPaths.log.info(msg);
        }
    }
    
    public static void assertPathAllowed(final Path pathToAssert, final Set<Path> allowPaths) throws SolrException {
        if (pathToAssert == null) {
            return;
        }
        if (OS.isFamilyWindows() && pathToAssert.toString().startsWith("\\\\")) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Path " + pathToAssert + " disallowed. UNC paths not supported. Please use drive letter instead.");
        }
        final Path path = Paths.get(pathToAssert.toString(), new String[0]).normalize();
        if (path.startsWith("..")) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Path " + pathToAssert + " disallowed due to path traversal..");
        }
        if (!path.isAbsolute()) {
            return;
        }
        if (allowPaths.contains(Paths.get("_ALL_", new String[0]))) {
            return;
        }
        if (allowPaths.stream().noneMatch(p -> path.startsWith(Paths.get(p.toString(), new String[0])))) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Path " + path + " must be relative to SOLR_HOME, SOLR_DATA_HOME coreRootDirectory. Set system property 'solr.allowPaths' to add other allowed paths.");
        }
    }
    
    static {
        log = LoggerFactory.getLogger((Class)MethodHandles.lookup().lookupClass());
        loggedOnce = new ConcurrentSkipListSet<String>();
    }
}
