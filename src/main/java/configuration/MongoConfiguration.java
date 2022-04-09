package configuration;

import com.mongodb.client.model.Indexes;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class MongoConfiguration {

    @Inject
    Logger logger;

    @Inject
    ReactiveMongoClient mongoClient;

    void startup(@Observes StartupEvent event) {
        mongoClient.getDatabase("microservices")
                .listCollectionNames().toUni()
                .onFailure().invoke(Throwable::printStackTrace)
                .chain(collections -> {
                    logger.infof("COLLECTIONS >>>>>>>> %s", collections);
                    if (collections.contains("bankAccounts")) {
                        return Uni.createFrom().voidItem();
                    }
                    return mongoClient.getDatabase("microservices")
                            .createCollection("bankAccounts")
                            .onFailure().invoke(Throwable::printStackTrace)
                            .chain(v -> mongoClient.getDatabase("microservices")
                                    .getCollection("bankAccounts")
                                    .createIndex(Indexes.ascending("aggregateId")));
                })
                .subscribe().with(result -> logger.infof("listCollections: %s", result));


        mongoClient.getDatabase("microservices").getCollection("bankAccounts")
                .listIndexes().collect().asList()
                .onFailure().invoke(Throwable::printStackTrace)
                .subscribe().with(result -> logger.infof("bankAccounts indexes: %s", result));
    }
}
