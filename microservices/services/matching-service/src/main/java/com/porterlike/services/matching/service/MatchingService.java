package com.porterlike.services.matching.service;

import com.porterlike.services.matching.dto.DriverCandidate;
import com.porterlike.services.matching.dto.NearbyDriverRequest;
import com.porterlike.services.matching.dto.NearbyDriverResponse;
import com.porterlike.services.matching.dto.RegisterDriverGeoRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class MatchingService {

    private static final String GEO_KEY_PREFIX = "drivers:geo:";
    private static final String AVAILABLE_KEY_PREFIX = "drivers:available:";

    private final RedisTemplate<String, String> redisTemplate;

    public MatchingService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Register or update a driver's geo position in the Redis GEO index
     * for their vehicle type bucket.
     */
    public void registerDriverLocation(RegisterDriverGeoRequest request) {
        String geoKey = geoKey(request.vehicleType());
        String availableKey = availableKey(request.vehicleType());
        redisTemplate.opsForGeo().add(
                geoKey,
                new Point(request.longitude(), request.latitude()),
                request.driverId()
        );
        redisTemplate.opsForSet().add(availableKey, request.driverId());
    }

    /**
     * Mark a driver as unavailable (offline or busy) so they are excluded
     * from future match results without removing their geo position.
     */
    public void markUnavailable(String driverId, String vehicleType) {
        redisTemplate.opsForSet().remove(availableKey(vehicleType), driverId);
    }

    /**
     * Find the nearest available drivers within the requested radius.
     * Results are ranked by distance (closest first) and a composite score
     * that can be extended with ratings and acceptance-rate inputs.
     */
    public NearbyDriverResponse findNearby(NearbyDriverRequest request) {
        String geoKey = geoKey(request.vehicleType());
        String availableKey = availableKey(request.vehicleType());

        Circle searchArea = new Circle(
                new Point(request.longitude(), request.latitude()),
                new Distance(request.radiusKm(), Metrics.KILOMETERS)
        );

        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance()
                .includeCoordinates()
                .sortAscending()
                .limit(request.limit() * 3L); // over-fetch so we can filter unavailable

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisTemplate.opsForGeo().radius(geoKey, searchArea, args);

        if (results == null) {
            return new NearbyDriverResponse(List.of(), 0);
        }

        Set<String> available = redisTemplate.opsForSet().members(availableKey);

        List<DriverCandidate> candidates = new ArrayList<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {
            String driverId = result.getContent().getName();
            if (available == null || !available.contains(driverId)) {
                continue;
            }
            double distanceKm = result.getDistance().getValue();
            Point pos = result.getContent().getPoint();
            double lat = pos != null ? pos.getY() : 0;
            double lon = pos != null ? pos.getX() : 0;
            double score = computeScore(distanceKm);
            candidates.add(new DriverCandidate(driverId, request.vehicleType(), lat, lon, distanceKm, score));
            if (candidates.size() >= request.limit()) {
                break;
            }
        }

        return new NearbyDriverResponse(candidates, candidates.size());
    }

    /**
     * Simple scoring model — extend with acceptance-rate and driver-rating inputs.
     * A lower distance yields a higher score (max 100 for 0 km, 0 for ≥ 10 km).
     */
    private double computeScore(double distanceKm) {
        double raw = Math.max(0.0, 100.0 - (distanceKm / 10.0) * 100.0);
        return Math.round(raw * 10.0) / 10.0;
    }

    private static String geoKey(String vehicleType) {
        return GEO_KEY_PREFIX + vehicleType.toUpperCase(Locale.ROOT);
    }

    private static String availableKey(String vehicleType) {
        return AVAILABLE_KEY_PREFIX + vehicleType.toUpperCase(Locale.ROOT);
    }
}
