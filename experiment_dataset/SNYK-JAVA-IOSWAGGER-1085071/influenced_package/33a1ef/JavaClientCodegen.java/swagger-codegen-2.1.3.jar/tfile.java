// 
// Decompiled by Procyon v0.5.36
// 

package io.swagger.codegen.languages;

import com.google.common.base.Strings;
import java.util.Iterator;
import java.util.List;
import io.swagger.codegen.CodegenProperty;
import io.swagger.codegen.CodegenModel;
import java.util.Map;
import io.swagger.models.Model;
import org.apache.commons.lang.StringUtils;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.codegen.SupportingFile;
import java.io.File;
import io.swagger.codegen.CodegenType;
import io.swagger.codegen.CliOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.Arrays;
import io.swagger.codegen.CodegenConfig;
import io.swagger.codegen.DefaultCodegen;

public class JavaClientCodegen extends DefaultCodegen implements CodegenConfig
{
    protected String invokerPackage;
    protected String groupId;
    protected String artifactId;
    protected String artifactVersion;
    protected String sourceFolder;
    protected String localVariablePrefix;
    protected Boolean serializableModel;
    
    public JavaClientCodegen() {
        this.invokerPackage = "io.swagger.client";
        this.groupId = "io.swagger";
        this.artifactId = "swagger-java-client";
        this.artifactVersion = "1.0.0";
        this.sourceFolder = "src/main/java";
        this.localVariablePrefix = "";
        this.serializableModel = false;
        this.outputFolder = "generated-code/java";
        this.modelTemplateFiles.put("model.mustache", ".java");
        this.apiTemplateFiles.put("api.mustache", ".java");
        this.templateDir = "Java";
        this.apiPackage = "io.swagger.client.api";
        this.modelPackage = "io.swagger.client.model";
        this.reservedWords = new HashSet<String>(Arrays.asList("abstract", "continue", "for", "new", "switch", "assert", "default", "if", "package", "synchronized", "boolean", "do", "goto", "private", "this", "break", "double", "implements", "protected", "throw", "byte", "else", "import", "public", "throws", "case", "enum", "instanceof", "return", "transient", "catch", "extends", "int", "short", "try", "char", "final", "interface", "static", "void", "class", "finally", "long", "strictfp", "volatile", "const", "float", "native", "super", "while"));
        this.languageSpecificPrimitives = new HashSet<String>(Arrays.asList("String", "boolean", "Boolean", "Double", "Integer", "Long", "Float", "Object"));
        this.instantiationTypes.put("array", "ArrayList");
        this.instantiationTypes.put("map", "HashMap");
        this.cliOptions.add(new CliOption("invokerPackage", "root package for generated code"));
        this.cliOptions.add(new CliOption("groupId", "groupId in generated pom.xml"));
        this.cliOptions.add(new CliOption("artifactId", "artifactId in generated pom.xml"));
        this.cliOptions.add(new CliOption("artifactVersion", "artifact version in generated pom.xml"));
        this.cliOptions.add(new CliOption("sourceFolder", "source folder for generated code"));
        this.cliOptions.add(new CliOption("localVariablePrefix", "prefix for generated code members and local variables"));
        this.cliOptions.add(new CliOption("serializableModel", "boolean - toggle \"implements Serializable\" for generated models"));
        this.supportedLibraries.put("<default>", "HTTP client: Jersey client 1.18. JSON processing: Jackson 2.4.2");
        this.supportedLibraries.put("jersey2", "HTTP client: Jersey client 2.6");
        this.cliOptions.add(this.buildLibraryCliOption(this.supportedLibraries));
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
        this.additionalProperties.put("serializableModel", this.serializableModel);
        this.sanitizeConfig();
        final String invokerFolder = (this.sourceFolder + File.separator + this.invokerPackage).replace(".", File.separator);
        this.supportingFiles.add(new SupportingFile("pom.mustache", "", "pom.xml"));
        this.supportingFiles.add(new SupportingFile("ApiClient.mustache", invokerFolder, "ApiClient.java"));
        this.supportingFiles.add(new SupportingFile("apiException.mustache", invokerFolder, "ApiException.java"));
        this.supportingFiles.add(new SupportingFile("Configuration.mustache", invokerFolder, "Configuration.java"));
        this.supportingFiles.add(new SupportingFile("JSON.mustache", invokerFolder, "JSON.java"));
        this.supportingFiles.add(new SupportingFile("Pair.mustache", invokerFolder, "Pair.java"));
        this.supportingFiles.add(new SupportingFile("StringUtil.mustache", invokerFolder, "StringUtil.java"));
        this.supportingFiles.add(new SupportingFile("TypeRef.mustache", invokerFolder, "TypeRef.java"));
        final String authFolder = (this.sourceFolder + File.separator + this.invokerPackage + ".auth").replace(".", File.separator);
        this.supportingFiles.add(new SupportingFile("auth/Authentication.mustache", authFolder, "Authentication.java"));
        this.supportingFiles.add(new SupportingFile("auth/HttpBasicAuth.mustache", authFolder, "HttpBasicAuth.java"));
        this.supportingFiles.add(new SupportingFile("auth/ApiKeyAuth.mustache", authFolder, "ApiKeyAuth.java"));
        this.supportingFiles.add(new SupportingFile("auth/OAuth.mustache", authFolder, "OAuth.java"));
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
        return this.outputFolder + "/" + this.sourceFolder + "/" + this.apiPackage().replace('.', File.separatorChar);
    }
    
    @Override
    public String modelFileFolder() {
        return this.outputFolder + "/" + this.sourceFolder + "/" + this.modelPackage().replace('.', File.separatorChar);
    }
    
    @Override
    public String toVarName(String name) {
        name = name.replaceAll("-", "_");
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
    public String toModelName(final String name) {
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
            return String.format("new ArrayList<%s>()", this.getTypeDeclaration(ap.getItems()));
        }
        if (p instanceof MapProperty) {
            final MapProperty ap2 = (MapProperty)p;
            return String.format("new HashMap<String, %s>()", this.getTypeDeclaration(ap2.getAdditionalProperties()));
        }
        return super.toDefaultValue(p);
    }
    
    @Override
    public String getSwaggerType(final Property p) {
        final String swaggerType = super.getSwaggerType(p);
        String type = null;
        if (this.typeMapping.containsKey(swaggerType)) {
            type = this.typeMapping.get(swaggerType);
            if (this.languageSpecificPrimitives.contains(type)) {
                return this.toModelName(type);
            }
        }
        else {
            type = swaggerType;
        }
        return this.toModelName(type);
    }
    
    @Override
    public String toOperationId(final String operationId) {
        if (StringUtils.isEmpty(operationId)) {
            throw new RuntimeException("Empty method name (operationId) not allowed");
        }
        if (this.reservedWords.contains(operationId)) {
            throw new RuntimeException(operationId + " (reserved word) cannot be used as method name");
        }
        return DefaultCodegen.camelize(operationId, true);
    }
    
    @Override
    public CodegenModel fromModel(final String name, final Model model, final Map<String, Model> allDefinitions) {
        CodegenModel codegenModel = super.fromModel(name, model, allDefinitions);
        if (allDefinitions != null && codegenModel != null && codegenModel.parent != null && codegenModel.hasEnums) {
            final Model parentModel = allDefinitions.get(this.toModelName(codegenModel.parent));
            final CodegenModel parentCodegenModel = super.fromModel(codegenModel.parent, parentModel);
            codegenModel = this.reconcileInlineEnums(codegenModel, parentCodegenModel);
        }
        return codegenModel;
    }
    
    private CodegenModel reconcileInlineEnums(final CodegenModel codegenModel, final CodegenModel parentCodegenModel) {
        if (parentCodegenModel.hasEnums) {
            final List<CodegenProperty> parentModelCodegenProperties = parentCodegenModel.vars;
            final List<CodegenProperty> codegenProperties = codegenModel.vars;
            for (final CodegenProperty parentModelCodegenPropery : parentModelCodegenProperties) {
                if (parentModelCodegenPropery.isEnum) {
                    final Iterator<CodegenProperty> iterator = codegenProperties.iterator();
                    while (iterator.hasNext()) {
                        final CodegenProperty codegenProperty = iterator.next();
                        if (codegenProperty.isEnum && codegenProperty.equals(parentModelCodegenPropery)) {
                            iterator.remove();
                        }
                    }
                }
            }
            codegenModel.vars = codegenProperties;
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
}
