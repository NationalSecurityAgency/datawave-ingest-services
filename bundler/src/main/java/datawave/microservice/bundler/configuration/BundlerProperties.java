package datawave.microservice.bundler.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "bundler")
public class BundlerProperties {
    private String workDir;
    private String bundleOutputDir;
    
    private boolean preservePath;
    private String manifestPathRoot;
    private String manifestOutputDir;
    
    public String getWorkDir() {
        return workDir;
    }
    
    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }
    
    public String getBundleOutputDir() {
        return bundleOutputDir;
    }
    
    public void setBundleOutputDir(String bundleOutputDir) {
        this.bundleOutputDir = bundleOutputDir;
    }
    
    public String getManifestOutputDir() {
        return manifestOutputDir;
    }
    
    public void setManifestOutputDir(String manifestOutputDir) {
        this.manifestOutputDir = manifestOutputDir;
    }
    
    public String getManifestPathRoot() {
        return manifestPathRoot;
    }
    
    public void setManifestPathRoot(String manifestPathRoot) {
        this.manifestPathRoot = manifestPathRoot;
    }
    
    public boolean isPreservePath() {
        return preservePath;
    }
    
    public void setPreservePath(boolean preservePath) {
        this.preservePath = preservePath;
    }
}
