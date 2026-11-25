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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static co.edu.icesi.cineconecta.playback.Fixtures.*;
import static co.edu.icesi.cineconecta.playback.PlaybackEvent.Type.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RF-17 a RF-21 con repositorios Mongo simulados en memoria (mismo contrato:
 * actualización condicional por lastEventSeq y progreso único por unidad).
 */
class PlaybackServiceTest {
    static final Instant T0 = Instant.parse("2026-11-20T10:00:00Z");

    AccountDataGateway oracle = mock(AccountDataGateway.class);
    CatalogGateway catalog = mock(CatalogGateway.class);
    ViewingSessionRepository sessions = mock(ViewingSessionRepository.class);
    ViewingProgressRepository progressRepo = mock(ViewingProgressRepository.class);
    MutableClock clock = new MutableClock(T0);

    Map<String, ViewingSessionDocument> sessionStore = new HashMap<>();
    Map<String, ViewingProgressDocument> progressStore = new HashMap<>();
    PlaybackService service;

    @BeforeEach
    void setUp() {
        ProfileGuard guard = new ProfileGuard(oracle);
        TransientRetry retry = new TransientRetry(3, 0);
        service = new PlaybackService(new PlaybackAuthorizationService(guard, oracle, catalog), guard, sessions,
                new ProgressService(progressRepo, retry), retry, clock, 3);

        when(oracle.findProfile(PROFILE)).thenReturn(Optional.of(profile()));
        when(oracle.findProfile(FOREIGN_PROFILE)).thenReturn(Optional.of(foreign()));
        when(oracle.findActiveSubscriptionId(eq(ACCOUNT), any())).thenReturn(Optional.of(SUBSCRIPTION));
        when(catalog.findById("CNT-001")).thenReturn(Optional.of(movie()));
        when(catalog.findById("CNT-002")).thenReturn(Optional.of(series()));

        // --- viewing_sessions en memoria ---
        when(sessions.insert(any(ViewingSessionDocument.class))).thenAnswer(i -> {
            ViewingSessionDocument s = i.getArgument(0);
            if (sessionStore.putIfAbsent(s.getId(), s.copy()) != null) throw new DuplicateKeyException("E11000");
            return s;
        });
        when(sessions.findById(anyString())).thenAnswer(i ->
                Optional.ofNullable(sessionStore.get((String) i.getArgument(0))).map(ViewingSessionDocument::copy));
        when(sessions.compareAndUpdate(any(), anyInt())).thenAnswer(i -> casSession(i.getArgument(0), i.getArgument(1)));
        when(sessions.findByProfileIdAndUnitKeyAndEndedAtIsNull(anyLong(), anyString())).thenAnswer(i ->
                sessionStore.values().stream()
                        .filter(s -> s.getProfileId() == (long) i.getArgument(0)
                                && s.getUnitKey().equals(i.getArgument(1)) && s.isOpen())
                        .map(ViewingSessionDocument::copy).toList());

        // --- viewing_progress en memoria (único por profileId+unitKey, sin pisar algo más reciente) ---
        when(progressRepo.findByProfileIdAndUnitKey(anyLong(), anyString())).thenAnswer(i ->
                Optional.ofNullable(progressStore.get(i.getArgument(0) + "|" + i.getArgument(1))));
        doAnswer(i -> {
            ViewingProgressDocument p = i.getArgument(0);
            String key = p.getProfileId() + "|" + p.getUnitKey();
            ViewingProgressDocument old = progressStore.get(key);
            if (old != null && !old.getLastSessionId().equals(p.getLastSessionId())
                    && old.getLastActivityAt().isAfter(p.getLastActivityAt())) {
                throw new DuplicateKeyException("E11000 uq_progress_profile_unit");
            }
            progressStore.put(key, p);
            return null;
        }).when(progressRepo).upsert(any());
    }

    private boolean casSession(ViewingSessionDocument updated, int expectedSeq) {
        ViewingSessionDocument stored = sessionStore.get(updated.getId());
        if (stored == null || !stored.isOpen() || stored.getLastEventSeq() != expectedSeq) return false;
        sessionStore.put(updated.getId(), updated.copy());
        return true;
    }

    private PlaybackState startMovie() {
        return service.start(ACCOUNT, PROFILE, "CNT-001", null);
    }

    private PlaybackState event(PlaybackState s, int seq, PlaybackEvent.Type type, Integer seconds, Integer pos) {
        return service.apply(ACCOUNT, PROFILE, s.sessionId(), new PlaybackEvent(seq, type, seconds, pos));
    }

    private PlaybackState watch(PlaybackState s, int seq, int seconds) {
        clock.advance(Duration.ofSeconds(seconds));
        return event(s, seq, WATCH, seconds, null);
    }

    private ViewingProgressDocument progressOf(String unitKey) {
        return progressStore.get(PROFILE + "|" + unitKey);
    }

    @Nested
    class TiempoVistoYPosicion {
        @Test
        void startCreatesOpenSessionLinkedToSubscription() {
            PlaybackState s = startMovie();

            assertTrue(s.open());
            assertEquals(0, s.positionSeconds());
            ViewingSessionDocument doc = sessionStore.get(s.sessionId());
            assertEquals(SUBSCRIPTION, doc.getSubscriptionId());
            assertEquals("movie", doc.getContentTypeSnapshot());
        }

        @Test
        @DisplayName("Avanzar (salto) cambia la posición pero no suma tiempo visto")
        void seekDoesNotCountAsWatched() {
            PlaybackState s = startMovie();
            s = watch(s, 1, 60);
            s = event(s, 2, SEEK, null, 600);
            s = watch(s, 3, 30);

            assertEquals(630, s.positionSeconds());
            assertEquals(90, s.watchedSeconds());
        }

        @Test
        @DisplayName("El cliente no puede declarar más tiempo visto que el tiempo real transcurrido")
        void watchedTimeIsCappedByWallClock() {
            PlaybackState s = startMovie();
            clock.advance(Duration.ofSeconds(10));

            s = event(s, 1, WATCH, 500, null);

            assertEquals(13, s.watchedSeconds()); // 10 s reales + 3 s de tolerancia
            assertEquals(13, s.positionSeconds());
        }

        @Test
        void positionNeverExceedsDuration() {
            PlaybackState s = startMovie();
            s = event(s, 1, SEEK, null, 990);
            s = watch(s, 2, 60);

            assertEquals(1000, s.positionSeconds());
            assertEquals(10, s.watchedSeconds());
        }

        @Test
        void pauseClosesSessionAndKeepsPosition() {
            PlaybackState s = startMovie();
            s = watch(s, 1, 120);
            s = event(s, 2, PAUSE, null, null);

            assertFalse(s.open());
            assertEquals("paused", s.endReason());
            assertEquals(120, progressOf("CNT-001").getLastPositionSeconds());
            assertFalse(progressOf("CNT-001").isCompleted());
        }

        @Test
        void eventsAfterCloseAreRejected() {
            PlaybackState s = startMovie();
            event(s, 1, PAUSE, null, null);

            ApiException ex = assertThrows(ApiException.class, () -> watch(s, 2, 10));
            assertEquals(ErrorCodes.SESSION_CLOSED, ex.code());
        }

        @Test
        void invalidEventsAreRejected() {
            PlaybackState s = startMovie();
            assertEquals(ErrorCodes.INVALID_EVENT,
                    assertThrows(ApiException.class, () -> event(s, 1, WATCH, 0, null)).code());
            assertEquals(ErrorCodes.INVALID_EVENT,
                    assertThrows(ApiException.class, () -> event(s, 1, SEEK, null, -5)).code());
        }
    }

    @Nested
    class FinalizacionYProgreso {
        @Test
        @DisplayName("Al 89 % no está completado; al 90 % sí")
        void ninetyPercentRule() {
            PlaybackState s = startMovie();
            s = event(s, 1, SEEK, null, 890);
            assertFalse(s.completed());
            assertFalse(progressOf("CNT-001").isCompleted());

            s = watch(s, 2, 10); // 900 / 1000
            assertTrue(s.completed());
            assertTrue(progressOf("CNT-001").isCompleted());
            assertTrue(s.open(), "llegar al 90 % no cierra la sesión");
        }

        @Test
        void manualFinishCompletesEvenAtLowPosition() {
            PlaybackState s = startMovie();
            s = watch(s, 1, 60);
            s = event(s, 2, FINISH, null, null);

            assertTrue(s.completed());
            assertEquals("finished", s.endReason());
            assertTrue(progressOf("CNT-001").isCompleted());
        }

        @Test
        @DisplayName("Reanudar parte desde la última posición guardada en una sesión nueva")
        void resumeFromSavedPosition() {
            PlaybackState first = startMovie();
            first = watch(first, 1, 200);
            event(first, 2, PAUSE, null, null);

            PlaybackState second = startMovie();

            assertNotEquals(first.sessionId(), second.sessionId());
            assertEquals(200, second.positionSeconds());
            assertEquals(0, second.watchedSeconds());
        }

        @Test
        @DisplayName("Repetir la reproducción conserva cada sesión histórica")
        void historyIsCumulative() {
            PlaybackState first = startMovie();
            watch(first, 1, 100);
            event(first, 2, FINISH, null, null);

            PlaybackState second = startMovie();
            watch(second, 1, 50);

            assertEquals(2, sessionStore.size());
            ViewingSessionDocument old = sessionStore.get(first.sessionId());
            assertEquals(100, old.getWatchedSeconds());
            assertEquals("finished", old.getEndReason());
            assertEquals(0, second.positionSeconds(), "un completado se vuelve a ver desde 0");
            assertFalse(progressOf("CNT-001").isCompleted(), "el progreso actual se restablece");
            assertEquals(second.sessionId(), progressOf("CNT-001").getLastSessionId());
        }

        @Test
        void startingAgainInterruptsAbandonedSession() {
            PlaybackState abandoned = startMovie();
            watch(abandoned, 1, 30);

            startMovie();

            ViewingSessionDocument old = sessionStore.get(abandoned.sessionId());
            assertFalse(old.isOpen());
            assertEquals("interrupted", old.getEndReason());
            assertEquals(30, old.getWatchedSeconds());
        }

        @Test
        @DisplayName("Episodios: cada episodio tiene su propio progreso")
        void episodesHaveIndependentProgress() {
            PlaybackState ep1 = service.start(ACCOUNT, PROFILE, "CNT-002", "EP-002-01");
            ep1 = event(ep1, 1, SEEK, null, 2200);  // >= 90 % de 2400
            PlaybackState ep2 = service.start(ACCOUNT, PROFILE, "CNT-002", "EP-002-02");
            watch(ep2, 1, 40);

            assertTrue(progressOf("EP-002-01").isCompleted());
            assertFalse(progressOf("EP-002-02").isCompleted());
            assertEquals(40, progressOf("EP-002-02").getLastPositionSeconds());
            assertEquals("CNT-002", progressOf("EP-002-02").getContentId());
            assertEquals("series", sessionStore.get(ep2.sessionId()).getContentTypeSnapshot());
            assertEquals("EP-002-02", sessionStore.get(ep2.sessionId()).getEpisodeId());
        }
    }

    @Nested
    class AccesoAjeno {
        @Test
        @DisplayName("Usar la sesión de otro perfil responde como inexistente")
        void sessionOfAnotherProfileIsNotVisible() {
            PlaybackState mine = startMovie();
            when(oracle.findProfile(99L)).thenReturn(Optional.of(
                    new co.edu.icesi.cineconecta.integration.ProfileAccess(99L, ACCOUNT, 18, true)));

            ApiException ex = assertThrows(ApiException.class, () ->
                    service.apply(ACCOUNT, 99L, mine.sessionId(), new PlaybackEvent(1, WATCH, 5, null)));
            assertEquals(ErrorCodes.SESSION_NOT_FOUND, ex.code());
            assertEquals(0, sessionStore.get(mine.sessionId()).getWatchedSeconds());
        }

        @Test
        void profileOfAnotherAccountCannotSendEvents() {
            PlaybackState mine = startMovie();
            ApiException ex = assertThrows(ApiException.class, () ->
                    service.apply(ACCOUNT, FOREIGN_PROFILE, mine.sessionId(), new PlaybackEvent(1, WATCH, 5, null)));
            assertEquals(ErrorCodes.PROFILE_NOT_ALLOWED, ex.code());
        }

        @Test
        void foreignAccountCannotReadState() {
            PlaybackState mine = startMovie();
            ApiException ex = assertThrows(ApiException.class, () ->
                    service.state(OTHER_ACCOUNT, PROFILE, mine.sessionId()));
            assertEquals(ErrorCodes.PROFILE_NOT_ALLOWED, ex.code());
        }
    }

    @Nested
    class Reintentos {
        @Test
        @DisplayName("Reenviar el mismo evento (mismo seq) no suma dos veces")
        void duplicatedEventIsIgnored() {
            PlaybackState s = startMovie();
            s = watch(s, 1, 30);
            clock.advance(Duration.ofSeconds(30));
            PlaybackState again = event(s, 1, WATCH, 30, null);

            assertEquals(30, again.watchedSeconds());
            assertEquals(1, again.lastEventSeq());
        }

        @Test
        void retriedPauseOnClosedSessionIsIdempotent() {
            PlaybackState s = startMovie();
            event(s, 1, PAUSE, null, null);
            PlaybackState again = event(s, 1, PAUSE, null, null);
            assertEquals("paused", again.endReason());
        }

        @Test
        @DisplayName("Fallo transitorio de Mongo: se reintenta y el evento se aplica una sola vez")
        void transientFailureIsRetried() {
            PlaybackState s = startMovie();
            AtomicInteger calls = new AtomicInteger();
            doAnswer(i -> {
                if (calls.incrementAndGet() == 1) throw new DataAccessResourceFailureException("socket reset");
                return casSession(i.getArgument(0), i.getArgument(1));
            }).when(sessions).compareAndUpdate(any(), anyInt());

            s = watch(s, 1, 20);

            assertEquals(20, s.watchedSeconds());
            assertEquals(2, calls.get());
        }

        @Test
        @DisplayName("Escritura que llegó pero cuya respuesta se perdió: el reintento no duplica")
        void ambiguousWriteIsNotDoubleCounted() {
            PlaybackState s = startMovie();
            AtomicInteger calls = new AtomicInteger();
            doAnswer(i -> {
                boolean applied = casSession(i.getArgument(0), i.getArgument(1));
                if (calls.incrementAndGet() == 1) throw new DataAccessResourceFailureException("timeout after write");
                return applied;
            }).when(sessions).compareAndUpdate(any(), anyInt());

            s = watch(s, 1, 20);

            assertEquals(20, s.watchedSeconds());
            assertEquals(20, sessionStore.get(s.sessionId()).getWatchedSeconds());
        }

        @Test
        void lateRetryOfOldSessionDoesNotOverwriteNewerProgress() {
            PlaybackState first = startMovie();
            first = watch(first, 1, 300);
            event(first, 2, PAUSE, null, null);
            PlaybackState second = startMovie();
            clock.advance(Duration.ofMinutes(5));
            watch(second, 1, 100);

            event(first, 2, PAUSE, null, null); // reintento tardío del evento viejo

            assertEquals(second.sessionId(), progressOf("CNT-001").getLastSessionId());
            assertEquals(400, progressOf("CNT-001").getLastPositionSeconds());
        }
    }

    @Nested
    class FallosDeConexion {
        @Test
        @DisplayName("Mongo caído al iniciar: 503 y no se informa una sesión creada")
        void mongoDownOnStart() {
            when(sessions.insert(any(ViewingSessionDocument.class)))
                    .thenThrow(new DataAccessResourceFailureException("connection refused"));

            ApiException ex = assertThrows(ApiException.class, this::start);

            assertEquals(ErrorCodes.MONGO_UNAVAILABLE, ex.code());
            assertTrue(sessionStore.isEmpty());
            verify(sessions, times(3)).insert(any(ViewingSessionDocument.class));
        }

        private void start() {
            startMovie();
        }

        @Test
        @DisplayName("Mongo caído durante un evento: 503, el evento no se da por guardado")
        void mongoDownOnEvent() {
            PlaybackState s = startMovie();
            doThrow(new DataAccessResourceFailureException("connection refused"))
                    .when(sessions).compareAndUpdate(any(), anyInt());

            clock.advance(Duration.ofSeconds(30));
            ApiException ex = assertThrows(ApiException.class, () -> event(s, 1, WATCH, 30, null));

            assertEquals(ErrorCodes.MONGO_UNAVAILABLE, ex.code());
            assertEquals(0, sessionStore.get(s.sessionId()).getWatchedSeconds());
        }

        @Test
        @DisplayName("Si falla el progreso tras guardar la sesión, el reintento lo re-sincroniza")
        void progressFailureIsRecoveredByRetry() {
            PlaybackState s = startMovie();
            doThrow(new DataAccessResourceFailureException("down")).when(progressRepo).upsert(any());

            clock.advance(Duration.ofSeconds(40));
            assertThrows(ApiException.class, () -> event(s, 1, WATCH, 40, null));
            assertEquals(40, sessionStore.get(s.sessionId()).getWatchedSeconds(), "la sesión sí quedó guardada");

            doAnswer(i -> {
                ViewingProgressDocument p = i.getArgument(0);
                progressStore.put(p.getProfileId() + "|" + p.getUnitKey(), p);
                return null;
            }).when(progressRepo).upsert(any());
            PlaybackState again = event(s, 1, WATCH, 40, null);

            assertEquals(40, again.watchedSeconds(), "no se suma dos veces");
            assertEquals(40, progressOf("CNT-001").getLastPositionSeconds());
        }

        @Test
        void oracleDownOnEventIsRejected() {
            PlaybackState s = startMovie();
            when(oracle.findProfile(PROFILE))
                    .thenThrow(new org.springframework.jdbc.CannotGetJdbcConnectionException("ORA-12541"));

            ApiException ex = assertThrows(ApiException.class, () -> watch(s, 1, 10));
            assertEquals(ErrorCodes.ORACLE_UNAVAILABLE, ex.code());
            assertEquals(0, sessionStore.get(s.sessionId()).getWatchedSeconds());
        }

        @Test
        void sessionsAreNeverDeleted() {
            PlaybackState s = startMovie();
            watch(s, 1, 10);
            event(s, 2, FINISH, null, null);
            startMovie();
            verify(sessions, never()).delete(any());
            verify(sessions, never()).deleteById(any());
            verify(sessions, never()).deleteAll();
            assertEquals(2, List.copyOf(sessionStore.values()).size());
        }
    }
}
