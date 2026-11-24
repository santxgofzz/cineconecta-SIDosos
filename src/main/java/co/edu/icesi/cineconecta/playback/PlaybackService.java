package co.edu.icesi.cineconecta.playback;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.error.ErrorCodes;
import co.edu.icesi.cineconecta.common.resilience.TransientRetry;
import co.edu.icesi.cineconecta.integration.ProfileGuard;
import co.edu.icesi.cineconecta.persistence.mongo.document.ViewingSessionDocument;
import co.edu.icesi.cineconecta.persistence.mongo.repository.ViewingSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Reproducción simulada (RF-17, RF-18). Cada inicio crea una sesión histórica
 * nueva en {@code viewing_sessions}; los eventos la actualizan con control
 * optimista por {@code lastEventSeq}.
 *
 * <p>Tiempo visto vs. posición: WATCH suma a ambos; SEEK solo mueve la posición.
 * Además, el tiempo declarado por WATCH se limita al tiempo real transcurrido
 * desde el evento anterior (más una tolerancia), para que el cliente no pueda
 * inflar los minutos consumidos.</p>
 */
@Service
public class PlaybackService {
    static final String PAUSED = "paused";
    static final String FINISHED = "finished";
    static final String INTERRUPTED = "interrupted";

    private final PlaybackAuthorizationService authorization;
    private final ProfileGuard profiles;
    private final ViewingSessionRepository sessions;
    private final ProgressService progress;
    private final TransientRetry retry;
    private final Clock clock;
    private final int toleranceSeconds;

    public PlaybackService(PlaybackAuthorizationService authorization, ProfileGuard profiles,
                           ViewingSessionRepository sessions, ProgressService progress, TransientRetry retry,
                           Clock clock,
                           @Value("${cineconecta.playback.watch-tolerance-seconds:3}") int toleranceSeconds) {
        this.authorization = authorization;
        this.profiles = profiles;
        this.sessions = sessions;
        this.progress = progress;
        this.retry = retry;
        this.clock = clock;
        this.toleranceSeconds = toleranceSeconds;
    }

    public PlaybackState start(long accountId, long profileId, String contentId, String episodeId) {
        Instant now = clock.instant();
        PlaybackGrant grant = authorization.authorize(accountId, profileId, contentId, episodeId, now);

        int resumeAt = mongo(() -> progress.resumePosition(profileId, grant.unitKey(), grant.durationSeconds()));
        interruptOpenSessions(profileId, grant.unitKey(), now);

        ViewingSessionDocument session = new ViewingSessionDocument(UUID.randomUUID().toString(), profileId,
                grant.subscriptionId(), grant.content().id(), grant.episodeId(), grant.unitKey(),
                grant.content().type(), now, resumeAt, grant.durationSeconds());
        // insert con _id propio: un reintento tras un fallo ambiguo choca con la clave y no duplica.
        ViewingSessionDocument saved = mongo(() -> insertIdempotent(session));
        mongo(() -> {
            progress.record(saved);
            return null;
        });
        return PlaybackState.of(saved);
    }

    public PlaybackState state(long accountId, long profileId, String sessionId) {
        profiles.requireOwnedActiveProfile(accountId, profileId);
        return PlaybackState.of(loadOwned(profileId, sessionId));
    }

    public PlaybackState apply(long accountId, long profileId, String sessionId, PlaybackEvent event) {
        profiles.requireOwnedActiveProfile(accountId, profileId);
        validate(event);
        ViewingSessionDocument current = loadOwned(profileId, sessionId);

        if (event.seq() <= current.getLastEventSeq()) {
            // Reintento de un evento ya aplicado: no se vuelve a sumar, solo se re-sincroniza el progreso.
            mongo(() -> {
                progress.record(current);
                return null;
            });
            return PlaybackState.of(current);
        }
        if (!current.isOpen()) {
            throw ApiException.conflict(ErrorCodes.SESSION_CLOSED,
                    "La sesión ya terminó. Inicia una nueva reproducción para continuar.");
        }

        ViewingSessionDocument next = current.copy();
        Instant now = clock.instant();
        applyEvent(next, event, now);
        next.setLastEventSeq(event.seq());
        next.setLastEventAt(now);

        boolean applied = mongo(() -> retry.call(() -> sessions.compareAndUpdate(next, current.getLastEventSeq())));
        if (!applied) {
            ViewingSessionDocument reloaded = loadOwned(profileId, sessionId);
            if (reloaded.getLastEventSeq() >= event.seq()) {
                // La primera escritura sí llegó (respuesta perdida) o un duplicado concurrente ganó.
                mongo(() -> {
                    progress.record(reloaded);
                    return null;
                });
                return PlaybackState.of(reloaded);
            }
            throw ApiException.conflict(ErrorCodes.SESSION_CONFLICT,
                    "La sesión cambió mientras se procesaba el evento. Reintenta.");
        }
        mongo(() -> {
            progress.record(next);
            return null;
        });
        return PlaybackState.of(next);
    }

    private void applyEvent(ViewingSessionDocument s, PlaybackEvent e, Instant now) {
        int duration = s.getDurationSecondsSnapshot();
        switch (e.type()) {
            case WATCH -> {
                long elapsed = Math.max(0, Duration.between(s.getLastEventAt(), now).toSeconds());
                long allowedByClock = elapsed + toleranceSeconds;
                int remaining = Math.max(0, duration - s.getLastPositionSeconds());
                int played = (int) Math.min(Math.min(e.seconds(), allowedByClock), remaining);
                s.setWatchedSeconds(s.getWatchedSeconds() + played);
                s.setLastPositionSeconds(s.getLastPositionSeconds() + played);
            }
            case SEEK -> s.setLastPositionSeconds(Math.min(e.positionSeconds(), duration));
            case PAUSE -> s.close(now, PAUSED);
            case FINISH -> {
                s.setCompletedSnapshot(true);
                s.close(now, FINISHED);
            }
        }
        if (ProgressService.reachesCompletion(s.getLastPositionSeconds(), duration)) {
            s.setCompletedSnapshot(true);
        }
    }

    private static void validate(PlaybackEvent e) {
        if (e == null || e.seq() == null || e.seq() < 1 || e.type() == null) {
            throw ApiException.badRequest(ErrorCodes.INVALID_EVENT, "Evento inválido.");
        }
        if (e.type() == PlaybackEvent.Type.WATCH && (e.seconds() == null || e.seconds() < 1)) {
            throw ApiException.badRequest(ErrorCodes.INVALID_EVENT, "WATCH requiere seconds >= 1.");
        }
        if (e.type() == PlaybackEvent.Type.SEEK && (e.positionSeconds() == null || e.positionSeconds() < 0)) {
            throw ApiException.badRequest(ErrorCodes.INVALID_EVENT, "SEEK requiere positionSeconds >= 0.");
        }
    }

    private ViewingSessionDocument loadOwned(long profileId, String sessionId) {
        ViewingSessionDocument s = mongo(() -> retry.call(() -> sessions.findById(sessionId))).orElse(null);
        if (s == null || s.getProfileId() == null || s.getProfileId() != profileId) {
            // Sesión inexistente o de otro perfil: misma respuesta para no revelar datos ajenos.
            throw ApiException.notFound(ErrorCodes.SESSION_NOT_FOUND, "La sesión no existe.");
        }
        return s;
    }

    /** Sesiones abiertas de la misma unidad (pestaña cerrada, caída) se cierran como interrumpidas. */
    private void interruptOpenSessions(long profileId, String unitKey, Instant now) {
        var open = mongo(() -> retry.call(() -> sessions.findByProfileIdAndUnitKeyAndEndedAtIsNull(profileId, unitKey)));
        for (ViewingSessionDocument s : open) {
            ViewingSessionDocument closed = s.copy();
            closed.close(now, INTERRUPTED);
            closed.setLastEventAt(now);
            mongo(() -> retry.call(() -> sessions.compareAndUpdate(closed, s.getLastEventSeq())));
        }
    }

    private ViewingSessionDocument insertIdempotent(ViewingSessionDocument session) {
        try {
            return retry.call(() -> sessions.insert(session));
        } catch (org.springframework.dao.DuplicateKeyException dup) {
            return sessions.findById(session.getId()).orElseThrow(() -> dup);
        }
    }

    private static <T> T mongo(Supplier<T> op) {
        try {
            return op.get();
        } catch (DataAccessException ex) {
            throw ApiException.unavailable(ErrorCodes.MONGO_UNAVAILABLE,
                    "No se pudo confirmar la reproducción: la base de actividad no está disponible.", ex);
        }
    }
}
