// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.cloud.dataflow.server.controller;

import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.Iterator;
import org.springframework.data.domain.PageImpl;
import java.util.Collection;
import java.util.ArrayList;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskExecutionException;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import java.io.Serializable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.PagedResources;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecutionRel;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;
import org.springframework.cloud.dataflow.server.controller.support.ArgumentSanitizer;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({ "/tasks/executions" })
@ExposesResourceFor(TaskExecutionResource.class)
public class TaskExecutionController
{
    private final Assembler taskAssembler;
    private final TaskService taskService;
    private final TaskExplorer explorer;
    private final TaskDefinitionRepository taskDefinitionRepository;
    private final ArgumentSanitizer argumentSanitizer;
    
    public TaskExecutionController(final TaskExplorer explorer, final TaskService taskService, final TaskDefinitionRepository taskDefinitionRepository) {
        this.taskAssembler = new Assembler();
        this.argumentSanitizer = new ArgumentSanitizer();
        Assert.notNull((Object)explorer, "explorer must not be null");
        Assert.notNull((Object)taskService, "taskService must not be null");
        Assert.notNull((Object)taskDefinitionRepository, "taskDefinitionRepository must not be null");
        this.taskService = taskService;
        this.explorer = explorer;
        this.taskDefinitionRepository = taskDefinitionRepository;
    }
    
    @RequestMapping(value = { "" }, method = { RequestMethod.GET })
    @ResponseStatus(HttpStatus.OK)
    public PagedResources<TaskExecutionResource> list(final Pageable pageable, final PagedResourcesAssembler<TaskJobExecutionRel> assembler) {
        final Page<TaskExecution> taskExecutions = (Page<TaskExecution>)this.explorer.findAll(pageable);
        final Page<TaskJobExecutionRel> result = this.getPageableRelationships(taskExecutions, pageable);
        return (PagedResources<TaskExecutionResource>)assembler.toResource((Page)result, (ResourceAssembler)this.taskAssembler);
    }
    
    @RequestMapping(value = { "" }, method = { RequestMethod.GET }, params = { "name" })
    @ResponseStatus(HttpStatus.OK)
    public PagedResources<TaskExecutionResource> retrieveTasksByName(@RequestParam("name") final String taskName, final Pageable pageable, final PagedResourcesAssembler<TaskJobExecutionRel> assembler) {
        if (this.taskDefinitionRepository.findOne((Serializable)taskName) == null) {
            throw new NoSuchTaskDefinitionException(taskName);
        }
        final Page<TaskExecution> taskExecutions = (Page<TaskExecution>)this.explorer.findTaskExecutionsByName(taskName, pageable);
        final Page<TaskJobExecutionRel> result = this.getPageableRelationships(taskExecutions, pageable);
        return (PagedResources<TaskExecutionResource>)assembler.toResource((Page)result, (ResourceAssembler)this.taskAssembler);
    }
    
    @RequestMapping(value = { "" }, method = { RequestMethod.POST }, params = { "name" })
    @ResponseStatus(HttpStatus.CREATED)
    public long launch(@RequestParam("name") final String taskName, @RequestParam(required = false) final String properties, @RequestParam(required = false) final String arguments) {
        final Map<String, String> propertiesToUse = (Map<String, String>)DeploymentPropertiesUtils.parse(properties);
        DeploymentPropertiesUtils.validateDeploymentProperties((Map)propertiesToUse);
        final List<String> argumentsToUse = (List<String>)DeploymentPropertiesUtils.parseParamList(arguments, " ");
        return this.taskService.executeTask(taskName, propertiesToUse, DeploymentPropertiesUtils.removeQuoting((List)argumentsToUse));
    }
    
    @RequestMapping(value = { "/{id}" }, method = { RequestMethod.GET })
    @ResponseStatus(HttpStatus.OK)
    public TaskExecutionResource view(@PathVariable("id") final long id) {
        TaskExecution taskExecution = this.explorer.getTaskExecution(id);
        if (taskExecution == null) {
            throw new NoSuchTaskExecutionException(id);
        }
        taskExecution = this.sanitizePotentialSensitiveKeys(taskExecution);
        final TaskJobExecutionRel taskJobExecutionRel = new TaskJobExecutionRel(taskExecution, (List)new ArrayList(this.explorer.getJobExecutionIdsByTaskExecutionId(taskExecution.getExecutionId())));
        return this.taskAssembler.toResource(taskJobExecutionRel);
    }
    
    @RequestMapping(value = { "/{id}" }, method = { RequestMethod.DELETE })
    @ResponseStatus(HttpStatus.OK)
    public void cleanup(@PathVariable("id") final long id) {
        final TaskExecution taskExecution = this.explorer.getTaskExecution(id);
        if (taskExecution == null) {
            throw new NoSuchTaskExecutionException(id);
        }
        this.taskService.cleanupExecution(id);
    }
    
    private Page<TaskJobExecutionRel> getPageableRelationships(final Page<TaskExecution> taskExecutions, final Pageable pageable) {
        final List<TaskJobExecutionRel> taskJobExecutionRels = new ArrayList<TaskJobExecutionRel>();
        for (final TaskExecution taskExecution : taskExecutions.getContent()) {
            taskJobExecutionRels.add(new TaskJobExecutionRel(this.sanitizePotentialSensitiveKeys(taskExecution), (List)new ArrayList(this.explorer.getJobExecutionIdsByTaskExecutionId(taskExecution.getExecutionId()))));
        }
        return (Page<TaskJobExecutionRel>)new PageImpl((List)taskJobExecutionRels, pageable, taskExecutions.getTotalElements());
    }
    
    private TaskExecution sanitizePotentialSensitiveKeys(final TaskExecution taskExecution) {
        final List<String> args = (List<String>)taskExecution.getArguments().stream().map(argument -> this.argumentSanitizer.sanitize(argument)).collect(Collectors.toList());
        taskExecution.setArguments((List)args);
        return taskExecution;
    }
    
    private static class Assembler extends ResourceAssemblerSupport<TaskJobExecutionRel, TaskExecutionResource>
    {
        public Assembler() {
            super((Class)TaskExecutionController.class, (Class)TaskExecutionResource.class);
        }
        
        public TaskExecutionResource toResource(final TaskJobExecutionRel taskJobExecutionRel) {
            return (TaskExecutionResource)this.createResourceWithId((Object)taskJobExecutionRel.getTaskExecution().getExecutionId(), (Object)taskJobExecutionRel);
        }
        
        public TaskExecutionResource instantiateResource(final TaskJobExecutionRel taskJobExecutionRel) {
            return new TaskExecutionResource(taskJobExecutionRel);
        }
    }
}
