package bankAccount.delivery.kafkaConsumer;

import es.Event;
import es.SerializerUtils;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class KafkaConsumer {

    @Inject
    Logger logger;

    @Incoming(value = "eventstore-in")
    public Uni<Void> process(Message<byte[]> message) {
        return Uni.createFrom().item(message)
                .onItem().invoke(eventsRecord -> {
                    logger.infof("consumer process >>> events: %s", new String(message.getPayload()));
                    final Event[] events = SerializerUtils.deserializeEventsFromJsonBytes(message.getPayload());
                    List.of(events).forEach(event -> logger.infof("deserialized event >>>>>>> %s", event));
                })
                .onFailure().invoke(Throwable::printStackTrace)
                .onItem().transform(Message::ack)
                .onFailure().invoke(Throwable::printStackTrace)
                .replaceWithVoid();
    }

}
