package datawave.microservice.feeder.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FeederProperties.class)
public class FeederConfiguration {}
