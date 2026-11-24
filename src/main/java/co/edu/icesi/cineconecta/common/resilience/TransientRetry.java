package co.edu.icesi.cineconecta.common.resilience;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Reintenta operaciones IDEMPOTENTES ante fallos transitorios de conexión.
 * Solo debe envolver operaciones que puedan repetirse sin efectos dobles
 * (upsert, inserción protegida por índice único, actualización condicional).
 * Si se agotan los intentos se relanza el último error: el llamador decide el
 * mensaje y nunca informa éxito.
 */
@Component
public class TransientRetry {
    private final int maxAttempts;
    private final long backoffMillis;

    public TransientRetry(@Value("${cineconecta.retry.max-attempts:3}") int maxAttempts,
                          @Value("${cineconecta.retry.backoff-millis:150}") long backoffMillis) {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts debe ser >= 1");
        this.maxAttempts = maxAttempts;
        this.backoffMillis = Math.max(0, backoffMillis);
    }

    public <T> T call(Supplier<T> op) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return op.get();
            } catch (DataAccessResourceFailureException | TransientDataAccessException ex) {
                last = ex;
                if (attempt < maxAttempts) pause(attempt);
            }
        }
        throw last;
    }

    public void run(Runnable op) {
        call(() -> {
            op.run();
            return null;
        });
    }

    public static boolean isTransient(Throwable ex) {
        return ex instanceof DataAccessResourceFailureException || ex instanceof TransientDataAccessException;
    }

    private void pause(int attempt) {
        if (backoffMillis == 0) return;
        try {
            Thread.sleep(backoffMillis * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
