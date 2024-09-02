// 
// Decompiled by Procyon v0.5.36
// 

package io.swagger.codegen.languages;

import java.util.ArrayList;
import io.swagger.codegen.CodegenOperation;
import io.swagger.models.Operation;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.swagger.codegen.SupportingFile;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import io.swagger.codegen.CodegenProperty;
import io.swagger.codegen.CodegenModel;
import io.swagger.codegen.CliOption;

public class JavaJerseyServerCodegen extends AbstractJavaJAXRSServerCodegen
{
    public JavaJerseyServerCodegen() {
        this.outputFolder = "generated-code/JavaJaxRS-Jersey";
        this.apiTemplateFiles.put("apiService.mustache", ".java");
        this.apiTemplateFiles.put("apiServiceImpl.mustache", ".java");
        this.apiTemplateFiles.put("apiServiceFactory.mustache", ".java");
        this.apiTestTemplateFiles.clear();
        this.modelDocTemplateFiles.remove("model_doc.mustache");
        this.apiDocTemplateFiles.remove("api_doc.mustache");
        final String s = "JavaJaxRS";
        this.templateDir = s;
        this.embeddedTemplateDir = s;
        final CliOption library = new CliOption("library", "library template (sub-template) to use");
        this.supportedLibraries.put("jersey1", "Jersey core 1.x");
        this.supportedLibraries.put("jersey2", "Jersey core 2.x (default)");
        library.setEnum(this.supportedLibraries);
        library.setDefault("jersey1");
        this.cliOptions.add(library);
    }
    
    @Override
    public String getName() {
        return "jaxrs";
    }
    
    @Override
    public String getHelp() {
        return "Generates a Java JAXRS Server application based on Jersey framework.";
    }
    
    @Override
    public void postProcessModelProperty(final CodegenModel model, final CodegenProperty property) {
        super.postProcessModelProperty(model, property);
        if ("null".equals(property.example)) {
            property.example = null;
        }
        if (!BooleanUtils.toBoolean(model.isEnum)) {
            model.imports.add("JsonProperty");
            if (BooleanUtils.toBoolean(model.hasEnums)) {
                model.imports.add("JsonValue");
            }
        }
    }
    
    @Override
    public void processOpts() {
        super.processOpts();
        if (StringUtils.isEmpty((CharSequence)this.library)) {
            this.setLibrary("jersey2");
        }
        if (this.additionalProperties.containsKey("implFolder")) {
            this.implFolder = this.additionalProperties.get("implFolder");
        }
        if ("joda".equals(this.dateLibrary)) {
            this.supportingFiles.add(new SupportingFile("JodaDateTimeProvider.mustache", (this.sourceFolder + '/' + this.apiPackage).replace(".", "/"), "JodaDateTimeProvider.java"));
            this.supportingFiles.add(new SupportingFile("JodaLocalDateProvider.mustache", (this.sourceFolder + '/' + this.apiPackage).replace(".", "/"), "JodaLocalDateProvider.java"));
        }
        else if (this.dateLibrary.startsWith("java8")) {
            this.supportingFiles.add(new SupportingFile("OffsetDateTimeProvider.mustache", (this.sourceFolder + '/' + this.apiPackage).replace(".", "/"), "OffsetDateTimeProvider.java"));
            this.supportingFiles.add(new SupportingFile("LocalDateProvider.mustache", (this.sourceFolder + '/' + this.apiPackage).replace(".", "/"), "LocalDateProvider.java"));
        }
        this.writeOptional(this.outputFolder, new SupportingFile("pom.mustache", "", "pom.xml"));
        this.writeOptional(this.outputFolder, new SupportingFile("README.mustache", "", "README.md"));
        this.supportingFiles.add(new SupportingFile("ApiException.mustache", (this.sourceFolder + '/' + this.apiPackage).replace(".", "/"), "ApiException.java"));
        this.supportingFiles.add(new SupportingFile("ApiOriginFilter.mustache", (this.sourceFolder + '/' + this.apiPackage).replace(".", "/"), "ApiOriginFilter.java"));
        this.supportingFiles.add(new SupportingFile("ApiResponseMessage.mustache", (this.sourceFolder + '/' + this.apiPackage).replace(".", "/"), "ApiResponseMessage.java"));
        this.supportingFiles.add(new SupportingFile("NotFoundException.mustache", (this.sourceFolder + '/' + this.apiPackage).replace(".", "/"), "NotFoundException.java"));
        this.supportingFiles.add(new SupportingFile("jacksonJsonProvider.mustache", (this.sourceFolder + '/' + this.apiPackage).replace(".", "/"), "JacksonJsonProvider.java"));
        this.writeOptional(this.outputFolder, new SupportingFile("bootstrap.mustache", (this.implFolder + '/' + this.apiPackage).replace(".", "/"), "Bootstrap.java"));
        this.writeOptional(this.outputFolder, new SupportingFile("web.mustache", "src/main/webapp/WEB-INF", "web.xml"));
        this.supportingFiles.add(new SupportingFile("StringUtil.mustache", (this.sourceFolder + '/' + this.apiPackage).replace(".", "/"), "StringUtil.java"));
    }
    
    @Override
    public Map<String, Object> postProcessModelsEnum(Map<String, Object> objs) {
        objs = super.postProcessModelsEnum(objs);
        final List<Map<String, String>> imports = objs.get("imports");
        final List<Object> models = objs.get("models");
        for (final Object _mo : models) {
            final Map<String, Object> mo = (Map<String, Object>)_mo;
            final CodegenModel cm = mo.get("model");
            if (Boolean.TRUE.equals(cm.isEnum) && cm.allowableValues != null) {
                cm.imports.add(this.importMapping.get("JsonValue"));
                final Map<String, String> item = new HashMap<String, String>();
                item.put("import", this.importMapping.get("JsonValue"));
                imports.add(item);
            }
        }
        return objs;
    }
    
    @Override
    public void addOperationToGroup(final String tag, final String resourcePath, final Operation operation, final CodegenOperation co, final Map<String, List<CodegenOperation>> operations) {
        String basePath = resourcePath;
        if (basePath.startsWith("/")) {
            basePath = basePath.substring(1);
        }
        final int pos = basePath.indexOf("/");
        if (pos > 0) {
            basePath = basePath.substring(0, pos);
        }
        if (basePath == "") {
            basePath = "default";
        }
        else {
            if (co.path.startsWith("/" + basePath)) {
                co.path = co.path.substring(("/" + basePath).length());
            }
            co.subresourceOperation = !co.path.isEmpty();
        }
        List<CodegenOperation> opList = operations.get(basePath);
        if (opList == null) {
            opList = new ArrayList<CodegenOperation>();
            operations.put(basePath, opList);
        }
        opList.add(co);
        co.baseName = basePath;
    }
}
