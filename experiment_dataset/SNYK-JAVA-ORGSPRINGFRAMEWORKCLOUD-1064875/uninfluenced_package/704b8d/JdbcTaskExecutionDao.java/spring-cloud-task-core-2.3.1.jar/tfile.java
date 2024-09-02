// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.cloud.task.repository.dao;

import java.util.HashSet;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.cloud.task.repository.database.PagingQueryProvider;
import java.util.Iterator;
import org.springframework.data.domain.PageImpl;
import org.springframework.util.CollectionUtils;
import org.springframework.data.domain.Sort;
import org.springframework.cloud.task.repository.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.dao.DataAccessException;
import java.sql.SQLException;
import java.util.TreeSet;
import java.sql.ResultSet;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Map;
import java.util.Collections;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.cloud.task.repository.TaskExecution;
import java.util.List;
import java.util.Date;
import org.springframework.util.Assert;
import java.util.Set;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.batch.item.database.Order;
import java.util.LinkedHashMap;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class JdbcTaskExecutionDao implements TaskExecutionDao
{
    public static final String SELECT_CLAUSE = "TASK_EXECUTION_ID, START_TIME, END_TIME, TASK_NAME, EXIT_CODE, EXIT_MESSAGE, ERROR_MESSAGE, LAST_UPDATED, EXTERNAL_EXECUTION_ID, PARENT_EXECUTION_ID ";
    public static final String FROM_CLAUSE = "%PREFIX%EXECUTION";
    public static final String RUNNING_TASK_WHERE_CLAUSE = "where TASK_NAME = :taskName AND END_TIME IS NULL ";
    public static final String TASK_NAME_WHERE_CLAUSE = "where TASK_NAME = :taskName ";
    private static final String SAVE_TASK_EXECUTION = "INSERT into %PREFIX%EXECUTION(TASK_EXECUTION_ID, EXIT_CODE, START_TIME, TASK_NAME, LAST_UPDATED, EXTERNAL_EXECUTION_ID, PARENT_EXECUTION_ID)values (:taskExecutionId, :exitCode, :startTime, :taskName, :lastUpdated, :externalExecutionId, :parentExecutionId)";
    private static final String CREATE_TASK_ARGUMENT = "INSERT into %PREFIX%EXECUTION_PARAMS(TASK_EXECUTION_ID, TASK_PARAM ) values (:taskExecutionId, :taskParam)";
    private static final String START_TASK_EXECUTION_PREFIX = "UPDATE %PREFIX%EXECUTION set START_TIME = :startTime, TASK_NAME = :taskName, LAST_UPDATED = :lastUpdated";
    private static final String START_TASK_EXECUTION_EXTERNAL_ID_SUFFIX = ", EXTERNAL_EXECUTION_ID = :externalExecutionId, PARENT_EXECUTION_ID = :parentExecutionId where TASK_EXECUTION_ID = :taskExecutionId";
    private static final String START_TASK_EXECUTION_SUFFIX = ", PARENT_EXECUTION_ID = :parentExecutionId where TASK_EXECUTION_ID = :taskExecutionId";
    private static final String CHECK_TASK_EXECUTION_EXISTS = "SELECT COUNT(*) FROM %PREFIX%EXECUTION WHERE TASK_EXECUTION_ID = :taskExecutionId";
    private static final String UPDATE_TASK_EXECUTION = "UPDATE %PREFIX%EXECUTION set END_TIME = :endTime, EXIT_CODE = :exitCode, EXIT_MESSAGE = :exitMessage, ERROR_MESSAGE = :errorMessage, LAST_UPDATED = :lastUpdated where TASK_EXECUTION_ID = :taskExecutionId";
    private static final String UPDATE_TASK_EXECUTION_EXTERNAL_EXECUTION_ID = "UPDATE %PREFIX%EXECUTION set EXTERNAL_EXECUTION_ID = :externalExecutionId where TASK_EXECUTION_ID = :taskExecutionId";
    private static final String GET_EXECUTION_BY_ID = "SELECT TASK_EXECUTION_ID, START_TIME, END_TIME, TASK_NAME, EXIT_CODE, EXIT_MESSAGE, ERROR_MESSAGE, LAST_UPDATED, EXTERNAL_EXECUTION_ID, PARENT_EXECUTION_ID from %PREFIX%EXECUTION where TASK_EXECUTION_ID = :taskExecutionId";
    private static final String FIND_ARGUMENT_FROM_ID = "SELECT TASK_EXECUTION_ID, TASK_PARAM from %PREFIX%EXECUTION_PARAMS where TASK_EXECUTION_ID = :taskExecutionId";
    private static final String TASK_EXECUTION_COUNT = "SELECT COUNT(*) FROM %PREFIX%EXECUTION ";
    private static final String TASK_EXECUTION_COUNT_BY_NAME = "SELECT COUNT(*) FROM %PREFIX%EXECUTION where TASK_NAME = :taskName";
    private static final String RUNNING_TASK_EXECUTION_COUNT_BY_NAME = "SELECT COUNT(*) FROM %PREFIX%EXECUTION where TASK_NAME = :taskName AND END_TIME IS NULL ";
    private static final String RUNNING_TASK_EXECUTION_COUNT = "SELECT COUNT(*) FROM %PREFIX%EXECUTION where END_TIME IS NULL ";
    private static final String LAST_TASK_EXECUTIONS_BY_TASK_NAMES = "select TE2.* from (select MAX(TE.TASK_EXECUTION_ID) as TASK_EXECUTION_ID, TE.TASK_NAME, TE.START_TIME from (select TASK_NAME, MAX(START_TIME) as START_TIME      FROM %PREFIX%EXECUTION where TASK_NAME in (:taskNames)      GROUP BY TASK_NAME) TE_MAX inner join %PREFIX%EXECUTION TE ON TE.TASK_NAME = TE_MAX.TASK_NAME AND TE.START_TIME = TE_MAX.START_TIME group by TE.TASK_NAME, TE.START_TIME) TE1 inner join %PREFIX%EXECUTION TE2 ON TE1.TASK_EXECUTION_ID = TE2.TASK_EXECUTION_ID order by TE2.START_TIME DESC, TE2.TASK_EXECUTION_ID DESC";
    private static final String FIND_TASK_NAMES = "SELECT distinct TASK_NAME from %PREFIX%EXECUTION order by TASK_NAME";
    private static final String FIND_TASK_EXECUTION_BY_JOB_EXECUTION_ID = "SELECT TASK_EXECUTION_ID FROM %PREFIX%TASK_BATCH WHERE JOB_EXECUTION_ID = :jobExecutionId";
    private static final String FIND_JOB_EXECUTION_BY_TASK_EXECUTION_ID = "SELECT JOB_EXECUTION_ID FROM %PREFIX%TASK_BATCH WHERE TASK_EXECUTION_ID = :taskExecutionId";
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private String tablePrefix;
    private DataSource dataSource;
    private LinkedHashMap<String, Order> orderMap;
    private DataFieldMaxValueIncrementer taskIncrementer;
    private static final Set<String> validSortColumns;
    
    public JdbcTaskExecutionDao(final DataSource dataSource, final String tablePrefix) {
        this(dataSource);
        Assert.hasText(tablePrefix, "tablePrefix must not be null nor empty");
        this.tablePrefix = tablePrefix;
    }
    
    public JdbcTaskExecutionDao(final DataSource dataSource) {
        this.tablePrefix = "TASK_";
        Assert.notNull((Object)dataSource, "The dataSource must not be null.");
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        this.dataSource = dataSource;
        (this.orderMap = new LinkedHashMap<String, Order>()).put("START_TIME", Order.DESCENDING);
        this.orderMap.put("TASK_EXECUTION_ID", Order.DESCENDING);
    }
    
    @Override
    public TaskExecution createTaskExecution(final String taskName, final Date startTime, final List<String> arguments, final String externalExecutionId) {
        return this.createTaskExecution(taskName, startTime, arguments, externalExecutionId, null);
    }
    
    @Override
    public TaskExecution createTaskExecution(final String taskName, final Date startTime, final List<String> arguments, final String externalExecutionId, final Long parentExecutionId) {
        final long nextExecutionId = this.getNextExecutionId();
        final TaskExecution taskExecution = new TaskExecution(nextExecutionId, null, taskName, startTime, null, null, arguments, null, externalExecutionId);
        final MapSqlParameterSource queryParameters = new MapSqlParameterSource().addValue("taskExecutionId", (Object)nextExecutionId, -5).addValue("exitCode", (Object)null, 4).addValue("startTime", (Object)startTime, 93).addValue("taskName", (Object)taskName, 12).addValue("lastUpdated", (Object)new Date(), 93).addValue("externalExecutionId", (Object)externalExecutionId, 12).addValue("parentExecutionId", (Object)parentExecutionId, -5);
        this.jdbcTemplate.update(this.getQuery("INSERT into %PREFIX%EXECUTION(TASK_EXECUTION_ID, EXIT_CODE, START_TIME, TASK_NAME, LAST_UPDATED, EXTERNAL_EXECUTION_ID, PARENT_EXECUTION_ID)values (:taskExecutionId, :exitCode, :startTime, :taskName, :lastUpdated, :externalExecutionId, :parentExecutionId)"), (SqlParameterSource)queryParameters);
        this.insertTaskArguments(nextExecutionId, arguments);
        return taskExecution;
    }
    
    @Override
    public TaskExecution startTaskExecution(final long executionId, final String taskName, final Date startTime, final List<String> arguments, final String externalExecutionId) {
        return this.startTaskExecution(executionId, taskName, startTime, arguments, externalExecutionId, null);
    }
    
    @Override
    public TaskExecution startTaskExecution(final long executionId, final String taskName, final Date startTime, final List<String> arguments, final String externalExecutionId, final Long parentExecutionId) {
        final TaskExecution taskExecution = new TaskExecution(executionId, null, taskName, startTime, null, null, arguments, null, externalExecutionId, parentExecutionId);
        final MapSqlParameterSource queryParameters = new MapSqlParameterSource().addValue("startTime", (Object)startTime, 93).addValue("exitCode", (Object)null, 4).addValue("taskName", (Object)taskName, 12).addValue("lastUpdated", (Object)new Date(), 93).addValue("parentExecutionId", (Object)parentExecutionId, -5).addValue("taskExecutionId", (Object)executionId, -5);
        String updateString = "UPDATE %PREFIX%EXECUTION set START_TIME = :startTime, TASK_NAME = :taskName, LAST_UPDATED = :lastUpdated";
        if (externalExecutionId == null) {
            updateString += ", PARENT_EXECUTION_ID = :parentExecutionId where TASK_EXECUTION_ID = :taskExecutionId";
        }
        else {
            updateString += ", EXTERNAL_EXECUTION_ID = :externalExecutionId, PARENT_EXECUTION_ID = :parentExecutionId where TASK_EXECUTION_ID = :taskExecutionId";
            queryParameters.addValue("externalExecutionId", (Object)externalExecutionId, 12);
        }
        this.jdbcTemplate.update(this.getQuery(updateString), (SqlParameterSource)queryParameters);
        this.insertTaskArguments(executionId, arguments);
        return taskExecution;
    }
    
    @Override
    public void completeTaskExecution(final long taskExecutionId, final Integer exitCode, final Date endTime, final String exitMessage, final String errorMessage) {
        final MapSqlParameterSource queryParameters = new MapSqlParameterSource().addValue("taskExecutionId", (Object)taskExecutionId, -5);
        if ((int)this.jdbcTemplate.queryForObject(this.getQuery("SELECT COUNT(*) FROM %PREFIX%EXECUTION WHERE TASK_EXECUTION_ID = :taskExecutionId"), (SqlParameterSource)queryParameters, (Class)Integer.class) != 1) {
            throw new IllegalStateException("Invalid TaskExecution, ID " + taskExecutionId + " not found.");
        }
        final MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("endTime", (Object)endTime, 93).addValue("exitCode", (Object)exitCode, 4).addValue("exitMessage", (Object)exitMessage, 12).addValue("errorMessage", (Object)errorMessage, 12).addValue("lastUpdated", (Object)new Date(), 93).addValue("taskExecutionId", (Object)taskExecutionId, -5);
        this.jdbcTemplate.update(this.getQuery("UPDATE %PREFIX%EXECUTION set END_TIME = :endTime, EXIT_CODE = :exitCode, EXIT_MESSAGE = :exitMessage, ERROR_MESSAGE = :errorMessage, LAST_UPDATED = :lastUpdated where TASK_EXECUTION_ID = :taskExecutionId"), (SqlParameterSource)parameters);
    }
    
    @Override
    public void completeTaskExecution(final long taskExecutionId, final Integer exitCode, final Date endTime, final String exitMessage) {
        this.completeTaskExecution(taskExecutionId, exitCode, endTime, exitMessage, null);
    }
    
    @Override
    public TaskExecution getTaskExecution(final long executionId) {
        final MapSqlParameterSource queryParameters = new MapSqlParameterSource().addValue("taskExecutionId", (Object)executionId, -5);
        try {
            final TaskExecution taskExecution = (TaskExecution)this.jdbcTemplate.queryForObject(this.getQuery("SELECT TASK_EXECUTION_ID, START_TIME, END_TIME, TASK_NAME, EXIT_CODE, EXIT_MESSAGE, ERROR_MESSAGE, LAST_UPDATED, EXTERNAL_EXECUTION_ID, PARENT_EXECUTION_ID from %PREFIX%EXECUTION where TASK_EXECUTION_ID = :taskExecutionId"), (SqlParameterSource)queryParameters, (RowMapper)new TaskExecutionRowMapper());
            taskExecution.setArguments(this.getTaskArguments(executionId));
            return taskExecution;
        }
        catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
    
    @Override
    public long getTaskExecutionCountByTaskName(final String taskName) {
        final MapSqlParameterSource queryParameters = new MapSqlParameterSource().addValue("taskName", (Object)taskName, 12);
        try {
            return (long)this.jdbcTemplate.queryForObject(this.getQuery("SELECT COUNT(*) FROM %PREFIX%EXECUTION where TASK_NAME = :taskName"), (SqlParameterSource)queryParameters, (Class)Long.class);
        }
        catch (EmptyResultDataAccessException e) {
            return 0L;
        }
    }
    
    @Override
    public long getRunningTaskExecutionCountByTaskName(final String taskName) {
        final MapSqlParameterSource queryParameters = new MapSqlParameterSource().addValue("taskName", (Object)taskName, 12);
        try {
            return (long)this.jdbcTemplate.queryForObject(this.getQuery("SELECT COUNT(*) FROM %PREFIX%EXECUTION where TASK_NAME = :taskName AND END_TIME IS NULL "), (SqlParameterSource)queryParameters, (Class)Long.class);
        }
        catch (EmptyResultDataAccessException e) {
            return 0L;
        }
    }
    
    @Override
    public long getRunningTaskExecutionCount() {
        try {
            final MapSqlParameterSource queryParameters = new MapSqlParameterSource();
            return (long)this.jdbcTemplate.queryForObject(this.getQuery("SELECT COUNT(*) FROM %PREFIX%EXECUTION where END_TIME IS NULL "), (SqlParameterSource)queryParameters, (Class)Long.class);
        }
        catch (EmptyResultDataAccessException e) {
            return 0L;
        }
    }
    
    @Override
    public List<TaskExecution> getLatestTaskExecutionsByTaskNames(final String... taskNames) {
        Assert.notEmpty((Object[])taskNames, "At least 1 task name must be provided.");
        final List<String> taskNamesAsList = new ArrayList<String>();
        for (final String taskName : taskNames) {
            if (StringUtils.hasText(taskName)) {
                taskNamesAsList.add(taskName);
            }
        }
        Assert.isTrue(taskNamesAsList.size() == taskNames.length, String.format("Task names must not contain any empty elements but %s of %s were empty or null.", taskNames.length - taskNamesAsList.size(), taskNames.length));
        try {
            final Map<String, List<String>> paramMap = Collections.singletonMap("taskNames", taskNamesAsList);
            return (List<TaskExecution>)this.jdbcTemplate.query(this.getQuery("select TE2.* from (select MAX(TE.TASK_EXECUTION_ID) as TASK_EXECUTION_ID, TE.TASK_NAME, TE.START_TIME from (select TASK_NAME, MAX(START_TIME) as START_TIME      FROM %PREFIX%EXECUTION where TASK_NAME in (:taskNames)      GROUP BY TASK_NAME) TE_MAX inner join %PREFIX%EXECUTION TE ON TE.TASK_NAME = TE_MAX.TASK_NAME AND TE.START_TIME = TE_MAX.START_TIME group by TE.TASK_NAME, TE.START_TIME) TE1 inner join %PREFIX%EXECUTION TE2 ON TE1.TASK_EXECUTION_ID = TE2.TASK_EXECUTION_ID order by TE2.START_TIME DESC, TE2.TASK_EXECUTION_ID DESC"), (Map)paramMap, (RowMapper)new TaskExecutionRowMapper());
        }
        catch (EmptyResultDataAccessException e) {
            return Collections.emptyList();
        }
    }
    
    @Override
    public TaskExecution getLatestTaskExecutionForTaskName(final String taskName) {
        Assert.hasText(taskName, "The task name must not be empty.");
        final List<TaskExecution> taskExecutions = this.getLatestTaskExecutionsByTaskNames(taskName);
        if (taskExecutions.isEmpty()) {
            return null;
        }
        if (taskExecutions.size() == 1) {
            return taskExecutions.get(0);
        }
        throw new IllegalStateException("Only expected a single TaskExecution but received " + taskExecutions.size());
    }
    
    @Override
    public long getTaskExecutionCount() {
        try {
            return (long)this.jdbcTemplate.queryForObject(this.getQuery("SELECT COUNT(*) FROM %PREFIX%EXECUTION "), (SqlParameterSource)new MapSqlParameterSource(), (Class)Long.class);
        }
        catch (EmptyResultDataAccessException e) {
            return 0L;
        }
    }
    
    @Override
    public Page<TaskExecution> findRunningTaskExecutions(final String taskName, final Pageable pageable) {
        return this.queryForPageableResults(pageable, "TASK_EXECUTION_ID, START_TIME, END_TIME, TASK_NAME, EXIT_CODE, EXIT_MESSAGE, ERROR_MESSAGE, LAST_UPDATED, EXTERNAL_EXECUTION_ID, PARENT_EXECUTION_ID ", "%PREFIX%EXECUTION", "where TASK_NAME = :taskName AND END_TIME IS NULL ", new MapSqlParameterSource("taskName", (Object)taskName), this.getRunningTaskExecutionCountByTaskName(taskName));
    }
    
    @Override
    public Page<TaskExecution> findTaskExecutionsByName(final String taskName, final Pageable pageable) {
        return this.queryForPageableResults(pageable, "TASK_EXECUTION_ID, START_TIME, END_TIME, TASK_NAME, EXIT_CODE, EXIT_MESSAGE, ERROR_MESSAGE, LAST_UPDATED, EXTERNAL_EXECUTION_ID, PARENT_EXECUTION_ID ", "%PREFIX%EXECUTION", "where TASK_NAME = :taskName ", new MapSqlParameterSource("taskName", (Object)taskName), this.getTaskExecutionCountByTaskName(taskName));
    }
    
    @Override
    public List<String> getTaskNames() {
        return (List<String>)this.jdbcTemplate.queryForList(this.getQuery("SELECT distinct TASK_NAME from %PREFIX%EXECUTION order by TASK_NAME"), (SqlParameterSource)new MapSqlParameterSource(), (Class)String.class);
    }
    
    @Override
    public Page<TaskExecution> findAll(final Pageable pageable) {
        return this.queryForPageableResults(pageable, "TASK_EXECUTION_ID, START_TIME, END_TIME, TASK_NAME, EXIT_CODE, EXIT_MESSAGE, ERROR_MESSAGE, LAST_UPDATED, EXTERNAL_EXECUTION_ID, PARENT_EXECUTION_ID ", "%PREFIX%EXECUTION", null, new MapSqlParameterSource(), this.getTaskExecutionCount());
    }
    
    public void setTaskIncrementer(final DataFieldMaxValueIncrementer taskIncrementer) {
        this.taskIncrementer = taskIncrementer;
    }
    
    @Override
    public long getNextExecutionId() {
        return this.taskIncrementer.nextLongValue();
    }
    
    @Override
    public Long getTaskExecutionIdByJobExecutionId(final long jobExecutionId) {
        final MapSqlParameterSource queryParameters = new MapSqlParameterSource().addValue("jobExecutionId", (Object)jobExecutionId, -5);
        try {
            return (Long)this.jdbcTemplate.queryForObject(this.getQuery("SELECT TASK_EXECUTION_ID FROM %PREFIX%TASK_BATCH WHERE JOB_EXECUTION_ID = :jobExecutionId"), (SqlParameterSource)queryParameters, (Class)Long.class);
        }
        catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
    
    @Override
    public Set<Long> getJobExecutionIdsByTaskExecutionId(final long taskExecutionId) {
        final MapSqlParameterSource queryParameters = new MapSqlParameterSource().addValue("taskExecutionId", (Object)taskExecutionId, -5);
        try {
            return (Set<Long>)this.jdbcTemplate.query(this.getQuery("SELECT JOB_EXECUTION_ID FROM %PREFIX%TASK_BATCH WHERE TASK_EXECUTION_ID = :taskExecutionId"), (SqlParameterSource)queryParameters, (ResultSetExtractor)new ResultSetExtractor<Set<Long>>() {
                public Set<Long> extractData(final ResultSet resultSet) throws SQLException, DataAccessException {
                    final Set<Long> jobExecutionIds = new TreeSet<Long>();
                    while (resultSet.next()) {
                        jobExecutionIds.add(resultSet.getLong("JOB_EXECUTION_ID"));
                    }
                    return jobExecutionIds;
                }
            });
        }
        catch (DataAccessException e) {
            return Collections.emptySet();
        }
    }
    
    @Override
    public void updateExternalExecutionId(final long taskExecutionId, final String externalExecutionId) {
        final MapSqlParameterSource queryParameters = new MapSqlParameterSource().addValue("externalExecutionId", (Object)externalExecutionId, 12).addValue("taskExecutionId", (Object)taskExecutionId, -5);
        if (this.jdbcTemplate.update(this.getQuery("UPDATE %PREFIX%EXECUTION set EXTERNAL_EXECUTION_ID = :externalExecutionId where TASK_EXECUTION_ID = :taskExecutionId"), (SqlParameterSource)queryParameters) != 1) {
            throw new IllegalStateException("Invalid TaskExecution, ID " + taskExecutionId + " not found.");
        }
    }
    
    private Page<TaskExecution> queryForPageableResults(final Pageable pageable, final String selectClause, final String fromClause, final String whereClause, final MapSqlParameterSource queryParameters, final long totalCount) {
        final SqlPagingQueryProviderFactoryBean factoryBean = new SqlPagingQueryProviderFactoryBean();
        factoryBean.setSelectClause(selectClause);
        factoryBean.setFromClause(fromClause);
        if (StringUtils.hasText(whereClause)) {
            factoryBean.setWhereClause(whereClause);
        }
        final Sort sort = pageable.getSort();
        final LinkedHashMap<String, Order> sortOrderMap = new LinkedHashMap<String, Order>();
        if (sort != null) {
            for (final Sort.Order sortOrder : sort) {
                if (!JdbcTaskExecutionDao.validSortColumns.contains(sortOrder.getProperty().toUpperCase())) {
                    throw new IllegalArgumentException(String.format("Invalid sort option selected: %s", sortOrder.getProperty()));
                }
                sortOrderMap.put(sortOrder.getProperty(), sortOrder.isAscending() ? Order.ASCENDING : Order.DESCENDING);
            }
        }
        if (!CollectionUtils.isEmpty((Map)sortOrderMap)) {
            factoryBean.setSortKeys(sortOrderMap);
        }
        else {
            factoryBean.setSortKeys(this.orderMap);
        }
        factoryBean.setDataSource(this.dataSource);
        PagingQueryProvider pagingQueryProvider;
        try {
            pagingQueryProvider = factoryBean.getObject();
            pagingQueryProvider.init(this.dataSource);
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
        final String query = pagingQueryProvider.getPageQuery(pageable);
        final List<TaskExecution> resultList = (List<TaskExecution>)this.jdbcTemplate.query(this.getQuery(query), (SqlParameterSource)queryParameters, (RowMapper)new TaskExecutionRowMapper());
        return (Page<TaskExecution>)new PageImpl((List)resultList, pageable, totalCount);
    }
    
    private String getQuery(final String base) {
        return StringUtils.replace(base, "%PREFIX%", this.tablePrefix);
    }
    
    private void insertTaskArguments(final long executionId, final List<String> taskArguments) {
        for (final String args : taskArguments) {
            this.insertArgument(executionId, args);
        }
    }
    
    private void insertArgument(final long taskExecutionId, final String taskParam) {
        final MapSqlParameterSource queryParameters = new MapSqlParameterSource().addValue("taskExecutionId", (Object)taskExecutionId, -5).addValue("taskParam", (Object)taskParam, 12);
        this.jdbcTemplate.update(this.getQuery("INSERT into %PREFIX%EXECUTION_PARAMS(TASK_EXECUTION_ID, TASK_PARAM ) values (:taskExecutionId, :taskParam)"), (SqlParameterSource)queryParameters);
    }
    
    private List<String> getTaskArguments(final long taskExecutionId) {
        final List<String> params = new ArrayList<String>();
        final RowCallbackHandler handler = (RowCallbackHandler)new RowCallbackHandler() {
            public void processRow(final ResultSet rs) throws SQLException {
                params.add(rs.getString(2));
            }
        };
        this.jdbcTemplate.query(this.getQuery("SELECT TASK_EXECUTION_ID, TASK_PARAM from %PREFIX%EXECUTION_PARAMS where TASK_EXECUTION_ID = :taskExecutionId"), (SqlParameterSource)new MapSqlParameterSource("taskExecutionId", (Object)taskExecutionId), handler);
        return params;
    }
    
    static {
        (validSortColumns = new HashSet<String>(10)).add("TASK_EXECUTION_ID");
        JdbcTaskExecutionDao.validSortColumns.add("START_TIME");
        JdbcTaskExecutionDao.validSortColumns.add("END_TIME");
        JdbcTaskExecutionDao.validSortColumns.add("TASK_NAME");
        JdbcTaskExecutionDao.validSortColumns.add("EXIT_CODE");
        JdbcTaskExecutionDao.validSortColumns.add("EXIT_MESSAGE");
        JdbcTaskExecutionDao.validSortColumns.add("ERROR_MESSAGE");
        JdbcTaskExecutionDao.validSortColumns.add("LAST_UPDATED");
        JdbcTaskExecutionDao.validSortColumns.add("EXTERNAL_EXECUTION_ID");
        JdbcTaskExecutionDao.validSortColumns.add("PARENT_EXECUTION_ID");
    }
    
    private final class TaskExecutionRowMapper implements RowMapper<TaskExecution>
    {
        public TaskExecution mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final long id = rs.getLong("TASK_EXECUTION_ID");
            Long parentExecutionId = rs.getLong("PARENT_EXECUTION_ID");
            if (rs.wasNull()) {
                parentExecutionId = null;
            }
            return new TaskExecution(id, this.getNullableExitCode(rs), rs.getString("TASK_NAME"), rs.getTimestamp("START_TIME"), rs.getTimestamp("END_TIME"), rs.getString("EXIT_MESSAGE"), JdbcTaskExecutionDao.this.getTaskArguments(id), rs.getString("ERROR_MESSAGE"), rs.getString("EXTERNAL_EXECUTION_ID"), parentExecutionId);
        }
        
        private Integer getNullableExitCode(final ResultSet rs) throws SQLException {
            final int exitCode = rs.getInt("EXIT_CODE");
            return rs.wasNull() ? null : Integer.valueOf(exitCode);
        }
    }
}
