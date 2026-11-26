package co.edu.icesi.cineconecta.subscription;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.security.CurrentAccount;
import co.edu.icesi.cineconecta.config.SecurityConfig;
import co.edu.icesi.cineconecta.subscription.BillingModels.Plan;
import co.edu.icesi.cineconecta.subscription.BillingModels.PurchaseReceipt;
import co.edu.icesi.cineconecta.subscription.BillingModels.PurchaseRequest;
import co.edu.icesi.cineconecta.subscription.BillingModels.SubscriptionHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BillingPageController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({SecurityConfig.class, BillingPageErrors.class})
class BillingPageControllerTest {
    private static final Instant START = Instant.parse("2026-11-25T15:00:00Z");

    @Autowired MockMvc mvc;
    @MockitoBean BillingService billing;
    @MockitoBean CurrentAccount currentAccount;

    private void prepararCuentaConHistorial(boolean active) {
        when(currentAccount.requireAccountId()).thenReturn(17L);
        when(billing.plans()).thenReturn(List.of(new Plan(3, "Básico", new BigDecimal("23000.00"), 2)));
        when(billing.history(17L)).thenReturn(List.of(new SubscriptionHistory(
                40, 3, "Básico", new BigDecimal("23000.00"), "SIM-40", START,
                START.plusSeconds(2592000), active)));
    }

    @Test
    void muestraPlanesEHistorialSoloDeLaCuentaAutenticada() throws Exception {
        prepararCuentaConHistorial(false);
        mvc.perform(get("/subscriptions").with(user("titular")))
                .andExpect(status().isOk())
                .andExpect(view().name("subscription/index"))
                .andExpect(content().string(containsString("Básico")))
                .andExpect(content().string(containsString("SIM-40")))
                .andExpect(content().string(containsString("Finalizada")));
        verify(billing).history(17L);
    }

    @Test
    void adviertePlanVigenteYNoMuestraBotonDeCompra() throws Exception {
        prepararCuentaConHistorial(true);
        mvc.perform(get("/subscriptions").with(user("titular")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ya tienes una suscripción vigente")))
                .andExpect(content().string(not(containsString("Contratar plan</button>"))));
    }

    @Test
    void compraConCsrfYCuentaDeSesionHaceRedirectSinRepetirPost() throws Exception {
        prepararCuentaConHistorial(false);
        var receipt = new PurchaseReceipt(51, 3, "Básico", new BigDecimal("23000.00"),
                "SIM-51", START, START.plusSeconds(2592000));
        when(billing.purchase(eq(17L), any(PurchaseRequest.class))).thenReturn(receipt);

        mvc.perform(post("/subscriptions/purchase")
                        .with(user("titular")).with(csrf())
                        .param("planId", "3")
                        .param("approvePayment", "true")
                        .param("accountId", "999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscriptions"))
                .andExpect(flash().attribute("receipt", receipt));
        verify(billing).purchase(eq(17L), argThat(p -> p.planId().equals(3L) && p.approvePayment()));
        verify(billing, never()).purchase(eq(999L), any());
    }

    @Test
    void pagoRechazadoVuelveConErrorYNoMuestraExito() throws Exception {
        when(currentAccount.requireAccountId()).thenReturn(17L);
        when(billing.purchase(eq(17L), any(PurchaseRequest.class))).thenThrow(
                ApiException.badRequest("PAYMENT_REJECTED", "El pago simulado fue rechazado."));

        mvc.perform(post("/subscriptions/purchase").with(user("titular")).with(csrf())
                        .param("planId", "3").param("approvePayment", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "El pago simulado fue rechazado."))
                .andExpect(flash().attributeCount(1));
    }

    @Test
    void faltaDeCsrfBloqueaLaCompra() throws Exception {
        mvc.perform(post("/subscriptions/purchase").with(user("titular"))
                        .param("planId", "3").param("approvePayment", "true"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(billing);
    }

    @Test
    void sinSesionRedirigeAlLogin() throws Exception {
        mvc.perform(get("/subscriptions"))
                .andExpect(status().is3xxRedirection());
        verifyNoInteractions(billing);
    }

    @Test
    void cuentaSinPermisosNoVeSuscripciones() throws Exception {
        when(currentAccount.requireAccountId()).thenThrow(ApiException.forbidden(
                "ACCOUNT_REQUIRED", "Debes iniciar sesión como titular de una cuenta."));
        mvc.perform(get("/subscriptions").with(user("administrador")))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Debes iniciar sesión como titular")));
        verifyNoInteractions(billing);
    }

    @Test
    void datosDeCompraInvalidosSeRechazanConUnMensajeEntendible() throws Exception {
        mvc.perform(post("/subscriptions/purchase").with(user("titular")).with(csrf())
                        .param("planId", "texto").param("approvePayment", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Revisa los datos de la solicitud.")));
        verifyNoInteractions(billing);
    }

    @Test
    void fallaEnOracleNoFiltraDetallesInternosDeLaBase() throws Exception {
        when(currentAccount.requireAccountId()).thenReturn(17L);
        when(billing.plans()).thenThrow(new DataAccessResourceFailureException("ORA-12345 prueba interna"));
        mvc.perform(get("/subscriptions").with(user("titular")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(containsString("No se pueden consultar las suscripciones")))
                .andExpect(content().string(not(containsString("ORA-12345"))));
    }

    @Test
    void errorDeCompraEnOracleDevuelveAvisoSinDatosTecnicos() throws Exception {
        when(currentAccount.requireAccountId()).thenReturn(17L);
        when(billing.purchase(eq(17L), any(PurchaseRequest.class)))
                .thenThrow(new DataAccessResourceFailureException("jdbc:oracle:ejemplo"));
        mvc.perform(post("/subscriptions/purchase").with(user("titular")).with(csrf())
                        .param("planId", "3").param("approvePayment", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "No se pudo confirmar la compra. Verifica la conexion e intenta de nuevo."));
    }
}
