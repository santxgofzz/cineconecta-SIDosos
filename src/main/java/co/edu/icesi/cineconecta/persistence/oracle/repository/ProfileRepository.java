package co.edu.icesi.cineconecta.persistence.oracle.repository;

import co.edu.icesi.cineconecta.persistence.oracle.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {
    List<ProfileEntity> findByAccountIdOrderByProfileIdAsc(Long accountId);

    Optional<ProfileEntity> findByProfileIdAndAccountId(Long profileId, Long accountId);

    long countByAccountIdAndIsActive(Long accountId, String isActive);

    boolean existsByAccountIdAndNameIgnoreCase(Long accountId, String name);

    boolean existsByAccountIdAndNameIgnoreCaseAndProfileIdNot(Long accountId, String name, Long profileId);
}
