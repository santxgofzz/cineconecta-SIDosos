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

    @Override
    public void upsert(ViewingProgressDocument p) {
        Query query = Query.query(Criteria.where("profileId").is(p.getProfileId()).and("unitKey").is(p.getUnitKey()));
        Update update = new Update()
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
