Datawave Ingest Service is designed to layer on top of the existing ingest code to run ingest as a service in kubernetes. The service is built on Spring Boot to supply easy access to queues which will provide message chunks to be processed by ingest. 

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
