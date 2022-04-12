package es;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.KafkaClientService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.opentracing.Traced;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@ApplicationScoped
public class KafkaEventBus implements EventBus {

    private final static Logger logger = Logger.getLogger(KafkaEventBus.class);

    private static final int PUBLISH_TIMEOUT = 1000;
    private static final int BACKOFF_TIMEOUT = 300;
    private static final int RETRY_COUNT = 3;

    @Inject
    KafkaClientService kafkaClientService;

    @ConfigProperty(name = "mp.messaging.incoming.eventstore-in.topic", defaultValue = "eventstore")
    String eventStoreTopic;


    @Traced
    public Uni<Void> publish(List<Event> events) {
        final byte[] eventsBytes = SerializerUtils.serializeToJsonBytes(events.toArray(new Event[]{}));
        final ProducerRecord<String, byte[]> record = new ProducerRecord<>(eventStoreTopic, eventsBytes);
        logger.infof("publish kafka record value >>>>> %s", new String(record.value()));

        return kafkaClientService.<String, byte[]>getProducer("eventstore-out")
                .send(record)
                .ifNoItem().after(Duration.ofMillis(PUBLISH_TIMEOUT)).fail()
                .onFailure().invoke(Throwable::printStackTrace)
                .onFailure().retry().withBackOff(Duration.of(BACKOFF_TIMEOUT, ChronoUnit.MILLIS)).atMost(RETRY_COUNT)
                .onItem().invoke(msg -> logger.infof("publish key: %s, value: %s", record.key(), new String(record.value())))
                .replaceWithVoid();
    }
}
