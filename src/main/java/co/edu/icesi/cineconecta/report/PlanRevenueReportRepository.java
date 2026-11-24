package co.edu.icesi.cineconecta.report;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PlanRevenueReportRepository {
    private static final String REPORT_SQL = """
            WITH parametros AS (
                SELECT CAST(:fecha_inicio AS TIMESTAMP) AS fecha_inicio,
                       CAST(:fecha_fin AS TIMESTAMP) AS fecha_fin
                FROM dual
            ),
            suscripciones_periodo AS (
                SELECT s.PLANS_planId AS planId,
                       COUNT(*) AS cantidad_suscripciones
                FROM SUBSCRIPTIONS s
                CROSS JOIN parametros p
                WHERE s.startsAt >= p.fecha_inicio
                  AND s.startsAt <  p.fecha_fin
                GROUP BY s.PLANS_planId
            ),
            ingresos_periodo AS (
                SELECT s.PLANS_planId AS planId,
                       SUM(pg.amount) AS ingresos
                FROM PAYMENTS pg
                JOIN SUBSCRIPTIONS s
                  ON s.subscriptionId = pg.SUBSCRIPTIONS_subscriptionId
                CROSS JOIN parametros p
                WHERE pg.paidAt >= p.fecha_inicio
                  AND pg.paidAt <  p.fecha_fin
                GROUP BY s.PLANS_planId
            )
            SELECT pl.planId AS plan_id,
                   pl.name AS nombre_plan,
                   NVL(ip.ingresos, 0) AS ingresos,
                   NVL(sp.cantidad_suscripciones, 0) AS cantidad_suscripciones
            FROM PLANS pl
            LEFT JOIN ingresos_periodo ip
              ON ip.planId = pl.planId
            LEFT JOIN suscripciones_periodo sp
              ON sp.planId = pl.planId
            ORDER BY pl.planId
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PlanRevenueReportRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PlanRevenueReportRow> findByPeriod(LocalDate desde, LocalDate hastaExclusiva) {
        var parameters = new MapSqlParameterSource()
                .addValue("fecha_inicio", Timestamp.valueOf(desde.atStartOfDay()), Types.TIMESTAMP)
                .addValue("fecha_fin", Timestamp.valueOf(hastaExclusiva.atStartOfDay()), Types.TIMESTAMP);

        return jdbcTemplate.query(REPORT_SQL, parameters, (resultSet, rowNumber) ->
                new PlanRevenueReportRow(
                        resultSet.getLong("plan_id"),
                        resultSet.getString("nombre_plan"),
                        resultSet.getBigDecimal("ingresos"),
                        resultSet.getLong("cantidad_suscripciones")));
    }
}
