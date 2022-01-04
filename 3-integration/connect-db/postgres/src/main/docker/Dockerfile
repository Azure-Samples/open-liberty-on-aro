# open liberty base image
FROM openliberty/open-liberty:kernel-java8-openj9-ubi

# Add config, app and jdbc driver
COPY --chown=1001:0 liberty/wlp/usr/servers/defaultServer/server.xml /config/server.xml
COPY --chown=1001:0 ${project.artifactId}.war /config/apps/
COPY --chown=1001:0 liberty/wlp/usr/shared/resources/postgresql-42.2.4.jar /opt/ol/wlp/usr/shared/resources/

# This script will add the requested XML snippets, grow image to be fit-for-purpose and apply interim fixes
RUN configure.sh
