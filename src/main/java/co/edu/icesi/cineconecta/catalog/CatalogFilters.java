package co.edu.icesi.cineconecta.catalog;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.error.ErrorCodes;

import java.util.Locale;
import java.util.Set;

public record CatalogFilters(String q, String type, String genre, String language, Integer year,
                             Integer page, Integer size) {
    static final Set<String> TYPES = Set.of("movie", "series", "documentary");

    public CatalogFilters {
        q = normalize(q, 100, "título");
        type = normalize(type, 20, "tipo");
        genre = normalize(genre, 80, "género");
        language = normalize(language, 40, "idioma");
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
        if (type != null) {
            type = type.toLowerCase(Locale.ROOT);
            if (!TYPES.contains(type)) {
                throw invalid("Tipo de contenido inválido.");
            }
        }
        if (year != null && (year < 1888 || year > 2100)) {
            throw invalid("El año debe estar entre 1888 y 2100.");
        }
        if (page < 0 || size < 1 || size > 100) {
            throw invalid("La página debe ser positiva o cero y el tamaño debe estar entre 1 y 100.");
        }
    }

    private static String normalize(String value, int limit, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > limit || normalized.codePoints().anyMatch(Character::isISOControl)) {
            throw invalid("Filtro de " + label + " inválido.");
        }
        return normalized;
    }

    static ApiException invalid(String message) {
        return ApiException.badRequest(ErrorCodes.INVALID_CONTENT, message);
    }
}
