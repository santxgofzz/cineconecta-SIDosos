package co.edu.icesi.cineconecta.subscription;

import co.edu.icesi.cineconecta.common.security.CurrentAccount;
import co.edu.icesi.cineconecta.subscription.BillingModels.PurchaseReceipt;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BillingControllerTest {
    @Test
    void purchaseUsesAuthenticatedAccountInsteadOfClientSuppliedId() throws Exception {
        BillingService service = mock(BillingService.class);
        CurrentAccount currentAccount = mock(CurrentAccount.class);
        when(currentAccount.requireAccountId()).thenReturn(17L);
        var instant = Instant.parse("2026-11-25T14:00:00Z");
        when(service.purchase(eq(17L), any())).thenReturn(new PurchaseReceipt(
                120L, 3L, "Basico", new BigDecimal("23000.00"), "SIM-REF", instant, instant.plusSeconds(2592000)));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new BillingController(service, currentAccount)).build();

        mvc.perform(post("/api/subscriptions/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":3,\"approvePayment\":true,\"accountId\":999}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscriptionId").value(120))
                .andExpect(jsonPath("$.paidAmount").value(23000.0));
        verify(service).purchase(eq(17L), argThat(req -> req.planId().equals(3L) && req.approvePayment()));
    }

    @Test
    void missingPaymentDecisionIsRejectedByValidation() throws Exception {
        BillingService service = mock(BillingService.class);
        CurrentAccount account = mock(CurrentAccount.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new BillingController(service, account)).build();
        mvc.perform(post("/api/subscriptions/purchase")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"planId\":3}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
