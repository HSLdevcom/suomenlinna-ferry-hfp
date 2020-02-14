package fi.hsl.suomenlinna_hfp.common.model;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Objects;

public class LatLng {
    private static final int EARTH_RADIUS_IN_METERS = 6371 * 1000;

    private double latitude;
    private double longitude;

    public LatLng() {
    }

    public LatLng(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
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

    public static class LatLngDeserializer implements JsonDeserializer<LatLng> {
        @Override
        public LatLng deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonArray coordinates = json.getAsJsonObject().get("coordinates").getAsJsonArray();
            return new LatLng(coordinates.get(1).getAsDouble(), coordinates.get(0).getAsDouble());
        }
    }

}
