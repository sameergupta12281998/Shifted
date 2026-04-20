package com.porterlike.services.fraud.api;

import com.porterlike.services.fraud.dto.BookingFraudCheckRequest;
import com.porterlike.services.fraud.dto.FraudCheckResponse;
import com.porterlike.services.fraud.dto.PaymentFraudCheckRequest;
import com.porterlike.services.fraud.model.FraudReport;
import com.porterlike.services.fraud.service.FraudDetectionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fraud")
public class FraudController {

    private final FraudDetectionService fraudDetectionService;

    public FraudController(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    @PostMapping("/check/booking")
    public FraudCheckResponse checkBooking(@Valid @RequestBody BookingFraudCheckRequest request) {
        return fraudDetectionService.checkBooking(request);
    }

    @PostMapping("/check/payment")
    public FraudCheckResponse checkPayment(@Valid @RequestBody PaymentFraudCheckRequest request) {
        return fraudDetectionService.checkPayment(request);
    }

    @GetMapping("/reports/pending")
    public List<FraudReport> pendingReports() {
        return fraudDetectionService.getPendingReports();
    }

    @PostMapping("/reports/{reportId}/review")
    public FraudReport markReviewed(@PathVariable UUID reportId) {
        return fraudDetectionService.markReviewed(reportId);
    }
}
