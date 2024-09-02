// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.zookeeper.server;

import org.slf4j.LoggerFactory;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.Iterator;
import org.apache.zookeeper.proto.GetEphemeralsResponse;
import java.util.Collection;
import org.apache.zookeeper.proto.GetEphemeralsRequest;
import org.apache.zookeeper.proto.WhoAmIResponse;
import org.apache.zookeeper.server.util.AuthUtil;
import org.apache.zookeeper.proto.RemoveWatchesRequest;
import java.util.Locale;
import org.apache.zookeeper.proto.CheckWatchesRequest;
import org.apache.zookeeper.proto.GetChildren2Response;
import org.apache.zookeeper.proto.GetChildren2Request;
import org.apache.zookeeper.proto.GetAllChildrenNumberResponse;
import org.apache.zookeeper.proto.GetAllChildrenNumberRequest;
import org.apache.zookeeper.proto.GetChildrenRequest;
import org.apache.zookeeper.data.Id;
import java.util.ArrayList;
import org.apache.zookeeper.proto.GetACLResponse;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.proto.GetACLRequest;
import org.apache.zookeeper.proto.ErrorResponse;
import org.apache.zookeeper.proto.AddWatchRequest;
import org.apache.zookeeper.proto.SetWatches2;
import org.apache.zookeeper.Watcher;
import java.util.Collections;
import org.apache.zookeeper.proto.SetWatches;
import org.apache.zookeeper.proto.GetDataRequest;
import org.apache.zookeeper.proto.ExistsResponse;
import org.apache.zookeeper.proto.ExistsRequest;
import org.apache.zookeeper.proto.SyncResponse;
import org.apache.zookeeper.proto.SyncRequest;
import org.apache.zookeeper.proto.SetACLResponse;
import java.nio.charset.StandardCharsets;
import org.apache.zookeeper.server.quorum.QuorumZooKeeperServer;
import org.apache.zookeeper.proto.SetDataResponse;
import org.apache.zookeeper.proto.Create2Response;
import org.apache.zookeeper.proto.CreateResponse;
import org.apache.zookeeper.proto.GetDataResponse;
import java.util.List;
import org.apache.zookeeper.proto.GetChildrenResponse;
import org.apache.zookeeper.Op;
import org.apache.zookeeper.MultiOperationRecord;
import java.io.IOException;
import org.apache.zookeeper.OpResult;
import org.apache.zookeeper.MultiResponse;
import org.apache.jute.Record;
import org.apache.zookeeper.proto.ReplyHeader;
import org.apache.zookeeper.txn.ErrorTxn;
import org.apache.zookeeper.audit.AuditHelper;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.common.Time;
import org.apache.zookeeper.server.util.RequestPathMetricsCollector;
import org.slf4j.Logger;

public class FinalRequestProcessor implements RequestProcessor
{
    private static final Logger LOG;
    private final RequestPathMetricsCollector requestPathMetricsCollector;
    ZooKeeperServer zks;
    
    public FinalRequestProcessor(final ZooKeeperServer zks) {
        this.zks = zks;
        this.requestPathMetricsCollector = zks.getRequestPathMetricsCollector();
    }
    
    private DataTree.ProcessTxnResult applyRequest(final Request request) {
        final DataTree.ProcessTxnResult rc = this.zks.processTxn(request);
        if (request.type == -11 && this.connClosedByClient(request) && (this.closeSession(this.zks.serverCnxnFactory, request.sessionId) || this.closeSession(this.zks.secureServerCnxnFactory, request.sessionId))) {
            return rc;
        }
        if (request.getHdr() != null) {
            final long propagationLatency = Time.currentWallTime() - request.getHdr().getTime();
            if (propagationLatency >= 0L) {
                ServerMetrics.getMetrics().PROPAGATION_LATENCY.add(propagationLatency);
            }
        }
        return rc;
    }
    
    @Override
    public void processRequest(final Request request) {
        FinalRequestProcessor.LOG.debug("Processing request:: {}", (Object)request);
        if (FinalRequestProcessor.LOG.isTraceEnabled()) {
            long traceMask = 2L;
            if (request.type == 11) {
                traceMask = 128L;
            }
            ZooTrace.logRequest(FinalRequestProcessor.LOG, traceMask, 'E', request, "");
        }
        DataTree.ProcessTxnResult rc = null;
        if (!request.isThrottled()) {
            rc = this.applyRequest(request);
        }
        if (request.cnxn == null) {
            return;
        }
        final ServerCnxn cnxn = request.cnxn;
        final long lastZxid = this.zks.getZKDatabase().getDataTreeLastProcessedZxid();
        String lastOp = "NA";
        this.zks.decInProcess();
        this.zks.requestFinished(request);
        KeeperException.Code err = KeeperException.Code.OK;
        Record rsp = null;
        String path = null;
        int responseSize = 0;
        try {
            if (request.getHdr() != null && request.getHdr().getType() == -1) {
                AuditHelper.addAuditLog(request, rc, true);
                if (request.getException() != null) {
                    throw request.getException();
                }
                throw KeeperException.create(KeeperException.Code.get(((ErrorTxn)request.getTxn()).getErr()));
            }
            else {
                final KeeperException ke = request.getException();
                if (ke instanceof KeeperException.SessionMovedException) {
                    throw ke;
                }
                if (ke != null && request.type != 14) {
                    throw ke;
                }
                FinalRequestProcessor.LOG.debug("{}", (Object)request);
                if (request.isStale()) {
                    ServerMetrics.getMetrics().STALE_REPLIES.add(1L);
                }
                if (request.isThrottled()) {
                    throw KeeperException.create(KeeperException.Code.THROTTLEDOP);
                }
                AuditHelper.addAuditLog(request, rc);
                switch (request.type) {
                    case 11: {
                        lastOp = "PING";
                        this.updateStats(request, lastOp, lastZxid);
                        responseSize = cnxn.sendResponse(new ReplyHeader(-2, lastZxid, 0), null, "response");
                        return;
                    }
                    case -10: {
                        lastOp = "SESS";
                        this.updateStats(request, lastOp, lastZxid);
                        this.zks.finishSessionInit(request.cnxn, true);
                        return;
                    }
                    case 14: {
                        lastOp = "MULT";
                        rsp = (Record)new MultiResponse();
                        for (final DataTree.ProcessTxnResult subTxnResult : rc.multiResult) {
                            OpResult subResult = null;
                            switch (subTxnResult.type) {
                                case 13: {
                                    subResult = new OpResult.CheckResult();
                                    break;
                                }
                                case 1: {
                                    subResult = new OpResult.CreateResult(subTxnResult.path);
                                    break;
                                }
                                case 15:
                                case 19:
                                case 21: {
                                    subResult = new OpResult.CreateResult(subTxnResult.path, subTxnResult.stat);
                                    break;
                                }
                                case 2:
                                case 20: {
                                    subResult = new OpResult.DeleteResult();
                                    break;
                                }
                                case 5: {
                                    subResult = new OpResult.SetDataResult(subTxnResult.stat);
                                    break;
                                }
                                case -1: {
                                    subResult = new OpResult.ErrorResult(subTxnResult.err);
                                    if (subTxnResult.err == KeeperException.Code.SESSIONMOVED.intValue()) {
                                        throw new KeeperException.SessionMovedException();
                                    }
                                    break;
                                }
                                default: {
                                    throw new IOException("Invalid type of op");
                                }
                            }
                            ((MultiResponse)rsp).add(subResult);
                        }
                        break;
                    }
                    case 22: {
                        lastOp = "MLTR";
                        final MultiOperationRecord multiReadRecord = new MultiOperationRecord();
                        ByteBufferInputStream.byteBuffer2Record(request.request, (Record)multiReadRecord);
                        rsp = (Record)new MultiResponse();
                        for (final Op readOp : multiReadRecord) {
                            OpResult subResult2 = null;
                            try {
                                switch (readOp.getType()) {
                                    case 8: {
                                        final Record rec = this.handleGetChildrenRequest(readOp.toRequestRecord(), cnxn, request.authInfo);
                                        subResult2 = new OpResult.GetChildrenResult(((GetChildrenResponse)rec).getChildren());
                                        break;
                                    }
                                    case 4: {
                                        final Record rec = this.handleGetDataRequest(readOp.toRequestRecord(), cnxn, request.authInfo);
                                        final GetDataResponse gdr = (GetDataResponse)rec;
                                        subResult2 = new OpResult.GetDataResult(gdr.getData(), gdr.getStat());
                                        break;
                                    }
                                    default: {
                                        throw new IOException("Invalid type of readOp");
                                    }
                                }
                            }
                            catch (KeeperException e) {
                                subResult2 = new OpResult.ErrorResult(e.code().intValue());
                            }
                            ((MultiResponse)rsp).add(subResult2);
                        }
                        break;
                    }
                    case 1: {
                        lastOp = "CREA";
                        rsp = (Record)new CreateResponse(rc.path);
                        err = KeeperException.Code.get(rc.err);
                        this.requestPathMetricsCollector.registerRequest(request.type, rc.path);
                        break;
                    }
                    case 15:
                    case 19:
                    case 21: {
                        lastOp = "CREA";
                        rsp = (Record)new Create2Response(rc.path, rc.stat);
                        err = KeeperException.Code.get(rc.err);
                        this.requestPathMetricsCollector.registerRequest(request.type, rc.path);
                        break;
                    }
                    case 2:
                    case 20: {
                        lastOp = "DELE";
                        err = KeeperException.Code.get(rc.err);
                        this.requestPathMetricsCollector.registerRequest(request.type, rc.path);
                        break;
                    }
                    case 5: {
                        lastOp = "SETD";
                        rsp = (Record)new SetDataResponse(rc.stat);
                        err = KeeperException.Code.get(rc.err);
                        this.requestPathMetricsCollector.registerRequest(request.type, rc.path);
                        break;
                    }
                    case 16: {
                        lastOp = "RECO";
                        rsp = (Record)new GetDataResponse(((QuorumZooKeeperServer)this.zks).self.getQuorumVerifier().toString().getBytes(StandardCharsets.UTF_8), rc.stat);
                        err = KeeperException.Code.get(rc.err);
                        break;
                    }
                    case 7: {
                        lastOp = "SETA";
                        rsp = (Record)new SetACLResponse(rc.stat);
                        err = KeeperException.Code.get(rc.err);
                        this.requestPathMetricsCollector.registerRequest(request.type, rc.path);
                        break;
                    }
                    case -11: {
                        lastOp = "CLOS";
                        err = KeeperException.Code.get(rc.err);
                        break;
                    }
                    case 9: {
                        lastOp = "SYNC";
                        final SyncRequest syncRequest = new SyncRequest();
                        ByteBufferInputStream.byteBuffer2Record(request.request, (Record)syncRequest);
                        rsp = (Record)new SyncResponse(syncRequest.getPath());
                        this.requestPathMetricsCollector.registerRequest(request.type, syncRequest.getPath());
                        break;
                    }
                    case 13: {
                        lastOp = "CHEC";
                        rsp = (Record)new SetDataResponse(rc.stat);
                        err = KeeperException.Code.get(rc.err);
                        break;
                    }
                    case 3: {
                        lastOp = "EXIS";
                        final ExistsRequest existsRequest = new ExistsRequest();
                        ByteBufferInputStream.byteBuffer2Record(request.request, (Record)existsRequest);
                        path = existsRequest.getPath();
                        if (path.indexOf(0) != -1) {
                            throw new KeeperException.BadArgumentsException();
                        }
                        final Stat stat = this.zks.getZKDatabase().statNode(path, existsRequest.getWatch() ? cnxn : null);
                        rsp = (Record)new ExistsResponse(stat);
                        this.requestPathMetricsCollector.registerRequest(request.type, path);
                        break;
                    }
                    case 4: {
                        lastOp = "GETD";
                        final GetDataRequest getDataRequest = new GetDataRequest();
                        ByteBufferInputStream.byteBuffer2Record(request.request, (Record)getDataRequest);
                        path = getDataRequest.getPath();
                        rsp = this.handleGetDataRequest((Record)getDataRequest, cnxn, request.authInfo);
                        this.requestPathMetricsCollector.registerRequest(request.type, path);
                        break;
                    }
                    case 101: {
                        lastOp = "SETW";
                        final SetWatches setWatches = new SetWatches();
                        request.request.rewind();
                        ByteBufferInputStream.byteBuffer2Record(request.request, (Record)setWatches);
                        final long relativeZxid = setWatches.getRelativeZxid();
                        this.zks.getZKDatabase().setWatches(relativeZxid, setWatches.getDataWatches(), setWatches.getExistWatches(), setWatches.getChildWatches(), Collections.emptyList(), Collections.emptyList(), cnxn);
                        break;
                    }
                    case 105: {
                        lastOp = "STW2";
                        final SetWatches2 setWatches2 = new SetWatches2();
                        request.request.rewind();
                        ByteBufferInputStream.byteBuffer2Record(request.request, (Record)setWatches2);
                        final long relativeZxid = setWatches2.getRelativeZxid();
                        this.zks.getZKDatabase().setWatches(relativeZxid, setWatches2.getDataWatches(), setWatches2.getExistWatches(), setWatches2.getChildWatches(), setWatches2.getPersistentWatches(), setWatches2.getPersistentRecursiveWatches(), cnxn);
                        break;
                    }
                    case 106: {
                        lastOp = "ADDW";
                        final AddWatchRequest addWatcherRequest = new AddWatchRequest();
                        ByteBufferInputStream.byteBuffer2Record(request.request, (Record)addWatcherRequest);
                        this.zks.getZKDatabase().addWatch(addWatcherRequest.getPath(), cnxn, addWatcherRequest.getMode());
                        rsp = (Record)new ErrorResponse(0);
                        break;
                    }
                    case 6: {
                        lastOp = "GETA";
                        final GetACLRequest getACLRequest = new GetACLRequest();
                        ByteBufferInputStream.byteBuffer2Record(request.request, (Record)getACLRequest);
                        path = getACLRequest.getPath();
                        final DataNode n = this.zks.getZKDatabase().getNode(path);
                        if (n == null) {
                            throw new KeeperException.NoNodeException();
                        }
                        this.zks.checkACL(request.cnxn, this.zks.getZKDatabase().aclForNode(n), 17, request.authInfo, path, null);
                        final Stat stat2 = new Stat();
                        final List<ACL> acl = this.zks.getZKDatabase().getACL(path, stat2);
                        this.requestPathMetricsCollector.registerRequest(request.type, getACLRequest.getPath());
                        try {
                            this.zks.checkACL(request.cnxn, this.zks.getZKDatabase().aclForNode(n), 16, request.authInfo, path, null);
                            rsp = (Record)new GetACLResponse((List)acl, stat2);
                        }
                        catch (KeeperException.NoAuthException e5) {
                            final List<ACL> acl2 = new ArrayList<ACL>(acl.size());
                            for (final ACL a : acl) {
                                if ("digest".equals(a.getId().getScheme())) {
                                    final Id id = a.getId();
                                    final Id id2 = new Id(id.getScheme(), id.getId().replaceAll(":.*", ":x"));
                                    acl2.add(new ACL(a.getPerms(), id2));
                                }
                                else {
                                    acl2.add(a);
                                }
                            }
                            rsp = (Record)new GetACLResponse((List)acl2, stat2);
                        }
                        break;
                    }
                    case 8: {
                        lastOp = "GETC";
                        final GetChildrenRequest getChildrenRequest = new GetChildrenRequest();
                        ByteBufferInputStream.byteBuffer2Record(request.request, (Record)getChildrenRequest);
                        path = getChildrenRequest.getPath();
                        rsp = this.handleGetChildrenRequest((Record)getChildrenRequest, cnxn, request.authInfo);
                        this.requestPathMetricsCollector.registerRequest(request.type, path);
                        break;
                    }
                    case 104: {
                        lastOp = "GETACN";
                        final GetAllChildrenNumberRequest getAllChildrenNumberRequest = new GetAllChildrenNumberRequest();
                        ByteBufferInputStream.byteBuffer2Record(request.request, (Record)getAllChildrenNumberRequest);
                        path = getAllChildrenNumberRequest.getPath();
                        final DataNode n = this.zks.getZKDatabase().getNode(path);
                        if (n == null) {
                            throw new KeeperException.NoNodeException();
                        }
                        this.zks.checkACL(request.cnxn, this.zks.getZKDatabase().aclForNode(n), 1, request.authInfo, path, null);
                        final int number = this.zks.getZKDatabase().getAllChildrenNumber(path);
                        rsp = (Record)new GetAllChildrenNumberResponse(number);
                        break;
                    }
                    case 12: {
                        lastOp = "GETC";
                        final GetChildren2Request getChildren2Request = new GetChildren2Request();
                        ByteBufferInputStream.byteBuffer2Record(request.request, (Record)getChildren2Request);
                        final Stat stat = new Stat();
                        path = getChildren2Request.getPath();
                        final DataNode n2 = this.zks.getZKDatabase().getNode(path);
                        if (n2 == null) {
                            throw new KeeperException.NoNodeException();
                        }
                        this.zks.checkACL(request.cnxn, this.zks.getZKDatabase().aclForNode(n2), 1, request.authInfo, path, null);
                        final List<String> children = this.zks.getZKDatabase().getChildren(path, stat, getChildren2Request.getWatch() ? cnxn : null);
                        rsp = (Record)new GetChildren2Response((List)children, stat);
                        this.requestPathMetricsCollector.registerRequest(request.type, path);
                        break;
                    }
                    case 17: {
                        lastOp = "CHKW";
                        final CheckWatchesRequest checkWatches = new CheckWatchesRequest();
                        ByteBufferInputStream.byteBuffer2Record(request.request, (Record)checkWatches);
                        final Watcher.WatcherType type = Watcher.WatcherType.fromInt(checkWatches.getType());
                        path = checkWatches.getPath();
                        final boolean containsWatcher = this.zks.getZKDatabase().containsWatcher(path, type, cnxn);
                        if (!containsWatcher) {
                            final String msg = String.format(Locale.ENGLISH, "%s (type: %s)", path, type);
                            throw new KeeperException.NoWatcherException(msg);
                        }
                        this.requestPathMetricsCollector.registerRequest(request.type, checkWatches.getPath());
                        break;
                    }
                    case 18: {
                        lastOp = "REMW";
                        final RemoveWatchesRequest removeWatches = new RemoveWatchesRequest();
                        ByteBufferInputStream.byteBuffer2Record(request.request, (Record)removeWatches);
                        final Watcher.WatcherType type = Watcher.WatcherType.fromInt(removeWatches.getType());
                        path = removeWatches.getPath();
                        final boolean removed = this.zks.getZKDatabase().removeWatch(path, type, cnxn);
                        if (!removed) {
                            final String msg = String.format(Locale.ENGLISH, "%s (type: %s)", path, type);
                            throw new KeeperException.NoWatcherException(msg);
                        }
                        this.requestPathMetricsCollector.registerRequest(request.type, removeWatches.getPath());
                        break;
                    }
                    case 107: {
                        lastOp = "HOMI";
                        rsp = (Record)new WhoAmIResponse((List)AuthUtil.getClientInfos(request.authInfo));
                        break;
                    }
                    case 103: {
                        lastOp = "GETE";
                        final GetEphemeralsRequest getEphemerals = new GetEphemeralsRequest();
                        ByteBufferInputStream.byteBuffer2Record(request.request, (Record)getEphemerals);
                        final String prefixPath = getEphemerals.getPrefixPath();
                        final Set<String> allEphems = this.zks.getZKDatabase().getDataTree().getEphemerals(request.sessionId);
                        final List<String> ephemerals = new ArrayList<String>();
                        if (prefixPath == null || prefixPath.trim().isEmpty() || "/".equals(prefixPath.trim())) {
                            ephemerals.addAll(allEphems);
                        }
                        else {
                            for (final String p : allEphems) {
                                if (p.startsWith(prefixPath)) {
                                    ephemerals.add(p);
                                }
                            }
                        }
                        rsp = (Record)new GetEphemeralsResponse((List)ephemerals);
                        break;
                    }
                }
            }
        }
        catch (KeeperException.SessionMovedException e6) {
            cnxn.sendCloseSession();
            return;
        }
        catch (KeeperException e2) {
            err = e2.code();
        }
        catch (Exception e3) {
            FinalRequestProcessor.LOG.error("Failed to process {}", (Object)request, (Object)e3);
            final StringBuilder sb = new StringBuilder();
            final ByteBuffer bb = request.request;
            bb.rewind();
            while (bb.hasRemaining()) {
                sb.append(Integer.toHexString(bb.get() & 0xFF));
            }
            FinalRequestProcessor.LOG.error("Dumping request buffer: 0x{}", (Object)sb.toString());
            err = KeeperException.Code.MARSHALLINGERROR;
        }
        final ReplyHeader hdr = new ReplyHeader(request.cxid, lastZxid, err.intValue());
        this.updateStats(request, lastOp, lastZxid);
        try {
            if (path == null || rsp == null) {
                responseSize = cnxn.sendResponse(hdr, rsp, "response");
            }
            else {
                final int opCode = request.type;
                Stat stat = null;
                switch (opCode) {
                    case 4: {
                        final GetDataResponse getDataResponse = (GetDataResponse)rsp;
                        stat = getDataResponse.getStat();
                        responseSize = cnxn.sendResponse(hdr, rsp, "response", path, stat, opCode);
                        break;
                    }
                    case 12: {
                        final GetChildren2Response getChildren2Response = (GetChildren2Response)rsp;
                        stat = getChildren2Response.getStat();
                        responseSize = cnxn.sendResponse(hdr, rsp, "response", path, stat, opCode);
                        break;
                    }
                    default: {
                        responseSize = cnxn.sendResponse(hdr, rsp, "response");
                        break;
                    }
                }
            }
            if (request.type == -11) {
                cnxn.sendCloseSession();
            }
        }
        catch (IOException e4) {
            FinalRequestProcessor.LOG.error("FIXMSG", (Throwable)e4);
        }
        finally {
            ServerMetrics.getMetrics().RESPONSE_BYTES.add(responseSize);
        }
    }
    
    private Record handleGetChildrenRequest(final Record request, final ServerCnxn cnxn, final List<Id> authInfo) throws KeeperException, IOException {
        final GetChildrenRequest getChildrenRequest = (GetChildrenRequest)request;
        final String path = getChildrenRequest.getPath();
        final DataNode n = this.zks.getZKDatabase().getNode(path);
        if (n == null) {
            throw new KeeperException.NoNodeException();
        }
        this.zks.checkACL(cnxn, this.zks.getZKDatabase().aclForNode(n), 1, authInfo, path, null);
        final List<String> children = this.zks.getZKDatabase().getChildren(path, null, getChildrenRequest.getWatch() ? cnxn : null);
        return (Record)new GetChildrenResponse((List)children);
    }
    
    private Record handleGetDataRequest(final Record request, final ServerCnxn cnxn, final List<Id> authInfo) throws KeeperException, IOException {
        final GetDataRequest getDataRequest = (GetDataRequest)request;
        final String path = getDataRequest.getPath();
        final DataNode n = this.zks.getZKDatabase().getNode(path);
        if (n == null) {
            throw new KeeperException.NoNodeException();
        }
        this.zks.checkACL(cnxn, this.zks.getZKDatabase().aclForNode(n), 1, authInfo, path, null);
        final Stat stat = new Stat();
        final byte[] b = this.zks.getZKDatabase().getData(path, stat, getDataRequest.getWatch() ? cnxn : null);
        return (Record)new GetDataResponse(b, stat);
    }
    
    private boolean closeSession(final ServerCnxnFactory serverCnxnFactory, final long sessionId) {
        return serverCnxnFactory != null && serverCnxnFactory.closeSession(sessionId, ServerCnxn.DisconnectReason.CLIENT_CLOSED_SESSION);
    }
    
    private boolean connClosedByClient(final Request request) {
        return request.cnxn == null;
    }
    
    @Override
    public void shutdown() {
        FinalRequestProcessor.LOG.info("shutdown of request processor complete");
    }
    
    private void updateStats(final Request request, final String lastOp, final long lastZxid) {
        if (request.cnxn == null) {
            return;
        }
        final long currentTime = Time.currentElapsedTime();
        this.zks.serverStats().updateLatency(request, currentTime);
        request.cnxn.updateStatsForResponse(request.cxid, lastZxid, lastOp, request.createTime, currentTime);
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)FinalRequestProcessor.class);
    }
}
