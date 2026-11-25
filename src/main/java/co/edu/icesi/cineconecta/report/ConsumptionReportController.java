package co.edu.icesi.cineconecta.report;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Controller
public class ConsumptionReportController {
    private final ConsumptionReportService service;

    public ConsumptionReportController(ConsumptionReportService service) {
        this.service = service;
    }

    @GetMapping("/admin/reportes/consumo-por-plan")
    public String consumoPorPlan(
            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);

        if (desde == null && hasta == null) {
            return "report/consumo-por-plan";
        }
        if (desde == null || hasta == null) {
            model.addAttribute("error", "Selecciona las dos fechas para generar el informe.");
        } else if (hasta.isBefore(desde)) {
            model.addAttribute("error", "La fecha final debe ser igual o posterior a la inicial.");
        } else {
            model.addAttribute("informe", service.generate(desde, hasta));
        }

        return "report/consumo-por-plan";
    }

    @GetMapping("/admin/reportes/consumo-por-plan.csv")
    public ResponseEntity<byte[]> descargarCsv(
            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new ResponseStatusException(BAD_REQUEST, "Selecciona un rango de fechas válido.");
        }

        ConsumptionReport report = service.generate(desde, hasta);
        byte[] body = toCsv(report).getBytes(StandardCharsets.UTF_8);
        String fileName = "consumo-por-plan-" + desde + "-" + hasta + ".csv";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(body);
    }

    private static String toCsv(ConsumptionReport report) {
        StringBuilder csv = new StringBuilder("\uFEFFPlan,Tipo de contenido,Sesiones,Segundos consumidos,Perfiles únicos\r\n");
        for (ConsumptionReportRow row : report.filas()) {
            csv.append(csvCell(row.nombrePlan())).append(',')
                    .append(csvCell(row.tipoContenido())).append(',')
                    .append(row.sesiones()).append(',')
                    .append(row.segundosConsumidos()).append(',')
                    .append(row.perfilesUnicos()).append("\r\n");
        }
        csv.append(csvCell("Total")).append(",,")
                .append(report.sesionesTotales()).append(',')
                .append(report.segundosTotales()).append(",\r\n");
        return csv.toString();
    }

    private static String csvCell(String value) {
        String safeValue = value == null ? "" : value;
        int firstNonWhitespace = 0;
        while (firstNonWhitespace < safeValue.length()
                && Character.isWhitespace(safeValue.charAt(firstNonWhitespace))) {
            firstNonWhitespace++;
        }
        if (firstNonWhitespace < safeValue.length()
                && "=+-@".indexOf(safeValue.charAt(firstNonWhitespace)) >= 0) {
            safeValue = "'" + safeValue;
        }
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
