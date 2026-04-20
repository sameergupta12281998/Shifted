package com.porterlike.services.matching.dto;

import java.util.List;

public record NearbyDriverResponse(
        List<DriverCandidate> drivers,
        int total
) {}
