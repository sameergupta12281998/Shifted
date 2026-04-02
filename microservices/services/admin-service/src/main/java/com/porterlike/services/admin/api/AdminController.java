package com.porterlike.services.admin.api;

import com.porterlike.services.admin.dto.ServiceStatusResponse;
import com.porterlike.services.admin.security.AuthenticatedPrincipal;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final RestTemplate restTemplate;
    private final String discoveryUrl;

    public AdminController(
            RestTemplate restTemplate,
            @Value("${app.discovery.url:http://localhost:8761}") String discoveryUrl
    ) {
        this.restTemplate = restTemplate;
        this.discoveryUrl = discoveryUrl;
    }

    /**
     * Returns the current instance identity. Useful for health and load-balancer checks.
     */
    @GetMapping("/ping")
    public Map<String, String> ping(
            @RequestHeader("X-Authenticated-User-Id") String principalId,
            @RequestHeader("X-Authenticated-Role") String principalRole
    ) {
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(principalId, principalRole);
        requireAdmin(principal);
        return Map.of(
                "instance", System.getenv().getOrDefault("HOSTNAME", "unknown-instance"),
                "status", "UP"
        );
    }

    /**
     * Probes each downstream service actuator and returns a health summary.
     */
    @GetMapping("/health/services")
    public List<ServiceStatusResponse> serviceHealth(
            @RequestHeader("X-Authenticated-User-Id") String principalId,
            @RequestHeader("X-Authenticated-Role") String principalRole
    ) {
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(principalId, principalRole);
        requireAdmin(principal);

        List<String> services = List.of("auth-service", "booking-service", "driver-service", "tracking-service");
        return services.stream()
                .map(name -> probeService(name, discoveryUrl.replace("8761", servicePort(name))))
                .toList();
    }

    private ServiceStatusResponse probeService(String name, String baseUrl) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restTemplate.getForObject(baseUrl + "/actuator/health", Map.class);
            String status = body != null ? String.valueOf(body.getOrDefault("status", "UNKNOWN")) : "UNKNOWN";
            return new ServiceStatusResponse(name, status, body != null ? body : Map.of());
        } catch (RestClientException e) {
            return new ServiceStatusResponse(name, "DOWN", Map.of("error", e.getMessage()));
        }
    }

    private String servicePort(String name) {
        return switch (name) {
            case "auth-service" -> "8081";
            case "booking-service" -> "8082";
            case "driver-service" -> "8083";
            case "tracking-service" -> "8085";
            default -> "8080";
        };
    }

    private void requireAdmin(AuthenticatedPrincipal principal) {
        if (!principal.isAdmin()) {
            throw new SecurityException("Admin access required");
        }
    }
}
