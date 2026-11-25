package co.edu.icesi.cineconecta.catalog;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.integration.AccountDataGateway;
import co.edu.icesi.cineconecta.integration.ProfileAccess;
import co.edu.icesi.cineconecta.integration.ProfileGuard;
import co.edu.icesi.cineconecta.persistence.mongo.document.ContentDocument;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CatalogServiceTest {
    private static final Instant AT = Instant.parse("2026-01-31T12:00:00Z");
    private static final CatalogFilters FILTERS = new CatalogFilters(null, null, null, null, null, 0, 20);
    private final AccountDataGateway oracle = mock(AccountDataGateway.class);
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final CatalogService service = new CatalogService(new ProfileGuard(oracle), oracle, mongo,
            Clock.fixed(AT, ZoneOffset.UTC));

    @Test
    void blocksAnInactiveOrForeignProfileBeforeReadingMongo() {
        when(oracle.findProfile(1)).thenReturn(Optional.of(new ProfileAccess(1, 9, 18, true)));
        when(oracle.findProfile(2)).thenReturn(Optional.of(new ProfileAccess(2, 7, 12, false)));
        rejects(() -> service.search(7, 1, FILTERS), "PROFILE_NOT_ALLOWED");
        rejects(() -> service.detail(7, 2, "CNT-002"), "PROFILE_INACTIVE");
        verifyNoInteractions(mongo);
    }

    @Test
    void subscriptionAtItsExclusiveEndYieldsNoAccess() {
        ownedProfile();
        when(oracle.findActiveSubscriptionId(7, AT)).thenReturn(Optional.empty());
        rejects(() -> service.search(7, 1, FILTERS), "SUBSCRIPTION_REQUIRED");
        verifyNoInteractions(mongo);
    }

    @Test
    void translatesOutagesFromBothStores() {
        ownedProfile();
        when(oracle.findActiveSubscriptionId(7, AT)).thenThrow(new DataAccessResourceFailureException("Oracle"));
        rejects(() -> service.search(7, 1, FILTERS), "ORACLE_UNAVAILABLE");
        verifyNoInteractions(mongo);
        org.mockito.Mockito.doReturn(Optional.of(5L)).when(oracle).findActiveSubscriptionId(7, AT);
        when(mongo.count(any(Query.class), eq(ContentDocument.class)))
                .thenThrow(new DataAccessResourceFailureException("Mongo"));
        rejects(() -> service.search(7, 1, FILTERS), "MONGO_UNAVAILABLE");
    }

    @Test
    void validatesPagingAndRejectsUnknownTypes() {
        rejects(() -> new CatalogFilters(null, null, null, null, null, 0, 101), "INVALID_CONTENT");
        rejects(() -> new CatalogFilters(null, "tv", null, null, null, 0, 20), "INVALID_CONTENT");
    }

    private void ownedProfile() {
        when(oracle.findProfile(1)).thenReturn(Optional.of(new ProfileAccess(1, 7, 12, true)));
    }

    private static void rejects(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.code()).isEqualTo(code));
    }
}
