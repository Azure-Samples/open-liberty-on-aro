# WebSphere Liberty base image
FROM icr.io/appcafe/websphere-liberty:kernel-java17-openj9-ubi

# Add config
COPY --chown=1001:0 liberty/wlp/usr/servers/defaultServer/server.xml /config/server.xml

# This script will add the requested XML snippets to enable Liberty features and grow image to be fit-for-purpose using featureUtility.
# Only available in 'kernel-slim'. The 'full' tag already includes all features for convenience.
RUN features.sh

# Add app and other dependencies
COPY --chown=1001:0 javaee-cafe.war /config/apps/
COPY --chown=1001:0 liberty/wlp/usr/shared/resources/mssql-jdbc-8.2.2.jre8.jar /opt/ibm/wlp/usr/shared/resources/

# This script will add the requested XML snippets, grow image to be fit-for-purpose and apply interim fixes
RUN configure.sh
