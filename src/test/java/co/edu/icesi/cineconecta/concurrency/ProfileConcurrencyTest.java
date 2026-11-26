package co.edu.icesi.cineconecta.concurrency;

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
import co.edu.icesi.cineconecta.profile.ProfileRequest;
import co.edu.icesi.cineconecta.profile.ProfileResponse;
import co.edu.icesi.cineconecta.profile.ProfileService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifica que el bloqueo pesimista de la cuenta serializa altas concurrentes de
 * perfiles de modo que nunca se supere el máximo permitido por el plan.
 */
class ProfileConcurrencyTest {
    private static final Instant NOW = Instant.parse("2026-11-15T12:00:00Z");
    private static final long ACCOUNT_ID = 1L;

    @Test
    void concurrentProfileCreationsNeverExceedPlanMaxProfiles() throws Exception {
        var accountRowLock = new ReentrantLock();
        var activeProfiles = new ArrayList<ProfileEntity>();
        activeProfiles.add(new ProfileEntity(15L, "Principal", 18, ProfileEntity.ACTIVE, ACCOUNT_ID));
        var sequence = new AtomicLong(100L);

        AccountRepository accounts = mock(AccountRepository.class);
        ProfileRepository profiles = mock(ProfileRepository.class);
        SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
        PlanRepository plans = mock(PlanRepository.class);
        AgeRatingRepository ageRatings = mock(AgeRatingRepository.class);

        when(accounts.findByIdForUpdate(ACCOUNT_ID)).thenAnswer(inv -> {
            accountRowLock.lock();
            return Optional.of(new AccountEntity(ACCOUNT_ID, "Titular", "t@ejemplo.com", "hash", NOW));
        });
        when(subscriptions.findActiveSubscriptions(ACCOUNT_ID, NOW)).thenReturn(List.of(
                new SubscriptionEntity(8L, NOW.minusSeconds(3600), NOW.plusSeconds(3600), ACCOUNT_ID, 2L)));
        // Plan con máximo 2 perfiles y ya existe 1: solo cabe 1 más.
        when(plans.findById(2L)).thenReturn(Optional.of(
                new PlanEntity(2L, new BigDecimal("39900"), 2, "Duo")));
        when(ageRatings.findById(12)).thenReturn(Optional.of(new AgeRatingEntity(12, "Mayores de 12 anos")));
        when(profiles.countByAccountIdAndIsActive(ACCOUNT_ID, ProfileEntity.ACTIVE))
                .thenAnswer(inv -> (long) activeProfiles.size());
        when(profiles.existsByAccountIdAndNameIgnoreCase(any(), anyString())).thenAnswer(inv -> {
            String candidate = inv.getArgument(1);
            return activeProfiles.stream().anyMatch(p -> p.getName().equalsIgnoreCase(candidate));
        });
        when(profiles.saveAndFlush(any(ProfileEntity.class))).thenAnswer(inv -> {
            ProfileEntity e = inv.getArgument(0);
            ProfileEntity saved = new ProfileEntity(
                    sequence.getAndIncrement(), e.getName(), e.getMaxAgeRating(), e.getIsActive(), e.getAccountId());
            activeProfiles.add(saved);
            return saved;
        });

        var service = new ProfileService(
                accounts, profiles, subscriptions, plans, ageRatings, Clock.fixed(NOW, ZoneOffset.UTC));

        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Callable<String> task1 = createAttempt(service, ready, start, accountRowLock, "Perfil A");
            Callable<String> task2 = createAttempt(service, ready, start, accountRowLock, "Perfil B");
            Future<String> f1 = pool.submit(task1);
            Future<String> f2 = pool.submit(task2);

            ready.await(5, TimeUnit.SECONDS);
            start.countDown();

            List<String> outcomes = List.of(f1.get(5, TimeUnit.SECONDS), f2.get(5, TimeUnit.SECONDS));
            long createdCount = outcomes.stream().filter("CREATED"::equals).count();
            long limitExceededCount = outcomes.stream().filter(ErrorCodes.PROFILE_LIMIT_EXCEEDED::equals).count();

            assertEquals(1L, createdCount);
            assertEquals(1L, limitExceededCount);
            assertEquals(2, activeProfiles.size());
        } finally {
            pool.shutdownNow();
        }
    }

    private static Callable<String> createAttempt(ProfileService service,
                                                  CountDownLatch ready,
                                                  CountDownLatch start,
                                                  ReentrantLock accountRowLock,
                                                  String profileName) {
        return () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            try {
                ProfileResponse created = service.createProfile(ACCOUNT_ID, new ProfileRequest(profileName, 12));
                return created.profileId() != null ? "CREATED" : "UNEXPECTED";
            } catch (ApiException ex) {
                return ex.code();
            } finally {
                if (accountRowLock.isHeldByCurrentThread()) {
                    accountRowLock.unlock();
                }
            }
        };
    }
}
