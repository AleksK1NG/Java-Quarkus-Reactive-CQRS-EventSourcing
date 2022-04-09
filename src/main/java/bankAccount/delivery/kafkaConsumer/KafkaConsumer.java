package bankAccount.delivery.kafkaConsumer;

import bankAccount.events.AddressUpdatedEvent;
import bankAccount.events.BalanceDepositedEvent;
import bankAccount.events.BankAccountCreatedEvent;
import bankAccount.events.EmailChangedEvent;
import es.Event;
import es.SerializerUtils;
import es.exceptions.InvalidEventTypeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class KafkaConsumer {

    @Inject
    Logger logger;

    @Incoming(value = "eventstore-in")
    public Uni<Void> process(Message<byte[]> message) {
        return Uni.createFrom().item(message)
                .onItem().transformToMulti(eventsRecord -> {
                    logger.infof("consumer process >>> events: %s", new String(message.getPayload()));
                    final Event[] events = SerializerUtils.deserializeEventsFromJsonBytes(message.getPayload());
                    return Multi.createFrom().items(events);
                }).onItem().invoke(this::when)
                .onFailure().invoke(ex -> logger.errorf("process Multi: %s", ex.getMessage()))
                .toUni()
                .onItem().transform(e -> message.ack()).replaceWithVoid()
                .onFailure().invoke(ex -> logger.errorf("process ack exception: %s", ex.getMessage()));

//                .onItem().invoke(eventsRecord -> {
//                    logger.infof("consumer process >>> events: %s", new String(message.getPayload()));
//                    final Event[] events = SerializerUtils.deserializeEventsFromJsonBytes(message.getPayload());
//                    List.of(events).forEach(event -> logger.infof("deserialized event >>>>>>> %s", event));
//                })
//                .onFailure().invoke(Throwable::printStackTrace)
//                .onItem().transform(Message::ack)
//                .onFailure().invoke(Throwable::printStackTrace)
//                .replaceWithVoid();
    }

    private Uni<Void> when(Event event) {
        final var aggregateId = event.getAggregateId();
        logger.infof("(when) aggregateId: %s", aggregateId);

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
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> handle(EmailChangedEvent event) {
        logger.infof("(when) EmailChangedEvent: %s, aggregateID: %s", event, event.getAggregateId());
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> handle(AddressUpdatedEvent event) {
        logger.infof("(when) AddressUpdatedEvent: %s, aggregateID: %s", event, event.getAggregateId());
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> handle(BalanceDepositedEvent event) {
        logger.infof("(when) BalanceDepositedEvent: %s, aggregateID: %s", event, event.getAggregateId());
        return Uni.createFrom().voidItem();
    }
}
