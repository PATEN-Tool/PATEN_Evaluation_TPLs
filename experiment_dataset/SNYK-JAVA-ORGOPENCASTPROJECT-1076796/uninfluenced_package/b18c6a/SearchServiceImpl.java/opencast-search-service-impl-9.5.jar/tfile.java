// 
// Decompiled by Procyon v0.5.36
// 

package org.opencastproject.search.impl;

import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import org.osgi.service.cm.ConfigurationException;
import org.opencastproject.util.LoadUtil;
import java.util.Dictionary;
import java.util.Iterator;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.util.SecurityUtil;
import org.osgi.framework.ServiceException;
import org.opencastproject.search.api.SearchQuery;
import java.util.Arrays;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.search.impl.persistence.SearchServiceDatabaseException;
import java.util.Date;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import java.util.Collections;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchResult;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import java.io.FileOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrServerException;
import java.io.IOException;
import java.io.File;
import org.opencastproject.solr.SolrServerFactory;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import com.google.common.cache.CacheLoader;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.CacheBuilder;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.opencastproject.security.api.User;
import org.opencastproject.util.data.Tuple;
import com.google.common.cache.LoadingCache;
import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.search.impl.persistence.SearchServiceDatabase;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.workspace.api.Workspace;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.metadata.api.StaticMetadataService;
import java.util.List;
import org.opencastproject.search.impl.solr.SolrIndexManager;
import org.opencastproject.search.impl.solr.SolrRequester;
import org.apache.solr.client.solrj.SolrServer;
import org.slf4j.Logger;
import org.opencastproject.security.api.StaticFileAuthorization;
import org.osgi.service.cm.ManagedService;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.job.api.AbstractJobProducer;

public final class SearchServiceImpl extends AbstractJobProducer implements SearchService, ManagedService, StaticFileAuthorization
{
    private static final Logger logger;
    public static final String CONFIG_SOLR_URL = "org.opencastproject.search.solr.url";
    public static final String CONFIG_SOLR_ROOT = "org.opencastproject.search.solr.dir";
    public static final String JOB_TYPE = "org.opencastproject.search";
    public static final float DEFAULT_ADD_JOB_LOAD = 0.1f;
    public static final float DEFAULT_DELETE_JOB_LOAD = 0.1f;
    public static final String ADD_JOB_LOAD_KEY = "job.load.add";
    public static final String DELETE_JOB_LOAD_KEY = "job.load.delete";
    private float addJobLoad;
    private float deleteJobLoad;
    private int retriesToPopulateIndex;
    private SolrServer solrServer;
    private SolrRequester solrRequester;
    private SolrIndexManager indexManager;
    private List<StaticMetadataService> mdServices;
    private Mpeg7CatalogService mpeg7CatalogService;
    private SeriesService seriesService;
    private Workspace workspace;
    private SecurityService securityService;
    private AuthorizationService authorizationService;
    private ServiceRegistry serviceRegistry;
    private SearchServiceDatabase persistence;
    protected UserDirectoryService userDirectoryService;
    protected OrganizationDirectoryService organizationDirectory;
    protected MediaPackageSerializer serializer;
    private LoadingCache<Tuple<User, String>, Boolean> cache;
    private static final Pattern staticFilePattern;
    
    public SearchServiceImpl() {
        super("org.opencastproject.search");
        this.addJobLoad = 0.1f;
        this.deleteJobLoad = 0.1f;
        this.retriesToPopulateIndex = 0;
        this.mdServices = new ArrayList<StaticMetadataService>();
        this.userDirectoryService = null;
        this.organizationDirectory = null;
        this.serializer = null;
        this.cache = null;
        this.cache = (LoadingCache<Tuple<User, String>, Boolean>)CacheBuilder.newBuilder().maximumSize(2048L).expireAfterWrite(1L, TimeUnit.MINUTES).build((CacheLoader)new CacheLoader<Tuple<User, String>, Boolean>() {
            public Boolean load(final Tuple<User, String> key) {
                return SearchServiceImpl.this.loadUrlAccess((String)key.getB());
            }
        });
    }
    
    public SolrIndexManager getSolrIndexManager() {
        return this.indexManager;
    }
    
    public void activate(final ComponentContext cc) throws IllegalStateException {
        super.activate(cc);
        final String solrServerUrlConfig = StringUtils.trimToNull(cc.getBundleContext().getProperty("org.opencastproject.search.solr.url"));
        SearchServiceImpl.logger.info("Setting up solr server");
        this.solrServer = new Object() {
            SolrServer create() {
                if (solrServerUrlConfig != null) {
                    try {
                        SearchServiceImpl.logger.info("Setting up solr server at {}", (Object)solrServerUrlConfig);
                        final URL solrServerUrl = new URL(solrServerUrlConfig);
                        return SearchServiceImpl.setupSolr(solrServerUrl);
                    }
                    catch (MalformedURLException e) {
                        throw this.connectError(solrServerUrlConfig, e);
                    }
                }
                final String solrRoot = SolrServerFactory.getEmbeddedDir(cc, "org.opencastproject.search.solr.dir", "search");
                try {
                    SearchServiceImpl.logger.debug("Setting up solr server at {}", (Object)solrRoot);
                    return SearchServiceImpl.setupSolr(new File(solrRoot));
                }
                catch (IOException e2) {
                    throw this.connectError(solrServerUrlConfig, e2);
                }
                catch (SolrServerException e3) {
                    throw this.connectError(solrServerUrlConfig, (Exception)e3);
                }
            }
            
            IllegalStateException connectError(final String target, final Exception e) {
                SearchServiceImpl.logger.error("Unable to connect to solr at {}: {}", (Object)target, (Object)e.getMessage());
                return new IllegalStateException("Unable to connect to solr at " + target, e);
            }
        }.create();
        this.solrRequester = new SolrRequester(this.solrServer, this.securityService, this.serializer);
        this.indexManager = new SolrIndexManager(this.solrServer, this.workspace, this.mdServices, this.seriesService, this.mpeg7CatalogService, this.securityService);
        final String systemUserName = cc.getBundleContext().getProperty("org.opencastproject.security.digest.user");
        this.populateIndex(systemUserName);
    }
    
    public void deactivate() {
        SolrServerFactory.shutdown(this.solrServer);
    }
    
    static SolrServer setupSolr(final File solrRoot) throws IOException, SolrServerException {
        SearchServiceImpl.logger.info("Setting up solr search index at {}", (Object)solrRoot);
        final File solrConfigDir = new File(solrRoot, "conf");
        if (solrConfigDir.exists()) {
            SearchServiceImpl.logger.info("solr search index found at {}", (Object)solrConfigDir);
        }
        else {
            SearchServiceImpl.logger.info("solr config directory doesn't exist.  Creating {}", (Object)solrConfigDir);
            FileUtils.forceMkdir(solrConfigDir);
        }
        copyClasspathResourceToFile("/solr/conf/protwords.txt", solrConfigDir);
        copyClasspathResourceToFile("/solr/conf/schema.xml", solrConfigDir);
        copyClasspathResourceToFile("/solr/conf/scripts.conf", solrConfigDir);
        copyClasspathResourceToFile("/solr/conf/solrconfig.xml", solrConfigDir);
        copyClasspathResourceToFile("/solr/conf/stopwords.txt", solrConfigDir);
        copyClasspathResourceToFile("/solr/conf/synonyms.txt", solrConfigDir);
        final File solrDataDir = new File(solrRoot, "data");
        if (!solrDataDir.exists()) {
            FileUtils.forceMkdir(solrDataDir);
        }
        final File solrIndexDir = new File(solrDataDir, "index");
        if (solrIndexDir.isDirectory() && solrIndexDir.list().length == 0) {
            FileUtils.deleteDirectory(solrIndexDir);
        }
        return SolrServerFactory.newEmbeddedInstance(solrRoot, solrDataDir);
    }
    
    static SolrServer setupSolr(final URL url) {
        SearchServiceImpl.logger.info("Connecting to solr search index at {}", (Object)url);
        return SolrServerFactory.newRemoteInstance(url);
    }
    
    static void copyClasspathResourceToFile(final String classpath, final File dir) {
        InputStream in = null;
        FileOutputStream fos = null;
        try {
            in = SearchServiceImpl.class.getResourceAsStream(classpath);
            final File file = new File(dir, FilenameUtils.getName(classpath));
            SearchServiceImpl.logger.debug("copying " + classpath + " to " + file);
            fos = new FileOutputStream(file);
            IOUtils.copy(in, (OutputStream)fos);
        }
        catch (IOException e) {
            throw new RuntimeException("Error copying solr classpath resource to the filesystem", e);
        }
        finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly((OutputStream)fos);
        }
    }
    
    public SearchResult getByQuery(final String query, final int limit, final int offset) throws SearchException {
        try {
            SearchServiceImpl.logger.debug("Searching index using custom query '" + query + "'");
            return this.solrRequester.getByQuery(query, limit, offset);
        }
        catch (SolrServerException e) {
            throw new SearchException((Throwable)e);
        }
    }
    
    public Job add(final MediaPackage mediaPackage) throws SearchException, MediaPackageException, IllegalArgumentException, UnauthorizedException, ServiceRegistryException {
        try {
            return this.serviceRegistry.createJob("org.opencastproject.search", Operation.Add.toString(), (List)Collections.singletonList(MediaPackageParser.getAsXml(mediaPackage)), Float.valueOf(this.addJobLoad));
        }
        catch (ServiceRegistryException e) {
            throw new SearchException((Throwable)e);
        }
    }
    
    public void addSynchronously(final MediaPackage mediaPackage) throws SearchException, IllegalArgumentException, UnauthorizedException, NotFoundException, SearchServiceDatabaseException {
        if (mediaPackage == null) {
            throw new IllegalArgumentException("Unable to add a null mediapackage");
        }
        final String mediaPackageId = mediaPackage.getIdentifier().toString();
        SearchServiceImpl.logger.debug("Attempting to add media package {} to search index", (Object)mediaPackageId);
        final AccessControlList acl = (AccessControlList)this.authorizationService.getActiveAcl(mediaPackage).getA();
        final AccessControlList seriesAcl = this.persistence.getAccessControlLists(mediaPackage.getSeries(), mediaPackageId).stream().reduce(new AccessControlList(acl.getEntries()), AccessControlList::mergeActions);
        SearchServiceImpl.logger.debug("Updating series with merged access control list: {}", (Object)seriesAcl);
        final Date now = new Date();
        try {
            if (this.indexManager.add(mediaPackage, acl, seriesAcl, now)) {
                SearchServiceImpl.logger.info("Added media package `{}` to the search index, using ACL `{}`", (Object)mediaPackageId, (Object)acl);
            }
            else {
                SearchServiceImpl.logger.warn("Failed to add media package {} to the search index", (Object)mediaPackageId);
            }
        }
        catch (SolrServerException e) {
            throw new SearchException((Throwable)e);
        }
        try {
            this.persistence.storeMediaPackage(mediaPackage, acl, now);
        }
        catch (SearchServiceDatabaseException e2) {
            throw new SearchException(String.format("Could not store media package to search database %s", mediaPackageId), (Throwable)e2);
        }
    }
    
    public Job delete(final String mediaPackageId) throws SearchException, UnauthorizedException, NotFoundException {
        try {
            return this.serviceRegistry.createJob("org.opencastproject.search", Operation.Delete.toString(), (List)Arrays.asList(mediaPackageId), Float.valueOf(this.deleteJobLoad));
        }
        catch (ServiceRegistryException e) {
            throw new SearchException((Throwable)e);
        }
    }
    
    public boolean deleteSynchronously(final String mediaPackageId) throws SearchException {
        try {
            final SearchResult result = this.solrRequester.getForWrite(new SearchQuery().withId(mediaPackageId));
            if (result.getItems().length == 0) {
                SearchServiceImpl.logger.warn("Can not delete mediapackage {}, which is not available for the current user to delete from the search index.", (Object)mediaPackageId);
                return false;
            }
            final String seriesId = result.getItems()[0].getDcIsPartOf();
            SearchServiceImpl.logger.info("Removing media package {} from search index", (Object)mediaPackageId);
            final Date now = new Date();
            try {
                this.persistence.deleteMediaPackage(mediaPackageId, now);
                SearchServiceImpl.logger.info("Removed mediapackage {} from search persistence", (Object)mediaPackageId);
            }
            catch (NotFoundException e3) {
                SearchServiceImpl.logger.info("Could not find mediapackage with id {} in persistence, but will try remove it from index anyway.", (Object)mediaPackageId);
            }
            catch (SearchServiceDatabaseException e) {
                throw new SearchException(String.format("Could not delete mediapackage with id %s from persistence storage", mediaPackageId), (Throwable)e);
            }
            final boolean success = this.indexManager.delete(mediaPackageId, now);
            if (seriesId != null) {
                if (this.persistence.getMediaPackages(seriesId).size() > 0) {
                    final AccessControlList seriesAcl = this.persistence.getAccessControlLists(seriesId, new String[0]).stream().reduce(new AccessControlList(), AccessControlList::mergeActions);
                    this.indexManager.addSeries(seriesId, seriesAcl);
                }
                else {
                    this.indexManager.delete(seriesId, now);
                }
            }
            return success;
        }
        catch (SolrServerException | SearchServiceDatabaseException ex2) {
            final Exception ex;
            final Exception e2 = ex;
            SearchServiceImpl.logger.info("Could not delete media package with id {} from search index", (Object)mediaPackageId);
            throw new SearchException((Throwable)e2);
        }
    }
    
    public void clear() throws SearchException {
        try {
            SearchServiceImpl.logger.info("Clearing the search index");
            this.indexManager.clear();
        }
        catch (SolrServerException e) {
            throw new SearchException((Throwable)e);
        }
    }
    
    public SearchResult getByQuery(final SearchQuery q) throws SearchException {
        try {
            SearchServiceImpl.logger.debug("Searching index using query object '" + q + "'");
            return this.solrRequester.getForRead(q);
        }
        catch (SolrServerException e) {
            throw new SearchException((Throwable)e);
        }
    }
    
    public SearchResult getForAdministrativeRead(final SearchQuery q) throws SearchException, UnauthorizedException {
        final User user = this.securityService.getUser();
        if (!user.hasRole("ROLE_ADMIN") && !user.hasRole(user.getOrganization().getAdminRole())) {
            throw new UnauthorizedException(user, this.getClass().getName() + ".getForAdministrativeRead");
        }
        try {
            return this.solrRequester.getForAdministrativeRead(q);
        }
        catch (SolrServerException e) {
            throw new SearchException((Throwable)e);
        }
    }
    
    protected void populateIndex(final String systemUserName) {
        long instancesInSolr = 0L;
        try {
            instancesInSolr = this.indexManager.count();
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
        if (instancesInSolr > 0L) {
            SearchServiceImpl.logger.debug("Search index found");
            return;
        }
        if (instancesInSolr == 0L) {
            SearchServiceImpl.logger.info("No search index found");
            int total = 0;
            Iterator<Tuple<MediaPackage, String>> mediaPackages;
            try {
                total = this.persistence.countMediaPackages();
                SearchServiceImpl.logger.info("Starting population of search index from {} items in database", (Object)total);
                mediaPackages = this.persistence.getAllMediaPackages();
            }
            catch (SearchServiceDatabaseException e2) {
                SearchServiceImpl.logger.error("Unable to load the search entries: {}", (Object)e2.getMessage());
                throw new ServiceException(e2.getMessage());
            }
            int errors = 0;
            int current = 0;
            while (mediaPackages.hasNext()) {
                ++current;
                try {
                    final Tuple<MediaPackage, String> episode = mediaPackages.next();
                    final MediaPackage mediaPackage = (MediaPackage)episode.getA();
                    final String mediaPackageId = mediaPackage.getIdentifier().toString();
                    final Organization organization = this.organizationDirectory.getOrganization((String)episode.getB());
                    this.securityService.setOrganization(organization);
                    this.securityService.setUser(SecurityUtil.createSystemUser(systemUserName, organization));
                    final AccessControlList acl = this.persistence.getAccessControlList(mediaPackageId);
                    final Date modificationDate = this.persistence.getModificationDate(mediaPackageId);
                    final Date deletionDate = this.persistence.getDeletionDate(mediaPackageId);
                    final AccessControlList seriesAcl = this.persistence.getAccessControlLists(mediaPackage.getSeries(), mediaPackageId).stream().reduce(new AccessControlList(acl.getEntries()), AccessControlList::mergeActions);
                    SearchServiceImpl.logger.debug("Updating series with merged access control list: {}", (Object)seriesAcl);
                    this.indexManager.add((MediaPackage)episode.getA(), acl, seriesAcl, deletionDate, modificationDate);
                }
                catch (Exception e3) {
                    SearchServiceImpl.logger.error("Unable to index search instances", (Throwable)e3);
                    if (this.retryToPopulateIndex(systemUserName)) {
                        SearchServiceImpl.logger.warn("Trying to re-index search index later. Aborting for now.");
                        return;
                    }
                    ++errors;
                }
                finally {
                    this.securityService.setOrganization((Organization)null);
                    this.securityService.setUser((User)null);
                }
                if (current % 100 == 0) {
                    SearchServiceImpl.logger.info("Indexing search {}/{} ({} percent done)", new Object[] { current, total, current * 100 / total });
                }
            }
            if (errors > 0) {
                SearchServiceImpl.logger.error("Skipped {} erroneous search entries while populating the search index", (Object)errors);
            }
            SearchServiceImpl.logger.info("Finished populating search index");
        }
    }
    
    private boolean retryToPopulateIndex(final String systemUserName) {
        if (this.retriesToPopulateIndex > 0) {
            return false;
        }
        long instancesInSolr = 0L;
        try {
            instancesInSolr = this.indexManager.count();
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
        if (instancesInSolr > 0L) {
            SearchServiceImpl.logger.debug("Search index found, other files could be indexed. No retry needed.");
            return false;
        }
        ++this.retriesToPopulateIndex;
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(30000L);
                }
                catch (InterruptedException ex) {}
                SearchServiceImpl.this.populateIndex(systemUserName);
            }
        }.start();
        return true;
    }
    
    protected String process(final Job job) throws Exception {
        Operation op = null;
        final String operation = job.getOperation();
        final List<String> arguments = (List<String>)job.getArguments();
        try {
            op = Operation.valueOf(operation);
            switch (op) {
                case Add: {
                    final MediaPackage mediaPackage = MediaPackageParser.getFromXml((String)arguments.get(0));
                    this.addSynchronously(mediaPackage);
                    return null;
                }
                case Delete: {
                    final String mediapackageId = arguments.get(0);
                    final boolean deleted = this.deleteSynchronously(mediapackageId);
                    return Boolean.toString(deleted);
                }
                default: {
                    throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
                }
            }
        }
        catch (IllegalArgumentException e) {
            throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'", (Throwable)e);
        }
        catch (IndexOutOfBoundsException e2) {
            throw new ServiceRegistryException("This argument list for operation '" + op + "' does not meet expectations", (Throwable)e2);
        }
        catch (Exception e3) {
            throw new ServiceRegistryException("Error handling operation '" + op + "'", (Throwable)e3);
        }
    }
    
    void testSetup(final SolrServer server, final SolrRequester requester, final SolrIndexManager manager) {
        this.solrServer = server;
        this.solrRequester = requester;
        this.indexManager = manager;
    }
    
    public void setStaticMetadataService(final StaticMetadataService mdService) {
        this.mdServices.add(mdService);
        if (this.indexManager != null) {
            this.indexManager.setStaticMetadataServices(this.mdServices);
        }
    }
    
    public void unsetStaticMetadataService(final StaticMetadataService mdService) {
        this.mdServices.remove(mdService);
        if (this.indexManager != null) {
            this.indexManager.setStaticMetadataServices(this.mdServices);
        }
    }
    
    public void setMpeg7CatalogService(final Mpeg7CatalogService mpeg7CatalogService) {
        this.mpeg7CatalogService = mpeg7CatalogService;
    }
    
    public void setPersistence(final SearchServiceDatabase persistence) {
        this.persistence = persistence;
    }
    
    public void setSeriesService(final SeriesService seriesService) {
        this.seriesService = seriesService;
    }
    
    public void setWorkspace(final Workspace workspace) {
        this.workspace = workspace;
    }
    
    public void setAuthorizationService(final AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }
    
    public void setServiceRegistry(final ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }
    
    public void setSecurityService(final SecurityService securityService) {
        this.securityService = securityService;
    }
    
    public void setUserDirectoryService(final UserDirectoryService userDirectoryService) {
        this.userDirectoryService = userDirectoryService;
    }
    
    public void setOrganizationDirectoryService(final OrganizationDirectoryService organizationDirectory) {
        this.organizationDirectory = organizationDirectory;
    }
    
    protected OrganizationDirectoryService getOrganizationDirectoryService() {
        return this.organizationDirectory;
    }
    
    protected SecurityService getSecurityService() {
        return this.securityService;
    }
    
    protected ServiceRegistry getServiceRegistry() {
        return this.serviceRegistry;
    }
    
    protected UserDirectoryService getUserDirectoryService() {
        return this.userDirectoryService;
    }
    
    protected void setMediaPackageSerializer(final MediaPackageSerializer serializer) {
        this.serializer = serializer;
        if (this.solrRequester != null) {
            this.solrRequester.setMediaPackageSerializer(serializer);
        }
    }
    
    public void updated(final Dictionary properties) throws ConfigurationException {
        this.addJobLoad = LoadUtil.getConfiguredLoadValue(properties, "job.load.add", Float.valueOf(0.1f), this.serviceRegistry);
        this.deleteJobLoad = LoadUtil.getConfiguredLoadValue(properties, "job.load.delete", Float.valueOf(0.1f), this.serviceRegistry);
    }
    
    public List<Pattern> getProtectedUrlPattern() {
        return Collections.singletonList(SearchServiceImpl.staticFilePattern);
    }
    
    private boolean loadUrlAccess(final String mediaPackageId) {
        SearchServiceImpl.logger.debug("Check if user `{}` has access to media package `{}`", (Object)this.securityService.getUser(), (Object)mediaPackageId);
        final SearchQuery query = new SearchQuery().withId(mediaPackageId).includeEpisodes(true).includeSeries(false);
        return this.getByQuery(query).size() > 0L;
    }
    
    public boolean verifyUrlAccess(final String path) {
        final User user = this.securityService.getUser();
        if (user.hasRole("ROLE_ADMIN")) {
            SearchServiceImpl.logger.debug("Allow access for admin `{}`", (Object)user);
            return true;
        }
        final Matcher m = SearchServiceImpl.staticFilePattern.matcher(path);
        if (!m.matches()) {
            SearchServiceImpl.logger.debug("Path does not match pattern. Preventing access.");
            return false;
        }
        final String organizationId = m.group(1);
        if (!this.securityService.getOrganization().getId().equals(organizationId)) {
            SearchServiceImpl.logger.debug("The user's organization does not match. Preventing access.");
            return false;
        }
        final String mediaPackageId = m.group(2);
        final boolean access = (boolean)this.cache.getUnchecked((Object)Tuple.tuple((Object)user, (Object)mediaPackageId));
        SearchServiceImpl.logger.debug("Check if user `{}` has access to media package `{}` using cache: {}", new Object[] { user, mediaPackageId, access });
        return access;
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)SearchServiceImpl.class);
        staticFilePattern = Pattern.compile("^/([^/]+)/engage-player/([^/]+)/.*$");
    }
    
    private enum Operation
    {
        Add, 
        Delete;
    }
}
