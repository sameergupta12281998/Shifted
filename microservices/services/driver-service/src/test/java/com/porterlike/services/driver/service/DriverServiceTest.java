package com.porterlike.services.driver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.porterlike.services.driver.dto.DriverLocationRequest;
import com.porterlike.services.driver.dto.DriverResponse;
import com.porterlike.services.driver.dto.RegisterDriverRequest;
import com.porterlike.services.driver.model.Driver;
import com.porterlike.services.driver.model.VerificationStatus;
import com.porterlike.services.driver.repository.DriverOfferRepository;
import com.porterlike.services.driver.repository.DriverRepository;
import com.porterlike.services.driver.security.AuthenticatedPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private DriverOfferRepository driverOfferRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private DriverService driverService;

    private UUID driverId;
    private UUID accountId;
    private Driver driver;

    @BeforeEach
    void setUp() {
        driverId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        driver = new Driver();
        driver.setName("Raj");
        driver.setAccountId(accountId);
        driver.setVehicleType("BIKE");
        driver.setVehicleNumber("KA01AB1234");
        driver.setOnline(false);
        driver.setAvailable(true);
        driver.setUpdatedAt(Instant.now());
        ReflectionTestUtils.setField(driver, "id", driverId);

        // inject tracking endpoint via reflection since it comes from @Value
        ReflectionTestUtils.setField(driverService, "trackingEndpoint", "http://tracking-service/tracking/location");
    }

    // --- register ---

    @Test
    void registerCreatesNewDriverForDriverRole() {
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(accountId.toString(), "DRIVER");
        RegisterDriverRequest request = new RegisterDriverRequest("Raj", "BIKE", "KA01AB1234");

        given(driverRepository.findByAccountId(accountId)).willReturn(Optional.empty());
        given(driverRepository.save(any())).willReturn(driver);

        DriverResponse response = driverService.register(principal, request);

        assertThat(response.name()).isEqualTo("Raj");
        assertThat(response.vehicleType()).isEqualTo("BIKE");
        verify(driverRepository).save(any());
    }

    @Test
    void registerReturnsExistingDriverWhenAlreadyRegistered() {
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(accountId.toString(), "DRIVER");
        RegisterDriverRequest request = new RegisterDriverRequest("Raj", "BIKE", "KA01AB1234");

        given(driverRepository.findByAccountId(accountId)).willReturn(Optional.of(driver));

        DriverResponse response = driverService.register(principal, request);

        assertThat(response.id()).isEqualTo(driverId.toString());
        verify(driverRepository, never()).save(any());
    }

    @Test
    void registerAllowsAdminRole() {
        UUID adminId = UUID.randomUUID();
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(adminId.toString(), "ADMIN");
        RegisterDriverRequest request = new RegisterDriverRequest("Raj", "BIKE", "KA01AB1234");

        given(driverRepository.findByAccountId(adminId)).willReturn(Optional.empty());
        given(driverRepository.save(any())).willReturn(driver);

        DriverResponse response = driverService.register(principal, request);

        assertThat(response).isNotNull();
    }

    @Test
    void registerThrowsForUserRole() {
        UUID userId = UUID.randomUUID();
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(userId.toString(), "USER");
        RegisterDriverRequest request = new RegisterDriverRequest("Raj", "BIKE", "KA01AB1234");

        assertThatThrownBy(() -> driverService.register(principal, request))
                .isInstanceOf(SecurityException.class);
    }

    // --- setOnline ---

    @Test
    void setOnlineTrueMarksDriverOnlineAndAvailable() {
        driver.setOnline(false);
        driver.setAvailable(false);
        driver.setVerificationStatus(VerificationStatus.APPROVED);
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(accountId.toString(), "DRIVER");

        given(driverRepository.findByIdForUpdate(driverId)).willReturn(Optional.of(driver));
        given(driverRepository.save(driver)).willReturn(driver);

        DriverResponse response = driverService.setOnline(principal, driverId, true);

        assertThat(response.online()).isTrue();
        assertThat(response.available()).isTrue();
    }

    @Test
    void setOnlineFalseMarksDriverOfflineAndUnavailable() {
        driver.setOnline(true);
        driver.setAvailable(true);
        driver.setVerificationStatus(VerificationStatus.APPROVED);
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(accountId.toString(), "DRIVER");

        given(driverRepository.findByIdForUpdate(driverId)).willReturn(Optional.of(driver));
        given(driverRepository.save(driver)).willReturn(driver);

        DriverResponse response = driverService.setOnline(principal, driverId, false);

        assertThat(response.online()).isFalse();
        assertThat(response.available()).isFalse();
    }

    @Test
    void setOnlineThrowsSecurityExceptionWhenNotVerified() {
        driver.setVerificationStatus(VerificationStatus.UNVERIFIED);
        driver.setOnline(false);
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(accountId.toString(), "DRIVER");

        given(driverRepository.findByIdForUpdate(driverId)).willReturn(Optional.of(driver));

        assertThatThrownBy(() -> driverService.setOnline(principal, driverId, true))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("must be verified");
    }

    @Test
    void setOnlineThrowsSecurityExceptionWhenPending() {
        driver.setVerificationStatus(VerificationStatus.PENDING);
        driver.setOnline(false);
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(accountId.toString(), "DRIVER");

        given(driverRepository.findByIdForUpdate(driverId)).willReturn(Optional.of(driver));

        assertThatThrownBy(() -> driverService.setOnline(principal, driverId, true))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("must be verified");
    }

    @Test
    void setOnlineSucceedsWhenApproved() {
        driver.setVerificationStatus(VerificationStatus.APPROVED);
        driver.setOnline(false);
        driver.setAvailable(false);
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(accountId.toString(), "DRIVER");

        given(driverRepository.findByIdForUpdate(driverId)).willReturn(Optional.of(driver));
        given(driverRepository.save(driver)).willReturn(driver);

        DriverResponse response = driverService.setOnline(principal, driverId, true);

        assertThat(response.online()).isTrue();
        assertThat(response.verificationStatus()).isEqualTo("APPROVED");
    }

    // --- verification ---

    @Test
    void approveVerificationSetStatusToApproved() {
        driver.setVerificationStatus(VerificationStatus.PENDING);
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(UUID.randomUUID().toString(), "ADMIN");

        given(driverRepository.findById(driverId)).willReturn(Optional.of(driver));
        given(driverRepository.save(any())).willAnswer(invocation -> {
            Driver d = invocation.getArgument(0);
            return d;
        });

        DriverResponse response = driverService.approveVerification(principal, driverId);

        assertThat(response.verificationStatus()).isEqualTo("APPROVED");
    }

    @Test
    void approveVerificationThrowsSecurityExceptionForNonAdmin() {
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(accountId.toString(), "DRIVER");

        assertThatThrownBy(() -> driverService.approveVerification(principal, driverId))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Only admins");
    }

    @Test
    void rejectVerificationSetStatusToRejected() {
        driver.setVerificationStatus(VerificationStatus.PENDING);
        driver.setOnline(true);
        driver.setAvailable(true);
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(UUID.randomUUID().toString(), "ADMIN");

        given(driverRepository.findById(driverId)).willReturn(Optional.of(driver));
        given(driverRepository.save(any())).willAnswer(invocation -> {
            Driver d = invocation.getArgument(0);
            return d;
        });

        DriverResponse response = driverService.rejectVerification(principal, driverId);

        assertThat(response.verificationStatus()).isEqualTo("REJECTED");
        assertThat(response.online()).isFalse();
    }

    @Test
    void rejectVerificationThrowsSecurityExceptionForNonAdmin() {
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(accountId.toString(), "DRIVER");

        assertThatThrownBy(() -> driverService.rejectVerification(principal, driverId))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Only admins");
    }

    // --- nearby ---

    @Test
    void nearbyReturnsDriversUpToLimit() {
        Driver d1 = buildDriver(UUID.randomUUID(), UUID.randomUUID(), "BIKE");
        Driver d2 = buildDriver(UUID.randomUUID(), UUID.randomUUID(), "BIKE");

        given(driverRepository.findTop20ByOnlineTrueAndAvailableTrueAndVehicleTypeOrderByUpdatedAtDesc("BIKE"))
                .willReturn(List.of(d1, d2));

        List<DriverResponse> result = driverService.nearby("BIKE", 5);

        assertThat(result).hasSize(2);
    }

    @Test
    void nearbyEnforcesMaxLimitOf20() {
        given(driverRepository.findTop20ByOnlineTrueAndAvailableTrueAndVehicleTypeOrderByUpdatedAtDesc("BIKE"))
                .willReturn(List.of());

        List<DriverResponse> result = driverService.nearby("BIKE", 999);

        assertThat(result).isEmpty();
    }

    @Test
    void nearbyEnforcesMinLimitOf1() {
        given(driverRepository.findTop20ByOnlineTrueAndAvailableTrueAndVehicleTypeOrderByUpdatedAtDesc("BIKE"))
                .willReturn(List.of());

        List<DriverResponse> result = driverService.nearby("BIKE", 0);

        assertThat(result).isEmpty();
    }

    // --- assign ---

    @Test
    void assignReturnsTrueWhenDriverOnlineAndAvailable() {
        driver.setOnline(true);
        driver.setAvailable(true);
        UUID bookingId = UUID.randomUUID();

        given(driverRepository.findByIdForUpdate(driverId)).willReturn(Optional.of(driver));
        given(driverRepository.save(driver)).willReturn(driver);

        boolean result = driverService.assign(driverId, bookingId);

        assertThat(result).isTrue();
        assertThat(driver.getCurrentBookingId()).isEqualTo(bookingId);
        assertThat(driver.isAvailable()).isFalse();
    }

    @Test
    void assignReturnsFalseWhenDriverOffline() {
        driver.setOnline(false);
        driver.setAvailable(true);
        UUID bookingId = UUID.randomUUID();

        given(driverRepository.findByIdForUpdate(driverId)).willReturn(Optional.of(driver));

        boolean result = driverService.assign(driverId, bookingId);

        assertThat(result).isFalse();
        verify(driverRepository, never()).save(any());
    }

    @Test
    void assignReturnsFalseWhenDriverUnavailable() {
        driver.setOnline(true);
        driver.setAvailable(false);
        UUID bookingId = UUID.randomUUID();

        given(driverRepository.findByIdForUpdate(driverId)).willReturn(Optional.of(driver));

        boolean result = driverService.assign(driverId, bookingId);

        assertThat(result).isFalse();
    }

    @Test
    void assignThrowsWhenDriverNotFound() {
        UUID bookingId = UUID.randomUUID();
        given(driverRepository.findByIdForUpdate(driverId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.assign(driverId, bookingId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Driver not found");
    }

    // --- complete ---

    @Test
    void completeReturnsTrueAndFreesDriver() {
        UUID bookingId = UUID.randomUUID();
        driver.setOnline(true);
        driver.setAvailable(false);
        ReflectionTestUtils.setField(driver, "currentBookingId", bookingId);
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(accountId.toString(), "DRIVER");

        given(driverRepository.findByIdForUpdate(driverId)).willReturn(Optional.of(driver));
        given(driverRepository.save(driver)).willReturn(driver);

        boolean result = driverService.complete(principal, driverId, bookingId);

        assertThat(result).isTrue();
        assertThat(driver.getCurrentBookingId()).isNull();
        assertThat(driver.isAvailable()).isTrue();
    }

    @Test
    void completeReturnsFalseWhenBookingIdMismatch() {
        UUID bookingId = UUID.randomUUID();
        UUID otherBookingId = UUID.randomUUID();
        ReflectionTestUtils.setField(driver, "currentBookingId", otherBookingId);
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(accountId.toString(), "DRIVER");

        given(driverRepository.findByIdForUpdate(driverId)).willReturn(Optional.of(driver));

        boolean result = driverService.complete(principal, driverId, bookingId);

        assertThat(result).isFalse();
    }

    // --- updateLocation ---

    @Test
    void updateLocationSavesAndPostsToTracking() {
        driver.setOnline(true);
        DriverLocationRequest request = new DriverLocationRequest(12.97, 77.59);
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(accountId.toString(), "DRIVER");

        given(driverRepository.findByIdForUpdate(driverId)).willReturn(Optional.of(driver));
        given(driverRepository.save(driver)).willReturn(driver);

        DriverResponse response = driverService.updateLocation(principal, driverId, request);

        assertThat(response.latitude()).isEqualTo(12.97);
        assertThat(response.longitude()).isEqualTo(77.59);
        verify(restTemplate).exchange(
            eq("http://tracking-service/tracking/location"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Void.class)
        );
    }

    // --- helpers ---

    private Driver buildDriver(UUID id, UUID accId, String vehicleType) {
        Driver d = new Driver();
        d.setName("Driver");
        d.setAccountId(accId);
        d.setVehicleType(vehicleType);
        d.setVehicleNumber("TN01XX" + id.toString().substring(0, 4).toUpperCase());
        d.setOnline(true);
        d.setAvailable(true);
        d.setUpdatedAt(Instant.now());
        ReflectionTestUtils.setField(d, "id", id);
        return d;
    }
}
