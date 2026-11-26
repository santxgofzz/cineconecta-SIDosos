package co.edu.icesi.cineconecta.subscription;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.subscription.BillingModels.PurchaseRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** pruebas reales contra oracle, activadas solo cuando el equipo lo indique. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BillingService.class, JdbcBillingRepository.class, BillingOracleIntegrationTest.Soporte.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@EnabledIfEnvironmentVariable(named = "RUN_ORACLE_IT", matches = "(?i)true")
class BillingOracleIntegrationTest {
    private static final Instant AHORA = Instant.parse("2026-11-25T15:00:00Z");
    private static final BigDecimal PRECIO = new BigDecimal("24900.00");

    @Autowired private BillingService billing;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired @Qualifier("repositorioS3") private BillingRepository repositorio;

    private Long accountId;
    private Long planId;

    @TestConfiguration(proxyBeanMethods = false)
    static class Soporte {
        @Bean
        @Primary
        Clock clockS3() {
            return Clock.fixed(AHORA, ZoneOffset.UTC);
        }

        // solo las pruebas usan este espia, en produccion se usa el repositorio normal.
        @Bean("repositorioS3")
        @Primary
        BillingRepository repositorioS3(JdbcBillingRepository real) {
            return spy(real);
        }
    }

    @BeforeEach
    void prepararDatosAislados() {
        String identificador = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        accountId = (long) ThreadLocalRandom.current().nextInt(1_000_000_000, 1_700_000_000);
        planId = (long) ThreadLocalRandom.current().nextInt(1_700_000_000, 2_000_000_000);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("""
                    INSERT INTO ACCOUNTS (accountId, fullName, email, passwordHash, createdAt)
                    VALUES (?, ?, ?, ?, ?)
                    """, accountId, "Cuenta temporal S3", "s3." + identificador + "@example.invalid",
                    "$2a$10$N6x1FpG9O.z.1m2WdNsOHuvafSqXwrWftEnp2u9xulEVTyNECduRK",
                    Timestamp.from(AHORA));
            jdbc.update("""
                    INSERT INTO PLANS (planId, price, maxProfiles, name)
                    VALUES (?, ?, ?, ?)
                    """, planId, PRECIO, 2, "S3-" + identificador);
        });
    }

    @AfterEach
    void borrarDatosTemporales() {
        // se borra por la cuenta de prueba, nunca por rangos globales ni por fechas.
        try {
            if (accountId != null && planId != null) {
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    jdbc.update("""
                            DELETE FROM PAYMENTS
                             WHERE SUBSCRIPTIONS_subscriptionId IN
                                   (SELECT subscriptionId FROM SUBSCRIPTIONS
                                     WHERE ACCOUNTS_accountId = ?)
                            """, accountId);
                    jdbc.update("DELETE FROM SUBSCRIPTIONS WHERE ACCOUNTS_accountId = ?", accountId);
                    jdbc.update("DELETE FROM ACCOUNTS WHERE accountId = ?", accountId);
                    jdbc.update("DELETE FROM PLANS WHERE planId = ?", planId);
                });
            }
        } finally {
            reset(repositorio);
        }
    }

    @Test
    void dosContratacionesSimultaneasDejanUnaSolaSuscripcionYPago() throws Exception {
        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        List<Resultado> resultados = new ArrayList<>();

        try {
            Callable<Resultado> intento = () -> {
                preparados.countDown();
                if (!salida.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("no se libero el inicio de la prueba");
                }
                try {
                    var recibo = billing.purchase(accountId, new PurchaseRequest(planId, true));
                    return new Resultado(true, "OK", recibo.subscriptionId());
                } catch (ApiException error) {
                    return new Resultado(false, error.code(), null);
                }
            };
            Future<Resultado> primera = ejecutor.submit(intento);
            Future<Resultado> segunda = ejecutor.submit(intento);
            assertTrue(preparados.await(10, TimeUnit.SECONDS), "ambos hilos deben quedar preparados");
            salida.countDown();
            resultados.add(primera.get(45, TimeUnit.SECONDS));
            resultados.add(segunda.get(45, TimeUnit.SECONDS));
        } finally {
            salida.countDown();
            ejecutor.shutdownNow();
            assertTrue(ejecutor.awaitTermination(15, TimeUnit.SECONDS), "los hilos deben terminar antes de limpiar");
        }

        assertEquals(1, resultados.stream().filter(Resultado::aprobado).count(),
                "solamente una de las dos compras puede confirmarse");
        assertEquals(1, resultados.stream().filter(r -> "SUBSCRIPTION_OVERLAP".equals(r.codigo())).count(),
                "la otra compra tiene que rechazarse por periodo superpuesto");
        assertEquals(1, contarSuscripciones());
        assertEquals(1, contarPagos());
        assertEquals(0, contarSuscripcionesSinPago());
        System.out.println("s3 ok: 2 compras simultaneas; 1 confirmada, 1 rechazada; 1 pago y 0 registros parciales");
    }

    @Test
    void falloAntesDeGuardarPagoRevierteLaSuscripcionEnOracle() {
        // el espia falla justo despues del insert real de la suscripcion.
        // la excepcion sale del servicio y el gestor transaccional hace rollback.
        doThrow(new IllegalStateException("fallo forzado s3 antes del pago"))
                .when(repositorio).insertPayment(anyLong(), anyLong(), any(BigDecimal.class), anyString(), any(Instant.class));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> billing.purchase(accountId, new PurchaseRequest(planId, true)));
        assertEquals("fallo forzado s3 antes del pago", error.getMessage());
        verify(repositorio).insertSubscription(anyLong(), eq(accountId), eq(planId), any(), any());
        verify(repositorio).insertPayment(anyLong(), anyLong(), any(), anyString(), any());

        assertEquals(0, contarSuscripciones(), "oracle debe revertir la suscripcion insertada");
        assertEquals(0, contarPagos(), "tampoco debe quedar un pago parcial");
        System.out.println("s3 ok: fallo antes de pago; rollback real; 0 suscripciones y 0 pagos");
    }

    @Test
    void pagoRechazadoNoEscribeEnOracle() {
        ApiException error = assertThrows(ApiException.class,
                () -> billing.purchase(accountId, new PurchaseRequest(planId, false)));
        assertEquals("PAYMENT_REJECTED", error.code());
        assertEquals(0, contarSuscripciones());
        assertEquals(0, contarPagos());
        System.out.println("s3 ok: pago rechazado; sin escrituras en oracle");
    }

    private int contarSuscripciones() {
        Integer cantidad = jdbc.queryForObject(
                "SELECT COUNT(*) FROM SUBSCRIPTIONS WHERE ACCOUNTS_accountId = ?", Integer.class, accountId);
        return cantidad == null ? 0 : cantidad;
    }

    private int contarPagos() {
        Integer cantidad = jdbc.queryForObject("""
                SELECT COUNT(*) FROM PAYMENTS p
                  JOIN SUBSCRIPTIONS s ON s.subscriptionId = p.SUBSCRIPTIONS_subscriptionId
                 WHERE s.ACCOUNTS_accountId = ?
                """, Integer.class, accountId);
        return cantidad == null ? 0 : cantidad;
    }

    private int contarSuscripcionesSinPago() {
        Integer cantidad = jdbc.queryForObject("""
                SELECT COUNT(*) FROM SUBSCRIPTIONS s
                 WHERE s.ACCOUNTS_accountId = ?
                   AND NOT EXISTS (
                       SELECT 1 FROM PAYMENTS p
                        WHERE p.SUBSCRIPTIONS_subscriptionId = s.subscriptionId
                   )
                """, Integer.class, accountId);
        return cantidad == null ? 0 : cantidad;
    }

    private record Resultado(boolean aprobado, String codigo, Long subscriptionId) { }
}
