package co.edu.icesi.cineconecta.persistence.mongo.repository;

import co.edu.icesi.cineconecta.persistence.mongo.document.ViewingSessionDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/** Actualización condicional (optimista) de una sesión abierta. */
class ViewingSessionStoreImpl implements ViewingSessionStore {
    private final MongoTemplate mongo;

    ViewingSessionStoreImpl(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public boolean compareAndUpdate(ViewingSessionDocument s, int expectedSeq) {
        Query query = Query.query(Criteria.where("_id").is(s.getId())
                .and("endedAt").is(null)
                .and("lastEventSeq").is(expectedSeq));
        Update update = new Update()
                .set("watchedSeconds", s.getWatchedSeconds())
                .set("lastPositionSeconds", s.getLastPositionSeconds())
                .set("completedSnapshot", s.isCompletedSnapshot())
                .set("lastEventSeq", s.getLastEventSeq())
                .set("lastEventAt", s.getLastEventAt());
        if (!s.isOpen()) {
            update.set("endedAt", s.getEndedAt()).set("endReason", s.getEndReason());
        }
        return mongo.updateFirst(query, update, ViewingSessionDocument.class).getModifiedCount() == 1;
    }
}
