package datawave.microservice.ingest.messaging;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import datawave.microservice.ingest.configuration.IngestProperties;
import datawave.microservice.ingest.driver.IngestDriver;

/**
 * Responsible for processing messages off of the configured rabbitmq topic and kicking off the configured ingest job for each file. The uuid, attempts, and
 * message will be used to create a FileSplit and passed to an IngestDriver to process.
 */
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
                log.trace("got message: {}", message);
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
                    
                    log.warn("Failed to process ingest for split: {}", fileName, e);
                }
                long duration = System.currentTimeMillis() - start;
                log.info("Completed uuid: {} attempt: {} file: {} in {} ms", messageUuid, attempt, fileSplit.getPath(), duration);
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
        List<Pattern> dirPatterns = new ArrayList<>();
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        if (properties != null) {
            for (String fsConfigResource : properties.getFsConfigResources()) {
                File f = new File(fsConfigResource);
                if (f.isFile()) {
                    conf.addResource(new Path(fsConfigResource));
                    log.info("Added resource: {}", fsConfigResource);
                } else if (f.isDirectory()) {
                    if (properties.getResourceDirPatterns() != null) {
                        for (String patternString : properties.getResourceDirPatterns()) {
                            dirPatterns.add(Pattern.compile(patternString));
                        }
                    }
                    log.info("Adding resource directory: {}", fsConfigResource);
                    for (File child : f.listFiles()) {
                        if (!child.isDirectory() && child.exists()) {
                            boolean matches = dirPatterns.isEmpty();
                            
                            for (Pattern p : dirPatterns) {
                                if (p.matcher(child.toString()).matches()) {
                                    matches = true;
                                }
                            }
                            
                            if (matches) {
                                conf.addResource(new Path(child.toString()));
                                log.info("Adding resource directory file: {}", child);
                            }
                        }
                    }
                }
            }
        } else {
            log.warn("Properties null!");
        }
        
        return conf;
    }
}
