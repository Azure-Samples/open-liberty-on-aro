# open liberty base image
FROM icr.io/appcafe/open-liberty:kernel-slim-java17-openj9-ubi

# Add config, app and jdbc driver
COPY --chown=1001:0 src/main/liberty/config/server.xml /config/server.xml
COPY --chown=1001:0 target/javaee-cafe.war /config/apps/
COPY --chown=1001:0 postgresql-42.4.1.jar /opt/ol/wlp/usr/shared/resources/

# This script will add the requested XML snippets, grow image to be fit-for-purpose and apply interim fixes
RUN configure.sh
