package co.edu.icesi.cineconecta.admin;

import co.edu.icesi.cineconecta.persistence.mongo.document.ContentDocument;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.function.Function;

public record CatalogInput(
        @NotBlank String type,
        @NotBlank @Size(max = 250) String title,
        @NotNull @Size(max = 5000) String synopsis,
        @NotNull @Min(1888) @Max(2100) Integer releaseYear,
        @NotEmpty List<@NotBlank String> genres,
        @NotEmpty List<@NotBlank String> countries,
        @NotEmpty List<@NotBlank String> audioLanguages,
        @NotNull List<@NotBlank String> subtitleLanguages,
        @NotNull @Min(0) @Max(18) Integer ageRating,
        @NotBlank @Size(max = 2048) String coverUrl,
        @Positive Integer durationSeconds,
        String director,
        List<@NotNull @Valid CastMember> cast,
        List<@NotNull @Valid Season> seasons,
        List<@NotBlank String> topics,
        List<@NotBlank String> interviewees,
        List<@NotNull @Valid Source> sources) {

    public record CastMember(@NotBlank String actorName, String characterName) {}

    public record Season(@NotNull @Positive Integer number, @NotNull String description,
                         @NotEmpty List<@NotNull @Valid Episode> episodes) {}

    public record Episode(@Size(max = 128) String id, @NotNull @Positive Integer number,
                          @NotBlank String title, @NotNull String synopsis,
                          @NotNull @Positive Integer durationSeconds) {}

    public record Source(@NotBlank String name, @Size(max = 2048) String url) {}

    static CatalogInput from(ContentDocument content) {
        return new CatalogInput(content.type(), content.title(), content.synopsis(),
                content.releaseYear(), content.genres(), content.countries(), content.audioLanguages(),
                content.subtitleLanguages(), content.ageRating(), content.coverUrl(), content.durationSeconds(),
                content.director(), map(content.cast(), item -> new CastMember(item.actorName(), item.characterName())),
                map(content.seasons(), season -> new Season(season.number(), season.description(),
                        map(season.episodes(), ep -> new Episode(ep.episodeId(), ep.number(), ep.title(),
                                ep.synopsis(), ep.durationSeconds())))),
                content.topics(), content.interviewees(), map(content.sources(), item -> new Source(item.name(), item.url())));
    }

    static <T, R> List<R> map(List<T> items, Function<T, R> mapper) {
        return items == null ? null : items.stream().map(mapper).toList();
    }
}
