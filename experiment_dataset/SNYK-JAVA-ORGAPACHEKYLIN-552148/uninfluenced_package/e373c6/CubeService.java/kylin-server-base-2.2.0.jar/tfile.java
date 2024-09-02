// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.kylin.rest.service;

import java.util.HashMap;
import org.slf4j.LoggerFactory;
import org.apache.kylin.metadata.cachesync.Broadcaster;
import org.apache.kylin.metadata.draft.Draft;
import org.apache.kylin.common.persistence.RootPersistentEntity;
import org.springframework.security.access.AccessDeniedException;
import org.apache.kylin.rest.exception.ForbiddenException;
import org.apache.kylin.job.execution.DefaultChainedExecutable;
import org.apache.kylin.job.execution.AbstractExecutable;
import org.apache.kylin.engine.EngineFactory;
import org.apache.kylin.metadata.model.SegmentRange;
import com.google.common.collect.Lists;
import org.apache.kylin.cube.CubeManager;
import org.apache.kylin.cube.CubeSegment;
import org.apache.kylin.metadata.model.DataModelDesc;
import com.google.common.base.Preconditions;
import org.apache.kylin.rest.response.CubeInstanceResponse;
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
import org.apache.kylin.common.persistence.AclEntity;
import org.apache.kylin.rest.security.AclPermission;
import org.apache.kylin.cube.cuboid.CuboidCLI;
import org.apache.kylin.rest.exception.BadRequestException;
import org.apache.kylin.rest.msg.MsgPicker;
import org.apache.kylin.cube.model.CubeDesc;
import java.io.IOException;
import org.apache.kylin.cube.CubeUpdate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.apache.kylin.metadata.project.ProjectInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import org.apache.kylin.cube.CubeInstance;
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
    public static final char[] VALID_CUBENAME;
    protected Cache<String, HBaseResponse> htableInfoCache;
    @Autowired
    @Qualifier("accessService")
    private AccessService accessService;
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
        if (StringUtils.isEmpty(cubeName) || !StringUtils.containsOnly(cubeName, CubeService.VALID_CUBENAME)) {
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
        final ProjectInstance project = (null != projectName) ? this.getProjectManager().getProject(projectName) : null;
        if (null == project) {
            cubeInstances = (List<CubeInstance>)this.getCubeManager().listAllCubes();
            this.aclEvaluate.checkIsGlobalAdmin();
        }
        else {
            cubeInstances = this.listAllCubes(projectName);
            this.aclEvaluate.hasProjectReadPermission(project);
        }
        List<CubeInstance> filterModelCubes = new ArrayList<CubeInstance>();
        if (modelName != null) {
            for (final CubeInstance cubeInstance : cubeInstances) {
                final boolean isCubeMatch = cubeInstance.getDescriptor().getModelName().toLowerCase().equals(modelName.toLowerCase());
                if (isCubeMatch) {
                    filterModelCubes.add(cubeInstance);
                }
            }
        }
        else {
            filterModelCubes = cubeInstances;
        }
        final List<CubeInstance> filterCubes = new ArrayList<CubeInstance>();
        for (final CubeInstance cubeInstance2 : filterModelCubes) {
            final boolean isCubeMatch2 = null == cubeName || (!exactMatch && cubeInstance2.getName().toLowerCase().contains(cubeName.toLowerCase())) || (exactMatch && cubeInstance2.getName().toLowerCase().equals(cubeName.toLowerCase()));
            if (isCubeMatch2) {
                filterCubes.add(cubeInstance2);
            }
        }
        return filterCubes;
    }
    
    public CubeInstance updateCubeCost(final CubeInstance cube, final int cost) throws IOException {
        this.aclEvaluate.hasProjectWritePermission(cube.getProjectInstance());
        if (cube.getCost() == cost) {
            return cube;
        }
        cube.setCost(cost);
        final String owner = SecurityContextHolder.getContext().getAuthentication().getName();
        cube.setOwner(owner);
        final CubeUpdate cubeBuilder = new CubeUpdate(cube).setOwner(owner).setCost(cost);
        return this.getCubeManager().updateCube(cubeBuilder);
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
        if (!createdDesc.getError().isEmpty()) {
            throw new BadRequestException(createdDesc.getErrorMsg());
        }
        final int cuboidCount = CuboidCLI.simulateCuboidGeneration(createdDesc, false);
        CubeService.logger.info("New cube " + cubeName + " has " + cuboidCount + " cuboids");
        final CubeInstance createdCube = this.getCubeManager().createCube(cubeName, project.getName(), createdDesc, owner);
        this.accessService.init((AclEntity)createdCube, AclPermission.ADMINISTRATION);
        this.accessService.inherit((AclEntity)createdCube, (AclEntity)project);
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
        this.aclEvaluate.hasProjectWritePermission(cube.getProjectInstance());
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
            final ProjectInstance newProject = projectManager.moveRealizationToProject(RealizationType.CUBE, cube.getName(), newProjectName, owner);
            this.accessService.inherit((AclEntity)cube, (AclEntity)newProject);
        }
        return updatedCubeDesc;
    }
    
    public void deleteCube(final CubeInstance cube) throws IOException {
        this.aclEvaluate.hasProjectWritePermission(cube.getProjectInstance());
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
        this.accessService.clean((AclEntity)cube, true);
    }
    
    public CubeInstance purgeCube(final CubeInstance cube) throws IOException {
        this.aclEvaluate.hasProjectOperationPermission(cube.getProjectInstance());
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
        this.aclEvaluate.hasProjectWritePermission(cube.getProjectInstance());
        final Message msg = MsgPicker.getMsg();
        final String cubeName = cube.getName();
        final RealizationStatusEnum ostatus = cube.getStatus();
        if (null != ostatus && !RealizationStatusEnum.READY.equals((Object)ostatus)) {
            throw new BadRequestException(String.format(msg.getDISABLE_NOT_READY_CUBE(), cubeName, ostatus));
        }
        cube.setStatus(RealizationStatusEnum.DISABLED);
        try {
            final CubeUpdate cubeBuilder = new CubeUpdate(cube);
            cubeBuilder.setStatus(RealizationStatusEnum.DISABLED);
            return this.getCubeManager().updateCube(cubeBuilder);
        }
        catch (IOException e) {
            cube.setStatus(ostatus);
            throw e;
        }
    }
    
    public CubeInstance enableCube(final CubeInstance cube) throws IOException {
        this.aclEvaluate.hasProjectWritePermission(cube.getProjectInstance());
        final Message msg = MsgPicker.getMsg();
        final String cubeName = cube.getName();
        final RealizationStatusEnum ostatus = cube.getStatus();
        if (!cube.getStatus().equals((Object)RealizationStatusEnum.DISABLED)) {
            throw new BadRequestException(String.format(msg.getENABLE_NOT_DISABLED_CUBE(), cubeName, ostatus));
        }
        if (cube.getSegments(SegmentStatusEnum.READY).size() == 0) {
            throw new BadRequestException(String.format(msg.getNO_READY_SEGMENT(), cubeName));
        }
        final List<CubingJob> cubingJobs = this.jobService.listJobsByRealizationName(cube.getName(), null, EnumSet.of(ExecutableState.READY, ExecutableState.RUNNING));
        if (!cubingJobs.isEmpty()) {
            throw new BadRequestException(msg.getENABLE_WITH_RUNNING_JOB());
        }
        if (!cube.getDescriptor().checkSignature()) {
            throw new BadRequestException(String.format(msg.getINCONSISTENT_CUBE_DESC_SIGNATURE(), cube.getDescriptor()));
        }
        try {
            final CubeUpdate cubeBuilder = new CubeUpdate(cube);
            cubeBuilder.setStatus(RealizationStatusEnum.READY);
            return this.getCubeManager().updateCube(cubeBuilder);
        }
        catch (IOException e) {
            cube.setStatus(ostatus);
            throw e;
        }
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
    
    public CubeInstanceResponse createCubeInstanceResponse(final CubeInstance cube) {
        Preconditions.checkState(!cube.getDescriptor().isDraft());
        final CubeInstanceResponse r = new CubeInstanceResponse(cube);
        final CubeDesc cubeDesc = cube.getDescriptor();
        final DataModelDesc modelDesc = cubeDesc.getModel();
        r.setModel(cubeDesc.getModelName());
        r.setLastModified(cubeDesc.getLastModified());
        r.setPartitionDateStart(cubeDesc.getPartitionDateStart());
        if (modelDesc != null) {
            r.setPartitionDateColumn(modelDesc.getPartitionDesc().getPartitionDateColumn());
            r.setIs_streaming(modelDesc.getRootFactTable().getTableDesc().getSourceType() == 1);
        }
        r.setProject(cube.getProject());
        return r;
    }
    
    public void updateCubeNotifyList(final CubeInstance cube, final List<String> notifyList) throws IOException {
        this.aclEvaluate.hasProjectOperationPermission(cube.getProjectInstance());
        final CubeDesc desc = cube.getDescriptor();
        desc.setNotifyList((List)notifyList);
        this.getCubeDescManager().updateCubeDesc(desc);
    }
    
    public CubeInstance rebuildLookupSnapshot(final CubeInstance cube, final String segmentName, final String lookupTable) throws IOException {
        this.aclEvaluate.hasProjectOperationPermission(cube.getProjectInstance());
        final CubeSegment seg = cube.getSegment(segmentName, SegmentStatusEnum.READY);
        this.getCubeManager().buildSnapshotTable(seg, lookupTable);
        return cube;
    }
    
    public CubeInstance deleteSegment(final CubeInstance cube, final String segmentName) throws IOException {
        this.aclEvaluate.hasProjectOperationPermission(cube.getProjectInstance());
        final Message msg = MsgPicker.getMsg();
        if (!segmentName.equals(((CubeSegment)cube.getSegments().get(0)).getName()) && !segmentName.equals(((CubeSegment)cube.getSegments().get(cube.getSegments().size() - 1)).getName())) {
            throw new BadRequestException(String.format(msg.getDELETE_NOT_FIRST_LAST_SEG(), segmentName));
        }
        CubeSegment toDelete = null;
        for (final CubeSegment seg : cube.getSegments()) {
            if (seg.getName().equals(segmentName)) {
                toDelete = seg;
            }
        }
        if (toDelete == null) {
            throw new BadRequestException(String.format(msg.getSEG_NOT_FOUND(), segmentName));
        }
        if (toDelete.getStatus() != SegmentStatusEnum.READY) {
            throw new BadRequestException(String.format(msg.getDELETE_NOT_READY_SEG(), segmentName));
        }
        final CubeUpdate update = new CubeUpdate(cube);
        update.setToRemoveSegs(new CubeSegment[] { toDelete });
        return CubeManager.getInstance(this.getConfig()).updateCube(update);
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
        final CubeUpdate update = new CubeUpdate(cube);
        update.setToRemoveSegs((CubeSegment[])cube.getSegments().toArray((Object[])new CubeSegment[cube.getSegments().size()]));
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
                final CubeUpdate cubeBuilder = new CubeUpdate(cube);
                cubeBuilder.setToRemoveSegs((CubeSegment[])toRemoveSegs.toArray(new CubeSegment[toRemoveSegs.size()]));
                try {
                    this.getCubeManager().updateCube(cubeBuilder);
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
        if (!StringUtils.containsOnly(cubeName, CubeService.VALID_CUBENAME)) {
            CubeService.logger.info("Invalid Cube name {}, only letters, numbers and underline supported.", (Object)cubeName);
            throw new BadRequestException(String.format(msg.getINVALID_CUBE_NAME(), cubeName));
        }
        if (!isDraft) {
            final DataModelDesc modelDesc = this.modelService.getMetadataManager().getDataModelDesc(desc.getModelName());
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
        if (!desc.getError().isEmpty()) {
            throw new BadRequestException(desc.getErrorMsg());
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
        this.aclEvaluate.hasProjectWritePermission(this.getProjectManager().getProject(draft.getProject()));
        this.getDraftManager().delete(draft.getUuid());
    }
    
    public CubeDesc updateCube(final CubeInstance cube, CubeDesc desc, final ProjectInstance project) throws IOException {
        this.aclEvaluate.hasProjectWritePermission(cube.getProjectInstance());
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
        if (!desc.getError().isEmpty()) {
            throw new BadRequestException(desc.getErrorMsg());
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
            this.aclEvaluate.hasProjectReadPermission(this.getProjectManager().getProject(project));
        }
        final List<Draft> result = new ArrayList<Draft>();
        for (final Draft d : this.getDraftManager().list(project)) {
            final RootPersistentEntity e = d.getEntity();
            if (e instanceof CubeDesc) {
                final CubeDesc c = (CubeDesc)e;
                if ((cubeName != null && (!exactMatch || !cubeName.toLowerCase().equals(c.getName().toLowerCase())) && (exactMatch || !c.getName().toLowerCase().contains(cubeName.toLowerCase()))) || (modelName != null && !modelName.toLowerCase().equals(c.getModelName().toLowerCase()))) {
                    continue;
                }
                result.add(d);
            }
        }
        return result;
    }
    
    public void afterPropertiesSet() throws Exception {
        Broadcaster.getInstance(this.getConfig()).registerStaticListener((Broadcaster.Listener)new HTableInfoSyncListener(), new String[] { "cube" });
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)CubeService.class);
        VALID_CUBENAME = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_".toCharArray();
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
