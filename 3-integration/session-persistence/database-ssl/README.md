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

Checkout the repo to local directory, switch directory to `<local-repo-clone/3-integration/session-persistence/database-ssl>`, then run the following commands to build the sample and run it with liberty maven plugin:

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

## References

See the following references for more information.

* [Database Session Persistence](https://openliberty.io/docs/latest/reference/feature/sessionDatabase-1.0.html)
