package co.edu.icesi.cineconecta.playback;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.error.ApiExceptionHandler;
import co.edu.icesi.cineconecta.common.error.ErrorCodes;
import co.edu.icesi.cineconecta.common.security.AccountPrincipal;
import co.edu.icesi.cineconecta.common.security.CurrentAccount;
import co.edu.icesi.cineconecta.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** RNF-02: la autorización se aplica en el servidor, no solo en la interfaz. */
@WebMvcTest(PlaybackController.class)
@Import({SecurityConfig.class, CurrentAccount.class, ApiExceptionHandler.class})
class PlaybackControllerSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean PlaybackService playback;

    record Holder(long accountId) implements AccountPrincipal {
        @Override public long getAccountId() { return accountId; }
    }

    private static UsernamePasswordAuthenticationToken holder(long accountId) {
        return new UsernamePasswordAuthenticationToken(new Holder(accountId), null,
                AuthorityUtils.createAuthorityList("ROLE_CUSTOMER"));
    }

    private static final String BODY = "{\"contentId\":\"CNT-001\"}";

    @Test
    void anonymousCannotStartPlayback() throws Exception {
        mvc.perform(post("/api/profiles/15/playback/sessions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().is3xxRedirection());
        verifyNoInteractions(playback);
    }

    @Test
    void authenticatedUserWithoutAccountIsRejected() throws Exception {
        mvc.perform(post("/api/profiles/15/playback/sessions").with(csrf()).with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.ACCOUNT_REQUIRED));
        verifyNoInteractions(playback);
    }

    @Test
    void accountIdComesFromSessionNotFromRequest() throws Exception {
        when(playback.start(eq(7L), eq(21L), any(), any()))
                .thenThrow(ApiException.forbidden(ErrorCodes.PROFILE_NOT_ALLOWED, "ajeno"));

        mvc.perform(post("/api/profiles/21/playback/sessions?accountId=2").with(csrf())
                        .with(authentication(holder(7L)))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.PROFILE_NOT_ALLOWED));
        verify(playback).start(7L, 21L, "CNT-001", null);
    }

    @Test
    void databaseOutageIsReportedAs503() throws Exception {
        when(playback.start(anyLong(), anyLong(), any(), any()))
                .thenThrow(ApiException.unavailable(ErrorCodes.ORACLE_UNAVAILABLE, "Oracle caído", null));

        mvc.perform(post("/api/profiles/15/playback/sessions").with(csrf())
                        .with(authentication(holder(1L)))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(ErrorCodes.ORACLE_UNAVAILABLE));
    }

    @Test
    void missingCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/api/profiles/15/playback/sessions").with(authentication(holder(1L)))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(playback);
    }
}
