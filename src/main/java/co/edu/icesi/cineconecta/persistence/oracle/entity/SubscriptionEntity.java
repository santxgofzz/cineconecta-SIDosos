package co.edu.icesi.cineconecta.persistence.oracle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Período de suscripción asociado a una cuenta y un plan en SUBSCRIPTIONS. */
@Entity
@Table(name = "SUBSCRIPTIONS")
public class SubscriptionEntity {
    @Id
    @Column(name = "SUBSCRIPTIONID", nullable = false)
    private Long subscriptionId;

    @Column(name = "STARTSAT", nullable = false)
    private Instant startsAt;

    @Column(name = "ENDSAT", nullable = false)
    private Instant endsAt;

    @Column(name = "ACCOUNTS_ACCOUNTID", nullable = false)
    private Long accountId;

    @Column(name = "PLANS_PLANID", nullable = false)
    private Long planId;

    protected SubscriptionEntity() {}

    public SubscriptionEntity(Long subscriptionId, Instant startsAt, Instant endsAt, Long accountId, Long planId) {
        this.subscriptionId = subscriptionId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.accountId = accountId;
        this.planId = planId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getPlanId() {
        return planId;
    }
}
