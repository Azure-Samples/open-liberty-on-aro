# Integrate your Open Liberty application with Azure managed databases

[Azure managed databases](https://azure.microsoft.com/product-categories/databases/) are managed, intelligent, and flexible cloud database services offered by Microsoft Azure. In this guide, you will integrate your Open Liberty application with Azure managed databases to enable data persistence. The Open Liberty application is running on an Azure Red Hat OpenShift (ARO) 4 cluster. You learn how to:
> [!div class="checklist"]
>
> * Connect your application to Azure SQL Database
> * Connect your application to Azure Database for PostgreSQL

## Before you begin

In previous guide, a Java application, which is running inside Open Liberty runtime, is deployed to an ARO 4 cluster. If you have not done these steps, start with [Deploy a Java application with Open Liberty on an Azure Red Hat OpenShift 4 cluster](howto-deploy-java-openliberty-app.md) and return here to continue.

## Connect your application to Azure SQL Database

Part of the Azure SQL family, [Azure SQL Database](https://azure.microsoft.com/services/sql-database/) is the intelligent, scalable, and relational database service built for the cloud. Using Azure SQL Database for persisting your application's data, you can focus on application development as a developer.

### Create an Azure SQL Database single database

Follow the instructions below to set up an Azure SQL Database single database for connectivity.

1. Create a single database in Azure SQL Database by following "[Quickstart: Create an Azure SQL Database single database](https://docs.microsoft.com/azure/azure-sql/database/single-database-create-quickstart)".
   > [!NOTE]
   >
   > * In configuring **Basics** step, write down **Database name**, ***Server name**.database.windows.net*, **Server admin login** and **Password**.
   > * In configuring **Networking** step, set **Allow Azure services and resources to access this server** to **Yes**. It will allow access from your Open Liberty application running on ARO 4 cluster to this database server.
   >   ![create-sql-database-networking](./media/howto-integrate-azure-managed-databases/create-sql-database-networking.png)

2. Once your database is created, open **your SQL server** > **Firewalls and virtual networks** > Set **Minimal TLS Version** to **>1.0** > Click **Save**.
   ![sql-database-minimum-TLS-version](./media/howto-integrate-azure-managed-databases/sql-database-minimum-TLS-version.png)
3. Open **your SQL database** > **Connection strings** > Select **JDBC**. Write down the **Port number** following sql server address. For example, **1433** is the port number in the example below.
   ![sql-server-jdbc-connection-string](./media/howto-integrate-azure-managed-databases/sql-server-jdbc-connection-string.png)

### Prepare your Open Liberty application

The application `<path-to-repo>/2-simple` used in the [previous guide](howto-deploy-java-openliberty-app.md) has no database connectivity. Follow instructions below to make it connect to Azure SQL Database.

1. Update `server.xml` by enabling **jpa-2.2** feature and adding **dataSource** configuration.

   ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <server description="defaultServer">
        <featureManager>
            ...
            <!-- Enable jpa-2.2 -->
            <feature>jpa-2.2</feature>
        </featureManager>

        ...

        <!-- Configure data source which connects to Azure SQL Database -->
        <dataSource id="JavaEECafeDB" jndiName="jdbc/JavaEECafeDB">
            <jdbcDriver libraryRef="driver-library" />
            <properties.microsoft.sqlserver
                serverName="${db.server.name}"
                portNumber="${db.port.number}"
                databaseName="${db.name}"
                user="${db.user}"
                password="${db.password}" />
        </dataSource>

        <!-- Configure JDBC driver library -->
        <library id="driver-library">
            <fileset dir="${shared.resource.dir}" includes="*.jar" />
        </library>
    </server>
   ```

   Refer to `<path-to-repo>/3-integration/connect-db/mssql/src/main/liberty/config/server.xml` for full changes.

2. Add a new configuration file `persistence.xml` to `<path-to-repo>/2-simple/src/main/resources/META-INF` to configure data persistence schema for your application.

   ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <persistence version="2.1"
        xmlns="http://xmlns.jcp.org/xml/ns/persistence"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence http://xmlns.jcp.org/xml/ns/persistence/persistence_2_1.xsd">
        <persistence-unit name="coffees">
            <jta-data-source>jdbc/JavaEECafeDB</jta-data-source>
            <properties>
                <property
                    name="javax.persistence.schema-generation.database.action"
                    value="create" />
                <property name="openjpa.jdbc.SynchronizeMappings"
                    value="buildSchema" />
                <property name="eclipselink.logging.level.sql" value="FINE" />
                <property name="eclipselink.logging.parameters" value="true" />
                <property name="hibernate.show_sql" value="true" />
            </properties>
        </persistence-unit>
    </persistence>
   ```

   Refer to `<path-to-repo>/3-integration/connect-db/mssql/src/main/resources/META-INF/persistence.xml` for full changes.

3. Update `Coffee.java` to make it as **a JPA Entity**. The following code snippet lists the major changes:

   ```java
    package cafe.model.entity;

    import javax.persistence.Entity;
    import javax.persistence.GeneratedValue;
    import javax.persistence.Id;
    import javax.persistence.NamedQuery;

    ...

    @XmlRootElement
    @Entity
    @NamedQuery(name = "findAllCoffees", query = "SELECT o FROM Coffee o")
    public class Coffee implements Serializable {

        private static final long serialVersionUID = 1L;

        @Id
        @GeneratedValue
        private Long id;

        ...

    }
   ```

   Refer to `<path-to-repo>/3-integration/connect-db/mssql/src/main/java/cafe/model/entity/Coffee.java` for full changes.

4. Update `CafeRepository.java` to make it as **a Stateless Bean**, which implements create, read, update, and delete coffees using `javax.persistence.EntityManager` and `javax.persistence.PersistenceContext` APIs.

   ```java
    import javax.ejb.Stateless;
    import javax.persistence.EntityManager;
    import javax.persistence.PersistenceContext;

    import cafe.model.entity.Coffee;
    ...

    @Stateless
    public class CafeRepository {

        private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

        @PersistenceContext
        private EntityManager entityManager;

        public List<Coffee> getAllCoffees() {
            logger.log(Level.INFO, "Finding all coffees.");

            return this.entityManager.createNamedQuery("findAllCoffees", Coffee.class).getResultList();
        }

        public Coffee persistCoffee(Coffee coffee) {
            logger.log(Level.INFO, "Persisting the new coffee {0}.", coffee);

            this.entityManager.persist(coffee);
            return coffee;
        }

        public void removeCoffeeById(Long coffeeId) {
            logger.log(Level.INFO, "Removing a coffee {0}.", coffeeId);

            Coffee coffee = entityManager.find(Coffee.class, coffeeId);
            this.entityManager.remove(coffee);
        }

        public Coffee findCoffeeById(Long coffeeId) {
            logger.log(Level.INFO, "Finding the coffee with id {0}.", coffeeId);

            return this.entityManager.find(Coffee.class, coffeeId);
        }
    }
   ```

   Refer to `<path-to-repo>/3-integration/connect-db/mssql/src/main/java/cafe/model/CafeRepository.java` for full changes.

5. Update `pom.xml` to include new properties for configuring database connection, new dependency for JDBC driver, and new plugin for `maven-dependency-plugin`.

   ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
      ...

      <properties>
        ...

        <!-- Maven properties for configuring database connection -->
        <db.server.name></db.server.name>
        <db.port.number></db.port.number>
        <db.name></db.name>
        <db.user></db.user>
        <db.password></db.password>
      </properties>
      <dependencies>
        ...

        <!-- Dependency for "mssql-jdbc" driver -->
        <dependency>
          <groupId>com.microsoft.sqlserver</groupId>
          <artifactId>mssql-jdbc</artifactId>
          <version>8.2.2.jre8</version>
          <type>jar</type>
        </dependency>
      </dependencies>
      <profiles>
        <profile>
          ...

          <build>
            <plugins>
              <plugin>
                ...

                <executions>
                  <execution>
                    ...

                    <configuration>
                      ...

                      <!-- Open Liberty server bootstrap properties for configuring database connection -->
                      <bootstrapProperties>
                        <db.server.name>${db.server.name}</db.server.name>
                        <db.port.number>${db.port.number}</db.port.number>
                        <db.name>${db.name}</db.name>
                        <db.user>${db.user}</db.user>
                        <db.password>${db.password}</db.password>
                      </bootstrapProperties>
                    </configuration>
                  </execution>
                </executions>
              </plugin>

              <!-- Plugin for "maven-dependency-plugin" to copy jdbc driver -->
              <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <version>3.1.2</version>
                <executions>
                  <execution>
                    <id>copy-jdbc-driver</id>
                    <phase>package</phase>
                    <goals>
                      <goal>copy-dependencies</goal>
                    </goals>
                    <configuration>
                      <includeArtifactIds>mssql-jdbc</includeArtifactIds>
                      <outputDirectory>${project.build.directory}/liberty/wlp/usr/shared/resources</outputDirectory>
                    </configuration>
                  </execution>
                </executions>
              </plugin>
            </plugins>
          </build>
        </profile>
      </profiles>
    </project>
   ```

   Refer to `<path-to-repo>/3-integration/connect-db/mssql/pom.xml` for full changes.

6. Run your application with **liberty-maven-plugin** using the following commands:

   ```bash
   cd <path-to-repo>/2-simple
   mvn clean package
   mvn -Ddb.server.name=<Server name>.database.windows.net -Ddb.port.number=<Port number> -Ddb.name=<Database name> -Ddb.user=<Server admin login>@<Server name> -Ddb.password=<Password> liberty:dev
   ```

   > [!NOTE]
   >
   > * **Database name**, **Server name**, **Server admin login**, **Password** and **Port number** are properties you wrote down in previous step "[Create an Azure SQL Database single database](#create-an-azure-sql-database-single-database)".
   > * [Create a firewall rule](https://docs.microsoft.com/azure/azure-sql/database/firewall-create-server-level-portal-quickstart) for IP address of your client if you encountered the similar error below. Then re-run the application.
   >   * [ERROR   ] CWWJP9992E: Exception [EclipseLink-4002] (Eclipse Persistence Services - 2.7.7.v20200504-69f2c2b80d): org.eclipse.persistence.exceptions.DatabaseExceptionInternal Exception: java.sql.SQLException: Cannot open server 'xxxxxxx' requested by the login. Client with IP address 'xxx.xxx.xxx.xx' is not allowed to access the server.  To enable access, use the Windows Azure Management Portal or run sp_set_firewall_rule on the master database to create a firewall rule for this IP address or address range.  It may take up to five minutes for this change to take effect. ClientConnectionId:xxxx-xxxx-xxxx-xxxx-xxxx: SQL State = S0001, Error Code = 40,615

   Once the application is up and running, open [http://localhost:9080](http://localhost:9080) in your browser, verify the database connectivity works by creating new coffees and deleting existing coffees in the home page. Press **Control-C** to stop the application and Open Liberty server.

For reference, these changes have already been applied in `<path-to-repo>/3-integration/connect-db/mssql` of your local clone.

### Build application image

Here is the **Dockerfile** (located at `<path-to-repo>/3-integration/connect-db/mssql/Dockerfile`) for building the application image:

```Dockerfile
# open liberty base image
FROM openliberty/open-liberty:kernel-java8-openj9-ubi

# Add config, app and jdbc driver
COPY --chown=1001:0 src/main/liberty/config/server.xml /config/server.xml
COPY --chown=1001:0 target/javaee-cafe.war /config/apps/
COPY --chown=1001:0 mssql-jdbc-8.2.2.jre8.jar /opt/ol/wlp/usr/shared/resources/

# This script will add the requested XML snippets, grow image to be fit-for-purpose and apply interim fixes
RUN configure.sh
```

1. Change directory to `<path-to-repo>/3-integration/connect-db/mssql` of your local clone.
2. Download [mssql-jdbc-8.2.2.jre8.jar](https://repo1.maven.org/maven2/com/microsoft/sqlserver/mssql-jdbc/8.2.2.jre8/mssql-jdbc-8.2.2.jre8.jar) and put it to current working directory.
3. Run the following commands to build application image and push to your ACR instance.

   ```bash
   # Build project and generate war package
   mvn clean package

   # Build and tag application image
   docker build -t javaee-cafe-connect-db-mssql:1.0.0 --pull .

   # Create a new tag with your ACR instance info that refers to source image
   # Note: replace "${Container_Registry_URL}" with the fully qualified name of your ACR instance
   docker tag javaee-cafe-connect-db-mssql:1.0.0 ${Container_Registry_URL}/javaee-cafe-connect-db-mssql:1.0.0

   # Log in to your ACR instance
   # Note: replace "${Registry_Name}" with the name of your ACR instance
   az acr login -n ${Registry_Name}

   # Push image to your ACR instance
   # Note: replace "${Container_Registry_URL}" with the fully qualified name of your ACR instance
   docker push ${Container_Registry_URL}/javaee-cafe-connect-db-mssql:1.0.0
   ```

   > [!NOTE]
   >
   > * Replace **${Container_Registry_URL}** with the fully qualified name of your ACR instance.
   > * Replace **${Registry_Name}** with the name of your ACR instance.

### Run the application with Docker

Before deploying the containerized application to a remote cluster, run with your local Docker to verify whether it works.

1. Run `docker run -it --rm -p 9080:9080 -e DB_SERVER_NAME=<Server name>.database.windows.net -e DB_PORT_NUMBER=<Port number> -e DB_NAME=<Database name> -e DB_USER=<Server admin login>@<Server name> -e DB_PASSWORD=<Password> javaee-cafe-connect-db-mssql:1.0.0` in your console.
   > [!NOTE]
   >
   > * **Database name**, **Server name**, **Server admin login**, **Password** and **Port number** are properties you wrote down in previous step "[Create an Azure SQL Database single database](#create-an-azure-sql-database-single-database)".
   > * [Create a firewall rule](https://docs.microsoft.com/azure/azure-sql/database/firewall-create-server-level-portal-quickstart) for IP address of your client if you encountered the similar error below. Then re-run the application.
   >   * [ERROR   ] CWWJP9992E: Exception [EclipseLink-4002] (Eclipse Persistence Services - 2.7.7.v20200504-69f2c2b80d): org.eclipse.persistence.exceptions.DatabaseExceptionInternal Exception: java.sql.SQLException: Cannot open server 'xxxxxxx' requested by the login. Client with IP address 'xxx.xxx.xxx.xx' is not allowed to access the server.  To enable access, use the Windows Azure Management Portal or run sp_set_firewall_rule on the master database to create a firewall rule for this IP address or address range.  It may take up to five minutes for this change to take effect. ClientConnectionId:xxxx-xxxx-xxxx-xxxx-xxxx DSRA0010E: SQL State = S0001, Error Code = 40,615

2. Wait for Open Liberty to start and the application to deploy successfully.
3. Open [http://localhost:9080/](http://localhost:9080/) in your browser to visit the application home page.
4. Press **Control-C** to stop the application and Open Liberty server.

### Deploy the application to ARO 4 cluster

In this step, the application, which refers to the newly generated image, will be deployed to the ARO 4 cluster and connects to the Azure SQL Database for data persistence.

#### Create secret for database connection credentials

First, create a **Secret** to store database connection credentials. The YAML file is located at `<path-to-repo>/3-integration/connect-db/db-secret.yaml`.

```yaml
apiVersion: v1
kind: Secret
metadata:
  # - replace "${DB_Type}" with "mssql" for testing DB connection with Azure SQL
  name: db-secret-${DB_Type}
  namespace: open-liberty-demo
type: Opaque
stringData:
  db.server.name: ${DB_SERVER_NAME}
  db.port.number: "${DB_PORT_NUMBER}"
  db.name: ${DB_NAME}
  db.user: ${DB_USER}
  db.password: ${DB_PASSWORD}
```

> [!NOTE]
>
> * Replace **${DB_Type}** with **mssql**.
> * Replace **${DB_SERVER_NAME}** with ***Server name**.database.windows.net* you wrote down before.
> * Replace **${DB_PORT_NUMBER}** with **Port number** you wrote down before.
> * Replace **${DB_NAME}** with **Database name** you wrote down before.
> * Replace **${DB_USER}** with ***Server admin login**@**Server name*** you wrote down before.
> * Replace **${DB_PASSWORD}** with **Password** you wrote down before.

1. Change directory to `<path-to-repo>/3-integration/connect-db`.
2. Change project to **open-liberty-demo**:

   ```bash
   oc project open-liberty-demo
   ````

   > [!NOTE]
   >
   > * Refer to [Set up Azure Red Hat OpenShift cluster](howto-deploy-java-openliberty-app.md#set-up-azure-red-hat-openshift-cluster) on how to connect to the cluster.
   > * **open-liberty-demo** is already created in the [previous guide](howto-deploy-java-openliberty-app.md).

3. Create **Secret**:

   ```bash
   oc create -f ./db-secret.yaml
   ```

#### Deploy application

Now we can deploy the sample application, which connects to Azure SQL Database, using the YAML file located at `<path-to-repo>/3-integration/connect-db/openlibertyapplication.yaml`.

```yaml
apiVersion: openliberty.io/v1beta1
kind: OpenLibertyApplication
metadata:
  # Note:
  # - replace "${DB_Type}" with "mssql" for testing DB connection with Azure SQL
  name: javaee-cafe-connect-db-${DB_Type}
  namespace: open-liberty-demo
spec:
  replicas: 1
  # Note:
  # - replace "${Container_Registry_URL}" with your container registry URL
  # - replace "${Image_Name}" with "javaee-cafe-connect-db-mssql" for testing DB connection with Azure SQL
  applicationImage: ${Container_Registry_URL}/${Image_Name}:1.0.0
  pullSecret: registry-secret
  expose: true
  # Note:
  # - replace "${DB_Type}" with "mssql" for testing DB connection with Azure SQL
  env:
  - name: DB_SERVER_NAME
    valueFrom:
      secretKeyRef:
        name: db-secret-${DB_Type}
        key: db.server.name
  - name: DB_PORT_NUMBER
    valueFrom:
      secretKeyRef:
        name: db-secret-${DB_Type}
        key: db.port.number
  - name: DB_NAME
    valueFrom:
      secretKeyRef:
        name: db-secret-${DB_Type}
        key: db.name
  - name: DB_USER
    valueFrom:
      secretKeyRef:
        name: db-secret-${DB_Type}
        key: db.user
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: db-secret-${DB_Type}
        key: db.password
```

> [!NOTE]
>
> * Replace **${DB_Type}** with **mssql**.
> * Replace **${Container_Registry_URL}** with the fully qualified name of your ACR instance.
> * Replace **${Image_Name}** with **javaee-cafe-connect-db-mssql**.

1. Run the following commands to deploy your Open Liberty Application:

   ```bash
   # Create OpenLibertyApplication custom resource
   oc create -f ./openlibertyapplication.yaml

   # Check if OpenLibertyApplication instance created
   oc get openlibertyapplication

   # Check if deployment created by Operator is ready
   oc get deployment

   # Check if route is created by Operator
   oc get route
   ```

2. Once the Open Liberty Application is up and running, open **HOST/PORT** of the route in your browser to visit the application home page.

For reference, you can find these deployment files from `<path-to-repo>/3-integration/connect-db` of your local clone.

## Connect your application to Azure Database for PostgreSQL

[Azure Database for PostgreSQL](https://azure.microsoft.com/services/postgresql/) is another fully managed database service from Microsoft Azure, based on community [PostgreSQL](https://docs.microsoft.com/azure/postgresql/concepts-supported-versions). Using Azure Database for PostgreSQL, developers can build or migrate their workloads with confidence.

### Create an Azure Database for PostgreSQL server

Follow the instructions below to set up an Azure Database for PostgreSQL server for data persistence.

1. Create an Azure Database for PostgreSQL server by following [Create an Azure Database for PostgreSQL server](https://docs.microsoft.com/azure/postgresql/quickstart-create-server-database-portal#create-an-azure-database-for-postgresql-server).
   > [!NOTE]
   > In configuring **Basics** step, write down **Admin username** and **Password**.
2. Once your database is created, open **your Azure Database for PostgreSQL server** > **Connection security** > Set **Allow access to Azure services** to **Yes** > Click **+ Add current client IP address** > Click **Save**.
   ![postgres-connection-security](./media/howto-integrate-azure-managed-databases/postgres-connection-security.png)
3. Open **your Azure Database for PostgreSQL server** > **Connection strings** > **JDBC**. Write down the **Server name** and **Port number** in ***Server name**.postgres.database.azure.com:**Port number*** format.
   ![postgre-server-jdbc-connection-string](./media/howto-integrate-azure-managed-databases/postgre-server-jdbc-connection-string.png)

### Prepare your Open Liberty application (PostgreSQL)

The application `<path-to-repo>/2-simple` used in the [previous guide](howto-deploy-java-openliberty-app.md) has no database connectivity. Follow instructions below to make it connect to Azure Database for PostgreSQL.

1. Update `server.xml` by enabling **jpa-2.2** feature and adding **dataSource** configuration. The updated file is almost same as the one for **Azure Database for PostgreSQL**, except the **dataSource** configuration.

   ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <server description="defaultServer">
        ...

        <!-- Configure data source which connects to Azure Database for PostgreSQL -->
        <dataSource id="JavaEECafeDB" jndiName="jdbc/JavaEECafeDB">
            <jdbcDriver libraryRef="driver-library" />
            <!-- Use "properties.postgresql" for Azure Database for PostgreSQL -->
            <!-- SSL communication is enabled for Azure Database for PostgreSQL -->
            <properties.postgresql
                serverName="${db.server.name}"
                portNumber="${db.port.number}"
                databaseName="${db.name}"
                user="${db.user}"
                password="${db.password}"
                ssl="true" />
        </dataSource>

        ...
    </server>
   ```

   Refer to `<path-to-repo>/3-integration/connect-db/postgres/src/main/liberty/config/server.xml` for full changes.

2. Add a new configuration file `persistence.xml` to configure data persistence schema for your application. The new file is same as the one for **Azure SQL Database**. Refer to `<path-to-repo>/3-integration/connect-db/postgres/src/main/resources/META-INF/persistence.xml` for full changes.

3. Update `Coffee.java` to make it as **a JPA Entity**. The updated file is same as the one for **Azure SQL Database**. Refer to `<path-to-repo>/3-integration/connect-db/postgres/src/main/java/cafe/model/entity/Coffee.java` for full changes.

4. Update `CafeRepository.java` to make it as **a Stateless Bean**, which implements create, read, update, and delete coffees using `javax.persistence.EntityManager` and `javax.persistence.PersistenceContext` APIs. The updated file is same as the one for **Azure SQL Database**. Refer to `<path-to-repo>/3-integration/connect-db/postgres/src/main/java/cafe/model/CafeRepository.java` for full changes.

5. Update `pom.xml` to include new properties for configuring database connection, new dependency for JDBC driver, and new plugin for `maven-dependency-plugin`. The updated file is almost same as the one for **Azure SQL Database**, except the JDBC driver dependency.

   ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
      ...

      <dependencies>
        ...

        <!-- Dependency for postgresql JDBC driver -->
        <dependency>
          <groupId>org.postgresql</groupId>
          <artifactId>postgresql</artifactId>
          <version>42.2.4</version>
          <type>jar</type>
        </dependency>
      </dependencies>
      <profiles>
        <profile>
          ...

          <build>
            <plugins>
              ...

              <plugin>
                ...

                <executions>
                  <execution>
                    ...

                    <configuration>
                      <includeArtifactIds>postgresql</includeArtifactIds>
                      ...
                    </configuration>
                  </execution>
                </executions>
              </plugin>
            </plugins>
          </build>
        </profile>
      </profiles>
    </project>
   ```

   Refer to `<path-to-repo>/3-integration/connect-db/postgres/pom.xml` for full changes.

6. Run your application with **liberty-maven-plugin** using the following commands:

   ```bash
   cd <path-to-repo>/2-simple
   mvn clean package
   mvn -Ddb.server.name=<Server name>.postgres.database.azure.com -Ddb.port.number=<Port number> -Ddb.name=postgres -Ddb.user=<Admin username>@<Server name> -Ddb.password=<Password> liberty:dev
   ```

   > [!NOTE]
   >
   > * **Server name**, **Port number**, **Admin username** and **Password** are properties you wrote down in previous step "[Create an Azure Database for PostgreSQL server](#create-an-azure-database-for-postgresql-server)".
   > * [Create a firewall rule](https://docs.microsoft.com/azure/postgresql/howto-manage-firewall-using-portal) for IP address of your client if you encountered the similar error below. Then re-run the application.
   >   * [ERROR   ] CWWJP9992E: Exception [EclipseLink-4002] (Eclipse Persistence Services - 2.7.7.v20200504-69f2c2b80d): org.eclipse.persistence.exceptions.DatabaseException. Internal Exception: java.sql.SQLException: FATAL: no pg_hba.conf entry for host "xxx.xxx.xxx.xxx", user "xxxxxx", database "xxxxxx", SSL on DSRA0010E: SQL State = 28000, Error Code = 0

   Once the application is up and running, open [http://localhost:9080](http://localhost:9080) in your browser, verify the database connectivity works by creating new coffees and deleting existing coffees in the home page. Press **Control-C** to stop the application and Open Liberty server.

For reference, these changes have already been applied in `<path-to-repo>/3-integration/connect-db/postgres` of your local clone.

### Build application image (PostgreSQL)

The **Dockerfile**, which is used for building the application image, is almost same as the one for **Azure SQL Database**, except the JDBC driver. It's located at `<path-to-repo>/3-integration/connect-db/postgres/Dockerfile`):

```Dockerfile
# open liberty base image
FROM openliberty/open-liberty:kernel-java8-openj9-ubi

# Add config, app and jdbc driver
COPY --chown=1001:0 src/main/liberty/config/server.xml /config/server.xml
COPY --chown=1001:0 target/javaee-cafe.war /config/apps/
COPY --chown=1001:0 postgresql-42.2.4.jar /opt/ol/wlp/usr/shared/resources/

# This script will add the requested XML snippets, grow image to be fit-for-purpose and apply interim fixes
RUN configure.sh
```

1. Change directory to `<path-to-repo>/3-integration/connect-db/postgres` of your local clone.
2. Download [postgresql-42.2.4.jar](https://repo1.maven.org/maven2/org/postgresql/postgresql/42.2.4/postgresql-42.2.4.jar) and put it to current working directory.
3. Run the following commands to build application image and push to your ACR instance.

   ```bash
   # Build project and generate war package
   mvn clean package

   # Build and tag application image
   docker build -t javaee-cafe-connect-db-postgres:1.0.0 --pull .

   # Create a new tag with your ACR instance info that refers to source image
   # Note: replace "${Container_Registry_URL}" with the fully qualified name of your ACR instance
   docker tag javaee-cafe-connect-db-postgres:1.0.0 ${Container_Registry_URL}/javaee-cafe-connect-db-postgres:1.0.0

   # Log in to your ACR instance
   # Note: replace "${Registry_Name}" with the name of your ACR instance
   az acr login -n ${Registry_Name}

   # Push image to your ACR instance
   # Note: replace "${Container_Registry_URL}" with the fully qualified name of your ACR instance
   docker push ${Container_Registry_URL}/javaee-cafe-connect-db-postgres:1.0.0
   ```

   > [!NOTE]
   >
   > * Replace **${Container_Registry_URL}** with the fully qualified name of your ACR instance.
   > * Replace **${Registry_Name}** with the name of your ACR instance.

### Run the application with Docker (PostgreSQL)

Before deploying the containerized application to a remote cluster, run with your local Docker to verify whether it works.

1. Run `docker run -it --rm -p 9080:9080 -e DB_SERVER_NAME=<Server name>.postgres.database.azure.com -e DB_PORT_NUMBER=<Port number> -e DB_NAME=postgres -e DB_USER=<Admin username>@<Server name> -e DB_PASSWORD=<Password> javaee-cafe-connect-db-postgres:1.0.0` in your console.
   > [!NOTE]
   >
   > * **Server name**, **Port number**, **Admin username** and **Password** are properties you wrote down in previous step "[Create an Azure Database for PostgreSQL server](#create-an-azure-database-for-postgresql-server)".
   > * [Create a firewall rule](https://docs.microsoft.com/azure/postgresql/howto-manage-firewall-using-portal) for IP address of your client if you encountered the similar error below. Then re-run the application.
   >   * [ERROR   ] CWWJP9992E: Exception [EclipseLink-4002] (Eclipse Persistence Services - 2.7.7.v20200504-69f2c2b80d): org.eclipse.persistence.exceptions.DatabaseException. Internal Exception: java.sql.SQLException: FATAL: no pg_hba.conf entry for host "xxx.xxx.xxx.xxx", user "xxxxxx", database "xxxxxx", SSL on DSRA0010E: SQL State = 28000, Error Code = 0

2. Wait for Open Liberty to start and the application to deploy successfully.
3. Open [http://localhost:9080/](http://localhost:9080/) in your browser to visit the application home page.
4. Press **Control-C** to stop the application and Open Liberty server.

### Deploy the application to ARO 4 cluster (PostgreSQL)

In this step, the application, which refers to the newly generated image, will be deployed to the ARO 4 cluster and connects to the Azure Database for PostgreSQL for data persistence.

#### Create secret for database connection credentials (PostgreSQL)

First, create a **Secret** to store database connection credentials. The YAML file, which is located at `<path-to-repo>/3-integration/connect-db/db-secret.yaml`, is same as the one for **Azure SQL Database**.

```yaml
apiVersion: v1
kind: Secret
metadata:
  # - replace "${DB_Type}" with "postgres" for testing DB connection with Azure Database for PostgreSQL
  name: db-secret-${DB_Type}
  namespace: open-liberty-demo
type: Opaque
stringData:
  db.server.name: ${DB_SERVER_NAME}
  db.port.number: "${DB_PORT_NUMBER}"
  db.name: ${DB_NAME}
  db.user: ${DB_USER}
  db.password: ${DB_PASSWORD}
```

> [!NOTE]
>
> * Replace **${DB_Type}** with **postgres**.
> * Replace **${DB_SERVER_NAME}** with ***<Server name>**.postgres.database.azure.com* you wrote down before.
> * Replace **${DB_PORT_NUMBER}** with **Port number** you wrote down before.
> * Replace **${DB_NAME}** with **postgres**.
> * Replace **${DB_USER}** with ***<Admin username>**@**<Server name>*** you wrote down before.
> * Replace **${DB_PASSWORD}** with **Password** you wrote down before.

1. Change directory to `<path-to-repo>/3-integration/connect-db`.
2. Change project to **open-liberty-demo**:

   ```bash
   oc project open-liberty-demo
   ````

   > [!NOTE]
   >
   > * Refer to [Set up Azure Red Hat OpenShift cluster](howto-deploy-java-openliberty-app.md#set-up-azure-red-hat-openshift-cluster) on how to connect to the cluster.
   > * **open-liberty-demo** is already created in the [previous guide](howto-deploy-java-openliberty-app.md).

3. Create **Secret**:

   ```bash
   oc create -f ./db-secret.yaml
   ```

#### Deploy application (PostgreSQL)

Now we can deploy the sample application, which connects to Azure Database for PostgreSQL, using the YAML file located at `<path-to-repo>/3-integration/connect-db/openlibertyapplication.yaml`. The YAML file is same as the one for **Azure SQL Database**.

```yaml
apiVersion: openliberty.io/v1beta1
kind: OpenLibertyApplication
metadata:
  # Note:
  # - replace "${DB_Type}" with "postgres" for testing DB connection with Azure Database for PostgreSQL
  name: javaee-cafe-connect-db-${DB_Type}
  namespace: open-liberty-demo
spec:
  replicas: 1
  # Note:
  # - replace "${Container_Registry_URL}" with your container registry URL
  # - replace "${Image_Name}" with "javaee-cafe-connect-db-postgres" for testing DB connection with Azure Database for PostgreSQL
  applicationImage: ${Container_Registry_URL}/${Image_Name}:1.0.0
  pullSecret: registry-secret
  expose: true
  # Note:
  # - replace "${DB_Type}" with "postgres" for testing DB connection with Azure Database for PostgreSQL
  env:
  - name: DB_SERVER_NAME
    valueFrom:
      secretKeyRef:
        name: db-secret-${DB_Type}
        key: db.server.name
  - name: DB_PORT_NUMBER
    valueFrom:
      secretKeyRef:
        name: db-secret-${DB_Type}
        key: db.port.number
  - name: DB_NAME
    valueFrom:
      secretKeyRef:
        name: db-secret-${DB_Type}
        key: db.name
  - name: DB_USER
    valueFrom:
      secretKeyRef:
        name: db-secret-${DB_Type}
        key: db.user
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: db-secret-${DB_Type}
        key: db.password
```

> [!NOTE]
>
> * Replace **${DB_Type}** with **postgres**.
> * Replace **${Container_Registry_URL}** with the fully qualified name of your ACR instance.
> * Replace **${Image_Name}** with **javaee-cafe-connect-db-postgres**.

1. Run the following commands to deploy your Open Liberty Application:

   ```bash
   # Create OpenLibertyApplication custom resource
   oc create -f ./openlibertyapplication.yaml

   # Check if OpenLibertyApplication instance created
   oc get openlibertyapplication

   # Check if deployment created by Operator is ready
   oc get deployment

   # Check if route is created by Operator
   oc get route
   ```

2. Once the Open Liberty Application is up and running, open **HOST/PORT** of the route in your browser to visit the application home page.

For reference, you can find these deployment files from `<path-to-repo>/3-integration/connect-db` of your local clone.

## Next steps

In this guide, you learned how to:
> [!div class="checklist"]
>
> * Connect your application to Azure SQL Database
> * Connect your application to Azure Database for PostgreSQL

Advance to these guides, which integrate Open Liberty application with other Azure services:
> [!div class="nextstepaction"]
> [Integrate your Open Liberty application with Azure Active Directory OpenID Connect](howto-integrate-aad-oidc.md)

> [!div class="nextstepaction"]
> [Integrate your Open Liberty application with Elasticsearch stack](howto-integrate-elasticsearch-stack.md)

If you've finished all of above guides, advance to the complete guide, which incorporates all of Azure service integrations:
> [!div class="nextstepaction"]
> [Integrate your Open Liberty application with different Azure services](howto-integrate-all.md)

Here are references used in this guide:

* [Azure managed databases](https://azure.microsoft.com/product-categories/databases/)
* [Quickstart: Create an Azure SQL Database single database](https://docs.microsoft.com/azure/azure-sql/database/single-database-create-quickstart)
* [Quickstart: Create a server-level firewall rule using the Azure portal](https://docs.microsoft.com/azure/azure-sql/database/firewall-create-server-level-portal-quickstart)
* [Quickstart: Create an Azure Database for PostgreSQL server in the Azure portal](https://docs.microsoft.com/azure/postgresql/quickstart-create-server-database-portal)
* [Create and manage firewall rules for Azure Database for PostgreSQL - Single Server using the Azure portal](https://docs.microsoft.com/azure/postgresql/howto-manage-firewall-using-portal)
* [Defines a data source configuration](https://openliberty.io/docs/ref/config/#dataSource.html)
