package co.edu.icesi.cineconecta.integration;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Consulta de solo lectura sobre PROFILES y SUBSCRIPTIONS (modelo relacional
 * entregado). Los nombres físicos de columnas están centralizados aquí: si el
 * script 01_schema.sql del equipo usa otros nombres, solo se ajustan estas
 * sentencias.
 */
@Component
public class JdbcAccountDataGateway implements AccountDataGateway {
    static final String SQL_PROFILE = """
            SELECT PROFILE_ID, ACCOUNT_ID, MAX_AGE_RATING, IS_ACTIVE
              FROM PROFILES
             WHERE PROFILE_ID = ?""";

    /** Inicio inclusivo, fin exclusivo. Si hubiera varias (no debería), la más reciente. */
    static final String SQL_ACTIVE_SUBSCRIPTION = """
            SELECT SUBSCRIPTION_ID
              FROM SUBSCRIPTIONS
             WHERE ACCOUNT_ID = ?
               AND STARTS_AT <= ?
               AND ENDS_AT > ?
             ORDER BY STARTS_AT DESC
             FETCH FIRST 1 ROWS ONLY""";

    private final JdbcTemplate jdbc;

    public JdbcAccountDataGateway(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ProfileAccess> findProfile(long profileId) {
        List<ProfileAccess> rows = jdbc.query(SQL_PROFILE, (rs, n) -> new ProfileAccess(
                rs.getLong("PROFILE_ID"),
                rs.getLong("ACCOUNT_ID"),
                rs.getInt("MAX_AGE_RATING"),
                isActive(rs.getString("IS_ACTIVE"))), profileId);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<Long> findActiveSubscriptionId(long accountId, Instant at) {
        Timestamp ts = Timestamp.from(at);
        List<Long> rows = jdbc.query(SQL_ACTIVE_SUBSCRIPTION, (rs, n) -> rs.getLong(1), accountId, ts, ts);
        return rows.stream().findFirst();
    }

    /** Acepta NUMBER(1) o CHAR(1) ('1', 'Y', 'S', 'T'). */
    static boolean isActive(String raw) {
        if (raw == null) return false;
        String v = raw.trim().toUpperCase();
        return v.equals("1") || v.equals("Y") || v.equals("S") || v.equals("T") || v.equals("TRUE");
    }
}
