package co.edu.icesi.cineconecta.account;

import co.edu.icesi.cineconecta.admin.AdminCatalogController;
import co.edu.icesi.cineconecta.admin.AdminCatalogService;
import co.edu.icesi.cineconecta.admin.AdminLoginController;
import co.edu.icesi.cineconecta.common.error.ApiExceptionHandler;
import co.edu.icesi.cineconecta.common.security.CurrentAccount;
import co.edu.icesi.cineconecta.config.AdminSecurityConfig;
import co.edu.icesi.cineconecta.config.SecurityConfig;
import co.edu.icesi.cineconecta.persistence.mongo.document.AdminUserDocument;
import co.edu.icesi.cineconecta.persistence.mongo.repository.AdminUserRepository;
import co.edu.icesi.cineconecta.profile.ProfileController;
import co.edu.icesi.cineconecta.profile.ProfileResponse;
import co.edu.icesi.cineconecta.profile.ProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AccountController.class,
        ProfileController.class,
        AdminCatalogController.class,
        AdminLoginController.class
}, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({AdminSecurityConfig.class, SecurityConfig.class, CurrentAccount.class, ApiExceptionHandler.class})
class AccountSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;

    @MockitoBean AccountService accountService;
    @MockitoBean ProfileService profileService;
    @MockitoBean AccountUserDetailsService accountUsers;
    @MockitoBean AdminCatalogService adminCatalog;
    @MockitoBean AdminUserRepository adminUsers;

    @Test
    void registrationIsPublicWithCsrfAndRejectsMissingCsrf() throws Exception {
        var req = new AccountRegistrationRequest("Laura Gómez", "laura@ejemplo.com", "ClaveSegura2026!");
        when(accountService.register(any())).thenReturn(
                new AccountResponse(100L, "Laura Gómez", "laura@ejemplo.com", Instant.EPOCH));

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(100L))
                .andExpect(jsonPath("$.email").value("laura@ejemplo.com"));
    }

    @Test
    void customerLogsInViaOracleUserDetailsAndUsesSessionAccountId() throws Exception {
        String secret = UUID.randomUUID().toString();
        var customer = new AccountUserDetails(15L, "cliente@ejemplo.com", encoder.encode(secret));
        when(accountUsers.loadUserByUsername("cliente@ejemplo.com")).thenReturn(customer);
        when(accountService.getAccount(15L))
                .thenReturn(new AccountResponse(15L, "Cliente Demo", "cliente@ejemplo.com", Instant.EPOCH));
        when(profileService.listProfiles(15L)).thenReturn(List.of(
                new ProfileResponse(50L, 15L, "Principal", 18, "Mayores de 18 anos", true, "A")));

        mvc.perform(get("/api/accounts/me")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/profiles")).andExpect(status().is3xxRedirection());

        mvc.perform(post("/login").with(csrf())
                        .param("username", "cliente@ejemplo.com")
                        .param("password", "clave-incorrecta"))
                .andExpect(status().isUnauthorized());

        var login = mvc.perform(post("/login").with(csrf())
                        .param("username", "cliente@ejemplo.com")
                        .param("password", secret))
                .andExpect(status().isNoContent())
                .andReturn();
        var customerSession = (MockHttpSession) login.getRequest().getSession(false);

        mvc.perform(get("/api/accounts/me").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(15L));

        mvc.perform(get("/api/profiles").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].profileId").value(50L));

        // El cliente NO puede acceder al panel/API de administración de MongoDB.
        mvc.perform(get("/api/admin/contents").session(customerSession))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/contents").with(user(customer)))
                .andExpect(status().isForbidden());

        verify(accountService).getAccount(15L);
        verify(profileService).listProfiles(15L);
        verifyNoInteractions(adminCatalog);
    }

    @Test
    void mongoAdminSessionCannotAccessCustomerAccountOrProfileEndpoints() throws Exception {
        String secret = UUID.randomUUID().toString();
        when(accountUsers.loadUserByUsername(any())).thenThrow(new UsernameNotFoundException("No es cliente"));
        when(adminUsers.findByUsernameNormalized("admin.local")).thenReturn(Optional.of(
                new AdminUserDocument(new ObjectId(), "admin.local", encoder.encode(secret), "ADMIN", true, Instant.EPOCH)));

        var adminLogin = mvc.perform(post("/admin/login").with(csrf())
                        .param("username", "admin.local")
                        .param("password", secret))
                .andExpect(status().isNoContent())
                .andReturn();
        var adminSession = (MockHttpSession) adminLogin.getRequest().getSession(false);

        // El contexto de seguridad de admin es independiente del de clientes.
        mvc.perform(get("/api/accounts/me").session(adminSession))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/profiles").session(adminSession))
                .andExpect(status().is3xxRedirection());

        // Incluso si hubiera un principal autenticado con ROLE_ADMIN sin AccountPrincipal, CurrentAccount lo rechaza.
        mvc.perform(get("/api/accounts/me").with(user("admin.local").roles("ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_REQUIRED"));

        verifyNoInteractions(accountService, profileService);
    }
}
