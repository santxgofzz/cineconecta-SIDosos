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
            SELECT PROFILEID, ACCOUNTS_ACCOUNTID, MAXAGERATING, ISACTIVE
              FROM PROFILES
             WHERE PROFILEID = ?""";

    /** Inicio inclusivo, fin exclusivo. Si hubiera varias (no debería), la más reciente. */
    static final String SQL_ACTIVE_SUBSCRIPTION = """
            SELECT SUBSCRIPTIONID
              FROM SUBSCRIPTIONS
             WHERE ACCOUNTS_ACCOUNTID = ?
               AND STARTSAT <= ?
               AND ENDSAT > ?
             ORDER BY STARTSAT DESC
             FETCH FIRST 1 ROWS ONLY""";

    private final JdbcTemplate jdbc;

    public JdbcAccountDataGateway(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ProfileAccess> findProfile(long profileId) {
        List<ProfileAccess> rows = jdbc.query(SQL_PROFILE, (rs, n) -> new ProfileAccess(
                rs.getLong("PROFILEID"),
                rs.getLong("ACCOUNTS_ACCOUNTID"),
                rs.getInt("MAXAGERATING"),
                isActive(rs.getString("ISACTIVE"))), profileId);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<Long> findActiveSubscriptionId(long accountId, Instant at) {
        Timestamp ts = Timestamp.from(at);
        List<Long> rows = jdbc.query(SQL_ACTIVE_SUBSCRIPTION, (rs, n) -> rs.getLong(1), accountId, ts, ts);
        return rows.stream().findFirst();
    }

    /** Acepta código de estado 'A' (DM_PROFILE_STATES) o banderas ('1', 'Y', 'S', 'T'). */
    static boolean isActive(String raw) {
        if (raw == null) return false;
        String v = raw.trim().toUpperCase();
        return v.equals("A") || v.equals("1") || v.equals("Y") || v.equals("S") || v.equals("T") || v.equals("TRUE");
    }
}
