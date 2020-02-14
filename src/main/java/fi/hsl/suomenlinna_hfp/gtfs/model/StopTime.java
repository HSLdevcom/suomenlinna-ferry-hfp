package fi.hsl.suomenlinna_hfp.gtfs.model;

import java.util.Objects;

public class StopTime {
    private String tripId;
    private Integer arrivalTime; //Arrival time in seconds since midnight
    private Integer departureTime; //Departure time in seconds since midnight
    private String stopId;
    private Integer stopSequence;
    private Integer pickupType;
    private Integer dropOffType;

    public StopTime(String tripId, Integer arrivalTime, Integer departureTime, String stopId, Integer stopSequence, Integer pickupType, Integer dropOffType) {
        this.tripId = tripId;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.stopId = stopId;
        this.stopSequence = stopSequence;
        this.pickupType = pickupType;
        this.dropOffType = dropOffType;
    }

    public String getTripId() {
        return tripId;
    }

    public Integer getArrivalTime() {
        return arrivalTime;
    }

    public Integer getDepartureTime() {
        return departureTime;
    }

    public String getStopId() {
        return stopId;
    }

    public Integer getStopSequence() {
        return stopSequence;
    }

    /**
     * Gets the pickup type, 0 = normal, 1 = no pickup, 2 = must phone agency for pickup, 3 = must coordinate with driver for pickup
     * @return Pickup type
     */
    public Integer getPickupType() {
        return pickupType;
    }

    /**
     * Gets the drop off type, 0 = normal, 1 = no drop off, 2 = must phone agency for drop off, 3 = must coordinate with driver for drop off
     * @return Dro poff type
     */
    public Integer getDropOffType() {
        return dropOffType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StopTime stopTime = (StopTime) o;
        return Objects.equals(tripId, stopTime.tripId) &&
                Objects.equals(arrivalTime, stopTime.arrivalTime) &&
                Objects.equals(departureTime, stopTime.departureTime) &&
                Objects.equals(stopId, stopTime.stopId) &&
                Objects.equals(stopSequence, stopTime.stopSequence) &&
                Objects.equals(pickupType, stopTime.pickupType) &&
                Objects.equals(dropOffType, stopTime.dropOffType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tripId, arrivalTime, departureTime, stopId, stopSequence, pickupType, dropOffType);
    }

    @Override
    public String toString() {
        return "StopTime{" +
                "tripId='" + tripId + '\'' +
                ", arrivalTime=" + arrivalTime +
                ", departureTime=" + departureTime +
                ", stopId='" + stopId + '\'' +
                ", stopSequence=" + stopSequence +
                ", pickupType=" + pickupType +
                ", dropOffType=" + dropOffType +
                '}';
    }
}
