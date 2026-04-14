package com.porterlike.services.pricing.api;

import com.porterlike.services.pricing.dto.PricingEstimateRequest;
import com.porterlike.services.pricing.dto.PricingEstimateResponse;
import com.porterlike.services.pricing.service.PricingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pricing")
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping("/estimate")
    public PricingEstimateResponse estimate(@Valid @RequestBody PricingEstimateRequest request) {
        return pricingService.estimate(request);
    }
}
