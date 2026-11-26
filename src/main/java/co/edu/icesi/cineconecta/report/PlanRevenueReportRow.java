package co.edu.icesi.cineconecta.report;

import java.math.BigDecimal;

public record PlanRevenueReportRow(
        long planId,
        String nombrePlan,
        BigDecimal ingresos,
        long cantidadSuscripciones) {
}
