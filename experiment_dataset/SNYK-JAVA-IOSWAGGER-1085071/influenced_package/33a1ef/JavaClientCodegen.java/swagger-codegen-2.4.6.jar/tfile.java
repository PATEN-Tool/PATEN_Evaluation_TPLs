// 
// Decompiled by Procyon v0.5.36
// 

package io.swagger.codegen.languages;

import org.slf4j.LoggerFactory;
import com.google.common.collect.LinkedListMultimap;
import java.util.HashMap;
import org.apache.commons.lang3.BooleanUtils;
import io.swagger.codegen.CodegenProperty;
import io.swagger.codegen.CodegenModel;
import java.util.Collection;
import java.util.ArrayList;
import io.swagger.codegen.DefaultCodegen;
import java.util.Collections;
import io.swagger.codegen.CodegenParameter;
import java.util.Comparator;
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
import io.swagger.codegen.languages.features.GzipFeatures;
import io.swagger.codegen.languages.features.PerformBeanValidationFeatures;
import io.swagger.codegen.languages.features.BeanValidationFeatures;

public class JavaClientCodegen extends AbstractJavaCodegen implements BeanValidationFeatures, PerformBeanValidationFeatures, GzipFeatures
{
    static final String MEDIA_TYPE = "mediaType";
    private static final Logger LOGGER;
    public static final String USE_RX_JAVA = "useRxJava";
    public static final String USE_RX_JAVA2 = "useRxJava2";
    public static final String DO_NOT_USE_RX = "doNotUseRx";
    public static final String USE_PLAY_WS = "usePlayWS";
    public static final String PLAY_VERSION = "playVersion";
    public static final String PARCELABLE_MODEL = "parcelableModel";
    public static final String USE_RUNTIME_EXCEPTION = "useRuntimeException";
    public static final String PLAY_24 = "play24";
    public static final String PLAY_25 = "play25";
    public static final String RETROFIT_1 = "retrofit";
    public static final String RETROFIT_2 = "retrofit2";
    public static final String REST_ASSURED = "rest-assured";
    protected String gradleWrapperPackage;
    protected boolean useRxJava;
    protected boolean useRxJava2;
    protected boolean doNotUseRx;
    protected boolean usePlayWS;
    protected String playVersion;
    protected boolean parcelableModel;
    protected boolean useBeanValidation;
    protected boolean performBeanValidation;
    protected boolean useGzipFeature;
    protected boolean useRuntimeException;
    private static final Pattern JSON_MIME_PATTERN;
    private static final Pattern JSON_VENDOR_MIME_PATTERN;
    
    public JavaClientCodegen() {
        this.gradleWrapperPackage = "gradle.wrapper";
        this.useRxJava = false;
        this.useRxJava2 = false;
        this.doNotUseRx = true;
        this.usePlayWS = false;
        this.playVersion = "play25";
        this.parcelableModel = false;
        this.useBeanValidation = false;
        this.performBeanValidation = false;
        this.useGzipFeature = false;
        this.useRuntimeException = false;
        this.outputFolder = "generated-code" + File.separator + "java";
        final String s = "Java";
        this.templateDir = s;
        this.embeddedTemplateDir = s;
        this.invokerPackage = "io.swagger.client";
        this.artifactId = "swagger-java-client";
        this.apiPackage = "io.swagger.client.api";
        this.modelPackage = "io.swagger.client.model";
        this.cliOptions.add(CliOption.newBoolean("useRxJava", "Whether to use the RxJava adapter with the retrofit2 library."));
        this.cliOptions.add(CliOption.newBoolean("useRxJava2", "Whether to use the RxJava2 adapter with the retrofit2 library."));
        this.cliOptions.add(CliOption.newBoolean("parcelableModel", "Whether to generate models for Android that implement Parcelable with the okhttp-gson library."));
        this.cliOptions.add(CliOption.newBoolean("usePlayWS", "Use Play! Async HTTP client (Play WS API)"));
        this.cliOptions.add(CliOption.newString("playVersion", "Version of Play! Framework (possible values \"play24\", \"play25\")"));
        this.cliOptions.add(CliOption.newBoolean("supportJava6", "Whether to support Java6 with the Jersey1 library."));
        this.cliOptions.add(CliOption.newBoolean("useBeanValidation", "Use BeanValidation API annotations"));
        this.cliOptions.add(CliOption.newBoolean("performBeanValidation", "Perform BeanValidation"));
        this.cliOptions.add(CliOption.newBoolean("useGzipFeature", "Send gzip-encoded requests"));
        this.cliOptions.add(CliOption.newBoolean("useRuntimeException", "Use RuntimeException instead of Exception"));
        this.supportedLibraries.put("jersey1", "HTTP client: Jersey client 1.19.4. JSON processing: Jackson 2.8.9. Enable Java6 support using '-DsupportJava6=true'. Enable gzip request encoding using '-DuseGzipFeature=true'.");
        this.supportedLibraries.put("feign", "HTTP client: OpenFeign 9.4.0. JSON processing: Jackson 2.8.9");
        this.supportedLibraries.put("jersey2", "HTTP client: Jersey client 2.25.1. JSON processing: Jackson 2.8.9");
        this.supportedLibraries.put("okhttp-gson", "HTTP client: OkHttp 2.7.5. JSON processing: Gson 2.8.1. Enable Parcelable models on Android using '-DparcelableModel=true'. Enable gzip request encoding using '-DuseGzipFeature=true'.");
        this.supportedLibraries.put("retrofit", "HTTP client: OkHttp 2.7.5. JSON processing: Gson 2.3.1 (Retrofit 1.9.0). IMPORTANT NOTE: retrofit1.x is no longer actively maintained so please upgrade to 'retrofit2' instead.");
        this.supportedLibraries.put("retrofit2", "HTTP client: OkHttp 3.8.0. JSON processing: Gson 2.6.1 (Retrofit 2.3.0). Enable the RxJava adapter using '-DuseRxJava[2]=true'. (RxJava 1.x or 2.x)");
        this.supportedLibraries.put("resttemplate", "HTTP client: Spring RestTemplate 4.3.9-RELEASE. JSON processing: Jackson 2.8.9");
        this.supportedLibraries.put("resteasy", "HTTP client: Resteasy client 3.1.3.Final. JSON processing: Jackson 2.8.9");
        this.supportedLibraries.put("vertx", "HTTP client: VertX client 3.2.4. JSON processing: Jackson 2.8.9");
        this.supportedLibraries.put("google-api-client", "HTTP client: Google API client 1.23.0. JSON processing: Jackson 2.8.9");
        this.supportedLibraries.put("rest-assured", "HTTP client: rest-assured : 3.1.0. JSON processing: Gson 2.6.1. Only for Java8");
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
        if (this.additionalProperties.containsKey("useRxJava") && this.additionalProperties.containsKey("useRxJava2")) {
            JavaClientCodegen.LOGGER.warn("You specified both RxJava versions 1 and 2 but they are mutually exclusive. Defaulting to v2.");
        }
        else if (this.additionalProperties.containsKey("useRxJava")) {
            this.setUseRxJava(Boolean.valueOf(this.additionalProperties.get("useRxJava").toString()));
        }
        if (this.additionalProperties.containsKey("useRxJava2")) {
            this.setUseRxJava2(Boolean.valueOf(this.additionalProperties.get("useRxJava2").toString()));
        }
        if (!this.useRxJava && !this.useRxJava2) {
            this.additionalProperties.put("doNotUseRx", true);
        }
        if (this.additionalProperties.containsKey("usePlayWS")) {
            this.setUsePlayWS(Boolean.valueOf(this.additionalProperties.get("usePlayWS").toString()));
        }
        this.additionalProperties.put("usePlayWS", this.usePlayWS);
        if (this.additionalProperties.containsKey("playVersion")) {
            this.setPlayVersion(this.additionalProperties.get("playVersion").toString());
        }
        this.additionalProperties.put("playVersion", this.playVersion);
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
        if (this.additionalProperties.containsKey("useGzipFeature")) {
            this.setUseGzipFeature(this.convertPropertyToBooleanAndWriteBack("useGzipFeature"));
        }
        if (this.additionalProperties.containsKey("useRuntimeException")) {
            this.setUseRuntimeException(this.convertPropertyToBooleanAndWriteBack("useRuntimeException"));
        }
        final String invokerFolder = (this.sourceFolder + '/' + this.invokerPackage).replace(".", "/");
        final String authFolder = (this.sourceFolder + '/' + this.invokerPackage + ".auth").replace(".", "/");
        final String apiFolder = (this.sourceFolder + '/' + this.apiPackage).replace(".", "/");
        this.writeOptional(this.outputFolder, new SupportingFile("pom.mustache", "", "pom.xml"));
        this.writeOptional(this.outputFolder, new SupportingFile("README.mustache", "", "README.md"));
        this.writeOptional(this.outputFolder, new SupportingFile("build.gradle.mustache", "", "build.gradle"));
        this.writeOptional(this.outputFolder, new SupportingFile("build.sbt.mustache", "", "build.sbt"));
        this.writeOptional(this.outputFolder, new SupportingFile("settings.gradle.mustache", "", "settings.gradle"));
        this.writeOptional(this.outputFolder, new SupportingFile("gradle.properties.mustache", "", "gradle.properties"));
        this.writeOptional(this.outputFolder, new SupportingFile("manifest.mustache", this.projectFolder, "AndroidManifest.xml"));
        this.supportingFiles.add(new SupportingFile("travis.mustache", "", ".travis.yml"));
        this.supportingFiles.add(new SupportingFile("ApiClient.mustache", invokerFolder, "ApiClient.java"));
        if (!"resttemplate".equals(this.getLibrary()) && !"rest-assured".equals(this.getLibrary())) {
            this.supportingFiles.add(new SupportingFile("StringUtil.mustache", invokerFolder, "StringUtil.java"));
        }
        if (!"google-api-client".equals(this.getLibrary()) && !"rest-assured".equals(this.getLibrary())) {
            this.supportingFiles.add(new SupportingFile("auth/HttpBasicAuth.mustache", authFolder, "HttpBasicAuth.java"));
            this.supportingFiles.add(new SupportingFile("auth/ApiKeyAuth.mustache", authFolder, "ApiKeyAuth.java"));
            this.supportingFiles.add(new SupportingFile("auth/OAuth.mustache", authFolder, "OAuth.java"));
            this.supportingFiles.add(new SupportingFile("auth/OAuthFlow.mustache", authFolder, "OAuthFlow.java"));
        }
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
        if (!"feign".equals(this.getLibrary()) && !"resttemplate".equals(this.getLibrary()) && !this.usesAnyRetrofitLibrary() && !"google-api-client".equals(this.getLibrary()) && !"rest-assured".equals(this.getLibrary())) {
            this.supportingFiles.add(new SupportingFile("apiException.mustache", invokerFolder, "ApiException.java"));
            this.supportingFiles.add(new SupportingFile("Configuration.mustache", invokerFolder, "Configuration.java"));
            this.supportingFiles.add(new SupportingFile("Pair.mustache", invokerFolder, "Pair.java"));
            this.supportingFiles.add(new SupportingFile("auth/Authentication.mustache", authFolder, "Authentication.java"));
        }
        if ("feign".equals(this.getLibrary())) {
            this.additionalProperties.put("jackson", "true");
            this.supportingFiles.add(new SupportingFile("ParamExpander.mustache", invokerFolder, "ParamExpander.java"));
            this.supportingFiles.add(new SupportingFile("EncodingUtils.mustache", invokerFolder, "EncodingUtils.java"));
        }
        else if ("okhttp-gson".equals(this.getLibrary()) || StringUtils.isEmpty((CharSequence)this.getLibrary())) {
            this.supportingFiles.add(new SupportingFile("ApiCallback.mustache", invokerFolder, "ApiCallback.java"));
            this.supportingFiles.add(new SupportingFile("ApiResponse.mustache", invokerFolder, "ApiResponse.java"));
            this.supportingFiles.add(new SupportingFile("JSON.mustache", invokerFolder, "JSON.java"));
            this.supportingFiles.add(new SupportingFile("ProgressRequestBody.mustache", invokerFolder, "ProgressRequestBody.java"));
            this.supportingFiles.add(new SupportingFile("ProgressResponseBody.mustache", invokerFolder, "ProgressResponseBody.java"));
            this.supportingFiles.add(new SupportingFile("GzipRequestInterceptor.mustache", invokerFolder, "GzipRequestInterceptor.java"));
            this.additionalProperties.put("gson", "true");
        }
        else if (this.usesAnyRetrofitLibrary()) {
            this.supportingFiles.add(new SupportingFile("auth/OAuthOkHttpClient.mustache", authFolder, "OAuthOkHttpClient.java"));
            this.supportingFiles.add(new SupportingFile("CollectionFormats.mustache", invokerFolder, "CollectionFormats.java"));
            this.additionalProperties.put("gson", "true");
            if ("retrofit2".equals(this.getLibrary()) && !this.usePlayWS) {
                this.supportingFiles.add(new SupportingFile("JSON.mustache", invokerFolder, "JSON.java"));
            }
        }
        else if ("jersey2".equals(this.getLibrary())) {
            this.supportingFiles.add(new SupportingFile("JSON.mustache", invokerFolder, "JSON.java"));
            this.supportingFiles.add(new SupportingFile("ApiResponse.mustache", invokerFolder, "ApiResponse.java"));
            this.additionalProperties.put("jackson", "true");
        }
        else if ("resteasy".equals(this.getLibrary())) {
            this.supportingFiles.add(new SupportingFile("JSON.mustache", invokerFolder, "JSON.java"));
            this.additionalProperties.put("jackson", "true");
        }
        else if ("jersey1".equals(this.getLibrary())) {
            this.additionalProperties.put("jackson", "true");
        }
        else if ("resttemplate".equals(this.getLibrary())) {
            this.additionalProperties.put("jackson", "true");
            this.supportingFiles.add(new SupportingFile("auth/Authentication.mustache", authFolder, "Authentication.java"));
        }
        else if ("vertx".equals(this.getLibrary())) {
            this.typeMapping.put("file", "AsyncFile");
            this.importMapping.put("AsyncFile", "io.vertx.core.file.AsyncFile");
            this.setJava8Mode(true);
            this.additionalProperties.put("java8", "true");
            this.additionalProperties.put("jackson", "true");
            this.apiTemplateFiles.put("apiImpl.mustache", "Impl.java");
            this.apiTemplateFiles.put("rxApiImpl.mustache", ".java");
            this.supportingFiles.remove(new SupportingFile("manifest.mustache", this.projectFolder, "AndroidManifest.xml"));
        }
        else if ("google-api-client".equals(this.getLibrary())) {
            this.additionalProperties.put("jackson", "true");
        }
        else if ("rest-assured".equals(this.getLibrary())) {
            this.additionalProperties.put("gson", "true");
            this.apiTemplateFiles.put("api.mustache", ".java");
            this.supportingFiles.add(new SupportingFile("ResponseSpecBuilders.mustache", invokerFolder, "ResponseSpecBuilders.java"));
            this.supportingFiles.add(new SupportingFile("JSON.mustache", invokerFolder, "JSON.java"));
            this.supportingFiles.add(new SupportingFile("GsonObjectMapper.mustache", invokerFolder, "GsonObjectMapper.java"));
        }
        else {
            JavaClientCodegen.LOGGER.error("Unknown library option (-l/--library): " + this.getLibrary());
        }
        if (this.usePlayWS) {
            final Iterator<SupportingFile> iter = this.supportingFiles.iterator();
            while (iter.hasNext()) {
                final SupportingFile sf = iter.next();
                if (sf.templateFile.startsWith("auth/")) {
                    iter.remove();
                }
            }
            this.apiTemplateFiles.remove("api.mustache");
            if ("play24".equals(this.playVersion)) {
                this.additionalProperties.put("play24", true);
                this.apiTemplateFiles.put("play24/api.mustache", ".java");
                this.supportingFiles.add(new SupportingFile("play24/ApiClient.mustache", invokerFolder, "ApiClient.java"));
                this.supportingFiles.add(new SupportingFile("play24/Play24CallFactory.mustache", invokerFolder, "Play24CallFactory.java"));
                this.supportingFiles.add(new SupportingFile("play24/Play24CallAdapterFactory.mustache", invokerFolder, "Play24CallAdapterFactory.java"));
            }
            else {
                this.additionalProperties.put("play25", true);
                this.apiTemplateFiles.put("play25/api.mustache", ".java");
                this.supportingFiles.add(new SupportingFile("play25/ApiClient.mustache", invokerFolder, "ApiClient.java"));
                this.supportingFiles.add(new SupportingFile("play25/Play25CallFactory.mustache", invokerFolder, "Play25CallFactory.java"));
                this.supportingFiles.add(new SupportingFile("play25/Play25CallAdapterFactory.mustache", invokerFolder, "Play25CallAdapterFactory.java"));
                this.additionalProperties.put("java8", "true");
            }
            this.supportingFiles.add(new SupportingFile("play-common/auth/ApiKeyAuth.mustache", authFolder, "ApiKeyAuth.java"));
            this.supportingFiles.add(new SupportingFile("auth/Authentication.mustache", authFolder, "Authentication.java"));
            this.supportingFiles.add(new SupportingFile("Pair.mustache", invokerFolder, "Pair.java"));
            this.additionalProperties.put("jackson", "true");
            this.additionalProperties.remove("gson");
        }
        if (this.additionalProperties.containsKey("jackson")) {
            this.supportingFiles.add(new SupportingFile("RFC3339DateFormat.mustache", invokerFolder, "RFC3339DateFormat.java"));
            if ("threetenbp".equals(this.dateLibrary) && !this.usePlayWS) {
                this.supportingFiles.add(new SupportingFile("CustomInstantDeserializer.mustache", invokerFolder, "CustomInstantDeserializer.java"));
            }
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
                    if (this.usesRetrofit2Library() && StringUtils.isNotEmpty((CharSequence)operation.path) && operation.path.startsWith("/")) {
                        operation.path = operation.path.substring(1);
                    }
                    if (operation.allParams != null) {
                        Collections.sort(operation.allParams, new Comparator<CodegenParameter>() {
                            @Override
                            public int compare(final CodegenParameter one, final CodegenParameter another) {
                                if (one.isPathParam && another.isQueryParam) {
                                    return -1;
                                }
                                if (one.isQueryParam && another.isPathParam) {
                                    return 1;
                                }
                                return 0;
                            }
                        });
                        final Iterator<CodegenParameter> iterator = operation.allParams.iterator();
                        while (iterator.hasNext()) {
                            final CodegenParameter param = iterator.next();
                            param.hasMore = iterator.hasNext();
                        }
                    }
                }
            }
        }
        if ("feign".equals(this.getLibrary())) {
            final Map<String, Object> operations = objs.get("operations");
            final List<CodegenOperation> operationList = operations.get("operation");
            for (final CodegenOperation op : operationList) {
                final String path = op.path;
                final String[] items = path.split("/", -1);
                for (int i = 0; i < items.length; ++i) {
                    if (items[i].matches("^\\{(.*)\\}$")) {
                        items[i] = "{" + DefaultCodegen.camelize(items[i].substring(1, items[i].length() - 1), true) + "}";
                    }
                }
                op.path = StringUtils.join((Object[])items, "/");
            }
        }
        return objs;
    }
    
    @Override
    public String apiFilename(final String templateName, final String tag) {
        if ("vertx".equals(this.getLibrary())) {
            final String suffix = this.apiTemplateFiles().get(templateName);
            String subFolder = "";
            if (templateName.startsWith("rx")) {
                subFolder = "/rxjava";
            }
            return this.apiFileFolder() + subFolder + '/' + this.toApiFilename(tag) + suffix;
        }
        return super.apiFilename(templateName, tag);
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
                model.imports.add("JsonValue");
            }
            if (this.additionalProperties.containsKey("gson")) {
                model.imports.add("SerializedName");
                model.imports.add("TypeAdapter");
                model.imports.add("JsonAdapter");
                model.imports.add("JsonReader");
                model.imports.add("JsonWriter");
                model.imports.add("IOException");
            }
        }
        else if (this.additionalProperties.containsKey("jackson")) {
            model.imports.add("JsonValue");
            model.imports.add("JsonCreator");
        }
    }
    
    @Override
    public Map<String, Object> postProcessAllModels(final Map<String, Object> objs) {
        final Map<String, Object> allProcessedModels = super.postProcessAllModels(objs);
        if (!this.additionalProperties.containsKey("gsonFactoryMethod")) {
            final List<Object> allModels = new ArrayList<Object>();
            for (final String name : allProcessedModels.keySet()) {
                final Map<String, Object> models = allProcessedModels.get(name);
                try {
                    allModels.add(models.get("models").get(0));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            this.additionalProperties.put("parent", this.modelInheritanceSupportInGson(allModels));
        }
        return allProcessedModels;
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
    
    private List<Map<String, Object>> modelInheritanceSupportInGson(final List<?> allModels) {
        final LinkedListMultimap<CodegenModel, CodegenModel> byParent = (LinkedListMultimap<CodegenModel, CodegenModel>)LinkedListMultimap.create();
        for (final Object m : allModels) {
            final Map entry = (Map)m;
            final CodegenModel parent = entry.get("model").parentModel;
            if (null != parent) {
                byParent.put((Object)parent, (Object)entry.get("model"));
            }
        }
        final List<Map<String, Object>> parentsList = new ArrayList<Map<String, Object>>();
        for (final CodegenModel parentModel : byParent.keySet()) {
            final List<Map<String, Object>> childrenList = new ArrayList<Map<String, Object>>();
            final Map<String, Object> parent2 = new HashMap<String, Object>();
            parent2.put("classname", parentModel.classname);
            final List<CodegenModel> childrenModels = (List<CodegenModel>)byParent.get((Object)parentModel);
            for (final CodegenModel model : childrenModels) {
                final Map<String, Object> child = new HashMap<String, Object>();
                child.put("name", model.name);
                child.put("classname", model.classname);
                childrenList.add(child);
            }
            parent2.put("children", childrenList);
            parent2.put("discriminator", parentModel.discriminator);
            parentsList.add(parent2);
        }
        return parentsList;
    }
    
    public void setUseRxJava(final boolean useRxJava) {
        this.useRxJava = useRxJava;
        this.doNotUseRx = false;
    }
    
    public void setUseRxJava2(final boolean useRxJava2) {
        this.useRxJava2 = useRxJava2;
        this.doNotUseRx = false;
    }
    
    public void setDoNotUseRx(final boolean doNotUseRx) {
        this.doNotUseRx = doNotUseRx;
    }
    
    public void setUsePlayWS(final boolean usePlayWS) {
        this.usePlayWS = usePlayWS;
    }
    
    public void setPlayVersion(final String playVersion) {
        this.playVersion = playVersion;
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
    
    @Override
    public void setUseGzipFeature(final boolean useGzipFeature) {
        this.useGzipFeature = useGzipFeature;
    }
    
    public void setUseRuntimeException(final boolean useRuntimeException) {
        this.useRuntimeException = useRuntimeException;
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
