package co.edu.icesi.cineconecta.subscription;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

/** modelos de entrada y salida del modulo de suscripciones. */
public final class BillingModels {
    private BillingModels() {}

    public record Plan(long planId, String name, BigDecimal price, int maxProfiles) {}

    // el pago es simulado: la decision se usa solo para la practica de sid2.
    public record PurchaseRequest(@NotNull @Positive Long planId,
                                  @NotNull Boolean approvePayment) {}

    public record PurchaseReceipt(long subscriptionId, long planId, String planName,
                                  BigDecimal paidAmount, String paymentReference,
                                  Instant startsAt, Instant endsAt) {}

    public record HistoryRow(long subscriptionId, long planId, String planName,
                             BigDecimal paidAmount, String paymentReference,
                             Instant startsAt, Instant endsAt) {}

    public record SubscriptionHistory(long subscriptionId, long planId, String planName,
                                      BigDecimal paidAmount, String paymentReference,
                                      Instant startsAt, Instant endsAt, boolean active) {}
}
