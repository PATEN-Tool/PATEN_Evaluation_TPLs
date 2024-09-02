// 
// Decompiled by Procyon v0.5.36
// 

package io.swagger.codegen.languages;

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
    
    public JavaClientCodegen() {
        this.invokerPackage = "io.swagger.client";
        this.groupId = "io.swagger";
        this.artifactId = "swagger-java-client";
        this.artifactVersion = "1.0.0";
        this.sourceFolder = "src/main/java";
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
        final String invokerFolder = (this.sourceFolder + File.separator + this.invokerPackage).replace(".", File.separator);
        this.supportingFiles.add(new SupportingFile("pom.mustache", "", "pom.xml"));
        this.supportingFiles.add(new SupportingFile("ApiClient.mustache", invokerFolder, "ApiClient.java"));
        this.supportingFiles.add(new SupportingFile("apiException.mustache", invokerFolder, "ApiException.java"));
        this.supportingFiles.add(new SupportingFile("Configuration.mustache", invokerFolder, "Configuration.java"));
        this.supportingFiles.add(new SupportingFile("JsonUtil.mustache", invokerFolder, "JsonUtil.java"));
        this.supportingFiles.add(new SupportingFile("StringUtil.mustache", invokerFolder, "StringUtil.java"));
        final String authFolder = (this.sourceFolder + File.separator + this.invokerPackage + ".auth").replace(".", File.separator);
        this.supportingFiles.add(new SupportingFile("auth/Authentication.mustache", authFolder, "Authentication.java"));
        this.supportingFiles.add(new SupportingFile("auth/HttpBasicAuth.mustache", authFolder, "HttpBasicAuth.java"));
        this.supportingFiles.add(new SupportingFile("auth/ApiKeyAuth.mustache", authFolder, "ApiKeyAuth.java"));
        this.supportingFiles.add(new SupportingFile("auth/OAuth.mustache", authFolder, "OAuth.java"));
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
        if (this.reservedWords.contains(operationId)) {
            throw new RuntimeException(operationId + " (reserved word) cannot be used as method name");
        }
        return DefaultCodegen.camelize(operationId, true);
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
}
