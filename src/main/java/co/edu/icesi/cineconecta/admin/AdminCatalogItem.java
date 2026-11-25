package co.edu.icesi.cineconecta.admin;

import co.edu.icesi.cineconecta.persistence.mongo.document.ContentDocument;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.time.Instant;

public record AdminCatalogItem(String id, String status, Instant createdAt, Instant updatedAt,
                               long revision, @JsonUnwrapped CatalogInput details) {
    static AdminCatalogItem from(ContentDocument content) {
        return new AdminCatalogItem(content.id(), content.status(), content.createdAt(), content.updatedAt(),
                content.updatedAt().toEpochMilli(), CatalogInput.from(content));
    }
}
