package co.edu.icesi.cineconecta.subscription;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.subscription.BillingModels.Plan;
import co.edu.icesi.cineconecta.subscription.BillingModels.PurchaseReceipt;
import co.edu.icesi.cineconecta.subscription.BillingModels.PurchaseRequest;
import co.edu.icesi.cineconecta.subscription.BillingModels.SubscriptionHistory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class BillingService {
    private static final long SUBSCRIPTION_DAYS = 30;
    private final BillingRepository repository;
    private final Clock clock;

    public BillingService(BillingRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Plan> plans() {
        return repository.findPlans();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionHistory> history(long accountId) {
        Instant now = clock.instant();
        return repository.findHistory(accountId).stream().map(row ->
                new SubscriptionHistory(row.subscriptionId(), row.planId(), row.planName(),
                        row.paidAmount(), row.paymentReference(), row.startsAt(), row.endsAt(),
                        SubscriptionWindow.isActive(row.startsAt(), row.endsAt(), now))).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseReceipt purchase(long accountId, PurchaseRequest request) {
        if (accountId < 1 || request == null || request.planId() == null || request.planId() < 1
                || request.approvePayment() == null) {
            throw ApiException.badRequest("PURCHASE_INVALID", "Datos de contratacion invalidos.");
        }
        // si el pago simulado se rechaza, no escribimos nada en oracle.
        if (!request.approvePayment()) {
            throw ApiException.badRequest("PAYMENT_REJECTED", "El pago simulado fue rechazado.");
        }

        // bloquear la cuenta serializa las compras que usan este servicio para el mismo titular.
        if (!repository.lockAccount(accountId)) {
            throw ApiException.notFound("ACCOUNT_NOT_FOUND", "La cuenta autenticada no existe.");
        }
        Plan plan = repository.findPlan(request.planId()).orElseThrow(() ->
                ApiException.notFound("PLAN_NOT_FOUND", "El plan seleccionado no existe."));
        if (plan.price() == null || plan.price().compareTo(BigDecimal.ZERO) < 0 || plan.maxProfiles() < 1) {
            throw new IllegalStateException("El plan tiene datos invalidos en oracle");
        }

        Instant startsAt = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        Instant endsAt = startsAt.plus(SUBSCRIPTION_DAYS, ChronoUnit.DAYS);
        if (repository.hasOverlappingSubscription(accountId, startsAt, endsAt)) {
            throw ApiException.conflict("SUBSCRIPTION_OVERLAP", "Ya hay una suscripcion que se cruza con este periodo.");
        }
        if (repository.countActiveProfiles(accountId) > plan.maxProfiles()) {
            throw ApiException.conflict("PROFILE_LIMIT", "Desactiva los perfiles excedentes antes de contratar este plan.");
        }

        long subscriptionId = repository.nextSubscriptionId();
        long paymentId = repository.nextPaymentId();
        String reference = "SIM-" + UUID.randomUUID();

        // las dos escrituras pertenecen a una unica transaccion relacional.
        // si el pago falla, spring revierte tambien la suscripcion.
        repository.insertSubscription(subscriptionId, accountId, plan.planId(), startsAt, endsAt);
        repository.insertPayment(paymentId, subscriptionId, plan.price(), reference, startsAt);

        return new PurchaseReceipt(subscriptionId, plan.planId(), plan.name(),
                plan.price(), reference, startsAt, endsAt);
    }
}
