package co.edu.icesi.cineconecta.subscription;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.subscription.BillingModels.HistoryRow;
import co.edu.icesi.cineconecta.subscription.BillingModels.Plan;
import co.edu.icesi.cineconecta.subscription.BillingModels.PurchaseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-25T14:00:00Z");
    private static final Plan BASIC = new Plan(3L, "Basico", new BigDecimal("23000.00"), 2);
    @Mock BillingRepository repository;
    BillingService service;

    @BeforeEach
    void setup() {
        service = new BillingService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void allowValidPurchase() {
        when(repository.lockAccount(17L)).thenReturn(true);
        when(repository.findPlan(3L)).thenReturn(Optional.of(BASIC));
        when(repository.nextSubscriptionId()).thenReturn(120L);
        when(repository.nextPaymentId()).thenReturn(245L);
    }

    @Test
    void approvedPurchaseUsesOraclePriceAndWritesExactlyOnePayment() {
        allowValidPurchase();
        var receipt = service.purchase(17L, new PurchaseRequest(3L, true));
        assertEquals(120L, receipt.subscriptionId());
        assertEquals(BASIC.price(), receipt.paidAmount());
        assertEquals(NOW, receipt.startsAt());
        assertEquals(NOW.plus(30, java.time.temporal.ChronoUnit.DAYS), receipt.endsAt());
        assertTrue(receipt.paymentReference().startsWith("SIM-"));

        verify(repository).lockAccount(17L);
        verify(repository).hasOverlappingSubscription(17L, receipt.startsAt(), receipt.endsAt());
        verify(repository).countActiveProfiles(17L);
        verify(repository).insertSubscription(120L, 17L, 3L, receipt.startsAt(), receipt.endsAt());
        verify(repository).insertPayment(245L, 120L, BASIC.price(), receipt.paymentReference(), NOW);
    }

    @Test
    void rejectedPaymentDoesNotTouchDatabase() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.purchase(17L, new PurchaseRequest(3L, false)));
        assertEquals("PAYMENT_REJECTED", ex.code());
        verifyNoInteractions(repository);
    }

    @Test
    void invalidPurchaseDoesNotTouchDatabase() {
        assertEquals("PURCHASE_INVALID", assertThrows(ApiException.class,
                () -> service.purchase(17L, new PurchaseRequest(-1L, true))).code());
        verifyNoInteractions(repository);
    }

    @Test
    void missingAccountDoesNotWriteAnything() {
        assertEquals("ACCOUNT_NOT_FOUND", assertThrows(ApiException.class,
                () -> service.purchase(17L, new PurchaseRequest(3L, true))).code());
        verify(repository, never()).insertSubscription(anyLong(), anyLong(), anyLong(), any(), any());
    }

    @Test
    void overlappingPeriodIsRejected() {
        when(repository.lockAccount(17L)).thenReturn(true);
        when(repository.findPlan(3L)).thenReturn(Optional.of(BASIC));
        when(repository.hasOverlappingSubscription(eq(17L), any(), any())).thenReturn(true);
        var ex = assertThrows(ApiException.class,
                () -> service.purchase(17L, new PurchaseRequest(3L, true)));
        assertEquals("SUBSCRIPTION_OVERLAP", ex.code());
        verify(repository, never()).insertPayment(anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    void tooManyActiveProfilesAreRejected() {
        when(repository.lockAccount(17L)).thenReturn(true);
        when(repository.findPlan(3L)).thenReturn(Optional.of(BASIC));
        when(repository.countActiveProfiles(17L)).thenReturn(3);
        var ex = assertThrows(ApiException.class,
                () -> service.purchase(17L, new PurchaseRequest(3L, true)));
        assertEquals("PROFILE_LIMIT", ex.code());
        verify(repository, never()).insertSubscription(anyLong(), anyLong(), anyLong(), any(), any());
    }

    @Test
    void paymentErrorPropagatesAndServiceHasRollbackTransaction() throws Exception {
        allowValidPurchase();
        doThrow(new IllegalStateException("error de pago simulado")).when(repository)
                .insertPayment(anyLong(), anyLong(), any(), anyString(), any());
        assertThrows(IllegalStateException.class,
                () -> service.purchase(17L, new PurchaseRequest(3L, true)));
        verify(repository).insertSubscription(eq(120L), eq(17L), eq(3L), any(), any());
        var annotation = BillingService.class.getMethod("purchase", long.class, PurchaseRequest.class)
                .getAnnotation(Transactional.class);
        assertNotNull(annotation);
        assertEquals(Exception.class, annotation.rollbackFor()[0]);
        // la verificacion real de rollback oracle se deja como prueba s3.
    }

    @Test
    void historyChecksDatesUsingStartInclusiveEndExclusive() {
        when(repository.findHistory(17L)).thenReturn(List.of(
                new HistoryRow(1L, 3L, "Basico", BASIC.price(), "P1", NOW.minusSeconds(50), NOW),
                new HistoryRow(2L, 3L, "Basico", BASIC.price(), "P2", NOW, NOW.plusSeconds(50))));
        var rows = service.history(17L);
        assertFalse(rows.get(0).active());
        assertTrue(rows.get(1).active());
    }
}
