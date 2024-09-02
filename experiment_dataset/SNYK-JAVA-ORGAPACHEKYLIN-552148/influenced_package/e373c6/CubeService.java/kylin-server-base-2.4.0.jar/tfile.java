// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.kylin.rest.service;

import java.util.HashMap;
import org.slf4j.LoggerFactory;
import org.apache.kylin.job.common.PatternedLogger;
import org.apache.kylin.common.util.CliCommandExecutor;
import com.google.common.base.Preconditions;
import org.apache.kylin.rest.exception.InternalErrorException;
import org.apache.kylin.engine.mr.common.CuboidRecommenderUtil;
import org.apache.kylin.cube.cuboid.Cuboid;
import org.apache.kylin.rest.response.CuboidTreeResponse;
import org.apache.kylin.cube.cuboid.CuboidScheduler;
import org.apache.kylin.rest.response.CubeInstanceResponse;
import org.apache.kylin.metadata.cachesync.Broadcaster;
import org.apache.kylin.metadata.model.FunctionDesc;
import org.apache.kylin.metadata.model.MeasureDesc;
import org.apache.kylin.metadata.draft.Draft;
import org.apache.kylin.common.persistence.RootPersistentEntity;
import org.springframework.security.access.AccessDeniedException;
import org.apache.kylin.rest.exception.ForbiddenException;
import org.apache.kylin.metadata.model.DataModelDesc;
import org.apache.kylin.job.execution.DefaultChainedExecutable;
import org.apache.kylin.job.execution.AbstractExecutable;
import org.apache.kylin.engine.EngineFactory;
import org.apache.kylin.metadata.model.SegmentRange;
import java.util.Collection;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Map;
import com.google.common.collect.Maps;
import org.apache.kylin.cube.CubeManager;
import org.apache.kylin.cube.CubeSegment;
import org.apache.kylin.common.KylinConfig;
import java.util.Date;
import org.apache.kylin.rest.response.MetricsResponse;
import org.apache.kylin.rest.request.MetricsRequest;
import org.apache.kylin.metadata.model.SegmentStatusEnum;
import org.apache.kylin.metadata.realization.RealizationStatusEnum;
import org.apache.kylin.engine.mr.CubingJob;
import java.util.Set;
import java.util.EnumSet;
import org.apache.kylin.job.execution.ExecutableState;
import org.apache.kylin.metadata.project.ProjectManager;
import org.apache.kylin.metadata.realization.RealizationType;
import org.apache.kylin.metadata.project.RealizationEntry;
import java.util.Collections;
import org.springframework.security.access.prepost.PreAuthorize;
import org.apache.kylin.rest.msg.Message;
import org.apache.kylin.cube.cuboid.CuboidCLI;
import org.apache.kylin.rest.exception.BadRequestException;
import org.apache.kylin.rest.msg.MsgPicker;
import org.apache.kylin.cube.model.CubeDesc;
import org.apache.kylin.metadata.project.ProjectInstance;
import java.io.IOException;
import org.apache.kylin.cube.CubeUpdate;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import org.apache.kylin.cube.CubeInstance;
import org.apache.kylin.rest.util.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import com.google.common.cache.CacheBuilder;
import org.apache.kylin.rest.util.AclEvaluate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.kylin.rest.response.HBaseResponse;
import com.google.common.cache.Cache;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.InitializingBean;

@Component("cubeMgmtService")
public class CubeService extends BasicService implements InitializingBean
{
    private static final Logger logger;
    protected Cache<String, HBaseResponse> htableInfoCache;
    @Autowired
    @Qualifier("projectService")
    private ProjectService projectService;
    @Autowired
    @Qualifier("jobService")
    private JobService jobService;
    @Autowired
    @Qualifier("modelMgmtService")
    private ModelService modelService;
    @Autowired
    private AclEvaluate aclEvaluate;
    
    public CubeService() {
        this.htableInfoCache = (Cache<String, HBaseResponse>)CacheBuilder.newBuilder().build();
    }
    
    public boolean isCubeNameVaildate(final String cubeName) {
        if (StringUtils.isEmpty(cubeName) || !ValidateUtil.isAlphanumericUnderscore(cubeName)) {
            return false;
        }
        for (final CubeInstance cubeInstance : this.getCubeManager().listAllCubes()) {
            if (cubeName.equalsIgnoreCase(cubeInstance.getName())) {
                return false;
            }
        }
        return true;
    }
    
    public List<CubeInstance> listAllCubes(final String cubeName, final String projectName, final String modelName, final boolean exactMatch) {
        List<CubeInstance> cubeInstances = null;
        if (null == projectName) {
            cubeInstances = (List<CubeInstance>)this.getCubeManager().listAllCubes();
            this.aclEvaluate.checkIsGlobalAdmin();
        }
        else {
            cubeInstances = this.listAllCubes(projectName);
            this.aclEvaluate.checkProjectReadPermission(projectName);
        }
        List<CubeInstance> filterModelCubes = new ArrayList<CubeInstance>();
        if (modelName != null) {
            for (final CubeInstance cubeInstance : cubeInstances) {
                final boolean isModelMatch = cubeInstance.getDescriptor().getModelName().equalsIgnoreCase(modelName);
                if (isModelMatch) {
                    filterModelCubes.add(0, cubeInstance);
                }
            }
        }
        else {
            filterModelCubes = cubeInstances;
        }
        final List<CubeInstance> filterCubes = new ArrayList<CubeInstance>();
        for (final CubeInstance cubeInstance2 : filterModelCubes) {
            final boolean isCubeMatch = null == cubeName || (!exactMatch && cubeInstance2.getName().toLowerCase().contains(cubeName.toLowerCase())) || (exactMatch && cubeInstance2.getName().toLowerCase().equals(cubeName.toLowerCase()));
            if (isCubeMatch) {
                filterCubes.add(cubeInstance2);
            }
        }
        return filterCubes;
    }
    
    public CubeInstance updateCubeCost(final CubeInstance cube, final int cost) throws IOException {
        this.aclEvaluate.checkProjectWritePermission(cube);
        if (cube.getCost() == cost) {
            return cube;
        }
        cube.setCost(cost);
        final String owner = SecurityContextHolder.getContext().getAuthentication().getName();
        cube.setOwner(owner);
        final CubeUpdate update = new CubeUpdate(cube.latestCopyForWrite()).setOwner(owner).setCost(cost);
        return this.getCubeManager().updateCube(update);
    }
    
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#project, 'ADMINISTRATION') or hasPermission(#project, 'MANAGEMENT')")
    public CubeInstance createCubeAndDesc(final ProjectInstance project, final CubeDesc desc) throws IOException {
        final Message msg = MsgPicker.getMsg();
        final String cubeName = desc.getName();
        if (this.getCubeManager().getCube(cubeName) != null) {
            throw new BadRequestException(String.format(msg.getCUBE_ALREADY_EXIST(), cubeName));
        }
        if (this.getCubeDescManager().getCubeDesc(desc.getName()) != null) {
            throw new BadRequestException(String.format(msg.getCUBE_DESC_ALREADY_EXIST(), desc.getName()));
        }
        final String owner = SecurityContextHolder.getContext().getAuthentication().getName();
        final CubeDesc createdDesc = this.getCubeDescManager().createCubeDesc(desc);
        if (createdDesc.isBroken()) {
            throw new BadRequestException(createdDesc.getErrorsAsString());
        }
        final int cuboidCount = CuboidCLI.simulateCuboidGeneration(createdDesc, false);
        CubeService.logger.info("New cube " + cubeName + " has " + cuboidCount + " cuboids");
        final CubeInstance createdCube = this.getCubeManager().createCube(cubeName, project.getName(), createdDesc, owner);
        return createdCube;
    }
    
    public List<CubeInstance> listAllCubes(final String projectName) {
        final ProjectManager projectManager = this.getProjectManager();
        final ProjectInstance project = projectManager.getProject(projectName);
        if (project == null) {
            return Collections.emptyList();
        }
        final ArrayList<CubeInstance> result = new ArrayList<CubeInstance>();
        for (final RealizationEntry projectDataModel : project.getRealizationEntries()) {
            if (projectDataModel.getType() == RealizationType.CUBE) {
                final CubeInstance cube = this.getCubeManager().getCube(projectDataModel.getRealization());
                if (cube != null) {
                    result.add(cube);
                }
                else {
                    CubeService.logger.error("Cube instance " + projectDataModel.getRealization() + " is failed to load");
                }
            }
        }
        return result;
    }
    
    protected boolean isCubeInProject(final String projectName, final CubeInstance target) {
        final ProjectManager projectManager = this.getProjectManager();
        final ProjectInstance project = projectManager.getProject(projectName);
        if (project == null) {
            return false;
        }
        for (final RealizationEntry projectDataModel : project.getRealizationEntries()) {
            if (projectDataModel.getType() == RealizationType.CUBE) {
                final CubeInstance cube = this.getCubeManager().getCube(projectDataModel.getRealization());
                if (cube == null) {
                    CubeService.logger.error("Project " + projectName + " contains realization " + projectDataModel.getRealization() + " which is not found by CubeManager");
                }
                else {
                    if (cube.equals((Object)target)) {
                        return true;
                    }
                    continue;
                }
            }
        }
        return false;
    }
    
    public CubeDesc updateCubeAndDesc(final CubeInstance cube, final CubeDesc desc, final String newProjectName, final boolean forceUpdate) throws IOException {
        this.aclEvaluate.checkProjectWritePermission(cube);
        final Message msg = MsgPicker.getMsg();
        final List<CubingJob> cubingJobs = this.jobService.listJobsByRealizationName(cube.getName(), null, EnumSet.of(ExecutableState.READY, ExecutableState.RUNNING));
        if (!cubingJobs.isEmpty()) {
            throw new BadRequestException(String.format(msg.getDISCARD_JOB_FIRST(), cube.getName()));
        }
        if (!forceUpdate && !cube.getDescriptor().consistentWith(desc)) {
            throw new BadRequestException(String.format(msg.getINCONSISTENT_CUBE_DESC(), desc.getName()));
        }
        final CubeDesc updatedCubeDesc = this.getCubeDescManager().updateCubeDesc(desc);
        final int cuboidCount = CuboidCLI.simulateCuboidGeneration(updatedCubeDesc, false);
        CubeService.logger.info("Updated cube " + cube.getName() + " has " + cuboidCount + " cuboids");
        final ProjectManager projectManager = this.getProjectManager();
        if (!this.isCubeInProject(newProjectName, cube)) {
            final String owner = SecurityContextHolder.getContext().getAuthentication().getName();
            projectManager.moveRealizationToProject(RealizationType.CUBE, cube.getName(), newProjectName, owner);
        }
        return updatedCubeDesc;
    }
    
    public void deleteCube(final CubeInstance cube) throws IOException {
        this.aclEvaluate.checkProjectWritePermission(cube);
        final Message msg = MsgPicker.getMsg();
        final List<CubingJob> cubingJobs = this.jobService.listJobsByRealizationName(cube.getName(), null, EnumSet.of(ExecutableState.READY, ExecutableState.RUNNING, ExecutableState.ERROR));
        if (!cubingJobs.isEmpty()) {
            throw new BadRequestException(String.format(msg.getDISCARD_JOB_FIRST(), cube.getName()));
        }
        try {
            this.releaseAllJobs(cube);
        }
        catch (Exception e) {
            CubeService.logger.error("error when releasing all jobs", (Throwable)e);
        }
        final int cubeNum = this.getCubeManager().getCubesByDesc(cube.getDescriptor().getName()).size();
        this.getCubeManager().dropCube(cube.getName(), cubeNum == 1);
    }
    
    public CubeInstance purgeCube(final CubeInstance cube) throws IOException {
        this.aclEvaluate.checkProjectOperationPermission(cube);
        final Message msg = MsgPicker.getMsg();
        final String cubeName = cube.getName();
        final List<CubingJob> cubingJobs = this.jobService.listJobsByRealizationName(cubeName, null, EnumSet.of(ExecutableState.READY, ExecutableState.RUNNING, ExecutableState.ERROR, ExecutableState.STOPPED));
        if (!cubingJobs.isEmpty()) {
            throw new BadRequestException(String.format(msg.getDISCARD_JOB_FIRST(), cubeName));
        }
        final RealizationStatusEnum ostatus = cube.getStatus();
        if (null != ostatus && !RealizationStatusEnum.DISABLED.equals((Object)ostatus)) {
            throw new BadRequestException(String.format(msg.getPURGE_NOT_DISABLED_CUBE(), cubeName, ostatus));
        }
        this.releaseAllSegments(cube);
        return cube;
    }
    
    public CubeInstance disableCube(final CubeInstance cube) throws IOException {
        this.aclEvaluate.checkProjectWritePermission(cube);
        final Message msg = MsgPicker.getMsg();
        final String cubeName = cube.getName();
        final RealizationStatusEnum ostatus = cube.getStatus();
        if (null != ostatus && !RealizationStatusEnum.READY.equals((Object)ostatus)) {
            throw new BadRequestException(String.format(msg.getDISABLE_NOT_READY_CUBE(), cubeName, ostatus));
        }
        return this.getCubeManager().updateCubeStatus(cube, RealizationStatusEnum.DISABLED);
    }
    
    public void checkEnableCubeCondition(final CubeInstance cube) {
        this.aclEvaluate.checkProjectWritePermission(cube);
        final Message msg = MsgPicker.getMsg();
        final String cubeName = cube.getName();
        final RealizationStatusEnum ostatus = cube.getStatus();
        if (!cube.getStatus().equals((Object)RealizationStatusEnum.DISABLED)) {
            throw new BadRequestException(String.format(msg.getENABLE_NOT_DISABLED_CUBE(), cubeName, ostatus));
        }
        if (cube.getSegments(SegmentStatusEnum.READY).size() == 0) {
            throw new BadRequestException(String.format(msg.getNO_READY_SEGMENT(), cubeName));
        }
        if (!cube.getDescriptor().checkSignature()) {
            throw new BadRequestException(String.format(msg.getINCONSISTENT_CUBE_DESC_SIGNATURE(), cube.getDescriptor()));
        }
    }
    
    public CubeInstance enableCube(final CubeInstance cube) throws IOException {
        return this.getCubeManager().updateCubeStatus(cube, RealizationStatusEnum.READY);
    }
    
    public MetricsResponse calculateMetrics(final MetricsRequest request) {
        final List<CubeInstance> cubes = (List<CubeInstance>)this.getCubeManager().listAllCubes();
        final MetricsResponse metrics = new MetricsResponse();
        final Date startTime = (null == request.getStartTime()) ? new Date(-1L) : request.getStartTime();
        final Date endTime = (null == request.getEndTime()) ? new Date() : request.getEndTime();
        metrics.increase("totalCubes", 0.0f);
        metrics.increase("totalStorage", 0.0f);
        for (final CubeInstance cube : cubes) {
            Date createdDate = new Date(-1L);
            createdDate = ((cube.getCreateTimeUTC() == 0L) ? createdDate : new Date(cube.getCreateTimeUTC()));
            if (createdDate.getTime() > startTime.getTime() && createdDate.getTime() < endTime.getTime()) {
                metrics.increase("totalCubes");
            }
        }
        metrics.increase("aveStorage", (((HashMap<K, Float>)metrics).get("totalCubes") == 0.0f) ? 0.0f : (((HashMap<K, Float>)metrics).get("totalStorage") / ((HashMap<K, Float>)metrics).get("totalCubes")));
        return metrics;
    }
    
    public HBaseResponse getHTableInfo(final String cubeName, final String tableName) throws IOException {
        final String key = cubeName + "/" + tableName;
        HBaseResponse hr = (HBaseResponse)this.htableInfoCache.getIfPresent((Object)key);
        if (null != hr) {
            return hr;
        }
        hr = new HBaseResponse();
        if ("hbase".equals(this.getConfig().getMetadataUrl().getScheme())) {
            try {
                CubeService.logger.debug("Loading HTable info " + cubeName + ", " + tableName);
                hr = (HBaseResponse)Class.forName("org.apache.kylin.rest.service.HBaseInfoUtil").getMethod("getHBaseInfo", String.class, KylinConfig.class).invoke(null, tableName, this.getConfig());
            }
            catch (Throwable e) {
                throw new IOException(e);
            }
        }
        this.htableInfoCache.put((Object)key, (Object)hr);
        return hr;
    }
    
    public void updateCubeNotifyList(final CubeInstance cube, final List<String> notifyList) throws IOException {
        this.aclEvaluate.checkProjectOperationPermission(cube);
        final CubeDesc desc = cube.getDescriptor();
        desc.setNotifyList((List)notifyList);
        this.getCubeDescManager().updateCubeDesc(desc);
    }
    
    public CubeInstance rebuildLookupSnapshot(final CubeInstance cube, final String segmentName, final String lookupTable) throws IOException {
        this.aclEvaluate.checkProjectOperationPermission(cube);
        final CubeSegment seg = cube.getSegment(segmentName, SegmentStatusEnum.READY);
        this.getCubeManager().buildSnapshotTable(seg, lookupTable);
        return cube;
    }
    
    public CubeInstance deleteSegment(final CubeInstance cube, final String segmentName) throws IOException {
        this.aclEvaluate.checkProjectOperationPermission(cube);
        final Message msg = MsgPicker.getMsg();
        CubeSegment toDelete = null;
        for (final CubeSegment seg : cube.getSegments()) {
            if (seg.getName().equals(segmentName)) {
                toDelete = seg;
                break;
            }
        }
        if (toDelete == null) {
            throw new BadRequestException(String.format(msg.getSEG_NOT_FOUND(), segmentName));
        }
        if (toDelete.getStatus() != SegmentStatusEnum.READY) {
            throw new BadRequestException(String.format(msg.getDELETE_NOT_READY_SEG(), segmentName));
        }
        if (!segmentName.equals(((CubeSegment)cube.getSegments().get(0)).getName()) && !segmentName.equals(((CubeSegment)cube.getSegments().get(cube.getSegments().size() - 1)).getName())) {
            CubeService.logger.warn(String.format(msg.getDELETE_SEGMENT_CAUSE_GAPS(), cube.getName(), segmentName));
        }
        return CubeManager.getInstance(this.getConfig()).updateCubeDropSegments(cube, new CubeSegment[] { toDelete });
    }
    
    protected void releaseAllJobs(final CubeInstance cube) {
        final List<CubingJob> cubingJobs = this.jobService.listJobsByRealizationName(cube.getName(), null);
        for (final CubingJob cubingJob : cubingJobs) {
            final ExecutableState status = cubingJob.getStatus();
            if (status != ExecutableState.SUCCEED && status != ExecutableState.DISCARDED) {
                this.getExecutableManager().discardJob(cubingJob.getId());
            }
        }
    }
    
    private void releaseAllSegments(final CubeInstance cube) throws IOException {
        this.releaseAllJobs(cube);
        final CubeUpdate update = new CubeUpdate(cube.latestCopyForWrite());
        update.setToRemoveSegs((CubeSegment[])cube.getSegments().toArray((Object[])new CubeSegment[cube.getSegments().size()]));
        update.setCuboids((Map)Maps.newHashMap());
        update.setCuboidsRecommend((Set)Sets.newHashSet());
        update.setUpdateTableSnapshotPath((Map)Maps.newHashMap());
        CubeManager.getInstance(this.getConfig()).updateCube(update);
    }
    
    public void updateOnNewSegmentReady(final String cubeName) {
        final KylinConfig kylinConfig = KylinConfig.getInstanceFromEnv();
        final String serverMode = kylinConfig.getServerMode();
        if ("job".equals(serverMode.toLowerCase()) || "all".equals(serverMode.toLowerCase())) {
            final CubeInstance cube = this.getCubeManager().getCube(cubeName);
            if (cube != null) {
                final CubeSegment seg = cube.getLatestBuiltSegment();
                if (seg != null && seg.getStatus() == SegmentStatusEnum.READY) {
                    this.keepCubeRetention(cubeName);
                    this.mergeCubeSegment(cubeName);
                }
            }
        }
    }
    
    private void keepCubeRetention(final String cubeName) {
        CubeService.logger.info("checking keepCubeRetention");
        CubeInstance cube = this.getCubeManager().getCube(cubeName);
        final CubeDesc desc = cube.getDescriptor();
        if (desc.getRetentionRange() <= 0L) {
            return;
        }
        synchronized (CubeService.class) {
            cube = this.getCubeManager().getCube(cubeName);
            final List<CubeSegment> readySegs = (List<CubeSegment>)cube.getSegments(SegmentStatusEnum.READY);
            if (readySegs.isEmpty()) {
                return;
            }
            final List<CubeSegment> toRemoveSegs = (List<CubeSegment>)Lists.newArrayList();
            final long tail = (long)readySegs.get(readySegs.size() - 1).getTSRange().end.v;
            final long head = tail - desc.getRetentionRange();
            for (final CubeSegment seg : readySegs) {
                if ((long)seg.getTSRange().end.v > 0L && (long)seg.getTSRange().end.v <= head) {
                    toRemoveSegs.add(seg);
                }
            }
            if (toRemoveSegs.size() > 0) {
                try {
                    this.getCubeManager().updateCubeDropSegments(cube, (Collection)toRemoveSegs);
                }
                catch (IOException e) {
                    CubeService.logger.error("Failed to remove old segment from cube " + cubeName, (Throwable)e);
                }
            }
        }
    }
    
    private void mergeCubeSegment(final String cubeName) {
        CubeInstance cube = this.getCubeManager().getCube(cubeName);
        if (!cube.needAutoMerge()) {
            return;
        }
        synchronized (CubeService.class) {
            try {
                cube = this.getCubeManager().getCube(cubeName);
                final SegmentRange offsets = cube.autoMergeCubeSegments();
                if (offsets != null) {
                    final CubeSegment newSeg = this.getCubeManager().mergeSegments(cube, (SegmentRange.TSRange)null, offsets, true);
                    CubeService.logger.debug("Will submit merge job on " + newSeg);
                    final DefaultChainedExecutable job = EngineFactory.createBatchMergeJob(newSeg, "SYSTEM");
                    this.getExecutableManager().addJob((AbstractExecutable)job);
                }
                else {
                    CubeService.logger.debug("Not ready for merge on cube " + cubeName);
                }
            }
            catch (IOException e) {
                CubeService.logger.error("Failed to auto merge cube " + cubeName, (Throwable)e);
            }
        }
    }
    
    public void validateCubeDesc(final CubeDesc desc, final boolean isDraft) {
        final Message msg = MsgPicker.getMsg();
        if (desc == null) {
            throw new BadRequestException(msg.getINVALID_CUBE_DEFINITION());
        }
        final String cubeName = desc.getName();
        if (StringUtils.isEmpty(cubeName)) {
            CubeService.logger.info("Cube name should not be empty.");
            throw new BadRequestException(msg.getEMPTY_CUBE_NAME());
        }
        if (!ValidateUtil.isAlphanumericUnderscore(cubeName)) {
            CubeService.logger.info("Invalid Cube name {}, only letters, numbers and underscore supported.", (Object)cubeName);
            throw new BadRequestException(String.format(msg.getINVALID_CUBE_NAME(), cubeName));
        }
        if (!isDraft) {
            final DataModelDesc modelDesc = this.modelService.getDataModelManager().getDataModelDesc(desc.getModelName());
            if (modelDesc == null) {
                throw new BadRequestException(String.format(msg.getMODEL_NOT_FOUND(), desc.getModelName()));
            }
            if (modelDesc.isDraft()) {
                CubeService.logger.info("Cannot use draft model.");
                throw new BadRequestException(String.format(msg.getUSE_DRAFT_MODEL(), desc.getModelName()));
            }
        }
    }
    
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#project, 'ADMINISTRATION') or hasPermission(#project, 'MANAGEMENT')")
    public CubeDesc saveCube(final CubeDesc desc, final ProjectInstance project) throws IOException {
        final Message msg = MsgPicker.getMsg();
        desc.setDraft(false);
        if (desc.getUuid() == null) {
            desc.updateRandomUuid();
        }
        try {
            this.createCubeAndDesc(project, desc);
        }
        catch (AccessDeniedException accessDeniedException) {
            throw new ForbiddenException(msg.getUPDATE_CUBE_NO_RIGHT());
        }
        if (desc.isBroken()) {
            throw new BadRequestException(desc.getErrorsAsString());
        }
        return desc;
    }
    
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#project, 'ADMINISTRATION') or hasPermission(#project, 'MANAGEMENT')")
    public void saveDraft(final ProjectInstance project, final CubeInstance cube, final String uuid, final RootPersistentEntity... entities) throws IOException {
        final Draft draft = new Draft();
        draft.setProject(project.getName());
        draft.setUuid(uuid);
        draft.setEntities(entities);
        this.getDraftManager().save(draft);
    }
    
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#project, 'ADMINISTRATION') or hasPermission(#project, 'MANAGEMENT')")
    public void saveDraft(final ProjectInstance project, final String uuid, final RootPersistentEntity... entities) throws IOException {
        final Draft draft = new Draft();
        draft.setProject(project.getName());
        draft.setUuid(uuid);
        draft.setEntities(entities);
        this.getDraftManager().save(draft);
    }
    
    public void deleteDraft(final Draft draft) throws IOException {
        this.aclEvaluate.checkProjectWritePermission(draft.getProject());
        this.getDraftManager().delete(draft.getUuid());
    }
    
    public CubeDesc updateCube(final CubeInstance cube, CubeDesc desc, final ProjectInstance project) throws IOException {
        this.aclEvaluate.checkProjectWritePermission(cube);
        final Message msg = MsgPicker.getMsg();
        final String projectName = project.getName();
        desc.setDraft(false);
        try {
            if (cube.getSegments().size() != 0 && !cube.getDescriptor().consistentWith(desc)) {
                throw new BadRequestException(String.format(msg.getINCONSISTENT_CUBE_DESC(), desc.getName()));
            }
            desc = this.updateCubeAndDesc(cube, desc, projectName, true);
        }
        catch (AccessDeniedException accessDeniedException) {
            throw new ForbiddenException(msg.getUPDATE_CUBE_NO_RIGHT());
        }
        if (desc.isBroken()) {
            throw new BadRequestException(desc.getErrorsAsString());
        }
        return desc;
    }
    
    public Draft getCubeDraft(final String cubeName, final String projectName) throws IOException {
        final Iterator i$ = this.listCubeDrafts(cubeName, null, projectName, true).iterator();
        if (i$.hasNext()) {
            final Draft d = i$.next();
            return d;
        }
        return null;
    }
    
    public List<Draft> listCubeDrafts(final String cubeName, final String modelName, final String project, final boolean exactMatch) throws IOException {
        if (null == project) {
            this.aclEvaluate.checkIsGlobalAdmin();
        }
        else {
            this.aclEvaluate.checkProjectReadPermission(project);
        }
        final List<Draft> result = new ArrayList<Draft>();
        for (final Draft d : this.getDraftManager().list(project)) {
            final RootPersistentEntity e = d.getEntity();
            if (e instanceof CubeDesc) {
                final CubeDesc c = (CubeDesc)e;
                if ((cubeName != null && (!exactMatch || !cubeName.toLowerCase().equals(c.getName().toLowerCase())) && (exactMatch || !c.getName().toLowerCase().contains(cubeName.toLowerCase()))) || (modelName != null && !modelName.toLowerCase().equals(c.getModelName().toLowerCase()))) {
                    continue;
                }
                if (c.getMeasures() != null) {
                    for (final MeasureDesc m : c.getMeasures()) {
                        final FunctionDesc f = m.getFunction();
                        if (f.getExpression().equals("PERCENTILE")) {
                            f.setExpression("PERCENTILE_APPROX");
                        }
                    }
                }
                result.add(d);
            }
        }
        return result;
    }
    
    public void afterPropertiesSet() throws Exception {
        Broadcaster.getInstance(this.getConfig()).registerStaticListener((Broadcaster.Listener)new HTableInfoSyncListener(), new String[] { "cube" });
    }
    
    public CubeInstanceResponse createCubeInstanceResponse(final CubeInstance cube) {
        return new CubeInstanceResponse(cube, this.projectService.getProjectOfCube(cube.getName()));
    }
    
    public CuboidTreeResponse getCuboidTreeResponse(final CuboidScheduler cuboidScheduler, final Map<Long, Long> rowCountMap, final Map<Long, Long> hitFrequencyMap, final Map<Long, Long> queryMatchMap, final Set<Long> currentCuboidSet) {
        final long baseCuboidId = cuboidScheduler.getBaseCuboidId();
        final int dimensionCount = Long.bitCount(baseCuboidId);
        long cubeQueryCount = 0L;
        if (hitFrequencyMap != null) {
            for (final long queryCount : hitFrequencyMap.values()) {
                cubeQueryCount += queryCount;
            }
        }
        final CuboidTreeResponse.NodeInfo root = this.generateNodeInfo(baseCuboidId, dimensionCount, cubeQueryCount, rowCountMap, hitFrequencyMap, queryMatchMap, currentCuboidSet);
        final List<CuboidTreeResponse.NodeInfo> nodeQueue = (List<CuboidTreeResponse.NodeInfo>)Lists.newLinkedList();
        nodeQueue.add(root);
        while (!nodeQueue.isEmpty()) {
            final CuboidTreeResponse.NodeInfo parentNode = nodeQueue.remove(0);
            for (final long childId : cuboidScheduler.getSpanningCuboid((long)parentNode.getId())) {
                final CuboidTreeResponse.NodeInfo childNode = this.generateNodeInfo(childId, dimensionCount, cubeQueryCount, rowCountMap, hitFrequencyMap, queryMatchMap, currentCuboidSet);
                parentNode.addChild(childNode);
                nodeQueue.add(childNode);
            }
        }
        final CuboidTreeResponse result = new CuboidTreeResponse();
        result.setRoot(root);
        return result;
    }
    
    private CuboidTreeResponse.NodeInfo generateNodeInfo(final long cuboidId, final int dimensionCount, final long cubeQueryCount, final Map<Long, Long> rowCountMap, final Map<Long, Long> hitFrequencyMap, final Map<Long, Long> queryMatchMap, final Set<Long> currentCuboidSet) {
        final Long queryCount = (hitFrequencyMap == null || hitFrequencyMap.get(cuboidId) == null) ? 0L : hitFrequencyMap.get(cuboidId);
        final float queryRate = (cubeQueryCount <= 0L) ? 0.0f : (queryCount / (float)cubeQueryCount);
        final long queryExactlyMatchCount = (queryMatchMap == null || queryMatchMap.get(cuboidId) == null) ? 0L : queryMatchMap.get(cuboidId);
        final boolean ifExist = currentCuboidSet.contains(cuboidId);
        final long rowCount = (rowCountMap == null) ? 0L : rowCountMap.get(cuboidId);
        final CuboidTreeResponse.NodeInfo node = new CuboidTreeResponse.NodeInfo();
        node.setId(cuboidId);
        node.setName(Cuboid.getDisplayName(cuboidId, dimensionCount));
        node.setQueryCount(queryCount);
        node.setQueryRate(queryRate);
        node.setExactlyMatchCount(queryExactlyMatchCount);
        node.setExisted(ifExist);
        node.setRowCount(rowCount);
        return node;
    }
    
    public Map<Long, Long> formatQueryCount(final List<List<String>> orgQueryCount) {
        final Map<Long, Long> formattedQueryCount = (Map<Long, Long>)Maps.newLinkedHashMap();
        for (final List<String> hit : orgQueryCount) {
            formattedQueryCount.put(Long.parseLong(hit.get(0)), (long)Double.parseDouble(hit.get(1)));
        }
        return formattedQueryCount;
    }
    
    public Map<Long, Map<Long, Long>> formatRollingUpCount(final List<List<String>> orgRollingUpCount) {
        final Map<Long, Map<Long, Long>> formattedRollingUpCount = (Map<Long, Map<Long, Long>>)Maps.newLinkedHashMap();
        for (final List<String> rollingUp : orgRollingUpCount) {
            final Map<Long, Long> childMap = (Map<Long, Long>)Maps.newLinkedHashMap();
            childMap.put(Long.parseLong(rollingUp.get(1)), (long)Double.parseDouble(rollingUp.get(2)));
            formattedRollingUpCount.put(Long.parseLong(rollingUp.get(0)), childMap);
        }
        return formattedRollingUpCount;
    }
    
    public Map<Long, Long> getRecommendCuboidStatistics(final CubeInstance cube, final Map<Long, Long> hitFrequencyMap, final Map<Long, Map<Long, Long>> rollingUpCountSourceMap) throws IOException {
        this.aclEvaluate.checkProjectAdminPermission(cube.getProject());
        return (Map<Long, Long>)CuboidRecommenderUtil.getRecommendCuboidList(cube, (Map)hitFrequencyMap, (Map)rollingUpCountSourceMap);
    }
    
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#cube, 'ADMINISTRATION') or hasPermission(#cube, 'MANAGEMENT')")
    public void migrateCube(final CubeInstance cube, final String projectName) {
        final KylinConfig config = cube.getConfig();
        if (!config.isAllowAutoMigrateCube()) {
            throw new InternalErrorException("One click migration is disabled, please contact your ADMIN");
        }
        for (final CubeSegment segment : cube.getSegments()) {
            if (segment.getStatus() != SegmentStatusEnum.READY) {
                throw new InternalErrorException("At least one segment is not in READY state. Please check whether there are Running or Error jobs.");
            }
        }
        final String srcCfgUri = config.getAutoMigrateCubeSrcConfig();
        final String dstCfgUri = config.getAutoMigrateCubeDestConfig();
        Preconditions.checkArgument(StringUtils.isNotEmpty(srcCfgUri), (Object)"Source configuration should not be empty.");
        Preconditions.checkArgument(StringUtils.isNotEmpty(dstCfgUri), (Object)"Destination configuration should not be empty.");
        final String stringBuilder = "%s/bin/kylin.sh org.apache.kylin.tool.CubeMigrationCLI %s %s %s %s %s %s true true";
        final String cmd = String.format(stringBuilder, KylinConfig.getKylinHome(), srcCfgUri, dstCfgUri, cube.getName(), projectName, config.isAutoMigrateCubeCopyAcl(), config.isAutoMigrateCubePurge());
        CubeService.logger.info("One click migration cmd: " + cmd);
        final CliCommandExecutor exec = new CliCommandExecutor();
        final PatternedLogger patternedLogger = new PatternedLogger(CubeService.logger);
        try {
            exec.execute(cmd, (org.apache.kylin.common.util.Logger)patternedLogger);
        }
        catch (IOException e) {
            throw new InternalErrorException("Failed to perform one-click migrating", e);
        }
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)CubeService.class);
    }
    
    private class HTableInfoSyncListener extends Broadcaster.Listener
    {
        public void onClearAll(final Broadcaster broadcaster) throws IOException {
            CubeService.this.htableInfoCache.invalidateAll();
        }
        
        public void onEntityChange(final Broadcaster broadcaster, final String entity, final Broadcaster.Event event, final String cacheKey) throws IOException {
            final String cubeName = cacheKey;
            final String keyPrefix = cubeName + "/";
            for (final String k : CubeService.this.htableInfoCache.asMap().keySet()) {
                if (k.startsWith(keyPrefix)) {
                    CubeService.this.htableInfoCache.invalidate((Object)k);
                }
            }
        }
    }
}
