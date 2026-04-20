package com.porterlike.services.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.porterlike.services.booking.event.KafkaEventPublisher;
import com.porterlike.services.booking.model.Booking;
import com.porterlike.services.booking.model.BookingDriverOffer;
import com.porterlike.services.booking.model.BookingStatus;
import com.porterlike.services.booking.model.DriverOfferStatus;
import com.porterlike.services.booking.repository.BookingDriverOfferRepository;
import com.porterlike.services.booking.repository.BookingRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestTemplate;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class BookingAssignmentIntegrationTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingDriverOfferRepository offerRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private KafkaEventPublisher eventPublisher;

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        eventPublisher = mock(KafkaEventPublisher.class);
        bookingService = new BookingService(
                bookingRepository,
                offerRepository,
                restTemplate,
                eventPublisher,
                transactionManager,
                2,
                20,
                5,
                60
        );
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    @Test
    void assignmentSchedulerDispatchesPendingOfferToDriverService() {
        Booking booking = saveBooking(0, Instant.now().minusSeconds(5));
        UUID driverId = UUID.randomUUID();

        server.expect(requestTo("http://driver-service/driver/nearby?vehicleType=BIKE&limit=2"))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        """
                                [{"driverId":"%s","vehicleType":"BIKE"}]
                                """.formatted(driverId),
                        MediaType.APPLICATION_JSON
                ));
        server.expect(requestTo("http://driver-service/driver/%s/offer/%s?ttlSeconds=20".formatted(driverId, booking.getId())))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        """
                                {
                                  "offerId": "offer-1",
                                  "driverId": "%s",
                                  "bookingId": "%s",
                                  "status": "PENDING",
                                  "expiresAt": "2030-01-01T00:00:00Z"
                                }
                                """.formatted(driverId, booking.getId()),
                        MediaType.APPLICATION_JSON
                ));

        bookingService.assignmentScheduler();

        Booking persisted = bookingRepository.findById(booking.getId()).orElseThrow();
        BookingDriverOffer offer = offerRepository.findTopByBookingIdOrderByOfferedAtDesc(booking.getId()).orElseThrow();

        assertThat(persisted.getStatus()).isEqualTo(BookingStatus.CREATED);
        assertThat(persisted.getAssignmentAttempt()).isEqualTo(1);
        assertThat(persisted.getNextAssignmentAt()).isAfter(Instant.now().plusSeconds(4));
        assertThat(offer.getExternalOfferId()).isEqualTo("offer-1");
        assertThat(offer.getDriverId()).isEqualTo(driverId);
        assertThat(offer.getStatus()).isEqualTo(DriverOfferStatus.PENDING);
    }

    @Test
    void attemptAssignmentMarksBookingAssignedWhenDriverAccepts() {
        Booking booking = saveBooking(1, Instant.now().minusSeconds(5));
        UUID driverId = UUID.randomUUID();
        savePendingOffer(booking.getId(), driverId, "offer-accepted");

        server.expect(requestTo("http://driver-service/driver/offers/offer-accepted"))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        """
                                {
                                  "offerId": "offer-accepted",
                                  "driverId": "%s",
                                  "bookingId": "%s",
                                  "status": "ACCEPTED",
                                  "expiresAt": "2030-01-01T00:00:00Z"
                                }
                                """.formatted(driverId, booking.getId()),
                        MediaType.APPLICATION_JSON
                ));

        bookingService.attemptAssignment(booking.getId());

        Booking persisted = bookingRepository.findById(booking.getId()).orElseThrow();
        BookingDriverOffer offer = offerRepository.findTopByBookingIdOrderByOfferedAtDesc(booking.getId()).orElseThrow();

        assertThat(persisted.getStatus()).isEqualTo(BookingStatus.ASSIGNED);
        assertThat(persisted.getDriverId()).isEqualTo(driverId);
        assertThat(offer.getStatus()).isEqualTo(DriverOfferStatus.ACCEPTED);
    }

    @Test
    void assignmentSchedulerReschedulesRejectedOfferForRetry() {
        Booking booking = saveBooking(1, Instant.now().minusSeconds(5));
        UUID driverId = UUID.randomUUID();
        savePendingOffer(booking.getId(), driverId, "offer-rejected");

        server.expect(requestTo("http://driver-service/driver/offers/offer-rejected"))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        """
                                {
                                  "offerId": "offer-rejected",
                                  "driverId": "%s",
                                  "bookingId": "%s",
                                  "status": "REJECTED",
                                  "expiresAt": "2030-01-01T00:00:00Z"
                                }
                                """.formatted(driverId, booking.getId()),
                        MediaType.APPLICATION_JSON
                ));

        bookingService.assignmentScheduler();

        Booking persisted = bookingRepository.findById(booking.getId()).orElseThrow();
        BookingDriverOffer offer = offerRepository.findTopByBookingIdOrderByOfferedAtDesc(booking.getId()).orElseThrow();

        assertThat(persisted.getStatus()).isEqualTo(BookingStatus.CREATED);
        assertThat(persisted.getAssignmentAttempt()).isEqualTo(2);
        assertThat(persisted.getNextAssignmentAt()).isAfter(Instant.now().plusSeconds(9));
        assertThat(offer.getStatus()).isEqualTo(DriverOfferStatus.REJECTED);
    }

    @Test
    void assignmentSchedulerReschedulesWhenNearbyLookupFails() {
        Booking booking = saveBooking(0, Instant.now().minusSeconds(5));

        server.expect(requestTo("http://driver-service/driver/nearby?vehicleType=BIKE&limit=2"))
                .andExpect(method(GET))
                .andRespond(withServerError());

        bookingService.assignmentScheduler();

        Booking persisted = bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(persisted.getStatus()).isEqualTo(BookingStatus.CREATED);
        assertThat(persisted.getAssignmentAttempt()).isEqualTo(1);
        assertThat(persisted.getNextAssignmentAt()).isAfter(Instant.now().plusSeconds(4));
        assertThat(persisted.getNextAssignmentAt()).isBefore(Instant.now().plusSeconds(20));
    }

    @Test
    void assignmentSchedulerReschedulesWhenOfferCallFailsForAllCandidates() {
        Booking booking = saveBooking(0, Instant.now().minusSeconds(5));
        UUID driver1 = UUID.randomUUID();
        UUID driver2 = UUID.randomUUID();

        server.expect(requestTo("http://driver-service/driver/nearby?vehicleType=BIKE&limit=2"))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        """
                                [
                                  {"driverId":"%s","vehicleType":"BIKE"},
                                  {"driverId":"%s","vehicleType":"BIKE"}
                                ]
                                """.formatted(driver1, driver2),
                        MediaType.APPLICATION_JSON
                ));
        server.expect(requestTo("http://driver-service/driver/%s/offer/%s?ttlSeconds=20".formatted(driver1, booking.getId())))
                .andExpect(method(POST))
                .andRespond(withServerError());
        server.expect(requestTo("http://driver-service/driver/%s/offer/%s?ttlSeconds=20".formatted(driver2, booking.getId())))
                .andExpect(method(POST))
                .andRespond(withServerError());

        bookingService.assignmentScheduler();

        Booking persisted = bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(persisted.getStatus()).isEqualTo(BookingStatus.CREATED);
        assertThat(persisted.getAssignmentAttempt()).isEqualTo(1);
        assertThat(persisted.getNextAssignmentAt()).isAfter(Instant.now().plusSeconds(4));
        assertThat(offerRepository.findTopByBookingIdOrderByOfferedAtDesc(booking.getId())).isEmpty();
    }

    private Booking saveBooking(int assignmentAttempt, Instant nextAssignmentAt) {
        Booking booking = new Booking();
        booking.setUserId(UUID.randomUUID());
        booking.setPickup("Koramangala");
        booking.setDropAddress("Indiranagar");
        booking.setVehicleType("BIKE");
        booking.setStatus(BookingStatus.CREATED);
        booking.setIdempotencyKey("req-" + UUID.randomUUID());
        booking.setAssignmentAttempt(assignmentAttempt);
        booking.setNextAssignmentAt(nextAssignmentAt);
        booking.setCreatedAt(Instant.now().minusSeconds(30));
        booking.setUpdatedAt(Instant.now().minusSeconds(30));
        return bookingRepository.saveAndFlush(booking);
    }

    private void savePendingOffer(UUID bookingId, UUID driverId, String externalOfferId) {
        BookingDriverOffer offer = new BookingDriverOffer();
        offer.setBookingId(bookingId);
        offer.setDriverId(driverId);
        offer.setExternalOfferId(externalOfferId);
        offer.setStatus(DriverOfferStatus.PENDING);
        offer.setOfferedAt(Instant.now().minusSeconds(20));
        offer.setUpdatedAt(Instant.now().minusSeconds(20));
        offerRepository.saveAndFlush(offer);
    }
}