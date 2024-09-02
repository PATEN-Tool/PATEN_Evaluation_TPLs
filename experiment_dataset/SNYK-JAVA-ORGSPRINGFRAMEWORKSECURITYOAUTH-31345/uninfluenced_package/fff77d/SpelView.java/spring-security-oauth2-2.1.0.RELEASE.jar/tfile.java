// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.security.oauth2.provider.endpoint;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.util.HashMap;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.expression.Expression;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.context.expression.MapAccessor;
import org.springframework.security.oauth2.common.util.RandomValueStringGenerator;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.web.servlet.View;

class SpelView implements View
{
    private final String template;
    private final String prefix;
    private final SpelExpressionParser parser;
    private final StandardEvaluationContext context;
    private PropertyPlaceholderHelper.PlaceholderResolver resolver;
    
    public SpelView(final String template) {
        this.parser = new SpelExpressionParser();
        this.context = new StandardEvaluationContext();
        this.template = template;
        this.prefix = new RandomValueStringGenerator().generate() + "{";
        this.context.addPropertyAccessor((PropertyAccessor)new MapAccessor());
        this.resolver = (PropertyPlaceholderHelper.PlaceholderResolver)new PropertyPlaceholderHelper.PlaceholderResolver() {
            public String resolvePlaceholder(final String name) {
                final Expression expression = SpelView.this.parser.parseExpression(name);
                final Object value = expression.getValue((EvaluationContext)SpelView.this.context);
                return (value == null) ? null : value.toString();
            }
        };
    }
    
    public String getContentType() {
        return "text/html";
    }
    
    public void render(final Map<String, ?> model, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final Map<String, Object> map = new HashMap<String, Object>(model);
        final String path = ServletUriComponentsBuilder.fromContextPath(request).build().getPath();
        map.put("path", (path == null) ? "" : path);
        this.context.setRootObject((Object)map);
        final String maskedTemplate = this.template.replace("${", this.prefix);
        final PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper(this.prefix, "}");
        String result = helper.replacePlaceholders(maskedTemplate, this.resolver);
        result = result.replace(this.prefix, "${");
        response.setContentType(this.getContentType());
        response.getWriter().append(result);
    }
}
