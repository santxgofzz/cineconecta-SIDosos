package co.edu.icesi.cineconecta.persistence.oracle.repository;

import co.edu.icesi.cineconecta.persistence.oracle.entity.ProfileStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileStateRepository extends JpaRepository<ProfileStateEntity, String> {
}
