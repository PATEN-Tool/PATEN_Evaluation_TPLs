// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.struts2.rest;

import com.opensymphony.xwork2.util.logging.LoggerFactory;
import java.util.Iterator;
import com.opensymphony.xwork2.config.Configuration;
import com.opensymphony.xwork2.config.entities.PackageConfig;
import java.util.Map;
import java.util.HashMap;
import org.apache.struts2.RequestUtils;
import org.apache.struts2.dispatcher.mapper.ActionMapping;
import com.opensymphony.xwork2.config.ConfigurationManager;
import javax.servlet.http.HttpServletRequest;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.util.logging.Logger;
import org.apache.struts2.dispatcher.mapper.DefaultActionMapper;

public class RestActionMapper extends DefaultActionMapper
{
    protected static final Logger LOG;
    public static final String HTTP_METHOD_PARAM = "_method";
    private String idParameterName;
    private String indexMethodName;
    private String getMethodName;
    private String postMethodName;
    private String editMethodName;
    private String newMethodName;
    private String deleteMethodName;
    private String putMethodName;
    private String optionsMethodName;
    private String postContinueMethodName;
    private String putContinueMethodName;
    private boolean allowDynamicMethodCalls;
    
    public RestActionMapper() {
        this.idParameterName = "id";
        this.indexMethodName = "index";
        this.getMethodName = "show";
        this.postMethodName = "create";
        this.editMethodName = "edit";
        this.newMethodName = "editNew";
        this.deleteMethodName = "destroy";
        this.putMethodName = "update";
        this.optionsMethodName = "options";
        this.postContinueMethodName = "createContinue";
        this.putContinueMethodName = "updateContinue";
        this.allowDynamicMethodCalls = false;
        this.defaultMethodName = this.indexMethodName;
    }
    
    public String getIdParameterName() {
        return this.idParameterName;
    }
    
    @Inject(required = false, value = "struts.mapper.idParameterName")
    public void setIdParameterName(final String idParameterName) {
        this.idParameterName = idParameterName;
    }
    
    @Inject(required = false, value = "struts.mapper.indexMethodName")
    public void setIndexMethodName(final String indexMethodName) {
        this.indexMethodName = indexMethodName;
    }
    
    @Inject(required = false, value = "struts.mapper.getMethodName")
    public void setGetMethodName(final String getMethodName) {
        this.getMethodName = getMethodName;
    }
    
    @Inject(required = false, value = "struts.mapper.postMethodName")
    public void setPostMethodName(final String postMethodName) {
        this.postMethodName = postMethodName;
    }
    
    @Inject(required = false, value = "struts.mapper.editMethodName")
    public void setEditMethodName(final String editMethodName) {
        this.editMethodName = editMethodName;
    }
    
    @Inject(required = false, value = "struts.mapper.newMethodName")
    public void setNewMethodName(final String newMethodName) {
        this.newMethodName = newMethodName;
    }
    
    @Inject(required = false, value = "struts.mapper.deleteMethodName")
    public void setDeleteMethodName(final String deleteMethodName) {
        this.deleteMethodName = deleteMethodName;
    }
    
    @Inject(required = false, value = "struts.mapper.putMethodName")
    public void setPutMethodName(final String putMethodName) {
        this.putMethodName = putMethodName;
    }
    
    @Inject(required = false, value = "struts.mapper.optionsMethodName")
    public void setOptionsMethodName(final String optionsMethodName) {
        this.optionsMethodName = optionsMethodName;
    }
    
    @Inject(required = false, value = "struts.mapper.postContinueMethodName")
    public void setPostContinueMethodName(final String postContinueMethodName) {
        this.postContinueMethodName = postContinueMethodName;
    }
    
    @Inject(required = false, value = "struts.mapper.putContinueMethodName")
    public void setPutContinueMethodName(final String putContinueMethodName) {
        this.putContinueMethodName = putContinueMethodName;
    }
    
    @Inject(required = false, value = "struts.enable.DynamicMethodInvocation")
    public void setAllowDynamicMethodCalls(final String allowDynamicMethodCalls) {
        this.allowDynamicMethodCalls = "true".equalsIgnoreCase(allowDynamicMethodCalls);
    }
    
    public ActionMapping getMapping(final HttpServletRequest request, final ConfigurationManager configManager) {
        final ActionMapping mapping = new ActionMapping();
        String uri = RequestUtils.getUri(request);
        uri = this.dropExtension(uri, mapping);
        if (uri == null) {
            return null;
        }
        this.parseNameAndNamespace(uri, mapping, configManager);
        this.handleSpecialParameters(request, mapping);
        if (mapping.getName() == null) {
            return null;
        }
        this.handleDynamicMethodInvocation(mapping, mapping.getName());
        String fullName = mapping.getName();
        if (fullName != null && fullName.length() > 0) {
            final int scPos = fullName.indexOf(59);
            if (scPos > -1 && !"edit".equals(fullName.substring(scPos + 1))) {
                fullName = fullName.substring(0, scPos);
            }
            int lastSlashPos = fullName.lastIndexOf(47);
            String id = null;
            if (lastSlashPos > -1) {
                final int prevSlashPos = fullName.lastIndexOf(47, lastSlashPos - 1);
                if (prevSlashPos > -1 && mapping.getMethod() == null) {
                    mapping.setMethod(fullName.substring(lastSlashPos + 1));
                    fullName = fullName.substring(0, lastSlashPos);
                    lastSlashPos = prevSlashPos;
                }
                id = fullName.substring(lastSlashPos + 1);
            }
            if (mapping.getMethod() == null) {
                if (this.isOptions(request)) {
                    mapping.setMethod(this.optionsMethodName);
                }
                else if (lastSlashPos == -1 || lastSlashPos == fullName.length() - 1) {
                    if (this.isGet(request)) {
                        mapping.setMethod(this.indexMethodName);
                    }
                    else if (this.isPost(request)) {
                        if (this.isExpectContinue(request)) {
                            mapping.setMethod(this.postContinueMethodName);
                        }
                        else {
                            mapping.setMethod(this.postMethodName);
                        }
                    }
                }
                else if (id != null) {
                    if (this.isGet(request) && id.endsWith(";edit")) {
                        id = id.substring(0, id.length() - ";edit".length());
                        mapping.setMethod(this.editMethodName);
                    }
                    else if (this.isGet(request) && "new".equals(id)) {
                        mapping.setMethod(this.newMethodName);
                    }
                    else if (this.isDelete(request)) {
                        mapping.setMethod(this.deleteMethodName);
                    }
                    else if (this.isGet(request)) {
                        mapping.setMethod(this.getMethodName);
                    }
                    else if (this.isPut(request)) {
                        if (this.isExpectContinue(request)) {
                            mapping.setMethod(this.putContinueMethodName);
                        }
                        else {
                            mapping.setMethod(this.putMethodName);
                        }
                    }
                }
            }
            if (id != null) {
                if (!"new".equals(id)) {
                    if (mapping.getParams() == null) {
                        mapping.setParams((Map)new HashMap());
                    }
                    mapping.getParams().put(this.idParameterName, new String[] { id });
                }
                fullName = fullName.substring(0, lastSlashPos);
            }
            mapping.setName(this.cleanupActionName(fullName));
        }
        return mapping;
    }
    
    private void handleDynamicMethodInvocation(final ActionMapping mapping, final String name) {
        final int exclamation = name.lastIndexOf("!");
        if (exclamation != -1) {
            String actionName = name.substring(0, exclamation);
            String actionMethod = name.substring(exclamation + 1);
            final int scPos = actionMethod.indexOf(59);
            if (scPos != -1) {
                actionName += actionMethod.substring(scPos);
                actionMethod = actionMethod.substring(0, scPos);
            }
            mapping.setName(actionName);
            if (this.allowDynamicMethodCalls) {
                mapping.setMethod(this.cleanupMethodName(actionMethod));
            }
            else {
                mapping.setMethod((String)null);
            }
        }
    }
    
    protected void parseNameAndNamespace(final String uri, final ActionMapping mapping, final ConfigurationManager configManager) {
        final int lastSlash = uri.lastIndexOf("/");
        String namespace;
        String name;
        if (lastSlash == -1) {
            namespace = "";
            name = uri;
        }
        else if (lastSlash == 0) {
            namespace = "/";
            name = uri.substring(lastSlash + 1);
        }
        else {
            final Configuration config = configManager.getConfiguration();
            final String prefix = uri.substring(0, lastSlash);
            namespace = "";
            for (final Object o : config.getPackageConfigs().values()) {
                final String ns = ((PackageConfig)o).getNamespace();
                if (ns != null && prefix.startsWith(ns) && (prefix.length() == ns.length() || prefix.charAt(ns.length()) == '/') && ns.length() > namespace.length()) {
                    namespace = ns;
                }
            }
            name = uri.substring(namespace.length() + 1);
        }
        mapping.setNamespace(namespace);
        mapping.setName(name);
    }
    
    protected boolean isGet(final HttpServletRequest request) {
        return "get".equalsIgnoreCase(request.getMethod());
    }
    
    protected boolean isPost(final HttpServletRequest request) {
        return "post".equalsIgnoreCase(request.getMethod());
    }
    
    protected boolean isPut(final HttpServletRequest request) {
        return "put".equalsIgnoreCase(request.getMethod()) || (this.isPost(request) && "put".equalsIgnoreCase(request.getParameter("_method")));
    }
    
    protected boolean isDelete(final HttpServletRequest request) {
        return "delete".equalsIgnoreCase(request.getMethod()) || "delete".equalsIgnoreCase(request.getParameter("_method"));
    }
    
    protected boolean isOptions(final HttpServletRequest request) {
        return "options".equalsIgnoreCase(request.getMethod());
    }
    
    protected boolean isExpectContinue(final HttpServletRequest request) {
        final String expect = request.getHeader("Expect");
        return expect != null && expect.toLowerCase().contains("100-continue");
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)RestActionMapper.class);
    }
}
