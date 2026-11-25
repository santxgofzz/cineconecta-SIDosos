package co.edu.icesi.cineconecta.playback;

import co.edu.icesi.cineconecta.common.security.CurrentAccount;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/profiles/{profileId}")
public class ActivityController {
    private final ActivityService activity;
    private final CurrentAccount currentAccount;

    public ActivityController(ActivityService activity, CurrentAccount currentAccount) {
        this.activity = activity;
        this.currentAccount = currentAccount;
    }

    @GetMapping("/continue-watching")
    public List<ActivityService.ContinueItem> continueWatching(@PathVariable long profileId,
                                                               @RequestParam(defaultValue = "12") int limit) {
        return activity.continueWatching(currentAccount.requireAccountId(), profileId, limit);
    }

    @GetMapping("/history")
    public ActivityService.HistoryPage history(@PathVariable long profileId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return activity.history(currentAccount.requireAccountId(), profileId, page, size);
    }
}
