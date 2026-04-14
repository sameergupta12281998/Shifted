package com.porterlike.services.driver.service;

import com.porterlike.services.driver.dto.DriverLocationRequest;
import com.porterlike.services.driver.dto.DriverOfferResponse;
import com.porterlike.services.driver.dto.DriverResponse;
import com.porterlike.services.driver.dto.RegisterDriverRequest;
import com.porterlike.services.driver.dto.TrackingUpdateRequest;
import com.porterlike.services.driver.model.Driver;
import com.porterlike.services.driver.model.DriverOffer;
import com.porterlike.services.driver.model.DriverOfferStatus;
import com.porterlike.services.driver.model.VerificationStatus;
import com.porterlike.services.driver.repository.DriverOfferRepository;
import com.porterlike.services.driver.repository.DriverRepository;
import com.porterlike.services.driver.security.AuthenticatedPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final DriverOfferRepository driverOfferRepository;
    private final RestTemplate restTemplate;
    private final String trackingEndpoint;
    private final String trackingInternalServiceToken;

    public DriverService(
            DriverRepository driverRepository,
            DriverOfferRepository driverOfferRepository,
            RestTemplate restTemplate,
            @Value("${app.tracking.endpoint}") String trackingEndpoint,
            @Value("${app.tracking.internal-service-token}") String trackingInternalServiceToken
    ) {
        this.driverRepository = driverRepository;
        this.driverOfferRepository = driverOfferRepository;
        this.restTemplate = restTemplate;
        this.trackingEndpoint = trackingEndpoint;
        this.trackingInternalServiceToken = trackingInternalServiceToken;
    }

    @Transactional
    public DriverResponse register(AuthenticatedPrincipal principal, RegisterDriverRequest request) {
        if (!principal.isDriver() && !principal.isAdmin()) {
            throw new SecurityException("Only driver and admin accounts can register driver profiles");
        }

        Driver existing = driverRepository.findByAccountId(principal.userId()).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        Driver driver = new Driver();
        driver.setAccountId(principal.userId());
        driver.setName(request.name());
        driver.setVehicleType(request.vehicleType());
        driver.setVehicleNumber(request.vehicleNumber());
        driver.setOnline(false);
        driver.setAvailable(true);
        driver.setUpdatedAt(Instant.now());
        return toResponse(driverRepository.save(driver));
    }

    @Transactional
    public DriverResponse setOnline(AuthenticatedPrincipal principal, UUID driverId, boolean online) {
        Driver driver = authorizeDriverAccess(principal, driverId, true);
        if (online && driver.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new SecurityException("Driver must be verified and approved before going online");
        }
        driver.setOnline(online);
        if (!online) {
            driver.setAvailable(false);
        } else if (driver.getCurrentBookingId() == null) {
            driver.setAvailable(true);
        }
        driver.setUpdatedAt(Instant.now());
        return toResponse(driverRepository.save(driver));
    }

    @Transactional
    public DriverResponse updateLocation(AuthenticatedPrincipal principal, UUID driverId, DriverLocationRequest request) {
        Driver driver = authorizeDriverAccess(principal, driverId, true);
        driver.setLatitude(request.latitude());
        driver.setLongitude(request.longitude());
        driver.setUpdatedAt(Instant.now());
        Driver saved = driverRepository.save(driver);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Service-Token", trackingInternalServiceToken);
        restTemplate.exchange(
            trackingEndpoint,
            HttpMethod.POST,
            new HttpEntity<>(
                new TrackingUpdateRequest(
                    saved.getId().toString(),
                    saved.getCurrentBookingId() == null ? null : saved.getCurrentBookingId().toString(),
                    saved.getLatitude(),
                    saved.getLongitude()
                ),
                headers
            ),
            Void.class
        );

        return toResponse(saved);
    }

    public List<DriverResponse> nearby(String vehicleType, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return driverRepository.findTop20ByOnlineTrueAndAvailableTrueAndVehicleTypeOrderByUpdatedAtDesc(vehicleType)
                .stream()
                .limit(safeLimit)
                .map(this::toResponse)
                .toList();
    }

    public boolean assign(UUID driverId, UUID bookingId) {
        Driver driver = driverRepository.findByIdForUpdate(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
        if (!driver.isOnline() || !driver.isAvailable()) {
            return false;
        }
        driver.setAvailable(false);
        driver.setCurrentBookingId(bookingId);
        driver.setUpdatedAt(Instant.now());
        driverRepository.save(driver);
        return true;
    }

    @Transactional
    public DriverOfferResponse createOffer(AuthenticatedPrincipal principal, UUID driverId, UUID bookingId, int ttlSeconds) {
        if (principal.isPresent()) {
            authorizeDriverAccess(principal, driverId, false);
        }

        Driver driver = driverRepository.findByIdForUpdate(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
        if (!driver.isOnline() || !driver.isAvailable()) {
            throw new IllegalArgumentException("Driver unavailable for offer");
        }

        DriverOffer existing = driverOfferRepository.findByDriverIdAndBookingId(driverId, bookingId).orElse(null);
        if (existing != null) {
            expireIfNeeded(existing);
            if (existing.getStatus() == DriverOfferStatus.PENDING || existing.getStatus() == DriverOfferStatus.ACCEPTED) {
                return toOfferResponse(existing);
            }
        }

        Instant now = Instant.now();
        DriverOffer offer = new DriverOffer();
        offer.setExternalOfferId(UUID.randomUUID().toString());
        offer.setDriverId(driverId);
        offer.setBookingId(bookingId);
        offer.setStatus(DriverOfferStatus.PENDING);
        offer.setExpiresAt(now.plusSeconds(Math.max(ttlSeconds, 10)));
        offer.setCreatedAt(now);
        offer.setUpdatedAt(now);
        return toOfferResponse(driverOfferRepository.save(offer));
    }

    public DriverOfferResponse getOffer(AuthenticatedPrincipal principal, String offerId) {
        DriverOffer offer = findOffer(offerId);
        expireIfNeeded(offer);
        if (principal.isPresent()) {
            authorizeOfferAccess(principal, offer);
        }
        return toOfferResponse(offer);
    }

    @Transactional
    public DriverOfferResponse acceptOffer(AuthenticatedPrincipal principal, UUID driverId, String offerId) {
        authorizeDriverAccess(principal, driverId, false);
        DriverOffer offer = driverOfferRepository.findByExternalOfferIdForUpdate(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found"));
        if (!offer.getDriverId().equals(driverId)) {
            throw new IllegalArgumentException("Offer does not belong to this driver");
        }

        expireIfNeeded(offer);
        if (offer.getStatus() != DriverOfferStatus.PENDING) {
            return toOfferResponse(offer);
        }

        Driver driver = driverRepository.findByIdForUpdate(driverId)
            .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
        if (!driver.isOnline() || !driver.isAvailable()) {
            throw new IllegalArgumentException("Driver unavailable to accept offer");
        }

        driver.setAvailable(false);
        driver.setCurrentBookingId(offer.getBookingId());
        driver.setUpdatedAt(Instant.now());
        driverRepository.save(driver);

        offer.setStatus(DriverOfferStatus.ACCEPTED);
        offer.setUpdatedAt(Instant.now());
        return toOfferResponse(driverOfferRepository.save(offer));
    }

    @Transactional
    public DriverOfferResponse rejectOffer(AuthenticatedPrincipal principal, UUID driverId, String offerId) {
        authorizeDriverAccess(principal, driverId, false);
        DriverOffer offer = driverOfferRepository.findByExternalOfferIdForUpdate(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found"));
        if (!offer.getDriverId().equals(driverId)) {
            throw new IllegalArgumentException("Offer does not belong to this driver");
        }

        expireIfNeeded(offer);
        if (offer.getStatus() == DriverOfferStatus.PENDING) {
            offer.setStatus(DriverOfferStatus.REJECTED);
            offer.setUpdatedAt(Instant.now());
            offer = driverOfferRepository.save(offer);
        }
        return toOfferResponse(offer);
    }

    @Transactional
    public boolean complete(AuthenticatedPrincipal principal, UUID driverId, UUID bookingId) {
        Driver driver = authorizeDriverAccess(principal, driverId, true);
        if (driver.getCurrentBookingId() == null || !driver.getCurrentBookingId().equals(bookingId)) {
            return false;
        }
        driver.setCurrentBookingId(null);
        driver.setAvailable(driver.isOnline());
        driver.setUpdatedAt(Instant.now());
        driverRepository.save(driver);
        return true;
    }

    @Transactional
    public DriverResponse approveVerification(AuthenticatedPrincipal principal, UUID driverId) {
        if (!principal.isAdmin()) {
            throw new SecurityException("Only admins can approve driver verification");
        }
        Driver driver = find(driverId);
        driver.setVerificationStatus(VerificationStatus.APPROVED);
        driver.setUpdatedAt(Instant.now());
        return toResponse(driverRepository.save(driver));
    }

    @Transactional
    public DriverResponse rejectVerification(AuthenticatedPrincipal principal, UUID driverId) {
        if (!principal.isAdmin()) {
            throw new SecurityException("Only admins can reject driver verification");
        }
        Driver driver = find(driverId);
        driver.setVerificationStatus(VerificationStatus.REJECTED);
        if (driver.isOnline()) {
            driver.setOnline(false);
            driver.setAvailable(false);
        }
        driver.setUpdatedAt(Instant.now());
        return toResponse(driverRepository.save(driver));
    }

    private Driver find(UUID id) {
        return driverRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Driver not found"));
    }

    private DriverOffer findOffer(String offerId) {
        return driverOfferRepository.findByExternalOfferId(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found"));
    }

    private void expireIfNeeded(DriverOffer offer) {
        if (offer.getStatus() == DriverOfferStatus.PENDING && offer.getExpiresAt().isBefore(Instant.now())) {
            offer.setStatus(DriverOfferStatus.EXPIRED);
            offer.setUpdatedAt(Instant.now());
            driverOfferRepository.save(offer);
        }
    }

    private DriverResponse toResponse(Driver d) {
        return new DriverResponse(
                d.getId().toString(),
                d.getName(),
                d.getVehicleType(),
                d.getVehicleNumber(),
                d.getVerificationStatus().name(),
                d.isOnline(),
                d.isAvailable(),
                d.getCurrentBookingId() == null ? null : d.getCurrentBookingId().toString(),
                d.getLatitude(),
                d.getLongitude()
        );
    }

    private DriverOfferResponse toOfferResponse(DriverOffer offer) {
        return new DriverOfferResponse(
                offer.getExternalOfferId(),
                offer.getBookingId().toString(),
                offer.getDriverId().toString(),
                offer.getStatus().name(),
                offer.getExpiresAt().toString()
        );
    }

    private Driver authorizeDriverAccess(AuthenticatedPrincipal principal, UUID driverId, boolean forUpdate) {
        if (principal.isAdmin()) {
            return forUpdate
                    ? driverRepository.findByIdForUpdate(driverId).orElseThrow(() -> new IllegalArgumentException("Driver not found"))
                    : find(driverId);
        }
        if (!principal.isDriver()) {
            throw new SecurityException("Driver access denied");
        }

        Driver driver = forUpdate
                ? driverRepository.findByIdForUpdate(driverId).orElseThrow(() -> new IllegalArgumentException("Driver not found"))
                : find(driverId);
        if (driver.getAccountId() == null || !driver.getAccountId().equals(principal.userId())) {
            throw new SecurityException("Driver access denied");
        }
        return driver;
    }

    private void authorizeOfferAccess(AuthenticatedPrincipal principal, DriverOffer offer) {
        if (principal.isAdmin()) {
            return;
        }
        if (!principal.isDriver()) {
            throw new SecurityException("Offer access denied");
        }

        Driver driver = driverRepository.findById(offer.getDriverId())
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
        if (driver.getAccountId() == null || !driver.getAccountId().equals(principal.userId())) {
            throw new SecurityException("Offer access denied");
        }
    }
}
