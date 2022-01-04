package datawave.microservice.ingest.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@EnableConfigurationProperties(IngestProperties.class)
@ConfigurationProperties(prefix = "ingest")
public class IngestProperties {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private List<String> fsConfigResources;
    private String workPath;
    private String numShards;
    private String shardTableName;
    
    public List<String> getFsConfigResources() {
        return fsConfigResources;
    }
    
    public void setFsConfigResources(List<String> fsConfigResources) {
        this.fsConfigResources = fsConfigResources;
        log.info("Got resources: " + fsConfigResources);
    }
    
    public String getWorkPath() {
        return workPath;
    }
    
    public void setWorkPath(String workPath) {
        this.workPath = workPath;
    }
    
    public String getNumShards() {
        return numShards;
    }
    
    public void setNumShards(String numShards) {
        this.numShards = numShards;
    }
    
    public String getShardTableName() {
        return shardTableName;
    }
    
    public void setShardTableName(String shardTableName) {
        this.shardTableName = shardTableName;
    }
}
