package co.edu.icesi.cineconecta.persistence.mongo;

import co.edu.icesi.cineconecta.config.DevAdminBootstrap;
import co.edu.icesi.cineconecta.integration.MongoCatalogGateway;
import co.edu.icesi.cineconecta.persistence.mongo.repository.AdminUserRepository;
import co.edu.icesi.cineconecta.persistence.mongo.repository.ContentRepository;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "CINE_CATALOG_TEST_URI",
        matches = "mongodb://(?:127\\.0\\.0\\.1|localhost):[0-9]+/cineconecta_test_[a-z0-9_]+\\?directConnection=true")
class CatalogMongoIntegrationTest {
    @Test
    void repositoriesAndPlaybackReadTheSameSeededEpisode() {
        String uri = System.getenv("CINE_CATALOG_TEST_URI");
        try (var client = MongoClients.create(uri)) {
            var mongo = new MongoTemplate(client, new ConnectionString(uri).getDatabase());
            var repository = new MongoRepositoryFactory(mongo).getRepository(ContentRepository.class);

            var season = repository.findById("CNT-002").orElseThrow().seasons().getFirst();
            var episode = season.episodes().getFirst();
            assertThat(episode.episodeId()).isEqualTo("EP-002-01");
            var projected = new MongoCatalogGateway(mongo).findById("CNT-002").orElseThrow()
                    .findEpisode("EP-002-01").orElseThrow();
            assertThat(projected.durationSeconds()).isEqualTo(episode.durationSeconds());
        }
    }

    @Test
    void bootstrapCreatesAdminOnceWithBcrypt() {
        String uri = System.getenv("CINE_CATALOG_TEST_URI");
        String username = "bootstrap." + UUID.randomUUID();
        String original = UUID.randomUUID().toString();
        String replacement = UUID.randomUUID().toString();
        try (var client = MongoClients.create(uri)) {
            var mongo = new MongoTemplate(client, new ConnectionString(uri).getDatabase());
            var repository = new MongoRepositoryFactory(mongo).getRepository(AdminUserRepository.class);
            var encoder = new BCryptPasswordEncoder();
            var environment = new MockEnvironment()
                    .withProperty("spring.data.mongodb.uri", uri)
                    .withProperty("CINE_ADMIN_USERNAME", username.toUpperCase(java.util.Locale.ROOT))
                    .withProperty("CINE_ADMIN_PASSWORD", original);
            var bootstrap = new DevAdminBootstrap(environment, mongo, encoder);
            var args = new DefaultApplicationArguments();
            bootstrap.run(args);
            var admin = repository.findByUsernameNormalized(username).orElseThrow();

            environment.setProperty("CINE_ADMIN_PASSWORD", replacement);
            bootstrap.run(args);

            assertThat(repository.findById(admin.id())).contains(admin);
            assertThat(admin.active()).isTrue();
            assertThat(encoder.matches(original, admin.passwordHash())).isTrue();
            assertThat(encoder.matches(replacement, admin.passwordHash())).isFalse();
        }
    }
}
