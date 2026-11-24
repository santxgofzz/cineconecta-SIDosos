package co.edu.icesi.cineconecta.integration;

import java.time.Instant;
import java.util.Optional;

/**
 * Lecturas de Oracle que necesitan Mi lista y la reproducción. Es una
 * interfaz para no acoplar estos módulos a las entidades JPA del equipo de
 * cuentas/suscripciones y para poder simular caídas en las pruebas.
 * Las implementaciones lanzan {@link org.springframework.dao.DataAccessException}
 * si Oracle no responde.
 */
public interface AccountDataGateway {
    Optional<ProfileAccess> findProfile(long profileId);

    /** Suscripción vigente de la cuenta en el instante dado: [startsAt, endsAt). */
    Optional<Long> findActiveSubscriptionId(long accountId, Instant at);
}
