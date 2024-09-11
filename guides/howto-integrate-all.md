# Integrate your Liberty application with different Azure services

In this guide, you will integrate your Liberty application with different Azure services, including security, data persistence & distributed logging. The Liberty application is running on an Azure Red Hat OpenShift (ARO) 4 cluster. You learn how to:
> [!div class="checklist"]
>
> * Set up different services
> * Prepare your application
> * Prepare application image
> * Deploy sample application

## Before you begin

In previous guides, a Java application, which is running inside Open Liberty/WebSphere Liberty runtime, is deployed to an ARO 4 cluster. If you have not done these guides, walk them through with the following links and return here to continue.

* [Deploy a Java application inside Open Liberty/WebSphere Liberty on an Azure Red Hat OpenShift 4 cluster](howto-deploy-java-liberty-app.md)
* [Integrate your Liberty application with Elasticsearch stack](howto-integrate-elasticsearch-stack.md)
* [Integrate your Liberty application with Azure Database for PostgreSQL](howto-integrate-azure-database-for-postgres.md)
* [Integrate your Liberty application with Azure Active Directory OpenID Connect](howto-integrate-aad-oidc.md)

## Set up different services

When you complete all of previous guides, different services used for this guide have already set up. Let's recap them one by one.

1. [Set up Azure Red Hat OpenShift cluster](howto-deploy-java-liberty-app.md#set-up-azure-red-hat-openshift-cluster).
2. [Set up Azure Active Directory](howto-integrate-aad-oidc.md#set-up-azure-active-directory).
3. [Create an Azure Database for PostgreSQL Flexible server](howto-integrate-azure-database-for-postgres.md#create-an-azure-database-for-postgresql-flexible-server).
4. [Deploy cluster logging](howto-integrate-elasticsearch-stack.md#deploy-cluster-logging).

## Prepare your application

The application `<path-to-repo>/2-simple` used in the previous [basic guide](howto-deploy-java-liberty-app.md#prepare-the-liberty-application) hasn't enabled security, data persistence or distributed logging. To make it integrate with different services, a number of files need to be updated or created:

| File Name             | Source Path                     | Destination Path              | Operation  | Description           |
|-----------------------|---------------------------------|-------------------------------|------------|-----------------------|  
| `server.xml` | [`<path-to-repo>/2-simple/src/main/liberty/config/server.xml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/src/main/liberty/config/server.xml) | [`<path-to-repo>/4-finish/src/main/liberty/config/server.xml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/4-finish/src/main/liberty/config/server.xml) | Updated | Add `socialLogin-1.0`, `transportSecurity-1.0`, `appSecurity-3.0`, `jwt-1.0`, `mpJwt-1.1`, `mpConfig-1.3`, `jpa-2.2` features and their configurations. |
| `web.xml` | [`<path-to-repo>/2-simple/src/main/webapp/WEB-INF/web.xml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/src/main/webapp/WEB-INF/web.xml) | [`<path-to-repo>/4-finish/src/main/webapp/WEB-INF/web.xml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/4-finish/src/main/webapp/WEB-INF/web.xml) | Updated | Add `security-role` and `security-constraint` for accessing web resources of the application. |
| `CafeJwtUtil.java` | | [`<path-to-repo>/4-finish/src/main/java/cafe/web/view/CafeJwtUtil.java`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/4-finish/src/main/java/cafe/web/view/CafeJwtUtil.java) | New | The utility class for retrieving ID token issued from Azure AD, and providing an API to determine if the logged-on user is in the configured **admin group** of Azure AD. |
| `CafeRequestFilter.java` | | [`<path-to-repo>/4-finish/src/main/java/cafe/web/view/CafeRequestFilter.java`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/4-finish/src/main/java/cafe/web/view/CafeRequestFilter.java) | New | A client request filter for adding JWT token in **HTTP Authorization Header** for outbound requests. |
| `Cafe.java` | [`<path-to-repo>/2-simple/src/mainjava/cafe/web/view/Cafe.java`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/src/main/java/cafe/web/view/Cafe.java) | [`<path-to-repo>/4-finish/src/main/java/cafe/web/view/Cafe.java`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/4-finish/src/main/java/cafe/web/view/Cafe.java) | Updated | Register `CafeRequestFilter` for intercepting internal REST calls, add new APIs to get principal name of logged-on user and flag indicating whether the logged-on user can delete existing coffees or not. |
| `CafeResource.java` | [`<path-to-repo>/2-simple/src/main/java/cafe/web/rest/CafeResource.java`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/src/main/java/cafe/web/rest/CafeResource.java) | [`<path-to-repo>/4-finish/src/main/java/cafe/web/rest/CafeResource.java`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/4-finish/src/main/java/cafe/web/rest/CafeResource.java) | Updated | Inject `JsonWebToken` to verify the **groups claim** of the token for RBAC. |
| `Coffee.java` | [`<path-to-repo>/2-simple/src/main/java/cafe/model/entity/Coffee.java`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/src/main/java/cafe/model/entity/Coffee.java) | [`<path-to-repo>/4-finish/src/main/java/cafe/model/entity/Coffee.java`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/4-finish/src/main/java/cafe/model/entity/Coffee.java) | Updated | Annotate POJO `Coffee` with `jakarta.persistence.Entity` annotation to make it a JPA Entity. |
| `CafeRepository.java` | [`<path-to-repo>/2-simple/src/main/java/cafe/model/CafeRepository.java`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/src/main/java/cafe/model/CafeRepository.java) | [`<path-to-repo>/4-finish/src/main/java/cafe/model/CafeRepository.java`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/4-finish/src/main/java/cafe/model/CafeRepository.java) | Updated | Changed to be a Stateless Bean, `CafeRepository` implements create, read, update, and delete coffees using `jakarta.persistence.EntityManager` and `jakarta.persistence.PersistenceContext` APIs. |
| `persistence.xml` | | [`<path-to-repo>/4-finish/src/main/resources/META-INF/persistence.xml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/4-finish/src/main/resources/META-INF/persistence.xml) | New | A new configuration file to configure data persistence schema. |
| `messages.properties` | [`<path-to-repo>/2-simple/src/main/resources/cafe/web/messages.properties`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/src/main/resources/cafe/web/messages.properties) | [`<path-to-repo>/4-finish/src/main/resources/cafe/web/messages.properties`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/4-finish/src/main/resources/cafe/web/messages.properties) | Updated | Parameterize display name using the name of the logged-on user. |
| `messages_es.properties` | [`<path-to-repo>/2-simple/src/main/resources/cafe/web/messages_es.properties`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/src/main/resources/cafe/web/messages_es.properties) | [`<path-to-repo>/4-finish/src/main/resources/cafe/web/messages_es.properties`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/4-finish/src/main/resources/cafe/web/messages_es.properties) | Updated | Parameterize display name using the name of the logged-on user. |
| `index.xhtml` | [`<path-to-repo>/2-simple/src/main/webapp/index.xhtml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/src/main/webapp/index.xhtml) | [`<path-to-repo>/4-finish/src/main/webapp/index.xhtml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/4-finish/src/main/webapp/index.xhtml) | Updated | Display principal name of the logged-on user. Disable coffee delete button if the logged-on user is not authorized. |
| `pom.xml` | [`<path-to-repo>/2-simple/pom.xml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/pom.xml) | [`<path-to-repo>/4-finish/pom.xml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/4-finish/pom.xml) | Updated | Add new properties and dependencies for Social Login and database connection, and add new dependency for **Eclipse MicroProfile** and **postgresql** JDBC driver. |

For reference, these changes have already been applied in `<path-to-repo>/4-finish` of your local clone.

Execute the following commands to build application package:

```bash
cd <path-to-repo>/4-finish
mvn clean package
```

## Prepare application image

To build the application image, Dockerfile needs to be prepared in advance:

| File Name             | Source Path                     | Destination Path              | Operation  | Description           |
|-----------------------|---------------------------------|-------------------------------|------------|-----------------------|  
| `Dockerfile` | [`<path-to-repo>/2-simple/Dockerfile`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/Dockerfile) | [`<path-to-repo>/4-finish/Dockerfile`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/4-finish/Dockerfile) | Updated | Add JDBC driver into application image, which is based on Open Liberty base image. |
| `Dockerfile-wlp` | [`<path-to-repo>/2-simple/Dockerfile-wlp`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/Dockerfile-wlp) | [`<path-to-repo>/4-finish/Dockerfile-wlp`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/4-finish/Dockerfile-wlp) | Updated | Add JDBC driver into application image, which is based on WebSphere Liberty base image. |

Follow steps below to build the application image and push it to the built-in container image registry:

1. Change directory to `<path-to-repo>/4-finish` of your local clone.
2. Download [postgresql-42.4.1.jar](https://repo1.maven.org/maven2/org/postgresql/postgresql/42.4.1/postgresql-42.4.1.jar) and put it to current working directory.
3. Log in to the OpenShift web console from your browser using the credentials of the Azure AD user.
4. [Log in to the OpenShift CLI with the token for the Azure AD user](howto-deploy-java-liberty-app.md#log-in-to-the-openshift-cli-with-the-token).
5. Run the following commands to build application image and push it to the registry.

   ```bash
   # Build and tag application image
   # Note:
   # - replace "${Docker_File}" with "Dockerfile" to build application image with Open Liberty base image
   # - replace "${Docker_File}" with "Dockerfile-wlp" to build application image with WebSphere Liberty base image
   docker build -t javaee-cafe-all-in-one:1.0.0 --pull --file=${Docker_File} .

   # Note: replace "<Container_Registry_URL>" with the fully qualified name of the registry
   Container_Registry_URL=<Container_Registry_URL>

   # Create a new tag with registry info that refers to source image
   docker tag javaee-cafe-all-in-one:1.0.0:1.0.0 ${Container_Registry_URL}/open-liberty-demo/javaee-cafe-all-in-one:1.0.0

   # Log in to the built-in container image registry
   docker login -u $(oc whoami) -p $(oc whoami -t) ${Container_Registry_URL}

   # Push image to the built-in container image registry
   docker push ${Container_Registry_URL}/open-liberty-demo/javaee-cafe-all-in-one:1.0.0
   ```

## Deploy sample application

To integrate the application with Azure AD OpenID Connect and Azure Database for PostgreSQL server on the ARO 4 cluster, a number of Kubernetes resource YAML files need to be updated or created.

| File Name             | Source Path                     | Destination Path              | Operation  | Description           |
|-----------------------|---------------------------------|-------------------------------|------------|-----------------------|  
| `aad-oidc-secret.yaml` | | [`<path-to-repo>/4-finish/aad-oidc-secret.yaml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/4-finish/aad-oidc-secret.yaml) | New | A Kubernetes **Secret** resource with user input data, including `client.id`, `client.secret`, `tenant.id`, and `admin.group.id`. |
| `tls-crt-secret.yaml` | | [`<path-to-repo>/4-finish/tls-crt-secret.yaml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/4-finish/tls-crt-secret.yaml) | New | A Kubernetes **Secret** resource with user input data, including `ca.crt`, `destCA.crt`, `tls.crt`, and `tls.key`. |
| `db-secret.yaml` | | [`<path-to-repo>/4-finish/db-secret.yaml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/4-finish/db-secret.yaml) | New | A Kubernetes **Secret** resource with PostgreSQL database connection data, including `db.server.name`, `db.port.number`, `db.name`, `db.user`, and `db.password`. |
| `openlibertyapplication.yaml` | [`<path-to-repo>/2-simple/openlibertyapplication.yaml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/openlibertyapplication.yaml) | [`<path-to-repo>/4-finish/openlibertyapplication.yaml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/4-finish/openlibertyapplication.yaml) | Updated | Add environment variables whose values are from Secret `aad-oidc-secret` and `db-secret-postgres`. Specify existing certificate for **Route** and **Service** of **OpenLibertyApplication** custom resource. |

For reference, these changes have already been applied in `<path-to-repo>/4-finish` of your local clone.

Now you can deploy the sample Liberty application to the ARO 4 cluster with the following steps.

1. Log in to the OpenShift web console from your browser using the credentials of the Azure AD user.
2. [Log in to the OpenShift CLI with the token for the Azure AD user](howto-deploy-java-liberty-app.md#log-in-to-the-openshift-cli-with-the-token).
3. Run the following commands to deploy the application.

   ```bash
   # Change directory to "<path-to-repo>/4-finish"
   cd <path-to-repo>/4-finish

   # Change project to "open-liberty-demo"
   oc project open-liberty-demo

   # Create environment variables which will be passed to secret "aad-oidc-secret"
   # Note: replace "<client ID>", "<client secret>", "<tenant ID>", and "<group ID>" with the ones you noted down before
   export CLIENT_ID=<client ID>
   export CLIENT_SECRET=<client secret>
   export TENANT_ID=<tenant ID>
   export ADMIN_GROUP_ID=<group ID>

   # Create secret "aad-oidc-secret"
   envsubst < aad-oidc-secret.yaml | oc create -f -

   # Create TLS private key and certificate, which is also used as CA certificate for testing purpose
   openssl req -x509 -sha256 -nodes -days 365 -newkey rsa:2048 -keyout tls.key -out tls.crt

   # Create environment variables which will be passed to secret "tls-crt-secret"
   export CA_CRT=$(cat tls.crt | base64 -w 0)
   export DEST_CA_CRT=$(cat tls.crt | base64 -w 0)
   export TLS_CRT=$(cat tls.crt | base64 -w 0)
   export TLS_KEY=$(cat tls.key | base64 -w 0)

   # Create secret "tls-crt-secret"
   envsubst < tls-crt-secret.yaml | oc create -f -

   # Create environment variables which will be passed to secret "db-secret-postgres"
   # Note: replace "<Server name>", "<Port number>", "<Admin username>", and "<Password>" with the ones you noted down before
   export DB_SERVER_NAME=<Server name>.postgres.database.azure.com
   export DB_PORT_NUMBER=<Port number>
   export DB_NAME=postgres
   export DB_USER=<Admin username>@<Server name>
   export DB_PASSWORD=<Password>

   # Create Secret "db-secret-postgres"
   envsubst < db-secret.yaml | oc create -f -

   # Create OpenLibertyApplication "javaee-cafe-all-in-one"
   oc create -f openlibertyapplication.yaml

   # Check if OpenLibertyApplication instance is created
   oc get openlibertyapplication javaee-cafe-all-in-one

   # Check if deployment created by Operator is ready
   oc get deployment javaee-cafe-all-in-one

   # Get host of the route
   HOST=$(oc get route javaee-cafe-all-in-one --template='{{ .spec.host }}')
   echo "Route Host: $HOST"
   ```

Once the Liberty Application is up and running, copy the value of **Route Host** from console output.

1. Open your **Azure AD** > **App registrations** > your **registered application** > **Authentication** > Click **Add URI** in **Redirect URIs** section > Replace **\<Route_Host>** with the value of **Route Host** for `https://<Route_Host>/ibm/api/social-login/redirect/liberty-aad-oidc-javaeecafe`, and fill it into the text box > Click **Save**.
2. Replace **\<Route_Host>** with the value of **Route Host** for `https://<Route_Host>`, and open it in the **InPrivate** window of **Microsoft Edge**, verify the application is secured by Azure AD OpenID Connect and connected to Azure Database for PostgreSQL server.

   1. Sign in as a user, who doesn't belong to the admin group you created before.
   2. Update your password if necessary. Accept permission requested if necessary.
   3. You will see the email address of your AAD account displayed in the application home page, where the coffee **Delete** button is **disabled**.
   4. Create new coffees.
   5. Close the **InPrivate** window > open a new **InPrivate** window > sign in as another user, who does belong to the admin group you created before.
   6. Update your password if necessary. Accept permission requested if necessary.
   7. You will see the email address of your AAD account displayed in the application home page, where the coffee **Delete** button is **enabled** now.
   8. Create new coffees. Delete existing coffees.

The application logs are shipped to the Elasticsearch cluster, and can be visualized in the Kibana web console.

1. Log in to the OpenShift web console from your browser using the `kubeadmin` credentials. Click **Monitoring** > **Logging**.
2. In the new opened window, click **Log in with OpenShift**. Log in with `kubeadmin` if required.
3. Open **Management** > **Index Patterns** > Select **project.\*** > Click **Refresh field list** icon at top-right of the page.
4. Click **Discover**. Select index pattern **project.\*** from the dropdown list.
5. Add **kubernetes.namespace_name**, **kubernetes.pod_name**, **loglevel**, and **message** from **Available Fields** into **Selected Fields**. Discover application logs from the work area of the page.

If you want to log in using the Azure AD user to view logs in the Kibana web console, follow the steps above but replace index pattern **project.\*** with **project.open-liberty-demo.\<random-guid>.\***.

## Next steps

In this guide, you learned how to:
> [!div class="checklist"]
>
> * Set up different services
> * Prepare your application
> * Prepare application image
> * Deploy sample application

To free OpenShift resources created for these guides:

* [Connect to the ARO 4 cluster](/azure/openshift/tutorial-connect-cluster).
* Run `oc delete project open-liberty-demo` in a console.

To delete the ARO 4 cluster, follow "[Tutorial: Delete an Azure Red Hat OpenShift 4 cluster](https://docs.microsoft.com/azure/openshift/tutorial-delete-cluster)".
