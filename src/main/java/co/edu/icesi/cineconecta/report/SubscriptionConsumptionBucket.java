package co.edu.icesi.cineconecta.report;

import java.util.Set;

record SubscriptionConsumptionBucket(
        long subscriptionId,
        String contentType,
        long sessionCount,
        long watchedSeconds,
        Set<Long> profileIds) {
}
