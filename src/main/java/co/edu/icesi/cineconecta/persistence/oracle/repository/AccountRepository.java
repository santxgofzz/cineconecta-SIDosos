package co.edu.icesi.cineconecta.persistence.oracle.repository;

import co.edu.icesi.cineconecta.persistence.oracle.entity.AccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    Optional<AccountEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Bloquea la fila de la cuenta en Oracle (SELECT ... FOR UPDATE) para serializar alta de perfiles. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountEntity a where a.accountId = :accountId")
    Optional<AccountEntity> findByIdForUpdate(@Param("accountId") Long accountId);
}
