package bankAccount.queries;


import bankAccount.domain.BankAccountAggregate;
import bankAccount.domain.BankAccountDocument;
import bankAccount.dto.BankAccountResponseDTO;
import bankAccount.repository.BankAccountMongoPanacheRepository;
import es.EventStoreDB;
import io.smallrye.mutiny.Uni;
import mappers.BankAccountMapper;
import org.eclipse.microprofile.opentracing.Traced;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class BankAccountQueryHandler implements BankAccountQueryService {

    private final static Logger logger = Logger.getLogger(BankAccountQueryHandler.class);

    @Inject
    EventStoreDB eventStoreDB;

    @Inject
    BankAccountMongoPanacheRepository panacheRepository;

    @Override
    @Traced
    public Uni<BankAccountResponseDTO> handle(GetBankAccountByIDQuery query) {
        return panacheRepository.findByAggregateId(query.aggregateID())
                .onItem().transform(BankAccountMapper::bankAccountResponseDTOFromDocument)
                .onItem().invoke(bankAccountResponseDTO -> logger.infof("(FIND panacheRepository.findByAggregateId) bankAccountResponseDTO: %s", bankAccountResponseDTO))
                .onFailure().invoke(ex -> logger.errorf("mongo aggregate not found: %s", ex.getMessage()))
                .onFailure().recoverWithUni(e -> eventStoreDB.load(query.aggregateID(), BankAccountAggregate.class)
                        .onFailure().invoke(ex -> logger.error("eventStoreDB.load", ex))
                        .onItem().invoke(bankAccountAggregate -> logger.infof("(eventStoreDB.load) >>> bankAccountAggregate: %s", bankAccountAggregate))
                        .onItem().call(bankAccountAggregate -> panacheRepository.persist(BankAccountMapper.bankAccountDocumentFromAggregate(bankAccountAggregate))
                                .onItem().invoke(bankAccountDocument -> logger.infof("(panacheRepository.persist) >>> bankAccountDocument: %s", bankAccountDocument)))
                        .onFailure().invoke(ex -> logger.error("persist", ex))
                        .onItem().transform(BankAccountMapper::bankAccountResponseDTOFromAggregate)
                        .onItem().invoke(bankAccountResponseDTO -> logger.infof("(bankAccountResponseDTO) >>> bankAccountResponseDTO: %s", bankAccountResponseDTO)));
    }

    @Override
    @Traced
    public Uni<List<BankAccountDocument>> handle(FindAllByBalanceQuery query) {
        return panacheRepository.findAllSortByBalanceWithPagination(query.page())
                .onItem().invoke(result -> logger.infof("(findAllSortByBalanceWithPagination) query: %s", query));
    }
}
