package datawave.microservice.file;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import datawave.microservice.file.configuration.FileScannerProperties;

/**
 * Base implementation to scan a directory and process found files based on maxFiles, maxSize, and maxAge.
 * <p>
 * inputFiles - the path to scan for files ignorePrefix - if specified files starting with ignorePrefix won't be processed maxFiles - if greater than -1 will
 * cap the number of files per scanFiles to maxFiles, unlimited otherwise maxSize - if greater than -1 max size of aggregated files to process per scanFiles
 * iteration, unlimited otherwise maxAge - if greater than -1 max age of a file before encountering it will trigger an immediate end scan, unlimited otherwise
 * </p>
 */
public abstract class FileScanner {
    protected Logger log = LoggerFactory.getLogger(this.getClass());
    
    protected Configuration conf;
    protected FileScannerProperties properties;
    
    private Cache<String,Boolean> failureCache;
    
    public FileScanner(Configuration conf, FileScannerProperties properties) {
        this.conf = conf;
        this.properties = properties;
        
        failureCache = CacheBuilder.newBuilder().expireAfterWrite(properties.getErrorRetryInterval(), TimeUnit.valueOf(properties.getErrorRetryTimeUnit()))
                        .build();
    }
    
    protected List<Path> workingFiles = new ArrayList<>();
    
    @Scheduled(fixedRateString = "${file.frequency:5000}")
    public void scanFiles() {
        log.warn("Scanning files! " + properties.getInputDir());
        try {
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
            boolean exceededFileCount = false;
            
            while (!exceededFileCount && files.hasNext() && !exceededSize) {
                LocatedFileStatus fileStatus = files.next();
                
                // always test against the failure cache first, if it is still in the cache, skip it
                if (failureCache.getIfPresent(fileStatus.getPath().toString()) != null) {
                    log.debug("Skipping cached failed file: " + fileStatus.getPath());
                    continue;
                }
                
                // skip files that should be ignored
                if (fileStatus.getPath().getName().startsWith(properties.getIgnorePrefix())) {
                    continue;
                }
                
                if (!preCheck(fileStatus)) {
                    log.info("File: " + fileStatus.getPath().toString() + " failed precheck");
                    continue;
                }
                
                // add the file and its manifest
                activeCount += addFiles(fileStatus);
                
                if (maxFiles > -1 && activeCount >= maxFiles) {
                    exceededFileCount = true;
                }
                
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
        } finally {
            try {
                cleanup();
            } catch (IOException e) {
                log.warn("Failed to cleanup working files", e);
            }
        }
    }
    
    /**
     * Process all files, any that fail to process should be added to the failure cache, but not prevent processing other files
     * 
     * @throws IOException
     */
    protected void process() throws IOException {
        // make a copy so cleanup can happen within the loop
        List<Path> toProcess = new ArrayList<>(workingFiles);
        for (Path workingFile : toProcess) {
            try {
                process(workingFile);
            } catch (Throwable t) {
                // put the workingFile in the failure queue and go again
                processFailure(workingFile);
                log.warn("Failed to process file: " + workingFile, t);
            } finally {
                cleanup(workingFile);
            }
        }
    }
    
    /**
     * Add a Path to the failure cache to prevent this file from being reprocessed until it expires from the cache
     * 
     * @param workingFile
     */
    protected void processFailure(Path workingFile) {
        failureCache.put(workingFile.toString(), true);
    }
    
    /**
     * Process an individual file
     * 
     * @param workingFile
     * @throws IOException
     */
    protected void process(Path workingFile) throws IOException {
        // no-op
    }
    
    /**
     * Cleanup after processing a workingFile
     * 
     * @param workingFile
     */
    protected void cleanup(Path workingFile) {
        workingFiles.remove(workingFile);
    }
    
    /**
     * Cleanup associated with processing a set of working files
     * 
     * @throws IOException
     */
    protected void cleanup() throws IOException {
        workingFiles.clear();
    }
    
    /**
     * Add a file for processing
     * 
     * @param fileStatus
     * @return number of files added for processing
     */
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
}
