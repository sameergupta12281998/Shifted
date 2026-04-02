package com.porterlike.services.booking.repository;

import com.porterlike.services.booking.model.BookingDriverOffer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingDriverOfferRepository extends JpaRepository<BookingDriverOffer, UUID> {

    Optional<BookingDriverOffer> findTopByBookingIdOrderByOfferedAtDesc(UUID bookingId);

    boolean existsByBookingIdAndDriverId(UUID bookingId, UUID driverId);
}
