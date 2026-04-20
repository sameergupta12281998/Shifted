package com.porterlike.services.fraud.dto;

public record FraudCheckResponse(
        String subjectId,
        String riskLevel,
        boolean flagged,
        String reason
) {}
