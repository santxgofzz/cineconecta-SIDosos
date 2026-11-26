package co.edu.icesi.cineconecta.watchlist;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.error.ErrorCodes;
import co.edu.icesi.cineconecta.common.resilience.TransientRetry;
import co.edu.icesi.cineconecta.integration.CatalogEntry;
import co.edu.icesi.cineconecta.integration.CatalogGateway;
import co.edu.icesi.cineconecta.integration.ProfileAccess;
import co.edu.icesi.cineconecta.integration.ProfileGuard;
import co.edu.icesi.cineconecta.persistence.mongo.document.MyListItemDocument;
import co.edu.icesi.cineconecta.persistence.mongo.repository.MyListRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Mi lista (RF-15). Reglas:
 * <ul>
 *   <li>El perfil debe pertenecer a la cuenta autenticada y estar activo (Oracle).</li>
 *   <li>Solo se agregan contenidos publicados y permitidos para la edad del perfil.</li>
 *   <li>Agregar dos veces es idempotente: el índice único (profileId, contentId)
 *       resuelve incluso solicitudes simultáneas o reintentos.</li>
 *   <li>Retirar es idempotente y funciona aunque el contenido ya esté retirado.</li>
 *   <li>Al listar, los títulos retirados o inexistentes se marcan como no disponibles.</li>
 * </ul>
 */
@Service
public class MyListService {
    private final MyListRepository repository;
    private final CatalogGateway catalog;
    private final ProfileGuard profiles;
    private final TransientRetry retry;
    private final Clock clock;

    public MyListService(MyListRepository repository, CatalogGateway catalog, ProfileGuard profiles,
                         TransientRetry retry, Clock clock) {
        this.repository = repository;
        this.catalog = catalog;
        this.profiles = profiles;
        this.retry = retry;
        this.clock = clock;
    }

    public record AddResult(MyListEntryView entry, boolean created) {}

    public AddResult add(long accountId, long profileId, String contentId) {
        ProfileAccess profile = profiles.requireOwnedActiveProfile(accountId, profileId);
        CatalogEntry content = mongo(() -> catalog.findById(contentId))
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.CONTENT_NOT_FOUND, "El contenido no existe."));
        if (!content.isPublished()) {
            throw ApiException.conflict(ErrorCodes.CONTENT_NOT_AVAILABLE,
                    "El contenido no está disponible y no puede agregarse a Mi lista.");
        }
        if (content.ageRatingOrMax() > profile.maxAgeRating()) {
            throw ApiException.forbidden(ErrorCodes.AGE_RATING_EXCEEDED,
                    "El contenido supera la clasificación permitida para el perfil.");
        }

        var existing = mongo(() -> repository.findByProfileIdAndContentId(profileId, contentId));
        if (existing.isPresent()) {
            return new AddResult(MyListEntryView.of(existing.get(), content), false);
        }
        Instant now = clock.instant();
        try {
            MyListItemDocument saved = mongo(() -> retry.call(
                    () -> repository.insert(new MyListItemDocument(profileId, contentId, now))));
            return new AddResult(MyListEntryView.of(saved, content), true);
        } catch (DuplicateKeyException dup) {
            // Otra solicitud (o un reintento cuya primera escritura sí llegó) ya lo guardó.
            MyListItemDocument stored = mongo(() -> repository.findByProfileIdAndContentId(profileId, contentId))
                    .orElseThrow(() -> dup);
            return new AddResult(MyListEntryView.of(stored, content), false);
        }
    }

    /** @return true si se eliminó, false si no estaba (idempotente). */
    public boolean remove(long accountId, long profileId, String contentId) {
        profiles.requireOwnedActiveProfile(accountId, profileId);
        long deleted = mongo(() -> retry.call(() -> repository.deleteByProfileIdAndContentId(profileId, contentId)));
        return deleted > 0;
    }

    public List<MyListEntryView> list(long accountId, long profileId) {
        profiles.requireOwnedActiveProfile(accountId, profileId);
        List<MyListItemDocument> items = mongo(() -> retry.call(
                () -> repository.findByProfileIdOrderByAddedAtDesc(profileId)));
        Map<String, CatalogEntry> contents = mongo(() -> retry.call(
                () -> catalog.findByIds(items.stream().map(MyListItemDocument::getContentId).toList())));
        return items.stream().map(i -> MyListEntryView.of(i, contents.get(i.getContentId()))).toList();
    }

    private static <T> T mongo(java.util.function.Supplier<T> op) {
        try {
            return op.get();
        } catch (DuplicateKeyException dup) {
            throw dup;
        } catch (DataAccessException ex) {
            throw ApiException.unavailable(ErrorCodes.MONGO_UNAVAILABLE,
                    "No se pudo confirmar la operación en Mi lista: la base de catálogo no está disponible.", ex);
        }
    }
}
