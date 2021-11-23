FROM maven:3.8.3-openjdk-8-slim as build
COPY ../../.. /usr/src/app/src
COPY ../../../../../pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml clean package

FROM adoptopenjdk/openjdk8:alpine-jre
COPY --from=build /usr/src/app/target/web-crawler-1.0.0.jar /usr/app/flighttracker-1.0.0-SNAPSHOT.jar