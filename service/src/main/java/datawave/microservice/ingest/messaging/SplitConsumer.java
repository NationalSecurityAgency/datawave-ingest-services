package datawave.microservice.ingest.messaging;

import datawave.microservice.ingest.configuration.IngestProperties;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptID;
import org.apache.hadoop.mapreduce.task.TaskAttemptContextImpl;
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
            
            BasicInputMessage basicInputMessage = new BasicInputMessage(properties);
            basicInputMessage.setMessage(s);
            RecordReader rr = null;
            try {
                rr = basicInputMessage.getRecordReader();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            if (rr != null) {
                try {
                    // TODO
                    rr.initialize(basicInputMessage.getSplit(), new TaskAttemptContextImpl(new org.apache.hadoop.conf.Configuration(), new TaskAttemptID()));
                    while (rr.nextKeyValue()) {
                        // hand them off to the event mapper
                        log.info("got next key/value pair");
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
}
