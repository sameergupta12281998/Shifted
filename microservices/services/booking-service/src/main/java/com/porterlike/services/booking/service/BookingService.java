package com.porterlike.services.booking.service;

import com.porterlike.services.booking.dto.BookingResponse;
import com.porterlike.services.booking.dto.CreateBookingRequest;
import com.porterlike.services.booking.dto.DriverCandidateResponse;
import com.porterlike.services.booking.dto.DriverOfferResponse;
import com.porterlike.services.booking.model.Booking;
import com.porterlike.services.booking.model.BookingDriverOffer;
import com.porterlike.services.booking.model.BookingStatus;
import com.porterlike.services.booking.model.DriverOfferStatus;
import com.porterlike.services.booking.repository.BookingDriverOfferRepository;
import com.porterlike.services.booking.repository.BookingRepository;
import com.porterlike.services.booking.security.AuthenticatedPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingDriverOfferRepository offerRepository;
    private final RestTemplate restTemplate;
    private final int maxCandidates;
    private final int offerTtlSeconds;
    private final int initialBackoffSeconds;
    private final int maxBackoffSeconds;
        private final TransactionTemplate transactionTemplate;

    public BookingService(
            BookingRepository bookingRepository,
            BookingDriverOfferRepository offerRepository,
            RestTemplate restTemplate,
            PlatformTransactionManager transactionManager,
            @Value("${app.assignment.max-candidates}") int maxCandidates,
            @Value("${app.assignment.offer-ttl-seconds}") int offerTtlSeconds,
            @Value("${app.assignment.initial-backoff-seconds}") int initialBackoffSeconds,
            @Value("${app.assignment.max-backoff-seconds}") int maxBackoffSeconds
    ) {
        this.bookingRepository = bookingRepository;
        this.offerRepository = offerRepository;
        this.restTemplate = restTemplate;
        this.maxCandidates = maxCandidates;
        this.offerTtlSeconds = offerTtlSeconds;
        this.initialBackoffSeconds = initialBackoffSeconds;
        this.maxBackoffSeconds = maxBackoffSeconds;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public BookingResponse create(AuthenticatedPrincipal principal, String idempotencyKey, CreateBookingRequest request) {
        if (!principal.isUser() && !principal.isAdmin()) {
            throw new IllegalArgumentException("Only users and admins can create bookings");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        Booking booking = transactionTemplate.execute(status -> {
            Booking existing = bookingRepository.findByUserIdAndIdempotencyKey(principal.userId(), idempotencyKey).orElse(null);
            if (existing != null) {
                return existing;
            }

            Booking created = new Booking();
            created.setUserId(principal.userId());
            created.setPickup(request.pickup());
            created.setDropAddress(request.dropAddress());
            created.setVehicleType(request.vehicleType());
            created.setStatus(BookingStatus.CREATED);
            created.setIdempotencyKey(idempotencyKey);
            created.setAssignmentAttempt(0);
            created.setNextAssignmentAt(Instant.now());
            created.setCreatedAt(Instant.now());
            created.setUpdatedAt(Instant.now());
            return bookingRepository.save(created);
        });

        attemptAssignment(booking.getId());
        return toResponse(bookingRepository.findById(booking.getId()).orElseThrow());
    }

    public BookingResponse get(AuthenticatedPrincipal principal, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        authorizeBookingAccess(principal, booking);
        return toResponse(booking);
    }

    public BookingResponse cancel(AuthenticatedPrincipal principal, UUID bookingId) {
        Booking booking = transactionTemplate.execute(status -> {
            Booking lockedBooking = bookingRepository.findByIdForUpdate(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
            authorizeBookingAccess(principal, lockedBooking);
            if (lockedBooking.getStatus() == BookingStatus.COMPLETED) {
                throw new IllegalArgumentException("Completed booking cannot be cancelled");
            }
            lockedBooking.setStatus(BookingStatus.CANCELLED);
            lockedBooking.setUpdatedAt(Instant.now());
            return bookingRepository.save(lockedBooking);
        });
        return toResponse(booking);
    }

    @Scheduled(fixedDelayString = "${app.assignment.scheduler-delay-ms:5000}")
    public void assignmentScheduler() {
        List<UUID> dueBookingIds = bookingRepository.findDueBookingIds(BookingStatus.CREATED, Instant.now());
        for (UUID bookingId : dueBookingIds) {
            attemptAssignment(bookingId);
        }
    }

    public void attemptAssignment(UUID bookingId) {
        transactionTemplate.executeWithoutResult(status -> processAssignmentLocked(bookingId));
    }

    private void processAssignmentLocked(UUID bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (booking.getStatus() != BookingStatus.CREATED) {
            return;
        }

        Instant now = Instant.now();
        BookingDriverOffer latestOffer = offerRepository.findTopByBookingIdOrderByOfferedAtDesc(booking.getId())
                .orElse(null);

        if (latestOffer != null && latestOffer.getStatus() == DriverOfferStatus.PENDING) {
            DriverOfferResponse latestState = fetchOfferStatus(latestOffer.getExternalOfferId());
            if (latestState == null) {
                return;
            }

            DriverOfferStatus mappedStatus = DriverOfferStatus.valueOf(latestState.status());
            latestOffer.setStatus(mappedStatus);
            latestOffer.setUpdatedAt(now);
            offerRepository.save(latestOffer);

            if (mappedStatus == DriverOfferStatus.ACCEPTED) {
                booking.setDriverId(latestOffer.getDriverId());
                booking.setStatus(BookingStatus.ASSIGNED);
                booking.setUpdatedAt(now);
                bookingRepository.save(booking);
                return;
            }

            if (mappedStatus == DriverOfferStatus.PENDING) {
                return;
            }

            scheduleRetry(booking);
            return;
        }

        if (booking.getNextAssignmentAt() != null && booking.getNextAssignmentAt().isAfter(now)) {
            return;
        }

        dispatchNewOfferLocked(booking);
    }

    private void dispatchNewOfferLocked(Booking booking) {
        ResponseEntity<List<DriverCandidateResponse>> response;
        try {
            response = restTemplate.exchange(
                    "http://driver-service/driver/nearby?vehicleType={vehicleType}&limit={limit}",
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    new ParameterizedTypeReference<>() {
                    },
                    booking.getVehicleType(),
                    maxCandidates
            );
        } catch (Exception ex) {
            scheduleRetry(booking);
            return;
        }

        List<DriverCandidateResponse> candidates = response.getBody();
        if (candidates == null || candidates.isEmpty()) {
            scheduleRetry(booking);
            return;
        }

        for (DriverCandidateResponse candidate : candidates) {
            UUID driverId = UUID.fromString(candidate.driverId());
            if (offerRepository.existsByBookingIdAndDriverId(booking.getId(), driverId)) {
                continue;
            }

            try {
                DriverOfferResponse offered = restTemplate.postForObject(
                        "http://driver-service/driver/{driverId}/offer/{bookingId}?ttlSeconds={ttlSeconds}",
                        null,
                        DriverOfferResponse.class,
                        candidate.driverId(),
                        booking.getId().toString(),
                        offerTtlSeconds
                );
                if (offered != null && DriverOfferStatus.valueOf(offered.status()) == DriverOfferStatus.PENDING) {
                    BookingDriverOffer offer = new BookingDriverOffer();
                    offer.setBookingId(booking.getId());
                    offer.setDriverId(driverId);
                    offer.setExternalOfferId(offered.offerId());
                    offer.setStatus(DriverOfferStatus.PENDING);
                    offer.setOfferedAt(Instant.now());
                    offer.setUpdatedAt(Instant.now());
                    offerRepository.save(offer);

                    booking.setAssignmentAttempt(booking.getAssignmentAttempt() + 1);
                    booking.setNextAssignmentAt(Instant.now().plusSeconds(backoffSeconds(booking.getAssignmentAttempt())));
                    booking.setUpdatedAt(Instant.now());
                    bookingRepository.save(booking);
                    return;
                }
            } catch (Exception ignored) {
                // Try next available candidate.
            }
        }

        scheduleRetry(booking);
    }

    private DriverOfferResponse fetchOfferStatus(String offerId) {
        try {
            return restTemplate.getForObject(
                    "http://driver-service/driver/offers/{offerId}",
                    DriverOfferResponse.class,
                    offerId
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private void scheduleRetry(Booking booking) {
        booking.setAssignmentAttempt(booking.getAssignmentAttempt() + 1);
        booking.setNextAssignmentAt(Instant.now().plusSeconds(backoffSeconds(booking.getAssignmentAttempt())));
        booking.setUpdatedAt(Instant.now());
        bookingRepository.save(booking);
    }

    private long backoffSeconds(int attempt) {
        long computed = (long) initialBackoffSeconds * (1L << Math.max(0, attempt - 1));
        return Math.min(computed, maxBackoffSeconds);
    }

    private BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId().toString(),
                booking.getUserId().toString(),
                booking.getDriverId() == null ? null : booking.getDriverId().toString(),
                booking.getPickup(),
                booking.getDropAddress(),
                booking.getVehicleType(),
                booking.getStatus().name()
        );
    }

    private void authorizeBookingAccess(AuthenticatedPrincipal principal, Booking booking) {
        if (principal.isAdmin()) {
            return;
        }
        if (!principal.isUser() || !booking.getUserId().equals(principal.userId())) {
            throw new SecurityException("Booking access denied");
        }
    }
}
