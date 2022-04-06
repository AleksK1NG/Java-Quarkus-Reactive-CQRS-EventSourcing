package es;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;

import java.util.List;

public interface EventStoreDB {

    Future<RowSet<Row>> saveEvents(SqlConnection client, final List<Event> events);

    Future<RowSet<Event>> loadEvents(final String aggregateId, long version);

    <T extends AggregateRoot> Uni<Void> save(final T aggregate);

    <T extends AggregateRoot> Uni<T> load(final String aggregateId, final Class<T> aggregateType);

    Uni<Boolean> exists(final String aggregateId);
}
