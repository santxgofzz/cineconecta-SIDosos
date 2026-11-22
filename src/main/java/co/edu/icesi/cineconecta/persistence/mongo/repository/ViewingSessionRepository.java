package co.edu.icesi.cineconecta.persistence.mongo.repository;

import co.edu.icesi.cineconecta.persistence.mongo.document.ViewingSessionDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Las sesiones solo se insertan o se actualizan de forma condicional
 * ({@link ViewingSessionStore}); nunca se borran.
 */
public interface ViewingSessionRepository extends MongoRepository<ViewingSessionDocument, String>, ViewingSessionStore {
    /** Historial: usa el índice ix_sessions_profile_history. */
    Page<ViewingSessionDocument> findByProfileIdOrderByStartedAtDesc(Long profileId, Pageable pageable);

    /** Sesiones abiertas (sin endedAt) de una unidad, para cerrarlas como interrumpidas. */
    List<ViewingSessionDocument> findByProfileIdAndUnitKeyAndEndedAtIsNull(Long profileId, String unitKey);
}
