### Java Quarkus CQRS and EventSourcing microservice example 👋💫✨

#### 👨‍💻 Full list what has been used:
* [Quarkus](https://quarkus.io/) - Supersonic Subatomic Java.
* [RESTEasy Reactive](https://quarkus.io/guides/resteasy-reactive) - RESTEasy Reactive in Quarkus
* [Reactive PostgreSQL Client](https://vertx.io/docs/vertx-pg-client/java/) -The Reactive PostgreSQL Client.
* [quarkus-mongodb-client](https://quarkus.io/guides/mongodb) - MongoDB client
* [hibernate-validator](https://quarkus.io/guides/validation) - Hibernate validator
* [OpenAPI and Swagger UI](https://quarkus.io/guides/openapi-swaggerui) - OpenAPI and Swagger UI
* [Micrometer metrics](https://quarkus.io/guides/micrometer) - Micrometer metrics
* [OpenTracing](https://quarkus.io/guides/opentracing) - Jaeger OpenTracing
* [Kafka Reactive](https://quarkus.io/guides/kafka-reactive-getting-started) - SmallRye Reactive Messaging to interact with Apache Kafka.
* [Docker](https://www.docker.com/) - Docker
* [Prometheus](https://prometheus.io/) - Prometheus
* [Grafana](https://grafana.com/) - Grafana
* [Jaeger](https://www.jaegertracing.io/) - Jaeger tracing
* [Flyway](https://www.jaegertracing.io/) - database migrations.
* [SmallRye Fault Tolerance](https://quarkus.io/guides/smallrye-fault-tolerance) -  Fault Tolerance.


### Swagger UI:

http://localhost:8006/q/swagger-ui

### Jaeger UI:

http://localhost:16686

### Prometheus UI:

http://localhost:9090

### Grafana UI:

http://localhost:3005


For local development:
```
make local // runs docker-compose.yml
./mvnw compile quarkus:dev // run microservice
```

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```
