package com.porterlike.services.driver.repository;

import com.porterlike.services.driver.model.DriverOffer;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverOfferRepository extends JpaRepository<DriverOffer, UUID> {

    Optional<DriverOffer> findByExternalOfferId(String externalOfferId);

    Optional<DriverOffer> findByDriverIdAndBookingId(UUID driverId, UUID bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from DriverOffer o where o.externalOfferId = :offerId")
    Optional<DriverOffer> findByExternalOfferIdForUpdate(@Param("offerId") String offerId);
}
