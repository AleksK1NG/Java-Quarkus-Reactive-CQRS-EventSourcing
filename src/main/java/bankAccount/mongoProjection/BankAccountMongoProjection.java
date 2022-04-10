package bankAccount.mongoProjection;

import bankAccount.domain.BankAccountAggregate;
import bankAccount.domain.BankAccountDocument;
import bankAccount.events.AddressUpdatedEvent;
import bankAccount.events.BalanceDepositedEvent;
import bankAccount.events.BankAccountCreatedEvent;
import bankAccount.events.EmailChangedEvent;
import bankAccount.repository.BankAccountMongoPanacheRepository;
import bankAccount.repository.BankAccountMongoRepository;
import es.Event;
import es.EventStoreDB;
import es.Projection;
import es.SerializerUtils;
import es.exceptions.InvalidEventTypeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import mappers.BankAccountMapper;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class BankAccountMongoProjection implements Projection {

    @Inject
    Logger logger;

    @Inject
    BankAccountMongoRepository mongoRepository;

    @Inject
    BankAccountMongoPanacheRepository panacheRepository;

    @Inject
    EventStoreDB eventStore;

    @Incoming(value = "eventstore-in")
    public Uni<Void> process(Message<byte[]> message) {
        logger.infof("(consumer) process >>> events: %s", new String(message.getPayload()));
        final Event[] events = SerializerUtils.deserializeEventsFromJsonBytes(message.getPayload());
        if (events.length == 0)
            return Uni.createFrom().voidItem()
                    .onItem().invoke(() -> logger.warnf("empty events list aggregateId: %s", events[0].getAggregateId()));

        return Multi.createFrom().iterable(List.of(events))
                .onItem().call(event -> this.when(event)
                        .onFailure().call(() -> panacheRepository.deleteByAggregateId(events[0].getAggregateId())
                                .onFailure().invoke(ex -> logger.error("panacheRepository.deleteByAggregateId id: %s", events[0].getAggregateId(), ex))
                                .onItem().call(e -> eventStore.load(events[0].getAggregateId(), BankAccountAggregate.class)
                                        .onFailure().invoke(ex -> logger.error("eventStore.load", ex))
                                        .onItem().call(bankAccountAggregate -> panacheRepository.persist(BankAccountMapper.bankAccountDocumentFromAggregate(bankAccountAggregate))))
                                .onFailure().invoke(ex -> logger.error("panacheRepository.persist bankAccountAggregate", ex))))
                .toUni().replaceWithVoid()
                .onItem().invoke(v -> message.ack())
                .onFailure().invoke(ex -> logger.error("consumer process events aggregateId: %s", events[0].getAggregateId(), ex));
    }

    public Uni<Void> when(Event event) {
        final var aggregateId = event.getAggregateId();
        logger.infof("(when) >>>>> aggregateId: %s", aggregateId);

        switch (event.getEventType()) {
            case BankAccountCreatedEvent.BANK_ACCOUNT_CREATED_V1 -> {
                return handle(SerializerUtils.deserializeFromJsonBytes(event.getData(), BankAccountCreatedEvent.class));
            }
            case EmailChangedEvent.EMAIL_CHANGED_V1 -> {
                return handle(SerializerUtils.deserializeFromJsonBytes(event.getData(), EmailChangedEvent.class));

            }
            case AddressUpdatedEvent.ADDRESS_UPDATED_V1 -> {
                return handle(SerializerUtils.deserializeFromJsonBytes(event.getData(), AddressUpdatedEvent.class));
            }
            case BalanceDepositedEvent.BALANCE_DEPOSITED -> {
                return handle(SerializerUtils.deserializeFromJsonBytes(event.getData(), BalanceDepositedEvent.class));
            }
            default -> {
                return Uni.createFrom().failure(new InvalidEventTypeException(event.getEventType()));
            }
        }
    }

    private Uni<Void> handle(BankAccountCreatedEvent event) {
        logger.infof("(when) BankAccountCreatedEvent: %s, aggregateID: %s", event, event.getAggregateId());

        final var document = BankAccountDocument.builder()
                .aggregateId(event.getAggregateId())
                .email(event.getEmail())
                .address(event.getAddress())
                .userName(event.getUserName())
                .balance(BigDecimal.valueOf(0))
                .build();
        return panacheRepository.persist(document)
                .onItem().invoke(result -> logger.infof("persist document result: %s", result))
                .onFailure().invoke(Throwable::printStackTrace)
                .replaceWithVoid();
    }

    private Uni<Void> handle(EmailChangedEvent event) {
        return panacheRepository.findByAggregateId(event.getAggregateId())
                .onFailure().invoke(Throwable::printStackTrace)
                .chain(bankAccountDocument -> {
                    bankAccountDocument.setEmail(event.getNewEmail());
                    return panacheRepository.update(bankAccountDocument);
                })
                .onFailure().invoke(Throwable::printStackTrace)
                .onItem().invoke(updatedDocument -> logger.infof("(EmailChangedEvent) updatedDocument: %s", updatedDocument))
                .replaceWithVoid();
    }

    private Uni<Void> handle(AddressUpdatedEvent event) {
        logger.infof("(when) AddressUpdatedEvent: %s, aggregateID: %s", event, event.getAggregateId());
        return panacheRepository.findByAggregateId(event.getAggregateId())
                .onFailure().invoke(Throwable::printStackTrace)
                .chain(bankAccountDocument -> {
                    bankAccountDocument.setAddress(event.getNewAddress());
                    return panacheRepository.update(bankAccountDocument);
                })
                .onFailure().invoke(Throwable::printStackTrace)
                .onItem().invoke(updatedDocument -> logger.infof("(AddressUpdatedEvent) updatedDocument: %s", updatedDocument))
                .replaceWithVoid();
    }

    private Uni<Void> handle(BalanceDepositedEvent event) {
        logger.infof("(when) BalanceDepositedEvent: %s, aggregateID: %s", event, event.getAggregateId());

        return panacheRepository.findByAggregateId(event.getAggregateId())
                .onFailure().invoke(Throwable::printStackTrace)
                .chain(bankAccountDocument -> {
                    final var balance = bankAccountDocument.getBalance();
                    bankAccountDocument.setBalance(balance.add(event.getAmount()));
                    return panacheRepository.update(bankAccountDocument);
                })
                .onFailure().invoke(Throwable::printStackTrace)
                .onItem().invoke(updatedDocument -> logger.infof("(BalanceDepositedEvent) updatedDocument: %s", updatedDocument))
                .replaceWithVoid();
    }
}