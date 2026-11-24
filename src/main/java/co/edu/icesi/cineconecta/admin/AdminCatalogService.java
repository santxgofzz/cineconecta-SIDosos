package co.edu.icesi.cineconecta.admin;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.error.ErrorCodes;
import co.edu.icesi.cineconecta.persistence.mongo.document.ContentDocument;
import co.edu.icesi.cineconecta.persistence.mongo.repository.ContentRepository;
import jakarta.validation.Validator;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static co.edu.icesi.cineconecta.integration.CatalogEntry.DOCUMENTARY;
import static co.edu.icesi.cineconecta.integration.CatalogEntry.MOVIE;
import static co.edu.icesi.cineconecta.integration.CatalogEntry.PUBLISHED;
import static co.edu.icesi.cineconecta.integration.CatalogEntry.RETIRED;
import static co.edu.icesi.cineconecta.integration.CatalogEntry.SERIES;

@Service
public class AdminCatalogService {
    private static final String DRAFT = "draft";
    private static final List<String> EDITABLE_FIELDS = List.of(
            "title", "synopsis", "releaseYear", "genres", "countries", "audioLanguages",
            "subtitleLanguages", "ageRating", "coverUrl", "durationSeconds", "director",
            "cast", "seasons", "topics", "interviewees", "sources");

    private final ContentRepository contents;
    private final MongoTemplate mongo;
    private final Validator validator;
    private final Clock clock;

    public AdminCatalogService(ContentRepository contents, MongoTemplate mongo, Validator validator, Clock clock) {
        this.contents = contents;
        this.mongo = mongo;
        this.validator = validator;
        this.clock = clock;
    }

    public Page<AdminCatalogItem> list(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw invalid("La página debe ser positiva o cero y el tamaño debe estar entre 1 y 100.");
        }
        return contents.findAll(PageRequest.of(page, size, Sort.by("id"))).map(AdminCatalogItem::from);
    }

    public AdminCatalogItem get(String id) {
        return AdminCatalogItem.from(requireContent(id));
    }

    public AdminCatalogItem create(CatalogInput input) {
        validate(input);
        var seasons = mergeSeasons(input.seasons(), List.of());
        Instant now = Instant.ofEpochMilli(clock.millis());
        return AdminCatalogItem.from(contents.insert(document(input, "CNT-" + UUID.randomUUID(),
                DRAFT, now, now, seasons)));
    }

    public AdminCatalogItem update(String id, long revision, CatalogInput input) {
        var previous = requireContent(id);
        requireRevision(previous, revision);
        validate(input);
        if (!previous.type().equals(input.type())) {
            throw conflict("El tipo de contenido es estable.");
        }
        var seasons = mergeSeasons(input.seasons(), previous.seasons());
        var replacement = document(input, id, previous.status(), previous.createdAt(), previous.updatedAt(), seasons);
        Document fields = new Document();
        mongo.getConverter().write(replacement, fields);
        Update change = new Update();
        for (String field : EDITABLE_FIELDS) {
            if (fields.containsKey(field)) {
                change.set(field, fields.get(field));
            } else {
                change.unset(field);
            }
        }
        return apply(previous, change);
    }

    public AdminCatalogItem publish(String id) {
        var previous = requireContent(id);
        if (PUBLISHED.equals(previous.status())) {
            return AdminCatalogItem.from(previous);
        }
        CatalogInput input = CatalogInput.from(previous);
        validate(input);
        mergeSeasons(input.seasons(), previous.seasons());
        return apply(previous, new Update().set("status", PUBLISHED));
    }

    public AdminCatalogItem retire(String id) {
        var previous = requireContent(id);
        if (RETIRED.equals(previous.status())) {
            return AdminCatalogItem.from(previous);
        }
        return apply(previous, new Update().set("status", RETIRED));
    }

    private ContentDocument requireContent(String id) {
        return contents.findById(id).orElseThrow(() ->
                ApiException.notFound(ErrorCodes.CONTENT_NOT_FOUND, "No se encontró el contenido."));
    }

    private static void requireRevision(ContentDocument previous, long revision) {
        if (previous.updatedAt().toEpochMilli() != revision) {
            throw conflict("El contenido cambió. Vuelve a consultarlo antes de editarlo.");
        }
    }

    private AdminCatalogItem apply(ContentDocument previous, Update change) {
        long nextRevision = Math.max(clock.millis(), previous.updatedAt().toEpochMilli() + 1);
        Query query = Query.query(Criteria.where("_id").is(previous.id()).and("updatedAt").is(previous.updatedAt()));
        ContentDocument updated = mongo.findAndModify(query, change.set("updatedAt", Instant.ofEpochMilli(nextRevision)),
                FindAndModifyOptions.options().returnNew(true), ContentDocument.class);
        if (updated == null) {
            throw conflict("Otra operación cambió el contenido. Vuelve a consultarlo.");
        }
        return AdminCatalogItem.from(updated);
    }

    private void validate(CatalogInput input) {
        var violations = validator.validate(input);
        if (!violations.isEmpty()) {
            String field = violations.stream().map(v -> v.getPropertyPath().toString()).sorted().findFirst().orElseThrow();
            throw invalid("Campo inválido: " + field);
        }
        boolean singlePiece = input.durationSeconds() != null && input.director() != null && !input.director().isBlank();
        boolean documentaryFields = input.topics() != null || input.interviewees() != null || input.sources() != null;
        boolean validType = switch (input.type()) {
            case MOVIE -> singlePiece && input.cast() != null && input.seasons() == null && !documentaryFields;
            case SERIES -> input.seasons() != null && !input.seasons().isEmpty() && input.durationSeconds() == null
                    && input.director() == null && input.cast() == null && !documentaryFields;
            case DOCUMENTARY -> singlePiece && input.cast() == null && input.seasons() == null
                    && input.topics() != null && !input.topics().isEmpty();
            default -> false;
        };
        if (!validType) {
            throw invalid("Los campos específicos no coinciden con el tipo de contenido.");
        }
        requireHttpUrl(input.coverUrl());
        if (input.sources() != null) {
            input.sources().stream().map(CatalogInput.Source::url)
                    .filter(url -> url != null && !url.isBlank()).forEach(AdminCatalogService::requireHttpUrl);
        }
    }

    private static void requireHttpUrl(String value) {
        try {
            URI uri = URI.create(value.strip());
            if (uri.getHost() != null && ("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) {
                return;
            }
        } catch (IllegalArgumentException ignored) {
            // Mensaje de validación uniforme.
        }
        throw invalid("La dirección debe usar HTTP o HTTPS.");
    }

    private record Position(int season, int episode) {}

    private static List<ContentDocument.Season> mergeSeasons(List<CatalogInput.Season> requested,
                                                            List<ContentDocument.Season> previous) {
        if (requested == null) {
            return null;
        }
        Map<String, Position> retained = new HashMap<>();
        if (previous != null) {
            for (var season : previous) {
                for (var episode : season.episodes()) {
                    retained.put(episode.episodeId(), new Position(season.number(), episode.number()));
                }
            }
        }
        Set<Integer> seasonNumbers = new HashSet<>();
        List<ContentDocument.Season> result = requested.stream().map(season -> {
            if (!seasonNumbers.add(season.number())) {
                throw invalid("El número de temporada se repite.");
            }
            Set<Integer> episodeNumbers = new HashSet<>();
            var episodes = season.episodes().stream().map(episode -> {
                if (!episodeNumbers.add(episode.number())) {
                    throw invalid("El número de episodio se repite en la temporada.");
                }
                return keepEpisode(retained, season.number(), episode);
            }).toList();
            return new ContentDocument.Season(season.number(), season.description().strip(), episodes);
        }).toList();
        if (!retained.isEmpty()) {
            throw conflict("La edición quitaría episodios existentes.");
        }
        return result;
    }

    private static ContentDocument.Episode keepEpisode(Map<String, Position> retained, int season,
                                                       CatalogInput.Episode episode) {
        String id = episode.id();
        if (id == null) {
            id = "EP-" + UUID.randomUUID();
        } else if (!new Position(season, episode.number()).equals(retained.remove(id))) {
            throw conflict("El ID de episodio es desconocido, se repite o cambió de posición.");
        }
        return new ContentDocument.Episode(id, episode.number(), episode.title().strip(), episode.synopsis().strip(),
                episode.durationSeconds());
    }

    private static ContentDocument document(CatalogInput input, String id, String status, Instant created,
                                            Instant updated, List<ContentDocument.Season> seasons) {
        return new ContentDocument(id, input.type(), input.title().strip(), input.synopsis().strip(),
                input.releaseYear(), strings(input.genres()), strings(input.countries()), strings(input.audioLanguages()),
                strings(input.subtitleLanguages()), input.ageRating(), input.coverUrl().strip(), status, created, updated,
                input.durationSeconds(), strip(input.director()),
                CatalogInput.map(input.cast(), item -> new ContentDocument.CastMember(item.actorName().strip(), strip(item.characterName()))),
                seasons, strings(input.topics()), strings(input.interviewees()),
                CatalogInput.map(input.sources(), item -> new ContentDocument.Source(item.name().strip(), strip(item.url()))));
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }

    private static List<String> strings(List<String> values) {
        return CatalogInput.map(values, String::strip);
    }

    private static ApiException invalid(String message) {
        return ApiException.badRequest(ErrorCodes.INVALID_CONTENT, message);
    }

    private static ApiException conflict(String message) {
        return ApiException.conflict(ErrorCodes.CONTENT_CONFLICT, message);
    }
}
