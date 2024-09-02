// 
// Decompiled by Procyon v0.5.36
// 

package springfox.documentation.swagger2.web;

import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMethod;
import java.util.Iterator;
import java.util.List;
import io.swagger.models.Swagger;
import springfox.documentation.service.Documentation;
import org.springframework.http.HttpStatus;
import java.util.Optional;
import springfox.documentation.spring.web.json.Json;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import springfox.documentation.spi.DocumentationType;
import org.springframework.plugin.core.PluginRegistry;
import springfox.documentation.spring.web.json.JsonSerializer;
import springfox.documentation.swagger2.mappers.ServiceModelToSwagger2Mapper;
import springfox.documentation.spring.web.DocumentationCache;
import org.slf4j.Logger;
import springfox.documentation.spring.web.OnReactiveWebApplication;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import springfox.documentation.annotations.ApiIgnore;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ApiIgnore
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@RequestMapping({ "${springfox.documentation.swagger.v2.path:/v2/api-docs}" })
@Conditional({ OnReactiveWebApplication.class })
public class Swagger2ControllerWebFlux
{
    private static final Logger LOGGER;
    private static final String HAL_MEDIA_TYPE = "application/hal+json";
    private final DocumentationCache documentationCache;
    private final ServiceModelToSwagger2Mapper mapper;
    private final JsonSerializer jsonSerializer;
    private final PluginRegistry<WebFluxSwaggerTransformationFilter, DocumentationType> transformations;
    
    @Autowired
    public Swagger2ControllerWebFlux(final DocumentationCache documentationCache, final ServiceModelToSwagger2Mapper mapper, final JsonSerializer jsonSerializer, @Qualifier("webFluxSwaggerTransformationFilterRegistry") final PluginRegistry<WebFluxSwaggerTransformationFilter, DocumentationType> transformations) {
        this.documentationCache = documentationCache;
        this.mapper = mapper;
        this.jsonSerializer = jsonSerializer;
        this.transformations = transformations;
    }
    
    @RequestMapping(method = { RequestMethod.GET }, produces = { "application/json", "application/hal+json" })
    @ResponseBody
    public ResponseEntity<Json> getDocumentation(@RequestParam(value = "group", required = false) final String swaggerGroup, final ServerHttpRequest request) {
        final String groupName = Optional.ofNullable(swaggerGroup).orElse("default");
        final Documentation documentation = this.documentationCache.documentationByGroup(groupName);
        if (documentation == null) {
            Swagger2ControllerWebFlux.LOGGER.warn("Unable to find specification for group {}", (Object)groupName);
            return (ResponseEntity<Json>)new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        final Swagger swagger = this.mapper.mapDocumentation(documentation);
        SwaggerTransformationContext<ServerHttpRequest> context = new SwaggerTransformationContext<ServerHttpRequest>(swagger, request);
        final List<WebFluxSwaggerTransformationFilter> filters = (List<WebFluxSwaggerTransformationFilter>)this.transformations.getPluginsFor((Object)DocumentationType.SWAGGER_2);
        for (final WebFluxSwaggerTransformationFilter each : filters) {
            context = context.next(each.transform(context));
        }
        return (ResponseEntity<Json>)new ResponseEntity((Object)this.jsonSerializer.toJson((Object)context.getSpecification()), HttpStatus.OK);
    }
    
    static {
        LOGGER = LoggerFactory.getLogger((Class)Swagger2ControllerWebFlux.class);
    }
}
