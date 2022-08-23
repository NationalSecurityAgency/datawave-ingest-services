#The Bundler Service 
Monitor a directory for completed bundles of files. A complete bundle of files includes a given <filename> and <filename>.manifest file. Once reaching a threshold of complete bundles either by size, count, or age, the bundles will be merged.

Each <filename> is expected to be a SequenceFile<BulkIngestKey,Value>. They will be read one at a time and concatenated to the working directory. Once all files have been written into a single file in the working directory the file will be moved to the targetDirectory. If configured, the file will have the date added to the target directory.

Once the concatenated file has been successfully moved to the target directory, each of the manifests will be read and the files referenced in the manifests will have their file paths transformed by the configured values.

##Required configuration

###FileScannerProperties (prefix: file)
`file.inputDir` - Directory to monitor for new files

`file.frequency` - time in MS to scan file.inputDir for files to process

`file.ignorePrefix` - Files that match this prefix will not be processed

`file.maxAge` - (default -1) max age in MS before a file should be processed regardless of count or size

`file.maxSize` - (default -1) max size in bytes an aggregated set of files should be before they should be processed regardless of count or age

`file.maxFiles` - (default -1) max number of files before files should be processed regardless of age or size

`file.recursive` - (default false) if true recursively search inputDir for files

`file.errorRetryInterval` - (default 60000) interval that a file should not be retried if it resulted in a processing error

`file.errorRetryTimeUnit` - (default MILLISECONDS) default time unit for errorRetryInterval

`file.fsConfigResources` - List of files to be added to Configuration, applied in order. This should include hadoop core/site 

###BundlerProperties (prefix: bundler) 
`bundler.workDir` - the work dir where files will be aggregated

`bundler.bundleOutputDir` - final destination of all aggregated files

`bundler.dateBundleOutput` - if true dateFormat will be appended to the bundler.bundleOutputDir property with the current date for each file

`bundler.dateFormat` - SimpleDateFormat parsable format for the date to be applied if bundler.dateBundleOutput is true

`bundler.manifestPathToReplace` - the piece of the manifest path that should be replaced with bundler.manifestPathReplacement

`bundler.manifestPathReplacement` - replaces bundler.manifestPathToReplace before the referenced files in the manifest are moved

Failure to move a referenced manifest file will not cause a files processing to fail.