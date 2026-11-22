package co.edu.icesi.cineconecta.playback;

import co.edu.icesi.cineconecta.common.security.CurrentAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API del reproductor simulado. Toda autorización ocurre en el servidor. */
@RestController
@RequestMapping("/api/profiles/{profileId}/playback")
public class PlaybackController {
    private final PlaybackService playback;
    private final CurrentAccount currentAccount;

    public PlaybackController(PlaybackService playback, CurrentAccount currentAccount) {
        this.playback = playback;
        this.currentAccount = currentAccount;
    }

    public record StartRequest(@NotBlank String contentId, String episodeId) {}

    @PostMapping("/sessions")
    public ResponseEntity<PlaybackState> start(@PathVariable long profileId, @Valid @RequestBody StartRequest req) {
        PlaybackState state = playback.start(currentAccount.requireAccountId(), profileId, req.contentId(),
                req.episodeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(state);
    }

    @GetMapping("/sessions/{sessionId}")
    public PlaybackState state(@PathVariable long profileId, @PathVariable String sessionId) {
        return playback.state(currentAccount.requireAccountId(), profileId, sessionId);
    }

    @PostMapping("/sessions/{sessionId}/events")
    public PlaybackState event(@PathVariable long profileId, @PathVariable String sessionId,
                               @Valid @RequestBody PlaybackEvent event) {
        return playback.apply(currentAccount.requireAccountId(), profileId, sessionId, event);
    }
}
