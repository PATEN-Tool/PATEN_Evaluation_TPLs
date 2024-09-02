// 
// Decompiled by Procyon v0.5.36
// 

package io.swagger.codegen.languages;

import org.slf4j.LoggerFactory;
import java.util.HashMap;
import org.apache.commons.lang3.BooleanUtils;
import io.swagger.codegen.CodegenProperty;
import io.swagger.codegen.CodegenModel;
import java.util.Collection;
import java.util.ArrayList;
import io.swagger.codegen.CodegenOperation;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import org.apache.commons.lang3.StringUtils;
import io.swagger.codegen.SupportingFile;
import io.swagger.codegen.CodegenType;
import io.swagger.codegen.CliOption;
import java.io.File;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import io.swagger.codegen.languages.features.PerformBeanValidationFeatures;
import io.swagger.codegen.languages.features.BeanValidationFeatures;

public class JavaClientCodegen extends AbstractJavaCodegen implements BeanValidationFeatures, PerformBeanValidationFeatures
{
    static final String MEDIA_TYPE = "mediaType";
    private static final Logger LOGGER;
    public static final String USE_RX_JAVA = "useRxJava";
    public static final String USE_PLAY24_WS = "usePlay24WS";
    public static final String PARCELABLE_MODEL = "parcelableModel";
    public static final String RETROFIT_1 = "retrofit";
    public static final String RETROFIT_2 = "retrofit2";
    protected String gradleWrapperPackage;
    protected boolean useRxJava;
    protected boolean usePlay24WS;
    protected boolean parcelableModel;
    protected boolean useBeanValidation;
    protected boolean performBeanValidation;
    private static final Pattern JSON_MIME_PATTERN;
    private static final Pattern JSON_VENDOR_MIME_PATTERN;
    
    public JavaClientCodegen() {
        this.gradleWrapperPackage = "gradle.wrapper";
        this.useRxJava = false;
        this.usePlay24WS = false;
        this.parcelableModel = false;
        this.useBeanValidation = false;
        this.performBeanValidation = false;
        this.outputFolder = "generated-code" + File.separator + "java";
        final String s = "Java";
        this.templateDir = s;
        this.embeddedTemplateDir = s;
        this.invokerPackage = "io.swagger.client";
        this.artifactId = "swagger-java-client";
        this.apiPackage = "io.swagger.client.api";
        this.modelPackage = "io.swagger.client.model";
        this.cliOptions.add(CliOption.newBoolean("useRxJava", "Whether to use the RxJava adapter with the retrofit2 library."));
        this.cliOptions.add(CliOption.newBoolean("parcelableModel", "Whether to generate models for Android that implement Parcelable with the okhttp-gson library."));
        this.cliOptions.add(CliOption.newBoolean("usePlay24WS", "Use Play! 2.4 Async HTTP client (Play WS API)"));
        this.cliOptions.add(CliOption.newBoolean("supportJava6", "Whether to support Java6 with the Jersey1 library."));
        this.cliOptions.add(CliOption.newBoolean("useBeanValidation", "Use BeanValidation API annotations"));
        this.cliOptions.add(CliOption.newBoolean("performBeanValidation", "Perform BeanValidation"));
        this.supportedLibraries.put("jersey1", "HTTP client: Jersey client 1.19.1. JSON processing: Jackson 2.7.0. Enable Java6 support using '-DsupportJava6=true'.");
        this.supportedLibraries.put("feign", "HTTP client: Netflix Feign 8.16.0. JSON processing: Jackson 2.7.0");
        this.supportedLibraries.put("jersey2", "HTTP client: Jersey client 2.22.2. JSON processing: Jackson 2.7.0");
        this.supportedLibraries.put("okhttp-gson", "HTTP client: OkHttp 2.7.5. JSON processing: Gson 2.6.2. Enable Parcelable modles on Android using '-DparcelableModel=true'");
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
        if (this.additionalProperties.containsKey("usePlay24WS")) {
            this.setUsePlay24WS(Boolean.valueOf(this.additionalProperties.get("usePlay24WS").toString()));
        }
        this.additionalProperties.put("usePlay24WS", this.usePlay24WS);
        if (this.additionalProperties.containsKey("parcelableModel")) {
            this.setParcelableModel(Boolean.valueOf(this.additionalProperties.get("parcelableModel").toString()));
        }
        this.additionalProperties.put("parcelableModel", this.parcelableModel);
        if (this.additionalProperties.containsKey("useBeanValidation")) {
            this.setUseBeanValidation(this.convertPropertyToBooleanAndWriteBack("useBeanValidation"));
        }
        if (this.additionalProperties.containsKey("performBeanValidation")) {
            this.setPerformBeanValidation(this.convertPropertyToBooleanAndWriteBack("performBeanValidation"));
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
        if (this.performBeanValidation) {
            this.supportingFiles.add(new SupportingFile("BeanValidationException.mustache", invokerFolder, "BeanValidationException.java"));
        }
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
            this.additionalProperties.put("jackson", "true");
            this.supportingFiles.add(new SupportingFile("ParamExpander.mustache", invokerFolder, "ParamExpander.java"));
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
        if (Boolean.TRUE.equals(this.additionalProperties.get("usePlay24WS"))) {
            final Iterator<SupportingFile> iter = this.supportingFiles.iterator();
            while (iter.hasNext()) {
                final SupportingFile sf = iter.next();
                if (sf.templateFile.startsWith("auth/")) {
                    iter.remove();
                }
            }
            this.supportingFiles.add(new SupportingFile("play24/auth/ApiKeyAuth.mustache", authFolder, "ApiKeyAuth.java"));
            this.supportingFiles.add(new SupportingFile("auth/Authentication.mustache", authFolder, "Authentication.java"));
            this.supportingFiles.add(new SupportingFile("Pair.mustache", invokerFolder, "Pair.java"));
            this.supportingFiles.add(new SupportingFile("play24/ApiClient.mustache", invokerFolder, "ApiClient.java"));
            this.supportingFiles.add(new SupportingFile("play24/Play24CallFactory.mustache", invokerFolder, "Play24CallFactory.java"));
            this.supportingFiles.add(new SupportingFile("play24/Play24CallAdapterFactory.mustache", invokerFolder, "Play24CallAdapterFactory.java"));
            this.additionalProperties.put("jackson", "true");
            this.additionalProperties.remove("gson");
        }
        if (this.additionalProperties.containsKey("jackson")) {
            this.supportingFiles.add(new SupportingFile("RFC3339DateFormat.mustache", invokerFolder, "RFC3339DateFormat.java"));
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
                        if (isMultipartType(operation.consumes)) {
                            operation.isMultipart = Boolean.TRUE;
                        }
                        else {
                            operation.prioritizedContentTypes = prioritizeContentTypes(operation.consumes);
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
    
    static List<Map<String, String>> prioritizeContentTypes(final List<Map<String, String>> consumes) {
        if (consumes.size() <= 1) {
            return consumes;
        }
        final List<Map<String, String>> prioritizedContentTypes = new ArrayList<Map<String, String>>(consumes.size());
        final List<Map<String, String>> jsonVendorMimeTypes = new ArrayList<Map<String, String>>(consumes.size());
        final List<Map<String, String>> jsonMimeTypes = new ArrayList<Map<String, String>>(consumes.size());
        for (final Map<String, String> consume : consumes) {
            if (isJsonVendorMimeType(consume.get("mediaType"))) {
                jsonVendorMimeTypes.add(consume);
            }
            else if (isJsonMimeType(consume.get("mediaType"))) {
                jsonMimeTypes.add(consume);
            }
            else {
                prioritizedContentTypes.add(consume);
            }
            consume.put("hasMore", "true");
        }
        prioritizedContentTypes.addAll(0, jsonMimeTypes);
        prioritizedContentTypes.addAll(0, jsonVendorMimeTypes);
        prioritizedContentTypes.get(prioritizedContentTypes.size() - 1).put("hasMore", null);
        return prioritizedContentTypes;
    }
    
    private static boolean isMultipartType(final List<Map<String, String>> consumes) {
        final Map<String, String> firstType = consumes.get(0);
        return firstType != null && "multipart/form-data".equals(firstType.get("mediaType"));
    }
    
    @Override
    public void postProcessModelProperty(final CodegenModel model, final CodegenProperty property) {
        super.postProcessModelProperty(model, property);
        if (!BooleanUtils.toBoolean(Boolean.valueOf(model.isEnum))) {
            if (this.additionalProperties.containsKey("jackson")) {
                model.imports.add("JsonProperty");
            }
            if (this.additionalProperties.containsKey("gson")) {
                model.imports.add("SerializedName");
            }
        }
        else if (this.additionalProperties.containsKey("jackson")) {
            model.imports.add("JsonCreator");
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
    
    public void setUsePlay24WS(final boolean usePlay24WS) {
        this.usePlay24WS = usePlay24WS;
    }
    
    public void setParcelableModel(final boolean parcelableModel) {
        this.parcelableModel = parcelableModel;
    }
    
    @Override
    public void setUseBeanValidation(final boolean useBeanValidation) {
        this.useBeanValidation = useBeanValidation;
    }
    
    @Override
    public void setPerformBeanValidation(final boolean performBeanValidation) {
        this.performBeanValidation = performBeanValidation;
    }
    
    static boolean isJsonMimeType(final String mime) {
        return mime != null && JavaClientCodegen.JSON_MIME_PATTERN.matcher(mime).matches();
    }
    
    static boolean isJsonVendorMimeType(final String mime) {
        return mime != null && JavaClientCodegen.JSON_VENDOR_MIME_PATTERN.matcher(mime).matches();
    }
    
    static {
        LOGGER = LoggerFactory.getLogger((Class)JavaClientCodegen.class);
        JSON_MIME_PATTERN = Pattern.compile("(?i)application\\/json(;.*)?");
        JSON_VENDOR_MIME_PATTERN = Pattern.compile("(?i)application\\/vnd.(.*)+json(;.*)?");
    }
}
