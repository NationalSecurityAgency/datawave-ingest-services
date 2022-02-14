package datawave.microservice.bundler.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "bundler")
public class BundlerProperties {
    private String workDir;
    private String bundleOutputDir;
    private boolean dateBundleOutput = true;
    private String dateFormat = "yyyy/MM/dd";
    
    private boolean preservePath;
    
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
    
    public boolean isPreservePath() {
        return preservePath;
    }
    
    public void setPreservePath(boolean preservePath) {
        this.preservePath = preservePath;
    }
    
    public boolean isDateBundleOutput() {
        return dateBundleOutput;
    }
    
    public void setDateBundleOutput(boolean dateBundleOutput) {
        this.dateBundleOutput = dateBundleOutput;
    }
    
    public String getDateFormat() {
        return dateFormat;
    }
    
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
}
