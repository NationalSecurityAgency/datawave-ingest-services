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
Setup datawave quickstart, see <datawave-root>/docker/README.MD for details, minimally build quickstart and microservices. Suggest not to run ingest if pairing with this service.


Under the queryMicroservices branch navigate to docker/ copy all files and directories to another location (eg. /srv/data/datawave/). 
Copy from datawave-ingest-service/docker/docker-compose.yml and overwrite the existing file in <working-dir>/query-microservices/docker/
Copy datawave-ingest-service/docker/config/* to <working-dir>/query-microservices/docker/config/


Config:
Sample config is available in datawave-ingest-service/docker/ingest-config. This directory can be copied into the docker compose folder, <working-dir>/query-microservices/docker/

At minimum your config should include:
TODO


Building:
# build datawave 3.10.1
```bash
git clone git@github.com:NationalSecurityAgency/datawave.git
cd datawave
git checkout 3.10.1
mvn clean install -DskipTests
```

# checkout and build quickstart and microservices (required for version.microservice.starter being a SNAPSHOT and top build quickstart)
```bash
# This will checkout the feature/queryMicroservices branch for all of the submodules.
# By default, the submodules will all be in a detached head state.
git clone --recurse-submodules git@github.com:NationalSecurityAgency/datawave.git --branch feature/queryMicroservices datawave-query-microservices

cd datawave-query-microservices

# we need the 3.29.0-SNAPSHOT artifacts prior to the microservice build
mvn clean install -DskipTests

# Checkout the feature/queryMicroservices branch for each submodule so that we are no longer in a detached head state.
# The addition of `|| :` will ensure that the command is executed for each submodule, 
# ignoring failures for submodules that don't have a feature/queryMicroservices branch.
git submodule foreach 'git checkout feature/queryMicroservices || :'

# docker needs to be started prior to building the microservices
sudo systemctl start docker

# skipIngest, building quickstart and all microservices
mvn -Pcompose,docker,quickstart -Ddeploy -Dtar -Ddist -DskipTests -DskipIngest clean install -T1C
```

# build the datawave-ingest-service
```bash
cd datawave-ingest-service
mvn clean install -Pdocker
```

Configure Docker compose and environment:
```bash
# setup docker-compose for use with ingest-services by overwriting or updating docker-compose.yaml
# in this example we are overwriting, but can be updated instead
# this will not just copy over the docker-compose.yaml but also the necessary configs 
cp -R $WORKING_DIR/datawave-ingest/docker/* $WORKING_DIR/datawave-query-microservices/docker/
```

Launching:
```bash
# start docker
sudo systemctl start docker

# navigate to your working-dir for docker compose setup above
cd $WORKING_DIR/datawave-query-microservices/docker/

# run the bootstrap to set env variables
./bootstrap.sh

# docker compose-up
docker-compose up -d

# check that consol sees the service
http://localhost:8500/ui/demo_dc/services

# verify ingest is available
docker-compose logs ingest

docker-compose logs feeder

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

#<each processed file should apear here>

# posting to the rabbitmq topic
curl --request POST --user guest:guest --data '{"properties":{},"routing_key":"key001","payload":"Hello World","payload_encoding":"string"}'  localhost:15672/api/exchanges/%2f/ingest/publish
```
