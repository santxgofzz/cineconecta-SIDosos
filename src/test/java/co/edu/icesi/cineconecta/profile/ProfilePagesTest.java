package co.edu.icesi.cineconecta.profile;

import co.edu.icesi.cineconecta.account.AccountUserDetails;
import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.error.ErrorCodes;
import co.edu.icesi.cineconecta.common.security.CurrentAccount;
import co.edu.icesi.cineconecta.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProfilePageController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({SecurityConfig.class, CurrentAccount.class})
class ProfilePagesTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProfileService profileService;

    private static UsernamePasswordAuthenticationToken customer(long accountId) {
        var principal = new AccountUserDetails(accountId, "titular@ejemplo.com", "hash");
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    void anonymousUsersAreRedirectedToLogin() throws Exception {
        mvc.perform(get("/profiles"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/profiles/manage"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        verifyNoInteractions(profileService);
    }

    @Test
    void selectPageShowsOnlyActiveProfilesWithCatalogAndActivityLinks() throws Exception {
        when(profileService.listProfiles(7L)).thenReturn(List.of(
                new ProfileResponse(15L, 7L, "Principal", 18, "Mayores de 18 anos", true, "A"),
                new ProfileResponse(16L, 7L, "Pausado", 7, "Mayores de 7 anos", false, "I")
        ));

        mvc.perform(get("/profiles").with(authentication(customer(7L))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Principal")))
                .andExpect(content().string(containsString("/profiles/15/catalog")))
                .andExpect(content().string(containsString("/profiles/15/activity")))
                .andExpect(content().string(not(containsString("/profiles/16/catalog"))))
                .andExpect(content().string(containsString("/profiles/manage")))
                .andExpect(content().string(containsString("action=\"/logout\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void managePageRendersAgeRatingsAndAllProfileForms() throws Exception {
        when(profileService.listProfiles(7L)).thenReturn(List.of(
                new ProfileResponse(15L, 7L, "Principal", 18, "Mayores de 18 anos", true, "A"),
                new ProfileResponse(16L, 7L, "Kids", 7, "Mayores de 7 anos", false, "I")
        ));
        when(profileService.listAgeRatings()).thenReturn(List.of(
                new ProfileService.AgeRatingOption(0, "Todo publico"),
                new ProfileService.AgeRatingOption(7, "Mayores de 7 anos"),
                new ProfileService.AgeRatingOption(18, "Mayores de 18 anos")
        ));

        mvc.perform(get("/profiles/manage").with(authentication(customer(7L))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Todo publico")))
                .andExpect(content().string(containsString("Mayores de 7 anos")))
                .andExpect(content().string(containsString("Mayores de 18 anos")))
                .andExpect(content().string(containsString("/profiles/manage/15/edit")))
                .andExpect(content().string(containsString("/profiles/manage/15/deactivate")))
                .andExpect(content().string(containsString("/profiles/manage/16/reactivate")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void profileManagementActionsRequireCsrfAndSurfaceBusinessErrors() throws Exception {
        mvc.perform(post("/profiles/manage").with(authentication(customer(7L)))
                        .param("name", "Infantil")
                        .param("maxAgeRating", "7"))
                .andExpect(status().isForbidden());

        when(profileService.createProfile(7L, new ProfileRequest("Infantil", 7)))
                .thenReturn(new ProfileResponse(30L, 7L, "Infantil", 7, "Mayores de 7 anos", true, "A"));

        mvc.perform(post("/profiles/manage").with(authentication(customer(7L))).with(csrf())
                        .param("name", "  Infantil ")
                        .param("maxAgeRating", "7"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profiles/manage"))
                .andExpect(flash().attributeExists("successMessage"));
        verify(profileService).createProfile(7L, new ProfileRequest("Infantil", 7));

        when(profileService.createProfile(7L, new ProfileRequest("Extra", 12)))
                .thenThrow(ApiException.conflict(ErrorCodes.PROFILE_LIMIT_EXCEEDED,
                        "Se alcanzó el máximo de perfiles activos permitidos por el plan."));

        mvc.perform(post("/profiles/manage").with(authentication(customer(7L))).with(csrf())
                        .param("name", "Extra")
                        .param("maxAgeRating", "12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profiles/manage"))
                .andExpect(flash().attribute("errorMessage",
                        "Se alcanzó el máximo de perfiles activos permitidos por el plan."));

        mvc.perform(post("/profiles/manage/15/edit").with(authentication(customer(7L))).with(csrf())
                        .param("name", "Editado")
                        .param("maxAgeRating", "12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profiles/manage"))
                .andExpect(flash().attributeExists("successMessage"));
        verify(profileService).updateProfile(7L, 15L, new ProfileRequest("Editado", 12));

        mvc.perform(post("/profiles/manage/15/deactivate").with(authentication(customer(7L))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profiles/manage"))
                .andExpect(flash().attributeExists("successMessage"));
        verify(profileService).deactivateProfile(7L, 15L);

        when(profileService.reactivateProfile(7L, 16L))
                .thenThrow(ApiException.forbidden(ErrorCodes.SUBSCRIPTION_REQUIRED,
                        "La cuenta no tiene una suscripción vigente."));
        mvc.perform(post("/profiles/manage/16/reactivate").with(authentication(customer(7L))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profiles/manage"))
                .andExpect(flash().attribute("errorMessage",
                        "La cuenta no tiene una suscripción vigente."));
    }
}
