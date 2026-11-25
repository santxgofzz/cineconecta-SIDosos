package co.edu.icesi.cineconecta.common.resilience;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.QueryTimeoutException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TransientRetryTest {
    TransientRetry retry = new TransientRetry(3, 0);

    @Test
    void retriesTransientFailuresUntilSuccess() {
        AtomicInteger calls = new AtomicInteger();
        String r = retry.call(() -> {
            if (calls.incrementAndGet() < 3) throw new QueryTimeoutException("timeout");
            return "ok";
        });
        assertEquals("ok", r);
        assertEquals(3, calls.get());
    }

    @Test
    void givesUpAfterMaxAttemptsAndRethrows() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(DataAccessResourceFailureException.class, () -> retry.call(() -> {
            calls.incrementAndGet();
            throw new DataAccessResourceFailureException("down");
        }));
        assertEquals(3, calls.get());
    }

    @Test
    void doesNotRetryBusinessErrors() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(DuplicateKeyException.class, () -> retry.call(() -> {
            calls.incrementAndGet();
            throw new DuplicateKeyException("E11000");
        }));
        assertEquals(1, calls.get());
    }
}
