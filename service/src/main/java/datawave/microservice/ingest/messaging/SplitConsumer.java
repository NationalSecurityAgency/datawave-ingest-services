package datawave.microservice.ingest.messaging;

import datawave.ingest.data.RawRecordContainer;
import datawave.ingest.mapreduce.ContextWrappedStatusReporter;
import datawave.ingest.mapreduce.EventMapper;
import datawave.ingest.mapreduce.job.BulkIngestKey;
import datawave.microservice.ingest.configuration.IngestProperties;
import org.apache.accumulo.core.data.Value;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.map.WrappedMapper;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
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
                
                TaskID taskId = new TaskID();
                TaskAttemptID taskAttemptId = new TaskAttemptID(taskId, attempt);
                TaskAttemptContext taskAttemptContext = new TaskAttemptContextImpl(conf, taskAttemptId);
                
                try {
                    // set the override
                    conf.set("data.name.override", basicInputMessage.getDataName());
                    fileSplit = (FileSplit) basicInputMessage.getSplit();
                    if (fileSplit == null) {
                        throw new IllegalStateException("File split null from input message: " + message);
                    }
                    
                    conf.set("mapreduce.output.basename", fileSplit.getPath().getName());
                    
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
                    
                    // finalize the output
                    committer.commitTask(taskAttemptContext);
                } catch (IOException | InterruptedException e) {
                    // throw new exception to prevent ACK
                    log.error("Unable to process split: " + fileSplit.getPath().toString(), e);
                    throw new RuntimeException("Failed to process split: " + fileSplit.getPath().toString());
                } finally {
                    if (committer != null) {
                        try {
                            committer.abortTask(taskAttemptContext);
                        } catch (IOException e) {
                            log.warn("Failed to abort task for split: " + fileSplit.getPath().toString(), e);
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
}
