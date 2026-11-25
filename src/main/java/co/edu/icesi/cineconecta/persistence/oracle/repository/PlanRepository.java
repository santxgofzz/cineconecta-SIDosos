package co.edu.icesi.cineconecta.persistence.oracle.repository;

import co.edu.icesi.cineconecta.persistence.oracle.entity.PlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<PlanEntity, Long> {
}
