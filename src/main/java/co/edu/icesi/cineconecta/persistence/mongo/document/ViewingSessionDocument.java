package co.edu.icesi.cineconecta.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Sesión histórica de reproducción. Cada inicio crea un documento nuevo; nunca
 * se reemplaza ni se borra. {@code watchedSeconds} solo acumula tiempo
 * reproducido (no saltos) y {@code lastPositionSeconds} es la posición alcanzada.
 *
 * <p>{@code lastEventSeq} y {@code lastEventAt} son campos técnicos para hacer
 * idempotentes los reintentos del cliente y acotar el tiempo declarado al
 * tiempo real transcurrido. {@code durationSecondsSnapshot} y
 * {@code completedSnapshot} fotografían la unidad reproducida. El validador
 * no prohíbe campos adicionales.</p>
 */
@Document(collection = ViewingSessionDocument.COLLECTION)
public class ViewingSessionDocument {
    public static final String COLLECTION = "viewing_sessions";

    @Id
    private String id;
    private Long profileId;
    private Long subscriptionId;
    private String contentId;
    private String episodeId;
    private String unitKey;
    private String contentTypeSnapshot;
    private Instant startedAt;
    private Instant endedAt;
    private Integer watchedSeconds;
    private Integer lastPositionSeconds;
    private String endReason;
    private Integer durationSecondsSnapshot;
    private Boolean completedSnapshot;
    private Integer lastEventSeq;
    private Instant lastEventAt;

    protected ViewingSessionDocument() {}

    public ViewingSessionDocument(String id, Long profileId, Long subscriptionId, String contentId,
                                  String episodeId, String unitKey, String contentTypeSnapshot,
                                  Instant startedAt, int startPositionSeconds, int durationSecondsSnapshot) {
        this.id = id;
        this.profileId = profileId;
        this.subscriptionId = subscriptionId;
        this.contentId = contentId;
        this.episodeId = episodeId;
        this.unitKey = unitKey;
        this.contentTypeSnapshot = contentTypeSnapshot;
        this.startedAt = startedAt;
        this.watchedSeconds = 0;
        this.lastPositionSeconds = startPositionSeconds;
        this.durationSecondsSnapshot = durationSecondsSnapshot;
        this.completedSnapshot = false;
        this.lastEventSeq = 0;
        this.lastEventAt = startedAt;
    }

    /** Copia para calcular un nuevo estado sin mutar el leído (actualización optimista). */
    public ViewingSessionDocument copy() {
        ViewingSessionDocument c = new ViewingSessionDocument();
        c.id = id;
        c.profileId = profileId;
        c.subscriptionId = subscriptionId;
        c.contentId = contentId;
        c.episodeId = episodeId;
        c.unitKey = unitKey;
        c.contentTypeSnapshot = contentTypeSnapshot;
        c.startedAt = startedAt;
        c.endedAt = endedAt;
        c.watchedSeconds = watchedSeconds;
        c.lastPositionSeconds = lastPositionSeconds;
        c.endReason = endReason;
        c.durationSecondsSnapshot = durationSecondsSnapshot;
        c.completedSnapshot = completedSnapshot;
        c.lastEventSeq = lastEventSeq;
        c.lastEventAt = lastEventAt;
        return c;
    }

    public boolean isOpen() { return endedAt == null; }

    public String getId() { return id; }
    public Long getProfileId() { return profileId; }
    public Long getSubscriptionId() { return subscriptionId; }
    public String getContentId() { return contentId; }
    public String getEpisodeId() { return episodeId; }
    public String getUnitKey() { return unitKey; }
    public String getContentTypeSnapshot() { return contentTypeSnapshot; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public int getWatchedSeconds() { return watchedSeconds == null ? 0 : watchedSeconds; }
    public int getLastPositionSeconds() { return lastPositionSeconds == null ? 0 : lastPositionSeconds; }
    public String getEndReason() { return endReason; }
    public int getDurationSecondsSnapshot() { return durationSecondsSnapshot == null ? 0 : durationSecondsSnapshot; }
    public boolean isCompletedSnapshot() { return Boolean.TRUE.equals(completedSnapshot); }
    public int getLastEventSeq() { return lastEventSeq == null ? 0 : lastEventSeq; }
    public Instant getLastEventAt() { return lastEventAt == null ? startedAt : lastEventAt; }

    public void setWatchedSeconds(int watchedSeconds) { this.watchedSeconds = watchedSeconds; }
    public void setLastPositionSeconds(int lastPositionSeconds) { this.lastPositionSeconds = lastPositionSeconds; }
    public void setCompletedSnapshot(boolean completedSnapshot) { this.completedSnapshot = completedSnapshot; }
    public void setLastEventSeq(int lastEventSeq) { this.lastEventSeq = lastEventSeq; }
    public void setLastEventAt(Instant lastEventAt) { this.lastEventAt = lastEventAt; }

    public void close(Instant endedAt, String endReason) {
        this.endedAt = endedAt;
        this.endReason = endReason;
    }
}
