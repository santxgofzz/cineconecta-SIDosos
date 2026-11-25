package co.edu.icesi.cineconecta.subscription;

import co.edu.icesi.cineconecta.subscription.BillingModels.HistoryRow;
import co.edu.icesi.cineconecta.subscription.BillingModels.Plan;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** consulta las tablas originales del esquema oracle sin recrearlas con jpa. */
@Repository
public class JdbcBillingRepository implements BillingRepository {
    private static final String SQL_PLANS = "SELECT planId, name, price, maxProfiles FROM PLANS ORDER BY planId";
    private static final String SQL_PLAN = "SELECT planId, name, price, maxProfiles FROM PLANS WHERE planId = :planId";
    private static final String SQL_LOCK_ACCOUNT = "SELECT accountId FROM ACCOUNTS WHERE accountId = :accountId FOR UPDATE WAIT 5";
    private static final String SQL_ACTIVE_PROFILES = """
            SELECT COUNT(*) FROM PROFILES
             WHERE ACCOUNTS_accountId = :accountId
               AND UPPER(TRIM(isActive)) IN ('1', 'Y', 'S', 'T')
            """;
    private static final String SQL_OVERLAP = """
            SELECT COUNT(*) FROM SUBSCRIPTIONS
             WHERE ACCOUNTS_accountId = :accountId
               AND startsAt < :endsAt AND endsAt > :startsAt
            """;
    private static final String SQL_INSERT_SUBSCRIPTION = """
            INSERT INTO SUBSCRIPTIONS
                  (subscriptionId, startsAt, endsAt, ACCOUNTS_accountId, PLANS_planId)
            VALUES (:id, :startsAt, :endsAt, :accountId, :planId)
            """;
    private static final String SQL_INSERT_PAYMENT = """
            INSERT INTO PAYMENTS
                  (paymentId, paidAt, amount, reference, SUBSCRIPTIONS_subscriptionId)
            VALUES (:id, :paidAt, :amount, :reference, :subscriptionId)
            """;
    private static final String SQL_HISTORY = """
            SELECT s.subscriptionId AS sub_id,
                   s.startsAt AS start_at,
                   s.endsAt AS end_at,
                   p.planId AS plan_id,
                   p.name AS plan_name,
                   pay.amount AS paid_amount,
                   pay.reference AS payment_reference
              FROM SUBSCRIPTIONS s
              JOIN PLANS p ON p.planId = s.PLANS_planId
              JOIN PAYMENTS pay ON pay.SUBSCRIPTIONS_subscriptionId = s.subscriptionId
             WHERE s.ACCOUNTS_accountId = :accountId
             ORDER BY s.startsAt DESC, s.subscriptionId DESC
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcBillingRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Plan> findPlans() {
        return jdbc.query(SQL_PLANS, (rs, rowNum) -> plan(rs));
    }

    @Override
    public Optional<Plan> findPlan(long planId) {
        var rows = jdbc.query(SQL_PLAN, Map.of("planId", planId), (rs, rowNum) -> plan(rs));
        return rows.stream().findFirst();
    }

    @Override
    public boolean lockAccount(long accountId) {
        // este bloqueo debe permanecer hasta el commit o rollback de la transaccion.
        return !jdbc.query(SQL_LOCK_ACCOUNT, Map.of("accountId", accountId),
                (rs, rowNum) -> rs.getLong(1)).isEmpty();
    }

    @Override
    public int countActiveProfiles(long accountId) {
        Integer count = jdbc.queryForObject(SQL_ACTIVE_PROFILES, Map.of("accountId", accountId), Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public boolean hasOverlappingSubscription(long accountId, Instant startsAt, Instant endsAt) {
        var params = new MapSqlParameterSource()
                .addValue("accountId", accountId)
                .addValue("startsAt", asUtcTimestamp(startsAt))
                .addValue("endsAt", asUtcTimestamp(endsAt));
        Integer count = jdbc.queryForObject(SQL_OVERLAP, params, Integer.class);
        return count != null && count > 0;
    }

    @Override
    public long nextSubscriptionId() {
        Long id = jdbc.getJdbcTemplate().queryForObject("SELECT CC_SUBSCRIPTIONS_SEQ.NEXTVAL FROM DUAL", Long.class);
        if (id == null) throw new IllegalStateException("No se pudo reservar el id de suscripcion");
        return id;
    }

    @Override
    public long nextPaymentId() {
        Long id = jdbc.getJdbcTemplate().queryForObject("SELECT CC_PAYMENTS_SEQ.NEXTVAL FROM DUAL", Long.class);
        if (id == null) throw new IllegalStateException("No se pudo reservar el id de pago");
        return id;
    }

    @Override
    public void insertSubscription(long id, long accountId, long planId, Instant start, Instant end) {
        var params = new MapSqlParameterSource()
                .addValue("id", id).addValue("accountId", accountId).addValue("planId", planId)
                .addValue("startsAt", asUtcTimestamp(start)).addValue("endsAt", asUtcTimestamp(end));
        requireOneRow(jdbc.update(SQL_INSERT_SUBSCRIPTION, params), "suscripcion");
    }

    @Override
    public void insertPayment(long id, long subscriptionId, BigDecimal amount, String reference, Instant paidAt) {
        var params = new MapSqlParameterSource()
                .addValue("id", id).addValue("subscriptionId", subscriptionId)
                .addValue("paidAt", asUtcTimestamp(paidAt))
                .addValue("amount", amount).addValue("reference", reference);
        requireOneRow(jdbc.update(SQL_INSERT_PAYMENT, params), "pago");
    }

    @Override
    public List<HistoryRow> findHistory(long accountId) {
        return jdbc.query(SQL_HISTORY, Map.of("accountId", accountId), (rs, rowNum) ->
                new HistoryRow(rs.getLong("sub_id"), rs.getLong("plan_id"), rs.getString("plan_name"),
                        rs.getBigDecimal("paid_amount"), rs.getString("payment_reference"),
                        fromUtcTimestamp(rs, "start_at"), fromUtcTimestamp(rs, "end_at")));
    }

    private static Plan plan(ResultSet rs) throws SQLException {
        return new Plan(rs.getLong("planId"), rs.getString("name"),
                rs.getBigDecimal("price"), rs.getInt("maxProfiles"));
    }

    // oracle guarda timestamp sin zona; por acuerdo todas las fechas del modulo van en utc.
    static Timestamp asUtcTimestamp(Instant instant) {
        return Timestamp.valueOf(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
    }

    private static Instant fromUtcTimestamp(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value.toLocalDateTime().toInstant(ZoneOffset.UTC);
    }

    private static void requireOneRow(int count, String operation) {
        if (count != 1) throw new IllegalStateException("No se confirmo la escritura de " + operation);
    }
}
