package com.porterlike.services.tracking.api;

import com.porterlike.services.tracking.dto.TrackingSnapshot;
import com.porterlike.services.tracking.dto.TrackingUpdateRequest;
import com.porterlike.services.tracking.service.TrackingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tracking")
public class TrackingController {

    private final TrackingService trackingService;
    private final String internalServiceToken;

    public TrackingController(
            TrackingService trackingService,
            @Value("${app.security.internal-service-token}") String internalServiceToken
    ) {
        this.trackingService = trackingService;
        this.internalServiceToken = internalServiceToken;
    }

    @PostMapping("/location")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TrackingSnapshot update(
            @RequestHeader(value = "X-Internal-Service-Token", required = false) String serviceToken,
            @Valid @RequestBody TrackingUpdateRequest request
    ) {
        authorizeTrackingWrite(serviceToken);
        return trackingService.update(request);
    }

    @GetMapping("/driver/{driverId}")
    public TrackingSnapshot getDriverLocation(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) String principalId,
            @RequestHeader(value = "X-Authenticated-Role", required = false) String principalRole,
            @PathVariable("driverId") String driverId
    ) {
        authorizeTrackingRead(principalId, principalRole);
        return trackingService.getByDriver(driverId)
                .orElseThrow(() -> new NoLocationFoundException("No location found for driver: " + driverId));
    }

    private void authorizeTrackingWrite(String serviceToken) {
        if (internalServiceToken.equals(serviceToken)) {
            return;
        }
        throw new SecurityException("Tracking write access denied");
    }

    private void authorizeTrackingRead(String principalId, String principalRole) {
        if (principalId == null || principalId.isBlank() || principalRole == null || principalRole.isBlank()) {
            throw new SecurityException("Tracking read access denied");
        }
    }
}
