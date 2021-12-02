FROM azul/zulu-openjdk-alpine:8

LABEL version=1.0-SNAPSHOT \
      run="docker run datawave/ingest-service:latest" \
      description="DATAWAVE Ingest Microservice"

ADD ingest-service-1.0-SNAPSHOT-exec.jar /app.jar
RUN apk add libc6-compat curl

EXPOSE 8443 8080
ENTRYPOINT ["java","-jar","app.jar"]