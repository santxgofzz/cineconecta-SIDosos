package co.edu.icesi.cineconecta.persistence.mongo.document;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;
import java.util.List;

@Document("contents")
public record ContentDocument(
        @MongoId(FieldType.STRING) String id,
        String type,
        String title,
        String synopsis,
        Integer releaseYear,
        List<String> genres,
        List<String> countries,
        List<String> audioLanguages,
        List<String> subtitleLanguages,
        Integer ageRating,
        String coverUrl,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Integer durationSeconds,
        String director,
        List<CastMember> cast,
        List<Season> seasons,
        List<String> topics,
        List<String> interviewees,
        List<Source> sources) {

    public record CastMember(String actorName, String characterName) {}

    public record Season(Integer number, String description, List<Episode> episodes) {}

    public record Episode(@Field("id") String episodeId, Integer number, String title,
                          String synopsis, Integer durationSeconds) {}

    public record Source(String name, String url) {}
}
