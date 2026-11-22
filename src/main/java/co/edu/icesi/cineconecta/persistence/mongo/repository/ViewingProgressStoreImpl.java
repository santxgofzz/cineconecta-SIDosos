package co.edu.icesi.cineconecta.persistence.mongo.repository;

import co.edu.icesi.cineconecta.persistence.mongo.document.ViewingProgressDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

class ViewingProgressStoreImpl implements ViewingProgressStore {
    private final MongoTemplate mongo;

    ViewingProgressStoreImpl(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /**
     * Solo actualiza si el progreso guardado proviene de la misma sesión o es
     * más antiguo. Si existe uno más reciente de otra sesión, el filtro no
     * coincide, el upsert intenta insertar y el índice único lo rechaza
     * (DuplicateKeyException), que el servicio interpreta como "ya hay algo más nuevo".
     */
    @Override
    public void upsert(ViewingProgressDocument p) {
        Criteria notNewer = new Criteria().orOperator(
                Criteria.where("lastSessionId").is(p.getLastSessionId()),
                Criteria.where("lastActivityAt").lte(p.getLastActivityAt()));
        Query query = Query.query(new Criteria().andOperator(
                Criteria.where("profileId").is(p.getProfileId()),
                Criteria.where("unitKey").is(p.getUnitKey()),
                notNewer));
        Update update = new Update()
                .set("profileId", p.getProfileId())
                .set("unitKey", p.getUnitKey())
                .set("contentId", p.getContentId())
                .set("lastPositionSeconds", p.getLastPositionSeconds())
                .set("durationSeconds", p.getDurationSeconds())
                .set("completed", p.isCompleted())
                .set("lastActivityAt", p.getLastActivityAt())
                .set("lastSessionId", p.getLastSessionId());
        if (p.getEpisodeId() != null) {
            update.set("episodeId", p.getEpisodeId());
        }
        mongo.upsert(query, update, ViewingProgressDocument.class);
    }
}
