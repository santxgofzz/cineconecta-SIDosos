package co.edu.icesi.cineconecta.playback;

import co.edu.icesi.cineconecta.common.resilience.TransientRetry;
import co.edu.icesi.cineconecta.persistence.mongo.document.ViewingProgressDocument;
import co.edu.icesi.cineconecta.persistence.mongo.document.ViewingSessionDocument;
import co.edu.icesi.cineconecta.persistence.mongo.repository.ViewingProgressRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Progreso actual por (perfil, unidad) — RF-19, RF-20.
 * <ul>
 *   <li>Se deriva de la sesión: guarda la última posición, la duración, la
 *       actividad más reciente y la sesión que lo produjo.</li>
 *   <li>Completado al finalizar manualmente o con posición &gt;= 90 % de la duración.</li>
 *   <li>Una sesión nueva sobre una unidad completada empieza en 0 y restablece el
 *       progreso; las sesiones históricas no se tocan.</li>
 *   <li>Un evento antiguo (reintento tardío) no sobrescribe un progreso más reciente.</li>
 * </ul>
 */
@Service
public class ProgressService {
    private final ViewingProgressRepository repository;
    private final TransientRetry retry;

    public ProgressService(ViewingProgressRepository repository, TransientRetry retry) {
        this.repository = repository;
        this.retry = retry;
    }

    /** Regla del 90 %, en aritmética entera para evitar redondeos. */
    public static boolean reachesCompletion(int positionSeconds, int durationSeconds) {
        return durationSeconds > 0 && (long) positionSeconds * 10 >= (long) durationSeconds * 9;
    }

    /** Posición desde la que se reanuda: la guardada si está pendiente; 0 si estaba completada o no existe. */
    public int resumePosition(long profileId, String unitKey, int durationSeconds) {
        return retry.call(() -> repository.findByProfileIdAndUnitKey(profileId, unitKey))
                .filter(p -> !p.isCompleted())
                .map(p -> Math.max(0, Math.min(p.getLastPositionSeconds(), durationSeconds)))
                .orElse(0);
    }

    public void record(ViewingSessionDocument s) {
        ViewingProgressDocument progress = ViewingProgressDocument.of(s.getProfileId(), s.getContentId(),
                s.getEpisodeId(), s.getUnitKey(), s.getLastPositionSeconds(), s.getDurationSecondsSnapshot(),
                s.isCompletedSnapshot(), s.getLastEventAt(), s.getId());
        try {
            retry.run(() -> repository.upsert(progress));
        } catch (DuplicateKeyException newerExists) {
            // El filtro no coincidió porque ya hay progreso más reciente de otra sesión: se conserva ese.
        }
    }
}
