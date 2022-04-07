package es;


import bankAccount.exceptions.BankAccountNotFoundException;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class EventStore implements EventStoreDB {

    private final int SNAPSHOT_FREQUENCY = 3;

    @Inject
    Logger logger;

    @Inject
    PgPool pgPool;


    @Override
    public Future<RowSet<Row>> saveEvents(SqlConnection client, List<Event> events) {
        if (events.size() == 0) {
            logger.info("(saveEvents) empty events list");
            return Future.succeededFuture();
        }

        final List<Tuple> tupleList = events.stream()
                .map(event -> Tuple.of(
                        event.getAggregateId(),
                        event.getAggregateType(),
                        event.getEventType(),
                        Objects.isNull(event.getData()) ? new byte[]{} : event.getData(),
                        Objects.isNull(event.getMetaData()) ? new byte[]{} : event.getMetaData(),
                        event.getVersion()))
                .toList();


        return client.preparedQuery("INSERT INTO events (aggregate_id, aggregate_type, event_type, data, metadata, version, timestamp) " +
                        "values ($1, $2, $3, $4, $5, $6, now())")
                .executeBatch(tupleList)
                .onFailure(ex -> {
                    logger.errorf("(executeBatch) ex: %s", ex.getMessage());
                    ex.printStackTrace();
                })
                .onSuccess(result -> logger.infof("(saveEvents) result: %s", result.rowCount()));
    }

    @Override
    public Future<RowSet<Event>> loadEvents(String aggregateId, long version) {
        return pgPool.preparedQuery("select event_id ,aggregate_id, aggregate_type, event_type, data, metadata, version, timestamp" +
                        " from events e where e.aggregate_id = $1 and e.version > $2 ORDER BY e.version ASC")
                .mapping(row -> Event.builder()
                        .id(row.getUUID("event_id"))
                        .aggregateId(row.getString("aggregate_id"))
                        .aggregateType(row.getString("aggregate_type"))
                        .eventType(row.getString("event_type"))
                        .data(row.getBuffer("data").getBytes())
                        .metaData(row.getBuffer("metadata").getBytes())
                        .version(row.getLong("version"))
                        .timeStamp(row.getOffsetDateTime("timestamp").toZonedDateTime())
                        .build())
                .execute(Tuple.of(aggregateId, version))
                .onFailure(ex -> {
                    logger.errorf("(loadEvents) preparedQuery ex: %s", ex.getMessage());
                    ex.printStackTrace();
                });
    }


    private Future<RowSet<Row>> handleConcurrency(SqlConnection client, String aggregateID) {
        return client.preparedQuery("SELECT aggregate_id FROM events e WHERE e.aggregate_id = $1 LIMIT 1 FOR UPDATE")
                .execute(Tuple.of(aggregateID))
                .onFailure(ex -> {
                    logger.errorf("handleConcurrency ex: %s", ex.getMessage());
                    ex.printStackTrace();
                });
    }

    @Override
    public <T extends AggregateRoot> Uni<Void> save(T aggregate) {
        final var future = pgPool.withTransaction(client -> handleConcurrency(client, aggregate.getId())
                .compose(v -> saveEvents(client, aggregate.getChanges()))
                .compose(s -> aggregate.getVersion() % SNAPSHOT_FREQUENCY == 0 ? saveSnapshot(client, aggregate) : Future.succeededFuture())
                .onFailure(Throwable::printStackTrace)
                .onSuccess(success -> logger.infof("save success: %s", success)));

//        aggregate.clearChanges();
        return Uni.createFrom().completionStage(future.toCompletionStage()).replaceWithVoid();
    }

    private <T extends AggregateRoot> Future<RowSet<Row>> saveSnapshot(SqlConnection client, T aggregate) {
        logger.infof("saveSnapshot (SAVE SNAPSHOT) version >>>>>> %s", aggregate.getVersion());

        aggregate.toSnapshot();
        final var snapshot = EventSourcingUtils.snapshotFromAggregate(aggregate);

        return client.preparedQuery("INSERT INTO snapshots (aggregate_id, aggregate_type, data, metadata, version, timestamp) " +
                        "VALUES ($1, $2, $3, $4, $5, now()) " +
                        "ON CONFLICT (aggregate_id) " +
                        "DO UPDATE SET data = $3, version = $5, timestamp = now()")
                .execute(Tuple.of(
                        snapshot.getAggregateId(),
                        snapshot.getAggregateType(),
                        Objects.isNull(snapshot.getData()) ? new byte[]{} : snapshot.getData(),
                        Objects.isNull(snapshot.getMetaData()) ? new byte[]{} : snapshot.getMetaData(),
                        snapshot.getVersion()))
                .onFailure(ex -> {
                    logger.errorf("(saveSnapshot) preparedQuery ex: %s", ex.getMessage());
                    ex.printStackTrace();
                });
    }

    private Future<Snapshot> getSnapshot(SqlConnection client, String aggregateID) {
        return client.preparedQuery("select snapshot_id, aggregate_id, aggregate_type, data, metadata, version, timestamp from snapshots s where s.aggregate_id = $1")
                .mapping(row -> Snapshot.builder()
                        .id(row.getUUID("snapshot_id"))
                        .aggregateId(row.getString("aggregate_id"))
                        .aggregateType(row.getString("aggregate_type"))
                        .data(row.getBuffer("data").getBytes())
                        .metaData(row.getBuffer("metadata").getBytes())
                        .version(row.getLong("version"))
                        .timeStamp(row.getLocalDateTime("timestamp"))
                        .build())
                .execute(Tuple.of(aggregateID))
                .onFailure(ex -> {
                    logger.errorf("(getSnapshot) preparedQuery ex: %s", ex.getMessage());
                    ex.printStackTrace();
                })
                .compose(rowSetAsyncResult -> rowSetAsyncResult.size() == 0 ? Future.succeededFuture() : Future.succeededFuture(rowSetAsyncResult.iterator().next()))
                .onSuccess(snapshot -> {
                    if (snapshot != null) {
                        logger.infof("(getSnapshot) onSuccess snapshot version: %s", snapshot.getVersion());
                    }
                });
    }

    private <T extends AggregateRoot> T getAggregate(final String aggregateId, final Class<T> aggregateType) {
        try {
            return aggregateType.getConstructor(String.class).newInstance(aggregateId);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends AggregateRoot> T getSnapshotFromClass(Snapshot snapshot, String aggregateId, Class<T> aggregateType) {
        if (snapshot == null) {
            final var defaultSnapshot = EventSourcingUtils.snapshotFromAggregate(getAggregate(aggregateId, aggregateType));
            return EventSourcingUtils.aggregateFromSnapshot(defaultSnapshot, aggregateType);
        }
        return EventSourcingUtils.aggregateFromSnapshot(snapshot, aggregateType);
    }

    @Override
    public <T extends AggregateRoot> Uni<T> load(String aggregateId, Class<T> aggregateType) {
        final var future = pgPool.withTransaction(client -> this.getSnapshot(client, aggregateId)
                .compose(snapshot -> {
                    final var aggregate = getSnapshotFromClass(snapshot, aggregateId, aggregateType);
                    logger.infof("(load) aggregate: %s", aggregate);
                    return this.loadEvents(aggregate.getId(), aggregate.getVersion())
                            .transform(events -> {
                                if (events.succeeded() && events.result().size() > 0) {
                                    events.result().forEach(event -> {
                                        aggregate.raiseEvent(event);
                                        logger.infof("(load) loadEvents raiseEvent event version: %s", event.getVersion());
                                    });
                                    return Future.succeededFuture(aggregate);
                                } else {
                                    return (aggregate.getVersion() == 0) ? Future.failedFuture(new BankAccountNotFoundException(aggregateId)) : Future.succeededFuture(aggregate);
                                }
                            });
                }));

        return Uni.createFrom().completionStage(future.toCompletionStage());
    }

    @Override
    public Uni<Boolean> exists(String aggregateId) {
        return null;
    }
}
