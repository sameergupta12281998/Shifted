package com.porterlike.services.driver.api;

import com.porterlike.services.driver.dto.DriverLocationRequest;
import com.porterlike.services.driver.dto.DriverOfferResponse;
import com.porterlike.services.driver.dto.DriverResponse;
import com.porterlike.services.driver.dto.RegisterDriverRequest;
import com.porterlike.services.driver.security.AuthenticatedPrincipal;
import com.porterlike.services.driver.service.DriverService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/driver")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public DriverResponse register(
            @RequestHeader("X-Authenticated-User-Id") String principalId,
            @RequestHeader("X-Authenticated-Role") String principalRole,
            @Valid @RequestBody RegisterDriverRequest request
    ) {
        return driverService.register(AuthenticatedPrincipal.of(principalId, principalRole), request);
    }

    @PostMapping("/{id}/online")
    public DriverResponse setOnline(
            @RequestHeader("X-Authenticated-User-Id") String principalId,
            @RequestHeader("X-Authenticated-Role") String principalRole,
            @PathVariable("id") UUID id,
            @RequestParam("online") boolean online
    ) {
        return driverService.setOnline(AuthenticatedPrincipal.of(principalId, principalRole), id, online);
    }

    @PostMapping("/{id}/location")
    public DriverResponse updateLocation(
            @RequestHeader("X-Authenticated-User-Id") String principalId,
            @RequestHeader("X-Authenticated-Role") String principalRole,
            @PathVariable("id") UUID id,
            @Valid @RequestBody DriverLocationRequest request
    ) {
        return driverService.updateLocation(AuthenticatedPrincipal.of(principalId, principalRole), id, request);
    }

    @GetMapping("/nearby")
    public List<DriverResponse> nearby(
            @RequestParam("vehicleType") String vehicleType,
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        return driverService.nearby(vehicleType, limit);
    }

    @GetMapping("/instance")
    public Map<String, String> instance() {
        return Map.of("instance", System.getenv().getOrDefault("HOSTNAME", "unknown-instance"));
    }

    @PostMapping("/{id}/assign/{bookingId}")
    public Boolean assign(@PathVariable("id") UUID id, @PathVariable("bookingId") UUID bookingId) {
        return driverService.assign(id, bookingId);
    }

    @PostMapping("/{id}/offer/{bookingId}")
    public DriverOfferResponse createOffer(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) String principalId,
            @RequestHeader(value = "X-Authenticated-Role", required = false) String principalRole,
            @PathVariable("id") UUID id,
            @PathVariable("bookingId") UUID bookingId,
            @RequestParam(value = "ttlSeconds", defaultValue = "30") int ttlSeconds
    ) {
        return driverService.createOffer(AuthenticatedPrincipal.optional(principalId, principalRole), id, bookingId, ttlSeconds);
    }

    @GetMapping("/offers/{offerId}")
    public DriverOfferResponse getOffer(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) String principalId,
            @RequestHeader(value = "X-Authenticated-Role", required = false) String principalRole,
            @PathVariable("offerId") String offerId
    ) {
        return driverService.getOffer(AuthenticatedPrincipal.optional(principalId, principalRole), offerId);
    }

    @PostMapping("/{id}/offers/{offerId}/accept")
    public DriverOfferResponse acceptOffer(
            @RequestHeader("X-Authenticated-User-Id") String principalId,
            @RequestHeader("X-Authenticated-Role") String principalRole,
            @PathVariable("id") UUID id,
            @PathVariable("offerId") String offerId
    ) {
        return driverService.acceptOffer(AuthenticatedPrincipal.of(principalId, principalRole), id, offerId);
    }

    @PostMapping("/{id}/offers/{offerId}/reject")
    public DriverOfferResponse rejectOffer(
            @RequestHeader("X-Authenticated-User-Id") String principalId,
            @RequestHeader("X-Authenticated-Role") String principalRole,
            @PathVariable("id") UUID id,
            @PathVariable("offerId") String offerId
    ) {
        return driverService.rejectOffer(AuthenticatedPrincipal.of(principalId, principalRole), id, offerId);
    }

    @PostMapping("/{id}/complete/{bookingId}")
    public Boolean complete(
            @RequestHeader("X-Authenticated-User-Id") String principalId,
            @RequestHeader("X-Authenticated-Role") String principalRole,
            @PathVariable("id") UUID id,
            @PathVariable("bookingId") UUID bookingId
    ) {
        return driverService.complete(AuthenticatedPrincipal.of(principalId, principalRole), id, bookingId);
    }

    @PostMapping("/{id}/verify/approve")
    public DriverResponse approveVerification(
            @RequestHeader("X-Authenticated-User-Id") String principalId,
            @RequestHeader("X-Authenticated-Role") String principalRole,
            @PathVariable("id") UUID id
    ) {
        return driverService.approveVerification(AuthenticatedPrincipal.of(principalId, principalRole), id);
    }

    @PostMapping("/{id}/verify/reject")
    public DriverResponse rejectVerification(
            @RequestHeader("X-Authenticated-User-Id") String principalId,
            @RequestHeader("X-Authenticated-Role") String principalRole,
            @PathVariable("id") UUID id
    ) {
        return driverService.rejectVerification(AuthenticatedPrincipal.of(principalId, principalRole), id);
    }
}
