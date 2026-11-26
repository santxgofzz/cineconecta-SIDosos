package co.edu.icesi.cineconecta.report;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class PlanRevenueReportService {
    private final PlanRevenueReportRepository repository;

    public PlanRevenueReportService(PlanRevenueReportRepository repository) {
        this.repository = repository;
    }

    public List<PlanRevenueReportRow> generate(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalArgumentException("El rango de fechas no es válido.");
        }

        return repository.findByPeriod(desde, hasta.plusDays(1));
    }
}
