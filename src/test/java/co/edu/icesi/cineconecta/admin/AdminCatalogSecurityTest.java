package co.edu.icesi.cineconecta.admin;

import co.edu.icesi.cineconecta.config.AdminSecurityConfig;
import co.edu.icesi.cineconecta.config.SecurityConfig;
import co.edu.icesi.cineconecta.persistence.mongo.document.AdminUserDocument;
import co.edu.icesi.cineconecta.persistence.mongo.repository.AdminUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(controllers = {AdminCatalogController.class, AdminLoginController.class, AdminCatalogPageController.class},
        excludeAutoConfiguration = org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class)
@Import({AdminSecurityConfig.class, SecurityConfig.class})
class AdminCatalogSecurityTest {
    private static final String BASE = "/api/admin/contents";

    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;
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

    @Test
    void adminPagesRenderFormsAndRequireCsrfForStateChanges() throws Exception {
        var details = new CatalogInput("movie", "Título & portada", "", 2020, List.of("Drama"), List.of("CO"),
                List.of("es"), List.of(), 12, "https://example.org/image.jpg", 120, "Dirección",
                List.of(), null, null, null, null);
        var item = new AdminCatalogItem("CNT-1", "draft", Instant.EPOCH, Instant.EPOCH, 0, details);
        when(catalog.list(0, 20)).thenReturn(new PageImpl<>(List.of(item)));
        when(catalog.get("CNT-1")).thenReturn(item);
        when(catalog.create(details)).thenReturn(item);
        when(catalog.update("CNT-1", 0, details)).thenReturn(item);
        mvc.perform(get("/admin/login").accept(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("current-password")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/admin/contents")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/contents").with(user("titular").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/contents").with(user("admin").roles("ADMIN")))
                .andExpect(content().string(containsString("Título &amp; portada")))
                .andExpect(content().string(containsString("Confirmar publicación")));
        mvc.perform(get("/admin/contents/new").with(user("admin").roles("ADMIN")))
                .andExpect(content().string(containsString("Agregar temporada")));
        mvc.perform(get("/admin/contents/CNT-1/edit").with(user("admin").roles("ADMIN")))
                .andExpect(content().string(containsString("data-revision=\"0\"")));
        String payload = json.writeValueAsString(details);
        mvc.perform(post(BASE).with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());
        mvc.perform(put(BASE + "/CNT-1").with(user("admin").roles("ADMIN")).with(csrf())
                        .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());
        mvc.perform(post("/admin/contents/CNT-1/publish").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/contents/CNT-1/publish").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/contents/CNT-1/retire").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        verify(catalog).publish("CNT-1");
        verify(catalog).retire("CNT-1");
    }

    private AdminUserDocument admin(String secret, boolean active) {
        return new AdminUserDocument(new ObjectId(), "admin.local", encoder.encode(secret), "ADMIN",
                active, Instant.EPOCH);
    }
}
