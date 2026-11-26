package co.edu.icesi.cineconecta.integration;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.error.ErrorCodes;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * Verifica en Oracle que el perfil exista, pertenezca a la cuenta autenticada
 * y esté activo. Un perfil inexistente y uno ajeno responden igual para no
 * revelar identificadores de otras cuentas.
 */
@Component
public class ProfileGuard {
    private final AccountDataGateway oracle;

    public ProfileGuard(AccountDataGateway oracle) {
        this.oracle = oracle;
    }

    public ProfileAccess requireOwnedActiveProfile(long accountId, long profileId) {
        ProfileAccess profile;
        try {
            profile = oracle.findProfile(profileId).orElse(null);
        } catch (DataAccessException ex) {
            throw ApiException.unavailable(ErrorCodes.ORACLE_UNAVAILABLE,
                    "No se pudo verificar el perfil: la base de cuentas no está disponible.", ex);
        }
        if (profile == null || profile.accountId() != accountId) {
            throw ApiException.forbidden(ErrorCodes.PROFILE_NOT_ALLOWED, "El perfil no pertenece a tu cuenta.");
        }
        if (!profile.active()) {
            throw ApiException.forbidden(ErrorCodes.PROFILE_INACTIVE, "El perfil está desactivado.");
        }
        return profile;
    }
}
