// 
// Decompiled by Procyon v0.5.36
// 

package io.swagger.codegen.languages;

import java.util.ArrayList;
import java.util.List;
import io.swagger.codegen.CodegenOperation;
import io.swagger.models.Operation;
import io.swagger.codegen.SupportingFile;
import io.swagger.codegen.CodegenProperty;
import io.swagger.codegen.CodegenModel;
import java.util.Map;
import java.util.LinkedHashMap;
import io.swagger.codegen.CliOption;
import java.io.File;

public class JavaJerseyServerCodegen extends AbstractJavaJAXRSServerCodegen
{
    public JavaJerseyServerCodegen() {
        this.sourceFolder = "src/gen/java";
        this.invokerPackage = "io.swagger.api";
        this.artifactId = "swagger-jaxrs-server";
        this.outputFolder = "generated-code/JavaJaxRS-Jersey";
        this.modelTemplateFiles.put("model.mustache", ".java");
        this.apiTemplateFiles.put("api.mustache", ".java");
        this.apiTemplateFiles.put("apiService.mustache", ".java");
        this.apiTemplateFiles.put("apiServiceImpl.mustache", ".java");
        this.apiTemplateFiles.put("apiServiceFactory.mustache", ".java");
        this.apiPackage = "io.swagger.api";
        this.modelPackage = "io.swagger.model";
        this.additionalProperties.put("title", this.title);
        final String string = "JavaJaxRS" + File.separator + "jersey1_18";
        this.templateDir = string;
        this.embeddedTemplateDir = string;
        for (int i = 0; i < this.cliOptions.size(); ++i) {
            if ("library".equals(this.cliOptions.get(i).getOpt())) {
                this.cliOptions.remove(i);
                break;
            }
        }
        final CliOption library = new CliOption("library", "library template (sub-template) to use");
        library.setDefault("<default>");
        final Map<String, String> supportedLibraries = new LinkedHashMap<String, String>();
        supportedLibraries.put("<default>", "Jersey core 1.18.1");
        supportedLibraries.put("jersey2", "Jersey core 2.x");
        library.setEnum(supportedLibraries);
        this.cliOptions.add(library);
        this.cliOptions.add(new CliOption("implFolder", "folder for generated implementation code"));
        this.cliOptions.add(new CliOption("title", "a title describing the application"));
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
    }
    
    @Override
    public void processOpts() {
        super.processOpts();
        if (this.additionalProperties.containsKey("implFolder")) {
            this.implFolder = this.additionalProperties.get("implFolder");
        }
        this.supportingFiles.clear();
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
        if (this.additionalProperties.containsKey("dateLibrary")) {
            this.setDateLibrary(this.additionalProperties.get("dateLibrary").toString());
            this.additionalProperties.put(this.dateLibrary, "true");
        }
        if ("<default>".equals(this.library) || this.library == null) {
            if (this.templateDir.startsWith("JavaJaxRS")) {
                this.templateDir = "JavaJaxRS" + File.separator + "jersey1_18";
            }
            else {
                this.templateDir = this.templateDir + File.separator + "jersey1_18";
            }
        }
        if ("jersey2".equals(this.library)) {
            if (this.templateDir.startsWith("JavaJaxRS")) {
                this.templateDir = "JavaJaxRS" + File.separator + "jersey2";
            }
            else {
                this.templateDir = this.templateDir + File.separator + "jersey2";
            }
        }
        if ("joda".equals(this.dateLibrary)) {
            this.supportingFiles.add(new SupportingFile("JodaDateTimeProvider.mustache", (this.sourceFolder + '/' + this.apiPackage).replace(".", "/"), "JodaDateTimeProvider.java"));
            this.supportingFiles.add(new SupportingFile("JodaLocalDateProvider.mustache", (this.sourceFolder + '/' + this.apiPackage).replace(".", "/"), "JodaLocalDateProvider.java"));
        }
        else if ("java8".equals(this.dateLibrary)) {
            this.supportingFiles.add(new SupportingFile("LocalDateTimeProvider.mustache", (this.sourceFolder + '/' + this.apiPackage).replace(".", "/"), "LocalDateTimeProvider.java"));
            this.supportingFiles.add(new SupportingFile("LocalDateProvider.mustache", (this.sourceFolder + '/' + this.apiPackage).replace(".", "/"), "LocalDateProvider.java"));
        }
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
    
    public void hideGenerationTimestamp(final boolean hideGenerationTimestamp) {
        this.hideGenerationTimestamp = hideGenerationTimestamp;
    }
}
