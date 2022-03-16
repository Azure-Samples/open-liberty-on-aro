# Enable SSL for the Open Liberty application

This is an Open Liberty sample enabled with SSL support. Check out the source code and follow steps below to run the sample locally, run the sample in local Docker, and deploy the sample on the ARO 4 cluster.

## Run the sample locally

Checkout the repo to local directory, switch directory to `<local-repo-clone/3-integration/ssl>`, then run the following commands to build the sample and run it with liberty maven plugin:

```
mvn clean package
mvn liberty:dev
```

You should see message "The defaultServer server is ready to run a smarter planet" output in the command terminal.

Open https://localhost:9443 in browser to visit the app.

Press "Ctrl+C" to stop the app. 

## Run the sample in local Docker

Build the application image from Open Liberty base image, and run it in local Docker:

```
docker build -t javaee-cafe-ssl:1.0.0 .
docker run -it --rm -p 9443:9443 javaee-cafe-ssl:1.0.0
```

Alternatively, you can also build the application image from WebSphere Liberty base image, and run it in local Docker:

```
docker build -t javaee-cafe-ssl-wlp:1.0.0 --file=Dockerfile-wlp .
docker run -it --rm -p 9443:9443 javaee-cafe-ssl-wlp:1.0.0
```

Similarly, you should see message "The defaultServer server is ready to run a smarter planet" output in the command terminal.

Open https://localhost:9443 in browser to visit the app.

Press "Ctrl+C" to stop the app.  

## Deploy the sample on the ARO 4 cluster

You should already deployed an ARO 4 cluster, created a project `open-liberty-demo`, and a user who has been granted `admin` role to project `open-liberty-demo`.

First, sign in with the user and switch to project `open-liberty-demo`:

```
DEMO_PROJECT=open-liberty-demo
oc login -u <user-name> -p <password> --server=<api-server-url>
oc project $DEMO_PROJECT
```

Then create an image stream, a build config and then start the build to upload the contents for building the image: 

```
oc create imagestream javaee-cafe-ssl
oc new-build --name javaee-cafe-ssl --binary --strategy docker --to javaee-cafe-ssl:1.0.0
oc start-build javaee-cafe-ssl --from-dir . --follow
```

Finally, deploy the sample to the ARO 4 cluster:

### Option 1: create a passthrough route

You can configure a secure route using passthrough termination. With passthrough termination, encrypted traffic is sent straight to the destination without the router providing TLS termination. Therefore no key or certificate is required on the route.

```
oc apply -f openlibertyapplication-passthrough.yaml
```

To test the deployment, copy the value of `HOST/PORT` of the route which routes traffic to/from the application:

```
oc get route javaee-cafe-ssl-passthrough
```

Open `https://<copied-value>` in the browser to test the application.

Alternatively, you can also open the copied value in the browser, and it's expected to be automatically redirected to `https` endpoint accordingly. However, the fix for the open issue [insecureEdgeTerminationPolicy not set to redirect on passthrough route](https://github.com/OpenLiberty/open-liberty-operator/issues/297#issuecomment-996787319) seems not released in Open Liberty Operator 0.8.0 which is currently available from the OperatorHub, so redirect won't work now.

### Option 2: create a re-encrypt route with a custom certificate

You can also configure a secure route using reencrypt TLS termination with a custom certificate.

```
# Create secret "tls-crt-secret"
openssl req -x509 -sha256 -nodes -days 365 -newkey rsa:2048 -keyout tls.key -out tls.crt

export CA_CRT=$(cat tls.crt | base64 -w 0)
export DEST_CA_CRT=$(cat tls.crt | base64 -w 0)
export TLS_CRT=$(cat tls.crt | base64 -w 0)
export TLS_KEY=$(cat tls.key | base64 -w 0)
envsubst < tls-crt-secret.yaml | oc apply -f -

oc apply -f openlibertyapplication-reencrypt.yaml
```

To test the deployment, copy the value of `HOST/PORT` of the route which routes traffic to/from the application:

```
oc get route javaee-cafe-ssl-reencrypt
```

Open `https://<copied-value>` in the browser to test the application.

Alternatively, you can just open the copied value in the browser, it will be automatically redirected to `https` endpoint accordingly.

## Automatically roll the new image out to the deployment

When an image stream tag is updated to point to a new image, OpenShift Container Platform can automatically take action to roll the new image out to resources that were using the old image, e.g,  OpenShift Container Platform resources including deployment configurations and build configurations.

For CRD OpenLibertyApplication supported by the Open Liberty Operator, it's tested and verified that it can also be automatically triggered by changes to image stream tags.

Follow steps below to automatically roll the new image out to the deployment.

1. Open a new command terminal, sign in with the user, then watch the status of the pod of the deployment to monitor the rolling out progress:

   ```
   oc get pod -w
   ```

1. Make some code changes that are visible in the UI.
1. Switch to the original command terminal, build and run locally to verify if it works:

   ```
   mvn clean package
   mvn liberty:dev
   ```

1. start a new build using the same build configuration creatd before:

   ```
   oc start-build javaee-cafe-ssl --from-dir . --follow
   ```

1. Once the build completes and new image rolling out is done, refresh the UI of the sample app to check if the changes take effect. Press "Ctrl+C" in the new command terminal to stop the monitoring of pod status.

### Clean up

To clean up all resources deployed to the ARO 4 cluster:

```
oc delete openlibertyapplication javaee-cafe-ssl-passthrough
oc delete openlibertyapplication javaee-cafe-ssl-reencrypt
oc delete secret tls-crt-secret
oc delete buildconfig javaee-cafe-ssl
oc delete imagestream javaee-cafe-ssl
```

## References

See the following references for more information.

* [Specify your own certificates for the Service and Route](https://github.com/application-stacks/runtime-component-operator/blob/main/doc/user-guide-v1beta2.adoc#certificates)
* [TLS certificate configuration](https://github.com/OpenLiberty/ci.docker/blob/master/SECURITY.md#tls-certificate-configuration)
* [Secured routes](https://docs.openshift.com/container-platform/4.9/networking/routes/secured-routes.html)
* [Secured routes in OpenShift 3.7](https://docs.openshift.com/container-platform/3.7/architecture/networking/routes.html#secured-routes)
* [Triggering updates on image stream changes](https://docs.openshift.com/container-platform/4.9/openshift_images/triggering-updates-on-imagestream-changes.html)