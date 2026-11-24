package co.edu.icesi.cineconecta.persistence.mongo.repository;

import co.edu.icesi.cineconecta.persistence.mongo.document.MyListItemDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/** Consultas por perfil apoyadas en el índice único (profileId, contentId). */
public interface MyListRepository extends MongoRepository<MyListItemDocument, ObjectId> {
    List<MyListItemDocument> findByProfileIdOrderByAddedAtDesc(Long profileId);

    Optional<MyListItemDocument> findByProfileIdAndContentId(Long profileId, String contentId);

    long deleteByProfileIdAndContentId(Long profileId, String contentId);
}
