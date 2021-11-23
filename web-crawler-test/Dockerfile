FROM openjdk:8-jre-alpine
VOLUME /tmp
VOLUME /etc/app
VOLUME /var/app
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
RUN sh -c 'touch /app.jar'

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]