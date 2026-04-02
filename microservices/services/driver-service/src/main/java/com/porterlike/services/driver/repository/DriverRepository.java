package com.porterlike.services.driver.repository;

import com.porterlike.services.driver.model.Driver;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverRepository extends JpaRepository<Driver, UUID> {

    List<Driver> findTop20ByOnlineTrueAndAvailableTrueAndVehicleTypeOrderByUpdatedAtDesc(String vehicleType);

    Optional<Driver> findByAccountId(UUID accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Driver d where d.id = :id")
    Optional<Driver> findByIdForUpdate(@Param("id") UUID id);
}
