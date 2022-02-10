package datawave.microservice.file.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "file")
public class FileScannerProperties {
    private List<String> fsConfigResources;
    
    private String inputDir;
    
    private String ignorePrefix;
    private long maxAge;
    private long maxFiles;
    private long maxSize;
    
    private boolean recursive;
    
    public String getInputDir() {
        return inputDir;
    }
    
    public void setInputDir(String inputDir) {
        this.inputDir = inputDir;
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
    
    public String getIgnorePrefix() {
        return ignorePrefix;
    }
    
    public void setIgnorePrefix(String ignorePrefix) {
        this.ignorePrefix = ignorePrefix;
    }
    
    public List<String> getFsConfigResources() {
        return fsConfigResources;
    }
    
    public void setFsConfigResources(List<String> fsConfigResources) {
        this.fsConfigResources = fsConfigResources;
    }
    
    public boolean isRecursive() {
        return recursive;
    }
    
    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }
}
