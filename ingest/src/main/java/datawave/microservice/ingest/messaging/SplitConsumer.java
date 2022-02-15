package datawave.microservice.ingest.messaging;

import datawave.ingest.data.RawRecordContainer;
import datawave.ingest.mapreduce.ContextWrappedStatusReporter;
import datawave.ingest.mapreduce.EventMapper;
import datawave.ingest.mapreduce.job.BulkIngestKey;
import datawave.microservice.ingest.adapter.ManifestOutputFormat;
import datawave.microservice.ingest.configuration.IngestProperties;
import org.apache.accumulo.core.data.Value;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.Syncable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.map.WrappedMapper;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.task.MapContextImpl;
import org.apache.hadoop.mapreduce.task.TaskAttemptContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Configuration
@EnableConfigurationProperties(IngestProperties.class)
public class SplitConsumer {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    
    @Autowired
    private IngestProperties properties;
    
    @Bean
    public Consumer<Message<String>> splitSink() {
        return s -> {
            String message = s.getPayload();
            if (log.isTraceEnabled()) {
                log.trace("got message: " + message);
            }
            
            BasicInputMessage basicInputMessage = new BasicInputMessage(properties, getConf());
            basicInputMessage.setMessage(s.getPayload());
            RecordReader rr;
            EventMapper<LongWritable,RawRecordContainer,BulkIngestKey,Value> eventMapper = new EventMapper<>();
            SequenceFileOutputFormat outputFormat = new SequenceFileOutputFormat();
            try {
                rr = basicInputMessage.getRecordReader();
            } catch (IOException e) {
                // record the failure and throw an exception to prevent the ACK
                log.error("failed to get record reader", e);
                throw new RuntimeException(e);
            }
            
            if (rr != null) {
                FileSplit fileSplit = null;
                OutputCommitter committer = null;
                org.apache.hadoop.conf.Configuration conf = getConf();
                
                // get the header from the message to inform the task attempt #
                Object deliveryAttemptObj = s.getHeaders().get("deliveryAttempt");
                int attempt = 1;
                if (deliveryAttemptObj != null) {
                    attempt = ((AtomicInteger) deliveryAttemptObj).get();
                }
                
                TaskID taskId = null;
                TaskAttemptID taskAttemptId = null;
                TaskAttemptContext taskAttemptContext = null;
                
                try {
                    // set the override
                    conf.set("data.name.override", basicInputMessage.getDataName().trim());
                    fileSplit = (FileSplit) basicInputMessage.getSplit();
                    if (fileSplit == null) {
                        throw new IllegalStateException("File split null from input message: " + message);
                    }
                    
                    String messageUuid = s.getHeaders().getId().toString();
                    conf.set("mapreduce.output.basename", messageUuid);
                    taskId = new TaskID(new JobID(messageUuid, 1234), TaskType.MAP, attempt);
                    taskAttemptId = new TaskAttemptID(taskId, attempt);
                    taskAttemptContext = new TaskAttemptContextImpl(conf, taskAttemptId);
                    
                    RecordWriter recordWriter = outputFormat.getRecordWriter(taskAttemptContext);
                    committer = outputFormat.getOutputCommitter(taskAttemptContext);
                    rr.initialize(basicInputMessage.getSplit(), taskAttemptContext);
                    WrappedMapper wrappedMapper = new WrappedMapper();
                    Mapper.Context mapContext = wrappedMapper.getMapContext(new MapContextImpl(conf, taskAttemptId, rr, recordWriter, committer,
                                    new ContextWrappedStatusReporter(taskAttemptContext), basicInputMessage.getSplit()));
                    eventMapper.setup(mapContext);
                    while (rr.nextKeyValue()) {
                        // hand them off to the event mapper
                        log.trace("got next key/value pair");
                        eventMapper.map((LongWritable) rr.getCurrentKey(), (RawRecordContainer) rr.getCurrentValue(), mapContext);
                    }
                    
                    if (recordWriter instanceof Syncable) {
                        ((Syncable) recordWriter).hflush();
                    }
                    
                    // close the output first
                    recordWriter.close(taskAttemptContext);
                    
                    // finalize the output
                    committer.commitTask(taskAttemptContext);
                    createManifest(conf, messageUuid, attempt, fileSplit.getPath().toString());
                } catch (IOException | InterruptedException e) {
                    // throw new exception to prevent ACK
                    String fileName = "unknown file";
                    if (fileSplit != null) {
                        fileName = fileSplit.getPath().toString();
                    }
                    log.error("Unable to process split: " + fileName, e);
                    
                    throw new RuntimeException("Failed to process split: " + fileName);
                } finally {
                    if (committer != null) {
                        try {
                            // if there is anything that hasn't been committed abort it
                            committer.abortTask(taskAttemptContext);
                        } catch (IOException e) {
                            String fileName = "unknown file";
                            if (fileSplit != null) {
                                fileName = fileSplit.getPath().toString();
                            }
                            
                            log.warn("Failed to abort task for split: " + fileName, e);
                        }
                    }
                }
            }
        };
    }
    
    /**
     * fsConfigResources are applied in the order they are configured. Duplicate properties will be overridden by the last appearance
     * 
     * @return
     */
    @Bean
    public org.apache.hadoop.conf.Configuration getConf() {
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        if (properties != null) {
            for (String fsConfigResource : properties.getFsConfigResources()) {
                conf.addResource(new Path(fsConfigResource));
                log.info("Added resource: " + fsConfigResource);
            }
        } else {
            log.warn("Properties null!");
        }
        
        return conf;
    }
    
    private void createManifest(org.apache.hadoop.conf.Configuration baseConf, String uuid, int attempt, String filePath)
                    throws IOException, InterruptedException {
        FileOutputFormat<Text,Text> outputFormat = new ManifestOutputFormat<>();
        
        // create a copy of the conf
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration(baseConf);
        
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
