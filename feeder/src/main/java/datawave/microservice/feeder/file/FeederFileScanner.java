package datawave.microservice.feeder.file;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import datawave.microservice.feeder.configuration.FeederProperties;
import datawave.microservice.feeder.messaging.MessageSupplier;
import datawave.microservice.file.FileScanner;
import datawave.microservice.file.configuration.FileScannerProperties;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Feeder will run on ${file.frequency} interval. Files from the inputDir will be scanned and moved to the targetDir, preserving path beyond the inputDir if
 * configured into the targetDir. Once successfully moved a message will be added to the rabbitmq exchange
 */
@Component
public class FeederFileScanner extends FileScanner {
    private final FeederProperties feederProperties;
    private final MessageSupplier feedSource;
    private Cache<Path,Boolean> failureCache;
    
    @Autowired
    public FeederFileScanner(Configuration conf, FileScannerProperties fileScannerProperties, FeederProperties feederProperties, MessageSupplier feedSource) {
        super(conf, fileScannerProperties);
        this.feederProperties = feederProperties;
        this.feedSource = feedSource;
    }
    
    @Override
    protected void process(Path workingFile) throws IOException {
        Path targetDir = new Path(feederProperties.getTargetDir());
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
        } else {
            log.warn("Failed to rename " + workingFile + " to " + fileTarget);
            throw new IOException("Failed to rename " + workingFile + " to " + fileTarget);
        }
    }
}
