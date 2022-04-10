package es;

import io.smallrye.mutiny.Uni;

public interface Projection {
    Uni<Void> when(Event event);
}
