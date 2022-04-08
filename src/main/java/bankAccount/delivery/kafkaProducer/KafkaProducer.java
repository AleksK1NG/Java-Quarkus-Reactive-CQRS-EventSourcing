package bankAccount.delivery.kafkaProducer;


import es.Event;
import es.SerializerUtils;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.KafkaClientService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class KafkaProducer {

    @Inject
    Logger logger;

    @Inject
    KafkaClientService kafkaClientService;

    public Uni<Void> publish(List<Event> events) {
//        final var aggregateTypeTopic = EventSourcingUtils.getAggregateTypeTopic(events.get(0).getAggregateType());
        final byte[] eventsBytes = SerializerUtils.serializeToJsonBytes(events.toArray(new Event[]{}));
        final ProducerRecord<String, byte[]> record = new ProducerRecord<>("eventstore", eventsBytes);
        logger.infof("publish kafka record topic >>>>> %s", record.topic());

        return kafkaClientService.<String, byte[]>getProducer("eventstore-out")
                .send(record)
                .onFailure().invoke(Throwable::printStackTrace)
                .onItem().invoke(msg -> logger.infof("publish key: %s, value: %s", record.key(), new String(record.value())))
                .replaceWithVoid();
    }

//    @Outgoing("eventstore-out")
//    public Uni<ProducerRecord<String, byte[]>> publishEvent() {
//        return Uni.createFrom().item(() -> {
//            final var bytes = SerializerUtils.serializeToJsonBytes(event);
//            final ProducerRecord<String, byte[]> record = new ProducerRecord<>("eventstore", bytes);
//            logger.infof("publish kafka record >>>>> %s", record);
//            return record;
//        });
//    }
}
