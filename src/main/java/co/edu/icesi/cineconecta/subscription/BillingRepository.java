package co.edu.icesi.cineconecta.subscription;

import co.edu.icesi.cineconecta.subscription.BillingModels.HistoryRow;
import co.edu.icesi.cineconecta.subscription.BillingModels.Plan;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** operaciones de oracle necesarias para facturacion; se puede simular en las pruebas. */
public interface BillingRepository {
    List<Plan> findPlans();
    Optional<Plan> findPlan(long planId);
    boolean lockAccount(long accountId);
    int countActiveProfiles(long accountId);
    boolean hasOverlappingSubscription(long accountId, Instant startsAt, Instant endsAt);
    long nextSubscriptionId();
    long nextPaymentId();
    void insertSubscription(long subscriptionId, long accountId, long planId, Instant startsAt, Instant endsAt);
    void insertPayment(long paymentId, long subscriptionId, BigDecimal amount, String reference, Instant paidAt);
    List<HistoryRow> findHistory(long accountId);
}
