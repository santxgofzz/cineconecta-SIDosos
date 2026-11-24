package co.edu.icesi.cineconecta.admin;

import co.edu.icesi.cineconecta.config.AdminSecurityConfig;
import co.edu.icesi.cineconecta.config.SecurityConfig;
import co.edu.icesi.cineconecta.persistence.mongo.document.AdminUserDocument;
import co.edu.icesi.cineconecta.persistence.mongo.repository.AdminUserRepository;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminLoginController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({AdminSecurityConfig.class, SecurityConfig.class, AdminLoginMongoTest.MongoFixture.class})
@EnabledIfEnvironmentVariable(named = "CINE_CATALOG_TEST_URI",
        matches = "mongodb://(?:127\\.0\\.0\\.1|localhost):[0-9]+/cineconecta_test_[a-z0-9_]+\\?directConnection=true")
class AdminLoginMongoTest {
    @Autowired MockMvc mvc;
    @Autowired AdminUserRepository admins;
    @Autowired PasswordEncoder encoder;

    @Test
    void securityChainAuthenticatesOnlyActiveAdminsWithTheirStoredBcryptPassword() throws Exception {
        String secret = UUID.randomUUID().toString();
        String active = insertAdmin(secret, true);
        String inactive = insertAdmin(secret, false);

        mvc.perform(post("/admin/login").with(csrf()).param("username", active.toUpperCase(Locale.ROOT))
                        .param("password", secret)).andExpect(status().isNoContent());
        mvc.perform(post("/admin/login").with(csrf()).param("username", active)
                        .param("password", UUID.randomUUID().toString())).andExpect(status().isUnauthorized());
        mvc.perform(post("/admin/login").with(csrf()).param("username", inactive)
                        .param("password", secret)).andExpect(status().isUnauthorized());
    }

    private String insertAdmin(String secret, boolean active) {
        String username = "auth-test." + UUID.randomUUID();
        admins.insert(new AdminUserDocument(null, username, encoder.encode(secret), "ADMIN", active, Instant.now()));
        return username;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MongoFixture {
        @Bean
        MongoClient catalogAuthMongoClient() {
            return MongoClients.create(System.getenv("CINE_CATALOG_TEST_URI"));
        }

        @Bean
        AdminUserRepository catalogAuthAdminUsers(MongoClient client) {
            String database = new ConnectionString(System.getenv("CINE_CATALOG_TEST_URI")).getDatabase();
            return new MongoRepositoryFactory(new MongoTemplate(client, database)).getRepository(AdminUserRepository.class);
        }
    }
}
