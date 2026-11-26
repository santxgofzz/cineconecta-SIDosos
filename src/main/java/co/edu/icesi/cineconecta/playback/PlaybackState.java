package co.edu.icesi.cineconecta.playback;

import co.edu.icesi.cineconecta.persistence.mongo.document.ViewingSessionDocument;

import java.time.Instant;

/** Estado de una sesión devuelto al reproductor simulado. */
public record PlaybackState(String sessionId, String contentId, String episodeId, String unitKey,
                            String contentType, int positionSeconds, int watchedSeconds, int durationSeconds,
                            boolean completed, boolean open, String endReason, int lastEventSeq,
                            Instant startedAt, Instant endedAt) {
    static PlaybackState of(ViewingSessionDocument s) {
        return new PlaybackState(s.getId(), s.getContentId(), s.getEpisodeId(), s.getUnitKey(),
                s.getContentTypeSnapshot(), s.getLastPositionSeconds(), s.getWatchedSeconds(),
                s.getDurationSecondsSnapshot(), s.isCompletedSnapshot(), s.isOpen(), s.getEndReason(),
                s.getLastEventSeq(), s.getStartedAt(), s.getEndedAt());
    }
}
