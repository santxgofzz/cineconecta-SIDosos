package co.edu.icesi.cineconecta.watchlist;

import co.edu.icesi.cineconecta.integration.CatalogEntry;
import co.edu.icesi.cineconecta.persistence.mongo.document.MyListItemDocument;

import java.time.Instant;

/**
 * Elemento de Mi lista para la interfaz. {@code available=false} distingue
 * títulos retirados (o ya inexistentes) que se conservan en la lista.
 */
public record MyListEntryView(String contentId, String title, String type, Instant addedAt,
                              boolean available, String availability) {
    public static final String AVAILABLE = "AVAILABLE";
    public static final String RETIRED = "RETIRED";
    public static final String UNAVAILABLE = "UNAVAILABLE";

    static MyListEntryView of(MyListItemDocument item, CatalogEntry content) {
        if (content == null) {
            return new MyListEntryView(item.getContentId(), null, null, item.getAddedAt(), false, UNAVAILABLE);
        }
        String availability = content.isPublished() ? AVAILABLE : content.isRetired() ? RETIRED : UNAVAILABLE;
        return new MyListEntryView(item.getContentId(), content.title(), content.type(), item.getAddedAt(),
                content.isPublished(), availability);
    }
}
