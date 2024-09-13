# Integrate your Liberty application with Microsoft Entra Domain Service via Secure LDAP

In this guide, you will integrate your Liberty application with Microsoft Entra Domain Services via Secure LDAP for security. The Liberty application is running on an Azure Red Hat OpenShift (ARO) 4 cluster. You learn how to:
> [!div class="checklist"]
>
> * Configure secure LDAP for a Microsoft Entra Domain Services managed domain
> * Prepare your application
> * Prepare application image
> * Deploy sample application

## Before you begin

In previous guide, a Java application, which is running inside Open Liberty/WebSphere Liberty runtime, is deployed to an ARO 4 cluster. If you have not done these steps, start with [Deploy a Java application with Open Liberty/WebSphere Liberty on an Azure Red Hat OpenShift 4 cluster](howto-deploy-java-liberty-app.md) and return here to continue.

### Create and configure a Microsoft Entra Domain Services managed domain

You need to have a Microsoft Entra tenant. If you don't have an existing tenant, see [Quickstart: Set up a tenant](https://learn.microsoft.com/entra/identity-platform/quickstart-create-new-tenant). 

Complete the tutorial [Create and configure a Microsoft Entra Domain Services managed domain](https://learn.microsoft.com/azure/active-directory-domain-services/tutorial-create-instance) up to but not including the section [Enable user accounts for Domain Services](https://learn.microsoft.com/en-us/azure/active-directory-domain-services/tutorial-create-instance#enable-user-accounts-for-azure-ad-ds). That section requires special treatment in the context of this tutorial, as described in the next section. Be sure to complete the DNS actions completely and correctly.

Note down the value you specify when completing the step "Enter a DNS domain name for your managed domain." You use it later in this article.

### Create users and reset passwords

This section includes steps to create users and change their password, which is required to cause the users to propagate successfully through LDAP.

1. Within the Azure portal, ensure the subscription corresponding to the Microsoft Entra ID tenant is the currently active directory. To learn how to select the correct directory see [Associate or add an Azure subscription to your Microsoft Entra tenant](https://learn.microsoft.com/azure/active-directory/fundamentals/active-directory-how-subscriptions-associated-directory). If the incorrect directory is selected, you either aren't be able to create users, or you create users in the wrong directory.
1. In the search box at the top of the Azure portal, enter "Users".
1. Select **New user**.
1. Ensure **Create user** is selected.
1. Fill in values for User name, name, First name, and Last name. Leave the remaining fields at their default values.
1. Select **Create**.
1. Select the newly created user in the table.
1. Select **Reset password**.
1. In the panel that appears, select **Reset password**.
1. Note down the temporary password.
1. In an "incognito" browser window, visit [the Azure portal](https://portal.azure.com/) and log in with the user's credentials and password.
1. Change the password when prompted. Note down the new password. You use it later.
1. Log out and close the "incognito" window.

Repeat the steps from "Select **New user**" through "Log and out close" for each user you want to enable. Pls create at least two users.

### Configure secure LDAP for a Microsoft Entra Domain Services managed domain

This section walks you through a separate tutorial to configure secure LDAP for a Microsoft Entra Domain Services managed domain.

First, open the tutorial [Configure secure LDAP for a Microsoft Entra Domain Services managed domain](https://learn.microsoft.com/azure/active-directory-domain-services/tutorial-configure-ldaps) in a separate browser window so you can look at the below variations as you run through the tutorial.  

When you reach the section, [Export a certificate for client computers](https://learn.microsoft.com/azure/active-directory-domain-services/tutorial-configure-ldaps#export-a-certificate-for-client-computers), take note of where you save the certificate file ending in *.cer*. You use the certificate as input to the Open Liberty configuration.

When you reach the section, [Lock down secure LDAP access over the internet](https://learn.microsoft.com/azure/active-directory-domain-services/tutorial-configure-ldaps#lock-down-secure-ldap-access-over-the-internet), specify **Any** as the source. You tighten the security rule with a specific IP address later in this guide.

Before you execute the steps in [Test queries to the managed domain](https://learn.microsoft.com/azure/active-directory-domain-services/tutorial-configure-ldaps#test-queries-to-the-managed-domain), do the following steps to enable the testing to succeed.

   1. In the portal, visit the overview page for the Microsoft Entra Domain Services instance.
   1. In the Settings area, select **Properties**.
   1. In the right of the page, scroll down until you see **Admin group**. Under this heading should be a link for **AAD DC Administrators**. Select that link.
   1. In the **Manage** section, select **Members**.
   1. Select **Add members**.
   1. In the **Search** text field, enter some characters to locate one of the users you created in a preceding step.
   1. Select the user, then activate the **Select** button.
   1. This user is the one you must use when executing the steps in the **Test queries to the managed domain** section.
   
   >[!NOTE]
   >
   > Here are some tips about querying the LDAP data, which you'll need to do to collect some values necessary for Open Liberty configuration.
   >
   > * The tutorial advises use of the Windows program *LDP.exe*. This program is only available on Windows. For non-Windows users or you can't access the Secure LDAP server due to network settigns, you can try to use [Apache Directory Studio](https://directory.apache.org/studio/downloads.html) for the same purpose.
   > * When logging in to LDAP with *LDP.exe* or *Apache Directory Studio*, the username is just the part before the @. For example, if the user is `alice@contoso.onmicrosoft.com`, the username for the *LDP.exe* bind action is `alice`. Also, leave *LDP.exe* or *Apache Directory Studio* running and logged in for use in subsequent steps.
   >

In the section [Configure DNS zone for external access](https://learn.microsoft.com/azure/active-directory-domain-services/tutorial-configure-ldaps#configure-dns-zone-for-external-access), note down the value for **Secure LDAP external IP address**. You use it later.

Don't execute the steps in [Clean-up resources](https://learn.microsoft.com/azure/active-directory-domain-services/tutorial-configure-ldaps#clean-up-resources) until instructed to do so in this guide.

With the above variations in mind, complete [Configure secure LDAP for an Microsoft Entra Domain Services managed domain](https://learn.microsoft.com/azure/active-directory-domain-services/tutorial-configure-ldaps). You can collect the values necessary to provide to the Open Liberty Configuration after importing the certificate saved above to a new-created key store in the next section.

### Trust singer certificate of the LDAP server

For SSL communication with an LDAP server to succeed, the signer certificate for the LDAP server must be added to the trust store that is referenced by the Liberty server. Execute the following commands to complete this step.

```bash
# Change directory to "<path-to-repo>/3-integration/aad-ldap"
cd <path-to-repo>/3-integration/aad-ldap

# Note: write down the specified key store name and key store password for later use
KEYSTORE_NAME=<KEYSTORE_NAME>
KEYSTORE_PASS=<KEYSTORE_PASS>

# Generate a key store file with additional user inputs
keytool -genkeypair -keyalg RSA -storetype pkcs12 -keystore $(pwd)/src/main/liberty/config/$KEYSTORE_NAME -storepass $KEYSTORE_PASS -dname "CN=www.example.com, O=demo, C=US"

# Note: "<path-to-ldap-server-certificate>" is the *.cer* file you were asked to save aside in section "Set up Microsoft Entra"
LDAP_SERVER_CERTIFICATE_PAH=<path-to-ldap-server-certificate>

# Import singer certificate of the LDAP server
keytool -keystore $(pwd)/src/main/liberty/config/$KEYSTORE_NAME -storepass $KEYSTORE_PASS -import -file $LDAP_SERVER_CERTIFICATE_PAH -alias ldap -trustcacerts -noprompt
```

### Collect parameter values for Liberty LDAP configuration

Besides the trust store generated above, collect other parameter values from the Microsoft Entra DS deployed earlier.

| Parameter name | Description   | Value |
|----------------|---------------|---------|
| `LDAP_SERVER_HOST` | the secure LDAP DNS domain name of your managed domain | Copied from the section [Configure DNS zone for external access](https://learn.microsoft.com/entra/identity/domain-services/tutorial-configure-ldaps#configure-dns-zone-for-external-access), e.g., *ldaps.majguodscontoso.com* |
| `LDAP_SERVER_PORT` | Port of LDAP server | 636 |
| `LDAP_SERVER_IP_ADDRESS` | LDAP external IP address | Copied from the section [Configure DNS zone for external access](https://learn.microsoft.com/azure/active-directory-domain-services/tutorial-configure-ldaps#configure-dns-zone-for-external-access) |
| `LDAP_SERVER_BASEDN` | User Base DN | The value that uses **majguodscontoso.com** as example of domain is "OU=AADDC Users,DC=majguodscontoso,DC=com" |
| `LDAP_SERVER_BINDDN` | Principal | Get the display name of the user you added to group "AAD DC Administrators", e.g., **AdminTest**. The final value that uses **majguodscontoso.com** as example of domain is "CN=AdminTest,OU=AADDC Users,DC=majguodscontoso,DC=com" |
| `LDAP_SERVER_BINDPASSWORD` | Password for Principal | The password of the user you added to group "AAD DC Administrators" |
| `ADMIN_GROUP_NAME` | Name of the administrator group | "AAD DC Administrators" |
| `KEYSTORE_NAME` | Name of the key store | The value you wrote down in section "Trust singer certificate of the LDAP server" |
| `KEYSTORE_PASS` | Password of the key store | The value you wrote down in section "Trust singer certificate of the LDAP server" |

Now export these values as environment variables:

```bash
export LDAP_SERVER_HOST=<LDAP_SERVER_HOST>
export LDAP_SERVER_PORT=636
export LDAP_SERVER_IP_ADDRESS=<LDAP_SERVER_IP_ADDRESS>
export LDAP_SERVER_BASEDN="<LDAP_SERVER_BASEDN>"
export LDAP_SERVER_BINDDN="<LDAP_SERVER_BINDDN>"
export LDAP_SERVER_BINDPASSWORD=<LDAP_SERVER_BINDPASSWORD>
export ADMIN_GROUP_NAME="AAD DC Administrators"
export KEYSTORE_NAME=<KEYSTORE_NAME>
export KEYSTORE_PASS=<KEYSTORE_PASS>
```

## Prepare your application

The application `<path-to-repo>/2-simple` used in the [previous guide](howto-deploy-java-liberty-app.md#prepare-the-liberty-application) hasn't enabled authentication and authorization for security. To make it being protected by Microsoft Entra, a number of files need to be updated or created:

| File Name             | Source Path                     | Destination Path              | Operation  | Description           |
|-----------------------|---------------------------------|-------------------------------|------------|-----------------------|  
| `server.xml` | [`<path-to-repo>/2-simple/src/main/liberty/config/server.xml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/src/main/liberty/config/server.xml) | [`<path-to-repo>/3-integration/aad-ldap/src/main/liberty/config/server.xml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/aad-ldap/src/main/liberty/config/server.xml) | Updated | Add `ldapRegistry-3.0`, `transportSecurity-1.0`, `appSecurity-5.0`, `jwt-1.0`, `mpJwt-2.1` features and their configurations. |
| `web.xml` | [`<path-to-repo>/2-simple/src/main/webapp/WEB-INF/web.xml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/src/main/webapp/WEB-INF/web.xml) | [`<path-to-repo>/3-integration/aad-ldap/src/main/webapp/WEB-INF/web.xml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/aad-ldap/src/main/webapp/WEB-INF/web.xml) | Updated | Add `security-role` and `security-constraint` for accessing web resources of the application. |
| `CafeRequestFilter.java` | | [`<path-to-repo>/3-integration/aad-ldap/src/main/java/cafe/web/view/CafeRequestFilter.java`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/aad-ldap/src/main/java/cafe/web/view/CafeRequestFilter.java) | New | A client request filter for generating JWT token with **groups claim**, and adding it in **HTTP Authorization Header** for outbound requests. |
| `Cafe.java` | [`<path-to-repo>/2-simple/src/mainjava/cafe/web/view/Cafe.java`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/src/main/java/cafe/web/view/Cafe.java) | [`<path-to-repo>/3-integration/aad-ldap/src/main/java/cafe/web/view/Cafe.java`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/aad-ldap/src/main/java/cafe/web/view/Cafe.java) | Updated | Register `CafeRequestFilter` for intercepting internal REST calls, add new APIs to get principal name of logged-on user and flag indicating whether the logged-on user can delete existing coffees or not. |
| `CafeResource.java` | [`<path-to-repo>/2-simple/src/main/java/cafe/web/rest/CafeResource.java`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/src/main/java/cafe/web/rest/CafeResource.java) | [`<path-to-repo>/3-integration/aad-ldap/src/main/java/cafe/web/rest/CafeResource.java`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/aad-ldap/src/main/java/cafe/web/rest/CafeResource.java) | Updated | Inject `JsonWebToken` to verify the **groups claim** of the token for RBAC. |
| `messages.properties` | [`<path-to-repo>/2-simple/src/main/resources/cafe/web/messages.properties`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/src/main/resources/cafe/web/messages.properties) | [`<path-to-repo>/3-integration/aad-ldap/src/main/resources/cafe/web/messages.properties`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/aad-ldap/src/main/resources/cafe/web/messages.properties) | Updated | Parameterize display name using the name of the logged-on user. |
| `messages_es.properties` | [`<path-to-repo>/2-simple/src/main/resources/cafe/web/messages_es.properties`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/src/main/resources/cafe/web/messages_es.properties) | [`<path-to-repo>/3-integration/aad-ldap/src/main/resources/cafe/web/messages_es.properties`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/aad-ldap/src/main/resources/cafe/web/messages_es.properties) | Updated | Parameterize display name using the name of the logged-on user. |
| `index.xhtml` | [`<path-to-repo>/2-simple/src/main/webapp/index.xhtml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/src/main/webapp/index.xhtml) | [`<path-to-repo>/3-integration/aad-ldap/src/main/webapp/index.xhtml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/aad-ldap/src/main/webapp/index.xhtml) | Updated | Display principal name of the logged-on user. Disable coffee delete button if the logged-on user is not authorized. |
| `pom.xml` | [`<path-to-repo>/2-simple/pom.xml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/pom.xml) | [`<path-to-repo>/3-integration/aad-ldap/pom.xml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/aad-ldap/pom.xml) | Updated | Add new properties and dependencies for LDAP Registry, and add new dependency for **Eclipse MicroProfile**. |

For reference, these changes have already been applied in `<path-to-repo>/3-integration/aad-ldap` of your local clone.

To run the sample application with `liberty-maven-plugin` in your local machine, execute the following commands:

```bash
cd <path-to-repo>/3-integration/aad-ldap
mvn clean package
mvn liberty:dev
```

> [!NOTE]
>
> * If you used a non-routable domain name for LDAP server, add a host name entry with LDAP external IP address to your operating system to workaround the DNS resolution issue. For example, add `<LDAP_SERVER_IP_ADDRESS> <LDAP_SERVER_HOST>` to `/etc/hosts` for most Unix-like systems, or `C:\Windows\System32\drivers\etc\hosts` for Windows system.

Once the application is up and running, open [https://localhost:9443](https://localhost:9443) in the **InPrivate** window of **Microsoft Edge**, verify the application is secured by Microsoft Entra OpenID Connect.

1. Sign in using the **Name** of an Microsoft Entra ID user, who doesn't belong to the admin group you specified before.
2. You will see the name of your Microsoft Entra ID user displayed in the application home page, where the coffee **Delete** button is **disabled**.

   ![delete-button-disabled](./media/howto-integrate-aad-ldap/delete-button-disabled.png)
3. Close the **InPrivate** window > open a new **InPrivate** window > sign in using the **Name** of another Microsoft Entra ID user, who does belong to the admin group you specified before.
4. You will see the name of your Microsoft Entra ID user displayed in the application home page, where the coffee **Delete** button is **enabled** now.

   ![delete-button-enabled](./media/howto-integrate-aad-ldap/delete-button-enabled.png)
5. Press **Control-C** to stop the application and Open Liberty server.

## Prepare application image

To build the application image, Dockerfile needs to be prepared in advance:

| File Name             | Source Path                     | Destination Path              | Operation  | Description           |
|-----------------------|---------------------------------|-------------------------------|------------|-----------------------|  
| `Dockerfile` | [`<path-to-repo>/2-simple/Dockerfile`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/Dockerfile) | [`<path-to-repo>/3-integration/aad-ldap/Dockerfile`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/aad-ldap/Dockerfile) | Duplicated | Copied from `2-simple/Dockerfile`, which is based on Open Liberty base image. |
| `Dockerfile-wlp` | [`<path-to-repo>/2-simple/Dockerfile-wlp`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/Dockerfile-wlp) | [`<path-to-repo>/3-integration/aad-ldap/Dockerfile-wlp`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/aad-ldap/Dockerfile-wlp) | Duplicated | Copied from `2-simple/Dockerfile-wlp`, which is based on WebSphere Liberty base image. |

Follow steps below to build the application image:

```bash
# Change directory to "<path-to-repo>/3-integration/aad-ldap"
cd <path-to-repo>/3-integration/aad-ldap

# Build application image with Open Liberty base image
docker build -t javaee-cafe-aad-ldap:1.0.0 --pull .

# Alternatively, you can build application image with WebSphere Liberty base image
docker build -t javaee-cafe-aad-ldap:1.0.0 --pull --file Dockerfile-wlp .
```

After the application image is built, run with your local Docker to verify whether it works.

1. Run the following command in your console.

   ```bash
   docker run -it --rm -p 9443:9443 -e KEYSTORE_NAME=${KEYSTORE_NAME} -e KEYSTORE_PASS=${KEYSTORE_PASS} -e LDAP_SERVER_HOST=${LDAP_SERVER_HOST} -e LDAP_SERVER_PORT=${LDAP_SERVER_PORT} -e LDAP_SERVER_BASEDN="$LDAP_SERVER_BASEDN" -e LDAP_SERVER_BINDDN="$LDAP_SERVER_BINDDN" -e LDAP_SERVER_BINDPASSWORD=${LDAP_SERVER_BINDPASSWORD} -e ADMIN_GROUP_NAME="$ADMIN_GROUP_NAME" --mount type=bind,source=$(pwd)/src/main/liberty/config/${KEYSTORE_NAME},target=/config/${KEYSTORE_NAME} javaee-cafe-aad-ldap:1.0.0
   ```

   > [!NOTE]
   >
   > * If you used a non-routable domain name for LDAP server, add a host name entry with LDAP external IP address to the container to workaround the DNS resolution issue. For example, add `--add-host <LDAP_SERVER_HOST>:<LDAP_SERVER_IP_ADDRESS>` to the arguments list.

2. Wait for Liberty to start and the application to deploy successfully.
3. Open [https://localhost:9443/](https://localhost:9443/) in your browser to visit the application home page.
4. Press **Control-C** to stop the application and Liberty server.

When you're satisfied with the state of the application, build and push the container image to the built-in container image registry by following the instructions below:

1. Make sure you have already signed in to the OpenShift CLI using the `kubeadmin` credentials. If not, follow [Connect using the OpenShift CLI](https://learn.microsoft.com/en-us/azure/openshift/tutorial-connect-cluster#connect-using-the-openshift-cli) to sign using `oc login` command.
1. Run the following commands to build and push the image to the container registry of your Azure Red Hat OpenShift 4 cluster:

   ```bash
   # Change directory to "<path-to-repo>/3-integration/aad-ldap"
   cd <path-to-repo>/3-integration/aad-ldap

   # If you are building with the Open Liberty base image, the existing Dockerfile is ready for you

   # If you are building with the WebSphere Liberty base image, uncomment and execute the following two commands to rename Dockerfile-wlp to Dockerfile
   # mv Dockerfile Dockerfile.backup
   # mv Dockerfile-wlp Dockerfile

   # Change project to open-liberty-demo you created before
   oc project open-liberty-demo

   # Create an image stream
   oc create imagestream javaee-cafe-aad-ldap

   # Create a build configuration that specifies the image stream tag of the build output
   oc new-build --name javaee-cafe-aad-ldap-config --binary --strategy docker --to javaee-cafe-aad-ldap:1.0.0

   # Start the build to upload local contents, containerize, and output to the image stream tag specified before
   oc start-build javaee-cafe-aad-ldap-config --from-dir . --follow
   ```

## Deploy sample application

To integrate the application with Microsoft Entra OpenID Connect on the ARO 4 cluster, a number of Kubernetes resource YAML files need to be updated or created:

| File Name             | Source Path                     | Destination Path              | Operation  | Description           |
|-----------------------|---------------------------------|-------------------------------|------------|-----------------------|  
| `aad-ldap-secret.yaml` | | [`<path-to-repo>/3-integration/aad-ldap/aad-ldap-secret.yaml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/aad-ldap/aad-ldap-secret.yaml) | New | A Kubernetes **Secret** resource with user input data, including `ldap.server.host`, `ldap.server.port`, `ldap.server.baseDN`, `ldap.server.bindDN`, `ldap.server.bindPassword`, `keystore.name`, `keystore.pass`, and `admin.group.name`. |
| `tls-crt-secret.yaml` | | [`<path-to-repo>/3-integration/tls-crt-secret.yaml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/aad-ldap/tls-crt-secret.yaml) | New | A Kubernetes **Secret** resource with user input data, including `ca.crt`, `destCA.crt`, `tls.crt`, and `tls.key`. |
| `openlibertyapplication.yaml` | [`<path-to-repo>/2-simple/openlibertyapplication.yaml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/2-simple/openlibertyapplication.yaml) | [`<path-to-repo>/3-integration/aad-ldap/openlibertyapplication.yaml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/aad-ldap/openlibertyapplication.yaml) | Updated | Add environment variables whose values are from Secret `aad-ldap-secret`. Mount key store file from ConfigMap `keystore-config`. Specify existing certificate for **Route** and **Service** of **OpenLibertyApplication** custom resource. |
| `openlibertyapplication-hosts.yaml` | | [`<path-to-repo>/3-integration/aad-ldap/openlibertyapplication-hosts.yaml`](https://github.com/Azure-Samples/open-liberty-on-aro/blob/master/3-integration/aad-ldap/openlibertyapplication-hosts.yaml) | New | Inherit from `openlibertyapplication.yaml` and add an init container which is responsible for modifying `/etc/hosts` by adding the host name entry with LDAP external IP address. |

For reference, these changes have already been applied in `<path-to-repo>/3-integration/aad-ldap` of your local clone.

Now you can deploy the sample Liberty application to the ARO 4 cluster with the following steps.

1. Make sure you have already signed in to the OpenShift CLI using the `kubeadmin` credentials. If not, follow [Connect using the OpenShift CLI](https://learn.microsoft.com/en-us/azure/openshift/tutorial-connect-cluster#connect-using-the-openshift-cli) to sign using `oc login` command.
1. Run the following commands to deploy the application.

   ```bash
   # Change directory to "<path-to-repo>/3-integration/aad-ldap"
   cd <path-to-repo>/3-integration/aad-ldap

   # Change project to "open-liberty-demo"
   oc project open-liberty-demo

   # Make sure the following environment variables have already been defined before. They will be passed to secret "aad-ldap-secret", ConfigMap "keystore-config" and OpenLibertyApplication "javaee-cafe-aad-ldap"
   # export LDAP_SERVER_HOST=<LDAP_SERVER_HOST>
   # export LDAP_SERVER_IP_ADDRESS=<LDAP_SERVER_IP_ADDRESS>
   # export LDAP_SERVER_PORT=<LDAP_SERVER_PORT>
   # export LDAP_SERVER_BASEDN=<LDAP_SERVER_BASEDN>
   # export LDAP_SERVER_BINDDN=<LDAP_SERVER_BINDDN>
   # export LDAP_SERVER_BINDPASSWORD=<LDAP_SERVER_BINDPASSWORD>
   # export KEYSTORE_NAME=<KEYSTORE_NAME>
   # export KEYSTORE_PASS=<KEYSTORE_PASS>
   # export ADMIN_GROUP_NAME=<ADMIN_GROUP_NAME>

   # Create secret "aad-ldap-secret"
   envsubst < aad-ldap-secret.yaml | oc create -f -

   # Create ConfigMap keystore-config
   oc create configmap keystore-config --from-file=src/main/liberty/config/${KEYSTORE_NAME}

   # Create TLS private key and certificate, which is also used as CA certificate for testing purpose
   # Note that the CN is set to "localhost" as the view will invoke the REST API with "localhost" as the host
   openssl req -x509 -subj "/C=US/ST=majguo/L=OpenLiberty/O=demo/CN=localhost" -sha256 -nodes -days 365 -newkey rsa:2048 -keyout tls.key -out tls.crt

   # Create environment variables which will be passed to secret "tls-crt-secret"
   export CA_CRT=$(cat tls.crt | base64 -w 0)
   export DEST_CA_CRT=$(cat tls.crt | base64 -w 0)
   export TLS_CRT=$(cat tls.crt | base64 -w 0)
   export TLS_KEY=$(cat tls.key | base64 -w 0)

   # Create secret "tls-crt-secret"
   envsubst < tls-crt-secret.yaml | oc create -f -

   # Note: change "IS_DOMAIN_NAME_ROUTABLE" to true if the domain name of the LDAP server is routable
   IS_DOMAIN_NAME_ROUTABLE=false

   # Create OpenLibertyApplication "javaee-cafe-aad-ldap"
   if [ "$IS_DOMAIN_NAME_ROUTABLE" = true ]; then
       appName=javaee-cafe-aad-ldap
       envsubst < openlibertyapplication.yaml | oc create -f -
   else
       appName=javaee-cafe-aad-ldap-hosts
       envsubst < openlibertyapplication-hosts.yaml | oc create -f -
   fi

   # Check if OpenLibertyApplication instance is created
   oc get openlibertyapplication $appName

   # Check if deployment created by Operator is ready
   oc get deployment $appName

   # Get host of the route
   HOST=$(oc get route $appName --template='{{ .spec.host }}')
   echo "Route Host: https://$HOST"
   ```

Once the Liberty Application is up and running, replace **\<Route_Host>** with the console output of **Route Host** for `https://<Route_Host>`, and open it in your browser to visit the application home page.

### Lock down and secure LDAP access over the internet

While standing up the secure LDAP in the preceding steps, you had set the source as **Any** for the new added inbound security rule in the network security group.  Now that the Liberty application has been deployed and connected to LDAP, obtain the public IP address of `ingressProfile` for the ARO 4 cluster.

1. Log in to Azure CLI by running `az login` using your subscription in the console.
2. Run `az aro list -o table` to get list of deployed ARO 4 clusters. Find resource group name and cluster name from your specific ARO 4 cluster.
3. Run `az resource show -g <resource-group-name> -n <cluster-name> --resource-type "Microsoft.RedHatOpenShift/openShiftClusters --query "properties.apiserverProfile.ip" -o tsv` to get the IP address of `apiserverProfile` for your ARO 4 cluster.

Revisit [Lock down secure LDAP access over the internet](https://learn.microsoft.com/azure/active-directory-domain-services/tutorial-configure-ldaps#lock-down-secure-ldap-access-over-the-internet) and change **Any** to the IP address of `apiserverProfile` for the ARO 4 cluster.

## Next steps

In this guide, you learned how to:
> [!div class="checklist"]
>
> * Configure secure LDAP for a Microsoft Entra Domain Services managed domain
> * Prepare your application
> * Prepare application image
> * Deploy sample application

Advance to these guides, which integrate Liberty application with other Azure services:
> [!div class="nextstepaction"]
> [Integrate your Liberty application with Elasticsearch stack](howto-integrate-elasticsearch-stack.md)

> [!div class="nextstepaction"]
> [Integrate your Liberty application with Azure Database for PostgreSQL](howto-integrate-azure-database-for-postgres.md)

> [!div class="nextstepaction"]
> [Integrate your Liberty application with Microsoft Entra OpenID Connect](howto-integrate-aad-ldap.md)

If you've finished all of above guides, advance to the complete guide, which incorporates all of Azure service integrations:
> [!div class="nextstepaction"]
> [Integrate your Liberty application with different Azure services](howto-integrate-all.md)

Here are references used in this guide:

* [Tutorial: Create and configure a Microsoft Entra Domain Services managed domain](https://learn.microsoft.com/entra/identity/domain-services/tutorial-create-instance)
* [Tutorial: Configure secure LDAP for a Microsoft Entra Domain Services managed domain](https://learn.microsoft.com/entra/identity/domain-services/tutorial-configure-ldaps)
* [Configuring LDAP user registries in Liberty](https://www.ibm.com/support/knowledgecenter/SSEQTP_liberty/com.ibm.websphere.wlp.doc/ae/twlp_sec_ldap.html)
* [SSL configuration attributes](https://www.ibm.com/support/knowledgecenter/SSEQTP_liberty/com.ibm.websphere.wlp.doc/ae/rwlp_ssl.html)
* [Configuring the MicroProfile JSON Web Token](https://www.ibm.com/support/knowledgecenter/SSEQTP_liberty/com.ibm.websphere.wlp.doc/ae/twlp_sec_json.html)
* [Configuring authorization for applications in Liberty](https://www.ibm.com/support/knowledgecenter/SSEQTP_liberty/com.ibm.websphere.wlp.doc/ae/twlp_sec_rolebased.html)
* [LDAP User Registry](https://openliberty.io/docs/20.0.0.10/reference/config/ldapRegistry.html)
* [Securing a web application](https://openliberty.io/guides/security-intro.html)
