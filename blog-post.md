The foundational elements of technology in this article include the following:

* [Open Liberty](https://openliberty.io): Open Liberty is an IBM Open Source project that implements the Eclipse MicroProfile specifications and is also Java/Jakarta EE compatible. Open Liberty is fast to start up with a low memory footprint and supports live reloading for quick iterative development. It is simple to add and remove features from the latest versions of MicroProfile and Java/Jakarta EE. Zero migration lets you focus on what's important, not the APIs changing under you.
* [WebSphere Liberty](https://www.ibm.com/cloud/websphere-liberty): IBM WebSphere Liberty architecture shares the same code base as the open sourced Open Liberty server runtime, which provides additional benefits such as low-cost experimentation, customization and seamless migration from open source to production.
* [Azure Red Hat OpenShift](https://azure.microsoft.com/services/openshift/): Azure Red Hat OpenShift provides flexible, self-service deployment of fully managed OpenShift clusters. Maintain regulatory compliance and focus on your application development, while your master, infrastructure, and application nodes are patched, updated, and monitored by both Microsoft and Red Hat.

## Prepare the Liberty application

We will use a Java EE 8 application as our example in this guide. Open Liberty is a [Java EE 8 full profile](https://javaee.github.io/javaee-spec/javadocs/) compatible server, so it can easily run the application.  Open Liberty is also [Jakarta EE 8 full profile compatible](https://jakarta.ee/specifications/platform/8/apidocs/).  If you already have a Java EE, Jakarta EE or MicroProfile application running on an existing server (for example, IBM WebSphere Application Server, Oracle WebLogic Server, WildFly, JBoss EAP, and so on), only minimal changes should be necessary to make the application run on Liberty runtime.

### Sample application

Within the git repo you cloned in the Prerequisites section, change directory to `1-start`. The sample application uses Maven at build time and only Java EE 8 APIs (JAX-RS, EJB, CDI, JSON-B, JSF, Bean Validation) at runtime. This standard Java EE app will be used as a starting point to demonstrate the ease of migration to the Liberty runtime. Here is the project structure:

```Text
├── pom.xml                                         # Maven POM file
└── src
    └── main
        ├── java
        │   └── cafe
        │       ├── model
        │       │   ├── CafeRepository.java         # Cafe CRUD repository (in-memory)
        │       │   └── entity
        │       │       └── Coffee.java             # Coffee entity
        │       └── web
        │           ├── rest
        │           │   └── CafeResource.java       # Cafe CRUD REST APIs
        │           └── view
        │               └── Cafe.java               # Cafe bean in JSF client
        ├── resources
        │   ├── META-INF
        │   └── cafe
        │       └── web
        │           ├── messages.properties         # Resource bundle in EN
        │           └── messages_es.properties      # Resource bundle in ES
        └── webapp
            ├── WEB-INF
            │   ├── faces-config.xml                # JSF configuration file specifying resource bundles and supported locales
            │   └── web.xml                         # Deployment descriptor for a Servlet-based Java web application
            └── index.xhtml                         # Home page for JSF client
```
