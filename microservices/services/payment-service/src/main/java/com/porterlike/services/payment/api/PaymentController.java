package com.porterlike.services.payment.api;

import com.porterlike.services.payment.dto.ConfirmPaymentRequest;
import com.porterlike.services.payment.dto.ConfirmPaymentResponse;
import com.porterlike.services.payment.dto.CreatePaymentOrderRequest;
import com.porterlike.services.payment.dto.CreatePaymentOrderResponse;
import com.porterlike.services.payment.dto.RefundRequest;
import com.porterlike.services.payment.dto.RefundResponse;
import com.porterlike.services.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-order")
    public ResponseEntity<CreatePaymentOrderResponse> createOrder(@Valid @RequestBody CreatePaymentOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createOrder(request));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ConfirmPaymentResponse> confirm(@Valid @RequestBody ConfirmPaymentRequest request) {
        return ResponseEntity.ok(paymentService.confirm(request));
    }

    @PostMapping("/refund")
    public ResponseEntity<RefundResponse> refund(@Valid @RequestBody RefundRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(paymentService.refund(request));
    }

    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<List<CreatePaymentOrderResponse>> getByBookingId(@PathVariable String bookingId) {
        return ResponseEntity.ok(paymentService.getByBookingId(bookingId));
    }

    @PostMapping("/webhooks/provider")
    public ResponseEntity<Void> webhook(@RequestBody Map<String, Object> payload) {
        paymentService.handleWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
