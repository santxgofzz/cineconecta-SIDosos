package co.edu.icesi.cineconecta.persistence.mongo.document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Locale;

@Document("admin_users")
public record AdminUserDocument(
        @Id ObjectId id,
        String usernameNormalized,
        @JsonIgnore String passwordHash,
        String role,
        boolean active,
        Instant createdAt) {

    public static String normalizeUsername(String username) {
        return username.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "AdminUserDocument[id=" + id + ", active=" + active + "]";
    }
}
