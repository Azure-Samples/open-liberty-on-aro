# Enable database session persistence for the Open Liberty application

This is an Open Liberty sample enables persistence of HTTP sessions to a datasource using JDBC. Persisting HTTP session data to a database allows recovery of the data after a server restart or unexpected server failure. Check out the source code and follow steps below to run the sample locally, run the sample in local Docker, and deploy the sample on the ARO 4 cluster.

## Prerequisites

Please complete the following guides to set up the environment:

* [Integrate your Liberty application with Azure Database for PostgreSQL](../../../guides/howto-integrate-azure-database-for-postgres.md)

After completing the above guides, you should have the followings ready:

* An Azure Database for PostgreSQL Flexible Server instance is created.
* Environment variables below are set in the terminal:

  ```bash
  export DB_SERVER_NAME=${POSTGRESQL_SERVER_NAME}.postgres.database.azure.com
  export DB_PORT_NUMBER=5432
  export DB_NAME=${DB_NAME}
  export DB_USER=${DB_ADMIN}
  export DB_PASSWORD=${DB_ADMIN_PWD}
  ```

## Run the sample locally

Checkout the repo to local directory, switch directory to `<local-repo-clone>/3-integration/session-persistence/database-ssl`, then run the following commands to build the sample and run it with liberty maven plugin:

```bash
mvn clean package
mvn liberty:dev
```

You should see message "The defaultServer server is ready to run a smarter planet" output in the command terminal.

Open https://localhost:9443 in browser to visit the app.

1. Create a new coffee with name and price, select **Submit**.
1. The new coffee is added to the list of coffees. Besides, the name and price of the new coffee are persisted as session data to the database.
1. Refresh the page, the new coffee is still there.

Press "Ctrl+C" to stop the app. 

## Run the sample in local Docker

You can also run the application as a container in local Docker:

```bash
# Run the app with Open Liberty
mvn liberty:devc -DcontainerRunOpts="-e DB_SERVER_NAME=${DB_SERVER_NAME} -e DB_PORT_NUMBER=${DB_PORT_NUMBER} -e DB_NAME=${DB_NAME} -e DB_USER=${DB_USER} -e DB_PASSWORD=${DB_PASSWORD}" -Dcontainerfile=Dockerfile

# Alternatively, you can run the app with WebSphere Liberty
mvn liberty:devc -DcontainerRunOpts="-e DB_SERVER_NAME=${DB_SERVER_NAME} -e DB_PORT_NUMBER=${DB_PORT_NUMBER} -e DB_NAME=${DB_NAME} -e DB_USER=${DB_USER} -e DB_PASSWORD=${DB_PASSWORD}" -Dcontainerfile=Dockerfile-wlp
```

Similarly, you should see message "The defaultServer server is ready to run a smarter planet" output in the command terminal.

Open https://localhost:9443 in browser to visit the app, you should see the new coffee is still there because the session data is persisted to the database and recovered after the server restart.

You can do the similar steps as above to test the application.
Press "Ctrl+C" to stop the app.  

## Deploy the sample on the ARO 4 cluster

You should already deployed an ARO 4 cluster and created a project `open-liberty-demo`.

Make sure you have already signed in to the OpenShift CLI using the `kubeadmin` credentials. If not, follow [Connect using the OpenShift CLI](https://learn.microsoft.com/en-us/azure/openshift/tutorial-connect-cluster#connect-using-the-openshift-cli) to sign using `oc login` command.

Run the following commands to build and push the image to the container registry of your Azure Red Hat OpenShift 4 cluster:

```bash
# Change directory to "<path-to-repo>/3-integration/session-persistence/database-ssl"
cd <path-to-repo>/3-integration/session-persistence/database-ssl

# If you are building with the Open Liberty base image, the existing Dockerfile is ready for you

# If you are building with the WebSphere Liberty base image, uncomment and execute the following two commands to rename Dockerfile-wlp to Dockerfile
# mv Dockerfile Dockerfile.backup
# mv Dockerfile-wlp Dockerfile

# Change project to open-liberty-demo you created before
oc project open-liberty-demo

# Create an image stream
oc create imagestream javaee-cafe-session-persistence-db-ssl

# Create a build configuration that specifies the image stream tag of the build output
oc new-build --name javaee-cafe-session-persistence-db-ssl --binary --strategy docker --to javaee-cafe-session-persistence-db-ssl:1.0.0

# Start the build to upload local contents, containerize, and output to the image stream tag specified before
oc start-build javaee-cafe-session-persistence-db-ssl --from-dir . --follow
```

Run the following commands to deploy the application.

```bash
# Change directory to "<path-to-repo>/3-integration/session-persistence/database-ssl"
cd <path-to-repo>/3-integration/session-persistence/database-ssl

# Change project to "open-liberty-demo"
oc project open-liberty-demo

# Create secret for database connection
envsubst < db-secret.yaml | oc apply -f -

# Create TLS private key and certificate, which is also used as CA certificate for testing purpose
openssl req -x509 -subj "/C=US/ST=majguo/L=OpenLiberty/O=demo/CN=www.example.com" -sha256 -nodes -days 365 -newkey rsa:2048 -keyout tls.key -out tls.crt

# Create environment variables which will be passed to secret "tls-crt-secret"
export CA_CRT=$(cat tls.crt | base64 -w 0)
export DEST_CA_CRT=$(cat tls.crt | base64 -w 0)
export TLS_CRT=$(cat tls.crt | base64 -w 0)
export TLS_KEY=$(cat tls.key | base64 -w 0)

# Create a TLS certificate secret for reencrypt TLS termination.
envsubst < tls-crt-secret.yaml | oc apply -f -

# Create OpenLibertyApplication
oc create -f openlibertyapplication.yaml

# Check if OpenLibertyApplication instance is created
oc get openlibertyapplication javaee-cafe-session-persistence-db-ssl

# Check if deployment created by Operator is ready
oc get deployment javaee-cafe-session-persistence-db-ssl

# Get host of the route
HOST=$(oc get route javaee-cafe-session-persistence-db-ssl --template='{{ .spec.host }}')
echo "Route Host: https://$HOST"
```

Once the Liberty Application is up and running, copy the value of **Route Host** from console output, and open it in your browser to test the application deployed on the ARO 4 cluster.

1. Create a new coffee with name and price, select **Submit**.
1. The new coffee is added to the list of coffees. Besides, the name and price of the new coffee are persisted as session data to the database.
1. Copy the pod name displayed in the top-left corner of the page, and run the following command to delete the pod:

   ```bash
   oc delete pod <pod-name>
   ```

1. Refresh the page, the new coffee is still there because the session data is persisted to the database and recovered after switching to another pod due to the load balancer.

### Clean up

To clean up all resources deployed to the ARO 4 cluster:

```
oc delete openlibertyapplication javaee-cafe-session-persistence-db-ssl
oc delete secret tls-crt-secret
oc delete secret db-secret-postgres
oc delete buildconfig javaee-cafe-session-persistence-db-ssl
oc delete imagestream javaee-cafe-session-persistence-db-ssl
```

## References

See the following references for more information.

* [Database Session Persistence](https://openliberty.io/docs/latest/reference/feature/sessionDatabase-1.0.html)
