package com.porterlike.services.matching.api;

import com.porterlike.services.matching.dto.NearbyDriverRequest;
import com.porterlike.services.matching.dto.NearbyDriverResponse;
import com.porterlike.services.matching.dto.RegisterDriverGeoRequest;
import com.porterlike.services.matching.service.MatchingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/matching")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    /**
     * POST /matching/drivers/register
     * Called by driver-service whenever a driver goes online or updates location.
     */
    @PostMapping("/drivers/register")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerDriver(@Valid @RequestBody RegisterDriverGeoRequest request) {
        matchingService.registerDriverLocation(request);
    }

    /**
     * DELETE /matching/drivers/{driverId}/unavailable?vehicleType=BIKE
     * Called when a driver goes offline or accepts a booking.
     */
    @DeleteMapping("/drivers/{driverId}/unavailable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markUnavailable(
            @PathVariable("driverId") String driverId,
            @RequestParam("vehicleType") String vehicleType
    ) {
        matchingService.markUnavailable(driverId, vehicleType);
    }

    /**
     * POST /matching/find
     * Find nearest available drivers for a given vehicle type and pickup location.
     */
    @PostMapping("/find")
    public NearbyDriverResponse findNearby(@Valid @RequestBody NearbyDriverRequest request) {
        return matchingService.findNearby(request);
    }
}
