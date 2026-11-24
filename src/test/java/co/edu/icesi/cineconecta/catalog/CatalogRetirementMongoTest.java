package co.edu.icesi.cineconecta.catalog;

import co.edu.icesi.cineconecta.admin.AdminCatalogService;
import co.edu.icesi.cineconecta.admin.CatalogInput;
import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.resilience.TransientRetry;
import co.edu.icesi.cineconecta.config.MongoActivityIndexes;
import co.edu.icesi.cineconecta.integration.AccountDataGateway;
import co.edu.icesi.cineconecta.integration.MongoCatalogGateway;
import co.edu.icesi.cineconecta.integration.ProfileAccess;
import co.edu.icesi.cineconecta.integration.ProfileGuard;
import co.edu.icesi.cineconecta.persistence.mongo.document.ViewingProgressDocument;
import co.edu.icesi.cineconecta.persistence.mongo.document.ViewingSessionDocument;
import co.edu.icesi.cineconecta.persistence.mongo.repository.ContentRepository;
import co.edu.icesi.cineconecta.persistence.mongo.repository.MyListRepository;
import co.edu.icesi.cineconecta.playback.PlaybackAuthorizationService;
import co.edu.icesi.cineconecta.watchlist.MyListService;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Filters;
import jakarta.validation.Validation;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnabledIfEnvironmentVariable(named = "CINE_CATALOG_TEST_URI",
        matches = "mongodb://(?:127\\.0\\.0\\.1|localhost):[0-9]+/cineconecta_test_[a-z0-9_]+\\?directConnection=true")
class CatalogRetirementMongoTest {
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private static final List<String> ACTIVITY = List.of("my_list", "viewing_sessions", "viewing_progress");

    @Test
    void retirementPreservesAllThreeActivityDocumentsAndDeniesNewPlayback() {
        String uri = System.getenv("CINE_CATALOG_TEST_URI");
        try (var client = MongoClients.create(uri); var validation = Validation.buildDefaultValidatorFactory()) {
            var mongo = new MongoTemplate(client, new ConnectionString(uri).getDatabase());
            new MongoActivityIndexes(mongo).run(new DefaultApplicationArguments());
            var repositories = new MongoRepositoryFactory(mongo);
            var clock = Clock.fixed(NOW, ZoneOffset.UTC);
            var admin = new AdminCatalogService(repositories.getRepository(ContentRepository.class), mongo,
                    validation.getValidator(), clock);
            var oracle = mock(AccountDataGateway.class);
            when(oracle.findProfile(41)).thenReturn(Optional.of(new ProfileAccess(41, 7, 12, true)));
            when(oracle.findActiveSubscriptionId(7, NOW)).thenReturn(Optional.of(23L));
            var profileGuard = new ProfileGuard(oracle);
            var gateway = new MongoCatalogGateway(mongo);
            var watchlist = new MyListService(repositories.getRepository(MyListRepository.class), gateway,
                    profileGuard, new TransientRetry(1, 0), clock);
            var playback = new PlaybackAuthorizationService(profileGuard, oracle, gateway);

            var content = admin.publish(admin.create(series()).id());
            String id = content.id();
            String episode = content.details().seasons().getFirst().episodes().getFirst().id();
            var grant = playback.authorize(7, 41, id, episode, NOW);
            assertThat(grant.durationSeconds()).isEqualTo(120);
            assertThat(watchlist.add(7, 41, id).created()).isTrue();
            assertThat(watchlist.add(7, 41, id).created()).isFalse();

            String sessionId = UUID.randomUUID().toString();
            var session = new ViewingSessionDocument(sessionId, 41L, 23L, id, episode, episode,
                    "series", NOW.minusSeconds(35), 30, 120);
            session.setWatchedSeconds(30);
            session.close(NOW, "paused");
            mongo.insert(session);
            mongo.insert(ViewingProgressDocument.of(41L, id, episode, episode, 30, 120, false, NOW, sessionId));

            var before = snapshots(mongo, id);
            assertThat(before).allSatisfy(rows -> assertThat(rows).hasSize(1));
            admin.retire(id);

            assertThat(snapshots(mongo, id)).isEqualTo(before);
            assertThat(watchlist.list(7, 41).stream().filter(row -> row.contentId().equals(id)))
                    .singleElement().satisfies(row -> {
                        assertThat(row.available()).isFalse();
                        assertThat(row.availability()).isEqualTo("RETIRED");
                    });
            assertThatThrownBy(() -> playback.authorize(7, 41, id, episode, NOW))
                    .isInstanceOfSatisfying(ApiException.class,
                            error -> assertThat(error.code()).isEqualTo("CONTENT_NOT_AVAILABLE"));
        }
    }

    private static List<List<Document>> snapshots(MongoTemplate mongo, String contentId) {
        return ACTIVITY.stream().map(collection -> mongo.getCollection(collection)
                .find(Filters.eq("contentId", contentId)).into(new ArrayList<Document>())).map(List::copyOf).toList();
    }

    private static CatalogInput series() {
        var episode = new CatalogInput.Episode(null, 1, "Inicio", "Sinopsis", 120);
        var season = new CatalogInput.Season(1, "Primera", List.of(episode));
        return new CatalogInput("series", "Ensayo de retiro", "Sinopsis", 2025, List.of("Pruebas"), List.of("CO"),
                List.of("es"), List.of(), 12, "https://example.org/covers/fixture", null, null,
                null, List.of(season), null, null, null);
    }
}
