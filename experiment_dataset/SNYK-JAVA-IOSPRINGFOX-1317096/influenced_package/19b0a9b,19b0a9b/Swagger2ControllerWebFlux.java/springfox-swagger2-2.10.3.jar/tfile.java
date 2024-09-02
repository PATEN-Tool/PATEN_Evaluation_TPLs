// 
// Decompiled by Procyon v0.5.36
// 

package springfox.documentation.swagger2.web;

import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ResponseBody;
import springfox.documentation.spring.web.PropertySourcedMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.models.Swagger;
import springfox.documentation.service.Documentation;
import org.springframework.util.StringUtils;
import org.springframework.http.HttpStatus;
import java.util.Optional;
import springfox.documentation.spring.web.json.Json;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import springfox.documentation.spring.web.json.JsonSerializer;
import springfox.documentation.swagger2.mappers.ServiceModelToSwagger2Mapper;
import springfox.documentation.spring.web.DocumentationCache;
import org.slf4j.Logger;
import springfox.documentation.annotations.ApiIgnore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Controller;

@Controller
@ConditionalOnClass(name = { "org.springframework.web.reactive.BindingContext" })
@ApiIgnore
public class Swagger2ControllerWebFlux
{
    private static final String DEFAULT_URL = "/v2/api-docs";
    private static final Logger LOGGER;
    private static final String HAL_MEDIA_TYPE = "application/hal+json";
    private final DocumentationCache documentationCache;
    private final ServiceModelToSwagger2Mapper mapper;
    private final JsonSerializer jsonSerializer;
    
    @Autowired
    public Swagger2ControllerWebFlux(final DocumentationCache documentationCache, final ServiceModelToSwagger2Mapper mapper, final JsonSerializer jsonSerializer) {
        this.documentationCache = documentationCache;
        this.mapper = mapper;
        this.jsonSerializer = jsonSerializer;
    }
    
    @RequestMapping(value = { "/v2/api-docs" }, method = { RequestMethod.GET }, produces = { "application/json", "application/hal+json" })
    @PropertySourcedMapping(value = "${springfox.documentation.swagger.v2.path}", propertyKey = "springfox.documentation.swagger.v2.path")
    @ResponseBody
    public ResponseEntity<Json> getDocumentation(@RequestParam(value = "group", required = false) final String swaggerGroup, final ServerHttpRequest request) {
        final String groupName = Optional.ofNullable(swaggerGroup).orElse("default");
        final Documentation documentation = this.documentationCache.documentationByGroup(groupName);
        if (documentation == null) {
            Swagger2ControllerWebFlux.LOGGER.warn("Unable to find specification for group {}", (Object)groupName);
            return (ResponseEntity<Json>)new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        final Swagger swagger = this.mapper.mapDocumentation(documentation);
        swagger.basePath(StringUtils.isEmpty((Object)request.getPath().contextPath().value()) ? "/" : request.getPath().contextPath().value());
        if (StringUtils.isEmpty((Object)swagger.getHost())) {
            swagger.host(request.getURI().getAuthority());
        }
        return (ResponseEntity<Json>)new ResponseEntity((Object)this.jsonSerializer.toJson((Object)swagger), HttpStatus.OK);
    }
    
    static {
        LOGGER = LoggerFactory.getLogger((Class)Swagger2ControllerWebFlux.class);
    }
}
