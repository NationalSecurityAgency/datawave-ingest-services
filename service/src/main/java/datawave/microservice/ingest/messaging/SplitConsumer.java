package datawave.microservice.ingest.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@EnableConfigurationProperties(SplitConsumer.class)
public class SplitConsumer {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    
    @Bean
    public Consumer<String> splitSink() {
        return new Consumer<String>() {
            @Override
            public void accept(String s) {
                log.info("got message: " + s);
            }
        };
    }
}
