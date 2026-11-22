package co.edu.icesi.cineconecta.integration;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/** Lecturas del catálogo MongoDB. Lanza DataAccessException si Mongo no responde. */
public interface CatalogGateway {
    Optional<CatalogEntry> findById(String contentId);

    Map<String, CatalogEntry> findByIds(Collection<String> contentIds);
}
