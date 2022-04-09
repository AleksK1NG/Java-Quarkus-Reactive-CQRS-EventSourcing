package bankAccount.repository;


import bankAccount.events.BankAccountCreatedEvent;
import com.mongodb.client.result.InsertOneResult;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.smallrye.mutiny.Uni;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.math.BigDecimal;

@ApplicationScoped
public class BankAccountMongoRepository {

    @Inject
    Logger logger;

    @Inject
    ReactiveMongoClient mongoClient;

    @ConfigProperty(name = "mongodb.database", defaultValue = "microservices")
    String database;

    @ConfigProperty(name = "mongodb.bank-account-collection", defaultValue = "bankAccounts")
    String bankAccountsCollection;

    private ReactiveMongoCollection<Document> getBankAccountCollection() {
        return mongoClient.getDatabase(database).getCollection(bankAccountsCollection);
    }

    public Uni<InsertOneResult> createBankAccount(BankAccountCreatedEvent event) {
        final var document = new Document();
        document.put("aggregateId", event.getAggregateId());
        document.put("address", event.getAddress());
        document.put("email", event.getEmail());
        document.put("userName", event.getUserName());
        document.put("balance", BigDecimal.valueOf(0));

        logger.infof("(createBankAccount) document: %s", document.toJson());
        return mongoClient.getDatabase(database).getCollection(bankAccountsCollection).insertOne(document)
                .onItem().invoke(result -> logger.infof("(createBankAccount) insert aggregateID: %s, result: %s", event.getAggregateId(), result.wasAcknowledged()))
                .log()
                .onFailure().invoke(Throwable::printStackTrace);
    }
}
