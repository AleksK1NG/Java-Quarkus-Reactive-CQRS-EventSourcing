package bankAccount.commands;

public record CreateBankAccountCommand(String aggregateID, String email, String userName, String address) {
}
