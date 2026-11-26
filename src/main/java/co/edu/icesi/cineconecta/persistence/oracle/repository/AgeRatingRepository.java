package co.edu.icesi.cineconecta.persistence.oracle.repository;

import co.edu.icesi.cineconecta.persistence.oracle.entity.AgeRatingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgeRatingRepository extends JpaRepository<AgeRatingEntity, Integer> {
    List<AgeRatingEntity> findAllByOrderByAgeValueAsc();
}
