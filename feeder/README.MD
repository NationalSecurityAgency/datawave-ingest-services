Feeder Service - monitors a directory for new files, file thresholds may be set to the number of files, size in bytes, or age of files. Files will be moved to a target directory and posted to a message topic.

One service per directory to be monitored. Each directory will have a single associated dataType and InputFormatClass

Required configuration

FileScannerProperties (prefix: file)
file.inputDir - Directory to monitor for new files
file.frequency - time in MS to scan file.inputDir for files to process
file.ignorePrefix - Files that match this prefix will not be processed
file.maxAge - (default -1) max age in MS before a file should be processed regardless of count or size
file.maxSize - (default -1) max size in bytes an aggregated set of files should be before they should be processed regardless of count or age
file.maxFiles - (default -1) max number of files before files should be processed regardless of age or size
file.recursive - (default false) if true recursively search inputDir for files
file.errorRetryInterval - (default 60000) interval that a file should not be retried if it resulted in a processing error
file.errorRetryTimeUnit - (default MILLISECONDS) default time unit for errorRetryInterval
file.fsConfigResources - List of files to be added to Configuration, applied in order. This should include hadoop core/site 

FeederProperties (prefix: feeder)
feeder.targetDir - location to move files once they have been fed
feeder.inputFormatClass - expected class of files the feeder is monitoring in file.inputDir
feeder.dataType - expected dataType of files the feeder is monitoring in file.inputDir
feeder.preservePath - if true, any part of the path beyond the file.inputDir will be preserved when moving the file to feeder.targetDir

Additional Configuration (Spring Boot)
spring.cloud.stream.bindings.feedSource-out-0.destination - target exchange for messages

environment:
DEST_QUEUE - environment variable, defaults to ingest. May override to direct feeder messages to another queue