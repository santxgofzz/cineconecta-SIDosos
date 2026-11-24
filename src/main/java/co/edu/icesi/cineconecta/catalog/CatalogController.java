package co.edu.icesi.cineconecta.catalog;

import co.edu.icesi.cineconecta.common.security.CurrentAccount;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles/{profileId}/catalog")
public class CatalogController {
    private final CatalogService catalog;
    private final CurrentAccount accounts;

    public CatalogController(CatalogService catalog, CurrentAccount accounts) {
        this.catalog = catalog;
        this.accounts = accounts;
    }

    @GetMapping
    public CatalogService.Results search(@PathVariable long profileId,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(required = false) String type,
                                         @RequestParam(required = false) String genre,
                                         @RequestParam(required = false) String language,
                                         @RequestParam(required = false) Integer year,
                                         @RequestParam(required = false) Integer page,
                                         @RequestParam(required = false) Integer size) {
        return catalog.search(accounts.requireAccountId(), profileId,
                new CatalogFilters(q, type, genre, language, year, page, size));
    }

    @GetMapping("/{contentId}")
    public CatalogService.Detail detail(@PathVariable long profileId, @PathVariable String contentId) {
        return catalog.detail(accounts.requireAccountId(), profileId, contentId);
    }
}
