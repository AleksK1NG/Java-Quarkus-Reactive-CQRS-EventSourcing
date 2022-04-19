package es;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.SqlConnection;

import java.util.List;

public interface EventStoreDB {

    Uni<RowSet<Row>> saveEvents(SqlConnection client, final List<Event> events);

    Uni<RowSet<Event>> loadEvents(final String aggregateId, long version);

    <T extends AggregateRoot> Uni<Void> save(final T aggregate);

    <T extends AggregateRoot> Uni<T> load(final String aggregateId, final Class<T> aggregateType);

    Uni<Boolean> exists(final String aggregateId);
}
