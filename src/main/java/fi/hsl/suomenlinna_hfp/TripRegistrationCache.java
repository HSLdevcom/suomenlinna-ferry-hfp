package fi.hsl.suomenlinna_hfp;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fi.hsl.suomenlinna_hfp.hfp.model.TripDescriptor;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Caches a list of trips that have been registered for
 */
public class TripRegistrationCache {
    private final Map<TripPattern, Cache<TripDescriptor, Boolean>> cache = new HashMap<>(2);

    private final Duration cacheMaxAge;

    public TripRegistrationCache(Duration cacheMaxAge) {
        this.cacheMaxAge = cacheMaxAge;
    }

    private Cache<TripDescriptor, Boolean> getCacheByTripDescriptor(TripDescriptor tripDescriptor) {
        return cache.computeIfAbsent(new TripPattern(tripDescriptor.routeId, tripDescriptor.directionId), k -> Caffeine.newBuilder().expireAfterWrite(cacheMaxAge).build());
    }

    public boolean hasAnyTripBeenRegisteredForPattern(TripDescriptor tripDescriptor) {
        return getCacheByTripDescriptor(tripDescriptor).estimatedSize() > 0;
    }

    public void addRegistration(TripDescriptor tripDescriptor) {
        getCacheByTripDescriptor(tripDescriptor).put(tripDescriptor, true);
    }

    public boolean hasTripBeenRegistered(TripDescriptor tripDescriptor) {
        return Boolean.TRUE.equals(getCacheByTripDescriptor(tripDescriptor).getIfPresent(tripDescriptor));
    }

    private static class TripPattern {
        private final String routeId;
        private final String directionId;

        public TripPattern(String routeId, String directionId) {
            this.routeId = routeId;
            this.directionId = directionId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TripPattern that = (TripPattern) o;
            return Objects.equals(routeId, that.routeId) &&
                    Objects.equals(directionId, that.directionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(routeId, directionId);
        }
    }
}
