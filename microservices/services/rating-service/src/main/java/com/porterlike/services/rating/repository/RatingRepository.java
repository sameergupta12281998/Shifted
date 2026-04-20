package com.porterlike.services.rating.repository;

import com.porterlike.services.rating.model.Rating;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RatingRepository extends JpaRepository<Rating, UUID> {

    List<Rating> findByToUserIdAndRoleTarget(UUID toUserId, String roleTarget);

    Optional<Rating> findByBookingIdAndFromUserId(UUID bookingId, UUID fromUserId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.toUserId = :userId AND r.roleTarget = :role")
    Double findAverageScoreByToUserIdAndRoleTarget(
            @Param("userId") UUID userId,
            @Param("role") String role
    );

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.toUserId = :userId AND r.roleTarget = :role")
    long countByToUserIdAndRoleTarget(
            @Param("userId") UUID userId,
            @Param("role") String role
    );
}
