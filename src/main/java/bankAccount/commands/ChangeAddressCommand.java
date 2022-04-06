package bankAccount.commands;

public record ChangeAddressCommand(String aggregateID, String newAddress) {
}
