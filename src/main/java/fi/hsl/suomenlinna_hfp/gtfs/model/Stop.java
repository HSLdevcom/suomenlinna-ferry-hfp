package fi.hsl.suomenlinna_hfp.gtfs.model;

import fi.hsl.suomenlinna_hfp.common.model.LatLng;

import java.util.Objects;

public class Stop {
    private String id;
    private LatLng coordinates;

    public Stop(String id, LatLng coordinates) {
        this.id = id;
        this.coordinates = coordinates;
    }

    public String getId() {
        return id;
    }

    public LatLng getCoordinates() {
        return coordinates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stop stop = (Stop) o;
        return Objects.equals(id, stop.id) &&
                Objects.equals(coordinates, stop.coordinates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, coordinates);
    }

    @Override
    public String toString() {
        return "Stop{" +
                "id='" + id + '\'' +
                ", coordinates=" + coordinates +
                '}';
    }
}
