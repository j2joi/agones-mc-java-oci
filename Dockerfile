# syntax = docker/dockerfile:1.3
#ARG BASE_IMAGE=openjdk:16-alpine3.13
ARG BASE_BUILD=mcserverping-1.0.5.jar
ARG BASE_IMAGE=eclipse-temurin:17-jdk
FROM ${BASE_IMAGE}

# CI system should set this to a hash or git revision of the build directory and it's contents to
# ensure consistent cache updates.
#ARG BUILD_FILES_REV=1
#RUN --mount=target=/build,source=build \
#    REV=${BUILD_FILES_REV} /build/run.sh install-packages

#RUN --mount=target=/build,source=build \
#    REV=${BUILD_FILES_REV} /build/run.sh setup-user

#COPY --chmod=644 files/sudoers* /etc/sudoers.d


STOPSIGNAL SIGTERM

ENV UID=1000 GID=1000 \
  MEMORY="1G" \
  TYPE=VANILLA VERSION=LATEST \
  ENABLE_RCON=true RCON_PORT=25575 RCON_PASSWORD=minecraft \
  ENABLE_AUTOPAUSE=false AUTOPAUSE_TIMEOUT_EST=3600 AUTOPAUSE_TIMEOUT_KN=120 AUTOPAUSE_TIMEOUT_INIT=600 \
  AUTOPAUSE_PERIOD=10 AUTOPAUSE_KNOCK_INTERFACE=eth0 \
  ENABLE_AUTOSTOP=false AUTOSTOP_TIMEOUT_EST=3600 AUTOSTOP_TIMEOUT_INIT=1800 AUTOSTOP_PERIOD=10 \
  SERVER=${BASE_BUILD}

COPY --chmod=755 scripts/start* /
COPY target/${BASE_BUILD} /
RUN mkdir -p /libs
COPY libs/*.jar /libs

#RUN dos2unix /start* /autopause/* /autostop/* /rconcmds/*
#RUN dos2unix /start*

ENTRYPOINT [ "/start" ]
#HEALTHCHECK --start-period=1m CMD mc-health

