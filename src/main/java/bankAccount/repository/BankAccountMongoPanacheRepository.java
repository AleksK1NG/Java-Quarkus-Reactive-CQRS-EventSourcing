package bankAccount.repository;

import bankAccount.domain.BankAccountDocument;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BankAccountMongoPanacheRepository implements ReactivePanacheMongoRepository<BankAccountDocument> {

    public Uni<BankAccountDocument> findByAggregateId(String aggregateId) {
        return find("aggregateId", aggregateId).firstResult();
    }
}
