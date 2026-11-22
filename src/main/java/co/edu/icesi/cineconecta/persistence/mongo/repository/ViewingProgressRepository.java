package co.edu.icesi.cineconecta.persistence.mongo.repository;

import co.edu.icesi.cineconecta.persistence.mongo.document.ViewingProgressDocument;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ViewingProgressRepository extends MongoRepository<ViewingProgressDocument, ObjectId>,
        ViewingProgressStore {
    Optional<ViewingProgressDocument> findByProfileIdAndUnitKey(Long profileId, String unitKey);

    /** Continuar viendo: usa el índice ix_progress_continue. */
    List<ViewingProgressDocument> findByProfileIdAndCompletedFalseOrderByLastActivityAtDesc(Long profileId,
                                                                                         Pageable pageable);
}
