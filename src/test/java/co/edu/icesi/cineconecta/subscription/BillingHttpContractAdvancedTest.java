package co.edu.icesi.cineconecta.subscription;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.error.ApiExceptionHandler;
import co.edu.icesi.cineconecta.common.security.CurrentAccount;
import co.edu.icesi.cineconecta.subscription.BillingModels.Plan;
import co.edu.icesi.cineconecta.subscription.BillingModels.PurchaseRequest;
import co.edu.icesi.cineconecta.subscription.BillingModels.SubscriptionHistory;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BillingHttpContractAdvancedTest {
    private static final Instant AHORA = Instant.parse("2026-11-25T15:00:00Z");

    private static MockMvc prepararMvc(BillingService servicio, CurrentAccount cuenta) {
        return MockMvcBuilders.standaloneSetup(new BillingController(servicio, cuenta))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void listarPlanesExigeCuentaAutenticada() throws Exception {
        BillingService servicio = mock(BillingService.class);
        CurrentAccount cuenta = mock(CurrentAccount.class);
        when(cuenta.requireAccountId()).thenThrow(ApiException.forbidden(
                "ACCOUNT_REQUIRED", "Inicia sesion como titular"));

        prepararMvc(servicio, cuenta).perform(get("/api/subscriptions/plans"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_REQUIRED"));
        verifyNoInteractions(servicio);
    }

    @Test
    void elHistorialSoloConsultaLaCuentaDelServidor() throws Exception {
        BillingService servicio = mock(BillingService.class);
        CurrentAccount cuenta = mock(CurrentAccount.class);
        when(cuenta.requireAccountId()).thenReturn(17L);
        when(servicio.history(17L)).thenReturn(List.of(new SubscriptionHistory(
                120L, 3L, "Basico", new BigDecimal("23000.00"), "SIM-120",
                AHORA, AHORA.plusSeconds(2_592_000), true)));

        prepararMvc(servicio, cuenta)
                .perform(get("/api/subscriptions/history").param("accountId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subscriptionId").value(120))
                .andExpect(jsonPath("$[0].active").value(true));
        verify(servicio).history(17L);
        verify(servicio, never()).history(999L);
    }

    @Test
    void listadoDePlanesDevuelveDatosDeOraclePorServicio() throws Exception {
        BillingService servicio = mock(BillingService.class);
        CurrentAccount cuenta = mock(CurrentAccount.class);
        when(cuenta.requireAccountId()).thenReturn(17L);
        when(servicio.plans()).thenReturn(List.of(new Plan(3L, "Basico", new BigDecimal("23000.00"), 2)));

        prepararMvc(servicio, cuenta)
                .perform(get("/api/subscriptions/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].planId").value(3))
                .andExpect(jsonPath("$[0].maxProfiles").value(2));
        verify(servicio).plans();
    }

    @Test
    void comprarSinLoginResponde403AntesDeInvocarElServicio() throws Exception {
        BillingService servicio = mock(BillingService.class);
        CurrentAccount cuenta = mock(CurrentAccount.class);
        when(cuenta.requireAccountId()).thenThrow(ApiException.forbidden(
                "ACCOUNT_REQUIRED", "Inicia sesion como titular"));

        prepararMvc(servicio, cuenta)
                .perform(post("/api/subscriptions/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":3,\"approvePayment\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_REQUIRED"));
        verifyNoInteractions(servicio);
    }

    @Test
    void rechazarPagoDevuelve400YSinEscriturasEnOracle() throws Exception {
        BillingRepository repositorio = mock(BillingRepository.class);
        BillingService servicio = new BillingService(repositorio, Clock.fixed(AHORA, ZoneOffset.UTC));
        CurrentAccount cuenta = mock(CurrentAccount.class);
        when(cuenta.requireAccountId()).thenReturn(17L);

        prepararMvc(servicio, cuenta)
                .perform(post("/api/subscriptions/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":3,\"approvePayment\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAYMENT_REJECTED"));
        verifyNoInteractions(repositorio);
    }

    @Test
    void idDePlanNegativoNoLlegaAlServicio() throws Exception {
        BillingService servicio = mock(BillingService.class);
        CurrentAccount cuenta = mock(CurrentAccount.class);

        prepararMvc(servicio, cuenta)
                .perform(post("/api/subscriptions/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":-1,\"approvePayment\":true}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }
}
