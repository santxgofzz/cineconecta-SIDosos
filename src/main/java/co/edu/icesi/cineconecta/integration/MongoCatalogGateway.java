package co.edu.icesi.cineconecta.integration;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Proyección mínima sobre la colección {@code contents}. */
@Component
public class MongoCatalogGateway implements CatalogGateway {
    static final String COLLECTION = "contents";
    private final MongoTemplate mongo;

    public MongoCatalogGateway(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public Optional<CatalogEntry> findById(String contentId) {
        if (contentId == null || contentId.isBlank()) return Optional.empty();
        Query q = project(Query.query(Criteria.where("_id").is(contentId)));
        return Optional.ofNullable(mongo.findOne(q, CatalogEntry.class, COLLECTION));
    }

    @Override
    public Map<String, CatalogEntry> findByIds(Collection<String> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) return Map.of();
        Query q = project(Query.query(Criteria.where("_id").in(contentIds)));
        List<CatalogEntry> found = mongo.find(q, CatalogEntry.class, COLLECTION);
        return found.stream().collect(Collectors.toMap(CatalogEntry::id, Function.identity(), (a, b) -> a));
    }

    private static Query project(Query q) {
        q.fields().include("type", "title", "status", "ageRating", "durationSeconds",
                "seasons.number", "seasons.episodes.id", "seasons.episodes.number",
                "seasons.episodes.title", "seasons.episodes.durationSeconds");
        return q;
    }
}
