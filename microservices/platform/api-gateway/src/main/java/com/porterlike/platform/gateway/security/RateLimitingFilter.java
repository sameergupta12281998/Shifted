package com.porterlike.platform.gateway.security;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private final Map<String, FixedWindowCounter> counters = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int generalLimitPerMinute;
    private final int authLimitPerMinute;

    public RateLimitingFilter(
            @Value("${security.rate-limit.general-per-minute:120}") int generalLimitPerMinute,
            @Value("${security.rate-limit.auth-per-minute:20}") int authLimitPerMinute
    ) {
        this(Clock.systemUTC(), generalLimitPerMinute, authLimitPerMinute);
    }

    RateLimitingFilter(Clock clock, int generalLimitPerMinute, int authLimitPerMinute) {
        this.clock = clock;
        this.generalLimitPerMinute = generalLimitPerMinute;
        this.authLimitPerMinute = authLimitPerMinute;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/actuator/")) {
            return chain.filter(exchange);
        }

        int limit = path.startsWith("/auth/") ? authLimitPerMinute : generalLimitPerMinute;
        String key = buildKey(exchange, path.startsWith("/auth/"));
        WindowDecision decision = counters.compute(key, (unused, existing) -> {
            long currentWindow = currentWindow();
            if (existing == null || existing.windowStartEpochMinute != currentWindow) {
                return new FixedWindowCounter(currentWindow, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        }).toDecision(limit);

        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(limit));
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(decision.remaining()));

        if (decision.exceeded()) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        evictOldWindows();
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private String buildKey(ServerWebExchange exchange, boolean authPath) {
        if (!authPath) {
            String principalId = exchange.getRequest().getHeaders().getFirst(JwtAuthenticationFilter.AUTHENTICATED_USER_ID_HEADER);
            if (principalId != null && !principalId.isBlank()) {
                return "user:" + principalId;
            }
        }

        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return "ip:" + forwardedFor.split(",")[0].trim();
        }

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress == null ? "ip:unknown" : "ip:" + remoteAddress.getAddress().getHostAddress();
    }

    private long currentWindow() {
        return clock.instant().getEpochSecond() / 60;
    }

    private void evictOldWindows() {
        long cutoff = currentWindow() - 2;
        counters.entrySet().removeIf(entry -> entry.getValue().windowStartEpochMinute < cutoff);
    }

    private record FixedWindowCounter(long windowStartEpochMinute, AtomicInteger count) {
        WindowDecision toDecision(int limit) {
            int current = count.get();
            return new WindowDecision(current > limit, Math.max(0, limit - current));
        }
    }

    private record WindowDecision(boolean exceeded, int remaining) {
    }
}