package co.edu.icesi.cineconecta.report;

public record ConsumptionReportRow(
        Long planId,
        String nombrePlan,
        String tipoContenido,
        long sesiones,
        long segundosConsumidos,
        long perfilesUnicos) {

    public String tiempoConsumido() {
        return formatDuration(segundosConsumidos);
    }

    static String formatDuration(long totalSeconds) {
        long safeSeconds = Math.max(0L, totalSeconds);
        long hours = safeSeconds / 3600;
        long minutes = (safeSeconds % 3600) / 60;
        long seconds = safeSeconds % 60;
        return "%02d:%02d:%02d".formatted(hours, minutes, seconds);
    }
}
