package co.edu.icesi.cineconecta.subscription;

import java.time.Instant;
import java.util.Objects;

/** Regla de negocio: inicio inclusivo y fin exclusivo. */
public final class SubscriptionWindow {
    private SubscriptionWindow() {}

    public static boolean isActive(Instant start, Instant end, Instant at) {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        Objects.requireNonNull(at);
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("El inicio debe ser anterior al fin");
        }
        return !at.isBefore(start) && at.isBefore(end);
    }
}
