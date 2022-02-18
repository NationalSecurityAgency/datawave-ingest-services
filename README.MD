Datawave Ingest Service is designed to layer on top of the existing ingest code to run ingest as a service in kubernetes. The service is built on Spring Boot to supply easy access to queues which will provide message chunks to be processed by ingest.

The datawave ingest microservices are broken down into these three systems

1. Feeder service
2. Ingest service
3. Bundler service

These services will replace the existing flag maker, and ingest jobs.

Feeder service - monitors a directory for new files, file thresholds may be set to the number of files, size in bytes, or age of files. Files will be moved to a target directory and posted to a message topic.

Ingest Service - listens to a message queue, reads files from the location specified in the message and writes output to a directory along with a matching manifest file which references the original file.

Bundler Service - monitors a directory for sequence/manifest file pairs, based on file thresholds, size in bytes, or file age. Files are aggregated locally to a working directory, then pushed to a final target directory. All referenced manifest files are moved from their original location to a final destination by applying a find/replace based on configured properties.

Dependencies:
Docker (20.10.12+)
docker-compose (1.29.2+)
Datawave (feature/queryMicroservices) branch
Datawave (3.10.1)
Hadoop HDFS


Setup:
Under the queryMicroservices branch navigate to query-microservices/docker/ copy all files and directories to another location (eg. /srv/data/datawave/). 
Copy from datawave-ingest-service/docker/docker-compose.yml and overwrite the existing file in <working-dir>/query-microservices/docker/
Copy datawave-ingest-service/docker/config/* to <working-dir>/query-microservices/docker/config/


Config:
Sample config is available in datawave-ingest-service/docker/ingest-config. This directory can be copied into the docker compose folder, <working-dir>/query-microservices/docker/

At minimum your config should include:
TODO


Building:
# build datawave 3.10.1
mvn clean install -DskipTests

# build the datawave-ingest-service
mvn clean install -Pdocker


Launching:
# start docker
sudo systemctl start docker

# start your dfs instance
$HADOOP_PREFIX/sbin/start-dfs.sh

# navigate to your working-dir for docker compose setup above
cd $WORKING_DIR/query-microservices/docker/

# docker compose-up
docker-compose up

# check that consol sees the service
http://localhost:8500/ui/demo_dc/services

# verify ingest is available

# check config is available for ingest
http://localhost:8888/configserver/ingest-default,consul,compose.properties

# post a message to the ingest exchange to see the service process it
# defualt password guest/guest
http://localhost:15672/#/exchanges/%2F/ingest

# Payload should be file location,InputFormat,dataType
hdfs://localhost:9000/data/1.csv,datawave.ingest.csv.mr.input.CSVFileInputFormat,mycsv

# output will go to the preconfigured location within the container specified in ingest-config/mr-config.xml. The property mapreduce.output.fileoutputformat.outputdir can be updated to change the output directory.

# to get inside the compose container
docker ps

# copy the instance id
docker exec -it <instance-id> bash
cd <mapreduce.output.fileoutputformat.outputdir>
ls

each processed file should apear here


# posting to the rabbitmq topic
curl --request POST --user guest:guest --data '{"properties":{},"routing_key":"key001","payload":"Hello World","payload_encoding":"string"}'  localhost:15672/api/exchanges/%2f/ingest/publish
