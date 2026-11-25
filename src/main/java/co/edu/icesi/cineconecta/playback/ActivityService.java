package co.edu.icesi.cineconecta.playback;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.error.ErrorCodes;
import co.edu.icesi.cineconecta.common.resilience.TransientRetry;
import co.edu.icesi.cineconecta.integration.CatalogEntry;
import co.edu.icesi.cineconecta.integration.CatalogGateway;
import co.edu.icesi.cineconecta.integration.ProfileAccess;
import co.edu.icesi.cineconecta.integration.ProfileGuard;
import co.edu.icesi.cineconecta.persistence.mongo.document.ViewingProgressDocument;
import co.edu.icesi.cineconecta.persistence.mongo.document.ViewingSessionDocument;
import co.edu.icesi.cineconecta.persistence.mongo.repository.ViewingProgressRepository;
import co.edu.icesi.cineconecta.persistence.mongo.repository.ViewingSessionRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Continuar viendo (RF-21) e historial de sesiones (RF-18, RNF-07). */
@Service
public class ActivityService {
    static final int MAX_PAGE = 50;

    private final ProfileGuard profiles;
    private final ViewingProgressRepository progress;
    private final ViewingSessionRepository sessions;
    private final CatalogGateway catalog;
    private final TransientRetry retry;

    public ActivityService(ProfileGuard profiles, ViewingProgressRepository progress,
                           ViewingSessionRepository sessions, CatalogGateway catalog, TransientRetry retry) {
        this.profiles = profiles;
        this.progress = progress;
        this.sessions = sessions;
        this.catalog = catalog;
        this.retry = retry;
    }

    public record ContinueItem(String contentId, String episodeId, String unitKey, String title, String unitTitle,
                               String contentType, int positionSeconds, int durationSeconds, int percent,
                               Instant lastActivityAt) {}

    public record HistoryItem(String sessionId, String contentId, String episodeId, String title,
                              String contentType, Instant startedAt, Instant endedAt, int watchedSeconds,
                              int lastPositionSeconds, String endReason, boolean contentAvailable) {}

    public record HistoryPage(List<HistoryItem> items, int page, int size, long totalItems, int totalPages) {}

    /**
     * Pendientes ordenados por actividad más reciente. Excluye completados (en la
     * consulta) y contenidos no publicados o fuera de la clasificación del perfil.
     */
    public List<ContinueItem> continueWatching(long accountId, long profileId, int limit) {
        ProfileAccess profile = profiles.requireOwnedActiveProfile(accountId, profileId);
        int size = Math.max(1, Math.min(limit, MAX_PAGE));
        // Se lee de más para compensar los que se descartan por estar retirados.
        List<ViewingProgressDocument> pending = mongo(() -> retry.call(() ->
                progress.findByProfileIdAndCompletedFalseOrderByLastActivityAtDesc(profileId,
                        PageRequest.of(0, size * 3))));
        Map<String, CatalogEntry> contents = mongo(() -> retry.call(() ->
                catalog.findByIds(pending.stream().map(ViewingProgressDocument::getContentId).distinct().toList())));

        return pending.stream()
                .filter(p -> {
                    CatalogEntry c = contents.get(p.getContentId());
                    return c != null && c.isPublished() && c.ageRatingOrMax() <= profile.maxAgeRating();
                })
                .limit(size)
                .map(p -> {
                    CatalogEntry c = contents.get(p.getContentId());
                    String unitTitle = p.getEpisodeId() == null ? c.title()
                            : c.findEpisode(p.getEpisodeId()).map(CatalogEntry.Episode::title).orElse(p.getEpisodeId());
                    int pct = p.getDurationSeconds() == 0 ? 0
                            : (int) Math.min(100, 100L * p.getLastPositionSeconds() / p.getDurationSeconds());
                    return new ContinueItem(p.getContentId(), p.getEpisodeId(), p.getUnitKey(), c.title(), unitTitle,
                            c.type(), p.getLastPositionSeconds(), p.getDurationSeconds(), pct, p.getLastActivityAt());
                })
                .toList();
    }

    /** Historial completo, incluso de contenidos retirados (marcados como no disponibles). */
    public HistoryPage history(long accountId, long profileId, int page, int size) {
        profiles.requireOwnedActiveProfile(accountId, profileId);
        PageRequest req = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, MAX_PAGE)));
        Page<ViewingSessionDocument> result = mongo(() -> retry.call(() ->
                sessions.findByProfileIdOrderByStartedAtDesc(profileId, req)));
        Map<String, CatalogEntry> contents = mongo(() -> retry.call(() ->
                catalog.findByIds(result.getContent().stream().map(ViewingSessionDocument::getContentId)
                        .distinct().toList())));
        List<HistoryItem> items = result.getContent().stream().map(s -> {
            CatalogEntry c = contents.get(s.getContentId());
            return new HistoryItem(s.getId(), s.getContentId(), s.getEpisodeId(), c == null ? null : c.title(),
                    s.getContentTypeSnapshot(), s.getStartedAt(), s.getEndedAt(), s.getWatchedSeconds(),
                    s.getLastPositionSeconds(), s.getEndReason(), c != null && c.isPublished());
        }).toList();
        return new HistoryPage(items, req.getPageNumber(), req.getPageSize(), result.getTotalElements(),
                result.getTotalPages());
    }

    private static <T> T mongo(Supplier<T> op) {
        try {
            return op.get();
        } catch (DataAccessException ex) {
            throw ApiException.unavailable(ErrorCodes.MONGO_UNAVAILABLE,
                    "No se pudo consultar la actividad: la base de actividad no está disponible.", ex);
        }
    }
}
