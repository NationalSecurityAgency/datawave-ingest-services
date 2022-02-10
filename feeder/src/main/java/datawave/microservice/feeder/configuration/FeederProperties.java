package datawave.microservice.feeder.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "feeder")
public class FeederProperties {
    private String targetDir;
    private String inputFormatClass;
    private String dataType;
    private boolean preservePath;
    
    public String getTargetDir() {
        return targetDir;
    }
    
    public void setTargetDir(String targetDir) {
        this.targetDir = targetDir;
    }
    
    public String getInputFormatClass() {
        return inputFormatClass;
    }
    
    public void setInputFormatClass(String inputFormatClass) {
        this.inputFormatClass = inputFormatClass;
    }
    
    public String getDataType() {
        return dataType;
    }
    
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
    
    public boolean isPreservePath() {
        return preservePath;
    }
    
    public void setPreservePath(boolean preservePath) {
        this.preservePath = preservePath;
    }
}
