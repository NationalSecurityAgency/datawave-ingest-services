Ingest service monitors a message queue for files to process through Datawave Ingest. It will wrap each file up in a Map Reduce context and execute the EventMapper against it. The resulting SequenceFile<BulkIngestKey,Value> will be written to disk along with a matching .manifest file which corresponds to the original input file.

Required Configuration (prefix: ingest)
ingest.fsConfigResources - List of files to be added to Configuration, applied in order. This should include hadoop core/site. This should also include any DATAWAVE ingest config files. Additionally this must include mapreduce.output.fileoutputformat.outputdir, mapreduce.job.output.key.class, and mapreduce.job.output.value.class

Required config in fsConfigResources - 
All DATAWAVE ingest conf - See DATAWAVE ingest for full details
mapreduce.output.fileoutputformat.outputdir - the output directory to final work done by the ingest service. After a file is successfully processed the output Sequence file and manifest file will be written here.
mapreduce.job.output.key.class - datawave.ingest.mapreduce.job.BulkIngestKey
mapreduce.job.output.value.class - org.apache.accumulo.core.data.Value

Additional Config (Spring Boot)
spring.cloud.stream.rabbit.bindings.splitSink-in-0.consumer.autoBindDlq = true
spring.cloud.stream.bindings.splitSink-in-0.destination - name of the exchange to fetch messages from
spring.cloud.stream.bindings.splitSink-in-0.group - name of the group within the exchange to fetch messages from

If a file fails to process the message will not be ack'd and it will be sent to the DLQ if enabled. This may be controlled with configuration.
spring.cloud.stream.bindings.splitSink-in-0.consumer.maxAttempts - max retries
spring.cloud.stream.bindings.splitSink-in-0.consumer.concurrency - max concurrent processing threads

Files will be written as SequenceFile<BulkIngestKey,Value> to the specified output directory. Files will be written 1:1 with input files. A corresponding .manifest file will also be written that maintains a mapping from the uuid to the original input name.