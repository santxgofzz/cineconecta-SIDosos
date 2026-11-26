package co.edu.icesi.cineconecta.subscription;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.subscription.BillingModels.HistoryRow;
import co.edu.icesi.cineconecta.subscription.BillingModels.Plan;
import co.edu.icesi.cineconecta.subscription.BillingModels.PurchaseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingAdvancedRulesTest {
    private static final long CUENTA = 17L;
    private static final long ID_PLAN = 3L;
    private static final Instant AHORA = Instant.parse("2026-11-25T14:00:00.987654321Z");
    private static final BigDecimal PRECIO = new BigDecimal("23000.00");
    private static final Plan BASICO = new Plan(ID_PLAN, "Basico", PRECIO, 2);

    @Mock BillingRepository repositorio;
    private BillingService servicio;

    @BeforeEach
    void preparar() {
        servicio = new BillingService(repositorio, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private void prepararCuentaYPlan(Plan plan) {
        when(repositorio.lockAccount(CUENTA)).thenReturn(true);
        when(repositorio.findPlan(ID_PLAN)).thenReturn(Optional.of(plan));
    }

    private void prepararCompraValida(Plan plan) {
        prepararCuentaYPlan(plan);
        when(repositorio.nextSubscriptionId()).thenReturn(100L);
        when(repositorio.nextPaymentId()).thenReturn(200L);
    }

    @Test
    void solicitudNulaNoConsultaOracle() {
        assertCodigo("PURCHASE_INVALID", () -> servicio.purchase(CUENTA, null));
        verifyNoInteractions(repositorio);
    }

    @Test
    void cuentaInvalidaNoConsultaOracle() {
        assertCodigo("PURCHASE_INVALID", () -> servicio.purchase(0L, new PurchaseRequest(ID_PLAN, true)));
        verifyNoInteractions(repositorio);
    }

    @Test
    void identificadoresDePlanInvalidosNoConsultanOracle() {
        assertCodigo("PURCHASE_INVALID", () -> servicio.purchase(CUENTA, new PurchaseRequest(null, true)));
        assertCodigo("PURCHASE_INVALID", () -> servicio.purchase(CUENTA, new PurchaseRequest(0L, true)));
        assertCodigo("PURCHASE_INVALID", () -> servicio.purchase(CUENTA, new PurchaseRequest(-1L, true)));
        verifyNoInteractions(repositorio);
    }

    @Test
    void decisionDePagoFaltanteNoConsultaOracle() {
        assertCodigo("PURCHASE_INVALID", () -> servicio.purchase(CUENTA, new PurchaseRequest(ID_PLAN, null)));
        verifyNoInteractions(repositorio);
    }

    @Test
    void pagoRechazadoNoReservaIdsNiHaceEscrituras() {
        assertCodigo("PAYMENT_REJECTED", () -> servicio.purchase(CUENTA, new PurchaseRequest(ID_PLAN, false)));
        verifyNoInteractions(repositorio);
    }

    @Test
    void cuentaInexistenteNoConsultaNiReservaElPlan() {
        assertCodigo("ACCOUNT_NOT_FOUND", () -> servicio.purchase(CUENTA, new PurchaseRequest(ID_PLAN, true)));
        verify(repositorio).lockAccount(CUENTA);
        verifyNoMoreInteractions(repositorio);
    }

    @Test
    void planInexistenteNoReservaIds() {
        when(repositorio.lockAccount(CUENTA)).thenReturn(true);
        when(repositorio.findPlan(ID_PLAN)).thenReturn(Optional.empty());
        assertCodigo("PLAN_NOT_FOUND", () -> servicio.purchase(CUENTA, new PurchaseRequest(ID_PLAN, true)));
        verify(repositorio, never()).nextSubscriptionId();
        verify(repositorio, never()).nextPaymentId();
    }

    @Test
    void precioNegativoSeRechazaAntesDeEscribir() {
        prepararCuentaYPlan(new Plan(ID_PLAN, "Error", new BigDecimal("-1.00"), 2));
        assertThrows(IllegalStateException.class,
                () -> servicio.purchase(CUENTA, new PurchaseRequest(ID_PLAN, true)));
        verify(repositorio, never()).insertSubscription(anyLong(), anyLong(), anyLong(), any(), any());
    }

    @Test
    void precioNuloSeRechazaAntesDeEscribir() {
        prepararCuentaYPlan(new Plan(ID_PLAN, "Error", null, 2));
        assertThrows(IllegalStateException.class,
                () -> servicio.purchase(CUENTA, new PurchaseRequest(ID_PLAN, true)));
        verify(repositorio, never()).nextSubscriptionId();
    }

    @Test
    void planSinCuposSeRechazaAntesDeEscribir() {
        prepararCuentaYPlan(new Plan(ID_PLAN, "Error", PRECIO, 0));
        assertThrows(IllegalStateException.class,
                () -> servicio.purchase(CUENTA, new PurchaseRequest(ID_PLAN, true)));
        verify(repositorio, never()).nextSubscriptionId();
    }

    @Test
    void periodoSuperpuestoSeRechazaAntesDeReservarIds() {
        prepararCuentaYPlan(BASICO);
        when(repositorio.hasOverlappingSubscription(eq(CUENTA), any(), any())).thenReturn(true);
        assertCodigo("SUBSCRIPTION_OVERLAP", () -> servicio.purchase(CUENTA, new PurchaseRequest(ID_PLAN, true)));
        verify(repositorio, never()).nextSubscriptionId();
        verify(repositorio, never()).nextPaymentId();
        verify(repositorio, never()).countActiveProfiles(CUENTA);
    }

    @Test
    void noDejaContratarPlanConMenosCuposQuePerfilesActivos() {
        prepararCuentaYPlan(BASICO);
        when(repositorio.countActiveProfiles(CUENTA)).thenReturn(3);
        assertCodigo("PROFILE_LIMIT", () -> servicio.purchase(CUENTA, new PurchaseRequest(ID_PLAN, true)));
        verify(repositorio, never()).insertPayment(anyLong(), anyLong(), any(), anyString(), any());
    }

    @Test
    void permiteContratarCuandoLosPerfilesIgualanAlMaximo() {
        prepararCompraValida(BASICO);
        when(repositorio.countActiveProfiles(CUENTA)).thenReturn(2);
        var recibo = servicio.purchase(CUENTA, new PurchaseRequest(ID_PLAN, true));
        assertEquals(100L, recibo.subscriptionId());
        verify(repositorio).insertPayment(eq(200L), eq(100L), eq(PRECIO), anyString(), any());
    }

    @Test
    void usaMontoOracleYTreintaDiasExactosSinNanosegundos() {
        prepararCompraValida(BASICO);
        var recibo = servicio.purchase(CUENTA, new PurchaseRequest(ID_PLAN, true));
        Instant inicio = AHORA.truncatedTo(ChronoUnit.MILLIS);
        assertEquals(inicio, recibo.startsAt());
        assertEquals(inicio.plus(30, ChronoUnit.DAYS), recibo.endsAt());
        assertEquals(PRECIO, recibo.paidAmount());
        assertEquals(ID_PLAN, recibo.planId());
        verify(repositorio).insertSubscription(100L, CUENTA, ID_PLAN, recibo.startsAt(), recibo.endsAt());
        verify(repositorio).insertPayment(200L, 100L, PRECIO, recibo.paymentReference(), inicio);
    }

    @Test
    void falloAlInsertarSuscripcionNoIntentaInsertarPago() {
        prepararCompraValida(BASICO);
        doThrow(new DataAccessResourceFailureException("oracle no pudo insertar la suscripcion"))
                .when(repositorio).insertSubscription(anyLong(), anyLong(), anyLong(), any(), any());
        assertThrows(DataAccessResourceFailureException.class,
                () -> servicio.purchase(CUENTA, new PurchaseRequest(ID_PLAN, true)));
        verify(repositorio, never()).insertPayment(anyLong(), anyLong(), any(), anyString(), any());
    }

    @Test
    void falloAlInsertarPagoNoDevuelveReciboFalso() {
        prepararCompraValida(BASICO);
        doThrow(new DataAccessResourceFailureException("oracle no confirmo el pago"))
                .when(repositorio).insertPayment(anyLong(), anyLong(), any(), anyString(), any());
        assertThrows(DataAccessResourceFailureException.class,
                () -> servicio.purchase(CUENTA, new PurchaseRequest(ID_PLAN, true)));
        verify(repositorio).insertSubscription(anyLong(), eq(CUENTA), eq(ID_PLAN), any(), any());
        // aca no se prueba un rollback real; para eso esta BillingOracleIntegrationTest del s3.
    }

    @Test
    void historialRespetaInicioInclusivoFinExclusivoYPlanHistorico() {
        Instant instante = AHORA;
        when(repositorio.findHistory(CUENTA)).thenReturn(List.of(
                new HistoryRow(1L, 8L, "Anterior", new BigDecimal("18000"), "P-1",
                        instante.minus(30, ChronoUnit.DAYS), instante),
                new HistoryRow(2L, 3L, "Actual", PRECIO, "P-2",
                        instante, instante.plus(30, ChronoUnit.DAYS)),
                new HistoryRow(3L, 9L, "Futuro", new BigDecimal("35000"), "P-3",
                        instante.plus(1, ChronoUnit.DAYS), instante.plus(31, ChronoUnit.DAYS))));
        var historial = servicio.history(CUENTA);
        assertEquals(3, historial.size());
        assertFalse(historial.get(0).active());
        assertTrue(historial.get(1).active());
        assertFalse(historial.get(2).active());
        assertEquals("Anterior", historial.get(0).planName());
        assertEquals(3L, historial.get(1).planId());
        assertEquals("Futuro", historial.get(2).planName());
        verify(repositorio).findHistory(CUENTA);
    }

    @Test
    void historialVacioNoConsultaOtraCuenta() {
        when(repositorio.findHistory(CUENTA)).thenReturn(List.of());
        assertTrue(servicio.history(CUENTA).isEmpty());
        verify(repositorio).findHistory(CUENTA);
        verifyNoMoreInteractions(repositorio);
    }

    @Test
    void listaLosPlanesSinModificarOracle() {
        when(repositorio.findPlans()).thenReturn(List.of(BASICO));
        assertEquals(List.of(BASICO), servicio.plans());
        verify(repositorio).findPlans();
        verifyNoMoreInteractions(repositorio);
    }

    @Test
    void compraEstaDeclaradaComoTransaccionConRollback() throws Exception {
        Transactional transaccion = BillingService.class
                .getMethod("purchase", long.class, PurchaseRequest.class)
                .getAnnotation(Transactional.class);
        assertNotNull(transaccion);
        assertArrayEquals(new Class<?>[]{Exception.class}, transaccion.rollbackFor());
    }

    @Test
    void limitesDeVigenciaValidanElInstanteExacto() {
        Instant inicio = AHORA;
        Instant fin = inicio.plus(30, ChronoUnit.DAYS);
        assertFalse(SubscriptionWindow.isActive(inicio, fin, inicio.minus(1, ChronoUnit.NANOS)));
        assertTrue(SubscriptionWindow.isActive(inicio, fin, inicio));
        assertTrue(SubscriptionWindow.isActive(inicio, fin, fin.minus(1, ChronoUnit.NANOS)));
        assertFalse(SubscriptionWindow.isActive(inicio, fin, fin));
        assertFalse(SubscriptionWindow.isActive(inicio, fin, fin.plusNanos(1)));
        assertThrows(IllegalArgumentException.class, () -> SubscriptionWindow.isActive(inicio, inicio, inicio));
    }

    private static void assertCodigo(String esperado, org.junit.jupiter.api.function.Executable accion) {
        ApiException error = assertThrows(ApiException.class, accion);
        assertEquals(esperado, error.code());
    }
}
