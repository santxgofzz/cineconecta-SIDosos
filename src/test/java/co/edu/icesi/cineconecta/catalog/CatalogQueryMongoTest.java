package co.edu.icesi.cineconecta.catalog;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.integration.AccountDataGateway;
import co.edu.icesi.cineconecta.integration.ProfileAccess;
import co.edu.icesi.cineconecta.integration.ProfileGuard;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnabledIfEnvironmentVariable(named = "CINE_CATALOG_TEST_URI",
        matches = "mongodb://(?:127\\.0\\.0\\.1|localhost):[0-9]+/cineconecta_test_[a-z0-9_]+\\?directConnection=true")
class CatalogQueryMongoTest {
    @Test
    void filtersPagingAndDetailsUseTheSameProfileVisibility() {
        String uri = System.getenv("CINE_CATALOG_TEST_URI");
        var clock = Clock.fixed(Instant.parse("2026-02-01T12:00:00Z"), ZoneOffset.UTC);
        var oracle = mock(AccountDataGateway.class);
        when(oracle.findProfile(31)).thenReturn(Optional.of(new ProfileAccess(31, 7, 12, true)));
        when(oracle.findActiveSubscriptionId(7, clock.instant())).thenReturn(Optional.of(17L));
        try (var client = MongoClients.create(uri)) {
            var mongo = new MongoTemplate(client, new ConnectionString(uri).getDatabase());
            var catalog = new CatalogService(new ProfileGuard(oracle), oracle, mongo, clock);

            var combined = catalog.search(7, 31,
                    new CatalogFilters("RUTA", "movie", "Drama", "en", 2022, 0, 20));
            assertThat(combined.totalElements()).isEqualTo(1);
            assertThat(combined.items()).extracting(CatalogService.Item::id).containsExactly("CNT-001");
            assertThat(catalog.search(7, 31, new CatalogFilters(".*", null, null, null, null, 0, 20)).items())
                    .isEmpty();

            var first = catalog.search(7, 31, new CatalogFilters(" ", "documentary", "Ciencia", "es", null, 0, 1));
            var second = catalog.search(7, 31, new CatalogFilters(null, "documentary", "Ciencia", "es", null, 1, 1));
            assertThat(first.totalElements()).isEqualTo(2);
            assertThat(second.totalElements()).isEqualTo(first.totalElements());
            assertThat(first.totalPages()).isEqualTo(2);
            assertThat(first.items()).extracting(CatalogService.Item::id).containsExactly("CNT-007");
            assertThat(second.items()).extracting(CatalogService.Item::id).containsExactly("CNT-003");

            var all = catalog.search(7, 31, new CatalogFilters(null, null, null, null, null, 0, 100));
            assertThat(all.items()).extracting(CatalogService.Item::id)
                    .contains("CNT-001", "CNT-002").doesNotContain("CNT-004", "CNT-005", "CNT-006", "CNT-008");
            for (String hidden : new String[]{"CNT-004", "CNT-005", "CNT-006", "CNT-008", "missing"}) {
                assertThatThrownBy(() -> catalog.detail(7, 31, hidden)).isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.status()).isEqualTo(HttpStatus.NOT_FOUND));
            }
            var detail = catalog.detail(7, 31, "CNT-002");
            assertThat(detail.seasons()).extracting(CatalogService.Season::number).containsExactly(1, 2);
            assertThat(detail.seasons().getFirst().episodes()).extracting(CatalogService.Episode::id)
                    .containsExactly("EP-002-01", "EP-002-02");
            assertThat(detail.seasons().getFirst().episodes().getFirst().synopsis()).isNotBlank();
        }
    }
}
