package co.edu.icesi.cineconecta.admin;

import co.edu.icesi.cineconecta.config.AdminSecurityConfig;
import co.edu.icesi.cineconecta.config.SecurityConfig;
import co.edu.icesi.cineconecta.persistence.mongo.document.AdminUserDocument;
import co.edu.icesi.cineconecta.persistence.mongo.repository.AdminUserRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminCatalogController.class, AdminLoginController.class},
        excludeAutoConfiguration = org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class)
@Import({AdminSecurityConfig.class, SecurityConfig.class})
class AdminCatalogSecurityTest {
    private static final String BASE = "/api/admin/contents";

    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @MockitoBean AdminCatalogService catalog;
    @MockitoBean AdminUserRepository users;

    @Test
    void anonymousAndCustomerCannotUseAnyCatalogOperation() throws Exception {
        assertDenied(HttpMethod.GET, BASE);
        assertDenied(HttpMethod.GET, BASE + "/CNT-1");
        assertDenied(HttpMethod.POST, BASE);
        assertDenied(HttpMethod.PUT, BASE + "/CNT-1");
        assertDenied(HttpMethod.POST, BASE + "/CNT-1/publish");
        assertDenied(HttpMethod.POST, BASE + "/CNT-1/retire");
        verifyNoInteractions(catalog);
    }

    private void assertDenied(HttpMethod method, String path) throws Exception {
        mvc.perform(request(method, path).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(request(method, path).with(csrf()).with(user("titular").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void activeMongoAdministratorLogsInAndRequiresCsrfForChanges() throws Exception {
        String secret = UUID.randomUUID().toString();
        when(users.findByUsernameNormalized("admin.local")).thenReturn(Optional.of(admin(secret, true)));
        when(catalog.list(0, 20)).thenReturn(Page.empty());

        mvc.perform(get("/admin/login")).andExpect(status().isOk());
        mvc.perform(post("/admin/login").param("username", "admin.local").param("password", secret))
                .andExpect(status().isForbidden());
        var login = mvc.perform(post("/admin/login").with(csrf())
                        .param("username", "  ADMIN.Local ").param("password", secret))
                .andExpect(status().isNoContent()).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);

        mvc.perform(get(BASE).session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/profiles/12/my-list").session(session)).andExpect(status().is3xxRedirection());
        mvc.perform(post(BASE).session(session)).andExpect(status().isForbidden());
        mvc.perform(request(HttpMethod.PUT, BASE + "/CNT-1").session(session)).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/CNT-1/publish").session(session)).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/CNT-1/retire").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get(BASE).session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsIncorrectPasswordAndInactiveAdministrator() throws Exception {
        String secret = UUID.randomUUID().toString();
        when(users.findByUsernameNormalized("admin.local")).thenReturn(Optional.of(admin(secret, true)));
        mvc.perform(post("/admin/login").with(csrf())
                        .param("username", "admin.local").param("password", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());

        when(users.findByUsernameNormalized("admin.local")).thenReturn(Optional.of(admin(secret, false)));
        mvc.perform(post("/admin/login").with(csrf()).param("username", "admin.local").param("password", secret))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(catalog);
    }

    private AdminUserDocument admin(String secret, boolean active) {
        return new AdminUserDocument(new ObjectId(), "admin.local", encoder.encode(secret), "ADMIN",
                active, Instant.EPOCH);
    }
}
