package datawave.microservice.ingest.messaging;

import datawave.ingest.data.RawRecordContainer;
import datawave.ingest.mapreduce.ContextWrappedStatusReporter;
import datawave.ingest.mapreduce.EventMapper;
import datawave.ingest.mapreduce.job.BulkIngestKey;
import datawave.ingest.mapreduce.job.MultiRFileOutputFormatter;
import datawave.microservice.ingest.configuration.IngestProperties;
import org.apache.accumulo.core.data.Value;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.map.WrappedMapper;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat;
import org.apache.hadoop.mapreduce.task.MapContextImpl;
import org.apache.hadoop.mapreduce.task.TaskAttemptContextImpl;
import org.jboss.resteasy.annotations.providers.jaxb.Wrapped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.function.Consumer;

@Configuration
@EnableConfigurationProperties(IngestProperties.class)

public class SplitConsumer {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    
    @Autowired
    private IngestProperties properties;
    
    @Bean
    public Consumer<String> splitSink() {
        return s -> {
            log.info("got message: " + s);
            
            BasicInputMessage basicInputMessage = new BasicInputMessage(properties, getConf());
            basicInputMessage.setMessage(s);
            RecordReader rr = null;
            EventMapper<LongWritable,RawRecordContainer,BulkIngestKey,Value> eventMapper = new EventMapper<>();
            MapFileOutputFormat outputFormat = new MapFileOutputFormat();
            try {
                rr = basicInputMessage.getRecordReader();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            if (rr != null) {
                try {
                    // set the override
                    org.apache.hadoop.conf.Configuration conf = getConf();
                    conf.set("data.name.override", basicInputMessage.getDataName());
                    
                    TaskAttemptContext taskAttemptContext = new TaskAttemptContextImpl(conf, new TaskAttemptID());
                    RecordWriter recordWriter = outputFormat.getRecordWriter(taskAttemptContext);
                    OutputCommitter committer = outputFormat.getOutputCommitter(taskAttemptContext);
                    rr.initialize(basicInputMessage.getSplit(), taskAttemptContext);
                    WrappedMapper wrappedMapper = new WrappedMapper();
                    Mapper.Context mapContext = wrappedMapper.getMapContext(new MapContextImpl(conf, new TaskAttemptID(), rr, recordWriter, committer,
                                    new ContextWrappedStatusReporter(taskAttemptContext), basicInputMessage.getSplit()));
                    eventMapper.setup(mapContext);
                    while (rr.nextKeyValue()) {
                        // hand them off to the event mapper
                        log.info("got next key/value pair");
                        eventMapper.map((LongWritable) rr.getCurrentKey(), (RawRecordContainer) rr.getCurrentValue(), mapContext);
                        // TODO
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
        };
    }
    
    @Bean
    public org.apache.hadoop.conf.Configuration getConf() {
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        if (properties != null) {
            for (String fsConfigResource : properties.getFsConfigResources()) {
                conf.addResource(new Path(fsConfigResource));
                log.info("Added resource: " + fsConfigResource);
            }
            
            conf.set("mapreduce.output.fileoutputformat.outputdir", properties.getWorkPath());
            conf.set("num.shards", properties.getNumShards());
            conf.set("shard.table.name", properties.getShardTableName());
        } else {
            log.info("Properties null!");
        }
        
        return conf;
    }
}
