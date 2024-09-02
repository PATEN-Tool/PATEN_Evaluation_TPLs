// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.zookeeper.server;

import org.slf4j.LoggerFactory;
import java.nio.ByteBuffer;
import java.util.Iterator;
import org.apache.zookeeper.txn.TxnHeader;
import org.apache.zookeeper.proto.RemoveWatchesRequest;
import java.util.Locale;
import org.apache.zookeeper.proto.CheckWatchesRequest;
import org.apache.zookeeper.proto.GetChildren2Response;
import org.apache.zookeeper.proto.GetChildren2Request;
import org.apache.zookeeper.proto.GetChildrenResponse;
import org.apache.zookeeper.proto.GetChildrenRequest;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.ACL;
import java.util.ArrayList;
import org.apache.zookeeper.proto.GetACLResponse;
import org.apache.zookeeper.proto.GetACLRequest;
import java.util.List;
import org.apache.zookeeper.proto.SetWatches;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.proto.GetDataRequest;
import org.apache.zookeeper.proto.ExistsResponse;
import org.apache.zookeeper.proto.ExistsRequest;
import org.apache.zookeeper.proto.SyncResponse;
import org.apache.zookeeper.proto.SyncRequest;
import org.apache.zookeeper.proto.SetACLResponse;
import org.apache.zookeeper.proto.GetDataResponse;
import org.apache.zookeeper.server.quorum.QuorumZooKeeperServer;
import org.apache.zookeeper.proto.SetDataResponse;
import org.apache.zookeeper.proto.Create2Response;
import org.apache.zookeeper.proto.CreateResponse;
import java.io.IOException;
import org.apache.zookeeper.OpResult;
import org.apache.zookeeper.MultiResponse;
import org.apache.jute.Record;
import org.apache.zookeeper.proto.ReplyHeader;
import org.apache.zookeeper.common.Time;
import org.apache.zookeeper.txn.ErrorTxn;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;

public class FinalRequestProcessor implements RequestProcessor
{
    private static final Logger LOG;
    ZooKeeperServer zks;
    
    public FinalRequestProcessor(final ZooKeeperServer zks) {
        this.zks = zks;
    }
    
    @Override
    public void processRequest(final Request request) {
        if (FinalRequestProcessor.LOG.isDebugEnabled()) {
            FinalRequestProcessor.LOG.debug("Processing request:: " + request);
        }
        long traceMask = 2L;
        if (request.type == 11) {
            traceMask = 128L;
        }
        if (FinalRequestProcessor.LOG.isTraceEnabled()) {
            ZooTrace.logRequest(FinalRequestProcessor.LOG, traceMask, 'E', request, "");
        }
        DataTree.ProcessTxnResult rc = null;
        synchronized (this.zks.outstandingChanges) {
            rc = this.zks.processTxn(request);
            if (request.getHdr() != null) {
                final TxnHeader hdr = request.getHdr();
                final Record txn = request.getTxn();
                final long zxid = hdr.getZxid();
                while (!this.zks.outstandingChanges.isEmpty() && this.zks.outstandingChanges.peek().zxid <= zxid) {
                    final ZooKeeperServer.ChangeRecord cr = this.zks.outstandingChanges.remove();
                    if (cr.zxid < zxid) {
                        FinalRequestProcessor.LOG.warn("Zxid outstanding " + cr.zxid + " is less than current " + zxid);
                    }
                    if (this.zks.outstandingChangesForPath.get(cr.path) == cr) {
                        this.zks.outstandingChangesForPath.remove(cr.path);
                    }
                }
            }
            if (request.isQuorum()) {
                this.zks.getZKDatabase().addCommittedProposal(request);
            }
        }
        if (request.type == -11 && this.connClosedByClient(request) && (this.closeSession(this.zks.serverCnxnFactory, request.sessionId) || this.closeSession(this.zks.secureServerCnxnFactory, request.sessionId))) {
            return;
        }
        if (request.cnxn == null) {
            return;
        }
        final ServerCnxn cnxn = request.cnxn;
        String lastOp = "NA";
        this.zks.decInProcess();
        KeeperException.Code err = KeeperException.Code.OK;
        Record rsp = null;
        try {
            if (request.getHdr() != null && request.getHdr().getType() == -1) {
                if (request.getException() != null) {
                    throw request.getException();
                }
                throw KeeperException.create(KeeperException.Code.get(((ErrorTxn)request.getTxn()).getErr()));
            }
            else {
                final KeeperException ke = request.getException();
                if (ke != null && request.type != 14) {
                    throw ke;
                }
                if (FinalRequestProcessor.LOG.isDebugEnabled()) {
                    FinalRequestProcessor.LOG.debug("{}", (Object)request);
                }
                switch (request.type) {
                    case 11: {
                        this.zks.serverStats().updateLatency(request.createTime);
                        lastOp = "PING";
                        cnxn.updateStatsForResponse(request.cxid, request.zxid, lastOp, request.createTime, Time.currentElapsedTime());
                        cnxn.sendResponse(new ReplyHeader(-2, this.zks.getZKDatabase().getDataTreeLastProcessedZxid(), 0), null, "response");
                        return;
                    }
                    case -10: {
                        this.zks.serverStats().updateLatency(request.createTime);
                        lastOp = "SESS";
                        cnxn.updateStatsForResponse(request.cxid, request.zxid, lastOp, request.createTime, Time.currentElapsedTime());
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
                    case 1: {
                        lastOp = "CREA";
                        rsp = (Record)new CreateResponse(rc.path);
                        err = KeeperException.Code.get(rc.err);
                        break;
                    }
                    case 15:
                    case 19:
                    case 21: {
                        lastOp = "CREA";
                        rsp = (Record)new Create2Response(rc.path, rc.stat);
                        err = KeeperException.Code.get(rc.err);
                        break;
                    }
                    case 2:
                    case 20: {
                        lastOp = "DELE";
                        err = KeeperException.Code.get(rc.err);
                        break;
                    }
                    case 5: {
                        lastOp = "SETD";
                        rsp = (Record)new SetDataResponse(rc.stat);
                        err = KeeperException.Code.get(rc.err);
                        break;
                    }
                    case 16: {
                        lastOp = "RECO";
                        rsp = (Record)new GetDataResponse(((QuorumZooKeeperServer)this.zks).self.getQuorumVerifier().toString().getBytes(), rc.stat);
                        err = KeeperException.Code.get(rc.err);
                        break;
                    }
                    case 7: {
                        lastOp = "SETA";
                        rsp = (Record)new SetACLResponse(rc.stat);
                        err = KeeperException.Code.get(rc.err);
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
                        final String path = existsRequest.getPath();
                        if (path.indexOf(0) != -1) {
                            throw new KeeperException.BadArgumentsException();
                        }
                        final Stat stat = this.zks.getZKDatabase().statNode(path, existsRequest.getWatch() ? cnxn : null);
                        rsp = (Record)new ExistsResponse(stat);
                        break;
                    }
                    case 4: {
                        lastOp = "GETD";
                        final GetDataRequest getDataRequest = new GetDataRequest();
                        ByteBufferInputStream.byteBuffer2Record(request.request, (Record)getDataRequest);
                        final DataNode n = this.zks.getZKDatabase().getNode(getDataRequest.getPath());
                        if (n == null) {
                            throw new KeeperException.NoNodeException();
                        }
                        PrepRequestProcessor.checkACL(this.zks, this.zks.getZKDatabase().aclForNode(n), 1, request.authInfo);
                        final Stat stat = new Stat();
                        final byte[] b = this.zks.getZKDatabase().getData(getDataRequest.getPath(), stat, getDataRequest.getWatch() ? cnxn : null);
                        rsp = (Record)new GetDataResponse(b, stat);
                        break;
                    }
                    case 101: {
                        lastOp = "SETW";
                        final SetWatches setWatches = new SetWatches();
                        request.request.rewind();
                        ByteBufferInputStream.byteBuffer2Record(request.request, (Record)setWatches);
                        final long relativeZxid = setWatches.getRelativeZxid();
                        this.zks.getZKDatabase().setWatches(relativeZxid, setWatches.getDataWatches(), setWatches.getExistWatches(), setWatches.getChildWatches(), cnxn);
                        break;
                    }
                    case 6: {
                        lastOp = "GETA";
                        final GetACLRequest getACLRequest = new GetACLRequest();
                        ByteBufferInputStream.byteBuffer2Record(request.request, (Record)getACLRequest);
                        final DataNode n = this.zks.getZKDatabase().getNode(getACLRequest.getPath());
                        if (n == null) {
                            throw new KeeperException.NoNodeException();
                        }
                        PrepRequestProcessor.checkACL(this.zks, this.zks.getZKDatabase().aclForNode(n), 17, request.authInfo);
                        final Stat stat = new Stat();
                        final List<ACL> acl = this.zks.getZKDatabase().getACL(getACLRequest.getPath(), stat);
                        try {
                            PrepRequestProcessor.checkACL(this.zks, this.zks.getZKDatabase().aclForNode(n), 16, request.authInfo);
                            rsp = (Record)new GetACLResponse((List)acl, stat);
                        }
                        catch (KeeperException.NoAuthException e4) {
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
                            rsp = (Record)new GetACLResponse((List)acl2, stat);
                        }
                        break;
                    }
                    case 8: {
                        lastOp = "GETC";
                        final GetChildrenRequest getChildrenRequest = new GetChildrenRequest();
                        ByteBufferInputStream.byteBuffer2Record(request.request, (Record)getChildrenRequest);
                        final DataNode n = this.zks.getZKDatabase().getNode(getChildrenRequest.getPath());
                        if (n == null) {
                            throw new KeeperException.NoNodeException();
                        }
                        PrepRequestProcessor.checkACL(this.zks, this.zks.getZKDatabase().aclForNode(n), 1, request.authInfo);
                        final List<String> children = this.zks.getZKDatabase().getChildren(getChildrenRequest.getPath(), null, getChildrenRequest.getWatch() ? cnxn : null);
                        rsp = (Record)new GetChildrenResponse((List)children);
                        break;
                    }
                    case 12: {
                        lastOp = "GETC";
                        final GetChildren2Request getChildren2Request = new GetChildren2Request();
                        ByteBufferInputStream.byteBuffer2Record(request.request, (Record)getChildren2Request);
                        final Stat stat2 = new Stat();
                        final DataNode n2 = this.zks.getZKDatabase().getNode(getChildren2Request.getPath());
                        if (n2 == null) {
                            throw new KeeperException.NoNodeException();
                        }
                        PrepRequestProcessor.checkACL(this.zks, this.zks.getZKDatabase().aclForNode(n2), 1, request.authInfo);
                        final List<String> children2 = this.zks.getZKDatabase().getChildren(getChildren2Request.getPath(), stat2, getChildren2Request.getWatch() ? cnxn : null);
                        rsp = (Record)new GetChildren2Response((List)children2, stat2);
                        break;
                    }
                    case 17: {
                        lastOp = "CHKW";
                        final CheckWatchesRequest checkWatches = new CheckWatchesRequest();
                        ByteBufferInputStream.byteBuffer2Record(request.request, (Record)checkWatches);
                        final Watcher.WatcherType type = Watcher.WatcherType.fromInt(checkWatches.getType());
                        final boolean containsWatcher = this.zks.getZKDatabase().containsWatcher(checkWatches.getPath(), type, cnxn);
                        if (!containsWatcher) {
                            final String msg = String.format(Locale.ENGLISH, "%s (type: %s)", checkWatches.getPath(), type);
                            throw new KeeperException.NoWatcherException(msg);
                        }
                        break;
                    }
                    case 18: {
                        lastOp = "REMW";
                        final RemoveWatchesRequest removeWatches = new RemoveWatchesRequest();
                        ByteBufferInputStream.byteBuffer2Record(request.request, (Record)removeWatches);
                        final Watcher.WatcherType type = Watcher.WatcherType.fromInt(removeWatches.getType());
                        final boolean removed = this.zks.getZKDatabase().removeWatch(removeWatches.getPath(), type, cnxn);
                        if (!removed) {
                            final String msg = String.format(Locale.ENGLISH, "%s (type: %s)", removeWatches.getPath(), type);
                            throw new KeeperException.NoWatcherException(msg);
                        }
                        break;
                    }
                }
            }
        }
        catch (KeeperException.SessionMovedException e5) {
            cnxn.sendCloseSession();
            return;
        }
        catch (KeeperException e) {
            err = e.code();
        }
        catch (Exception e2) {
            FinalRequestProcessor.LOG.error("Failed to process " + request, (Throwable)e2);
            final StringBuilder sb = new StringBuilder();
            final ByteBuffer bb = request.request;
            bb.rewind();
            while (bb.hasRemaining()) {
                sb.append(Integer.toHexString(bb.get() & 0xFF));
            }
            FinalRequestProcessor.LOG.error("Dumping request buffer: 0x" + sb.toString());
            err = KeeperException.Code.MARSHALLINGERROR;
        }
        final long lastZxid = this.zks.getZKDatabase().getDataTreeLastProcessedZxid();
        final ReplyHeader hdr2 = new ReplyHeader(request.cxid, lastZxid, err.intValue());
        this.zks.serverStats().updateLatency(request.createTime);
        cnxn.updateStatsForResponse(request.cxid, lastZxid, lastOp, request.createTime, Time.currentElapsedTime());
        try {
            cnxn.sendResponse(hdr2, rsp, "response");
            if (request.type == -11) {
                cnxn.sendCloseSession();
            }
        }
        catch (IOException e3) {
            FinalRequestProcessor.LOG.error("FIXMSG", (Throwable)e3);
        }
    }
    
    private boolean closeSession(final ServerCnxnFactory serverCnxnFactory, final long sessionId) {
        return serverCnxnFactory != null && serverCnxnFactory.closeSession(sessionId);
    }
    
    private boolean connClosedByClient(final Request request) {
        return request.cnxn == null;
    }
    
    @Override
    public void shutdown() {
        FinalRequestProcessor.LOG.info("shutdown of request processor complete");
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)FinalRequestProcessor.class);
    }
}
