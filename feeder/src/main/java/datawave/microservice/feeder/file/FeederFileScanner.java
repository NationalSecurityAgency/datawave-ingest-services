package datawave.microservice.feeder.file;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import datawave.microservice.feeder.configuration.FeederProperties;
import datawave.microservice.feeder.messaging.MessageSupplier;
import datawave.microservice.file.FileScanner;
import datawave.microservice.file.configuration.FileScannerProperties;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class FeederFileScanner extends FileScanner {
    private final FeederProperties feederProperties;
    private final MessageSupplier feedSource;
    
    @Autowired
    public FeederFileScanner(Configuration conf, FileScannerProperties fileScannerProperties, FeederProperties feederProperties, MessageSupplier feedSource) {
        super(conf, fileScannerProperties);
        this.feederProperties = feederProperties;
        this.feedSource = feedSource;
    }
    
    @Override
    protected void process() throws IOException {
        Path targetDir = new Path(feederProperties.getTargetDir());
        List<Path> processedFiles = new ArrayList<>(workingFiles.size());
        try {
            for (Path workingFile : workingFiles) {
                log.info("would have sent message: " + workingFile + "," + feederProperties.getInputFormatClass() + "," + feederProperties.getDataType());
                
                Path fileTarget = new Path(targetDir, workingFile.getName());
                if (feederProperties.isPreservePath()) {
                    int startIndex = workingFile.toString().indexOf(properties.getInputDir());
                    int endIndex = startIndex + properties.getInputDir().length();
                    
                    if (startIndex > -1) {
                        String preservedPath = workingFile.toString().substring(endIndex + 1);
                        fileTarget = new Path(targetDir, preservedPath);
                    }
                }
                
                log.info("Moving file: " + workingFile + " to: " + fileTarget);
                FileSystem fs = FileSystem.get(workingFile.toUri(), conf);
                Path parentDir = fileTarget.getParent();
                if (!fs.exists(parentDir)) {
                    if (!fs.mkdirs(parentDir)) {
                        log.warn("Failed to create " + parentDir);
                        throw new IOException("Failed to create " + parentDir);
                    }
                }
                
                if (fs.rename(workingFile, fileTarget)) {
                    feedSource.send(MessageBuilder.withPayload(fileTarget + "," + feederProperties.getInputFormatClass() + "," + feederProperties.getDataType())
                                    .build());
                    processedFiles.add(workingFile);
                } else {
                    log.warn("Failed to rename " + workingFile + " to " + fileTarget);
                    throw new IOException("Failed to rename " + workingFile + " to " + fileTarget);
                }
            }
        } finally {
            workingFiles.removeAll(processedFiles);
        }
    }
    
    protected int addFiles(FileStatus fileStatus) {
        workingFiles.add(fileStatus.getPath());
        log.debug("Added " + fileStatus.getPath() + " to working files");
        
        return 1;
    }
    
    protected boolean preCheck(FileStatus toCheck) {
        return true;
    }
}
