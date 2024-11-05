package datawave.microservice.file.configuration;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Properties to be used with the FileScanner
 */
@Validated
@ConfigurationProperties(prefix = "file")
public class FileScannerProperties {
    private List<String> fsConfigResources;
    @NotNull
    private String inputDir;
    private String ignorePrefix;
    private long maxAge = -1;
    private long maxFiles = -1;
    private long maxSize = -1;
    private boolean recursive = false;
    private long errorRetryInterval = 60000;
    private String errorRetryTimeUnit = "MILLISECONDS";
    
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
    
    public long getErrorRetryInterval() {
        return errorRetryInterval;
    }
    
    public void setErrorRetryInterval(long errorRetryInterval) {
        this.errorRetryInterval = errorRetryInterval;
    }
    
    public String getErrorRetryTimeUnit() {
        return errorRetryTimeUnit;
    }
    
    public void setErrorRetryTimeUnit(String errorRetryTimeUnit) {
        this.errorRetryTimeUnit = errorRetryTimeUnit;
    }
}
