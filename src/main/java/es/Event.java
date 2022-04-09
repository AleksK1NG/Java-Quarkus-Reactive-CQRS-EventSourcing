package es;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    public Event(String eventType, String aggregateType) {
        this.id = UUID.randomUUID();
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.timeStamp = ZonedDateTime.now();
    }

    private UUID id;

    private String aggregateId;

    private String eventType;

    private String aggregateType;

    private long version;

    private byte[] data;

    private byte[] metaData;

//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    private ZonedDateTime timeStamp;


    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", aggregateId='" + aggregateId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", aggregateType='" + aggregateType + '\'' +
                ", version=" + version + '\'' +
                ", timeStamp=" + timeStamp + '\'' +
                ", data=" + new String(data) + '\'' +
                '}';
    }
}