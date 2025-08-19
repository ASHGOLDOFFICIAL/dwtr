ARG ALPINE_TAG=3.22
ARG JDK_TAG=21.0.7_6
ARG JRE_TAG=21.0.8_9
ARG SBT_TAG=1.11.3
ARG SCALA_TAG=3.3.6


FROM sbtscala/scala-sbt:eclipse-temurin-${JDK_TAG}_${SBT_TAG}_${SCALA_TAG} AS build
WORKDIR /build
COPY project ./project
COPY build.sbt .
RUN sbt update

COPY core/src ./core/src
RUN sbt core/assembly


FROM eclipse-temurin:${JRE_TAG}-jre-alpine-${ALPINE_TAG} AS runtime
RUN apk add argon2-libs
WORKDIR /usr/app
COPY --from=build /build/core/target/scala-3.3.6/*.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

LABEL authors="ashgoldofficial"