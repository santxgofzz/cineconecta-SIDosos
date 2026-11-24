package co.edu.icesi.cineconecta.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminCatalogPageController {
    private final AdminCatalogService catalog;
    private final ObjectMapper json;

    public AdminCatalogPageController(AdminCatalogService catalog, ObjectMapper json) {
        this.catalog = catalog;
        this.json = json;
    }

    @GetMapping(path = "/admin/login", produces = "text/html")
    public String login() {
        return "admin/login";
    }

    @GetMapping({"/admin", "/admin/contents"})
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("contents", catalog.list(page, 20));
        return "admin/list";
    }

    @GetMapping("/admin/contents/new")
    public String create() {
        return "admin/edit";
    }

    @GetMapping("/admin/contents/{id}/edit")
    public String edit(@PathVariable String id, Model model) throws JsonProcessingException {
        var item = catalog.get(id);
        model.addAttribute("item", item);
        model.addAttribute("initial", json.writeValueAsString(item.details()));
        return "admin/edit";
    }

    @PostMapping("/admin/contents/{id}/publish")
    public String publish(@PathVariable String id, RedirectAttributes redirect) {
        catalog.publish(id);
        redirect.addFlashAttribute("notice", "Contenido publicado.");
        return "redirect:/admin/contents";
    }

    @PostMapping("/admin/contents/{id}/retire")
    public String retire(@PathVariable String id, RedirectAttributes redirect) {
        catalog.retire(id);
        redirect.addFlashAttribute("notice", "Contenido retirado.");
        return "redirect:/admin/contents";
    }
}
