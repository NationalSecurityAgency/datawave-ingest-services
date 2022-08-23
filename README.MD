Datawave Ingest Service is designed to layer on top of the existing ingest code to run ingest as a service in kubernetes. The service is built on Spring Boot to supply easy access to queues which will provide message chunks to be processed by ingest.

The datawave ingest microservices are broken down into these three systems

* Feeder service
* Ingest service
* Bundler service

These services will replace the existing flag maker, and ingest jobs.

Feeder service - monitors a directory for new files, file thresholds may be set to the number of files, size in bytes, or age of files. Files will be moved to a target directory and posted to a message topic. See [README](ingest/README.MD)

Ingest Service - listens to a message queue, reads files from the location specified in the message and writes output to a directory along with a matching manifest file which references the original file. See [README](ingest/README.MD)

Bundler Service - monitors a directory for sequence/manifest file pairs, based on file thresholds, size in bytes, or file age. Files are aggregated locally to a working directory, then pushed to a final target directory. All referenced manifest files are moved from their original location to a final destination by applying a find/replace based on configured properties. See [README](bundler/README.MD)

##Dependencies
* Docker (20.10.12+)
* Datawave (3.10.1)

##Setup
Recommended to use an existing K8s environment. See [README](charts/README.MD)

##Building

#### Build Datawave
```bash
git clone git@github.com:NationalSecurityAgency/datawave.git
cd datawave
git checkout 3.10.1
mvn clean install -DskipTests
```

### Build Ingeset Microservices
```bash
mvn clean install -Pdocker
```

##Deploying
### Configuration
Feeder - See [README](feeder/README.MD)

Ingest - See [README](ingest/README.MD)

Bundler - See [README](bundler/README.MD)

### Deploying helm charts
```bash
helm install ingest charts/
```