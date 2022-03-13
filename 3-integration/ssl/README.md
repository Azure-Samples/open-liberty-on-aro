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

```
# Create secret "tls-crt-secret"
openssl req -x509 -sha256 -nodes -days 365 -newkey rsa:2048 -keyout tls.key -out tls.crt

export CA_CRT=$(cat tls.crt | base64 -w 0)
export DEST_CA_CRT=$(cat tls.crt | base64 -w 0)
export TLS_CRT=$(cat tls.crt | base64 -w 0)
export TLS_KEY=$(cat tls.key | base64 -w 0)
envsubst < tls-crt-secret.yaml | oc create -f -

envsubst < openlibertyapplication.yaml | oc create -f -
```

To test the deployment, copy the value of `HOST/PORT` of the route which routes traffic to/from the application:

```
oc get route javaee-cafe-ssl
```

Open `https://<copied-value>` in the browser to test the application.

### Clean up

To clean up all resources deployed to the ARO 4 cluster:

```
oc delete openlibertyapplication javaee-cafe-ssl
oc delete secret tls-crt-secret
oc delete buildconfig javaee-cafe-ssl
oc delete imagestream javaee-cafe-ssl
```

## References

See the following references for more information.

* [Specify your own certificates for the Service and Route](https://github.com/application-stacks/runtime-component-operator/blob/main/doc/user-guide-v1beta2.adoc#certificates)
* [TLS certificate configuration](https://github.com/OpenLiberty/ci.docker/blob/master/SECURITY.md#tls-certificate-configuration)