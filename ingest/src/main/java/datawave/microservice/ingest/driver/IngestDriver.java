package datawave.microservice.ingest.driver;

import datawave.ingest.data.RawRecordContainer;
import datawave.ingest.data.config.ConfigurationHelper;
import datawave.ingest.data.config.ingest.AccumuloHelper;
import datawave.ingest.mapreduce.ContextWrappedStatusReporter;
import datawave.ingest.mapreduce.EventMapper;
import datawave.ingest.mapreduce.job.BulkIngestKey;
import datawave.ingest.mapreduce.job.CBMutationOutputFormatter;
import datawave.ingest.mapreduce.job.TableConfigurationUtil;
import datawave.ingest.mapreduce.job.reduce.BulkIngestKeyDedupeCombiner;
import datawave.ingest.mapreduce.job.writer.AggregatingContextWriter;
import datawave.ingest.mapreduce.job.writer.ChainedContextWriter;
import datawave.ingest.mapreduce.job.writer.ContextWriter;
import datawave.ingest.mapreduce.job.writer.DedupeContextWriter;
import datawave.ingest.mapreduce.job.writer.LiveContextWriter;
import datawave.ingest.mapreduce.job.writer.TableCachingContextWriter;
import datawave.microservice.config.accumulo.AccumuloProperties;
import datawave.microservice.ingest.adapter.ManifestOutputFormat;
import datawave.microservice.ingest.configuration.IngestProperties;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.client.Accumulo;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.Syncable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.OutputCommitter;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.TaskAttemptID;
import org.apache.hadoop.mapreduce.TaskID;
import org.apache.hadoop.mapreduce.TaskType;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.map.WrappedMapper;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.task.MapContextImpl;
import org.apache.hadoop.mapreduce.task.TaskAttemptContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Provides a wrapper for executing DATAWAVE ingest code outside the MapReduce environment. The class will translate the IngestProperties and Hadoop
 * Configuration to the appropriate MR configuration to execute the EventMapper generating either SequenceFiles or live ingesting directly to Accumulo.
 */
public class IngestDriver {
    /**
     * When specified overrides the output location, otherwise mapreduce.output.fileoutputformat.outputdir will be used
     */
    private static final String OUTPUT_DIR_SYSTEM_PROPERTY = "OUTPUT_DIR";
    
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private Configuration conf;
    private RecordReader rr;
    private IngestProperties properties;
    
    public IngestDriver(Configuration conf, RecordReader rr, IngestProperties properties) {
        this.conf = conf;
        this.rr = rr;
        this.properties = properties;
        
        init();
    }
    
    /**
     * Copied from IngestJob.interpolateEnvironment(Configuration)
     *
     * Replace all occurrences of DATAWAVE_INGEST_HOME with the specified environment in config
     *
     * @param conf
     * @return
     */
    private Configuration interpolateEnvironment(Configuration conf) {
        String ingestHomeValue = System.getenv("DATAWAVE_INGEST_HOME");
        if (null == ingestHomeValue) {
            throw new IllegalArgumentException("DATAWAVE_INGEST_HOME must be set in the environment.");
        } else {
            this.log.info("Replacing ${DATAWAVE_INGEST_HOME} with " + ingestHomeValue);
            return ConfigurationHelper.interpolate(conf, "\\$\\{DATAWAVE_INGEST_HOME\\}", ingestHomeValue);
        }
    }
    
    /**
     * Apply ACCUMULO_USER and ACCUMULO_PASSWORD to AccumuloProperties if they exist and interpolate DATAWAVE_INGEST_HOME
     */
    private void init() {
        conf = interpolateEnvironment(conf);
        
        String envUser = System.getenv("ACCUMULO_USER");
        if (envUser != null) {
            properties.getAccumuloProperties().setUsername(envUser);
        }
        String envPassword = System.getenv("ACCUMULO_PASSWORD");
        if (envPassword != null) {
            properties.getAccumuloProperties().setPassword(envPassword);
        }
        
        // setup required accumulo parameters
        AccumuloHelper.setUsername(conf, properties.getAccumuloProperties().getUsername());
        try {
            AccumuloHelper.setPassword(conf, properties.getAccumuloProperties().getPassword().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            log.error("Failed to set password encoding", e);
        }
        
        AccumuloHelper.setZooKeepers(conf, properties.getAccumuloProperties().getZookeepers());
        AccumuloHelper.setInstanceName(conf, properties.getAccumuloProperties().getInstanceName());
        
        // setup table names after accumulo parameters are set
        new TableConfigurationUtil(conf);
    }
    
    public void ingest(String uuid, int attempt, FileSplit split) throws IOException, InterruptedException {
        OutputCommitter committer = null;
        TaskAttemptContext taskAttemptContext = null;
        EventMapper eventMapper = new EventMapper();
        Mapper.Context mapContext = null;
        
        try {
            OutputFormat outputFormat = null;
            if (properties.isLiveIngest()) {
                log.info("Appling Live Ingest Settings");
                // ContextWriters expect the Mutation class to be set to work correctly
                Job job = Job.getInstance(conf);
                job.setOutputKeyClass(Text.class);
                // ingest code definitely checks this
                job.setOutputValueClass(Mutation.class);
                
                // order by the bulk ingest keys (this may not be necessary)
                job.setSortComparatorClass(BulkIngestKey.Comparator.class);
                
                // probably not necessary, but ingest code might check this somewhere :)
                job.setSpeculativeExecution(false);
                job.setReduceSpeculativeExecution(false);
                
                // always disable for now
                // TODO revist this
                job.getConfiguration().setBoolean(EventMapper.CONTEXT_WRITER_OUTPUT_TABLE_COUNTERS, false);
                
                job.getConfiguration().setBoolean(BulkIngestKeyDedupeCombiner.USING_COMBINER, true);
                job.getConfiguration().setClass(EventMapper.CONTEXT_WRITER_CLASS, DedupeContextWriter.class, ChainedContextWriter.class);
                job.getConfiguration().setClass(DedupeContextWriter.CONTEXT_WRITER_CLASS, TableCachingContextWriter.class, ContextWriter.class);
                log.info("Applying combiner settings");
                
                job.getConfiguration().setClass(TableCachingContextWriter.CONTEXT_WRITER_CLASS, AggregatingContextWriter.class, ContextWriter.class);
                job.getConfiguration().setClass(AggregatingContextWriter.CONTEXT_WRITER_CLASS, LiveContextWriter.class, ContextWriter.class);
                
                AccumuloProperties accumuloProperties = properties.getAccumuloProperties();
                log.info("accumulo instance: " + accumuloProperties.getInstanceName());
                log.info("accumulo zookeepers: " + accumuloProperties.getZookeepers());
                log.info("accumulo username: " + accumuloProperties.getUsername());
                
                CBMutationOutputFormatter.configure()
                                .clientProperties(Accumulo.newClientProperties().to(accumuloProperties.getInstanceName(), accumuloProperties.getZookeepers())
                                                .as(accumuloProperties.getUsername(),
                                                                new PasswordToken(accumuloProperties.getPassword().getBytes(StandardCharsets.UTF_8)))
                                                .build())
                                .createTables(true).store(job);
                job.setOutputFormatClass(CBMutationOutputFormatter.class);
                
                // get the conf out of the job and overwrite
                conf = job.getConfiguration();
                
                outputFormat = new CBMutationOutputFormatter();
            } else {
                outputFormat = new SequenceFileOutputFormat();
                
                // set the output location into the configuration
                conf.set("mapreduce.output.fileoutputformat.outputdir", System.getenv(OUTPUT_DIR_SYSTEM_PROPERTY),
                                conf.get("mapreduce.output.fileoutputformat.outputdir"));
            }
            
            // always set the key/value output
            conf.set("mapreduce.job.output.key.class", BulkIngestKey.class.getName());
            conf.set("mapreduce.job.output.value.class", Value.class.getName());
            
            TaskID taskId = new TaskID(new JobID(uuid, 1234), TaskType.MAP, attempt);
            TaskAttemptID taskAttemptId = new TaskAttemptID(taskId, attempt);
            taskAttemptContext = new TaskAttemptContextImpl(conf, taskAttemptId);
            
            RecordWriter recordWriter = outputFormat.getRecordWriter(taskAttemptContext);
            committer = outputFormat.getOutputCommitter(taskAttemptContext);
            rr.initialize(split, taskAttemptContext);
            WrappedMapper wrappedMapper = new WrappedMapper();
            mapContext = wrappedMapper.getMapContext(
                            new MapContextImpl(conf, taskAttemptId, rr, recordWriter, committer, new ContextWrappedStatusReporter(taskAttemptContext), split));
            eventMapper.setup(mapContext);
            while (rr.nextKeyValue()) {
                // hand them off to the event mapper
                log.trace("got next key/value pair");
                eventMapper.map(rr.getCurrentKey(), (RawRecordContainer) rr.getCurrentValue(), mapContext);
            }
            
            // write out metadata updates
            eventMapper.cleanup(mapContext);
            
            if (recordWriter instanceof Syncable) {
                ((Syncable) recordWriter).hflush();
            }
            
            // close the output first
            recordWriter.close(taskAttemptContext);
            
            // finalize the output
            committer.commitTask(taskAttemptContext);
            
            if (properties.isLiveIngest()) {
                // live ingest, move the file to loaded
                Path src = split.getPath();
                FileSystem fs = FileSystem.get(src.toUri(), conf);
                Path dst = new Path(src.toString().replaceFirst("/flagged/", "/loaded/"));
                boolean mkdirs = fs.mkdirs(dst.getParent());
                if (mkdirs) {
                    boolean renamed = fs.rename(src, dst);
                    if (!renamed) {
                        throw new IOException("Unable to rename " + src + " to " + dst);
                    }
                } else {
                    throw new IOException("Unable to create parent dir: " + dst.getParent());
                }
            } else {
                // not live ingest, create a manifest for follow on processing
                createManifest(conf, uuid, attempt, split.getPath().toString());
            }
        } catch (IOException | InterruptedException e) {
            String fileName = split.getPath().toString();
            log.error("Unable to process split: " + fileName, e);
            throw new RuntimeException("Failed to process split: " + fileName);
        } finally {
            if (committer != null) {
                try {
                    // if there is anything that hasn't been committed abort it
                    committer.abortTask(taskAttemptContext);
                } catch (IOException e) {
                    String fileName = split.getPath().toString();
                    log.warn("Failed to abort task for split: " + fileName, e);
                }
            }
        }
    }
    
    private void createManifest(Configuration baseConf, String uuid, int attempt, String filePath) throws IOException, InterruptedException {
        FileOutputFormat<Text,Text> outputFormat = new ManifestOutputFormat<>();
        
        // create a copy of the conf
        Configuration conf = new Configuration(baseConf);
        
        // override the name with the uuid
        conf.set("mapreduce.output.basename", uuid);
        
        TaskID taskId = new TaskID(new JobID(uuid, 1234), TaskType.MAP, attempt);
        TaskAttemptID taskAttemptId = new TaskAttemptID(taskId, attempt);
        TaskAttemptContext taskAttemptContext = new TaskAttemptContextImpl(conf, taskAttemptId);
        
        RecordWriter<Text,Text> recordWriter = outputFormat.getRecordWriter(taskAttemptContext);
        recordWriter.write(new Text(uuid), new Text(filePath));
        taskAttemptContext.progress();
        recordWriter.close(taskAttemptContext);
        OutputCommitter committer = outputFormat.getOutputCommitter(taskAttemptContext);
        committer.commitTask(taskAttemptContext);
    }
}
