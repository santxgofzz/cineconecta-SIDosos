package co.edu.icesi.cineconecta.subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingJdbcRepositoryContractTest {
    @Mock NamedParameterJdbcTemplate jdbc;
    @Mock JdbcTemplate jdbcBase;
    private JdbcBillingRepository repositorio;

    @BeforeEach
    void preparar() {
        repositorio = new JdbcBillingRepository(jdbc);
    }

    @Test
    void comparaIntervalosAbiertosEnElFinalParaNoBloquearRenovacionesContiguas() {
        Instant inicio = Instant.parse("2026-11-25T15:00:00Z");
        Instant fin = Instant.parse("2026-12-25T15:00:00Z");
        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(0);
        assertFalse(repositorio.hasOverlappingSubscription(17L, inicio, fin));

        ArgumentCaptor<String> consulta = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> parametros = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForObject(consulta.capture(), parametros.capture(), eq(Integer.class));
        assertTrue(consulta.getValue().contains("startsAt < :endsAt"));
        assertTrue(consulta.getValue().contains("endsAt > :startsAt"));
        assertEquals(17L, parametros.getValue().getValue("accountId"));
        assertEquals(JdbcBillingRepository.asUtcTimestamp(inicio), parametros.getValue().getValue("startsAt"));
        assertEquals(JdbcBillingRepository.asUtcTimestamp(fin), parametros.getValue().getValue("endsAt"));
    }

    @Test
    void consultaDeSolapamientoDetectaResultadosPositivos() {
        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(1);
        assertTrue(repositorio.hasOverlappingSubscription(17L, Instant.EPOCH, Instant.EPOCH.plusSeconds(5)));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void bloqueoDeCuentaUsaForUpdateYEsperaFinita() {
        doReturn(List.of(17L)).when(jdbc).query(anyString(), any(Map.class), any(RowMapper.class));
        assertTrue(repositorio.lockAccount(17L));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), eq(Map.of("accountId", 17L)), any(RowMapper.class));
        assertTrue(sql.getValue().toUpperCase().contains("FOR UPDATE WAIT 20"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void bloqueoDeCuentaInexistenteNoSeConsideraExitoso() {
        doReturn(List.of()).when(jdbc).query(anyString(), any(Map.class), any(RowMapper.class));
        assertFalse(repositorio.lockAccount(999L));
    }

    @Test
    void insertaUnaSuscripcionConCuentaYPlanCorrectos() {
        when(jdbc.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
        Instant inicio = Instant.parse("2026-11-25T15:00:00Z");
        Instant fin = inicio.plusSeconds(2_592_000);
        repositorio.insertSubscription(120L, 17L, 3L, inicio, fin);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> valores = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(sql.capture(), valores.capture());
        assertTrue(sql.getValue().contains("INSERT INTO SUBSCRIPTIONS"));
        assertEquals(120L, valores.getValue().getValue("id"));
        assertEquals(17L, valores.getValue().getValue("accountId"));
        assertEquals(3L, valores.getValue().getValue("planId"));
        assertEquals(JdbcBillingRepository.asUtcTimestamp(inicio), valores.getValue().getValue("startsAt"));
        assertEquals(JdbcBillingRepository.asUtcTimestamp(fin), valores.getValue().getValue("endsAt"));
    }

    @Test
    void insertaPagoConPrecioYReferenciaDelServidor() {
        when(jdbc.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
        Instant instante = Instant.parse("2026-11-25T15:00:00Z");
        BigDecimal monto = new BigDecimal("23000.00");
        repositorio.insertPayment(245L, 120L, monto, "SIM-EVIDENCIA", instante);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> valores = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(sql.capture(), valores.capture());
        assertTrue(sql.getValue().contains("INSERT INTO PAYMENTS"));
        assertEquals(245L, valores.getValue().getValue("id"));
        assertEquals(120L, valores.getValue().getValue("subscriptionId"));
        assertEquals(monto, valores.getValue().getValue("amount"));
        assertEquals("SIM-EVIDENCIA", valores.getValue().getValue("reference"));
        assertEquals(JdbcBillingRepository.asUtcTimestamp(instante), valores.getValue().getValue("paidAt"));
    }

    @Test
    void ceroFilasInsertadasSeConsideraError() {
        when(jdbc.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(0);
        Instant instante = Instant.parse("2026-11-25T15:00:00Z");
        assertThrows(IllegalStateException.class,
                () -> repositorio.insertPayment(245L, 120L, BigDecimal.ONE, "SIM", instante));
    }

    @Test
    void generaIdsDesdeSecuenciasOracleSinCalcularMaximoManual() {
        when(jdbc.getJdbcTemplate()).thenReturn(jdbcBase);
        when(jdbcBase.queryForObject("SELECT CC_SUBSCRIPTIONS_SEQ.NEXTVAL FROM DUAL", Long.class))
                .thenReturn(120L);
        when(jdbcBase.queryForObject("SELECT CC_PAYMENTS_SEQ.NEXTVAL FROM DUAL", Long.class))
                .thenReturn(245L);
        assertEquals(120L, repositorio.nextSubscriptionId());
        assertEquals(245L, repositorio.nextPaymentId());
    }

    @Test
    void siLaSecuenciaNoDevuelveIdSeDetieneLaOperacion() {
        when(jdbc.getJdbcTemplate()).thenReturn(jdbcBase);
        when(jdbcBase.queryForObject("SELECT CC_SUBSCRIPTIONS_SEQ.NEXTVAL FROM DUAL", Long.class))
                .thenReturn(null);
        assertThrows(IllegalStateException.class, repositorio::nextSubscriptionId);
    }

    @Test
    void cuentaPerfilesActivosIncluyendoEstadoActivoA() {
        when(jdbc.queryForObject(anyString(), eq(Map.of("accountId", 17L)), eq(Integer.class)))
                .thenReturn(2);

        assertEquals(2, repositorio.countActiveProfiles(17L));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForObject(sql.capture(), eq(Map.of("accountId", 17L)), eq(Integer.class));
        assertTrue(sql.getValue().contains("'A'"));
    }

    @Test
    void convierteFechasAlTimestampUtcSinUsarHoraLocalDelComputador() {
        Instant instante = Instant.parse("2026-11-25T15:08:02.123Z");
        Timestamp convertido = JdbcBillingRepository.asUtcTimestamp(instante);
        assertEquals(LocalDateTime.parse("2026-11-25T15:08:02.123"), convertido.toLocalDateTime());
    }
}
