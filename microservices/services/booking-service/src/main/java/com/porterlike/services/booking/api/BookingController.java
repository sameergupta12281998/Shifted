package com.porterlike.services.booking.api;

import com.porterlike.services.booking.dto.BookingResponse;
import com.porterlike.services.booking.dto.CreateBookingRequest;
import com.porterlike.services.booking.security.AuthenticatedPrincipal;
import com.porterlike.services.booking.service.BookingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/booking")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-Authenticated-User-Id") String principalId,
            @RequestHeader("X-Authenticated-Role") String principalRole,
            @Valid @RequestBody CreateBookingRequest request
    ) {
        return bookingService.create(AuthenticatedPrincipal.of(principalId, principalRole), idempotencyKey, request);
    }

    @GetMapping("/{id}")
    public BookingResponse get(
            @RequestHeader("X-Authenticated-User-Id") String principalId,
            @RequestHeader("X-Authenticated-Role") String principalRole,
            @PathVariable("id") UUID id
    ) {
        return bookingService.get(AuthenticatedPrincipal.of(principalId, principalRole), id);
    }

    @GetMapping("/my")
    public List<BookingResponse> myBookings(
            @RequestHeader("X-Authenticated-User-Id") String principalId,
            @RequestHeader("X-Authenticated-Role") String principalRole
    ) {
        return bookingService.myBookings(AuthenticatedPrincipal.of(principalId, principalRole));
    }

    @PostMapping("/cancel/{id}")
    public BookingResponse cancel(
            @RequestHeader("X-Authenticated-User-Id") String principalId,
            @RequestHeader("X-Authenticated-Role") String principalRole,
            @PathVariable("id") UUID id
    ) {
        return bookingService.cancel(AuthenticatedPrincipal.of(principalId, principalRole), id);
    }
}
