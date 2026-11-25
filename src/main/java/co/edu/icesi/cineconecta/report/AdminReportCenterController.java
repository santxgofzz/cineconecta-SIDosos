package co.edu.icesi.cineconecta.report;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminReportCenterController {
    @GetMapping("/admin/reportes")
    public String informes() {
        return "admin/reportes";
    }
}
