package datawave.microservice.ingest.configuration;

import java.util.List;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import datawave.microservice.config.accumulo.AccumuloProperties;

@Validated
@EnableConfigurationProperties(IngestProperties.class)
@ConfigurationProperties(prefix = "ingest")
public class IngestProperties {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private List<String> fsConfigResources;
    private List<String> resourceDirPatterns;
    
    @NotNull
    private boolean liveIngest;
    
    @NotNull
    private AccumuloProperties accumuloProperties;
    
    public List<String> getFsConfigResources() {
        return fsConfigResources;
    }
    
    public void setFsConfigResources(List<String> fsConfigResources) {
        this.fsConfigResources = fsConfigResources;
        log.info("Got resources: " + fsConfigResources);
    }
    
    public List<String> getResourceDirPatterns() {
        return resourceDirPatterns;
    }
    
    public void setResourceDirPatterns(List<String> resourceDirPatterns) {
        this.resourceDirPatterns = resourceDirPatterns;
    }
    
    public AccumuloProperties getAccumuloProperties() {
        return accumuloProperties;
    }
    
    public void setAccumuloProperties(AccumuloProperties accumuloProperties) {
        this.accumuloProperties = accumuloProperties;
    }
    
    public boolean isLiveIngest() {
        return liveIngest;
    }
    
    public void setLiveIngest(boolean liveIngest) {
        this.liveIngest = liveIngest;
    }
}
