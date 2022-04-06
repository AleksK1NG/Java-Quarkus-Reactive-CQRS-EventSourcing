package bankAccount.commands;

import io.smallrye.mutiny.Uni;

public interface BankAccountCommandService {
    Uni<String> handle(CreateBankAccountCommand command);

    Uni<Void> handle(ChangeEmailCommand command);

    Uni<Void> handle(ChangeAddressCommand command);

    Uni<Void> handle(DepositAmountCommand command);
}
