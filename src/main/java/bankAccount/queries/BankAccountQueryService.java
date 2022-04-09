package bankAccount.queries;

import bankAccount.dto.BankAccountResponseDTO;
import io.smallrye.mutiny.Uni;

public interface BankAccountQueryService {
    Uni<BankAccountResponseDTO> handle(GetBankAccountByIDQuery query);
}
