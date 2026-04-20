package com.porterlike.services.rating.service;

import com.porterlike.services.rating.dto.RatingResponse;
import com.porterlike.services.rating.dto.RatingStats;
import com.porterlike.services.rating.dto.SubmitRatingRequest;
import com.porterlike.services.rating.model.Rating;
import com.porterlike.services.rating.repository.RatingRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;

    public RatingService(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    @Transactional
    public RatingResponse submit(SubmitRatingRequest request) {
        ratingRepository.findByBookingIdAndFromUserId(request.bookingId(), request.fromUserId())
                .ifPresent(existing -> {
                    throw new IllegalStateException("Rating already submitted for this booking");
                });

        Rating rating = new Rating();
        rating.setBookingId(request.bookingId());
        rating.setFromUserId(request.fromUserId());
        rating.setToUserId(request.toUserId());
        rating.setRoleTarget(request.roleTarget().toUpperCase());
        rating.setScore(request.score());
        rating.setComment(request.comment());
        rating.setCreatedAt(Instant.now());

        Rating saved = ratingRepository.save(rating);
        return toResponse(saved);
    }

    public RatingStats getDriverStats(UUID driverId) {
        return buildStats(driverId, "DRIVER");
    }

    public RatingStats getUserStats(UUID userId) {
        return buildStats(userId, "USER");
    }

    private RatingStats buildStats(UUID subjectId, String role) {
        Double avg = ratingRepository.findAverageScoreByToUserIdAndRoleTarget(subjectId, role);
        long count = ratingRepository.countByToUserIdAndRoleTarget(subjectId, role);
        return new RatingStats(subjectId, role, avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0, count);
    }

    private RatingResponse toResponse(Rating r) {
        return new RatingResponse(r.getId(), r.getBookingId(), r.getFromUserId(),
                r.getToUserId(), r.getRoleTarget(), r.getScore(), r.getComment());
    }
}
