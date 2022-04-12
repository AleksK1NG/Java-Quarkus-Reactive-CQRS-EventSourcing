package configuration;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class MongoConfiguration {

    private final static String AGGREGATE_ID = "aggregateId";

    private final static Logger logger = Logger.getLogger(MongoConfiguration.class);

    @Inject
    ReactiveMongoClient mongoClient;

    @ConfigProperty(name = "mongodb.database", defaultValue = "microservices")
    String database;

    @ConfigProperty(name = "mongodb.bank-account-collection", defaultValue = "bankAccounts")
    String bankAccountsCollection;

    void startup(@Observes StartupEvent event) {
        mongoClient.getDatabase(database)
                .listCollectionNames().toUni()
                .onFailure().invoke(Throwable::printStackTrace)
                .chain(collections -> {
                    logger.infof("startup mongo collections: %s", collections);
                    if (collections != null && collections.contains(bankAccountsCollection)) {
                        return Uni.createFrom().voidItem();
                    }
                    final var indexOptions = new IndexOptions().unique(true);
                    return mongoClient.getDatabase(database)
                            .createCollection(bankAccountsCollection)
                            .onFailure().invoke(Throwable::printStackTrace)
                            .chain(v -> mongoClient.getDatabase(database)
                                    .getCollection(bankAccountsCollection)
                                    .createIndex(Indexes.ascending(AGGREGATE_ID), indexOptions));
                })
                .subscribe().with(result -> logger.infof("listCollections: %s", result));


        mongoClient.getDatabase(database).getCollection(bankAccountsCollection)
                .listIndexes().collect().asList()
                .onFailure().invoke(Throwable::printStackTrace)
                .subscribe().with(result -> logger.infof("bankAccounts indexes: %s", result));
    }
}
