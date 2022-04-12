### Java Quarkus CQRS and EventSourcing microservice example 👋

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


### Swagger UI:

http://localhost:8006/q/swagger-ui

### Jaeger UI:

http://localhost:16686

### Prometheus UI:

http://localhost:9090

### Grafana UI:

http://localhost:3005

### Kafka UI:

http://localhost:9000/



For local development:
```
make local // runs docker-compose.local.yml
./mvnw compile quarkus:dev
```


# code-with-quarkus-reactive-pg Project

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

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

You can then execute your native executable with: `./target/code-with-quarkus-reactive-pg-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Related Guides

- Hibernate Validator ([guide](https://quarkus.io/guides/validation)): Validate object properties (field, getter) and method parameters for your beans (REST, CDI, JPA)
- Reactive PostgreSQL client ([guide](https://quarkus.io/guides/reactive-sql-clients)): Connect to the PostgreSQL database using the reactive pattern

## Provided Code

### RESTEasy Reactive

Easily start your Reactive RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
