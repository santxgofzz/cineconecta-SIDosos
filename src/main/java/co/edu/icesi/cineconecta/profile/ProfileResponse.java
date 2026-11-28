package co.edu.icesi.cineconecta.profile;

import co.edu.icesi.cineconecta.persistence.oracle.entity.ProfileEntity;

/** Representación pública de un perfil perteneciente a la cuenta autenticada. */
public record ProfileResponse(
        Long profileId,
        Long accountId,
        String name,
        Integer maxAgeRating,
        String ageRatingLabel,
        boolean active,
        String stateCode
) {
    public static ProfileResponse from(ProfileEntity entity, String ageRatingLabel) {
        return new ProfileResponse(
                entity.getProfileId(),
                entity.getAccountId(),
                entity.getName(),
                entity.getMaxAgeRating(),
                ageRatingLabel,
                entity.isActiveState(),
                entity.getIsActive()
        );
    }
}
