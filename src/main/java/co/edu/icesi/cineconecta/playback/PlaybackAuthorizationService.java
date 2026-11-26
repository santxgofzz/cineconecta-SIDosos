package co.edu.icesi.cineconecta.playback;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.error.ErrorCodes;
import co.edu.icesi.cineconecta.integration.AccountDataGateway;
import co.edu.icesi.cineconecta.integration.CatalogEntry;
import co.edu.icesi.cineconecta.integration.CatalogGateway;
import co.edu.icesi.cineconecta.integration.ProfileAccess;
import co.edu.icesi.cineconecta.integration.ProfileGuard;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * RF-16: autoriza una reproducción consultando ambas bases, en este orden:
 * <ol>
 *   <li>Oracle: perfil propio y activo; suscripción vigente de la cuenta en [inicio, fin).</li>
 *   <li>MongoDB: contenido publicado, clasificación &lt;= máxima del perfil y, si es
 *       serie, episodio existente (las series se reproducen por episodio).</li>
 * </ol>
 * Si cualquiera de las bases no responde se rechaza: nunca se autoriza por omisión.
 */
@Service
public class PlaybackAuthorizationService {
    private final ProfileGuard profiles;
    private final AccountDataGateway oracle;
    private final CatalogGateway catalog;

    public PlaybackAuthorizationService(ProfileGuard profiles, AccountDataGateway oracle, CatalogGateway catalog) {
        this.profiles = profiles;
        this.oracle = oracle;
        this.catalog = catalog;
    }

    public PlaybackGrant authorize(long accountId, long profileId, String contentId, String episodeId, Instant at) {
        ProfileAccess profile = profiles.requireOwnedActiveProfile(accountId, profileId);

        long subscriptionId;
        try {
            subscriptionId = oracle.findActiveSubscriptionId(accountId, at)
                    .orElseThrow(() -> ApiException.forbidden(ErrorCodes.SUBSCRIPTION_REQUIRED,
                            "La cuenta no tiene una suscripción vigente."));
        } catch (DataAccessException ex) {
            throw ApiException.unavailable(ErrorCodes.ORACLE_UNAVAILABLE,
                    "No se pudo verificar la suscripción: la base de cuentas no está disponible.", ex);
        }

        CatalogEntry content;
        try {
            content = catalog.findById(contentId).orElse(null);
        } catch (DataAccessException ex) {
            throw ApiException.unavailable(ErrorCodes.MONGO_UNAVAILABLE,
                    "No se pudo verificar el contenido: el catálogo no está disponible.", ex);
        }
        if (content == null) {
            throw ApiException.notFound(ErrorCodes.CONTENT_NOT_FOUND, "El contenido no existe.");
        }
        if (!content.isPublished()) {
            throw ApiException.forbidden(ErrorCodes.CONTENT_NOT_AVAILABLE, "El contenido no está disponible.");
        }
        if (content.ageRatingOrMax() > profile.maxAgeRating()) {
            throw ApiException.forbidden(ErrorCodes.AGE_RATING_EXCEEDED,
                    "El contenido supera la clasificación permitida para el perfil.");
        }

        if (content.isSeries()) {
            if (episodeId == null || episodeId.isBlank()) {
                throw ApiException.badRequest(ErrorCodes.EPISODE_REQUIRED,
                        "Las series se reproducen por episodio: indica el episodio.");
            }
            CatalogEntry.Episode ep = content.findEpisode(episodeId)
                    .orElseThrow(() -> ApiException.notFound(ErrorCodes.EPISODE_NOT_FOUND,
                            "El episodio no pertenece a la serie."));
            return new PlaybackGrant(profile, subscriptionId, content, ep.id(), ep.id(), ep.title(),
                    requirePositive(ep.durationSeconds()));
        }
        if (episodeId != null && !episodeId.isBlank()) {
            throw ApiException.badRequest(ErrorCodes.EPISODE_NOT_APPLICABLE,
                    "Solo las series tienen episodios.");
        }
        return new PlaybackGrant(profile, subscriptionId, content, null, content.id(), content.title(),
                requirePositive(content.durationSeconds()));
    }

    private static int requirePositive(Integer duration) {
        if (duration == null || duration <= 0) {
            throw ApiException.conflict(ErrorCodes.CONTENT_NOT_AVAILABLE,
                    "El contenido no tiene una duración válida.");
        }
        return duration;
    }
}
