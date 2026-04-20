package com.porterlike.services.analytics.repository;

import com.porterlike.services.analytics.model.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DomainEventRepository extends JpaRepository<DomainEvent, UUID> {

    long countByEventType(String eventType);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM DomainEvent e WHERE e.eventType = :type AND e.receivedAt >= :since")
    BigDecimal sumAmountByEventTypeSince(@Param("type") String type, @Param("since") Instant since);

    @Query("SELECT COUNT(e) FROM DomainEvent e WHERE e.eventType = :type AND e.receivedAt >= :since")
    long countByEventTypeSince(@Param("type") String type, @Param("since") Instant since);

    @Query("SELECT COUNT(DISTINCT e.driverId) FROM DomainEvent e WHERE e.eventType = 'driver.location.updated' AND e.receivedAt >= :since")
    long countActiveDriversSince(@Param("since") Instant since);
}
