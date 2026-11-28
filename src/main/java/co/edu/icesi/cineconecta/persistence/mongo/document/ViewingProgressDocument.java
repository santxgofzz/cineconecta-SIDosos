package co.edu.icesi.cineconecta.persistence.mongo.document;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Estado actual por (perfil, unidad reproducible). unitKey = contentId para
 * películas/documentales y episodeId para series. Índice único
 * {@code uq_progress_profile_unit}. Es un dato derivado de las sesiones: se
 * puede recalcular desde la última sesión si una escritura falla.
 */
@Document(collection = ViewingProgressDocument.COLLECTION)
public class ViewingProgressDocument {
    public static final String COLLECTION = "viewing_progress";

    @Id
    private ObjectId id;
    private Long profileId;
    private String contentId;
    private String episodeId;
    private String unitKey;
    private Integer lastPositionSeconds;
    private Boolean completed;
    private Instant lastActivityAt;
    private String lastSessionId;

    protected ViewingProgressDocument() {}

    public static ViewingProgressDocument of(Long profileId, String contentId, String episodeId, String unitKey,
                                             int position, boolean completed,
                                             Instant lastActivityAt, String lastSessionId) {
        ViewingProgressDocument d = new ViewingProgressDocument();
        d.profileId = profileId;
        d.contentId = contentId;
        d.episodeId = episodeId;
        d.unitKey = unitKey;
        d.lastPositionSeconds = position;
        d.completed = completed;
        d.lastActivityAt = lastActivityAt;
        d.lastSessionId = lastSessionId;
        return d;
    }

    public ObjectId getId() { return id; }
    public Long getProfileId() { return profileId; }
    public String getContentId() { return contentId; }
    public String getEpisodeId() { return episodeId; }
    public String getUnitKey() { return unitKey; }
    public int getLastPositionSeconds() { return lastPositionSeconds == null ? 0 : lastPositionSeconds; }
    public boolean isCompleted() { return Boolean.TRUE.equals(completed); }
    public Instant getLastActivityAt() { return lastActivityAt; }
    public String getLastSessionId() { return lastSessionId; }
}
