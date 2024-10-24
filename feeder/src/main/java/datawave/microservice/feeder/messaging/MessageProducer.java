package datawave.microservice.feeder.messaging;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import datawave.microservice.feeder.configuration.FeederProperties;

@Configuration
@EnableConfigurationProperties(FeederProperties.class)
public class MessageProducer {
    @Bean
    public MessageSupplier feedSource() {
        return new MessageSupplier();
    }
}
