// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.beam.sdk.io.mongodb;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.client.model.InsertManyOptions;
import java.util.Map;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PDone;
import java.util.NoSuchElementException;
import org.apache.beam.sdk.io.Source;
import com.mongodb.client.AggregateIterable;
import org.bson.BsonInt32;
import org.bson.BsonValue;
import org.bson.BsonString;
import org.bson.types.ObjectId;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import com.mongodb.client.model.Aggregates;
import java.util.Iterator;
import org.bson.BsonDocument;
import com.mongodb.client.model.Filters;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import com.mongodb.client.MongoDatabase;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.vendor.guava.v26_0_jre.com.google.common.annotations.VisibleForTesting;
import org.apache.beam.sdk.values.POutput;
import org.apache.beam.sdk.values.PInput;
import org.apache.beam.sdk.transforms.display.DisplayData;
import org.apache.beam.sdk.io.BoundedSource;
import org.apache.beam.sdk.io.Read;
import java.util.Arrays;
import org.bson.conversions.Bson;
import org.apache.beam.vendor.guava.v26_0_jre.com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import com.google.auto.value.AutoValue;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.transforms.PTransform;
import org.slf4j.LoggerFactory;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import com.mongodb.client.MongoCollection;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.slf4j.Logger;
import org.apache.beam.sdk.annotations.Experimental;

@Experimental(Experimental.Kind.SOURCE_SINK)
public class MongoDbIO
{
    private static final Logger LOG;
    
    public static Read read() {
        return new AutoValue_MongoDbIO_Read.Builder().setMaxConnectionIdleTime(60000).setNumSplits(0).setBucketAuto(false).setSslEnabled(false).setIgnoreSSLCertificate(false).setSslInvalidHostNameAllowed(false).setQueryFn((SerializableFunction<MongoCollection<Document>, MongoCursor<Document>>)FindQuery.create()).build();
    }
    
    public static Write write() {
        return new AutoValue_MongoDbIO_Write.Builder().setMaxConnectionIdleTime(60000).setBatchSize(1024L).setSslEnabled(false).setIgnoreSSLCertificate(false).setSslInvalidHostNameAllowed(false).setOrdered(true).build();
    }
    
    private MongoDbIO() {
    }
    
    private static MongoClientOptions.Builder getOptions(final int maxConnectionIdleTime, final boolean sslEnabled, final boolean sslInvalidHostNameAllowed) {
        final MongoClientOptions.Builder optionsBuilder = new MongoClientOptions.Builder();
        optionsBuilder.maxConnectionIdleTime(maxConnectionIdleTime);
        if (sslEnabled) {
            optionsBuilder.sslEnabled(sslEnabled).sslInvalidHostNameAllowed(sslInvalidHostNameAllowed).sslContext(SSLUtils.ignoreSSLCertificate());
        }
        return optionsBuilder;
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)MongoDbIO.class);
    }
    
    @AutoValue
    public abstract static class Read extends PTransform<PBegin, PCollection<Document>>
    {
        @Nullable
        abstract String uri();
        
        abstract int maxConnectionIdleTime();
        
        abstract boolean sslEnabled();
        
        abstract boolean sslInvalidHostNameAllowed();
        
        abstract boolean ignoreSSLCertificate();
        
        @Nullable
        abstract String database();
        
        @Nullable
        abstract String collection();
        
        abstract int numSplits();
        
        abstract boolean bucketAuto();
        
        abstract SerializableFunction<MongoCollection<Document>, MongoCursor<Document>> queryFn();
        
        abstract Builder builder();
        
        public Read withUri(final String uri) {
            Preconditions.checkArgument(uri != null, (Object)"MongoDbIO.read().withUri(uri) called with null uri");
            return this.builder().setUri(uri).build();
        }
        
        public Read withMaxConnectionIdleTime(final int maxConnectionIdleTime) {
            return this.builder().setMaxConnectionIdleTime(maxConnectionIdleTime).build();
        }
        
        public Read withSSLEnabled(final boolean sslEnabled) {
            return this.builder().setSslEnabled(sslEnabled).build();
        }
        
        public Read withSSLInvalidHostNameAllowed(final boolean invalidHostNameAllowed) {
            return this.builder().setSslInvalidHostNameAllowed(invalidHostNameAllowed).build();
        }
        
        public Read withIgnoreSSLCertificate(final boolean ignoreSSLCertificate) {
            return this.builder().setIgnoreSSLCertificate(ignoreSSLCertificate).build();
        }
        
        public Read withDatabase(final String database) {
            Preconditions.checkArgument(database != null, (Object)"database can not be null");
            return this.builder().setDatabase(database).build();
        }
        
        public Read withCollection(final String collection) {
            Preconditions.checkArgument(collection != null, (Object)"collection can not be null");
            return this.builder().setCollection(collection).build();
        }
        
        @Deprecated
        public Read withFilter(final String filter) {
            Preconditions.checkArgument(filter != null, (Object)"filter can not be null");
            Preconditions.checkArgument(this.queryFn().getClass() != FindQuery.class, (Object)"withFilter is only supported for FindQuery API");
            final FindQuery findQuery = (FindQuery)this.queryFn();
            final FindQuery queryWithFilter = findQuery.toBuilder().setFilters(FindQuery.bson2BsonDocument((Bson)Document.parse(filter))).build();
            return this.builder().setQueryFn((SerializableFunction<MongoCollection<Document>, MongoCursor<Document>>)queryWithFilter).build();
        }
        
        @Deprecated
        public Read withProjection(final String... fieldNames) {
            Preconditions.checkArgument(fieldNames.length > 0, (Object)"projection can not be null");
            Preconditions.checkArgument(this.queryFn().getClass() != FindQuery.class, (Object)"withFilter is only supported for FindQuery API");
            final FindQuery findQuery = (FindQuery)this.queryFn();
            final FindQuery queryWithProjection = findQuery.toBuilder().setProjection(Arrays.asList(fieldNames)).build();
            return this.builder().setQueryFn((SerializableFunction<MongoCollection<Document>, MongoCursor<Document>>)queryWithProjection).build();
        }
        
        public Read withNumSplits(final int numSplits) {
            Preconditions.checkArgument(numSplits >= 0, "invalid num_splits: must be >= 0, but was %s", numSplits);
            return this.builder().setNumSplits(numSplits).build();
        }
        
        public Read withBucketAuto(final boolean bucketAuto) {
            return this.builder().setBucketAuto(bucketAuto).build();
        }
        
        public Read withQueryFn(final SerializableFunction<MongoCollection<Document>, MongoCursor<Document>> queryBuilderFn) {
            return this.builder().setQueryFn(queryBuilderFn).build();
        }
        
        public PCollection<Document> expand(final PBegin input) {
            Preconditions.checkArgument(this.uri() != null, (Object)"withUri() is required");
            Preconditions.checkArgument(this.database() != null, (Object)"withDatabase() is required");
            Preconditions.checkArgument(this.collection() != null, (Object)"withCollection() is required");
            return (PCollection<Document>)input.apply((PTransform)org.apache.beam.sdk.io.Read.from((BoundedSource)new BoundedMongoDbSource(this)));
        }
        
        public void populateDisplayData(final DisplayData.Builder builder) {
            super.populateDisplayData(builder);
            builder.add(DisplayData.item("uri", this.uri()));
            builder.add(DisplayData.item("maxConnectionIdleTime", Integer.valueOf(this.maxConnectionIdleTime())));
            builder.add(DisplayData.item("sslEnabled", Boolean.valueOf(this.sslEnabled())));
            builder.add(DisplayData.item("sslInvalidHostNameAllowed", Boolean.valueOf(this.sslInvalidHostNameAllowed())));
            builder.add(DisplayData.item("ignoreSSLCertificate", Boolean.valueOf(this.ignoreSSLCertificate())));
            builder.add(DisplayData.item("database", this.database()));
            builder.add(DisplayData.item("collection", this.collection()));
            builder.add(DisplayData.item("numSplit", Integer.valueOf(this.numSplits())));
            builder.add(DisplayData.item("bucketAuto", Boolean.valueOf(this.bucketAuto())));
            builder.add(DisplayData.item("queryFn", this.queryFn().toString()));
        }
        
        @AutoValue.Builder
        abstract static class Builder
        {
            abstract Builder setUri(final String uri);
            
            abstract Builder setMaxConnectionIdleTime(final int maxConnectionIdleTime);
            
            abstract Builder setSslEnabled(final boolean value);
            
            abstract Builder setSslInvalidHostNameAllowed(final boolean value);
            
            abstract Builder setIgnoreSSLCertificate(final boolean value);
            
            abstract Builder setDatabase(final String database);
            
            abstract Builder setCollection(final String collection);
            
            abstract Builder setNumSplits(final int numSplits);
            
            abstract Builder setBucketAuto(final boolean bucketAuto);
            
            abstract Builder setQueryFn(final SerializableFunction<MongoCollection<Document>, MongoCursor<Document>> queryBuilder);
            
            abstract Read build();
        }
    }
    
    @VisibleForTesting
    static class BoundedMongoDbSource extends BoundedSource<Document>
    {
        private final Read spec;
        
        private BoundedMongoDbSource(final Read spec) {
            this.spec = spec;
        }
        
        public Coder<Document> getOutputCoder() {
            return (Coder<Document>)SerializableCoder.of((Class)Document.class);
        }
        
        public void populateDisplayData(final DisplayData.Builder builder) {
            this.spec.populateDisplayData(builder);
        }
        
        public BoundedSource.BoundedReader<Document> createReader(final PipelineOptions options) {
            return new BoundedMongoDbReader(this);
        }
        
        public long getEstimatedSizeBytes(final PipelineOptions pipelineOptions) {
            final MongoClient mongoClient = new MongoClient(new MongoClientURI(this.spec.uri(), getOptions(this.spec.maxConnectionIdleTime(), this.spec.sslEnabled(), this.spec.sslInvalidHostNameAllowed())));
            Throwable x0 = null;
            try {
                return this.getEstimatedSizeBytes(mongoClient, this.spec.database(), this.spec.collection());
            }
            catch (Throwable t) {
                x0 = t;
                throw t;
            }
            finally {
                $closeResource(x0, (AutoCloseable)mongoClient);
            }
        }
        
        private long getEstimatedSizeBytes(final MongoClient mongoClient, final String database, final String collection) {
            final MongoDatabase mongoDatabase = mongoClient.getDatabase(database);
            final BasicDBObject stat = new BasicDBObject();
            stat.append("collStats", (Object)collection);
            final Document stats = mongoDatabase.runCommand((Bson)stat);
            return ((Number)stats.get((Object)"size", (Class)Number.class)).longValue();
        }
        
        public List<BoundedSource<Document>> split(long desiredBundleSizeBytes, final PipelineOptions options) {
            final MongoClient mongoClient = new MongoClient(new MongoClientURI(this.spec.uri(), getOptions(this.spec.maxConnectionIdleTime(), this.spec.sslEnabled(), this.spec.sslInvalidHostNameAllowed())));
            Throwable x0 = null;
            try {
                final MongoDatabase mongoDatabase = mongoClient.getDatabase(this.spec.database());
                final List<BoundedSource<Document>> sources = new ArrayList<BoundedSource<Document>>();
                if (this.spec.queryFn().getClass() == AutoValue_FindQuery.class) {
                    List<Document> splitKeys;
                    if (this.spec.bucketAuto()) {
                        splitKeys = buildAutoBuckets(mongoDatabase, this.spec);
                    }
                    else {
                        if (this.spec.numSplits() > 0) {
                            final long estimatedSizeBytes = this.getEstimatedSizeBytes(mongoClient, this.spec.database(), this.spec.collection());
                            desiredBundleSizeBytes = estimatedSizeBytes / this.spec.numSplits();
                        }
                        if (desiredBundleSizeBytes < 1048576L) {
                            desiredBundleSizeBytes = 1048576L;
                        }
                        final BasicDBObject splitVectorCommand = new BasicDBObject();
                        splitVectorCommand.append("splitVector", (Object)(this.spec.database() + "." + this.spec.collection()));
                        splitVectorCommand.append("keyPattern", (Object)new BasicDBObject().append("_id", (Object)1));
                        splitVectorCommand.append("force", (Object)false);
                        MongoDbIO.LOG.debug("Splitting in chunk of {} MB", (Object)(desiredBundleSizeBytes / 1024L / 1024L));
                        splitVectorCommand.append("maxChunkSize", (Object)(desiredBundleSizeBytes / 1024L / 1024L));
                        final Document splitVectorCommandResult = mongoDatabase.runCommand((Bson)splitVectorCommand);
                        splitKeys = (List<Document>)splitVectorCommandResult.get((Object)"splitKeys");
                    }
                    if (splitKeys.size() < 1) {
                        MongoDbIO.LOG.debug("Split keys is low, using an unique source");
                        return (List<BoundedSource<Document>>)Collections.singletonList(this);
                    }
                    for (final String shardFilter : splitKeysToFilters(splitKeys)) {
                        final SerializableFunction<MongoCollection<Document>, MongoCursor<Document>> queryFn = this.spec.queryFn();
                        final BsonDocument filters = FindQuery.bson2BsonDocument((Bson)Document.parse(shardFilter));
                        final FindQuery findQuery = (FindQuery)queryFn;
                        final BsonDocument allFilters = FindQuery.bson2BsonDocument((Bson)((findQuery.filters() != null) ? Filters.and(new Bson[] { (Bson)findQuery.filters(), (Bson)filters }) : filters));
                        final FindQuery queryWithFilter = findQuery.toBuilder().setFilters(allFilters).build();
                        MongoDbIO.LOG.debug("using filters: " + allFilters.toJson());
                        sources.add(new BoundedMongoDbSource(this.spec.withQueryFn((SerializableFunction<MongoCollection<Document>, MongoCursor<Document>>)queryWithFilter)));
                    }
                }
                else {
                    final SerializableFunction<MongoCollection<Document>, MongoCursor<Document>> queryFn2 = this.spec.queryFn();
                    final AggregationQuery aggregationQuery = (AggregationQuery)queryFn2;
                    if (aggregationQuery.mongoDbPipeline().stream().anyMatch(s -> s.keySet().contains("$limit"))) {
                        return (List<BoundedSource<Document>>)Collections.singletonList(this);
                    }
                    final List<Document> splitKeys = buildAutoBuckets(mongoDatabase, this.spec);
                    for (final BsonDocument shardFilter2 : splitKeysToMatch(splitKeys)) {
                        final AggregationQuery queryWithBucket = aggregationQuery.toBuilder().setBucket(shardFilter2).build();
                        sources.add(new BoundedMongoDbSource(this.spec.withQueryFn((SerializableFunction<MongoCollection<Document>, MongoCursor<Document>>)queryWithBucket)));
                    }
                }
                return sources;
            }
            catch (Throwable t) {
                x0 = t;
                throw t;
            }
            finally {
                $closeResource(x0, (AutoCloseable)mongoClient);
            }
        }
        
        @VisibleForTesting
        static List<String> splitKeysToFilters(final List<Document> splitKeys) {
            final ArrayList<String> filters = new ArrayList<String>();
            String lowestBound = null;
            for (int i = 0; i < splitKeys.size(); ++i) {
                final String splitKey = splitKeys.get(i).get((Object)"_id").toString();
                if (i == 0) {
                    String rangeFilter = String.format("{ $and: [ {\"_id\":{$lte:ObjectId(\"%s\")}}", splitKey);
                    filters.add(String.format("%s ]}", rangeFilter));
                    if (splitKeys.size() == 1) {
                        rangeFilter = String.format("{ $and: [ {\"_id\":{$gt:ObjectId(\"%s\")}}", splitKey);
                        filters.add(String.format("%s ]}", rangeFilter));
                    }
                }
                else if (i == splitKeys.size() - 1) {
                    String rangeFilter = String.format("{ $and: [ {\"_id\":{$gt:ObjectId(\"%s\"),$lte:ObjectId(\"%s\")}}", lowestBound, splitKey);
                    filters.add(String.format("%s ]}", rangeFilter));
                    rangeFilter = String.format("{ $and: [ {\"_id\":{$gt:ObjectId(\"%s\")}}", splitKey);
                    filters.add(String.format("%s ]}", rangeFilter));
                }
                else {
                    final String rangeFilter = String.format("{ $and: [ {\"_id\":{$gt:ObjectId(\"%s\"),$lte:ObjectId(\"%s\")}}", lowestBound, splitKey);
                    filters.add(String.format("%s ]}", rangeFilter));
                }
                lowestBound = splitKey;
            }
            return filters;
        }
        
        @VisibleForTesting
        static List<BsonDocument> splitKeysToMatch(final List<Document> splitKeys) {
            final List<Bson> aggregates = new ArrayList<Bson>();
            ObjectId lowestBound = null;
            for (int i = 0; i < splitKeys.size(); ++i) {
                final ObjectId splitKey = splitKeys.get(i).getObjectId((Object)"_id");
                if (i == 0) {
                    aggregates.add(Aggregates.match(Filters.lte("_id", (Object)splitKey)));
                    if (splitKeys.size() == 1) {
                        aggregates.add(Aggregates.match(Filters.and(new Bson[] { Filters.gt("_id", (Object)splitKey) })));
                    }
                }
                else if (i == splitKeys.size() - 1) {
                    aggregates.add(Aggregates.match(Filters.and(new Bson[] { Filters.gt("_id", (Object)lowestBound), Filters.lte("_id", (Object)splitKey) })));
                    aggregates.add(Aggregates.match(Filters.and(new Bson[] { Filters.gt("_id", (Object)splitKey) })));
                }
                else {
                    aggregates.add(Aggregates.match(Filters.and(new Bson[] { Filters.gt("_id", (Object)lowestBound), Filters.lte("_id", (Object)splitKey) })));
                }
                lowestBound = splitKey;
            }
            return aggregates.stream().map(s -> s.toBsonDocument((Class)BasicDBObject.class, MongoClient.getDefaultCodecRegistry())).collect((Collector<? super Object, ?, List<BsonDocument>>)Collectors.toList());
        }
        
        @VisibleForTesting
        static List<Document> buildAutoBuckets(final MongoDatabase mongoDatabase, final Read spec) {
            final List<Document> splitKeys = new ArrayList<Document>();
            final MongoCollection<Document> mongoCollection = (MongoCollection<Document>)mongoDatabase.getCollection(spec.collection());
            final BsonDocument bucketAutoConfig = new BsonDocument();
            bucketAutoConfig.put("groupBy", (BsonValue)new BsonString("$_id"));
            bucketAutoConfig.put("buckets", (BsonValue)new BsonInt32((spec.numSplits() > 0) ? spec.numSplits() : 10));
            final BsonDocument bucketAuto = new BsonDocument("$bucketAuto", (BsonValue)bucketAutoConfig);
            final List<BsonDocument> aggregates = new ArrayList<BsonDocument>();
            aggregates.add(bucketAuto);
            final AggregateIterable<Document> buckets = (AggregateIterable<Document>)mongoCollection.aggregate((List)aggregates);
            for (final Document bucket : buckets) {
                final Document filter = new Document();
                filter.put("_id", ((Document)bucket.get((Object)"_id")).get((Object)"min"));
                splitKeys.add(filter);
            }
            return splitKeys;
        }
        
        private static /* synthetic */ void $closeResource(final Throwable x0, final AutoCloseable x1) {
            if (x0 != null) {
                try {
                    x1.close();
                }
                catch (Throwable t) {
                    x0.addSuppressed(t);
                }
            }
            else {
                x1.close();
            }
        }
    }
    
    private static class BoundedMongoDbReader extends BoundedSource.BoundedReader<Document>
    {
        private final BoundedMongoDbSource source;
        private MongoClient client;
        private MongoCursor<Document> cursor;
        private Document current;
        
        BoundedMongoDbReader(final BoundedMongoDbSource source) {
            this.source = source;
        }
        
        public boolean start() {
            final Read spec = this.source.spec;
            this.client = this.createClient(spec);
            final MongoDatabase mongoDatabase = this.client.getDatabase(spec.database());
            final MongoCollection<Document> mongoCollection = (MongoCollection<Document>)mongoDatabase.getCollection(spec.collection());
            this.cursor = (MongoCursor<Document>)spec.queryFn().apply((Object)mongoCollection);
            return this.advance();
        }
        
        public boolean advance() {
            if (this.cursor.hasNext()) {
                this.current = (Document)this.cursor.next();
                return true;
            }
            return false;
        }
        
        public BoundedMongoDbSource getCurrentSource() {
            return this.source;
        }
        
        public Document getCurrent() {
            return this.current;
        }
        
        public void close() {
            try {
                if (this.cursor != null) {
                    this.cursor.close();
                }
            }
            catch (Exception e) {
                MongoDbIO.LOG.warn("Error closing MongoDB cursor", (Throwable)e);
            }
            try {
                this.client.close();
            }
            catch (Exception e) {
                MongoDbIO.LOG.warn("Error closing MongoDB client", (Throwable)e);
            }
        }
        
        private MongoClient createClient(final Read spec) {
            return new MongoClient(new MongoClientURI(spec.uri(), getOptions(spec.maxConnectionIdleTime(), spec.sslEnabled(), spec.sslInvalidHostNameAllowed())));
        }
    }
    
    @AutoValue
    public abstract static class Write extends PTransform<PCollection<Document>, PDone>
    {
        @Nullable
        abstract String uri();
        
        abstract int maxConnectionIdleTime();
        
        abstract boolean sslEnabled();
        
        abstract boolean sslInvalidHostNameAllowed();
        
        abstract boolean ignoreSSLCertificate();
        
        abstract boolean ordered();
        
        @Nullable
        abstract String database();
        
        @Nullable
        abstract String collection();
        
        abstract long batchSize();
        
        abstract Builder builder();
        
        public Write withUri(final String uri) {
            Preconditions.checkArgument(uri != null, (Object)"uri can not be null");
            return this.builder().setUri(uri).build();
        }
        
        public Write withMaxConnectionIdleTime(final int maxConnectionIdleTime) {
            return this.builder().setMaxConnectionIdleTime(maxConnectionIdleTime).build();
        }
        
        public Write withSSLEnabled(final boolean sslEnabled) {
            return this.builder().setSslEnabled(sslEnabled).build();
        }
        
        public Write withSSLInvalidHostNameAllowed(final boolean invalidHostNameAllowed) {
            return this.builder().setSslInvalidHostNameAllowed(invalidHostNameAllowed).build();
        }
        
        public Write withOrdered(final boolean ordered) {
            return this.builder().setOrdered(ordered).build();
        }
        
        public Write withIgnoreSSLCertificate(final boolean ignoreSSLCertificate) {
            return this.builder().setIgnoreSSLCertificate(ignoreSSLCertificate).build();
        }
        
        public Write withDatabase(final String database) {
            Preconditions.checkArgument(database != null, (Object)"database can not be null");
            return this.builder().setDatabase(database).build();
        }
        
        public Write withCollection(final String collection) {
            Preconditions.checkArgument(collection != null, (Object)"collection can not be null");
            return this.builder().setCollection(collection).build();
        }
        
        public Write withBatchSize(final long batchSize) {
            Preconditions.checkArgument(batchSize >= 0L, "Batch size must be >= 0, but was %s", batchSize);
            return this.builder().setBatchSize(batchSize).build();
        }
        
        public PDone expand(final PCollection<Document> input) {
            Preconditions.checkArgument(this.uri() != null, (Object)"withUri() is required");
            Preconditions.checkArgument(this.database() != null, (Object)"withDatabase() is required");
            Preconditions.checkArgument(this.collection() != null, (Object)"withCollection() is required");
            input.apply((PTransform)ParDo.of((DoFn)new WriteFn(this)));
            return PDone.in(input.getPipeline());
        }
        
        public void populateDisplayData(final DisplayData.Builder builder) {
            builder.add(DisplayData.item("uri", this.uri()));
            builder.add(DisplayData.item("maxConnectionIdleTime", Integer.valueOf(this.maxConnectionIdleTime())));
            builder.add(DisplayData.item("sslEnable", Boolean.valueOf(this.sslEnabled())));
            builder.add(DisplayData.item("sslInvalidHostNameAllowed", Boolean.valueOf(this.sslInvalidHostNameAllowed())));
            builder.add(DisplayData.item("ignoreSSLCertificate", Boolean.valueOf(this.ignoreSSLCertificate())));
            builder.add(DisplayData.item("ordered", Boolean.valueOf(this.ordered())));
            builder.add(DisplayData.item("database", this.database()));
            builder.add(DisplayData.item("collection", this.collection()));
            builder.add(DisplayData.item("batchSize", Long.valueOf(this.batchSize())));
        }
        
        @AutoValue.Builder
        abstract static class Builder
        {
            abstract Builder setUri(final String uri);
            
            abstract Builder setMaxConnectionIdleTime(final int maxConnectionIdleTime);
            
            abstract Builder setSslEnabled(final boolean value);
            
            abstract Builder setSslInvalidHostNameAllowed(final boolean value);
            
            abstract Builder setIgnoreSSLCertificate(final boolean value);
            
            abstract Builder setOrdered(final boolean value);
            
            abstract Builder setDatabase(final String database);
            
            abstract Builder setCollection(final String collection);
            
            abstract Builder setBatchSize(final long batchSize);
            
            abstract Write build();
        }
        
        static class WriteFn extends DoFn<Document, Void>
        {
            private final Write spec;
            private transient MongoClient client;
            private List<Document> batch;
            
            WriteFn(final Write spec) {
                this.spec = spec;
            }
            
            @DoFn.Setup
            public void createMongoClient() {
                this.client = new MongoClient(new MongoClientURI(this.spec.uri(), getOptions(this.spec.maxConnectionIdleTime(), this.spec.sslEnabled(), this.spec.sslInvalidHostNameAllowed())));
            }
            
            @DoFn.StartBundle
            public void startBundle() {
                this.batch = new ArrayList<Document>();
            }
            
            @DoFn.ProcessElement
            public void processElement(final DoFn.ProcessContext ctx) {
                this.batch.add(new Document((Map)ctx.element()));
                if (this.batch.size() >= this.spec.batchSize()) {
                    this.flush();
                }
            }
            
            @DoFn.FinishBundle
            public void finishBundle() {
                this.flush();
            }
            
            private void flush() {
                if (this.batch.isEmpty()) {
                    return;
                }
                final MongoDatabase mongoDatabase = this.client.getDatabase(this.spec.database());
                final MongoCollection<Document> mongoCollection = (MongoCollection<Document>)mongoDatabase.getCollection(this.spec.collection());
                try {
                    mongoCollection.insertMany((List)this.batch, new InsertManyOptions().ordered(this.spec.ordered()));
                }
                catch (MongoBulkWriteException e) {
                    if (this.spec.ordered()) {
                        throw e;
                    }
                }
                this.batch.clear();
            }
            
            @DoFn.Teardown
            public void closeMongoClient() {
                this.client.close();
                this.client = null;
            }
        }
    }
}
