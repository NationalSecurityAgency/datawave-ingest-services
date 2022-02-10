package datawave.microservice.file;

import datawave.microservice.file.configuration.FileScannerProperties;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileScanner {
    protected Logger log = LoggerFactory.getLogger(this.getClass());
    
    protected FileScannerProperties properties;
    
    public FileScanner(FileScannerProperties properties) {
        this.properties = properties;
    }
    
    protected List<Path> workingFiles = new ArrayList<>();
    
    public void scanFiles() {
        log.warn("Scanning files! " + properties.getInputDir());
        try {
            org.apache.hadoop.conf.Configuration conf = getConf();
            Path inputDir = new Path(properties.getInputDir());
            
            FileSystem fs = FileSystem.get(inputDir.toUri(), conf);
            FileStatus dirStatus = fs.getFileStatus(inputDir);
            if (!dirStatus.isDirectory()) {
                log.error("input dir is not a directory: " + inputDir);
                throw new IllegalStateException("input dir is not a directory: " + inputDir);
            }
            
            RemoteIterator<LocatedFileStatus> files = fs.listFiles(inputDir, properties.isRecursive());
            long maxFiles = properties.getMaxFiles();
            long maxSize = properties.getMaxSize();
            long maxAge = properties.getMaxAge();
            long activeCount = 0;
            
            long workingSize = 0;
            
            boolean exceededAge = false;
            boolean exceededSize = false;
            
            while (activeCount < maxFiles && files.hasNext() && !exceededSize) {
                LocatedFileStatus fileStatus = files.next();
                // skip . files
                if (fileStatus.getPath().getName().startsWith(properties.getIgnorePrefix())) {
                    continue;
                }
                
                if (!preCheck(fileStatus)) {
                    log.info("File: " + fileStatus.getPath().toString() + " failed precheck");
                    continue;
                }
                
                // add the file and its manifest
                activeCount += addFiles(fileStatus);
                
                // only check if we care
                if (maxSize > -1) {
                    workingSize += fileStatus.getLen();
                }
                
                // test for file age
                if (maxAge > -1) {
                    long fileDate = fileStatus.getModificationTime();
                    if (System.currentTimeMillis() - fileDate > maxAge) {
                        exceededAge = true;
                        log.info("Exceeded age limit of : " + maxAge);
                    }
                }
                
                if (maxSize > -1 && workingSize >= maxSize) {
                    exceededSize = true;
                    log.info("Exceeded size limit at : " + workingSize);
                }
            }
            
            log.info("found: " + activeCount + " files");
            
            // check if we have enough files, or a file is old enough to trigger
            if (activeCount >= maxFiles || exceededAge || exceededSize) {
                log.info("hit threshold, processing files");
                process();
            }
        } catch (IOException e) {
            log.error("failed to process files", e);
        }
    }
    
    /**
     * Process all files, is responsible for cleaning up any processed files from workingFiles
     * 
     * @throws IOException
     */
    protected void process() throws IOException {
        throw new UnsupportedOperationException("should be implemented in subclass");
    }
    
    protected int addFiles(FileStatus fileStatus) {
        workingFiles.add(fileStatus.getPath());
        log.debug("Added " + fileStatus.getPath() + " to working files");
        
        return 1;
    }
    
    /**
     * Given a FileStatus return true if this file should be processed, false otherwise
     * 
     * @param toCheck
     * @return
     */
    protected boolean preCheck(FileStatus toCheck) {
        return true;
    }
    
    /**
     * fsConfigResources are applied in the order they are configured. Duplicate properties will be overridden by the last appearance
     *
     * @return
     */
    public org.apache.hadoop.conf.Configuration getConf() {
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        if (properties != null) {
            for (String fsConfigResource : properties.getFsConfigResources()) {
                conf.addResource(new Path(fsConfigResource));
                log.info("Added resource: " + fsConfigResource);
            }
        } else {
            log.warn("Properties null!");
        }
        
        return conf;
    }
}
