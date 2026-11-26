package co.edu.icesi.cineconecta.admin;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.error.ErrorCodes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/admin/contents")
public class AdminCatalogController {
    private final AdminCatalogService catalog;

    public AdminCatalogController(AdminCatalogService catalog) {
        this.catalog = catalog;
    }

    public record CatalogPage(List<AdminCatalogItem> items, int page, int size, long totalElements, int totalPages) {}

    @GetMapping
    public CatalogPage list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var result = catalog.list(page, size);
        return new CatalogPage(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminCatalogItem> get(@PathVariable String id) {
        return ok(catalog.get(id));
    }

    @PostMapping
    public ResponseEntity<AdminCatalogItem> create(@RequestBody CatalogInput input,
                                                   UriComponentsBuilder uriBuilder) {
        var item = catalog.create(input);
        var location = uriBuilder.path("/api/admin/contents/{id}").buildAndExpand(item.id()).toUri();
        return ResponseEntity.created(location).eTag(tag(item)).body(item);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminCatalogItem> update(@PathVariable String id,
                                                    @RequestHeader(HttpHeaders.IF_MATCH) String etag,
                                                    @RequestBody CatalogInput input) {
        return ok(catalog.update(id, revision(etag), input));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<AdminCatalogItem> publish(@PathVariable String id) {
        return ok(catalog.publish(id));
    }

    @PostMapping("/{id}/retire")
    public ResponseEntity<AdminCatalogItem> retire(@PathVariable String id) {
        return ok(catalog.retire(id));
    }

    private static ResponseEntity<AdminCatalogItem> ok(AdminCatalogItem item) {
        return ResponseEntity.ok().eTag(tag(item)).body(item);
    }

    private static String tag(AdminCatalogItem item) {
        return "\"" + item.revision() + "\"";
    }

    private static long revision(String etag) {
        if (etag.matches("\"-?[0-9]{1,19}\"")) {
            try {
                return Long.parseLong(etag.substring(1, etag.length() - 1));
            } catch (NumberFormatException ignored) {
                // Misma respuesta para un identificador fuera de rango.
            }
        }
        throw ApiException.badRequest(ErrorCodes.INVALID_CONTENT, "Envía el ETag del contenido en If-Match.");
    }
}
