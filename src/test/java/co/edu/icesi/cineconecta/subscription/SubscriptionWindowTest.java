package co.edu.icesi.cineconecta.subscription;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class SubscriptionWindowTest {
    private final Instant start = Instant.parse("2026-11-01T00:00:00Z");
    private final Instant end = Instant.parse("2026-12-01T00:00:00Z");

    @Test void startIncluded() {
        assertTrue(SubscriptionWindow.isActive(start, end, start));
    }

    @Test void endExcluded() {
        assertFalse(SubscriptionWindow.isActive(start, end, end));
    }

    @Test void invalidPeriodRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> SubscriptionWindow.isActive(end, start, start));
    }
}
