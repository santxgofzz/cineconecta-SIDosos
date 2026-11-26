package co.edu.icesi.cineconecta.account;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.error.ErrorCodes;
import co.edu.icesi.cineconecta.common.security.CurrentAccount;
import co.edu.icesi.cineconecta.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccountPageController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({SecurityConfig.class, CurrentAccount.class})
class AccountPagesTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;

    @MockitoBean AccountService accountService;
    @MockitoBean AccountUserDetailsService accountUsers;

    @Test
    void loginPageRendersFormCsrfAndMessages() throws Exception {
        mvc.perform(get("/login").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("action=\"/login\"")))
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(content().string(containsString("href=\"/register\"")));

        mvc.perform(get("/login").param("error", "").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo iniciar sesión")));

        mvc.perform(get("/login").param("registered", "").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cuenta creada correctamente")));

        mvc.perform(get("/login").param("logout", "").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sesión cerrada.")));
    }

    @Test
    void customerHtmlLoginRedirectsToProfileSelection() throws Exception {
        String secret = UUID.randomUUID().toString();
        when(accountUsers.loadUserByUsername("cliente@ejemplo.com"))
                .thenReturn(new AccountUserDetails(15L, "cliente@ejemplo.com", encoder.encode(secret)));

        mvc.perform(post("/login").with(csrf()).accept(MediaType.TEXT_HTML)
                        .param("username", "cliente@ejemplo.com")
                        .param("password", secret))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profiles"));
    }

    @Test
    void registerPageValidatesPasswordsHandlesConflictsAndCreatesAccount() throws Exception {
        mvc.perform(get("/register").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("name=\"fullName\"")))
                .andExpect(content().string(containsString("name=\"email\"")))
                .andExpect(content().string(containsString("name=\"confirmPassword\"")));

        mvc.perform(post("/register")
                        .param("fullName", "Ana")
                        .param("email", "ana@ejemplo.com")
                        .param("password", "ClaveSegura2026!")
                        .param("confirmPassword", "ClaveSegura2026!"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/register").with(csrf())
                        .param("fullName", "Ana Pérez")
                        .param("email", "ana@ejemplo.com")
                        .param("password", "ClaveSegura2026!")
                        .param("confirmPassword", "OtraClave2026!"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Las contraseñas no coinciden.")))
                .andExpect(content().string(containsString("ana@ejemplo.com")));
        verifyNoInteractions(accountService);

        when(accountService.register(any()))
                .thenThrow(ApiException.conflict(ErrorCodes.EMAIL_ALREADY_EXISTS,
                        "Ya existe una cuenta registrada con ese correo."))
                .thenReturn(new AccountResponse(100L, "Ana Pérez", "ana@ejemplo.com", Instant.EPOCH));

        mvc.perform(post("/register").with(csrf())
                        .param("fullName", "Ana Pérez")
                        .param("email", "ana@ejemplo.com")
                        .param("password", "ClaveSegura2026!")
                        .param("confirmPassword", "ClaveSegura2026!"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ya existe una cuenta registrada con ese correo.")));

        mvc.perform(post("/register").with(csrf())
                        .param("fullName", "Ana Pérez")
                        .param("email", "ana@ejemplo.com")
                        .param("password", "ClaveSegura2026!")
                        .param("confirmPassword", "ClaveSegura2026!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        verify(accountService, org.mockito.Mockito.times(2))
                .register(new AccountRegistrationRequest("Ana Pérez", "ana@ejemplo.com", "ClaveSegura2026!"));
    }
}
