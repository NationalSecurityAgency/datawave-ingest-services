package datawave.microservice.ingest.messaging;

import datawave.ingest.mapreduce.job.CBMutationOutputFormatter;
import datawave.microservice.ingest.configuration.IngestProperties;
import datawave.microservice.ingest.driver.IngestDriver;
import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
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
                
                String messageUuid = s.getHeaders().getId().toString();
                long start = System.currentTimeMillis();
                try {
                    // set the override
                    conf.set("data.name.override", basicInputMessage.getDataName().trim());
                    fileSplit = (FileSplit) basicInputMessage.getSplit();
                    if (fileSplit == null) {
                        throw new IllegalStateException("File split null from input message: " + message);
                    }
                    
                    // get the message uuid and use that as the output name to avoid collisions
                    conf.set("mapreduce.output.basename", messageUuid);
                    
                    IngestDriver driver = new IngestDriver(conf, rr, properties);
                    driver.ingest(messageUuid, attempt, fileSplit);
                } catch (IOException | InterruptedException e) {
                    String fileName = "unknown file";
                    if (fileSplit != null) {
                        fileName = fileSplit.getPath().toString();
                    }
                    
                    log.warn("Failed to process ingest for split: " + fileName, e);
                }
                long duration = System.currentTimeMillis() - start;
                log.info("Completed uuid:" + messageUuid + " attempt: " + attempt + " file: " + fileSplit.getPath() + " in " + duration + " ms");
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
