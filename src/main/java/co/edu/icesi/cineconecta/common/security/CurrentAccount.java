package co.edu.icesi.cineconecta.common.security;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.error.ErrorCodes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Obtiene la cuenta del titular desde el contexto de seguridad del servidor.
 * Nunca se acepta un accountId enviado por el cliente.
 */
@Component
public class CurrentAccount {
    public long requireAccountId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AccountPrincipal p) {
            return p.getAccountId();
        }
        throw ApiException.forbidden(ErrorCodes.ACCOUNT_REQUIRED,
                "Debes iniciar sesión como titular de una cuenta.");
    }
}
