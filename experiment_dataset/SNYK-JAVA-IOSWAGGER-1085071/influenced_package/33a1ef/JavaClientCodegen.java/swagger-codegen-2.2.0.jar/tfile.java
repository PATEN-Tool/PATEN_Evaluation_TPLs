// 
// Decompiled by Procyon v0.5.36
// 

package io.swagger.codegen.languages;

import org.slf4j.LoggerFactory;
import java.util.HashMap;
import org.apache.commons.lang3.BooleanUtils;
import io.swagger.codegen.CodegenProperty;
import io.swagger.codegen.CodegenModel;
import java.util.Iterator;
import io.swagger.codegen.CodegenOperation;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import io.swagger.codegen.SupportingFile;
import io.swagger.codegen.CodegenType;
import io.swagger.codegen.CliOption;
import java.io.File;
import org.slf4j.Logger;

public class JavaClientCodegen extends AbstractJavaCodegen
{
    private static final Logger LOGGER;
    public static final String USE_RX_JAVA = "useRxJava";
    public static final String RETROFIT_1 = "retrofit";
    public static final String RETROFIT_2 = "retrofit2";
    protected String gradleWrapperPackage;
    protected boolean useRxJava;
    
    public JavaClientCodegen() {
        this.gradleWrapperPackage = "gradle.wrapper";
        this.useRxJava = false;
        this.outputFolder = "generated-code" + File.separator + "java";
        final String s = "Java";
        this.templateDir = s;
        this.embeddedTemplateDir = s;
        this.invokerPackage = "io.swagger.client";
        this.artifactId = "swagger-java-client";
        this.apiPackage = "io.swagger.client.api";
        this.modelPackage = "io.swagger.client.model";
        this.cliOptions.add(CliOption.newBoolean("useRxJava", "Whether to use the RxJava adapter with the retrofit2 library."));
        this.supportedLibraries.put("jersey1", "HTTP client: Jersey client 1.19.1. JSON processing: Jackson 2.7.0");
        this.supportedLibraries.put("feign", "HTTP client: Netflix Feign 8.16.0. JSON processing: Jackson 2.7.0");
        this.supportedLibraries.put("jersey2", "HTTP client: Jersey client 2.22.2. JSON processing: Jackson 2.7.0");
        this.supportedLibraries.put("okhttp-gson", "HTTP client: OkHttp 2.7.5. JSON processing: Gson 2.6.2");
        this.supportedLibraries.put("retrofit", "HTTP client: OkHttp 2.7.5. JSON processing: Gson 2.3.1 (Retrofit 1.9.0). IMPORTANT NOTE: retrofit1.x is no longer actively maintained so please upgrade to 'retrofit2' instead.");
        this.supportedLibraries.put("retrofit2", "HTTP client: OkHttp 3.2.0. JSON processing: Gson 2.6.1 (Retrofit 2.0.2). Enable the RxJava adapter using '-DuseRxJava=true'. (RxJava 1.1.3)");
        final CliOption libraryOption = new CliOption("library", "library template (sub-template) to use");
        libraryOption.setEnum(this.supportedLibraries);
        libraryOption.setDefault("okhttp-gson");
        this.cliOptions.add(libraryOption);
        this.setLibrary("okhttp-gson");
    }
    
    @Override
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }
    
    @Override
    public String getName() {
        return "java";
    }
    
    @Override
    public String getHelp() {
        return "Generates a Java client library.";
    }
    
    @Override
    public void processOpts() {
        super.processOpts();
        if (this.additionalProperties.containsKey("useRxJava")) {
            this.setUseRxJava(Boolean.valueOf(this.additionalProperties.get("useRxJava").toString()));
        }
        final String invokerFolder = (this.sourceFolder + '/' + this.invokerPackage).replace(".", "/");
        final String authFolder = (this.sourceFolder + '/' + this.invokerPackage + ".auth").replace(".", "/");
        this.writeOptional(this.outputFolder, new SupportingFile("pom.mustache", "", "pom.xml"));
        this.writeOptional(this.outputFolder, new SupportingFile("README.mustache", "", "README.md"));
        this.writeOptional(this.outputFolder, new SupportingFile("build.gradle.mustache", "", "build.gradle"));
        this.writeOptional(this.outputFolder, new SupportingFile("build.sbt.mustache", "", "build.sbt"));
        this.writeOptional(this.outputFolder, new SupportingFile("settings.gradle.mustache", "", "settings.gradle"));
        this.writeOptional(this.outputFolder, new SupportingFile("gradle.properties.mustache", "", "gradle.properties"));
        this.writeOptional(this.outputFolder, new SupportingFile("manifest.mustache", this.projectFolder, "AndroidManifest.xml"));
        this.supportingFiles.add(new SupportingFile("travis.mustache", "", ".travis.yml"));
        this.supportingFiles.add(new SupportingFile("ApiClient.mustache", invokerFolder, "ApiClient.java"));
        this.supportingFiles.add(new SupportingFile("StringUtil.mustache", invokerFolder, "StringUtil.java"));
        this.supportingFiles.add(new SupportingFile("auth/HttpBasicAuth.mustache", authFolder, "HttpBasicAuth.java"));
        this.supportingFiles.add(new SupportingFile("auth/ApiKeyAuth.mustache", authFolder, "ApiKeyAuth.java"));
        this.supportingFiles.add(new SupportingFile("auth/OAuth.mustache", authFolder, "OAuth.java"));
        this.supportingFiles.add(new SupportingFile("auth/OAuthFlow.mustache", authFolder, "OAuthFlow.java"));
        this.supportingFiles.add(new SupportingFile("gradlew.mustache", "", "gradlew"));
        this.supportingFiles.add(new SupportingFile("gradlew.bat.mustache", "", "gradlew.bat"));
        this.supportingFiles.add(new SupportingFile("gradle-wrapper.properties.mustache", this.gradleWrapperPackage.replace(".", File.separator), "gradle-wrapper.properties"));
        this.supportingFiles.add(new SupportingFile("gradle-wrapper.jar", this.gradleWrapperPackage.replace(".", File.separator), "gradle-wrapper.jar"));
        this.supportingFiles.add(new SupportingFile("git_push.sh.mustache", "", "git_push.sh"));
        this.supportingFiles.add(new SupportingFile("gitignore.mustache", "", ".gitignore"));
        if ("feign".equals(this.getLibrary()) || "retrofit".equals(this.getLibrary())) {
            this.modelDocTemplateFiles.remove("model_doc.mustache");
            this.apiDocTemplateFiles.remove("api_doc.mustache");
        }
        if (!"feign".equals(this.getLibrary()) && !this.usesAnyRetrofitLibrary()) {
            this.supportingFiles.add(new SupportingFile("apiException.mustache", invokerFolder, "ApiException.java"));
            this.supportingFiles.add(new SupportingFile("Configuration.mustache", invokerFolder, "Configuration.java"));
            this.supportingFiles.add(new SupportingFile("Pair.mustache", invokerFolder, "Pair.java"));
            this.supportingFiles.add(new SupportingFile("auth/Authentication.mustache", authFolder, "Authentication.java"));
        }
        if ("feign".equals(this.getLibrary())) {
            this.supportingFiles.add(new SupportingFile("FormAwareEncoder.mustache", invokerFolder, "FormAwareEncoder.java"));
            this.additionalProperties.put("jackson", "true");
        }
        else if ("okhttp-gson".equals(this.getLibrary()) || StringUtils.isEmpty((CharSequence)this.getLibrary())) {
            this.supportingFiles.add(new SupportingFile("ApiCallback.mustache", invokerFolder, "ApiCallback.java"));
            this.supportingFiles.add(new SupportingFile("ApiResponse.mustache", invokerFolder, "ApiResponse.java"));
            this.supportingFiles.add(new SupportingFile("JSON.mustache", invokerFolder, "JSON.java"));
            this.supportingFiles.add(new SupportingFile("ProgressRequestBody.mustache", invokerFolder, "ProgressRequestBody.java"));
            this.supportingFiles.add(new SupportingFile("ProgressResponseBody.mustache", invokerFolder, "ProgressResponseBody.java"));
            this.additionalProperties.put("gson", "true");
        }
        else if (this.usesAnyRetrofitLibrary()) {
            this.supportingFiles.add(new SupportingFile("auth/OAuthOkHttpClient.mustache", authFolder, "OAuthOkHttpClient.java"));
            this.supportingFiles.add(new SupportingFile("CollectionFormats.mustache", invokerFolder, "CollectionFormats.java"));
            this.additionalProperties.put("gson", "true");
        }
        else if ("jersey2".equals(this.getLibrary())) {
            this.supportingFiles.add(new SupportingFile("JSON.mustache", invokerFolder, "JSON.java"));
            this.additionalProperties.put("jackson", "true");
        }
        else if ("jersey1".equals(this.getLibrary())) {
            this.additionalProperties.put("jackson", "true");
        }
        else {
            JavaClientCodegen.LOGGER.error("Unknown library option (-l/--library): " + this.getLibrary());
        }
    }
    
    private boolean usesAnyRetrofitLibrary() {
        return this.getLibrary() != null && this.getLibrary().contains("retrofit");
    }
    
    private boolean usesRetrofit2Library() {
        return this.getLibrary() != null && this.getLibrary().contains("retrofit2");
    }
    
    @Override
    public Map<String, Object> postProcessOperations(final Map<String, Object> objs) {
        super.postProcessOperations(objs);
        if (this.usesAnyRetrofitLibrary()) {
            final Map<String, Object> operations = objs.get("operations");
            if (operations != null) {
                final List<CodegenOperation> ops = operations.get("operation");
                for (final CodegenOperation operation : ops) {
                    if (operation.hasConsumes == Boolean.TRUE) {
                        final Map<String, String> firstType = operation.consumes.get(0);
                        if (firstType != null && "multipart/form-data".equals(firstType.get("mediaType"))) {
                            operation.isMultipart = Boolean.TRUE;
                        }
                    }
                    if (operation.returnType == null) {
                        operation.returnType = "Void";
                    }
                    if (this.usesRetrofit2Library() && StringUtils.isNotEmpty((CharSequence)operation.path) && operation.path.startsWith("/")) {
                        operation.path = operation.path.substring(1);
                    }
                }
            }
        }
        return objs;
    }
    
    @Override
    public void postProcessModelProperty(final CodegenModel model, final CodegenProperty property) {
        super.postProcessModelProperty(model, property);
        if (!BooleanUtils.toBoolean(model.isEnum)) {
            if (this.additionalProperties.containsKey("jackson")) {
                model.imports.add("JsonProperty");
            }
            if (this.additionalProperties.containsKey("gson")) {
                model.imports.add("SerializedName");
            }
        }
    }
    
    @Override
    public Map<String, Object> postProcessModelsEnum(Map<String, Object> objs) {
        objs = super.postProcessModelsEnum(objs);
        if (this.additionalProperties.containsKey("gson")) {
            final List<Map<String, String>> imports = objs.get("imports");
            final List<Object> models = objs.get("models");
            for (final Object _mo : models) {
                final Map<String, Object> mo = (Map<String, Object>)_mo;
                final CodegenModel cm = mo.get("model");
                if (Boolean.TRUE.equals(cm.isEnum) && cm.allowableValues != null) {
                    cm.imports.add(this.importMapping.get("SerializedName"));
                    final Map<String, String> item = new HashMap<String, String>();
                    item.put("import", this.importMapping.get("SerializedName"));
                    imports.add(item);
                }
            }
        }
        return objs;
    }
    
    public void setUseRxJava(final boolean useRxJava) {
        this.useRxJava = useRxJava;
    }
    
    static {
        LOGGER = LoggerFactory.getLogger((Class)JavaClientCodegen.class);
    }
}
