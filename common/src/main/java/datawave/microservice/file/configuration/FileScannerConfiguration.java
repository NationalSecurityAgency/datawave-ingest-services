package datawave.microservice.file.configuration;

import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expose the FileScannerProperties for auto wiring and create a 'conf' Configuration bean
 */
@Configuration
@EnableConfigurationProperties(FileScannerProperties.class)
public class FileScannerConfiguration {
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
