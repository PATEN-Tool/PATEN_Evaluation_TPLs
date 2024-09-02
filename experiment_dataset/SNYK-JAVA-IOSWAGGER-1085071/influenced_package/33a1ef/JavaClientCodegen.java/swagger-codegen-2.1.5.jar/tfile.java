// 
// Decompiled by Procyon v0.5.36
// 

package io.swagger.codegen.languages;

import org.slf4j.LoggerFactory;
import com.google.common.base.Strings;
import io.swagger.models.Path;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.codegen.CodegenOperation;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import io.swagger.codegen.CodegenParameter;
import io.swagger.codegen.CodegenProperty;
import io.swagger.codegen.CodegenModel;
import java.util.Map;
import io.swagger.models.Model;
import org.apache.commons.lang.StringUtils;
import io.swagger.models.properties.StringProperty;
import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.FloatProperty;
import io.swagger.models.properties.DoubleProperty;
import io.swagger.models.properties.LongProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.codegen.SupportingFile;
import io.swagger.codegen.CodegenType;
import io.swagger.codegen.CliOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.Arrays;
import java.io.File;
import org.slf4j.Logger;
import io.swagger.codegen.CodegenConfig;
import io.swagger.codegen.DefaultCodegen;

public class JavaClientCodegen extends DefaultCodegen implements CodegenConfig
{
    private static final Logger LOGGER;
    public static final String FULL_JAVA_UTIL = "fullJavaUtil";
    public static final String DEFAULT_LIBRARY = "<default>";
    protected String invokerPackage;
    protected String groupId;
    protected String artifactId;
    protected String artifactVersion;
    protected String projectFolder;
    protected String sourceFolder;
    protected String localVariablePrefix;
    protected boolean fullJavaUtil;
    protected String javaUtilPrefix;
    protected Boolean serializableModel;
    protected boolean serializeBigDecimalAsString;
    
    public JavaClientCodegen() {
        this.invokerPackage = "io.swagger.client";
        this.groupId = "io.swagger";
        this.artifactId = "swagger-java-client";
        this.artifactVersion = "1.0.0";
        this.projectFolder = "src" + File.separator + "main";
        this.sourceFolder = this.projectFolder + File.separator + "java";
        this.localVariablePrefix = "";
        this.javaUtilPrefix = "";
        this.serializableModel = false;
        this.serializeBigDecimalAsString = false;
        this.outputFolder = "generated-code" + File.separator + "java";
        this.modelTemplateFiles.put("model.mustache", ".java");
        this.apiTemplateFiles.put("api.mustache", ".java");
        final String s = "Java";
        this.templateDir = s;
        this.embeddedTemplateDir = s;
        this.apiPackage = "io.swagger.client.api";
        this.modelPackage = "io.swagger.client.model";
        this.reservedWords = new HashSet<String>(Arrays.asList("path", "queryParams", "headerParams", "formParams", "postBody", "accepts", "accept", "contentTypes", "contentType", "authNames", "abstract", "continue", "for", "new", "switch", "assert", "default", "if", "package", "synchronized", "boolean", "do", "goto", "private", "this", "break", "double", "implements", "protected", "throw", "byte", "else", "import", "public", "throws", "case", "enum", "instanceof", "return", "transient", "catch", "extends", "int", "short", "try", "char", "final", "interface", "static", "void", "class", "finally", "long", "strictfp", "volatile", "const", "float", "native", "super", "while"));
        this.languageSpecificPrimitives = new HashSet<String>(Arrays.asList("String", "boolean", "Boolean", "Double", "Integer", "Long", "Float", "Object", "byte[]"));
        this.instantiationTypes.put("array", "ArrayList");
        this.instantiationTypes.put("map", "HashMap");
        this.cliOptions.add(new CliOption("modelPackage", "package for generated models"));
        this.cliOptions.add(new CliOption("apiPackage", "package for generated api classes"));
        this.cliOptions.add(new CliOption("invokerPackage", "root package for generated code"));
        this.cliOptions.add(new CliOption("groupId", "groupId in generated pom.xml"));
        this.cliOptions.add(new CliOption("artifactId", "artifactId in generated pom.xml"));
        this.cliOptions.add(new CliOption("artifactVersion", "artifact version in generated pom.xml"));
        this.cliOptions.add(new CliOption("sourceFolder", "source folder for generated code"));
        this.cliOptions.add(new CliOption("localVariablePrefix", "prefix for generated code members and local variables"));
        this.cliOptions.add(CliOption.newBoolean("serializableModel", "boolean - toggle \"implements Serializable\" for generated models"));
        this.cliOptions.add(CliOption.newBoolean("bigDecimalAsString", "Treat BigDecimal values as Strings to avoid precision loss."));
        this.cliOptions.add(CliOption.newBoolean("fullJavaUtil", "whether to use fully qualified name for classes under java.util"));
        this.supportedLibraries.put("<default>", "HTTP client: Jersey client 1.18. JSON processing: Jackson 2.4.2");
        this.supportedLibraries.put("feign", "HTTP client: Netflix Feign 8.1.1");
        this.supportedLibraries.put("jersey2", "HTTP client: Jersey client 2.6");
        this.supportedLibraries.put("okhttp-gson", "HTTP client: OkHttp 2.4.0. JSON processing: Gson 2.3.1");
        this.supportedLibraries.put("retrofit", "HTTP client: OkHttp 2.4.0. JSON processing: Gson 2.3.1 (Retrofit 1.9.0)");
        this.supportedLibraries.put("retrofit2", "HTTP client: OkHttp 2.5.0. JSON processing: Gson 2.4 (Retrofit 2.0.0-beta2)");
        final CliOption library = new CliOption("library", "library template (sub-template) to use");
        library.setDefault("<default>");
        library.setEnum(this.supportedLibraries);
        library.setDefault("<default>");
        this.cliOptions.add(library);
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
        if (this.additionalProperties.containsKey("invokerPackage")) {
            this.setInvokerPackage(this.additionalProperties.get("invokerPackage"));
        }
        else {
            this.additionalProperties.put("invokerPackage", this.invokerPackage);
        }
        if (this.additionalProperties.containsKey("groupId")) {
            this.setGroupId(this.additionalProperties.get("groupId"));
        }
        else {
            this.additionalProperties.put("groupId", this.groupId);
        }
        if (this.additionalProperties.containsKey("artifactId")) {
            this.setArtifactId(this.additionalProperties.get("artifactId"));
        }
        else {
            this.additionalProperties.put("artifactId", this.artifactId);
        }
        if (this.additionalProperties.containsKey("artifactVersion")) {
            this.setArtifactVersion(this.additionalProperties.get("artifactVersion"));
        }
        else {
            this.additionalProperties.put("artifactVersion", this.artifactVersion);
        }
        if (this.additionalProperties.containsKey("sourceFolder")) {
            this.setSourceFolder(this.additionalProperties.get("sourceFolder"));
        }
        if (this.additionalProperties.containsKey("localVariablePrefix")) {
            this.setLocalVariablePrefix(this.additionalProperties.get("localVariablePrefix"));
        }
        if (this.additionalProperties.containsKey("serializableModel")) {
            this.setSerializableModel(Boolean.valueOf(this.additionalProperties.get("serializableModel").toString()));
        }
        if (this.additionalProperties.containsKey("library")) {
            this.setLibrary(this.additionalProperties.get("library"));
        }
        if (this.additionalProperties.containsKey("bigDecimalAsString")) {
            this.setSerializeBigDecimalAsString(Boolean.valueOf(this.additionalProperties.get("bigDecimalAsString").toString()));
        }
        this.additionalProperties.put("serializableModel", this.serializableModel);
        if (this.additionalProperties.containsKey("fullJavaUtil")) {
            this.setFullJavaUtil(Boolean.valueOf(this.additionalProperties.get("fullJavaUtil").toString()));
        }
        if (this.fullJavaUtil) {
            this.javaUtilPrefix = "java.util.";
        }
        this.additionalProperties.put("fullJavaUtil", this.fullJavaUtil);
        this.additionalProperties.put("javaUtilPrefix", this.javaUtilPrefix);
        if (this.fullJavaUtil) {
            this.typeMapping.put("array", "java.util.List");
            this.typeMapping.put("map", "java.util.Map");
            this.typeMapping.put("DateTime", "java.util.Date");
            this.typeMapping.remove("List");
            this.importMapping.remove("Date");
            this.importMapping.remove("Map");
            this.importMapping.remove("HashMap");
            this.importMapping.remove("Array");
            this.importMapping.remove("ArrayList");
            this.importMapping.remove("List");
            this.importMapping.remove("Set");
            this.importMapping.remove("DateTime");
            this.instantiationTypes.put("array", "java.util.ArrayList");
            this.instantiationTypes.put("map", "java.util.HashMap");
        }
        this.sanitizeConfig();
        this.importMapping.put("ToStringSerializer", "com.fasterxml.jackson.databind.ser.std.ToStringSerializer");
        this.importMapping.put("JsonSerialize", "com.fasterxml.jackson.databind.annotation.JsonSerialize");
        this.importMapping.put("ApiModelProperty", "io.swagger.annotations.ApiModelProperty");
        this.importMapping.put("ApiModel", "io.swagger.annotations.ApiModel");
        this.importMapping.put("JsonProperty", "com.fasterxml.jackson.annotation.JsonProperty");
        this.importMapping.put("JsonValue", "com.fasterxml.jackson.annotation.JsonValue");
        this.importMapping.put("Objects", "java.util.Objects");
        this.importMapping.put("StringUtil", this.invokerPackage + ".StringUtil");
        final String invokerFolder = (this.sourceFolder + '/' + this.invokerPackage).replace(".", "/");
        this.supportingFiles.add(new SupportingFile("pom.mustache", "", "pom.xml"));
        this.supportingFiles.add(new SupportingFile("README.mustache", "", "README.md"));
        this.supportingFiles.add(new SupportingFile("build.gradle.mustache", "", "build.gradle"));
        this.supportingFiles.add(new SupportingFile("settings.gradle.mustache", "", "settings.gradle"));
        this.supportingFiles.add(new SupportingFile("gradle.properties.mustache", "", "gradle.properties"));
        this.supportingFiles.add(new SupportingFile("manifest.mustache", this.projectFolder, "AndroidManifest.xml"));
        this.supportingFiles.add(new SupportingFile("ApiClient.mustache", invokerFolder, "ApiClient.java"));
        this.supportingFiles.add(new SupportingFile("StringUtil.mustache", invokerFolder, "StringUtil.java"));
        final String authFolder = (this.sourceFolder + '/' + this.invokerPackage + ".auth").replace(".", "/");
        if ("feign".equals(this.getLibrary())) {
            this.supportingFiles.add(new SupportingFile("FormAwareEncoder.mustache", invokerFolder, "FormAwareEncoder.java"));
        }
        else {
            this.supportingFiles.add(new SupportingFile("auth/HttpBasicAuth.mustache", authFolder, "HttpBasicAuth.java"));
            this.supportingFiles.add(new SupportingFile("auth/ApiKeyAuth.mustache", authFolder, "ApiKeyAuth.java"));
            this.supportingFiles.add(new SupportingFile("auth/OAuth.mustache", authFolder, "OAuth.java"));
            this.supportingFiles.add(new SupportingFile("auth/OAuthFlow.mustache", authFolder, "OAuthFlow.java"));
        }
        if (!"feign".equals(this.getLibrary()) && !"retrofit".equals(this.getLibrary()) && !"retrofit2".equals(this.getLibrary())) {
            this.supportingFiles.add(new SupportingFile("apiException.mustache", invokerFolder, "ApiException.java"));
            this.supportingFiles.add(new SupportingFile("Configuration.mustache", invokerFolder, "Configuration.java"));
            this.supportingFiles.add(new SupportingFile("Pair.mustache", invokerFolder, "Pair.java"));
            this.supportingFiles.add(new SupportingFile("auth/Authentication.mustache", authFolder, "Authentication.java"));
        }
        if ("okhttp-gson".equals(this.getLibrary())) {
            this.supportingFiles.add(new SupportingFile("ApiCallback.mustache", invokerFolder, "ApiCallback.java"));
            this.supportingFiles.add(new SupportingFile("ApiResponse.mustache", invokerFolder, "ApiResponse.java"));
            this.supportingFiles.add(new SupportingFile("JSON.mustache", invokerFolder, "JSON.java"));
            this.supportingFiles.add(new SupportingFile("ProgressRequestBody.mustache", invokerFolder, "ProgressRequestBody.java"));
            this.supportingFiles.add(new SupportingFile("ProgressResponseBody.mustache", invokerFolder, "ProgressResponseBody.java"));
            this.supportingFiles.add(new SupportingFile("build.sbt.mustache", "", "build.sbt"));
        }
        else if ("retrofit".equals(this.getLibrary()) || "retrofit2".equals(this.getLibrary())) {
            this.supportingFiles.add(new SupportingFile("auth/OAuthOkHttpClient.mustache", authFolder, "OAuthOkHttpClient.java"));
            this.supportingFiles.add(new SupportingFile("CollectionFormats.mustache", invokerFolder, "CollectionFormats.java"));
        }
        else if ("jersey2".equals(this.getLibrary())) {
            this.supportingFiles.add(new SupportingFile("JSON.mustache", invokerFolder, "JSON.java"));
        }
    }
    
    private void sanitizeConfig() {
        this.setApiPackage(this.sanitizePackageName(this.apiPackage));
        if (this.additionalProperties.containsKey("apiPackage")) {
            this.additionalProperties.put("apiPackage", this.apiPackage);
        }
        this.setModelPackage(this.sanitizePackageName(this.modelPackage));
        if (this.additionalProperties.containsKey("modelPackage")) {
            this.additionalProperties.put("modelPackage", this.modelPackage);
        }
        this.setInvokerPackage(this.sanitizePackageName(this.invokerPackage));
        if (this.additionalProperties.containsKey("invokerPackage")) {
            this.additionalProperties.put("invokerPackage", this.invokerPackage);
        }
    }
    
    @Override
    public String escapeReservedWord(final String name) {
        return "_" + name;
    }
    
    @Override
    public String apiFileFolder() {
        return this.outputFolder + "/" + this.sourceFolder + "/" + this.apiPackage().replace('.', '/');
    }
    
    @Override
    public String modelFileFolder() {
        return this.outputFolder + "/" + this.sourceFolder + "/" + this.modelPackage().replace('.', '/');
    }
    
    @Override
    public String toVarName(String name) {
        name = this.sanitizeName(name);
        if ("_".equals(name)) {
            name = "_u";
        }
        if (name.matches("^[A-Z_]*$")) {
            return name;
        }
        name = DefaultCodegen.camelize(name, true);
        if (this.reservedWords.contains(name) || name.matches("^\\d.*")) {
            name = this.escapeReservedWord(name);
        }
        return name;
    }
    
    @Override
    public String toParamName(final String name) {
        return this.toVarName(name);
    }
    
    @Override
    public String toModelName(String name) {
        name = this.sanitizeName(name);
        if (this.reservedWords.contains(name)) {
            throw new RuntimeException(name + " (reserved word) cannot be used as a model name");
        }
        return DefaultCodegen.camelize(name);
    }
    
    @Override
    public String toModelFilename(final String name) {
        return this.toModelName(name);
    }
    
    @Override
    public String getTypeDeclaration(final Property p) {
        if (p instanceof ArrayProperty) {
            final ArrayProperty ap = (ArrayProperty)p;
            final Property inner = ap.getItems();
            return this.getSwaggerType(p) + "<" + this.getTypeDeclaration(inner) + ">";
        }
        if (p instanceof MapProperty) {
            final MapProperty mp = (MapProperty)p;
            final Property inner = mp.getAdditionalProperties();
            return this.getSwaggerType(p) + "<String, " + this.getTypeDeclaration(inner) + ">";
        }
        return super.getTypeDeclaration(p);
    }
    
    @Override
    public String toDefaultValue(final Property p) {
        if (p instanceof ArrayProperty) {
            final ArrayProperty ap = (ArrayProperty)p;
            String pattern;
            if (this.fullJavaUtil) {
                pattern = "new java.util.ArrayList<%s>()";
            }
            else {
                pattern = "new ArrayList<%s>()";
            }
            return String.format(pattern, this.getTypeDeclaration(ap.getItems()));
        }
        if (p instanceof MapProperty) {
            final MapProperty ap2 = (MapProperty)p;
            String pattern;
            if (this.fullJavaUtil) {
                pattern = "new java.util.HashMap<String, %s>()";
            }
            else {
                pattern = "new HashMap<String, %s>()";
            }
            return String.format(pattern, this.getTypeDeclaration(ap2.getAdditionalProperties()));
        }
        if (p instanceof IntegerProperty) {
            final IntegerProperty dp = (IntegerProperty)p;
            if (dp.getDefault() != null) {
                return dp.getDefault().toString();
            }
            return "null";
        }
        else if (p instanceof LongProperty) {
            final LongProperty dp2 = (LongProperty)p;
            if (dp2.getDefault() != null) {
                return dp2.getDefault().toString() + "l";
            }
            return "null";
        }
        else if (p instanceof DoubleProperty) {
            final DoubleProperty dp3 = (DoubleProperty)p;
            if (dp3.getDefault() != null) {
                return dp3.getDefault().toString() + "d";
            }
            return "null";
        }
        else if (p instanceof FloatProperty) {
            final FloatProperty dp4 = (FloatProperty)p;
            if (dp4.getDefault() != null) {
                return dp4.getDefault().toString() + "f";
            }
            return "null";
        }
        else if (p instanceof BooleanProperty) {
            final BooleanProperty bp = (BooleanProperty)p;
            if (bp.getDefault() != null) {
                return bp.getDefault().toString();
            }
            return "null";
        }
        else {
            if (!(p instanceof StringProperty)) {
                return super.toDefaultValue(p);
            }
            final StringProperty sp = (StringProperty)p;
            if (sp.getDefault() == null) {
                return "null";
            }
            final String _default = sp.getDefault();
            if (sp.getEnum() == null) {
                return "\"" + this.escapeText(_default) + "\"";
            }
            return _default;
        }
    }
    
    @Override
    public String getSwaggerType(final Property p) {
        final String swaggerType = super.getSwaggerType(p);
        String type = null;
        if (this.typeMapping.containsKey(swaggerType)) {
            type = this.typeMapping.get(swaggerType);
            if (this.languageSpecificPrimitives.contains(type) || type.indexOf(".") >= 0) {
                return type;
            }
        }
        else {
            type = swaggerType;
        }
        if (null == type) {
            JavaClientCodegen.LOGGER.error("No Type defined for Property " + p);
        }
        return this.toModelName(type);
    }
    
    @Override
    public String toOperationId(final String operationId) {
        if (StringUtils.isEmpty(operationId)) {
            throw new RuntimeException("Empty method/operation name (operationId) not allowed");
        }
        if (this.reservedWords.contains(operationId)) {
            throw new RuntimeException(operationId + " (reserved word) cannot be used as method name");
        }
        return DefaultCodegen.camelize(this.sanitizeName(operationId), true);
    }
    
    @Override
    public CodegenModel fromModel(final String name, final Model model, final Map<String, Model> allDefinitions) {
        CodegenModel codegenModel = super.fromModel(name, model, allDefinitions);
        if (allDefinitions != null && codegenModel != null && codegenModel.parentSchema != null && codegenModel.hasEnums) {
            final Model parentModel = allDefinitions.get(codegenModel.parentSchema);
            final CodegenModel parentCodegenModel = super.fromModel(codegenModel.parent, parentModel);
            codegenModel = this.reconcileInlineEnums(codegenModel, parentCodegenModel);
        }
        return codegenModel;
    }
    
    @Override
    public void postProcessModelProperty(final CodegenModel model, final CodegenProperty property) {
        if (this.serializeBigDecimalAsString && property.baseType.equals("BigDecimal")) {
            property.vendorExtensions.put("extraAnnotation", "@JsonSerialize(using = ToStringSerializer.class)");
            model.imports.add("ToStringSerializer");
            model.imports.add("JsonSerialize");
        }
        if (model.isEnum == null || model.isEnum) {
            model.imports.add("ApiModelProperty");
            model.imports.add("ApiModel");
            final String lib = this.getLibrary();
            if (StringUtils.isEmpty(lib) || "feign".equals(lib) || "jersey2".equals(lib)) {
                model.imports.add("JsonProperty");
                if (model.hasEnums != null || model.hasEnums) {
                    model.imports.add("JsonValue");
                }
            }
        }
    }
    
    @Override
    public void postProcessParameter(final CodegenParameter parameter) {
    }
    
    @Override
    public Map<String, Object> postProcessModels(final Map<String, Object> objs) {
        final List<Object> models = objs.get("models");
        for (final Object _mo : models) {
            final Map<String, Object> mo = (Map<String, Object>)_mo;
            final CodegenModel cm = mo.get("model");
            for (final CodegenProperty var : cm.vars) {
                Map<String, Object> allowableValues = var.allowableValues;
                if (var.items != null) {
                    allowableValues = var.items.allowableValues;
                }
                if (allowableValues == null) {
                    continue;
                }
                final List<String> values = allowableValues.get("values");
                if (values == null) {
                    continue;
                }
                final List<Map<String, String>> enumVars = new ArrayList<Map<String, String>>();
                final String commonPrefix = this.findCommonPrefixOfVars(values);
                final int truncateIdx = commonPrefix.length();
                for (final String value : values) {
                    final Map<String, String> enumVar = new HashMap<String, String>();
                    String enumName;
                    if (truncateIdx == 0) {
                        enumName = value;
                    }
                    else {
                        enumName = value.substring(truncateIdx);
                        if ("".equals(enumName)) {
                            enumName = value;
                        }
                    }
                    enumVar.put("name", this.toEnumVarName(enumName));
                    enumVar.put("value", value);
                    enumVars.add(enumVar);
                }
                allowableValues.put("enumVars", enumVars);
                if (var.defaultValue == null) {
                    continue;
                }
                String enumName2 = null;
                final Iterator i$4 = enumVars.iterator();
                while (i$4.hasNext()) {
                    final Map<String, String> enumVar = i$4.next();
                    if (var.defaultValue.equals(enumVar.get("value"))) {
                        enumName2 = enumVar.get("name");
                        break;
                    }
                }
                if (enumName2 == null) {
                    continue;
                }
                var.defaultValue = var.datatypeWithEnum + "." + enumName2;
            }
        }
        return objs;
    }
    
    @Override
    public Map<String, Object> postProcessOperations(final Map<String, Object> objs) {
        if ("retrofit".equals(this.getLibrary()) || "retrofit2".equals(this.getLibrary())) {
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
                    if ("retrofit2".equals(this.getLibrary()) && StringUtils.isNotEmpty(operation.path) && operation.path.startsWith("/")) {
                        operation.path = operation.path.substring(1);
                    }
                }
            }
        }
        return objs;
    }
    
    @Override
    public void preprocessSwagger(final Swagger swagger) {
        if (swagger != null && swagger.getPaths() != null) {
            for (final String pathname : swagger.getPaths().keySet()) {
                final Path path = swagger.getPath(pathname);
                if (path.getOperations() != null) {
                    for (final Operation operation : path.getOperations()) {
                        boolean hasFormParameters = false;
                        for (final Parameter parameter : operation.getParameters()) {
                            if (parameter instanceof FormParameter) {
                                hasFormParameters = true;
                            }
                        }
                        final String defaultContentType = hasFormParameters ? "application/x-www-form-urlencoded" : "application/json";
                        final String contentType = (operation.getConsumes() == null || operation.getConsumes().isEmpty()) ? defaultContentType : operation.getConsumes().get(0);
                        final String accepts = this.getAccept(operation);
                        operation.setVendorExtension("x-contentType", (Object)contentType);
                        operation.setVendorExtension("x-accepts", (Object)accepts);
                    }
                }
            }
        }
    }
    
    private String getAccept(final Operation operation) {
        String accepts = null;
        final String defaultContentType = "application/json";
        if (operation.getProduces() != null && !operation.getProduces().isEmpty()) {
            final StringBuilder sb = new StringBuilder();
            for (final String produces : operation.getProduces()) {
                if (defaultContentType.equalsIgnoreCase(produces)) {
                    accepts = defaultContentType;
                    break;
                }
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(produces);
            }
            if (accepts == null) {
                accepts = sb.toString();
            }
        }
        else {
            accepts = defaultContentType;
        }
        return accepts;
    }
    
    @Override
    protected boolean needToImport(final String type) {
        return super.needToImport(type) && type.indexOf(".") < 0;
    }
    
    private String findCommonPrefixOfVars(final List<String> vars) {
        final String prefix = StringUtils.getCommonPrefix((String[])vars.toArray(new String[vars.size()]));
        return prefix.replaceAll("[a-zA-Z0-9]+\\z", "");
    }
    
    private String toEnumVarName(final String value) {
        final String var = value.replaceAll("\\W+", "_").toUpperCase();
        if (var.matches("\\d.*")) {
            return "_" + var;
        }
        return var;
    }
    
    private CodegenModel reconcileInlineEnums(final CodegenModel codegenModel, final CodegenModel parentCodegenModel) {
        if (parentCodegenModel.hasEnums) {
            final List<CodegenProperty> parentModelCodegenProperties = parentCodegenModel.vars;
            final List<CodegenProperty> codegenProperties = codegenModel.vars;
            boolean removedChildEnum = false;
            for (final CodegenProperty parentModelCodegenPropery : parentModelCodegenProperties) {
                if (parentModelCodegenPropery.isEnum) {
                    final Iterator<CodegenProperty> iterator = codegenProperties.iterator();
                    while (iterator.hasNext()) {
                        final CodegenProperty codegenProperty = iterator.next();
                        if (codegenProperty.isEnum && codegenProperty.equals(parentModelCodegenPropery)) {
                            iterator.remove();
                            removedChildEnum = true;
                        }
                    }
                }
            }
            if (removedChildEnum) {
                int count = 0;
                final int numVars = codegenProperties.size();
                for (final CodegenProperty codegenProperty : codegenProperties) {
                    ++count;
                    codegenProperty.hasMore = ((count < numVars) ? Boolean.valueOf(true) : null);
                }
                codegenModel.vars = codegenProperties;
            }
        }
        return codegenModel;
    }
    
    public void setInvokerPackage(final String invokerPackage) {
        this.invokerPackage = invokerPackage;
    }
    
    public void setGroupId(final String groupId) {
        this.groupId = groupId;
    }
    
    public void setArtifactId(final String artifactId) {
        this.artifactId = artifactId;
    }
    
    public void setArtifactVersion(final String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }
    
    public void setSourceFolder(final String sourceFolder) {
        this.sourceFolder = sourceFolder;
    }
    
    public void setLocalVariablePrefix(final String localVariablePrefix) {
        this.localVariablePrefix = localVariablePrefix;
    }
    
    public void setSerializeBigDecimalAsString(final boolean s) {
        this.serializeBigDecimalAsString = s;
    }
    
    public Boolean getSerializableModel() {
        return this.serializableModel;
    }
    
    public void setSerializableModel(final Boolean serializableModel) {
        this.serializableModel = serializableModel;
    }
    
    private String sanitizePackageName(String packageName) {
        packageName = packageName.trim();
        packageName = packageName.replaceAll("[^a-zA-Z0-9_\\.]", "_");
        if (Strings.isNullOrEmpty(packageName)) {
            return "invalidPackageName";
        }
        return packageName;
    }
    
    public void setFullJavaUtil(final boolean fullJavaUtil) {
        this.fullJavaUtil = fullJavaUtil;
    }
    
    static {
        LOGGER = LoggerFactory.getLogger((Class)JavaClientCodegen.class);
    }
}
