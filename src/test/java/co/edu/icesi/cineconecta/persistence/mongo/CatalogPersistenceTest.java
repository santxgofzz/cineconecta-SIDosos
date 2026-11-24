package co.edu.icesi.cineconecta.persistence.mongo;

import co.edu.icesi.cineconecta.integration.CatalogEntry;
import co.edu.icesi.cineconecta.persistence.mongo.document.AdminUserDocument;
import co.edu.icesi.cineconecta.persistence.mongo.document.ContentDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogPersistenceTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String CONTENT_ID = "0123456789abcdef01234567";

    private final MappingMongoConverter converter = converter();

    @Test
    void contentKeepsStringIdAndBsonDates() {
        Document raw = new Document("_id", CONTENT_ID).append("type", "movie")
                .append("title", "Película de prueba").append("durationSeconds", 3600)
                .append("createdAt", Date.from(NOW)).append("updatedAt", Date.from(NOW));
        ContentDocument content = converter.read(ContentDocument.class, raw);
        Document written = new Document();
        converter.write(content, written);

        assertThat(written).containsEntry("_id", CONTENT_ID)
                .containsEntry("durationSeconds", 3600).containsEntry("createdAt", Date.from(NOW));
        assertThat(converter.read(CatalogEntry.class, written).durationSeconds()).isEqualTo(3600);
        QueryMapper queries = new QueryMapper(converter);
        assertThat(queries.getMappedObject(new Document("_id", CONTENT_ID),
                converter.getMappingContext().getPersistentEntity(CatalogEntry.class)).get("_id"))
                .isEqualTo(CONTENT_ID);
    }

    @Test
    void episodeKeepsIdFieldCompatibleWithPlaybackProjection() {
        Document episode = new Document("id", "EP-002-01").append("number", 1)
                .append("title", "Inicio").append("synopsis", "Primer capítulo")
                .append("durationSeconds", 1500);
        Document season = new Document("number", 1).append("description", "Primera")
                .append("episodes", List.of(episode));
        Document raw = new Document("_id", "CNT-002").append("type", "series")
                .append("seasons", List.of(season));
        Document written = new Document();
        converter.write(converter.read(ContentDocument.class, raw), written);

        assertThat(written.getList("seasons", Document.class).getFirst()
                .getList("episodes", Document.class).getFirst()).containsEntry("id", "EP-002-01")
                .doesNotContainKey("_id");
        assertThat(converter.read(CatalogEntry.class, written).findEpisode("EP-002-01"))
                .contains(new CatalogEntry.Episode("EP-002-01", 1, "Inicio", 1500));
    }

    @Test
    void adminKeepsObjectIdAndNormalizesUsername() {
        ObjectId id = new ObjectId(CONTENT_ID);
        Document raw = new Document("_id", id).append("usernameNormalized", "admin.i")
                .append("passwordHash", "fixture").append("role", "ADMIN")
                .append("active", false).append("createdAt", Date.from(NOW));
        Document written = new Document();
        converter.write(converter.read(AdminUserDocument.class, raw), written);

        assertThat(written).containsEntry("_id", id).containsEntry("role", "ADMIN")
                .containsEntry("createdAt", Date.from(NOW));
        assertThat(AdminUserDocument.normalizeUsername("  ADMIN.I  ")).isEqualTo("admin.i");
    }

    @Test
    void adminHashIsExcludedFromJsonAndStringRepresentation() throws Exception {
        AdminUserDocument admin = new AdminUserDocument(new ObjectId(), "admin.i", "fixture", "ADMIN", false, NOW);
        assertThat(new ObjectMapper().findAndRegisterModules().writeValueAsString(admin))
                .contains("admin.i").doesNotContain("passwordHash", "fixture");
        assertThat(admin.toString()).doesNotContain("fixture");
    }

    private static MappingMongoConverter converter() {
        MongoMappingContext mappingContext = new MongoMappingContext();
        MappingMongoConverter converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, mappingContext);
        mappingContext.setSimpleTypeHolder(converter.getCustomConversions().getSimpleTypeHolder());
        mappingContext.afterPropertiesSet();
        converter.afterPropertiesSet();
        return converter;
    }
}
