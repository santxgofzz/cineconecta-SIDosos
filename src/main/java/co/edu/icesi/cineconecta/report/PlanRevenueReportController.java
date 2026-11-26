package co.edu.icesi.cineconecta.report;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Controller
public class PlanRevenueReportController {
    private final PlanRevenueReportService service;

    public PlanRevenueReportController(PlanRevenueReportService service) {
        this.service = service;
    }

    /** Mantiene el enlace previo sin exponer el contenido del informe. */
    @GetMapping("/reportes/ingresos-por-plan")
    public String redirigirRutaAnterior() {
        return "redirect:/admin/reportes/ingresos-por-plan";
    }

    @GetMapping("/admin/reportes/ingresos-por-plan")
    public String ingresosPorPlan(
            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);

        if (desde == null && hasta == null) {
            return "report/ingresos-por-plan";
        }
        if (desde == null || hasta == null) {
            model.addAttribute("error", "Selecciona las dos fechas para generar el informe.");
        } else if (hasta.isBefore(desde)) {
            model.addAttribute("error", "La fecha final debe ser igual o posterior a la inicial.");
        } else {
            model.addAttribute("resultados", service.generate(desde, hasta));
        }

        return "report/ingresos-por-plan";
    }

    @GetMapping("/admin/reportes/ingresos-por-plan.csv")
    public ResponseEntity<byte[]> descargarCsv(
            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new ResponseStatusException(BAD_REQUEST, "Selecciona un rango de fechas válido.");
        }

        List<PlanRevenueReportRow> resultados = service.generate(desde, hasta);
        List<String> rows = new ArrayList<>();
        rows.add(ReportCsvExport.row("Plan", "Ingresos", "Suscripciones iniciadas"));
        BigDecimal totalIngresos = BigDecimal.ZERO;
        long totalSuscripciones = 0;
        for (PlanRevenueReportRow resultado : resultados) {
            rows.add(ReportCsvExport.row(
                    resultado.nombrePlan(), resultado.ingresos(), resultado.cantidadSuscripciones()));
            totalIngresos = totalIngresos.add(resultado.ingresos());
            totalSuscripciones += resultado.cantidadSuscripciones();
        }
        rows.add(ReportCsvExport.row("Total", totalIngresos, totalSuscripciones));

        byte[] body = ReportCsvExport.document(rows).getBytes(StandardCharsets.UTF_8);
        String fileName = "ingresos-por-plan-" + desde + "-" + hasta + ".csv";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(body);
    }
}
