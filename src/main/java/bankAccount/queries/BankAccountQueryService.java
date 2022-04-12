package bankAccount.queries;

import bankAccount.domain.BankAccountDocument;
import bankAccount.dto.BankAccountResponseDTO;
import io.smallrye.mutiny.Uni;

import java.util.List;

public interface BankAccountQueryService {
    Uni<BankAccountResponseDTO> handle(GetBankAccountByIDQuery query);
    Uni<List<BankAccountDocument>> handle(FindAllByBalanceQuery query);
}
