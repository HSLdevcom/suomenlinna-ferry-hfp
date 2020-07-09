package fi.hsl.suomenlinna_hfp.common.model;

import java.util.Objects;

public class PassengerCount {
    private String vehicleId;
    private int currentPassengers;
    private int maxPassengers;

    public PassengerCount(String vehicleId, int currentPassengers, int maxPassengers) {
        this.vehicleId = vehicleId;
        this.currentPassengers = currentPassengers;
        this.maxPassengers = maxPassengers;
    }

    public double getPercentage() {
        return currentPassengers / (double)maxPassengers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PassengerCount that = (PassengerCount) o;
        return currentPassengers == that.currentPassengers &&
                maxPassengers == that.maxPassengers &&
                Objects.equals(vehicleId, that.vehicleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vehicleId, currentPassengers, maxPassengers);
    }

    @Override
    public String toString() {
        return "PassengerCount{" +
                "vehicleId='" + vehicleId + '\'' +
                ", currentPassengers=" + currentPassengers +
                ", maxPassengers=" + maxPassengers +
                '}';
    }
}
