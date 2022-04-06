package bankAccount.queries;

import bankAccount.domain.BankAccountAggregate;
import io.smallrye.mutiny.Uni;

public interface BankAccountQueryService {
    Uni<BankAccountAggregate> handle(GetBankAccountByIDQuery query);
}
