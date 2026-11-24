package co.edu.icesi.cineconecta.report;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PlanRevenueReportController {
    private final PlanRevenueReportService service;

    public PlanRevenueReportController(PlanRevenueReportService service) {
        this.service = service;
    }

    @GetMapping("/reportes/ingresos-por-plan")
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
}
