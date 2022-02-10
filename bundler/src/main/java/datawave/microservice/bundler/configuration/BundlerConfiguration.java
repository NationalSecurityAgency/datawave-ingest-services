package datawave.microservice.bundler.configuration;

import datawave.microservice.file.configuration.FileScannerProperties;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({FileScannerProperties.class, BundlerProperties.class})
public class BundlerConfiguration {
    @Autowired
    private FileScannerProperties fileScannerProperties;
    
    @Bean
    public org.apache.hadoop.conf.Configuration conf() {
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        if (fileScannerProperties != null && fileScannerProperties.getFsConfigResources() != null) {
            for (String fsConfigResource : fileScannerProperties.getFsConfigResources()) {
                conf.addResource(new Path(fsConfigResource));
            }
        }
        
        return conf;
    }
}
