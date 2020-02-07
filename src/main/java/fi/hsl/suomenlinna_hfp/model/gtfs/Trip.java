package fi.hsl.suomenlinna_hfp.model.gtfs;

import java.util.Objects;

public class Trip {
    private String routeId;
    private String serviceId;
    private String tripId;
    private Integer directionId;

    public Trip(String routeId, String serviceId, String tripId, Integer directionId) {
        this.routeId = routeId;
        this.serviceId = serviceId;
        this.tripId = tripId;
        this.directionId = directionId;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getTripId() {
        return tripId;
    }

    public Integer getDirectionId() {
        return directionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trip trip = (Trip) o;
        return Objects.equals(routeId, trip.routeId) &&
                Objects.equals(serviceId, trip.serviceId) &&
                Objects.equals(tripId, trip.tripId) &&
                Objects.equals(directionId, trip.directionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeId, serviceId, tripId, directionId);
    }

    @Override
    public String toString() {
        return "Trip{" +
                "routeId='" + routeId + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", tripId='" + tripId + '\'' +
                ", directionId=" + directionId +
                '}';
    }
}
