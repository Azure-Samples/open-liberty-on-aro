# open liberty base image
FROM icr.io/appcafe/open-liberty:kernel-slim-java17-openj9-ubi

# Add config file
COPY --chown=1001:0 src/main/liberty/config/server.xml /config/server.xml

# This script will add the requested XML snippets to enable Liberty features and grow image to be fit-for-purpose using featureUtility.
# Only available in 'kernel-slim'. The 'full' tag already includes all features for convenience.
RUN features.sh

# Add app and other dependencies
COPY --chown=1001:0 target/javaee-cafe.war /config/apps/
COPY --chown=1001:0 target/liberty/wlp/usr/shared/resources/postgresql-42.7.2.jar /opt/ol/wlp/usr/shared/resources/
ADD --chown=1001:0 infinispan /opt/ol/wlp/usr/shared/resources/infinispan

# This script will add the requested XML snippets, grow image to be fit-for-purpose and apply interim fixes
RUN configure.sh
