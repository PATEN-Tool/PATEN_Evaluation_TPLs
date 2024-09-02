// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.solr.handler.admin;

import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.util.Set;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorChain;
import java.util.Iterator;
import java.util.List;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.common.params.SolrParams;
import java.util.Map;
import org.apache.lucene.util.IOUtils;
import org.apache.solr.util.RefCounted;
import org.apache.solr.update.MergeIndexesCommand;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.core.SolrCore;
import org.apache.lucene.index.DirectoryReader;
import org.apache.solr.core.DirectoryFactory;
import org.apache.solr.core.CachingDirectoryFactory;
import org.apache.solr.common.SolrException;
import org.apache.lucene.store.Directory;
import java.util.HashMap;
import com.google.common.collect.Lists;
import org.slf4j.Logger;

class MergeIndexesOp implements CoreAdminHandler.CoreAdminOp
{
    private static final Logger log;
    
    @Override
    public void execute(final CoreAdminHandler.CallInfo it) throws Exception {
        final SolrParams params = it.req.getParams();
        final String cname = params.required().get("core");
        final SolrCore core = it.handler.coreContainer.getCore(cname);
        SolrQueryRequest wrappedReq = null;
        if (core == null) {
            return;
        }
        final List<SolrCore> sourceCores = (List<SolrCore>)Lists.newArrayList();
        final List<RefCounted<SolrIndexSearcher>> searchers = (List<RefCounted<SolrIndexSearcher>>)Lists.newArrayList();
        final List<DirectoryReader> readersToBeClosed = (List<DirectoryReader>)Lists.newArrayList();
        final Map<Directory, Boolean> dirsToBeReleased = new HashMap<Directory, Boolean>();
        try {
            final String[] dirNames = params.getParams("indexDir");
            if (dirNames == null || dirNames.length == 0) {
                final String[] sources = params.getParams("srcCore");
                if (sources == null || sources.length == 0) {
                    throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "At least one indexDir or srcCore must be specified");
                }
                for (int i = 0; i < sources.length; ++i) {
                    final String source = sources[i];
                    final SolrCore srcCore = it.handler.coreContainer.getCore(source);
                    if (srcCore == null) {
                        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Core: " + source + " does not exist");
                    }
                    sourceCores.add(srcCore);
                }
            }
            else {
                final DirectoryFactory dirFactory = core.getDirectoryFactory();
                for (int i = 0; i < dirNames.length; ++i) {
                    boolean markAsDone = false;
                    if (dirFactory instanceof CachingDirectoryFactory && !((CachingDirectoryFactory)dirFactory).getLivePaths().contains(dirNames[i])) {
                        markAsDone = true;
                    }
                    final Directory dir = dirFactory.get(dirNames[i], DirectoryFactory.DirContext.DEFAULT, core.getSolrConfig().indexConfig.lockType);
                    dirsToBeReleased.put(dir, markAsDone);
                    readersToBeClosed.add(DirectoryReader.open(dir));
                }
            }
            List<DirectoryReader> readers = null;
            if (readersToBeClosed.size() > 0) {
                readers = readersToBeClosed;
            }
            else {
                readers = (List<DirectoryReader>)Lists.newArrayList();
                for (final SolrCore solrCore : sourceCores) {
                    final RefCounted<SolrIndexSearcher> searcher = solrCore.getSearcher();
                    searchers.add(searcher);
                    readers.add(searcher.get().getIndexReader());
                }
            }
            final UpdateRequestProcessorChain processorChain = core.getUpdateProcessingChain(params.get("update.chain"));
            wrappedReq = new LocalSolrQueryRequest(core, it.req.getParams());
            final UpdateRequestProcessor processor = processorChain.createProcessor(wrappedReq, it.rsp);
            processor.processMergeIndexes(new MergeIndexesCommand(readers, it.req));
        }
        catch (Exception e) {
            MergeIndexesOp.log.error("ERROR executing merge:", (Throwable)e);
            throw e;
        }
        finally {
            for (final RefCounted<SolrIndexSearcher> searcher2 : searchers) {
                if (searcher2 != null) {
                    searcher2.decref();
                }
            }
            for (final SolrCore solrCore2 : sourceCores) {
                if (solrCore2 != null) {
                    solrCore2.close();
                }
            }
            IOUtils.closeWhileHandlingException((Iterable)readersToBeClosed);
            final Set<Map.Entry<Directory, Boolean>> entries = dirsToBeReleased.entrySet();
            for (final Map.Entry<Directory, Boolean> entry : entries) {
                final DirectoryFactory dirFactory2 = core.getDirectoryFactory();
                final Directory dir2 = entry.getKey();
                final boolean markAsDone2 = entry.getValue();
                if (markAsDone2) {
                    dirFactory2.doneWithDirectory(dir2);
                }
                dirFactory2.release(dir2);
            }
            if (wrappedReq != null) {
                wrappedReq.close();
            }
            core.close();
        }
    }
    
    static {
        log = LoggerFactory.getLogger((Class)MethodHandles.lookup().lookupClass());
    }
}
