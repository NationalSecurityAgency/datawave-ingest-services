package datawave.microservice.bundler;

import datawave.ingest.mapreduce.job.BulkIngestKey;
import datawave.microservice.bundler.configuration.BundlerProperties;
import org.apache.accumulo.core.data.Value;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Writable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Configuration
@EnableConfigurationProperties(BundlerProperties.class)
@Component
public class FileScanner {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    
    @Autowired
    private BundlerProperties properties;
    
    @Scheduled(fixedRate = 5000)
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
            
            RemoteIterator<LocatedFileStatus> files = fs.listFiles(inputDir, false);
            long maxFiles = properties.getMaxFiles();
            long maxSize = properties.getMaxSize();
            long maxAge = properties.getMaxAge();
            long activeCount = 0;
            
            long workingSize = 0;
            List<Path> workingFiles = new ArrayList<>();
            List<Path> workingManifests = new ArrayList<>();
            
            boolean exceededAge = false;
            boolean exceededSize = false;
            
            while (activeCount < maxFiles && files.hasNext() && !exceededSize) {
                LocatedFileStatus fileStatus = files.next();
                // skip . files
                if (fileStatus.getPath().getName().startsWith(".")) {
                    continue;
                }
                
                // skip manifest files
                if (!fileStatus.getPath().toString().endsWith(".manifest")) {
                    // find the matching manifest file and add it as well
                    Path manifestPath = new Path(inputDir, fileStatus.getPath().getName() + ".manifest");
                    FileStatus manifestStatus = fs.getFileStatus(manifestPath);
                    if (manifestStatus == null) {
                        log.warn("No matching manifest file for : " + fileStatus.getPath() + " expected: " + manifestPath + " to exist.");
                        continue;
                    }
                    
                    // add the file and its manifest
                    activeCount++;
                    workingFiles.add(fileStatus.getPath());
                    log.debug("Added " + fileStatus.getPath() + " to working files");
                    workingManifests.add(manifestPath);
                    log.debug("Added " + manifestPath + " to working manifests");
                    
                    workingSize += fileStatus.getLen();
                    
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
            }
            
            log.info("found: " + activeCount + " files");
            
            // check if we have enough files, or a file is old enough to trigger
            if (activeCount >= maxFiles || exceededAge || exceededSize) {
                log.info("combining");
                String uuid = combine(fs, new Path(properties.getWorkDir()), workingFiles, workingManifests);
                log.info("moving");
                move(new Path(properties.getWorkDir()), new Path(properties.getOutputDir()), uuid);
                log.info("cleanup");
                cleanup(fs, workingFiles, workingManifests);
            }
        } catch (IOException e) {
            log.error("failed to process files", e);
        }
    }
    
    private void cleanup(FileSystem fs, List<Path> workingFiles, List<Path> workingManifests) {
        for (Path workingFile : workingFiles) {
            try {
                fs.delete(workingFile, false);
            } catch (IOException e) {
                // TODO
                e.printStackTrace();
            }
        }
        
        for (Path workingManifest : workingManifests) {
            try {
                fs.delete(workingManifest, false);
            } catch (IOException e) {
                // TODO
                e.printStackTrace();
            }
        }
    }
    
    private void move(Path workingDir, Path outputDir, String uuid) {
        try {
            // TODO compute this once
            FileSystem fs = FileSystem.get(outputDir.toUri(), getConf());
            fs.moveFromLocalFile(new Path(workingDir, uuid), new Path(outputDir, uuid));
            fs.moveFromLocalFile(new Path(workingDir, uuid + ".manifest"), new Path(outputDir, uuid + ".manifest"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to move combined files", e);
        }
        
    }
    
    private String combine(FileSystem fs, Path workingDir, List<Path> workingFiles, List<Path> workingManifests) {
        String uuid = UUID.randomUUID().toString();
        Path combinedFile = new Path(workingDir, uuid);
        Path combinedManifestFile = new Path(workingDir, uuid + ".manifest");
        
        // create the output files
        SequenceFile.Writer writer = null;
        try {
            // TODO configurable?
            writer = SequenceFile.createWriter(getConf(), SequenceFile.Writer.file(combinedFile), SequenceFile.Writer.keyClass(BulkIngestKey.class),
                            SequenceFile.Writer.valueClass(Value.class));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create sequence file", e);
        }
        
        for (Path workingFile : workingFiles) {
            try {
                SequenceFile.Reader reader = new SequenceFile.Reader(getConf(), SequenceFile.Reader.file(workingFile));
                Class keyClass = reader.getKeyClass();
                Class valueClass = reader.getValueClass();
                Writable key = (Writable) keyClass.newInstance();
                Writable value = (Writable) valueClass.newInstance();
                while (reader.next(key, value)) {
                    writer.append(key, value);
                }
                // TODO finally
                reader.close();
            } catch (IOException | IllegalAccessException | InstantiationException e) {
                throw new RuntimeException("Failed to merge sequence file: " + workingFile, e);
            }
        }
        
        try {
            // TODO finally
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to close sequence file: " + combinedFile, e);
        }
        
        FSDataOutputStream outputStream = null;
        BufferedWriter bw = null;
        try {
            outputStream = fs.create(combinedManifestFile);
            bw = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create merged manifest: " + combinedManifestFile, e);
        }
        
        // merge up the manifests
        for (Path workingManifest : workingManifests) {
            try {
                FSDataInputStream inputStream = fs.open(workingManifest);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                String line = reader.readLine();
                bw.write(line);
                bw.write("\n");
                
                // TODO finally
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to merge manifest file: " + workingManifest, e);
            }
        }
        
        try {
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to close writer", e);
        }
        
        return uuid;
    }
    
    /**
     * fsConfigResources are applied in the order they are configured. Duplicate properties will be overridden by the last appearance
     *
     * @return
     */
    @Bean
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
