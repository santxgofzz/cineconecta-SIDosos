package co.edu.icesi.cineconecta.admin;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.persistence.mongo.repository.ContentRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AdminCatalogServiceTest {
    @Test
    void rejectsDuplicateSeasonNumbersBeforeWriting() {
        var season = new CatalogInput.Season(1, "Temporada", List.of(episode(null)));
        assertInvalid(input("series", null, null, List.of(season, season), null), "temporada");
    }

    @Test
    void rejectsDuplicateEpisodeNumbersInOneSeason() {
        var season = new CatalogInput.Season(1, "Temporada", List.of(episode(null), episode(null)));
        assertInvalid(input("series", null, null, List.of(season), null), "episodio");
    }

    @Test
    void rejectsClientSuppliedEpisodeIdentityOnCreation() {
        var season = new CatalogInput.Season(1, "Temporada", List.of(episode("EP-EXISTENTE")));
        assertInvalid(input("series", null, null, List.of(season), null), "ID de episodio");
    }

    @Test
    void rejectsIncompleteAndUnknownVariants() {
        assertInvalid(input("movie", null, List.of(), null, null), "tipo de contenido");
        assertInvalid(input("documentary", 100, null, null, List.of()), "tipo de contenido");
        assertInvalid(input("unknown", 100, null, null, null), "tipo de contenido");
    }

    private static void assertInvalid(CatalogInput input, String expectedMessage) {
        var repo = mock(ContentRepository.class);
        var mongo = mock(MongoTemplate.class);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var service = new AdminCatalogService(repo, mongo, factory.getValidator(),
                    Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
            assertThatThrownBy(() -> service.create(input)).isInstanceOf(ApiException.class)
                    .hasMessageContaining(expectedMessage);
            verifyNoInteractions(repo, mongo);
        }
    }

    private static CatalogInput.Episode episode(String id) {
        return new CatalogInput.Episode(id, 1, "Inicio", "Sinopsis", 120);
    }

    private static CatalogInput input(String type, Integer duration, List<CatalogInput.CastMember> cast,
                                      List<CatalogInput.Season> seasons, List<String> topics) {
        return new CatalogInput(type, "Título", "Sinopsis", 2020, List.of("Drama"), List.of("CO"),
                List.of("es"), List.of(), 12, "https://example.org/cover.jpg", duration,
                duration == null ? null : "Director", cast, seasons, topics, null, null);
    }
}
