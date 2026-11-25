package co.edu.icesi.cineconecta.report;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class HistoricalPlanReportRepository {
    private static final String SQL = """
            SELECT s.subscriptionId AS subscription_id,
                   p.planId AS plan_id,
                   p.name AS plan_name
            FROM SUBSCRIPTIONS s
            JOIN PLANS p
              ON p.planId = s.PLANS_planId
            WHERE s.subscriptionId IN (:subscription_ids)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public HistoricalPlanReportRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<HistoricalSubscriptionPlan> findPlans(Collection<Long> subscriptionIds) {
        if (subscriptionIds == null || subscriptionIds.isEmpty()) {
            return List.of();
        }
        var parameters = new MapSqlParameterSource().addValue("subscription_ids", subscriptionIds);
        return jdbc.query(SQL, parameters, (resultSet, rowNumber) -> new HistoricalSubscriptionPlan(
                resultSet.getLong("subscription_id"),
                resultSet.getLong("plan_id"),
                resultSet.getString("plan_name")));
    }
}
