package com.porterlike.services.payment.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.porterlike.services.payment.dto.CreatePaymentOrderRequest;
import com.porterlike.services.payment.dto.ConfirmPaymentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void createOrder_returns201WithStatusCreated() throws Exception {
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(
                "booking-123",
                new BigDecimal("250.00"),
                "UPI"
        );

        mockMvc.perform(post("/payments/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.bookingId").value("booking-123"))
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.method").value("UPI"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.currency").value("INR"));
    }

    @Test
    void confirmPayment_returns200WithStatusPaid() throws Exception {
        // Create an order first
        CreatePaymentOrderRequest createRequest = new CreatePaymentOrderRequest(
                "booking-456",
                new BigDecimal("500.00"),
                "CARD"
        );
        MvcResult createResult = mockMvc.perform(post("/payments/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String orderId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("orderId").asText();

        ConfirmPaymentRequest confirmRequest = new ConfirmPaymentRequest(orderId, "prov-ref-001");

        mockMvc.perform(post("/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentId").isNotEmpty());
    }

    @Test
    void getByBookingId_returnsPaymentsForBooking() throws Exception {
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(
                "booking-789",
                new BigDecimal("100.00"),
                "CASH"
        );
        mockMvc.perform(post("/payments/create-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get("/payments/bookings/booking-789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
