package es;


import bankAccount.exceptions.BankAccountNotFoundException;
import io.smallrye.mutiny.Uni;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import org.eclipse.microprofile.opentracing.Traced;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class EventStore implements EventStoreDB {

    private final int SNAPSHOT_FREQUENCY = 3;
    private final static Logger logger = Logger.getLogger(EventStore.class);
    private final static String SAVE_EVENTS_QUERY = "INSERT INTO events (aggregate_id, aggregate_type, event_type, data, metadata, version, timestamp) " +
            "values ($1, $2, $3, $4, $5, $6, now())";
    private final static String LOAD_EVENTS_QUERY = "select event_id ,aggregate_id, aggregate_type, event_type, data, metadata, version, timestamp" +
            " from events e where e.aggregate_id = $1 and e.version > $2 ORDER BY e.version ASC";
    private final static String HANDLE_CONCURRENCY_QUERY = "SELECT aggregate_id FROM events e WHERE e.aggregate_id = $1 LIMIT 1 FOR UPDATE";
    private final static String SAVE_SNAPSHOT_QUERY = "INSERT INTO snapshots (aggregate_id, aggregate_type, data, metadata, version, timestamp) " +
            "VALUES ($1, $2, $3, $4, $5, now()) " +
            "ON CONFLICT (aggregate_id) " +
            "DO UPDATE SET data = $3, version = $5, timestamp = now()";
    private final static String EXISTS_QUERY = "SELECT e.aggregate_id FROM events e WHERE e.aggregate_id = $1 LIMIT 1";
    private final static String GET_SNAPSHOT_QUERY = "select snapshot_id, aggregate_id, aggregate_type, data, metadata, version, timestamp from snapshots s where s.aggregate_id = $1";

    @Inject
    PgPool pgPool;

    @Inject
    EventBus eventBus;

    @Override
    @Traced
    public Future<RowSet<Row>> saveEvents(SqlConnection client, List<Event> events) {
        if (events.isEmpty()) {
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
                        event.getVersion())).toList();

        if (tupleList.size() == 1) {
            return client.preparedQuery(SAVE_EVENTS_QUERY).execute(tupleList.get(0))
                    .onFailure(ex -> logger.error("(execute) ex:", ex))
                    .onSuccess(result -> logger.infof("(saveEvents) execute result: %s", result.rowCount()));
        }

        return client.preparedQuery(SAVE_EVENTS_QUERY).executeBatch(tupleList)
                .onFailure(ex -> logger.error("(executeBatch) ex:", ex))
                .onSuccess(result -> logger.infof("(saveEvents) executeBatch result: %s", result.rowCount()));
    }

    @Override
    @Traced
    public Future<RowSet<Event>> loadEvents(String aggregateId, long version) {
        return pgPool.preparedQuery(LOAD_EVENTS_QUERY).mapping(row -> Event.builder()
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
                .onFailure(ex -> logger.error("(loadEvents) preparedQuery ex:", ex));
    }


    @Traced
    private Future<RowSet<Row>> handleConcurrency(SqlConnection client, String aggregateID) {
        return client.preparedQuery(HANDLE_CONCURRENCY_QUERY).execute(Tuple.of(aggregateID))
                .onFailure(ex -> logger.error("handleConcurrency ex", ex));
    }

    @Override
    @Traced
    public <T extends AggregateRoot> Uni<Void> save(T aggregate) {
        final List<Event> changes = new ArrayList<>(aggregate.getChanges());

        logger.infof("(SAVE) aggregate changes: >>>> %s", changes);
        final var future = pgPool.withTransaction(client -> handleConcurrency(client, aggregate.getId())
                .compose(v -> saveEvents(client, aggregate.getChanges()))
                .compose(s -> aggregate.getVersion() % SNAPSHOT_FREQUENCY == 0 ? saveSnapshot(client, aggregate) : Future.succeededFuture())
                .compose(a -> Future.fromCompletionStage(eventBus.publish(changes).convert().toCompletionStage()))
                .onFailure(Throwable::printStackTrace)
                .onSuccess(success -> logger.infof("save success: %s", success)));

//        aggregate.clearChanges();
        return Uni.createFrom().completionStage(future.toCompletionStage()).replaceWithVoid();
    }


    @Traced
    private <T extends AggregateRoot> Future<RowSet<Row>> saveSnapshot(SqlConnection client, T aggregate) {
        logger.infof("saveSnapshot (SAVE SNAPSHOT) version >>>>>> %s", aggregate.getVersion());

        aggregate.toSnapshot();
        final var snapshot = EventSourcingUtils.snapshotFromAggregate(aggregate);

        return client.preparedQuery(SAVE_SNAPSHOT_QUERY).execute(Tuple.of(
                        snapshot.getAggregateId(),
                        snapshot.getAggregateType(),
                        Objects.isNull(snapshot.getData()) ? new byte[]{} : snapshot.getData(),
                        Objects.isNull(snapshot.getMetaData()) ? new byte[]{} : snapshot.getMetaData(),
                        snapshot.getVersion()))
                .onFailure(ex -> logger.error("(saveSnapshot) preparedQuery execute:", ex));
    }

    @Traced
    private Future<Snapshot> getSnapshot(SqlConnection client, String aggregateID) {
        return client.preparedQuery(GET_SNAPSHOT_QUERY).mapping(row -> Snapshot.builder()
                        .id(row.getUUID("snapshot_id"))
                        .aggregateId(row.getString("aggregate_id"))
                        .aggregateType(row.getString("aggregate_type"))
                        .data(row.getBuffer("data").getBytes())
                        .metaData(row.getBuffer("metadata").getBytes())
                        .version(row.getLong("version"))
                        .timeStamp(row.getLocalDateTime("timestamp"))
                        .build())
                .execute(Tuple.of(aggregateID))
                .onFailure(ex -> logger.error("(getSnapshot) preparedQuery ex:", ex))
                .compose(result -> result.size() == 0 ? Future.succeededFuture() : Future.succeededFuture(result.iterator().next()))
                .onSuccess(snapshot -> logger.infof("(getSnapshot) onSuccess snapshot version: %s", Optional.ofNullable(snapshot).map(Snapshot::getVersion)));
    }

    @Traced
    private <T extends AggregateRoot> T getAggregate(final String aggregateId, final Class<T> aggregateType) {
        try {
            return aggregateType.getConstructor(String.class).newInstance(aggregateId);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Traced
    private <T extends AggregateRoot> T getSnapshotFromClass(Snapshot snapshot, String aggregateId, Class<T> aggregateType) {
        if (snapshot == null) {
            final var defaultSnapshot = EventSourcingUtils.snapshotFromAggregate(getAggregate(aggregateId, aggregateType));
            return EventSourcingUtils.aggregateFromSnapshot(defaultSnapshot, aggregateType);
        }
        return EventSourcingUtils.aggregateFromSnapshot(snapshot, aggregateType);
    }

    @Override
    @Traced
    public <T extends AggregateRoot> Uni<T> load(String aggregateId, Class<T> aggregateType) {
        final var future = pgPool.withTransaction(client -> this.getSnapshot(client, aggregateId)
                .compose(snapshot -> {
                    final var aggregate = getSnapshotFromClass(snapshot, aggregateId, aggregateType);
                    logger.infof("(load) aggregate: %s", aggregate);
                    return this.loadEvents(aggregate.getId(), aggregate.getVersion())
                            .transform(events -> raiseAggregateEvents(aggregate, events));
                }));

        return Uni.createFrom().completionStage(future.toCompletionStage());
    }

    @Traced
    private <T extends AggregateRoot> Future<@Nullable T> raiseAggregateEvents(T aggregate, AsyncResult<RowSet<Event>> events) {
        if (events.succeeded() && events.result().size() > 0) {
            events.result().forEach(event -> {
                aggregate.raiseEvent(event);
                logger.infof("(load) loadEvents raiseEvent event version: %s", event.getVersion());
            });
            return Future.succeededFuture(aggregate);
        } else {
            return (aggregate.getVersion() == 0) ? Future.failedFuture(new BankAccountNotFoundException(aggregate.getId())) : Future.succeededFuture(aggregate);
        }
    }

    @Override
    @Traced
    public Uni<Boolean> exists(String aggregateId) {
        final var result = pgPool.preparedQuery(EXISTS_QUERY).execute(Tuple.of(aggregateId))
                .map(m -> m.rowCount() > 0)
                .onFailure(ex -> logger.errorf("(exists) aggregateId: %s, ex: %s", aggregateId, ex.getMessage()))
                .onSuccess(isExists -> logger.infof("(exists) aggregateId: %s, exists: %s", aggregateId, isExists)).toCompletionStage();

        return Uni.createFrom().completionStage(result);
    }
}
