// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.webflow.mvc.view;

import org.apache.commons.logging.LogFactory;
import org.springframework.webflow.validation.ValidationHelper;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageResolver;
import org.springframework.binding.mapping.MappingResult;
import java.lang.reflect.Array;
import org.springframework.validation.BindingResult;
import org.springframework.webflow.core.collection.AttributeMap;
import java.util.List;
import javax.lang.model.SourceVersion;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.PropertyAccessorUtils;
import java.util.ArrayList;
import org.springframework.binding.expression.EvaluationException;
import org.springframework.binding.expression.support.StaticExpression;
import org.springframework.binding.convert.ConversionExecutor;
import org.springframework.binding.expression.ParserContext;
import org.springframework.binding.expression.Expression;
import org.springframework.util.Assert;
import org.springframework.binding.mapping.impl.DefaultMapping;
import org.springframework.binding.expression.support.FluentParserContext;
import java.util.Iterator;
import java.util.Set;
import org.springframework.webflow.core.collection.ParameterMap;
import org.springframework.binding.mapping.impl.DefaultMapper;
import org.springframework.web.util.WebUtils;
import org.springframework.core.style.ToStringCreator;
import org.springframework.webflow.execution.Event;
import java.io.Serializable;
import org.springframework.webflow.definition.TransitionDefinition;
import org.springframework.webflow.execution.FlowExecutionKey;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import org.springframework.webflow.validation.BeanValidationHintResolver;
import org.springframework.binding.expression.beanwrapper.BeanWrapperExpressionParser;
import org.springframework.webflow.validation.ValidationHintResolver;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.webflow.engine.builder.BinderConfiguration;
import org.springframework.binding.mapping.MappingResults;
import org.springframework.validation.Validator;
import org.springframework.binding.convert.ConversionService;
import org.springframework.binding.expression.ExpressionParser;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.binding.mapping.MappingResultsCriteria;
import org.apache.commons.logging.Log;
import org.springframework.webflow.execution.View;

public abstract class AbstractMvcView implements View
{
    private static final Log logger;
    private static final MappingResultsCriteria PROPERTY_NOT_FOUND_ERROR;
    private static final MappingResultsCriteria MAPPING_ERROR;
    private org.springframework.web.servlet.View view;
    private RequestContext requestContext;
    private ExpressionParser expressionParser;
    private final ExpressionParser emptyValueExpressionParser;
    private ConversionService conversionService;
    private Validator validator;
    private String fieldMarkerPrefix;
    private String eventIdParameterName;
    private String eventId;
    private MappingResults mappingResults;
    private BinderConfiguration binderConfiguration;
    private MessageCodesResolver messageCodesResolver;
    private boolean userEventProcessed;
    private ValidationHintResolver validationHintResolver;
    
    public AbstractMvcView(final org.springframework.web.servlet.View view, final RequestContext requestContext) {
        this.emptyValueExpressionParser = (ExpressionParser)new BeanWrapperExpressionParser();
        this.fieldMarkerPrefix = "_";
        this.eventIdParameterName = "_eventId";
        this.validationHintResolver = new BeanValidationHintResolver();
        this.view = view;
        this.requestContext = requestContext;
    }
    
    public void setExpressionParser(final ExpressionParser expressionParser) {
        this.expressionParser = expressionParser;
    }
    
    public void setConversionService(final ConversionService conversionService) {
        this.conversionService = conversionService;
    }
    
    public void setValidator(final Validator validator) {
        this.validator = validator;
    }
    
    public void setValidationHintResolver(final ValidationHintResolver validationHintResolver) {
        if (validationHintResolver != null) {
            this.validationHintResolver = validationHintResolver;
        }
    }
    
    public void setBinderConfiguration(final BinderConfiguration binderConfiguration) {
        this.binderConfiguration = binderConfiguration;
    }
    
    public void setMessageCodesResolver(final MessageCodesResolver messageCodesResolver) {
        this.messageCodesResolver = messageCodesResolver;
    }
    
    public void setFieldMarkerPrefix(final String fieldMarkerPrefix) {
        this.fieldMarkerPrefix = fieldMarkerPrefix;
    }
    
    public void setEventIdParameterName(final String eventIdParameterName) {
        this.eventIdParameterName = eventIdParameterName;
    }
    
    @Override
    public void render() throws IOException {
        final Map<String, Object> model = new HashMap<String, Object>();
        model.putAll(this.flowScopes());
        this.exposeBindingModel(model);
        model.put("flowRequestContext", this.requestContext);
        final FlowExecutionKey key = this.requestContext.getFlowExecutionContext().getKey();
        if (key != null) {
            model.put("flowExecutionKey", this.requestContext.getFlowExecutionContext().getKey().toString());
            model.put("flowExecutionUrl", this.requestContext.getFlowExecutionUrl());
        }
        model.put("currentUser", this.requestContext.getExternalContext().getCurrentUser());
        try {
            if (AbstractMvcView.logger.isDebugEnabled()) {
                AbstractMvcView.logger.debug((Object)("Rendering MVC [" + this.view + "] with model map [" + model + "]"));
            }
            this.doRender(model);
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e2) {
            final IllegalStateException ise = new IllegalStateException("Exception occurred rendering view " + this.view);
            ise.initCause(e2);
            throw ise;
        }
    }
    
    @Override
    public boolean userEventQueued() {
        return !this.userEventProcessed && this.getEventId() != null;
    }
    
    @Override
    public void processUserEvent() {
        final String eventId = this.getEventId();
        if (eventId == null) {
            return;
        }
        if (AbstractMvcView.logger.isDebugEnabled()) {
            AbstractMvcView.logger.debug((Object)("Processing user event '" + eventId + "'"));
        }
        final Object model = this.getModelObject();
        if (model != null) {
            if (AbstractMvcView.logger.isDebugEnabled()) {
                AbstractMvcView.logger.debug((Object)("Resolved model " + model));
            }
            final TransitionDefinition transition = this.requestContext.getMatchingTransition(eventId);
            if (this.shouldBind(model, transition)) {
                this.mappingResults = this.bind(model);
                if (this.hasErrors(this.mappingResults)) {
                    if (AbstractMvcView.logger.isDebugEnabled()) {
                        AbstractMvcView.logger.debug((Object)"Model binding resulted in errors; adding error messages to context");
                    }
                    this.addErrorMessages(this.mappingResults);
                }
                if (this.shouldValidate(model, transition)) {
                    this.validate(model, transition);
                }
            }
        }
        else if (AbstractMvcView.logger.isDebugEnabled()) {
            AbstractMvcView.logger.debug((Object)"No model to bind to; done processing user event");
        }
        this.userEventProcessed = true;
    }
    
    @Override
    public Serializable getUserEventState() {
        return new ViewActionStateHolder(this.eventId, this.userEventProcessed, this.mappingResults);
    }
    
    @Override
    public boolean hasFlowEvent() {
        return this.userEventProcessed && !this.requestContext.getMessageContext().hasErrorMessages();
    }
    
    @Override
    public Event getFlowEvent() {
        if (!this.hasFlowEvent()) {
            return null;
        }
        return new Event(this, this.getEventId(), this.requestContext.getRequestParameters().asAttributeMap());
    }
    
    @Override
    public void saveState() {
    }
    
    @Override
    public String toString() {
        return new ToStringCreator((Object)this).append("view", (Object)this.view).toString();
    }
    
    protected RequestContext getRequestContext() {
        return this.requestContext;
    }
    
    protected org.springframework.web.servlet.View getView() {
        return this.view;
    }
    
    protected ConversionService getConversionService() {
        return this.conversionService;
    }
    
    protected abstract void doRender(final Map<String, ?> p0) throws Exception;
    
    protected String getEventId() {
        if (this.eventId == null) {
            this.eventId = this.determineEventId(this.requestContext);
        }
        return this.eventId;
    }
    
    protected boolean shouldBind(final Object model, final TransitionDefinition transition) {
        return transition == null || transition.getAttributes().getBoolean("bind", true);
    }
    
    protected MappingResults getMappingResults() {
        return this.mappingResults;
    }
    
    protected BinderConfiguration getBinderConfiguration() {
        return this.binderConfiguration;
    }
    
    protected ExpressionParser getExpressionParser() {
        return this.expressionParser;
    }
    
    protected String getFieldMarkerPrefix() {
        return this.fieldMarkerPrefix;
    }
    
    protected String determineEventId(final RequestContext context) {
        return WebUtils.findParameterValue(context.getRequestParameters().asMap(), this.eventIdParameterName);
    }
    
    protected MappingResults bind(final Object model) {
        if (AbstractMvcView.logger.isDebugEnabled()) {
            AbstractMvcView.logger.debug((Object)"Binding to model");
        }
        final DefaultMapper mapper = new DefaultMapper();
        final ParameterMap requestParameters = this.requestContext.getRequestParameters();
        if (this.binderConfiguration != null) {
            this.addModelBindings(mapper, requestParameters.asMap().keySet(), model);
        }
        else {
            this.addDefaultMappings(mapper, requestParameters.asMap().keySet(), model);
        }
        return mapper.map((Object)requestParameters, model);
    }
    
    protected void addModelBindings(final DefaultMapper mapper, final Set<String> parameterNames, final Object model) {
        for (final BinderConfiguration.Binding binding : this.binderConfiguration.getBindings()) {
            final String parameterName = binding.getProperty();
            if (parameterNames.contains(parameterName)) {
                this.addMapping(mapper, binding, model);
            }
            else {
                if (this.fieldMarkerPrefix == null || !parameterNames.contains(this.fieldMarkerPrefix + parameterName)) {
                    continue;
                }
                this.addEmptyValueMapping(mapper, parameterName, model);
            }
        }
    }
    
    protected void addMapping(final DefaultMapper mapper, final BinderConfiguration.Binding binding, final Object model) {
        final Expression source = (Expression)new RequestParameterExpression(binding.getProperty());
        final ParserContext parserContext = (ParserContext)new FluentParserContext().evaluate((Class)model.getClass());
        final Expression target = this.expressionParser.parseExpression(binding.getProperty(), parserContext);
        final DefaultMapping mapping = new DefaultMapping(source, target);
        mapping.setRequired(binding.getRequired());
        if (binding.getConverter() != null) {
            Assert.notNull((Object)this.conversionService, "A ConversionService must be configured to use resolve custom converters to use during binding");
            final ConversionExecutor conversionExecutor = this.conversionService.getConversionExecutor(binding.getConverter(), (Class)String.class, target.getValueType(model));
            mapping.setTypeConverter(conversionExecutor);
        }
        if (AbstractMvcView.logger.isDebugEnabled()) {
            AbstractMvcView.logger.debug((Object)("Adding mapping for parameter '" + binding.getProperty() + "'"));
        }
        mapper.addMapping(mapping);
    }
    
    protected void addDefaultMappings(final DefaultMapper mapper, final Set<String> parameterNames, final Object model) {
        for (final String parameterName : parameterNames) {
            if (this.fieldMarkerPrefix != null && parameterName.startsWith(this.fieldMarkerPrefix)) {
                final String field = parameterName.substring(this.fieldMarkerPrefix.length());
                if (parameterNames.contains(field)) {
                    continue;
                }
                this.addEmptyValueMapping(mapper, field, model);
            }
            else {
                this.addDefaultMapping(mapper, parameterName, model);
            }
        }
    }
    
    protected void addEmptyValueMapping(final DefaultMapper mapper, final String field, final Object model) {
        final ParserContext parserContext = (ParserContext)new FluentParserContext().evaluate((Class)model.getClass());
        final Expression target = this.emptyValueExpressionParser.parseExpression(field, parserContext);
        try {
            final Class<?> propertyType = (Class<?>)target.getValueType(model);
            final Expression source = (Expression)new StaticExpression(this.getEmptyValue(propertyType));
            final DefaultMapping mapping = new DefaultMapping(source, target);
            if (AbstractMvcView.logger.isDebugEnabled()) {
                AbstractMvcView.logger.debug((Object)("Adding empty value mapping for parameter '" + field + "'"));
            }
            mapper.addMapping(mapping);
        }
        catch (EvaluationException ex) {}
    }
    
    protected void addDefaultMapping(final DefaultMapper mapper, final String parameter, final Object model) {
        final Expression source = (Expression)new RequestParameterExpression(parameter);
        final ParserContext parserContext = (ParserContext)new FluentParserContext().evaluate((Class)model.getClass());
        if (this.expressionParser instanceof BeanWrapperExpressionParser || this.checkModelProperty(parameter, model)) {
            final Expression target = this.expressionParser.parseExpression(parameter, parserContext);
            final DefaultMapping mapping = new DefaultMapping(source, target);
            if (AbstractMvcView.logger.isDebugEnabled()) {
                AbstractMvcView.logger.debug((Object)("Adding default mapping for parameter '" + parameter + "'"));
            }
            mapper.addMapping(mapping);
        }
    }
    
    private boolean checkModelProperty(String expression, final Object model) {
        final List<String> propertyNames = new ArrayList<String>();
        while (true) {
            final int index = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(expression);
            String nestedProperty = (index != -1) ? expression.substring(0, index) : expression;
            nestedProperty = PropertyAccessorUtils.getPropertyName(nestedProperty);
            propertyNames.add(nestedProperty);
            if (index == -1) {
                final BeanWrapperImpl beanWrapper = new BeanWrapperImpl(model);
                if (!beanWrapper.isReadableProperty((String)propertyNames.get(0))) {
                    return false;
                }
                for (int i = 0; i < propertyNames.size(); ++i) {
                    if (!SourceVersion.isName(propertyNames.get(i))) {
                        return false;
                    }
                }
                return true;
            }
            else {
                if (expression.length() == index + 1) {
                    return false;
                }
                expression = expression.substring(index + 1);
            }
        }
    }
    
    void restoreState(final ViewActionStateHolder stateHolder) {
        this.eventId = stateHolder.getEventId();
        this.userEventProcessed = stateHolder.getUserEventProcessed();
        this.mappingResults = stateHolder.getMappingResults();
    }
    
    protected boolean shouldValidate(final Object model, final TransitionDefinition transition) {
        final Boolean validateAttribute = this.getValidateAttribute(transition);
        if (validateAttribute != null) {
            return validateAttribute;
        }
        final AttributeMap<Object> flowExecutionAttributes = this.requestContext.getFlowExecutionContext().getAttributes();
        final Boolean validateOnBindingErrors = flowExecutionAttributes.getBoolean("validateOnBindingErrors");
        return validateOnBindingErrors == null || validateOnBindingErrors || !this.mappingResults.hasErrorResults();
    }
    
    private Map<String, Object> flowScopes() {
        if (this.requestContext.getCurrentState().isViewState()) {
            return (Map<String, Object>)this.requestContext.getConversationScope().union(this.requestContext.getFlowScope()).union(this.requestContext.getViewScope()).union(this.requestContext.getFlashScope()).union(this.requestContext.getRequestScope()).asMap();
        }
        return (Map<String, Object>)this.requestContext.getConversationScope().union(this.requestContext.getFlowScope()).union(this.requestContext.getFlashScope()).union(this.requestContext.getRequestScope()).asMap();
    }
    
    private void exposeBindingModel(final Map<String, Object> model) {
        final Object modelObject = this.getModelObject();
        if (modelObject != null) {
            final BindingModel bindingModel = new BindingModel(this.getModelExpression().getExpressionString(), modelObject, this.expressionParser, this.conversionService, this.requestContext.getMessageContext());
            bindingModel.setBinderConfiguration(this.binderConfiguration);
            bindingModel.setMappingResults(this.mappingResults);
            model.put(BindingResult.MODEL_KEY_PREFIX + this.getModelExpression().getExpressionString(), bindingModel);
        }
    }
    
    private Object getModelObject() {
        final Expression model = this.getModelExpression();
        if (model != null) {
            try {
                return model.getValue((Object)this.requestContext);
            }
            catch (EvaluationException e) {
                return null;
            }
        }
        return null;
    }
    
    private Expression getModelExpression() {
        return this.requestContext.getCurrentState().getAttributes().get("model");
    }
    
    private Object getEmptyValue(final Class<?> fieldType) {
        if ((fieldType != null && Boolean.TYPE.equals(fieldType)) || Boolean.class.equals(fieldType)) {
            return false;
        }
        if (fieldType != null && fieldType.isArray()) {
            return Array.newInstance(fieldType.getComponentType(), 0);
        }
        return null;
    }
    
    private boolean hasErrors(final MappingResults results) {
        return results.hasErrorResults() && !this.onlyPropertyNotFoundErrorsPresent(results);
    }
    
    private boolean onlyPropertyNotFoundErrorsPresent(final MappingResults results) {
        return results.getResults(AbstractMvcView.PROPERTY_NOT_FOUND_ERROR).size() == this.mappingResults.getErrorResults().size();
    }
    
    private void addErrorMessages(final MappingResults results) {
        final List<MappingResult> errors = (List<MappingResult>)results.getResults(AbstractMvcView.MAPPING_ERROR);
        for (final MappingResult error : errors) {
            this.requestContext.getMessageContext().addMessage(this.createMessageResolver(error));
        }
    }
    
    protected MessageResolver createMessageResolver(final MappingResult error) {
        final String model = this.getModelExpression().getExpressionString();
        final String field = error.getMapping().getTargetExpression().getExpressionString();
        final Class<?> fieldType = (Class<?>)error.getMapping().getTargetExpression().getValueType(this.getModelObject());
        final String[] messageCodes = this.messageCodesResolver.resolveMessageCodes(error.getCode(), model, field, (Class)fieldType);
        return new MessageBuilder().error().source((Object)field).codes(messageCodes).resolvableArg((Object)field).defaultText(error.getCode() + " on " + field).build();
    }
    
    private Boolean getValidateAttribute(final TransitionDefinition transition) {
        if (transition != null) {
            return transition.getAttributes().getBoolean("validate");
        }
        return null;
    }
    
    private void validate(final Object model, final TransitionDefinition transition) {
        if (AbstractMvcView.logger.isDebugEnabled()) {
            AbstractMvcView.logger.debug((Object)"Validating model");
        }
        final ValidationHelper helper = new ValidationHelper(model, this.requestContext, this.eventId, this.getModelExpression().getExpressionString(), this.expressionParser, this.messageCodesResolver, this.mappingResults, this.validationHintResolver);
        helper.setValidator(this.validator);
        helper.validate();
    }
    
    static {
        logger = LogFactory.getLog((Class)AbstractMvcView.class);
        PROPERTY_NOT_FOUND_ERROR = (MappingResultsCriteria)new PropertyNotFoundError();
        MAPPING_ERROR = (MappingResultsCriteria)new MappingError();
    }
    
    private static class PropertyNotFoundError implements MappingResultsCriteria
    {
        public boolean test(final MappingResult result) {
            return result.isError() && "propertyNotFound".equals(result.getCode());
        }
    }
    
    private static class MappingError implements MappingResultsCriteria
    {
        public boolean test(final MappingResult result) {
            return result.isError() && !AbstractMvcView.PROPERTY_NOT_FOUND_ERROR.test(result);
        }
    }
    
    private static class RequestParameterExpression implements Expression
    {
        private String parameterName;
        
        public RequestParameterExpression(final String parameterName) {
            this.parameterName = parameterName;
        }
        
        public String getExpressionString() {
            return this.parameterName;
        }
        
        public Object getValue(final Object context) throws EvaluationException {
            final ParameterMap parameters = (ParameterMap)context;
            return parameters.asMap().get(this.parameterName);
        }
        
        public Class<?> getValueType(final Object context) {
            return String.class;
        }
        
        public void setValue(final Object context, final Object value) throws EvaluationException {
            throw new UnsupportedOperationException("Setting request parameters is not allowed");
        }
        
        @Override
        public String toString() {
            return "parameter:'" + this.parameterName + "'";
        }
    }
}
