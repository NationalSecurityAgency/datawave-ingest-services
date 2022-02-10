package datawave.microservice.bundler.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BundlerProperties.class)
public class BundlerConfiguration {
    
}
