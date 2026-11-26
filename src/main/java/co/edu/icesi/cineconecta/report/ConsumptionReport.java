package co.edu.icesi.cineconecta.report;

import java.util.List;

public record ConsumptionReport(
        List<ConsumptionReportRow> filas,
        long sesionesTotales,
        long segundosTotales) {

    public String tiempoTotal() {
        return ConsumptionReportRow.formatDuration(segundosTotales);
    }
}
