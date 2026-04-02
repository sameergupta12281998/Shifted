package com.porterlike.services.booking.repository;

import com.porterlike.services.booking.model.Booking;
import com.porterlike.services.booking.model.BookingStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Booking> findByStatusOrderByCreatedAtAsc(BookingStatus status);

    Optional<Booking> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") UUID id);

    @Query("select b.id from Booking b where b.status = :status and (b.nextAssignmentAt is null or b.nextAssignmentAt <= :now) order by b.createdAt asc")
    List<UUID> findDueBookingIds(@Param("status") BookingStatus status, @Param("now") Instant now);
}
