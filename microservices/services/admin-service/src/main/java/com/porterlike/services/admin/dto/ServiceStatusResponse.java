package com.porterlike.services.admin.dto;

import java.util.Map;

public record ServiceStatusResponse(
        String service,
        String status,
        Map<String, Object> details
) {
}
