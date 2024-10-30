package datawave.microservice.feeder.messaging;

import datawave.microservice.feeder.configuration.FeederProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FeederProperties.class)
public class MessageProducer {
    @Bean
    public MessageSupplier feedSource() {
        return new MessageSupplier();
    }
}
