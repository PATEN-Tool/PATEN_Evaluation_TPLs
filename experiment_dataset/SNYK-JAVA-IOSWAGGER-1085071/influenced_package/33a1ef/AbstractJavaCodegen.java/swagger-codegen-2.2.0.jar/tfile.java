// 
// Decompiled by Procyon v0.5.36
// 

package io.swagger.codegen.languages;

import com.google.common.base.Strings;
import io.swagger.codegen.CodegenOperation;
import io.swagger.models.Path;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.List;
import org.apache.commons.lang3.BooleanUtils;
import io.swagger.codegen.CodegenProperty;
import io.swagger.codegen.CodegenModel;
import io.swagger.models.Model;
import org.apache.commons.lang3.StringUtils;
import io.swagger.codegen.CodegenParameter;
import io.swagger.models.properties.StringProperty;
import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.FloatProperty;
import io.swagger.models.properties.DoubleProperty;
import io.swagger.models.properties.LongProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import java.util.Map;
import java.util.HashMap;
import io.swagger.codegen.CliOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.Arrays;
import java.io.File;
import io.swagger.codegen.CodegenConfig;
import io.swagger.codegen.DefaultCodegen;

public abstract class AbstractJavaCodegen extends DefaultCodegen implements CodegenConfig
{
    public static final String FULL_JAVA_UTIL = "fullJavaUtil";
    public static final String DEFAULT_LIBRARY = "<default>";
    public static final String DATE_LIBRARY = "dateLibrary";
    protected String dateLibrary;
    protected String invokerPackage;
    protected String groupId;
    protected String artifactId;
    protected String artifactVersion;
    protected String projectFolder;
    protected String projectTestFolder;
    protected String sourceFolder;
    protected String testFolder;
    protected String localVariablePrefix;
    protected boolean fullJavaUtil;
    protected String javaUtilPrefix;
    protected Boolean serializableModel;
    protected boolean serializeBigDecimalAsString;
    protected boolean hideGenerationTimestamp;
    protected String apiDocPath;
    protected String modelDocPath;
    
    public AbstractJavaCodegen() {
        this.dateLibrary = "joda";
        this.invokerPackage = "io.swagger";
        this.groupId = "io.swagger";
        this.artifactId = "swagger-java";
        this.artifactVersion = "1.0.0";
        this.projectFolder = "src" + File.separator + "main";
        this.projectTestFolder = "src" + File.separator + "test";
        this.sourceFolder = this.projectFolder + File.separator + "java";
        this.testFolder = this.projectTestFolder + File.separator + "java";
        this.localVariablePrefix = "";
        this.javaUtilPrefix = "";
        this.serializableModel = false;
        this.serializeBigDecimalAsString = false;
        this.hideGenerationTimestamp = false;
        this.apiDocPath = "docs/";
        this.modelDocPath = "docs/";
        this.modelTemplateFiles.put("model.mustache", ".java");
        this.apiTemplateFiles.put("api.mustache", ".java");
        this.apiTestTemplateFiles.put("api_test.mustache", ".java");
        this.modelDocTemplateFiles.put("model_doc.mustache", ".md");
        this.apiDocTemplateFiles.put("api_doc.mustache", ".md");
        this.setReservedWordsLowerCase(Arrays.asList("localVarPath", "localVarQueryParams", "localVarHeaderParams", "localVarFormParams", "localVarPostBody", "localVarAccepts", "localVarAccept", "localVarContentTypes", "localVarContentType", "localVarAuthNames", "localReturnType", "ApiClient", "ApiException", "ApiResponse", "Configuration", "StringUtil", "abstract", "continue", "for", "new", "switch", "assert", "default", "if", "package", "synchronized", "boolean", "do", "goto", "private", "this", "break", "double", "implements", "protected", "throw", "byte", "else", "import", "public", "throws", "case", "enum", "instanceof", "return", "transient", "catch", "extends", "int", "short", "try", "char", "final", "interface", "static", "void", "class", "finally", "long", "strictfp", "volatile", "const", "float", "native", "super", "while"));
        this.languageSpecificPrimitives = new HashSet<String>(Arrays.asList("String", "boolean", "Boolean", "Double", "Integer", "Long", "Float", "Object", "byte[]"));
        this.instantiationTypes.put("array", "ArrayList");
        this.instantiationTypes.put("map", "HashMap");
        this.typeMapping.put("date", "Date");
        this.typeMapping.put("file", "File");
        this.typeMapping.put("UUID", "String");
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
        this.cliOptions.add(CliOption.newBoolean("fullJavaUtil", "whether to use fully qualified name for classes under java.util. This option only works for Java API client"));
        this.cliOptions.add(new CliOption("hideGenerationTimestamp", "hides the timestamp when files were generated"));
        final CliOption dateLibrary = new CliOption("dateLibrary", "Option. Date library to use");
        final Map<String, String> dateOptions = new HashMap<String, String>();
        dateOptions.put("java8", "Java 8 native");
        dateOptions.put("java8-localdatetime", "Java 8 using LocalDateTime (for legacy app only)");
        dateOptions.put("joda", "Joda");
        dateOptions.put("legacy", "Legacy java.util.Date");
        dateLibrary.setEnum(dateOptions);
        this.cliOptions.add(dateLibrary);
    }
    
    @Override
    public void processOpts() {
        super.processOpts();
        if (this.additionalProperties.containsKey("invokerPackage")) {
            this.setInvokerPackage(this.additionalProperties.get("invokerPackage"));
        }
        else if (this.additionalProperties.containsKey("apiPackage")) {
            final String derviedInvokerPackage = this.deriveInvokerPackageName(this.additionalProperties.get("apiPackage"));
            this.additionalProperties.put("invokerPackage", derviedInvokerPackage);
            this.setInvokerPackage(this.additionalProperties.get("invokerPackage"));
            AbstractJavaCodegen.LOGGER.info("Invoker Package Name, originally not set, is now dervied from api package name: " + derviedInvokerPackage);
        }
        else if (this.additionalProperties.containsKey("modelPackage")) {
            final String derviedInvokerPackage = this.deriveInvokerPackageName(this.additionalProperties.get("modelPackage"));
            this.additionalProperties.put("invokerPackage", derviedInvokerPackage);
            this.setInvokerPackage(this.additionalProperties.get("invokerPackage"));
            AbstractJavaCodegen.LOGGER.info("Invoker Package Name, originally not set, is now dervied from model package name: " + derviedInvokerPackage);
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
        this.additionalProperties.put("apiDocPath", this.apiDocPath);
        this.additionalProperties.put("modelDocPath", this.modelDocPath);
        this.importMapping.put("List", "java.util.List");
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
        this.importMapping.put("SerializedName", "com.google.gson.annotations.SerializedName");
        this.importMapping.put("Objects", "java.util.Objects");
        this.importMapping.put("StringUtil", this.invokerPackage + ".StringUtil");
        if (this.additionalProperties.containsKey("dateLibrary")) {
            this.setDateLibrary(this.additionalProperties.get("dateLibrary").toString());
            this.additionalProperties.put(this.dateLibrary, "true");
        }
        if ("joda".equals(this.dateLibrary)) {
            this.typeMapping.put("date", "LocalDate");
            this.typeMapping.put("DateTime", "DateTime");
            this.importMapping.put("LocalDate", "org.joda.time.LocalDate");
            this.importMapping.put("DateTime", "org.joda.time.DateTime");
        }
        else if (this.dateLibrary.startsWith("java8")) {
            this.additionalProperties.put("java8", "true");
            this.typeMapping.put("date", "LocalDate");
            this.importMapping.put("LocalDate", "java.time.LocalDate");
            if ("java8-localdatetime".equals(this.dateLibrary)) {
                this.typeMapping.put("DateTime", "LocalDateTime");
                this.importMapping.put("LocalDateTime", "java.time.LocalDateTime");
            }
            else {
                this.typeMapping.put("DateTime", "OffsetDateTime");
                this.importMapping.put("OffsetDateTime", "java.time.OffsetDateTime");
            }
        }
    }
    
    private void sanitizeConfig() {
        this.setApiPackage(sanitizePackageName(this.apiPackage));
        if (this.additionalProperties.containsKey("apiPackage")) {
            this.additionalProperties.put("apiPackage", this.apiPackage);
        }
        this.setModelPackage(sanitizePackageName(this.modelPackage));
        if (this.additionalProperties.containsKey("modelPackage")) {
            this.additionalProperties.put("modelPackage", this.modelPackage);
        }
        this.setInvokerPackage(sanitizePackageName(this.invokerPackage));
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
    public String apiTestFileFolder() {
        return this.outputFolder + "/" + this.testFolder + "/" + this.apiPackage().replace('.', '/');
    }
    
    @Override
    public String modelFileFolder() {
        return this.outputFolder + "/" + this.sourceFolder + "/" + this.modelPackage().replace('.', '/');
    }
    
    @Override
    public String apiDocFileFolder() {
        return (this.outputFolder + "/" + this.apiDocPath).replace('/', File.separatorChar);
    }
    
    @Override
    public String modelDocFileFolder() {
        return (this.outputFolder + "/" + this.modelDocPath).replace('/', File.separatorChar);
    }
    
    @Override
    public String toApiDocFilename(final String name) {
        return this.toApiName(name);
    }
    
    @Override
    public String toModelDocFilename(final String name) {
        return this.toModelName(name);
    }
    
    @Override
    public String toApiTestFilename(final String name) {
        return this.toApiName(name) + "Test";
    }
    
    @Override
    public String toVarName(String name) {
        name = this.sanitizeName(name);
        if ("class".equals(name.toLowerCase())) {
            return "PropertyClass";
        }
        if ("_".equals(name)) {
            name = "_u";
        }
        if (name.matches("^[A-Z_]*$")) {
            return name;
        }
        name = DefaultCodegen.camelize(name, true);
        if (this.isReservedWord(name) || name.matches("^\\d.*")) {
            name = this.escapeReservedWord(name);
        }
        return name;
    }
    
    @Override
    public String toParamName(final String name) {
        return this.toVarName(name);
    }
    
    @Override
    public String toModelName(final String name) {
        final String sanitizedName = this.sanitizeName(this.modelNamePrefix + name + this.modelNameSuffix);
        final String camelizedName = DefaultCodegen.camelize(sanitizedName);
        if (this.isReservedWord(camelizedName)) {
            final String modelName = "Model" + camelizedName;
            AbstractJavaCodegen.LOGGER.warn(camelizedName + " (reserved word) cannot be used as model name. Renamed to " + modelName);
            return modelName;
        }
        if (name.matches("^\\d.*")) {
            final String modelName = "Model" + camelizedName;
            AbstractJavaCodegen.LOGGER.warn(name + " (model name starts with number) cannot be used as model name. Renamed to " + modelName);
            return modelName;
        }
        return camelizedName;
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
    public void setParameterExampleValue(final CodegenParameter p) {
        String example;
        if (p.defaultValue == null) {
            example = p.example;
        }
        else {
            example = p.defaultValue;
        }
        String type = p.baseType;
        if (type == null) {
            type = p.dataType;
        }
        if ("String".equals(type)) {
            if (example == null) {
                example = p.paramName + "_example";
            }
            example = "\"" + this.escapeText(example) + "\"";
        }
        else if ("Integer".equals(type) || "Short".equals(type)) {
            if (example == null) {
                example = "56";
            }
        }
        else if ("Long".equals(type)) {
            if (example == null) {
                example = "56";
            }
            example += "L";
        }
        else if ("Float".equals(type)) {
            if (example == null) {
                example = "3.4";
            }
            example += "F";
        }
        else if ("Double".equals(type)) {
            example = "3.4";
            example += "D";
        }
        else if ("Boolean".equals(type)) {
            if (example == null) {
                example = "true";
            }
        }
        else if ("File".equals(type)) {
            if (example == null) {
                example = "/path/to/file";
            }
            example = "new File(\"" + this.escapeText(example) + "\")";
        }
        else if ("Date".equals(type)) {
            example = "new Date()";
        }
        else if (!this.languageSpecificPrimitives.contains(type)) {
            example = "new " + type + "()";
        }
        if (example == null) {
            example = "null";
        }
        else if (Boolean.TRUE.equals(p.isListContainer)) {
            example = "Arrays.asList(" + example + ")";
        }
        else if (Boolean.TRUE.equals(p.isMapContainer)) {
            example = "new HashMap()";
        }
        p.example = example;
    }
    
    @Override
    public String getSwaggerType(final Property p) {
        final String swaggerType = super.getSwaggerType(p);
        String type;
        if (this.typeMapping.containsKey(swaggerType)) {
            type = this.typeMapping.get(swaggerType);
            if (this.languageSpecificPrimitives.contains(type) || type.indexOf(".") >= 0 || type.equals("Map") || type.equals("List") || type.equals("File") || type.equals("Date")) {
                return type;
            }
        }
        else {
            type = swaggerType;
        }
        if (null == type) {
            AbstractJavaCodegen.LOGGER.error("No Type defined for Property " + p);
        }
        return this.toModelName(type);
    }
    
    @Override
    public String toOperationId(String operationId) {
        if (StringUtils.isEmpty((CharSequence)operationId)) {
            throw new RuntimeException("Empty method/operation name (operationId) not allowed");
        }
        operationId = DefaultCodegen.camelize(this.sanitizeName(operationId), true);
        if (this.isReservedWord(operationId)) {
            final String newOperationId = DefaultCodegen.camelize("call_" + operationId, true);
            AbstractJavaCodegen.LOGGER.warn(operationId + " (reserved word) cannot be used as method name. Renamed to " + newOperationId);
            return newOperationId;
        }
        return operationId;
    }
    
    @Override
    public CodegenModel fromModel(final String name, final Model model, final Map<String, Model> allDefinitions) {
        CodegenModel codegenModel = super.fromModel(name, model, allDefinitions);
        if (codegenModel.description != null) {
            codegenModel.imports.add("ApiModel");
        }
        if (allDefinitions != null && codegenModel != null && codegenModel.parentSchema != null && codegenModel.hasEnums) {
            final Model parentModel = allDefinitions.get(codegenModel.parentSchema);
            final CodegenModel parentCodegenModel = super.fromModel(codegenModel.parent, parentModel);
            codegenModel = reconcileInlineEnums(codegenModel, parentCodegenModel);
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
        if ("array".equals(property.containerType)) {
            model.imports.add("ArrayList");
        }
        else if ("map".equals(property.containerType)) {
            model.imports.add("HashMap");
        }
        if (!BooleanUtils.toBoolean(model.isEnum)) {
            model.imports.add("ApiModelProperty");
            model.imports.add("ApiModel");
        }
    }
    
    @Override
    public void postProcessParameter(final CodegenParameter parameter) {
    }
    
    @Override
    public Map<String, Object> postProcessModels(final Map<String, Object> objs) {
        return this.postProcessModelsEnum(objs);
    }
    
    @Override
    public Map<String, Object> postProcessOperations(final Map<String, Object> objs) {
        final List<Map<String, String>> imports = objs.get("imports");
        final Pattern pattern = Pattern.compile("java\\.util\\.(List|ArrayList|Map|HashMap)");
        final Iterator<Map<String, String>> itr = imports.iterator();
        while (itr.hasNext()) {
            final String _import = itr.next().get("import");
            if (pattern.matcher(_import).matches()) {
                itr.remove();
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
                        final String accepts = getAccept(operation);
                        operation.setVendorExtension("x-contentType", (Object)contentType);
                        operation.setVendorExtension("x-accepts", (Object)accepts);
                    }
                }
            }
        }
    }
    
    private static String getAccept(final Operation operation) {
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
    
    @Override
    public String toEnumName(final CodegenProperty property) {
        return this.sanitizeName(DefaultCodegen.camelize(property.name)) + "Enum";
    }
    
    @Override
    public String toEnumVarName(final String value, final String datatype) {
        if ("Integer".equals(datatype) || "Long".equals(datatype) || "Float".equals(datatype) || "Double".equals(datatype)) {
            String varName = "NUMBER_" + value;
            varName = varName.replaceAll("-", "MINUS_");
            varName = varName.replaceAll("\\+", "PLUS_");
            varName = varName.replaceAll("\\.", "_DOT_");
            return varName;
        }
        final String var = value.replaceAll("\\W+", "_").replaceAll("_+", "_").toUpperCase();
        if (var.matches("\\d.*")) {
            return "_" + var;
        }
        return var;
    }
    
    @Override
    public String toEnumValue(final String value, final String datatype) {
        if ("Integer".equals(datatype) || "Long".equals(datatype) || "Float".equals(datatype) || "Double".equals(datatype)) {
            return value;
        }
        return "\"" + this.escapeText(value) + "\"";
    }
    
    @Override
    public CodegenOperation fromOperation(final String path, final String httpMethod, final Operation operation, final Map<String, Model> definitions, final Swagger swagger) {
        final CodegenOperation op = super.fromOperation(path, httpMethod, operation, definitions, swagger);
        op.path = this.sanitizePath(op.path);
        return op;
    }
    
    private static CodegenModel reconcileInlineEnums(final CodegenModel codegenModel, final CodegenModel parentCodegenModel) {
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
    
    private static String sanitizePackageName(String packageName) {
        packageName = packageName.trim();
        packageName = packageName.replaceAll("[^a-zA-Z0-9_\\.]", "_");
        if (Strings.isNullOrEmpty(packageName)) {
            return "invalidPackageName";
        }
        return packageName;
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
    
    public void setSerializableModel(final Boolean serializableModel) {
        this.serializableModel = serializableModel;
    }
    
    private String sanitizePath(final String p) {
        return p.replaceAll("\"", "%22");
    }
    
    public void setFullJavaUtil(final boolean fullJavaUtil) {
        this.fullJavaUtil = fullJavaUtil;
    }
    
    public void setDateLibrary(final String library) {
        this.dateLibrary = library;
    }
    
    @Override
    public String escapeQuotationMark(final String input) {
        return input.replace("\"", "");
    }
    
    @Override
    public String escapeUnsafeCharacters(final String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }
    
    private String deriveInvokerPackageName(final String input) {
        final String[] parts = input.split(Pattern.quote("."));
        final StringBuilder sb = new StringBuilder();
        String delim = "";
        for (final String p : Arrays.copyOf(parts, parts.length - 1)) {
            sb.append(delim).append(p);
            delim = ".";
        }
        return sb.toString();
    }
}
