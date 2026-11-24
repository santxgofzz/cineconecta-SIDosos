package co.edu.icesi.cineconecta.catalog;

import co.edu.icesi.cineconecta.common.security.CurrentAccount;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/profiles/{profileId}/catalog")
public class CatalogPageController {
    private final CatalogService catalog;
    private final CurrentAccount accounts;

    public CatalogPageController(CatalogService catalog, CurrentAccount accounts) {
        this.catalog = catalog;
        this.accounts = accounts;
    }

    @GetMapping
    public String list(@PathVariable long profileId, @RequestParam(required = false) String q,
                       @RequestParam(required = false) String type, @RequestParam(required = false) String genre,
                       @RequestParam(required = false) String language, @RequestParam(required = false) Integer year,
                       @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
                       Model model) {
        var filters = new CatalogFilters(q, type, genre, language, year, page, size);
        model.addAttribute("result", catalog.search(accounts.requireAccountId(), profileId, filters));
        model.addAttribute("profileId", profileId);
        return "catalog/list";
    }

    @GetMapping("/{contentId}")
    public String detail(@PathVariable long profileId, @PathVariable String contentId, Model model) {
        model.addAttribute("detail", catalog.detail(accounts.requireAccountId(), profileId, contentId));
        model.addAttribute("profileId", profileId);
        return "catalog/detail";
    }
}
