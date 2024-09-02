// 
// Decompiled by Procyon v0.5.36
// 

package io.hawt.system;

import org.slf4j.LoggerFactory;
import javax.management.JMException;
import javax.management.ReflectionException;
import javax.management.MBeanException;
import javax.management.InstanceNotFoundException;
import java.util.Arrays;
import java.util.HashSet;
import io.hawt.web.proxy.ProxyDetails;
import java.util.Map;
import java.net.InetAddress;
import java.util.Set;
import io.hawt.util.Hosts;
import java.util.Iterator;
import java.util.ArrayList;
import javax.management.MalformedObjectNameException;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Collections;
import io.hawt.util.Strings;
import javax.management.ObjectName;
import javax.management.MBeanServer;
import java.util.regex.Pattern;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;

public class ProxyWhitelist
{
    private static final transient Logger LOG;
    private static final String FABRIC_MBEAN = "io.fabric8:type=Fabric";
    protected CopyOnWriteArraySet<String> whitelist;
    protected List<Pattern> regexWhitelist;
    protected MBeanServer mBeanServer;
    protected ObjectName fabricMBean;
    
    public ProxyWhitelist(final String whitelistStr) {
        this(whitelistStr, true);
    }
    
    public ProxyWhitelist(final String whitelistStr, final boolean probeLocal) {
        if (Strings.isBlank(whitelistStr)) {
            this.whitelist = new CopyOnWriteArraySet<String>();
            this.regexWhitelist = Collections.emptyList();
        }
        else {
            this.whitelist = new CopyOnWriteArraySet<String>(this.filterRegex(Strings.split(whitelistStr, ",")));
            this.regexWhitelist = this.buildRegexWhitelist(Strings.split(whitelistStr, ","));
        }
        if (probeLocal) {
            ProxyWhitelist.LOG.info("Probing local addresses ...");
            this.initialiseWhitelist();
        }
        else {
            ProxyWhitelist.LOG.info("Probing local addresses disabled");
            this.whitelist.add("localhost");
            this.whitelist.add("127.0.0.1");
        }
        ProxyWhitelist.LOG.info("Initial proxy whitelist: {}", (Object)this.whitelist);
        this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            this.fabricMBean = new ObjectName("io.fabric8:type=Fabric");
        }
        catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }
    
    protected List<String> filterRegex(final List<String> whitelist) {
        final List<String> result = new ArrayList<String>();
        for (final String element : whitelist) {
            if (!element.startsWith("r:")) {
                result.add(element);
            }
        }
        return result;
    }
    
    protected List<Pattern> buildRegexWhitelist(final List<String> whitelist) {
        final List<Pattern> patterns = new ArrayList<Pattern>();
        for (final String element : whitelist) {
            if (element.startsWith("r:")) {
                final String regex = element.substring(2);
                patterns.add(Pattern.compile(regex));
            }
        }
        return patterns;
    }
    
    protected void initialiseWhitelist() {
        final Map<String, Set<InetAddress>> localAddresses = (Map<String, Set<InetAddress>>)Hosts.getNetworkInterfaceAddresses(true);
        for (final Set<InetAddress> addresses : localAddresses.values()) {
            for (final InetAddress address : addresses) {
                this.whitelist.add(address.getHostAddress());
                this.whitelist.add(address.getHostName());
                this.whitelist.add(address.getCanonicalHostName());
            }
        }
    }
    
    public boolean isAllowed(final ProxyDetails details) {
        if (details.isAllowed(this.whitelist)) {
            return true;
        }
        ProxyWhitelist.LOG.debug("Updating proxy whitelist: {}, {}", (Object)this.whitelist, (Object)details);
        return (this.update() && details.isAllowed(this.whitelist)) || details.isAllowed(this.regexWhitelist);
    }
    
    public boolean update() {
        if (!this.mBeanServer.isRegistered(this.fabricMBean)) {
            ProxyWhitelist.LOG.debug("Whitelist MBean not available");
            return false;
        }
        final Set<String> newWhitelist = this.invokeMBean();
        final int previousSize = this.whitelist.size();
        this.whitelist.addAll(newWhitelist);
        if (this.whitelist.size() == previousSize) {
            ProxyWhitelist.LOG.debug("No new proxy whitelist to update");
            return false;
        }
        ProxyWhitelist.LOG.info("Updated proxy whitelist: {}", (Object)this.whitelist);
        return true;
    }
    
    protected Set<String> invokeMBean() {
        final Set<String> list = new HashSet<String>();
        try {
            final List<Map<String, Object>> containers = (List<Map<String, Object>>)this.mBeanServer.invoke(this.fabricMBean, "containers", new Object[] { Arrays.asList("localHostname", "localIp", "manualIp", "publicHostname", "publicIp") }, new String[] { List.class.getName() });
            ProxyWhitelist.LOG.debug("Returned containers from MBean: {}", (Object)containers);
            for (final Map<String, Object> container : containers) {
                for (final Object value : container.values()) {
                    if (value != null && Strings.isNotBlank(value.toString())) {
                        list.add(value.toString());
                    }
                }
            }
            ProxyWhitelist.LOG.debug("Extracted whitelist: {}", (Object)list);
        }
        catch (InstanceNotFoundException | MBeanException | ReflectionException ex2) {
            final JMException ex;
            final JMException e = ex;
            ProxyWhitelist.LOG.error("Invocation to whitelist MBean failed: " + e.getMessage(), (Throwable)e);
        }
        return list;
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)ProxyWhitelist.class);
    }
}
