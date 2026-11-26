package co.edu.icesi.cineconecta.report;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ConsumptionReportService {
    // Oracle IN lists are chunked below the 1,000-expression limit.
    private static final int ORACLE_ID_BATCH_SIZE = 900;

    private final MongoConsumptionReportRepository mongo;
    private final HistoricalPlanReportRepository oracle;
    private final ZoneId reportZone;

    public ConsumptionReportService(
            MongoConsumptionReportRepository mongo,
            HistoricalPlanReportRepository oracle,
            @Value("${cineconecta.report.time-zone:America/Bogota}") String reportTimeZone) {
        this.mongo = mongo;
        this.oracle = oracle;
        this.reportZone = ZoneId.of(reportTimeZone);
    }

    /**
     * Fecha por inicio de sesión en la zona configurada. El plan sale de la
     * suscripción histórica identificada en Mongo, no del plan vigente hoy.
     */
    public ConsumptionReport generate(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalArgumentException("El rango de fechas no es válido.");
        }

        var desdeIncluido = desde.atStartOfDay(reportZone).toInstant();
        var hastaExclusivo = hasta.plusDays(1).atStartOfDay(reportZone).toInstant();
        List<SubscriptionConsumptionBucket> buckets = mongo.summarizeBySubscriptionAndType(
                desdeIncluido, hastaExclusivo);
        if (buckets.isEmpty()) {
            return new ConsumptionReport(List.of(), 0, 0);
        }

        List<Long> subscriptionIds = buckets.stream()
                .map(SubscriptionConsumptionBucket::subscriptionId)
                .distinct()
                .sorted()
                .toList();
        Map<Long, HistoricalSubscriptionPlan> historicalPlans = findPlansInBatches(subscriptionIds);

        Map<ReportGroupKey, MutableConsumption> grouped = new HashMap<>();
        for (SubscriptionConsumptionBucket bucket : buckets) {
            HistoricalSubscriptionPlan plan = historicalPlans.get(bucket.subscriptionId());
            ReportGroupKey key = plan == null
                    ? new ReportGroupKey(null, bucket.subscriptionId(), bucket.contentType())
                    : new ReportGroupKey(plan.planId(), null, bucket.contentType());
            MutableConsumption total = grouped.computeIfAbsent(key, ignored -> new MutableConsumption());
            total.sessions += bucket.sessionCount();
            total.watchedSeconds += bucket.watchedSeconds();
            total.profileIds.addAll(bucket.profileIds());
        }

        Map<Long, String> planNames = new HashMap<>();
        historicalPlans.values().forEach(plan -> planNames.put(plan.planId(), plan.planName()));
        List<ConsumptionReportRow> rows = grouped.entrySet().stream()
                .map(entry -> toRow(entry.getKey(), entry.getValue(), planNames))
                .sorted(Comparator.comparing(ConsumptionReportRow::nombrePlan, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ConsumptionReportRow::tipoContenido))
                .toList();

        long totalSessions = rows.stream().mapToLong(ConsumptionReportRow::sesiones).sum();
        long totalSeconds = rows.stream().mapToLong(ConsumptionReportRow::segundosConsumidos).sum();
        return new ConsumptionReport(rows, totalSessions, totalSeconds);
    }

    private Map<Long, HistoricalSubscriptionPlan> findPlansInBatches(List<Long> subscriptionIds) {
        Map<Long, HistoricalSubscriptionPlan> plans = new HashMap<>();
        for (int offset = 0; offset < subscriptionIds.size(); offset += ORACLE_ID_BATCH_SIZE) {
            int end = Math.min(offset + ORACLE_ID_BATCH_SIZE, subscriptionIds.size());
            oracle.findPlans(subscriptionIds.subList(offset, end))
                    .forEach(plan -> plans.put(plan.subscriptionId(), plan));
        }
        return plans;
    }

    private static ConsumptionReportRow toRow(
            ReportGroupKey key, MutableConsumption total, Map<Long, String> planNames) {
        String planName = key.planId() == null
                ? "Sin plan histórico (suscripción " + key.missingSubscriptionId() + ")"
                : planNames.getOrDefault(key.planId(), "Plan " + key.planId());
        return new ConsumptionReportRow(
                key.planId(), planName, typeLabel(key.contentType()), total.sessions,
                total.watchedSeconds, total.profileIds.size());
    }

    private static String typeLabel(String contentType) {
        if (contentType == null) {
            return "Sin tipo";
        }
        return switch (contentType) {
            case "movie" -> "Película";
            case "series" -> "Serie";
            case "documentary" -> "Documental";
            default -> "Tipo desconocido (" + contentType + ")";
        };
    }

    private record ReportGroupKey(Long planId, Long missingSubscriptionId, String contentType) {}

    private static final class MutableConsumption {
        private long sessions;
        private long watchedSeconds;
        private final Set<Long> profileIds = new HashSet<>();
    }
}
