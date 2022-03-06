# syntax = docker/dockerfile:1.3
#ARG BASE_IMAGE=openjdk:16-alpine3.13
ARG JAVA_APP
ARG BASE_IMAGE=eclipse-temurin:17-jdk
FROM ${BASE_IMAGE}

#STOPSIGNAL SIGTERM

ENV ENABLE_RCON=true RCON_PORT=25575 RCON_PASSWORD=minecraft \
  SERVER=${JAVA_APP}

COPY --chmod=755 scripts/start* /
COPY target/${JAVA_APP} /
RUN mkdir -p /libs
COPY target/libs/*.jar /libs

#RUN dos2unix /start* /autopause/* /autostop/* /rconcmds/*
#RUN dos2unix /start*

ENTRYPOINT [ "/start", "dev.agones.cmd.TestMonitor"]
#HEALTHCHECK --start-period=1m CMD mc-health

