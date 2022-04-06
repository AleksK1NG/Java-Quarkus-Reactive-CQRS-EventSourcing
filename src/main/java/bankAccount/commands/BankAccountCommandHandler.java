package bankAccount.commands;


import bankAccount.domain.BankAccountAggregate;
import es.EventStoreDB;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class BankAccountCommandHandler implements BankAccountCommandService {

    @Inject
    Logger logger;

    @Inject
    EventStoreDB eventStoreDB;


    @Override
    public Uni<String> handle(CreateBankAccountCommand command) {
        final var aggregate = new BankAccountAggregate(command.aggregateID());
        aggregate.createBankAccount(command.email(), command.address(), command.userName());
        return eventStoreDB.save(aggregate).replaceWith(aggregate.getId())
                .onItem().invoke(() -> logger.infof("crated bank account: %s", aggregate));
    }

    @Override
    public Uni<Void> handle(ChangeEmailCommand command) {
        final var aggregate = new BankAccountAggregate(command.aggregateID());
        aggregate.changeEmail(command.newEmail());
        return eventStoreDB.save(aggregate)
                .onItem().invoke(() -> logger.infof("changed email: %s, id: %s", aggregate.getEmail(), aggregate.getId()));
    }

    @Override
    public Uni<Void> handle(ChangeAddressCommand command) {
        final var aggregate = new BankAccountAggregate(command.aggregateID());
        aggregate.changeAddress(command.newAddress());
        return eventStoreDB.save(aggregate)
                .onItem().invoke(() -> logger.infof("changed address: %s, id: %s", aggregate.getAddress(), aggregate.getId()));
    }

    @Override
    public Uni<Void> handle(DepositAmountCommand command) {
        final var aggregate = new BankAccountAggregate(command.aggregateID());
        aggregate.depositBalance(command.amount());
        return eventStoreDB.save(aggregate)
                .onItem().invoke(() -> logger.infof("deposited amount: %d, id: %s", aggregate.getBalance(), aggregate.getId()));
    }
}
