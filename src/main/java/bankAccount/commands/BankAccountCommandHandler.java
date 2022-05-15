package bankAccount.commands;


import bankAccount.domain.BankAccountAggregate;
import es.EventStoreDB;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.opentracing.Traced;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.UUID;

@ApplicationScoped
public class BankAccountCommandHandler implements BankAccountCommandService {

    private final static Logger logger = Logger.getLogger(BankAccountCommandHandler.class);

    @Inject
    EventStoreDB eventStoreDB;


    @Override
    @Traced
    public Uni<String> handle(CreateBankAccountCommand command) {
        final var aggregate = new BankAccountAggregate(UUID.randomUUID().toString());
        aggregate.createBankAccount(command.email(), command.address(), command.userName());
        return eventStoreDB.save(aggregate)
                .replaceWith(aggregate.getId())
                .onItem().invoke(() -> logger.infof("created bank account: %s", aggregate));
    }

    @Override
    @Traced
    public Uni<Void> handle(ChangeEmailCommand command) {
        return eventStoreDB.load(command.aggregateID(), BankAccountAggregate.class)
                .onItem().transform(aggregate -> {
                    aggregate.changeEmail(command.newEmail());
                    return aggregate;
                })
                .chain(aggregate -> eventStoreDB.save(aggregate))
                .onItem().invoke(() -> logger.infof("changed email: %s, id: %s", command.newEmail(), command.aggregateID()));
    }

    @Override
    @Traced
    public Uni<Void> handle(ChangeAddressCommand command) {
        return eventStoreDB.load(command.aggregateID(), BankAccountAggregate.class)
                .onItem().transform(aggregate -> {
                    aggregate.changeAddress(command.newAddress());
                    return aggregate;
                })
                .chain(aggregate -> eventStoreDB.save(aggregate))
                .onItem().invoke(() -> logger.infof("changed address: %s, id: %s", command.newAddress(), command.aggregateID()));
    }

    @Override
    @Traced
    public Uni<Void> handle(DepositAmountCommand command) {
        return eventStoreDB.load(command.aggregateID(), BankAccountAggregate.class)
                .onItem().transform(aggregate -> {
                    aggregate.depositBalance(command.amount());
                    return aggregate;
                })
                .chain(aggregate -> eventStoreDB.save(aggregate))
                .onItem().invoke(() -> logger.infof("deposited amount: %s, id: %s", command.amount(), command.aggregateID()));
    }
}
