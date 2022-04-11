package bankAccount.queries;


import bankAccount.domain.BankAccountAggregate;
import bankAccount.dto.BankAccountResponseDTO;
import bankAccount.repository.BankAccountMongoPanacheRepository;
import es.EventStoreDB;
import io.smallrye.mutiny.Uni;
import mappers.BankAccountMapper;
import org.eclipse.microprofile.opentracing.Traced;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class BankAccountQueryHandler implements BankAccountQueryService {

    @Inject
    Logger logger;

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
                .onFailure().invoke(ex -> logger.errorf("MONGO AGGREGATE NOT FOUND: %s", ex.getMessage()))
                .onFailure().recoverWithUni(e -> eventStoreDB.load(query.aggregateID(), BankAccountAggregate.class)
                        .onFailure().invoke(Throwable::printStackTrace)
                        .onItem().invoke(bankAccountAggregate -> logger.infof("(eventStoreDB.load) >>> bankAccountAggregate: %s", bankAccountAggregate))
                        .onFailure().invoke(Throwable::printStackTrace)
                        .onItem().call(bankAccountAggregate -> panacheRepository.persist(BankAccountMapper.bankAccountDocumentFromAggregate(bankAccountAggregate))
                                .onItem().invoke(bankAccountDocument -> logger.infof("(panacheRepository.persist) >>> bankAccountDocument: %s", bankAccountDocument)))
                        .onFailure().invoke(Throwable::printStackTrace)
                        .onItem().transform(BankAccountMapper::bankAccountResponseDTOFromAggregate)
                        .onItem().invoke(bankAccountResponseDTO -> logger.infof("(bankAccountResponseDTO) >>> bankAccountResponseDTO: %s", bankAccountResponseDTO)));
    }
}
