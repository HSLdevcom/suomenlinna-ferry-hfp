package fi.hsl.suomenlinna_hfp.hfp.utils;

import fi.hsl.suomenlinna_hfp.common.model.LatLng;
import fi.hsl.suomenlinna_hfp.hfp.model.TripDescriptor;
import fi.hsl.suomenlinna_hfp.hfp.model.VehicleId;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GeohashLevelCalculator {
    private final Map<VehicleId, LatLng> previousPositions = new HashMap<>();
    private final Map<VehicleId, TripDescriptor> previousTripDescriptors = new HashMap<>();
    private final Map<VehicleId, String> previousNextStops = new HashMap<>();

    public int getGeohashLevel(VehicleId vehicleId, LatLng position, TripDescriptor tripDescriptor, String nextStop) {
        LatLng previousPosition = previousPositions.put(vehicleId, position);
        TripDescriptor previousTripDescriptor = previousTripDescriptors.put(vehicleId, tripDescriptor);
        String previousNextStop = previousNextStops.put(vehicleId, nextStop);

        if (!Objects.equals(previousTripDescriptor, tripDescriptor) || !Objects.equals(previousNextStop, nextStop)) {
            //Trip descriptor or next stop changed in topic -> geohash level 0
            return 0;
        } else {
            //Calculate geohash level from coordinates
            return getGeohashLevel(previousPosition, position);
        }
    }

    private static int getGeohashLevel(LatLng previousPosition, LatLng currentPosition) {
        if (previousPosition == null || currentPosition == null) {
            return 0;
        }

        if ((long)previousPosition.getLatitude() != (long)currentPosition.getLatitude() ||
                (long)previousPosition.getLongitude() != (long)currentPosition.getLongitude()) {
            //Integer part changed -> geohash level 0
            return 0;
        } else {
            //Maximum geohash level is 5
            return Math.min(5,
                    Math.min(getGeohashLevel(previousPosition.getLatitude(), currentPosition.getLatitude()), getGeohashLevel(previousPosition.getLongitude(), currentPosition.getLongitude())));
        }
    }

    private static int getGeohashLevel(double coordinateA, double coordinateB) {
        BigDecimal a = BigDecimal.valueOf(coordinateA);
        String aAsString = a.remainder(BigDecimal.ONE).movePointRight(a.scale()).abs().toBigInteger().toString();

        BigDecimal b = BigDecimal.valueOf(coordinateB);
        String bAsString = b.remainder(BigDecimal.ONE).movePointRight(b.scale()).abs().toBigInteger().toString();

        for (int i = 0; i < Math.min(aAsString.length(), bAsString.length()); i++) {
            if (aAsString.charAt(i) != bAsString.charAt(i)) {
                return i+1;
            }
        }
        return 5;
    }
}
