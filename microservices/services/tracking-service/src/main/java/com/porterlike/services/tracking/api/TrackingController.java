package com.porterlike.services.tracking.api;

import com.porterlike.services.tracking.dto.TrackingSnapshot;
import com.porterlike.services.tracking.dto.TrackingUpdateRequest;
import com.porterlike.services.tracking.service.TrackingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tracking")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @PostMapping("/location")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TrackingSnapshot update(@Valid @RequestBody TrackingUpdateRequest request) {
        return trackingService.update(request);
    }

    @GetMapping("/driver/{driverId}")
    public TrackingSnapshot getDriverLocation(@PathVariable("driverId") String driverId) {
        return trackingService.getByDriver(driverId)
                .orElseThrow(() -> new IllegalArgumentException("No location found for driver"));
    }
}
