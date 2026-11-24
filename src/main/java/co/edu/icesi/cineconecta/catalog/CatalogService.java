package co.edu.icesi.cineconecta.catalog;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.error.ErrorCodes;
import co.edu.icesi.cineconecta.integration.AccountDataGateway;
import co.edu.icesi.cineconecta.integration.ProfileGuard;
import co.edu.icesi.cineconecta.persistence.mongo.document.ContentDocument;
import org.bson.BsonType;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.schema.JsonSchemaObject;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class CatalogService {
    private static final Sort LIST_ORDER = Sort.by("title", "id");
    private final ProfileGuard profiles;
    private final AccountDataGateway accounts;
    private final MongoTemplate mongo;
    private final Clock clock;

    public CatalogService(ProfileGuard profiles, AccountDataGateway accounts, MongoTemplate mongo, Clock clock) {
        this.profiles = profiles;
        this.accounts = accounts;
        this.mongo = mongo;
        this.clock = clock;
    }

    public record Item(String id, String title, String type, Integer releaseYear, Integer ageRating,
                       String coverUrl, List<String> genres) {
        private static Item from(ContentDocument content) {
            return new Item(content.id(), content.title(), content.type(), content.releaseYear(),
                    content.ageRating(), content.coverUrl(), content.genres());
        }
    }

    public record Results(List<Item> items, int page, int size, long totalElements, long totalPages,
                          CatalogFilters filters) {}

    public record Detail(Item content, String synopsis, List<String> countries, List<String> audioLanguages,
                         List<String> subtitleLanguages, Integer durationSeconds, String director, List<Season> seasons) {}

    public record Season(Integer number, String description, List<Episode> episodes) {}

    public record Episode(String id, Integer number, String title, String synopsis, Integer durationSeconds) {}

    public Results search(long accountId, long profileId, CatalogFilters filters) {
        int maxAge = authorize(accountId, profileId);
        Query query = filtered(maxAge, filters);
        try {
            long total = mongo.count(query, ContentDocument.class);
            Query page = Query.of(query).with(LIST_ORDER).limit(filters.size())
                    .skip((long) filters.page() * filters.size());
            page.fields().include("title", "type", "releaseYear", "ageRating", "coverUrl", "genres");
            List<Item> items = mongo.find(page, ContentDocument.class).stream().map(Item::from).toList();
            return new Results(items, filters.page(), filters.size(), total,
                    Math.ceilDiv(total, filters.size()), filters);
        } catch (DataAccessException ex) {
            throw unavailable(ex);
        }
    }

    public Detail detail(long accountId, long profileId, String id) {
        int maxAge = authorize(accountId, profileId);
        if (id.isBlank() || id.length() > 128) {
            throw CatalogFilters.invalid("Identificador de contenido inválido.");
        }
        ContentDocument document;
        try {
            document = mongo.findOne(visible(maxAge, null).addCriteria(Criteria.where("_id").is(id)), ContentDocument.class);
        } catch (DataAccessException ex) {
            throw unavailable(ex);
        }
        if (document == null) {
            throw ApiException.notFound(ErrorCodes.CONTENT_NOT_FOUND, "El contenido no está disponible en este perfil.");
        }
        return new Detail(Item.from(document), document.synopsis(), document.countries(), document.audioLanguages(),
                document.subtitleLanguages(), document.durationSeconds(), document.director(), orderedSeasons(document));
    }

    private int authorize(long accountId, long profileId) {
        var profile = profiles.requireOwnedActiveProfile(accountId, profileId);
        try {
            if (accounts.findActiveSubscriptionId(accountId, clock.instant()).isEmpty()) {
                throw ApiException.forbidden(ErrorCodes.SUBSCRIPTION_REQUIRED, "Necesitas una suscripción vigente.");
            }
        } catch (DataAccessException ex) {
            throw ApiException.unavailable(ErrorCodes.ORACLE_UNAVAILABLE,
                    "No se pudo verificar la suscripción.", ex);
        }
        return Math.min(profile.maxAgeRating(), 18);
    }

    private static Query filtered(int maxAge, CatalogFilters filters) {
        Query query = visible(maxAge, filters.type());
        if (filters.q() != null) {
            query.addCriteria(Criteria.where("title").regex(Pattern.compile(Pattern.quote(filters.q()), Pattern.CASE_INSENSITIVE)));
        }
        if (filters.genre() != null) {
            query.addCriteria(Criteria.where("genres").is(filters.genre()));
        }
        if (filters.language() != null) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("audioLanguages").is(filters.language()),
                    Criteria.where("subtitleLanguages").is(filters.language())));
        }
        if (filters.year() != null) {
            query.addCriteria(Criteria.where("releaseYear").is(filters.year()));
        }
        return query;
    }

    private static Query visible(int maxAge, String selectedType) {
        Criteria type = Criteria.where("type");
        if (selectedType == null) {
            type.in(CatalogFilters.TYPES);
        } else {
            type.is(selectedType);
        }
        return Query.query(Criteria.where("status").is("published")
                .and("ageRating").type(BsonType.INT32.getValue()).gte(0).lte(maxAge)
                .andOperator(type,
                        Criteria.where("ageRating").not().type(JsonSchemaObject.Type.ARRAY),
                        Criteria.where("status").not().type(JsonSchemaObject.Type.ARRAY),
                        Criteria.where("type").not().type(JsonSchemaObject.Type.ARRAY)));
    }

    private static List<Season> orderedSeasons(ContentDocument document) {
        if (document.seasons() == null) {
            return List.of();
        }
        return document.seasons().stream().sorted(Comparator.comparing(ContentDocument.Season::number))
                .map(season -> new Season(season.number(), season.description(), season.episodes().stream()
                        .sorted(Comparator.comparing(ContentDocument.Episode::number))
                        .map(ep -> new Episode(ep.episodeId(), ep.number(), ep.title(), ep.synopsis(),
                                ep.durationSeconds())).toList())).toList();
    }

    private static ApiException unavailable(DataAccessException cause) {
        return ApiException.unavailable(ErrorCodes.MONGO_UNAVAILABLE, "El catálogo no está disponible temporalmente.", cause);
    }
}
