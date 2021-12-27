package datawave.microservice.ingest.messaging;

import datawave.microservice.ingest.configuration.IngestProperties;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.task.TaskAttemptContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@org.springframework.context.annotation.Configuration
public class BasicInputMessage implements InputMessage {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private IngestProperties properties;
    
    private String message;
    
    private InputSplit split;
    private RecordReader recordReader;
    
    public BasicInputMessage(IngestProperties properties) {
        this.properties = properties;
    }
    
    @Override
    public InputSplit getSplit() throws IOException {
        if (split == null) {
            parse(message);
        }
        
        return split;
    }
    
    @Override
    public RecordReader getRecordReader() throws IOException {
        if (recordReader == null) {
            parse(message);
        }
        return recordReader;
    }
    
    /**
     * Basic message format filePath,InputFormatClass
     * 
     * @param message
     */
    private void parse(String message) throws IOException {
        String[] splits = message.split(",");
        if (splits.length != 2) {
            throw new IllegalArgumentException("Unexpected message format, should be filePath,InputFormatClass. Got " + message);
        }
        
        Path filePath = new Path(splits[0]);
        log.info("got file path: " + filePath);
        log.info("got input format class: " + splits[1]);
        
        Configuration conf = new Configuration();
        if (properties != null) {
            for (String fsConfigResource : properties.getFsConfigResources()) {
                conf.addResource(new Path(fsConfigResource));
                log.info("Added resource: " + fsConfigResource);
            }
        } else {
            log.info("Properties null!");
        }
        
        FileSystem fs = FileSystem.get(filePath.toUri(), conf);
        FileStatus fileStatus = fs.getFileStatus(filePath);
        long fileLen = fileStatus.getLen();
        
        split = new FileSplit(filePath, 0, fileLen, null);
        
        Class<? extends InputFormat> inputFormatClazz = null;
        try {
            inputFormatClazz = Class.forName(splits[1]).asSubclass(InputFormat.class);
            InputFormat inputFormat = inputFormatClazz.newInstance();
            
            // TODO something better here
            TaskAttemptContextImpl context = new TaskAttemptContextImpl(conf, new TaskAttemptID());
            recordReader = inputFormat.createRecordReader(split, context);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            log.error("Could not instantiate input format: " + splits[1], e);
        } catch (InterruptedException e) {
            log.error("Could not instantiate record reader", e);
        }
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
