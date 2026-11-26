package co.edu.icesi.cineconecta.playback;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.error.ErrorCodes;
import co.edu.icesi.cineconecta.integration.AccountDataGateway;
import co.edu.icesi.cineconecta.integration.CatalogGateway;
import co.edu.icesi.cineconecta.integration.ProfileGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

import java.time.Instant;
import java.util.Optional;

import static co.edu.icesi.cineconecta.playback.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** RF-16: autorización de reproducción consultando Oracle y MongoDB. */
class PlaybackAuthorizationServiceTest {
    static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");

    AccountDataGateway oracle = mock(AccountDataGateway.class);
    CatalogGateway catalog = mock(CatalogGateway.class);
    PlaybackAuthorizationService auth = new PlaybackAuthorizationService(new ProfileGuard(oracle), oracle, catalog);

    @BeforeEach
    void setUp() {
        when(oracle.findProfile(PROFILE)).thenReturn(Optional.of(profile()));
        when(oracle.findProfile(KIDS_PROFILE)).thenReturn(Optional.of(kids()));
        when(oracle.findProfile(INACTIVE_PROFILE)).thenReturn(Optional.of(inactive()));
        when(oracle.findProfile(FOREIGN_PROFILE)).thenReturn(Optional.of(foreign()));
        when(oracle.findActiveSubscriptionId(eq(ACCOUNT), any())).thenReturn(Optional.of(SUBSCRIPTION));
        for (var c : new co.edu.icesi.cineconecta.integration.CatalogEntry[]{
                movie(), adultMovie(), retiredMovie(), draftDoc(), series()}) {
            when(catalog.findById(c.id())).thenReturn(Optional.of(c));
        }
    }

    private ApiException rejected(long profileId, String contentId, String episodeId) {
        return assertThrows(ApiException.class, () -> auth.authorize(ACCOUNT, profileId, contentId, episodeId, NOW));
    }

    @Test
    void authorizesMovieWithContentIdAsUnitKey() {
        PlaybackGrant g = auth.authorize(ACCOUNT, PROFILE, "CNT-001", null, NOW);
        assertEquals("CNT-001", g.unitKey());
        assertEquals(SUBSCRIPTION, g.subscriptionId());
        assertEquals(1000, g.durationSeconds());
    }

    // ---- Clasificación por edad ----

    @Test
    @DisplayName("Clasificación: contenido 18+ rechazado para perfil de máx. 13")
    void ageRatingAboveProfileMaxIsRejected() {
        assertEquals(ErrorCodes.AGE_RATING_EXCEEDED, rejected(PROFILE, "CNT-018", null).code());
    }

    @Test
    @DisplayName("Clasificación: serie de 7 permitida exactamente en el límite del perfil infantil")
    void ageRatingEqualToMaxIsAllowed() {
        assertDoesNotThrow(() -> auth.authorize(ACCOUNT, KIDS_PROFILE, "CNT-002", "EP-002-01", NOW));
    }

    @Test
    void kidsProfileCannotPlay12PlusMovie() {
        assertEquals(ErrorCodes.AGE_RATING_EXCEEDED, rejected(KIDS_PROFILE, "CNT-001", null).code());
    }

    // ---- Suscripción vencida ----

    @Test
    @DisplayName("Suscripción vencida: sin suscripción vigente se rechaza y no se consulta el catálogo")
    void expiredSubscriptionIsRejected() {
        when(oracle.findActiveSubscriptionId(eq(ACCOUNT), any())).thenReturn(Optional.empty());

        ApiException ex = rejected(PROFILE, "CNT-001", null);

        assertEquals(ErrorCodes.SUBSCRIPTION_REQUIRED, ex.code());
        assertEquals(HttpStatus.FORBIDDEN, ex.status());
        verifyNoInteractions(catalog);
    }

    @Test
    void subscriptionIsCheckedAtTheRequestInstant() {
        auth.authorize(ACCOUNT, PROFILE, "CNT-001", null, NOW);
        verify(oracle).findActiveSubscriptionId(ACCOUNT, NOW);
    }

    // ---- Acceso ajeno ----

    @Test
    @DisplayName("Acceso ajeno: perfil de otra cuenta rechazado aunque se cambie el id en la URL")
    void foreignProfileIsRejected() {
        ApiException ex = rejected(FOREIGN_PROFILE, "CNT-001", null);
        assertEquals(ErrorCodes.PROFILE_NOT_ALLOWED, ex.code());
        verify(oracle, never()).findActiveSubscriptionId(anyLong(), any());
    }

    @Test
    void unknownProfileLooksTheSameAsForeign() {
        when(oracle.findProfile(999L)).thenReturn(Optional.empty());
        assertEquals(ErrorCodes.PROFILE_NOT_ALLOWED, rejected(999L, "CNT-001", null).code());
    }

    @Test
    void inactiveProfileIsRejected() {
        assertEquals(ErrorCodes.PROFILE_INACTIVE, rejected(INACTIVE_PROFILE, "CNT-001", null).code());
    }

    // ---- Disponibilidad del contenido ----

    @Test
    void retiredContentCannotBePlayed() {
        assertEquals(ErrorCodes.CONTENT_NOT_AVAILABLE, rejected(PROFILE, "CNT-008", null).code());
    }

    @Test
    void draftContentCannotBePlayed() {
        assertEquals(ErrorCodes.CONTENT_NOT_AVAILABLE, rejected(PROFILE, "CNT-007", null).code());
    }

    @Test
    void unknownContentIsNotFound() {
        when(catalog.findById("NOPE")).thenReturn(Optional.empty());
        assertEquals(ErrorCodes.CONTENT_NOT_FOUND, rejected(PROFILE, "NOPE", null).code());
    }

    // ---- Episodios ----

    @Test
    @DisplayName("Episodios: la serie se reproduce por episodio y su id es la unitKey")
    void episodeIsTheUnitOfASeries() {
        PlaybackGrant g = auth.authorize(ACCOUNT, PROFILE, "CNT-002", "EP-002-03", NOW);
        assertEquals("EP-002-03", g.unitKey());
        assertEquals("EP-002-03", g.episodeId());
        assertEquals(2280, g.durationSeconds());
        assertEquals("El regreso", g.unitTitle());
    }

    @Test
    void seriesWithoutEpisodeIsRejected() {
        assertEquals(ErrorCodes.EPISODE_REQUIRED, rejected(PROFILE, "CNT-002", null).code());
    }

    @Test
    void episodeFromAnotherSeriesIsRejected() {
        assertEquals(ErrorCodes.EPISODE_NOT_FOUND, rejected(PROFILE, "CNT-002", "EP-999-01").code());
    }

    @Test
    void episodeOnAMovieIsRejected() {
        assertEquals(ErrorCodes.EPISODE_NOT_APPLICABLE, rejected(PROFILE, "CNT-001", "EP-002-01").code());
    }

    // ---- Fallos de conexión ----

    @Test
    @DisplayName("Caída de Oracle al validar perfil: 503 y nunca se autoriza")
    void oracleDownWhileCheckingProfile() {
        when(oracle.findProfile(PROFILE)).thenThrow(new CannotGetJdbcConnectionException("ORA-12541"));

        ApiException ex = rejected(PROFILE, "CNT-001", null);

        assertEquals(ErrorCodes.ORACLE_UNAVAILABLE, ex.code());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.status());
        verifyNoInteractions(catalog);
    }

    @Test
    void oracleDownWhileCheckingSubscription() {
        when(oracle.findActiveSubscriptionId(eq(ACCOUNT), any()))
                .thenThrow(new CannotGetJdbcConnectionException("ORA-12541"));
        assertEquals(ErrorCodes.ORACLE_UNAVAILABLE, rejected(PROFILE, "CNT-001", null).code());
        verifyNoInteractions(catalog);
    }

    @Test
    @DisplayName("Caída de MongoDB al validar contenido: 503 y nunca se autoriza")
    void mongoDownWhileCheckingContent() {
        when(catalog.findById("CNT-001")).thenThrow(new DataAccessResourceFailureException("Timed out"));
        ApiException ex = rejected(PROFILE, "CNT-001", null);
        assertEquals(ErrorCodes.MONGO_UNAVAILABLE, ex.code());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.status());
    }
}
