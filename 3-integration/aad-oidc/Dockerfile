# open liberty base image
FROM icr.io/appcafe/open-liberty:kernel-slim-java17-openj9-ubi

# Add config and app
COPY --chown=1001:0 src/main/liberty/config/server.xml /config/server.xml
COPY --chown=1001:0 target/javaee-cafe.war /config/apps/

# This script will add the requested XML snippets, grow image to be fit-for-purpose and apply interim fixes
RUN configure.sh
