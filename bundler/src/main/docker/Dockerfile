FROM azul/zulu-openjdk-alpine:8

LABEL version=${project.version} \
      run="docker run ${docker.image.prefix}${project.artifactId}:latest" \
      description="${project.description}"

ADD ${project.build.finalName}-exec.jar /app.jar
RUN apk add libc6-compat curl
RUN apk add bash
ENV KEYSTORE_LOCATION=${KEYSTORE_LOCATION}
ENV KEYSTORE_PASSWORD=${KEYSTORE_PASSWORD}
ENV TRUSTSTORE_LOCATION=${TRUSTSTORE_LOCATION}
ENV TRUSTSTORE_PASSWORD=${TRUSTSTORE_PASSWORD}
EXPOSE 8443 8080 5005
ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005","-jar","app.jar"]