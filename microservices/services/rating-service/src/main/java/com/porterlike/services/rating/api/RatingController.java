package com.porterlike.services.rating.api;

import com.porterlike.services.rating.dto.RatingResponse;
import com.porterlike.services.rating.dto.RatingStats;
import com.porterlike.services.rating.dto.SubmitRatingRequest;
import com.porterlike.services.rating.service.RatingService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RatingResponse submit(@Valid @RequestBody SubmitRatingRequest request) {
        return ratingService.submit(request);
    }

    @GetMapping("/drivers/{driverId}")
    public RatingStats driverStats(@PathVariable UUID driverId) {
        return ratingService.getDriverStats(driverId);
    }

    @GetMapping("/users/{userId}")
    public RatingStats userStats(@PathVariable UUID userId) {
        return ratingService.getUserStats(userId);
    }
}
