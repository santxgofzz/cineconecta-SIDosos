package co.edu.icesi.cineconecta.admin;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.integration.MongoCatalogGateway;
import co.edu.icesi.cineconecta.persistence.mongo.repository.ContentRepository;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClients;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfEnvironmentVariable(named = "CINE_CATALOG_TEST_URI",
        matches = "mongodb://(?:127\\.0\\.0\\.1|localhost):[0-9]+/cineconecta_test_[a-z0-9_]+\\?directConnection=true")
class AdminCatalogMongoTest {
    @Test
    void lifecycleUsesTheSameContentAndEpisodeIdsFromDraftThroughRetirement() {
        String uri = System.getenv("CINE_CATALOG_TEST_URI");
        try (var client = MongoClients.create(uri); var validation = Validation.buildDefaultValidatorFactory()) {
            var mongo = new MongoTemplate(client, new ConnectionString(uri).getDatabase());
            var repository = new MongoRepositoryFactory(mongo).getRepository(ContentRepository.class);
            var service = new AdminCatalogService(repository, mongo, validation.getValidator(),
                    Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
            var playback = new MongoCatalogGateway(mongo);
            var episode = new CatalogInput.Episode(null, 1, "Inicio", "Sinopsis", 120);
            var season = new CatalogInput.Season(1, "Primera", List.of(episode));
            List<CatalogInput> entries = List.of(
                    input("movie", 120, List.of(), null, null),
                    input("documentary", 120, null, null, List.of("Historia")),
                    input("series", null, null, List.of(season), null));

            for (var entry : entries) {
                var created = service.create(entry);
                assertThat(created.id()).startsWith("CNT-");
                assertThat(created.status()).isEqualTo("draft");
                assertThat(service.get(created.id())).isEqualTo(created);
                var edited = service.update(created.id(), created.revision(), created.details());
                assertThat(edited.createdAt()).isEqualTo(created.createdAt());
                assertThat(edited.revision()).isGreaterThan(created.revision());
                assertThatThrownBy(() -> service.update(created.id(), created.revision(), created.details()))
                        .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.status()).isEqualTo(HttpStatus.CONFLICT));
                var published = service.publish(created.id());
                assertThat(service.publish(created.id())).isEqualTo(published);
                var projected = playback.findById(created.id()).orElseThrow();
                assertThat(projected.isPublished()).isTrue();
                if (projected.isSeries()) {
                    String episodeId = edited.details().seasons().getFirst().episodes().getFirst().id();
                    assertThat(projected.findEpisode(episodeId)).isPresent();
                    assertThatThrownBy(() -> service.update(created.id(), published.revision(), entry))
                            .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.status()).isEqualTo(HttpStatus.CONFLICT));
                    assertThat(service.get(created.id())).isEqualTo(published);
                } else {
                    assertThat(projected.durationSeconds()).isEqualTo(120);
                }
                var retired = service.retire(created.id());
                assertThat(service.retire(created.id())).isEqualTo(retired);
                assertThat(playback.findById(created.id()).orElseThrow().isRetired()).isTrue();
                assertThat(service.publish(created.id()).id()).isEqualTo(created.id());
            }
        }
    }

    private static CatalogInput input(String type, Integer duration, List<CatalogInput.CastMember> cast,
                                      List<CatalogInput.Season> seasons, List<String> topics) {
        return new CatalogInput(type, "Prueba " + type, "Sinopsis", 2020, List.of("Drama"), List.of("CO"),
                List.of("es"), List.of(), 12, "https://example.org/cover.jpg", duration,
                duration == null ? null : "Director", cast, seasons, topics, null, null);
    }
}
