// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.cloud.dataflow.server.controller;

import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import java.util.Iterator;
import org.springframework.data.domain.PageImpl;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskExecutionException;
import org.springframework.web.bind.annotation.PathVariable;
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
import org.springframework.cloud.task.repository.TaskExplorer;
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
    private TaskExplorer explorer;
    
    public TaskExecutionController(final TaskExplorer explorer) {
        this.taskAssembler = new Assembler();
        Assert.notNull((Object)explorer, "explorer must not be null");
        this.explorer = explorer;
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
        final Page<TaskExecution> taskExecutions = (Page<TaskExecution>)this.explorer.findTaskExecutionsByName(taskName, pageable);
        final Page<TaskJobExecutionRel> result = this.getPageableRelationships(taskExecutions, pageable);
        return (PagedResources<TaskExecutionResource>)assembler.toResource((Page)result, (ResourceAssembler)this.taskAssembler);
    }
    
    @RequestMapping(value = { "/{id}" }, method = { RequestMethod.GET })
    @ResponseStatus(HttpStatus.OK)
    public TaskExecutionResource view(@PathVariable("id") final long id) {
        final TaskExecution taskExecution = this.explorer.getTaskExecution(id);
        if (taskExecution == null) {
            throw new NoSuchTaskExecutionException(id);
        }
        final TaskJobExecutionRel taskJobExecutionRel = new TaskJobExecutionRel(taskExecution, (List)new ArrayList(this.explorer.getJobExecutionIdsByTaskExecutionId(taskExecution.getExecutionId())));
        return this.taskAssembler.toResource(taskJobExecutionRel);
    }
    
    private Page<TaskJobExecutionRel> getPageableRelationships(final Page<TaskExecution> taskExecutions, final Pageable pageable) {
        final List<TaskJobExecutionRel> taskJobExecutionRels = new ArrayList<TaskJobExecutionRel>();
        for (final TaskExecution taskExecution : taskExecutions.getContent()) {
            taskJobExecutionRels.add(new TaskJobExecutionRel(taskExecution, (List)new ArrayList(this.explorer.getJobExecutionIdsByTaskExecutionId(taskExecution.getExecutionId()))));
        }
        return (Page<TaskJobExecutionRel>)new PageImpl((List)taskJobExecutionRels, pageable, taskExecutions.getTotalElements());
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
