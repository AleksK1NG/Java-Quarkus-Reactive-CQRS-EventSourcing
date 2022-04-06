package bankAccount.queries;


import bankAccount.domain.BankAccountAggregate;
import es.EventStoreDB;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class BankAccountQueryHandler implements BankAccountQueryService {

    @Inject
    Logger logger;

    @Inject
    EventStoreDB eventStoreDB;

    @Override
    public Uni<BankAccountAggregate> handle(GetBankAccountByIDQuery query) {
        return eventStoreDB.load(query.aggregateID(), BankAccountAggregate.class)
                .onItem().invoke(bankAccountAggregate -> logger.infof("bankAccountAggregate: %s", bankAccountAggregate));
    }
}
