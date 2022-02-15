package datawave.microservice.ingest.messaging;

import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;

import java.io.IOException;

public interface InputMessage {
    InputSplit getSplit() throws IOException;
    
    String getDataName() throws IOException;
    
    RecordReader getRecordReader() throws IOException;
}
