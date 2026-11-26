package co.edu.icesi.cineconecta.profile;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.error.ErrorCodes;
import co.edu.icesi.cineconecta.persistence.oracle.entity.AgeRatingEntity;
import co.edu.icesi.cineconecta.persistence.oracle.entity.PlanEntity;
import co.edu.icesi.cineconecta.persistence.oracle.entity.ProfileEntity;
import co.edu.icesi.cineconecta.persistence.oracle.entity.SubscriptionEntity;
import co.edu.icesi.cineconecta.persistence.oracle.repository.AccountRepository;
import co.edu.icesi.cineconecta.persistence.oracle.repository.AgeRatingRepository;
import co.edu.icesi.cineconecta.persistence.oracle.repository.PlanRepository;
import co.edu.icesi.cineconecta.persistence.oracle.repository.ProfileRepository;
import co.edu.icesi.cineconecta.persistence.oracle.repository.SubscriptionRepository;
import co.edu.icesi.cineconecta.subscription.SubscriptionWindow;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestión de perfiles de una cuenta en Oracle. Serializa cambios críticos de
 * capacidad bloqueando la cuenta con {@code PESSIMISTIC_WRITE} (SELECT ... FOR UPDATE).
 */
@Service
public class ProfileService {
    public record AgeRatingOption(int ageValue, String label) {}

    private final AccountRepository accounts;
    private final ProfileRepository profiles;
    private final SubscriptionRepository subscriptions;
    private final PlanRepository plans;
    private final AgeRatingRepository ageRatings;
    private final Clock clock;

    public ProfileService(AccountRepository accounts,
                          ProfileRepository profiles,
                          SubscriptionRepository subscriptions,
                          PlanRepository plans,
                          AgeRatingRepository ageRatings,
                          Clock clock) {
        this.accounts = accounts;
        this.profiles = profiles;
        this.subscriptions = subscriptions;
        this.plans = plans;
        this.ageRatings = ageRatings;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AgeRatingOption> listAgeRatings() {
        return ageRatings.findAllByOrderByAgeValueAsc().stream()
                .map(r -> new AgeRatingOption(r.getAgeValue(), r.getLabel()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProfileResponse> listProfiles(long accountId) {
        Map<Integer, String> labels = ageRatings.findAllByOrderByAgeValueAsc().stream()
                .collect(Collectors.toMap(AgeRatingEntity::getAgeValue, AgeRatingEntity::getLabel));
        return profiles.findByAccountIdOrderByProfileIdAsc(accountId).stream()
                .map(p -> ProfileResponse.from(p, labels.getOrDefault(p.getMaxAgeRating(), String.valueOf(p.getMaxAgeRating()))))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(long accountId, long profileId) {
        ProfileEntity profile = requireOwnedProfile(accountId, profileId);
        return toResponse(profile);
    }

    @Transactional
    public ProfileResponse createProfile(long accountId, ProfileRequest request) {
        lockAccount(accountId);
        PlanEntity plan = requireActivePlan(accountId);
        ensureActiveCapacityAvailable(accountId, plan);

        AgeRatingEntity rating = requireValidAgeRating(request.maxAgeRating());
        String name = normalizeName(request.name());
        if (profiles.existsByAccountIdAndNameIgnoreCase(accountId, name)) {
            throw ApiException.conflict(ErrorCodes.PROFILE_NAME_CONFLICT,
                    "Ya existe un perfil con ese nombre en tu cuenta.");
        }

        ProfileEntity saved = saveProfileHandlingDuplicateName(
                new ProfileEntity(name, rating.getAgeValue(), accountId));
        return ProfileResponse.from(saved, rating.getLabel());
    }

    @Transactional
    public ProfileResponse updateProfile(long accountId, long profileId, ProfileRequest request) {
        ProfileEntity profile = requireOwnedProfile(accountId, profileId);
        AgeRatingEntity rating = requireValidAgeRating(request.maxAgeRating());
        String name = normalizeName(request.name());
        if (profiles.existsByAccountIdAndNameIgnoreCaseAndProfileIdNot(accountId, name, profileId)) {
            throw ApiException.conflict(ErrorCodes.PROFILE_NAME_CONFLICT,
                    "Ya existe otro perfil con ese nombre en tu cuenta.");
        }
        profile.updateDetails(name, rating.getAgeValue());
        ProfileEntity saved = saveProfileHandlingDuplicateName(profile);
        return ProfileResponse.from(saved, rating.getLabel());
    }

    @Transactional
    public ProfileResponse deactivateProfile(long accountId, long profileId) {
        lockAccount(accountId);
        ProfileEntity profile = requireOwnedProfile(accountId, profileId);
        profile.markInactive();
        ProfileEntity saved = profiles.saveAndFlush(profile);
        return toResponse(saved);
    }

    @Transactional
    public ProfileResponse reactivateProfile(long accountId, long profileId) {
        lockAccount(accountId);
        ProfileEntity profile = requireOwnedProfile(accountId, profileId);
        if (profile.isActiveState()) {
            return toResponse(profile);
        }
        PlanEntity plan = requireActivePlan(accountId);
        ensureActiveCapacityAvailable(accountId, plan);
        profile.markActive();
        ProfileEntity saved = profiles.saveAndFlush(profile);
        return toResponse(saved);
    }

    private void lockAccount(long accountId) {
        accounts.findByIdForUpdate(accountId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.ACCOUNT_NOT_FOUND, "Cuenta no encontrada."));
    }

    private ProfileEntity requireOwnedProfile(long accountId, long profileId) {
        return profiles.findByProfileIdAndAccountId(profileId, accountId)
                .orElseThrow(() -> ApiException.forbidden(ErrorCodes.PROFILE_NOT_ALLOWED,
                        "El perfil no pertenece a tu cuenta."));
    }

    private PlanEntity requireActivePlan(long accountId) {
        Instant now = Instant.now(clock);
        SubscriptionEntity active = subscriptions.findActiveSubscriptions(accountId, now).stream()
                .filter(s -> SubscriptionWindow.isActive(s.getStartsAt(), s.getEndsAt(), now))
                .findFirst()
                .orElseThrow(() -> ApiException.forbidden(ErrorCodes.SUBSCRIPTION_REQUIRED,
                        "La cuenta no tiene una suscripción vigente."));
        return plans.findById(active.getPlanId())
                .orElseThrow(() -> ApiException.forbidden(ErrorCodes.SUBSCRIPTION_REQUIRED,
                        "No se encontró el plan asociado a la suscripción vigente."));
    }

    private void ensureActiveCapacityAvailable(long accountId, PlanEntity plan) {
        long activeCount = profiles.countByAccountIdAndIsActive(accountId, ProfileEntity.ACTIVE);
        if (activeCount >= plan.getMaxProfiles()) {
            throw ApiException.conflict(ErrorCodes.PROFILE_LIMIT_EXCEEDED,
                    "Se alcanzó el máximo de perfiles activos permitidos por el plan.");
        }
    }

    private AgeRatingEntity requireValidAgeRating(Integer maxAgeRating) {
        return ageRatings.findById(maxAgeRating)
                .orElseThrow(() -> ApiException.badRequest(ErrorCodes.INVALID_AGE_RATING,
                        "Clasificación de edad no válida en DM_AGE_RATINGS."));
    }

    private static String normalizeName(String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw ApiException.badRequest(ErrorCodes.INVALID_INPUT, "El nombre del perfil es obligatorio.");
        }
        return trimmed;
    }

    private ProfileEntity saveProfileHandlingDuplicateName(ProfileEntity entity) {
        try {
            return profiles.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict(ErrorCodes.PROFILE_NAME_CONFLICT,
                    "Ya existe un perfil con ese nombre en tu cuenta.");
        }
    }

    private ProfileResponse toResponse(ProfileEntity entity) {
        String label = ageRatings.findById(entity.getMaxAgeRating())
                .map(AgeRatingEntity::getLabel)
                .orElseGet(() -> String.valueOf(entity.getMaxAgeRating()));
        return ProfileResponse.from(entity, label);
    }
}
