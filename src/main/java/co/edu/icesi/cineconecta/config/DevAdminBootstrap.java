package co.edu.icesi.cineconecta.config;

import co.edu.icesi.cineconecta.persistence.mongo.document.AdminUserDocument;
import com.mongodb.ConnectionString;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
@Profile("dev-admin-bootstrap")
public class DevAdminBootstrap implements ApplicationRunner {
    private final Environment environment;
    private final MongoTemplate mongo;
    private final PasswordEncoder encoder;

    public DevAdminBootstrap(Environment environment, MongoTemplate mongo, PasswordEncoder encoder) {
        this.environment = environment;
        this.mongo = mongo;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = environment.getProperty("CINE_ADMIN_USERNAME", "");
        String password = environment.getProperty("CINE_ADMIN_PASSWORD", "");
        if (username.isBlank() && password.isEmpty()) {
            return;
        }
        String normalized = AdminUserDocument.normalizeUsername(username);
        if (!normalized.matches("[a-z0-9][a-z0-9._-]{0,63}")
                || password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Configura un usuario válido y una contraseña de 12 a 72 bytes.");
        }
        ConnectionString connection = new ConnectionString(
                environment.getRequiredProperty("spring.data.mongodb.uri"));
        if (connection.getHosts().size() != 1
                || !connection.getHosts().getFirst().matches("(?:localhost|127\\.0\\.0\\.1):\\d+")
                || !mongo.getDb().getName().matches("cineconecta(?:_test_[a-z0-9_]+)?")) {
            throw new IllegalStateException("El bootstrap requiere una conexión MongoDB local.");
        }
        boolean indexed = mongo.indexOps(AdminUserDocument.class).getIndexInfo().stream()
                .anyMatch(index -> index.isUnique() && index.getName().equals("uq_admin_username"));
        if (!indexed) {
            throw new IllegalStateException("Ejecuta 02_catalog_admin.js antes del bootstrap.");
        }
        Query query = Query.query(Criteria.where("usernameNormalized").is(normalized));
        mongo.upsert(query, new Update()
                .setOnInsert("usernameNormalized", normalized)
                .setOnInsert("passwordHash", encoder.encode(password))
                .setOnInsert("role", "ADMIN")
                .setOnInsert("active", true)
                .setOnInsert("createdAt", Instant.now()), AdminUserDocument.class);
    }
}
