// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.cloud.dataflow.server.controller;

import org.springframework.cloud.dataflow.rest.resource.TaskExecutionsInfoResource;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.data.domain.Sort;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import java.util.Iterator;
import org.springframework.data.domain.PageImpl;
import java.util.HashSet;
import java.util.Arrays;
import org.springframework.cloud.dataflow.server.controller.support.TaskExecutionControllerDeleteAction;
import java.util.Set;
import org.springframework.cloud.dataflow.core.PlatformTaskExecutionInformation;
import org.springframework.cloud.dataflow.rest.resource.CurrentTaskExecutionsResource;
import org.springframework.cloud.dataflow.core.TaskManifest;
import java.util.Collection;
import java.util.ArrayList;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskExecutionException;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.PagedModel;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecutionRel;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;
import org.slf4j.LoggerFactory;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.cloud.dataflow.rest.util.TaskSanitizer;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.dataflow.server.service.TaskDeleteService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionInfoService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({ "/tasks/executions" })
@ExposesResourceFor(TaskExecutionResource.class)
public class TaskExecutionController
{
    private final Assembler taskAssembler;
    private final TaskExecutionService taskExecutionService;
    private final TaskExecutionInfoService taskExecutionInfoService;
    private final TaskDeleteService taskDeleteService;
    private final TaskExplorer explorer;
    private final TaskJobService taskJobService;
    private final TaskDefinitionRepository taskDefinitionRepository;
    private final TaskSanitizer taskSanitizer;
    private final Logger logger;
    private static final List<String> allowedSorts;
    
    public TaskExecutionController(final TaskExplorer explorer, final TaskExecutionService taskExecutionService, final TaskDefinitionRepository taskDefinitionRepository, final TaskExecutionInfoService taskExecutionInfoService, final TaskDeleteService taskDeleteService, final TaskJobService taskJobService) {
        this.taskAssembler = new Assembler();
        this.taskSanitizer = new TaskSanitizer();
        this.logger = LoggerFactory.getLogger((Class)TaskExecutionController.class);
        Assert.notNull((Object)explorer, "explorer must not be null");
        Assert.notNull((Object)taskExecutionService, "taskExecutionService must not be null");
        Assert.notNull((Object)taskDefinitionRepository, "taskDefinitionRepository must not be null");
        Assert.notNull((Object)taskExecutionInfoService, "taskDefinitionRetriever must not be null");
        Assert.notNull((Object)taskDeleteService, "taskDeleteService must not be null");
        Assert.notNull((Object)taskJobService, "taskJobService must not be null");
        this.taskExecutionService = taskExecutionService;
        this.explorer = explorer;
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.taskExecutionInfoService = taskExecutionInfoService;
        this.taskDeleteService = taskDeleteService;
        this.taskJobService = taskJobService;
    }
    
    @RequestMapping(value = { "" }, method = { RequestMethod.GET })
    @ResponseStatus(HttpStatus.OK)
    public PagedModel<TaskExecutionResource> list(final Pageable pageable, final PagedResourcesAssembler<TaskJobExecutionRel> assembler) {
        validatePageable(pageable);
        final Page<TaskExecution> taskExecutions = (Page<TaskExecution>)this.explorer.findAll(pageable);
        final Page<TaskJobExecutionRel> result = this.getPageableRelationships(taskExecutions, pageable);
        return (PagedModel<TaskExecutionResource>)assembler.toModel((Page)result, (RepresentationModelAssembler)this.taskAssembler);
    }
    
    @RequestMapping(value = { "" }, method = { RequestMethod.GET }, params = { "name" })
    @ResponseStatus(HttpStatus.OK)
    public PagedModel<TaskExecutionResource> retrieveTasksByName(@RequestParam("name") final String taskName, final Pageable pageable, final PagedResourcesAssembler<TaskJobExecutionRel> assembler) {
        validatePageable(pageable);
        this.taskDefinitionRepository.findById((Object)taskName).orElseThrow(() -> new NoSuchTaskDefinitionException(taskName));
        final Page<TaskExecution> taskExecutions = (Page<TaskExecution>)this.explorer.findTaskExecutionsByName(taskName, pageable);
        final Page<TaskJobExecutionRel> result = this.getPageableRelationships(taskExecutions, pageable);
        return (PagedModel<TaskExecutionResource>)assembler.toModel((Page)result, (RepresentationModelAssembler)this.taskAssembler);
    }
    
    @RequestMapping(value = { "" }, method = { RequestMethod.POST }, params = { "name" })
    @ResponseStatus(HttpStatus.CREATED)
    public long launch(@RequestParam("name") final String taskName, @RequestParam(required = false) final String properties, @RequestParam(required = false) final String arguments) {
        final Map<String, String> propertiesToUse = (Map<String, String>)DeploymentPropertiesUtils.parse(properties);
        final List<String> argumentsToUse = (List<String>)DeploymentPropertiesUtils.parseArgumentList(arguments, " ");
        return this.taskExecutionService.executeTask(taskName, propertiesToUse, argumentsToUse);
    }
    
    @RequestMapping(value = { "/{id}" }, method = { RequestMethod.GET })
    @ResponseStatus(HttpStatus.OK)
    public TaskExecutionResource view(@PathVariable("id") final long id) {
        TaskExecution taskExecution = this.explorer.getTaskExecution(id);
        if (taskExecution == null) {
            throw new NoSuchTaskExecutionException(id);
        }
        taskExecution = this.taskSanitizer.sanitizeTaskExecutionArguments(taskExecution);
        TaskManifest taskManifest = this.taskExecutionService.findTaskManifestById(id);
        taskManifest = this.taskSanitizer.sanitizeTaskManifest(taskManifest);
        final List<Long> jobExecutionIds = new ArrayList<Long>(this.explorer.getJobExecutionIdsByTaskExecutionId(taskExecution.getExecutionId()));
        final TaskJobExecutionRel taskJobExecutionRel = new TaskJobExecutionRel(taskExecution, (List)jobExecutionIds, taskManifest, this.getCtrTaskJobExecution(taskExecution, jobExecutionIds));
        return this.taskAssembler.toModel(taskJobExecutionRel);
    }
    
    @RequestMapping(value = { "/current" }, method = { RequestMethod.GET })
    @ResponseStatus(HttpStatus.OK)
    public Collection<CurrentTaskExecutionsResource> getCurrentTaskExecutionsInfo() {
        final List<PlatformTaskExecutionInformation> executionInformation = (List<PlatformTaskExecutionInformation>)this.taskExecutionInfoService.findAllPlatformTaskExecutionInformation().getTaskExecutionInformation();
        final List<CurrentTaskExecutionsResource> resources = new ArrayList<CurrentTaskExecutionsResource>();
        final CurrentTaskExecutionsResource currentTaskExecutionsResource;
        final List<CurrentTaskExecutionsResource> list;
        executionInformation.forEach(platformTaskExecutionInformation -> {
            currentTaskExecutionsResource = CurrentTaskExecutionsResource.fromTaskExecutionInformation(platformTaskExecutionInformation);
            list.add(currentTaskExecutionsResource);
            return;
        });
        return resources;
    }
    
    @RequestMapping(value = { "/{id}" }, method = { RequestMethod.DELETE })
    @ResponseStatus(HttpStatus.OK)
    public void cleanup(@PathVariable("id") final Set<Long> ids, @RequestParam(defaultValue = "CLEANUP", name = "action") final TaskExecutionControllerDeleteAction[] actions) {
        final Set<TaskExecutionControllerDeleteAction> actionsAsSet = new HashSet<TaskExecutionControllerDeleteAction>(Arrays.asList(actions));
        this.taskDeleteService.cleanupExecutions(actionsAsSet, ids);
    }
    
    @RequestMapping(method = { RequestMethod.DELETE })
    @ResponseStatus(HttpStatus.OK)
    public void cleanupAll(@RequestParam(defaultValue = "CLEANUP", name = "action") final TaskExecutionControllerDeleteAction[] actions, @RequestParam(defaultValue = "false", name = "completed") final boolean completed, @RequestParam(defaultValue = "", name = "name") final String taskName) {
        this.taskDeleteService.cleanupExecutions(new HashSet<TaskExecutionControllerDeleteAction>(Arrays.asList(actions)), this.taskExecutionService.getAllTaskExecutionIds(completed, taskName));
    }
    
    @RequestMapping(value = { "/{id}" }, method = { RequestMethod.POST })
    @ResponseStatus(HttpStatus.OK)
    public void stop(@PathVariable("id") final Set<Long> ids, @RequestParam(defaultValue = "", name = "platform") final String platform) {
        this.taskExecutionService.stopTaskExecution(ids, platform);
    }
    
    private Page<TaskJobExecutionRel> getPageableRelationships(final Page<TaskExecution> taskExecutions, final Pageable pageable) {
        final List<TaskJobExecutionRel> taskJobExecutionRels = new ArrayList<TaskJobExecutionRel>();
        for (final TaskExecution taskExecution : taskExecutions.getContent()) {
            TaskManifest taskManifest = this.taskExecutionService.findTaskManifestById(taskExecution.getExecutionId());
            taskManifest = this.taskSanitizer.sanitizeTaskManifest(taskManifest);
            final List<Long> jobExecutionIds = new ArrayList<Long>(this.explorer.getJobExecutionIdsByTaskExecutionId(taskExecution.getExecutionId()));
            taskJobExecutionRels.add(new TaskJobExecutionRel(this.taskSanitizer.sanitizeTaskExecutionArguments(taskExecution), (List)jobExecutionIds, taskManifest, this.getCtrTaskJobExecution(taskExecution, jobExecutionIds)));
        }
        return (Page<TaskJobExecutionRel>)new PageImpl((List)taskJobExecutionRels, pageable, taskExecutions.getTotalElements());
    }
    
    private TaskJobExecution getCtrTaskJobExecution(final TaskExecution taskExecution, final List<Long> jobExecutionIds) {
        TaskJobExecution taskJobExecution = null;
        final TaskDefinition taskDefinition = this.taskDefinitionRepository.findByTaskName(taskExecution.getTaskName());
        if (taskDefinition != null) {
            final TaskParser parser = new TaskParser(taskExecution.getTaskName(), taskDefinition.getDslText(), true, false);
            if (jobExecutionIds.size() > 0 && parser.parse().isComposed()) {
                try {
                    taskJobExecution = this.taskJobService.getJobExecution(jobExecutionIds.toArray(new Long[0])[0]);
                }
                catch (NoSuchJobExecutionException noSuchJobExecutionException) {
                    this.logger.warn(String.format("Job Execution for Task Execution %s could not be found.", taskExecution.getExecutionId()), (Throwable)noSuchJobExecutionException);
                }
            }
        }
        return taskJobExecution;
    }
    
    private static void validatePageable(final Pageable pageable) {
        if (pageable != null) {
            final Sort sort = pageable.getSort();
            if (sort != null) {
                for (final Sort.Order order : sort) {
                    final String property = order.getProperty();
                    if (property != null && !TaskExecutionController.allowedSorts.contains(property.toUpperCase())) {
                        throw new IllegalArgumentException("Sorting column " + order.getProperty() + " not allowed");
                    }
                }
            }
        }
    }
    
    static {
        allowedSorts = Arrays.asList("TASK_EXECUTION_ID", "START_TIME", "END_TIME", "TASK_NAME", "EXIT_CODE", "EXIT_MESSAGE", "ERROR_MESSAGE", "LAST_UPDATED", "EXTERNAL_EXECUTION_ID", "PARENT_EXECUTION_ID");
    }
    
    private static class Assembler extends RepresentationModelAssemblerSupport<TaskJobExecutionRel, TaskExecutionResource>
    {
        public Assembler() {
            super((Class)TaskExecutionController.class, (Class)TaskExecutionResource.class);
        }
        
        public TaskExecutionResource toModel(final TaskJobExecutionRel taskJobExecutionRel) {
            return (TaskExecutionResource)this.createModelWithId((Object)taskJobExecutionRel.getTaskExecution().getExecutionId(), (Object)taskJobExecutionRel);
        }
        
        public TaskExecutionResource instantiateModel(final TaskJobExecutionRel taskJobExecutionRel) {
            return new TaskExecutionResource(taskJobExecutionRel);
        }
    }
    
    private static class TaskExecutionsAssembler extends RepresentationModelAssemblerSupport<Integer, TaskExecutionsInfoResource>
    {
        public TaskExecutionsAssembler() {
            super((Class)TaskExecutionController.class, (Class)TaskExecutionsInfoResource.class);
        }
        
        public TaskExecutionsInfoResource toModel(final Integer totalExecutions) {
            final TaskExecutionsInfoResource taskExecutionsInfoResource = new TaskExecutionsInfoResource();
            taskExecutionsInfoResource.setTotalExecutions(totalExecutions);
            return (TaskExecutionsInfoResource)this.createModelWithId((Object)taskExecutionsInfoResource, (Object)totalExecutions);
        }
        
        public TaskExecutionsInfoResource instantiateModel(final Integer totalExecutions) {
            final TaskExecutionsInfoResource taskExecutionsInfoResource = new TaskExecutionsInfoResource();
            taskExecutionsInfoResource.setTotalExecutions(totalExecutions);
            return taskExecutionsInfoResource;
        }
    }
}
