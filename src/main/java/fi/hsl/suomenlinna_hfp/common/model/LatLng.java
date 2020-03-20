package fi.hsl.suomenlinna_hfp.common.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.util.Objects;

public class LatLng {
    private static final int EARTH_RADIUS_IN_METERS = 6371 * 1000;

    private final double latitude;
    private final double longitude;

    public LatLng(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    /**
     * Calculates distance in meters between coordinates
     * @param other Coordinates of the other location
     * @return Distance between locations in meters
     */
    public double distanceTo(LatLng other) {
        double latDistance = Math.toRadians(other.getLatitude() - latitude);
        double lonDistance = Math.toRadians(other.getLongitude() - longitude);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(other.getLatitude()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_IN_METERS * c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LatLng latLng = (LatLng) o;
        return Double.compare(latLng.latitude, latitude) == 0 &&
                Double.compare(latLng.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }

    @Override
    public String toString() {
        return "(" + latitude + ", " + longitude + ")";
    }

    public static class LatLngDeserializer extends StdDeserializer<LatLng> {
        public LatLngDeserializer() {
            this(LatLng.class);
        }

        protected LatLngDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public LatLng deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
            JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);
            ArrayNode coordinates = ((ArrayNode)jsonNode.get("coordinates"));

            return new LatLng(coordinates.get(1).asDouble(), coordinates.get(0).asDouble());
        }
    }

}
