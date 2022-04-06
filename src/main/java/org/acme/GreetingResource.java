package org.acme;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/user")
public class GreetingResource {

    @Inject
    PgPool pgPool;

    @Inject
    Logger logger;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getOrder() {

        final var args = Tuple.of(UUID.randomUUID(), "AlexanderBryksin@yandex.ru", "Alexander", "Moscow, Tayninskaya 9, kv 877 AWESOME", "NEW");
        logger.infof("GET order");
        final var query = pgPool.withTransaction(client -> {
            List<JsonObject> results = new ArrayList<>();
            return client.preparedQuery("INSERT INTO orders (id, user_email, user_name, delivery_address, status) " +
                    "VALUES ($1, $2, $3, $4, $5) RETURNING *").mapping(row -> row.toJson()).execute(args)
                    .onFailure(ex -> logger.errorf("preparedQuery ex: %s", ex.getMessage()))
                    .compose(inserted -> client.preparedQuery("select * from orders").execute())
                    .onFailure(ex -> logger.errorf("inserted preparedQuery ex: %s", ex.getMessage()))
                    .onSuccess(result -> {
                        result.forEach(row -> results.add(row.toJson()));
                    })
                    .flatMap(m -> Future.succeededFuture(results));
        });
        query.onSuccess(res -> logger.infof("withTransaction success: %s", res));

        pgPool.withTransaction(tx -> tx.preparedQuery("INSERT INTO orders (id, user_email, user_name, delivery_address, status) " +
                        "VALUES ($1, $2, $3, $4, $5) RETURNING *")
                .execute(Tuple.of(UUID.randomUUID(), "AlexanderBryksin@yandex.ru", "Alexander", "Moscow, Tayninskaya 9, kv 877 AWESOME", "NEW"))
                .compose(mapper -> {
                    List<String> list = new ArrayList<>();
                    mapper.value().forEach(row -> {
                        logger.infof("row: %s", row.toJson().toString());
                        list.add(row.toJson().toString());
                    });
                    return Future.succeededFuture(list);
                })
                .onComplete(comp -> {
                    if (comp.succeeded()) {
                        logger.infof("comp: >>> %s", comp.result());
                    } else {
                        logger.errorf("comp ex: %s", comp.cause());
                    }
                })
                .onFailure(ex -> logger.errorf("ex: %s", ex)));

        return Uni.createFrom().item(Response.ok("Ok").build());
    }


//    @GET
//    @Produces(MediaType.TEXT_PLAIN)
//    public Uni<Response> getOrder() {
//        logger.infof("GET order");
//        pgPool.withTransaction(tx -> {
//                    tx.preparedQuery("INSERT INTO orders (id, user_email, user_name, delivery_address, status, delivery_date, created_at, updated_at) " +
//                                    "VALUES ($1, $2, $3, $4, $5, $6, $76 $8)")
//                            .execute(Tuple.of(
//                                    UUID.randomUUID(),
//                                    "AlexanderBryksin@yandex.ru",
//                                    "Alexander",
//                                    "Moscow, Tayninskaya 9, kv 877 AWESOME"
//                                    , "NEW",
//                                    LocalDateTime.now(),
//                                    LocalDateTime.now(),
//                                    LocalDateTime.now()
//                            ), result -> {
//                                if (result.succeeded()) {
//                                    result.result().forEach(row -> logger.infof("row: %s", row.toJson().toString()));
//                                } else {
//                                    logger.errorf("error: %s", result.cause().getMessage());
//                                }
//                            });
//                    logger.info("completed");
//                    return null;
//                })
//                .onSuccess(v -> logger.info("onSuccess"))
//                .onFailure(ex -> logger.errorf("onFailure: %s", ex.getMessage()));
//        return Uni.createFrom().item(Response.ok("Ok").build());
//    }
}