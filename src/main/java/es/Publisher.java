package es;

import java.util.List;

public interface Publisher {
    void publishEvents(final List<Event> events);
}