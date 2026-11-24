package co.edu.icesi.cineconecta.playback;

import co.edu.icesi.cineconecta.integration.CatalogEntry;
import co.edu.icesi.cineconecta.integration.ProfileAccess;

/**
 * Resultado de una autorización exitosa: todo lo necesario para crear la
 * sesión, ya validado contra Oracle y MongoDB.
 *
 * @param unitKey contentId para películas/documentales, episodeId para series.
 */
public record PlaybackGrant(ProfileAccess profile, long subscriptionId, CatalogEntry content,
                            String episodeId, String unitKey, String unitTitle, int durationSeconds) {}
