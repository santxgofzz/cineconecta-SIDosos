package co.edu.icesi.cineconecta.persistence.oracle.repository;

import co.edu.icesi.cineconecta.persistence.oracle.entity.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {
    /** Suscripción vigente en el instante dado: inicio inclusivo y fin exclusivo [startsAt, endsAt). */
    @Query("""
            select s from SubscriptionEntity s
             where s.accountId = :accountId
               and s.startsAt <= :at
               and s.endsAt > :at
             order by s.startsAt desc""")
    List<SubscriptionEntity> findActiveSubscriptions(@Param("accountId") Long accountId, @Param("at") Instant at);
}
