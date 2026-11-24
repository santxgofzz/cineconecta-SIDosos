package co.edu.icesi.cineconecta.config;

import co.edu.icesi.cineconecta.persistence.mongo.document.MyListItemDocument;
import co.edu.icesi.cineconecta.persistence.mongo.document.ViewingProgressDocument;
import co.edu.icesi.cineconecta.persistence.mongo.document.ViewingSessionDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * Garantiza al arrancar los índices de Mi lista, sesiones y progreso con los
 * mismos nombres que {@code database/mongodb/runtime/01_playback_activity.js}.
 * {@code ensureIndex} es idempotente. Los validadores $jsonSchema se aplican
 * con ese script (requieren privilegios collMod que la app no debería tener).
 */
@Component
@ConditionalOnProperty(name = "cineconecta.mongo.ensure-indexes", havingValue = "true", matchIfMissing = true)
public class MongoActivityIndexes implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(MongoActivityIndexes.class);
    private final MongoTemplate mongo;

    public MongoActivityIndexes(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void run(ApplicationArguments args) {
        IndexOperations myList = mongo.indexOps(MyListItemDocument.COLLECTION);
        myList.ensureIndex(new Index().on("profileId", Sort.Direction.ASC).on("contentId", Sort.Direction.ASC)
                .unique().named("uq_my_list_profile_content"));

        IndexOperations sessions = mongo.indexOps(ViewingSessionDocument.COLLECTION);
        sessions.ensureIndex(new Index().on("startedAt", Sort.Direction.ASC).on("subscriptionId", Sort.Direction.ASC)
                .named("ix_sessions_period_sub"));
        sessions.ensureIndex(new Index().on("profileId", Sort.Direction.ASC).on("startedAt", Sort.Direction.DESC)
                .named("ix_sessions_profile_history"));

        IndexOperations progress = mongo.indexOps(ViewingProgressDocument.COLLECTION);
        progress.ensureIndex(new Index().on("profileId", Sort.Direction.ASC).on("unitKey", Sort.Direction.ASC)
                .unique().named("uq_progress_profile_unit"));
        progress.ensureIndex(new Index().on("profileId", Sort.Direction.ASC).on("completed", Sort.Direction.ASC)
                .on("lastActivityAt", Sort.Direction.DESC).named("ix_progress_continue"));
        log.info("Índices de my_list, viewing_sessions y viewing_progress verificados");
    }
}
