package co.edu.icesi.cineconecta.playback;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.error.ErrorCodes;
import co.edu.icesi.cineconecta.common.resilience.TransientRetry;
import co.edu.icesi.cineconecta.integration.AccountDataGateway;
import co.edu.icesi.cineconecta.integration.CatalogGateway;
import co.edu.icesi.cineconecta.integration.ProfileGuard;
import co.edu.icesi.cineconecta.persistence.mongo.document.ViewingProgressDocument;
import co.edu.icesi.cineconecta.persistence.mongo.document.ViewingSessionDocument;
import co.edu.icesi.cineconecta.persistence.mongo.repository.ViewingProgressRepository;
import co.edu.icesi.cineconecta.persistence.mongo.repository.ViewingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static co.edu.icesi.cineconecta.playback.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** RF-21 Continuar viendo y RF-18 historial. */
class ActivityServiceTest {
    static final Instant T = Instant.parse("2026-11-20T10:00:00Z");

    AccountDataGateway oracle = mock(AccountDataGateway.class);
    CatalogGateway catalog = mock(CatalogGateway.class);
    ViewingProgressRepository progress = mock(ViewingProgressRepository.class);
    ViewingSessionRepository sessions = mock(ViewingSessionRepository.class);
    ActivityService activity = new ActivityService(new ProfileGuard(oracle), progress, sessions, catalog,
            new TransientRetry(3, 0));

    @BeforeEach
    void setUp() {
        when(oracle.findProfile(PROFILE)).thenReturn(Optional.of(profile()));
        when(oracle.findProfile(FOREIGN_PROFILE)).thenReturn(Optional.of(foreign()));
        when(catalog.findByIds(any())).thenReturn(Map.of(
                "CNT-001", movie(), "CNT-002", series(), "CNT-008", retiredMovie(), "CNT-018", adultMovie()));
    }

    private static ViewingProgressDocument pending(String contentId, String episodeId, int pos, int dur, Instant at) {
        return ViewingProgressDocument.of(PROFILE, contentId, episodeId, episodeId == null ? contentId : episodeId,
                pos, dur, false, at, "SES-" + contentId);
    }

    @Test
    @DisplayName("Continuar viendo: orden por actividad reciente, sin retirados ni fuera de clasificación")
    void continueWatchingFiltersRetiredAndKeepsOrder() {
        when(progress.findByProfileIdAndCompletedFalseOrderByLastActivityAtDesc(eq(PROFILE), any())).thenReturn(List.of(
                pending("CNT-002", "EP-002-02", 600, 2520, T.plusSeconds(300)),
                pending("CNT-008", null, 100, 3000, T.plusSeconds(200)),
                pending("CNT-001", null, 250, 1000, T.plusSeconds(100)),
                pending("CNT-018", null, 10, 6000, T)));

        var items = activity.continueWatching(ACCOUNT, PROFILE, 10);

        assertEquals(List.of("CNT-002", "CNT-001"), items.stream().map(ActivityService.ContinueItem::contentId).toList());
        assertEquals("El viaje", items.get(0).unitTitle());
        assertEquals(600, items.get(0).positionSeconds());
        assertEquals(25, items.get(1).percent());
    }

    @Test
    void continueWatchingOnlyQueriesPendingProgress() {
        when(progress.findByProfileIdAndCompletedFalseOrderByLastActivityAtDesc(eq(PROFILE), any())).thenReturn(List.of());
        activity.continueWatching(ACCOUNT, PROFILE, 5);
        verify(progress).findByProfileIdAndCompletedFalseOrderByLastActivityAtDesc(PROFILE, PageRequest.of(0, 15));
    }

    @Test
    void continueWatchingRespectsLimit() {
        when(progress.findByProfileIdAndCompletedFalseOrderByLastActivityAtDesc(eq(PROFILE), any())).thenReturn(List.of(
                pending("CNT-001", null, 1, 1000, T), pending("CNT-002", "EP-002-01", 1, 2400, T)));
        assertEquals(1, activity.continueWatching(ACCOUNT, PROFILE, 1).size());
    }

    @Test
    void historyKeepsSessionsOfRetiredContent() {
        ViewingSessionDocument s = new ViewingSessionDocument("S1", PROFILE, SUBSCRIPTION, "CNT-008", null,
                "CNT-008", "movie", T, 0, 3000);
        s.setWatchedSeconds(120);
        s.close(T.plusSeconds(130), "paused");
        when(sessions.findByProfileIdOrderByStartedAtDesc(eq(PROFILE), any()))
                .thenReturn(new PageImpl<>(List.of(s), PageRequest.of(0, 20), 1));

        var page = activity.history(ACCOUNT, PROFILE, 0, 20);

        assertEquals(1, page.totalItems());
        assertEquals("Retirada", page.items().get(0).title());
        assertFalse(page.items().get(0).contentAvailable());
        assertEquals(120, page.items().get(0).watchedSeconds());
    }

    @Test
    void foreignProfileCannotSeeActivity() {
        assertEquals(ErrorCodes.PROFILE_NOT_ALLOWED,
                assertThrows(ApiException.class, () -> activity.history(ACCOUNT, FOREIGN_PROFILE, 0, 20)).code());
        assertEquals(ErrorCodes.PROFILE_NOT_ALLOWED,
                assertThrows(ApiException.class, () -> activity.continueWatching(ACCOUNT, FOREIGN_PROFILE, 5)).code());
        verifyNoInteractions(progress, sessions);
    }

    @Test
    void mongoDownReturnsUnavailableAfterRetries() {
        when(progress.findByProfileIdAndCompletedFalseOrderByLastActivityAtDesc(eq(PROFILE), any()))
                .thenThrow(new DataAccessResourceFailureException("down"));

        ApiException ex = assertThrows(ApiException.class, () -> activity.continueWatching(ACCOUNT, PROFILE, 5));

        assertEquals(ErrorCodes.MONGO_UNAVAILABLE, ex.code());
        verify(progress, times(3)).findByProfileIdAndCompletedFalseOrderByLastActivityAtDesc(eq(PROFILE), any());
    }
}
