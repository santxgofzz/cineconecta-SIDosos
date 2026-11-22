package co.edu.icesi.cineconecta.playback;

import co.edu.icesi.cineconecta.persistence.mongo.document.ViewingSessionDocument;
import org.springframework.stereotype.Service;

/**
 * Progreso actual por unidad reproducible. En este commit solo define el
 * contrato usado por {@link PlaybackService}; el guardado de progreso, la
 * regla del 90 % y Continuar viendo se implementan en el siguiente (F4).
 */
@Service
public class ProgressService {
    public int resumePosition(long profileId, String unitKey, int durationSeconds) {
        return 0;
    }

    public void record(ViewingSessionDocument session) {
        // F4
    }

    public static boolean reachesCompletion(int positionSeconds, int durationSeconds) {
        return false;
    }
}
