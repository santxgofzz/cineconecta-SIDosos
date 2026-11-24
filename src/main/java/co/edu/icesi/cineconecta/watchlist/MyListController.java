package co.edu.icesi.cineconecta.watchlist;

import co.edu.icesi.cineconecta.common.security.CurrentAccount;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de Mi lista. PUT y DELETE son idempotentes para que el cliente pueda
 * reintentar sin crear duplicados. La cuenta sale de la sesión del servidor.
 */
@RestController
@RequestMapping("/api/profiles/{profileId}/my-list")
public class MyListController {
    private final MyListService service;
    private final CurrentAccount currentAccount;

    public MyListController(MyListService service, CurrentAccount currentAccount) {
        this.service = service;
        this.currentAccount = currentAccount;
    }

    @GetMapping
    public List<MyListEntryView> list(@PathVariable long profileId) {
        return service.list(currentAccount.requireAccountId(), profileId);
    }

    /** 201 si se agregó, 200 si ya estaba. */
    @PutMapping("/{contentId}")
    public ResponseEntity<MyListEntryView> add(@PathVariable long profileId, @PathVariable String contentId) {
        MyListService.AddResult r = service.add(currentAccount.requireAccountId(), profileId, contentId);
        return ResponseEntity.status(r.created() ? HttpStatus.CREATED : HttpStatus.OK).body(r.entry());
    }

    /** 204 siempre que el estado final sea "no está en la lista". */
    @DeleteMapping("/{contentId}")
    public ResponseEntity<Void> remove(@PathVariable long profileId, @PathVariable String contentId) {
        service.remove(currentAccount.requireAccountId(), profileId, contentId);
        return ResponseEntity.noContent().build();
    }
}
