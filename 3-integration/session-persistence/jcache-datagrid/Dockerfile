# open liberty base image
FROM openliberty/open-liberty:kernel-java8-openj9-ubi

# Add config, app, jdbc driver & infinispan libs
COPY --chown=1001:0 src/main/liberty/config/server.xml /config/server.xml
COPY --chown=1001:0 target/javaee-cafe.war /config/apps/
COPY --chown=1001:0 postgresql-42.4.1.jar /opt/ol/wlp/usr/shared/resources/
ADD --chown=1001:0 infinispan /opt/ol/wlp/usr/shared/resources/infinispan

# This script will add the requested XML snippets, grow image to be fit-for-purpose and apply interim fixes
RUN configure.sh
