package com.porterlike.services.pricing.service;

import com.porterlike.services.pricing.dto.PricingEstimateRequest;
import com.porterlike.services.pricing.dto.PricingEstimateResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PricingService {

    private final String currency;
    private final double bikeBaseFare;
    private final double bikePerKm;
    private final double bikePerMinute;
    private final double miniTruckBaseFare;
    private final double miniTruckPerKm;
    private final double miniTruckPerMinute;

    public PricingService(
            @Value("${app.pricing.currency:INR}") String currency,
            @Value("${app.pricing.bike.base-fare:40}") double bikeBaseFare,
            @Value("${app.pricing.bike.per-km:12}") double bikePerKm,
            @Value("${app.pricing.bike.per-minute:1}") double bikePerMinute,
            @Value("${app.pricing.mini-truck.base-fare:80}") double miniTruckBaseFare,
            @Value("${app.pricing.mini-truck.per-km:20}") double miniTruckPerKm,
            @Value("${app.pricing.mini-truck.per-minute:2}") double miniTruckPerMinute
    ) {
        this.currency = currency;
        this.bikeBaseFare = bikeBaseFare;
        this.bikePerKm = bikePerKm;
        this.bikePerMinute = bikePerMinute;
        this.miniTruckBaseFare = miniTruckBaseFare;
        this.miniTruckPerKm = miniTruckPerKm;
        this.miniTruckPerMinute = miniTruckPerMinute;
    }

    public PricingEstimateResponse estimate(PricingEstimateRequest request) {
        RateCard rateCard = resolveRateCard(request.vehicleType());
        double distanceFare = request.distanceKm() * rateCard.perKm;
        double timeFare = request.durationMinutes() * rateCard.perMinute;
        double totalFare = round2(rateCard.baseFare + distanceFare + timeFare);

        return new PricingEstimateResponse(
                request.vehicleType().toUpperCase(Locale.ROOT),
                round2(request.distanceKm()),
                round2(request.durationMinutes()),
                round2(rateCard.baseFare),
                round2(distanceFare),
                round2(timeFare),
                totalFare,
                currency
        );
    }

    private RateCard resolveRateCard(String vehicleType) {
        return switch (vehicleType.toUpperCase(Locale.ROOT)) {
            case "BIKE" -> new RateCard(bikeBaseFare, bikePerKm, bikePerMinute);
            case "MINI_TRUCK", "MINI-TRUCK" -> new RateCard(miniTruckBaseFare, miniTruckPerKm, miniTruckPerMinute);
            default -> throw new IllegalArgumentException("Unsupported vehicle type: " + vehicleType);
        };
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private record RateCard(double baseFare, double perKm, double perMinute) {
    }
}
