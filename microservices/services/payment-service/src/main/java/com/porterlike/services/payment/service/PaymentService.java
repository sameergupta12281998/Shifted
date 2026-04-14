package com.porterlike.services.payment.service;

import com.porterlike.services.payment.dto.ConfirmPaymentRequest;
import com.porterlike.services.payment.dto.ConfirmPaymentResponse;
import com.porterlike.services.payment.dto.CreatePaymentOrderRequest;
import com.porterlike.services.payment.dto.CreatePaymentOrderResponse;
import com.porterlike.services.payment.dto.RefundRequest;
import com.porterlike.services.payment.dto.RefundResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PaymentService {

    @Value("${app.payment.provider:stub}")
    private String provider;

    @Value("${app.payment.currency:INR}")
    private String currency;

    private static class PaymentRecord {
        String orderId;
        String paymentId;
        String bookingId;
        BigDecimal amount;
        String method;
        String status;
        String provider;
        String currency;
    }

    private final ConcurrentHashMap<String, PaymentRecord> orders = new ConcurrentHashMap<>();

    public CreatePaymentOrderResponse createOrder(CreatePaymentOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        PaymentRecord record = new PaymentRecord();
        record.orderId = orderId;
        record.bookingId = request.bookingId();
        record.amount = request.amount();
        record.method = request.method();
        record.status = "CREATED";
        record.provider = provider;
        record.currency = currency;
        orders.put(orderId, record);
        return new CreatePaymentOrderResponse(orderId, record.bookingId, record.amount, record.currency, record.method, "CREATED", provider);
    }

    public ConfirmPaymentResponse confirm(ConfirmPaymentRequest request) {
        PaymentRecord record = orders.get(request.orderId());
        if (record == null) {
            throw new IllegalArgumentException("Order not found: " + request.orderId());
        }
        String paymentId = UUID.randomUUID().toString();
        record.paymentId = paymentId;
        record.status = "PAID";
        return new ConfirmPaymentResponse(paymentId, record.orderId, "PAID");
    }

    public RefundResponse refund(RefundRequest request) {
        PaymentRecord record = orders.values().stream()
                .filter(r -> request.paymentId().equals(r.paymentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + request.paymentId()));
        return new RefundResponse(UUID.randomUUID().toString(), record.paymentId, request.amount(), "QUEUED");
    }

    public List<CreatePaymentOrderResponse> getByBookingId(String bookingId) {
        return orders.values().stream()
                .filter(r -> bookingId.equals(r.bookingId))
                .map(r -> new CreatePaymentOrderResponse(r.orderId, r.bookingId, r.amount, r.currency, r.method, r.status, r.provider))
                .toList();
    }

    public void handleWebhook(Map<String, Object> payload) {
        // stub: no-op — real implementation routes to provider-specific handler
    }
}
