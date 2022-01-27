package datawave.microservice.bundler.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@EnableConfigurationProperties(BundlerProperties.class)
@ConfigurationProperties(prefix = "bundler")
public class BundlerProperties {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private List<String> fsConfigResources;
    private String inputDir;
    private String workDir;
    private String outputDir;
    private long maxAge;
    private long maxFiles;
    private long maxSize;
    
    public List<String> getFsConfigResources() {
        return fsConfigResources;
    }
    
    public void setFsConfigResources(List<String> fsConfigResources) {
        this.fsConfigResources = fsConfigResources;
        log.info("Got resources: " + fsConfigResources);
    }
    
    public String getInputDir() {
        return inputDir;
    }
    
    public void setInputDir(String inputDir) {
        this.inputDir = inputDir;
    }
    
    public String getWorkDir() {
        return workDir;
    }
    
    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }
    
    public String getOutputDir() {
        return outputDir;
    }
    
    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }
    
    public long getMaxAge() {
        return maxAge;
    }
    
    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }
    
    public long getMaxFiles() {
        return maxFiles;
    }
    
    public void setMaxFiles(long maxFiles) {
        this.maxFiles = maxFiles;
    }
    
    public long getMaxSize() {
        return maxSize;
    }
    
    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }
}
