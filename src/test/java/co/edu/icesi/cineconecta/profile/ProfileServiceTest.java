package co.edu.icesi.cineconecta.profile;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.error.ErrorCodes;
import co.edu.icesi.cineconecta.persistence.oracle.entity.AccountEntity;
import co.edu.icesi.cineconecta.persistence.oracle.entity.AgeRatingEntity;
import co.edu.icesi.cineconecta.persistence.oracle.entity.PlanEntity;
import co.edu.icesi.cineconecta.persistence.oracle.entity.ProfileEntity;
import co.edu.icesi.cineconecta.persistence.oracle.entity.SubscriptionEntity;
import co.edu.icesi.cineconecta.persistence.oracle.repository.AccountRepository;
import co.edu.icesi.cineconecta.persistence.oracle.repository.AgeRatingRepository;
import co.edu.icesi.cineconecta.persistence.oracle.repository.PlanRepository;
import co.edu.icesi.cineconecta.persistence.oracle.repository.ProfileRepository;
import co.edu.icesi.cineconecta.persistence.oracle.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfileServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-15T12:00:00Z");
    private static final long ACCOUNT_ID = 1L;

    private final AccountRepository accounts = mock(AccountRepository.class);
    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
    private final PlanRepository plans = mock(PlanRepository.class);
    private final AgeRatingRepository ageRatings = mock(AgeRatingRepository.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final ProfileService service = new ProfileService(
            accounts, profiles, subscriptions, plans, ageRatings, clock);

    @BeforeEach
    void setupCatalogsAndAccount() {
        when(accounts.findByIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.of(
                new AccountEntity(ACCOUNT_ID, "Titular Demo", "titular@ejemplo.com", "hash", NOW)));
        when(ageRatings.findById(7)).thenReturn(Optional.of(new AgeRatingEntity(7, "Mayores de 7 anos")));
        when(ageRatings.findById(15)).thenReturn(Optional.of(new AgeRatingEntity(15, "Mayores de 15 anos")));
        when(plans.findById(2L)).thenReturn(Optional.of(
                new PlanEntity(2L, new BigDecimal("49900"), 3, "Familiar")));
    }

    @Test
    void createsProfileWhenSubscriptionIsActiveAndUnderPlanLimit() {
        when(subscriptions.findActiveSubscriptions(ACCOUNT_ID, NOW)).thenReturn(List.of(
                new SubscriptionEntity(8L, NOW.minusSeconds(3600), NOW.plusSeconds(3600), ACCOUNT_ID, 2L)));
        when(profiles.countByAccountIdAndIsActive(ACCOUNT_ID, ProfileEntity.ACTIVE)).thenReturn(2L);
        when(profiles.existsByAccountIdAndNameIgnoreCase(ACCOUNT_ID, "Invitado")).thenReturn(false);
        when(profiles.saveAndFlush(any(ProfileEntity.class))).thenAnswer(inv -> {
            ProfileEntity e = inv.getArgument(0);
            return new ProfileEntity(100L, e.getName(), e.getMaxAgeRating(), e.getIsActive(), e.getAccountId());
        });

        var created = service.createProfile(ACCOUNT_ID, new ProfileRequest("  Invitado ", 7));

        assertEquals(100L, created.profileId());
        assertEquals("Invitado", created.name());
        assertEquals(7, created.maxAgeRating());
        assertEquals("Mayores de 7 anos", created.ageRatingLabel());
        assertTrue(created.active());
        assertEquals("A", created.stateCode());
    }

    @Test
    void rejectsExpiredSubscriptionEvenAtExactExclusiveEndBoundary() {
        when(subscriptions.findActiveSubscriptions(ACCOUNT_ID, NOW)).thenReturn(List.of(
                new SubscriptionEntity(8L, NOW.minusSeconds(3600), NOW, ACCOUNT_ID, 2L)));

        var ex = assertThrows(ApiException.class,
                () -> service.createProfile(ACCOUNT_ID, new ProfileRequest("Nuevo", 7)));

        assertEquals(HttpStatus.FORBIDDEN, ex.status());
        assertEquals(ErrorCodes.SUBSCRIPTION_REQUIRED, ex.code());
    }

    @Test
    void rejectsCreationWhenPlanProfileCapacityIsFull() {
        when(subscriptions.findActiveSubscriptions(ACCOUNT_ID, NOW)).thenReturn(List.of(
                new SubscriptionEntity(8L, NOW.minusSeconds(3600), NOW.plusSeconds(3600), ACCOUNT_ID, 2L)));
        when(profiles.countByAccountIdAndIsActive(ACCOUNT_ID, ProfileEntity.ACTIVE)).thenReturn(3L);

        var ex = assertThrows(ApiException.class,
                () -> service.createProfile(ACCOUNT_ID, new ProfileRequest("Cuarto", 7)));

        assertEquals(HttpStatus.CONFLICT, ex.status());
        assertEquals(ErrorCodes.PROFILE_LIMIT_EXCEEDED, ex.code());
    }

    @Test
    void rejectsForeignOrMissingProfileAccess() {
        when(profiles.findByProfileIdAndAccountId(999L, ACCOUNT_ID)).thenReturn(Optional.empty());

        var exGet = assertThrows(ApiException.class, () -> service.getProfile(ACCOUNT_ID, 999L));
        assertEquals(HttpStatus.FORBIDDEN, exGet.status());
        assertEquals(ErrorCodes.PROFILE_NOT_ALLOWED, exGet.code());

        var exUpdate = assertThrows(ApiException.class,
                () -> service.updateProfile(ACCOUNT_ID, 999L, new ProfileRequest("Ajeno", 7)));
        assertEquals(HttpStatus.FORBIDDEN, exUpdate.status());
        assertEquals(ErrorCodes.PROFILE_NOT_ALLOWED, exUpdate.code());
    }

    @Test
    void rejectsInvalidAgeRatingAndDuplicateProfileName() {
        when(subscriptions.findActiveSubscriptions(ACCOUNT_ID, NOW)).thenReturn(List.of(
                new SubscriptionEntity(8L, NOW.minusSeconds(3600), NOW.plusSeconds(3600), ACCOUNT_ID, 2L)));
        when(profiles.countByAccountIdAndIsActive(ACCOUNT_ID, ProfileEntity.ACTIVE)).thenReturn(1L);
        when(ageRatings.findById(99)).thenReturn(Optional.empty());

        var exAge = assertThrows(ApiException.class,
                () -> service.createProfile(ACCOUNT_ID, new ProfileRequest("Kids", 99)));
        assertEquals(HttpStatus.BAD_REQUEST, exAge.status());
        assertEquals(ErrorCodes.INVALID_AGE_RATING, exAge.code());

        when(profiles.existsByAccountIdAndNameIgnoreCase(ACCOUNT_ID, "Ninos")).thenReturn(true);
        var exDup = assertThrows(ApiException.class,
                () -> service.createProfile(ACCOUNT_ID, new ProfileRequest("Ninos", 7)));
        assertEquals(HttpStatus.CONFLICT, exDup.status());
        assertEquals(ErrorCodes.PROFILE_NAME_CONFLICT, exDup.code());
    }

    @Test
    void deactivatesProfileAndBlocksReactivationWhenPlanLimitReached() {
        var existing = new ProfileEntity(16L, "Ninos", 7, ProfileEntity.ACTIVE, ACCOUNT_ID);
        when(profiles.findByProfileIdAndAccountId(16L, ACCOUNT_ID)).thenReturn(Optional.of(existing));
        when(profiles.saveAndFlush(existing)).thenReturn(existing);

        var deactivated = service.deactivateProfile(ACCOUNT_ID, 16L);
        assertFalse(deactivated.active());
        assertEquals("I", deactivated.stateCode());

        when(subscriptions.findActiveSubscriptions(ACCOUNT_ID, NOW)).thenReturn(List.of(
                new SubscriptionEntity(8L, NOW.minusSeconds(3600), NOW.plusSeconds(3600), ACCOUNT_ID, 2L)));
        when(profiles.countByAccountIdAndIsActive(ACCOUNT_ID, ProfileEntity.ACTIVE)).thenReturn(3L);

        var ex = assertThrows(ApiException.class, () -> service.reactivateProfile(ACCOUNT_ID, 16L));
        assertEquals(HttpStatus.CONFLICT, ex.status());
        assertEquals(ErrorCodes.PROFILE_LIMIT_EXCEEDED, ex.code());
    }
}
