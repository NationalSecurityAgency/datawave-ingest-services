package datawave.microservice.ingest.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@EnableConfigurationProperties(IngestProperties.class)
@ConfigurationProperties(prefix = "ingest")
public class IngestProperties {
    private List<String> fsConfigResources;
    
    public List<String> getFsConfigResources() {
        return fsConfigResources;
    }
    
    public void setFsConfigResources(List<String> fsConfigResources) {
        this.fsConfigResources = fsConfigResources;
    }
    
}
