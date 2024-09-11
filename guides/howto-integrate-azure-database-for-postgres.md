# Integrate your Liberty application with Azure Database for PostgreSQL

[Azure Database for PostgreSQL](https://azure.microsoft.com/services/postgresql/) is another fully managed database service from Microsoft Azure, based on community [PostgreSQL](https://docs.microsoft.com/azure/postgresql/concepts-supported-versions). In this guide, you will integrate your Liberty application with Azure Database for PostgreSQL to enable data persistence. The Liberty application is running on an Azure Red Hat OpenShift (ARO) 4 cluster. You learn how to:
> [!div class="checklist"]
>
> * Connect your application to Azure Database for PostgreSQL

## Before you begin

In previous guide, a Java application, which is running inside Open Liberty/WebSphere Liberty runtime, is deployed to an ARO 4 cluster. If you have not done these steps, start with [Deploy a Java application with Open Liberty/WebSphere Liberty on an Azure Red Hat OpenShift 4 cluster](howto-deploy-java-liberty-app.md) and return here to continue.

## Create an Azure Database for PostgreSQL Flexible Server

Define the following variables to use in the following steps:

```bash
randomIdentifier=$(($RANDOM * $RANDOM))
randomDBIdentifier=$RANDOM$RANDOM
LOCATION=eastus2
RESOURCE_GROUP_NAME=postgres-rg-$randomIdentifier
POSTGRESQL_SERVER_NAME=postgres$randomIdentifier
DB_NAME=demodb
DB_ADMIN=demouser
DB_ADMIN_PWD='super$ecr3t'$randomDBIdentifier
```

Follow the instructions below to set up an Azure Database for PostgreSQL Flexible Server for data persistence.

```bash
az group create \
    --name $RESOURCE_GROUP_NAME \
    --location $LOCATION

az postgres flexible-server create \
    --name $POSTGRESQL_SERVER_NAME \
    --resource-group $RESOURCE_GROUP_NAME \
    --location $LOCATION \
    --sku-name Standard_B1ms \
    --tier Burstable \
    --admin-user $DB_ADMIN \
    --admin-password $DB_ADMIN_PWD \
    --database-name $DB_NAME \
    --public-access 0.0.0.0 \
    --yes
```

Once your database is created, open **your Azure Database for PostgreSQL flexible server** > **Settins** > **Networking**
1. Verify that **Allow public access to this resource through the internet using a public IP address** is selected.
1. Verify that **Allow public access from any Azure service within Azure to this server** is selected.
1. Select **+ Add current client IP address** > Select **Save**.

## Prepare your application (PostgreSQL)

The application `<path-to-repo>/2-simple` used in the [previous guide](howto-deploy-java-liberty-app.md#prepare-the-liberty-application) has no database connectivity. To make it connect to Azure Database for PostgreSQL, a number of files need to be updated or created:.

| File Name             | Source Path                     | Destination Path              | Operation  | Description           |
|-----------------------|---------------------------------|-------------------------------|------------|-----------------------|  
| `server.xml` | [`<path-to-repo>/2-simple/src/main/liberty/config/server.xml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/src/main/liberty/config/server.xml) | [`<path-to-repo>/3-integration/connect-db/postgres/src/main/liberty/config/server.xml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/connect-db/postgres/src/main/liberty/config/server.xml) | Updated | Add `jpa-2.2` feature, `dataSource` and `library` configurations for database connection. |
| `persistence.xml` | | [`<path-to-repo>/3-integration/connect-db/postgres/src/main/resources/META-INF/persistence.xml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/connect-db/postgres/src/main/resources/META-INF/persistence.xml) | New | A configuration file specifying data persistence schema for your application. |
| `Coffee.java` | [`<path-to-repo>/2-simple/src/main/java/cafe/model/entity/Coffee.java`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/src/main/java/cafe/model/entity/Coffee.java) | [`<path-to-repo>/3-integration/connect-db/postgres/src/main/java/cafe/model/entity/Coffee.java`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/connect-db/postgres/src/main/java/cafe/model/entity/Coffee.java) | Updated | Register `Coffee` class as a JPA Entity. |
| `CafeRepository.java` | [`<path-to-repo>/2-simple/src/main/java/cafe/model/CafeRepository.java`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/src/main/java/cafe/model/CafeRepository.java) | [`<path-to-repo>/3-integration/connect-db/postgres/src/main/java/cafe/model/CafeRepository.java`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/connect-db/postgres/src/main/java/cafe/model/CafeRepository.java) | Updated | Register `CafeRepository` class as a Stateless Bean which implements CRUD operations using `jakarta.persistence.EntityManager` and `jakarta.persistence.PersistenceContext` APIs. |
| `pom.xml` | [`<path-to-repo>/2-simple/pom.xml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/pom.xml) | [`<path-to-repo>/3-integration/connect-db/postgres/pom.xml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/connect-db/postgres/pom.xml) | Updated | Add new properties and dependencies for database connection, and add new plugin `maven-dependency-plugin`. |

For reference, these changes have already been applied in `<path-to-repo>/3-integration/connect-db/postgres` of your local clone.

To run the sample application with `liberty-maven-plugin` in your local machine, execute the following commands:

```bash
cd <path-to-repo>/3-integration/connect-db/postgres

# The following variables are used for deployment file generation
export DB_SERVER_NAME=${POSTGRESQL_SERVER_NAME}.postgres.database.azure.com
export DB_PORT_NUMBER=5432
export DB_NAME=${DB_NAME}
export DB_USER=${DB_ADMIN}
export DB_PASSWORD=${DB_ADMIN_PWD}
export NAMESPACE=open-liberty-demo

mvn clean package

# If you are running with Open Liberty
mvn liberty:devc -DcontainerRunOpts="-e DB_SERVER_NAME=${DB_SERVER_NAME} -e DB_PORT_NUMBER=${DB_PORT_NUMBER} -e DB_NAME=${DB_NAME} -e DB_USER=${DB_USER} -e DB_PASSWORD=${DB_PASSWORD}" -Dcontainerfile=Dockerfile

# If you are running with WebSphere Liberty
mvn liberty:devc -DcontainerRunOpts="-e DB_SERVER_NAME=${DB_SERVER_NAME} -e DB_PORT_NUMBER=${DB_PORT_NUMBER} -e DB_NAME=${DB_NAME} -e DB_USER=${DB_USER} -e DB_PASSWORD=${DB_PASSWORD}" -Dcontainerfile=Dockerfile-wlp
```

Once the application is up and running, open [https://localhost:9443](https://localhost:9443) in your browser, verify the database connectivity works by creating new coffees and deleting existing coffees in the home page. Press **Control-C** to stop the application and Open Liberty server.

## Prepare application image

To build the application image, Dockerfile needs to be prepared in advance:

| File Name             | Source Path                     | Destination Path              | Operation  | Description           |
|-----------------------|---------------------------------|-------------------------------|------------|-----------------------|  
| `Dockerfile` | [`<path-to-repo>/2-simple/Dockerfile`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/Dockerfile) | [`<path-to-repo>/3-integration/connect-db/postgres/Dockerfile`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/connect-db/postgres/Dockerfile) | Updated | Add JDBC driver into application image, which is based on Open Liberty base image. |
| `Dockerfile-wlp` | [`<path-to-repo>/2-simple/Dockerfile-wlp`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/Dockerfile-wlp) | [`<path-to-repo>/3-integration/connect-db/postgres/Dockerfile-wlp`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/connect-db/postgres/Dockerfile-wlp) | Updated | Add JDBC driver into application image, which is based on WebSphere Liberty base image. |

Follow steps below to build the application image:

1. Change directory to `<path-to-repo>/3-integration/connect-db/postgres` of your local clone.
1. Use the following commands to build and push the image to the container registry of your Azure Red Hat OpenShift 4 cluster:

   ```bash
   # If you are building with the Open Liberty base image, the existing Dockerfile is ready for you

   # If you are building with the WebSphere Liberty base image, uncomment and execute the following two commands to rename Dockerfile-wlp to Dockerfile
   # mv Dockerfile Dockerfile.backup
   # mv Dockerfile-wlp Dockerfile

   # Change project to open-liberty-demo
   oc project open-liberty-demo

   # Create an image stream
   oc create imagestream javaee-cafe-postgres

   # Create a build configuration that specifies the image stream tag of the build output
   oc new-build --name javaee-cafe-postgres-config --binary --strategy docker --to javaee-cafe-postgres:v1

   # Start the build to upload local contents, containerize, and output to the image stream tag specified before
   oc start-build javaee-cafe-postgres-config --from-dir . --follow
   ```

## Deploy sample application

To make the application connect to the Azure Database for PostgreSQL for data persistence, a number of Kubernetes resource YAML files need to be updated or created:

| File Name             | Source Path                     | Destination Path              | Operation  | Description           |
|-----------------------|---------------------------------|-------------------------------|------------|-----------------------|  
| `db-secret.yaml` | | [`<path-to-repo>/3-integration/connect-db/postgres/src/main/aro/db-secret.yaml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/connect-db/postgres/src/main/aro/db-secret.yaml) | New | A Kubernetes **Secret** resource with database connection credentials, including `db.server.name`, `db.port.number`, `db.name`, `db.user`, and `db.password`. |
| `openlibertyapplication.yaml` | [`<path-to-repo>/2-simple/openlibertyapplication.yaml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/openlibertyapplication.yaml) | [`<path-to-repo>/3-integration/connect-db/postgres/src/main/aro/openlibertyapplication.yaml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/connect-db/postgres/src/main/aro/openlibertyapplication.yaml) | Updated | Add environment variables whose values are from Secret `db-secret-postgres`. |

For reference, these changes have already been applied in `<path-to-repo>/3-integration/connect-db/postgres` of your local clone.

Now you can deploy the sample Liberty application to the ARO 4 cluster with the following steps.

1. Make sure you have already signed in to the OpenShift CLI using the `kubeadmin` credentials. If not, follow [Connect using the OpenShift CLI](https://learn.microsoft.com/en-us/azure/openshift/tutorial-connect-cluster#connect-using-the-openshift-cli) to sign using `oc login` command.
1. Run the following commands to deploy the application.

   ```bash
   # Change directory to "<path-to-repo>/3-integration/connect-db/postgres/target"
   cd <path-to-repo>/3-integration/connect-db/postgres/target

   # Change project to "open-liberty-demo"
   oc project open-liberty-demo

   # Create  database secret
   oc create -f db-secret.yaml

   # Create the deployment
   oc create -f openlibertyapplication.yaml

   # Check if OpenLibertyApplication instance is created
   oc get openlibertyapplication javaee-cafe-postgres

   # Check if deployment created by Operator is ready. All three pods must be ready. Press Ctrl + C to exit
   oc get deployment javaee-cafe-postgres --watch

   # Get host of the route
   HOST=$(oc get route javaee-cafe-postgres --template='{{ .spec.host }}')
   echo "Route Host: https://$HOST"
   ```

Wait for a while until the Liberty Application is up and running, open the output of **Route Host** in your browser to visit the application home page.

## Next steps

In this guide, you learned how to:
> [!div class="checklist"]
>
> * Connect your application to Azure SQL Database
> * Connect your application to Azure Database for PostgreSQL

Advance to these guides, which integrate Liberty application with other Azure services:
> [!div class="nextstepaction"]
> [Set up your Liberty application in a multi-node stateless cluster with load balancing](howto-setup-stateless-cluster.md)

> [!div class="nextstepaction"]
> [Integrate your Liberty application with Elasticsearch stack](howto-integrate-elasticsearch-stack.md)

> [!div class="nextstepaction"]
> [Integrate your Liberty application with Azure Active Directory OpenID Connect](howto-integrate-aad-oidc.md)

> [!div class="nextstepaction"]
> [Integrate your Liberty application with Azure Active Directory Domain Service via Secure LDAP](howto-integrate-aad-ldap.md)

If you've finished all of above guides, advance to the complete guide, which incorporates all of Azure service integrations:
> [!div class="nextstepaction"]
> [Integrate your Liberty application with different Azure services](howto-integrate-all.md)

Here are references used in this guide:

* [Quickstart: Create an Azure Database for PostgreSQL - Flexible Server instance using Azure CLI](https://learn.microsoft.com/azure/postgresql/flexible-server/quickstart-create-server-cli)
* [Defines a data source configuration](https://openliberty.io/docs/ref/config/#dataSource.html)
