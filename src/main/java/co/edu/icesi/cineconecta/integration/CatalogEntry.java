package co.edu.icesi.cineconecta.integration;

import java.util.List;
import java.util.Optional;

/**
 * Vista de solo lectura de un documento de {@code contents} con lo necesario
 * para Mi lista y reproducción. El módulo de catálogo es el dueño del documento.
 */
public record CatalogEntry(String id, String type, String title, String status, Integer ageRating,
                           Integer durationSeconds, List<Season> seasons) {
    public static final String MOVIE = "movie";
    public static final String SERIES = "series";
    public static final String DOCUMENTARY = "documentary";
    public static final String PUBLISHED = "published";
    public static final String RETIRED = "retired";

    public record Season(Integer number, List<Episode> episodes) {}

    public record Episode(String id, Integer number, String title, Integer durationSeconds) {}

    public boolean isPublished() { return PUBLISHED.equals(status); }

    public boolean isRetired() { return RETIRED.equals(status); }

    public boolean isSeries() { return SERIES.equals(type); }

    public int ageRatingOrMax() { return ageRating == null ? Integer.MAX_VALUE : ageRating; }

    public Optional<Episode> findEpisode(String episodeId) {
        if (seasons == null || episodeId == null) return Optional.empty();
        return seasons.stream()
                .filter(s -> s.episodes() != null)
                .flatMap(s -> s.episodes().stream())
                .filter(e -> episodeId.equals(e.id()))
                .findFirst();
    }
}
