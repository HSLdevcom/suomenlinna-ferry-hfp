package fi.hsl.suomenlinna_hfp.sbdrive.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.hsl.suomenlinna_hfp.common.model.LatLng;

import java.util.Objects;

public class VehicleState {
    public final Integer timestamp;
    public final String vehicleId;
    public final Double latitude;
    public final Double longitude;
    public final Double azimuth;
    public final Double speed;
    public final String runningMode;
    public final String connection;
    public final Boolean canReceivePetitions;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public VehicleState(Integer timestamp,
                        @JsonProperty("vehicle_id") String vehicleId,
                        @JsonProperty("lat") Double latitude,
                        @JsonProperty("lon") Double longitude,
                        Double azimuth,
                        Double speed,
                        @JsonProperty("running_mode") String runningMode,
                        String connection,
                        @JsonProperty("can_receive_petitions") Boolean canReceivePetitions) {
        this.timestamp = timestamp;
        this.vehicleId = vehicleId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.azimuth = azimuth;
        this.speed = speed;
        this.runningMode = runningMode;
        this.connection = connection;
        this.canReceivePetitions = canReceivePetitions;
    }

    public LatLng getCoordinates() {
        return new LatLng(latitude, longitude);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VehicleState that = (VehicleState) o;
        return Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(vehicleId, that.vehicleId) &&
                Objects.equals(latitude, that.latitude) &&
                Objects.equals(longitude, that.longitude) &&
                Objects.equals(azimuth, that.azimuth) &&
                Objects.equals(speed, that.speed) &&
                Objects.equals(runningMode, that.runningMode) &&
                Objects.equals(connection, that.connection) &&
                Objects.equals(canReceivePetitions, that.canReceivePetitions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, vehicleId, latitude, longitude, azimuth, speed, runningMode, connection, canReceivePetitions);
    }

    @Override
    public String toString() {
        return "VehicleState{" +
                "timestamp=" + timestamp +
                ", vehicleId='" + vehicleId + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", azimuth=" + azimuth +
                ", speed=" + speed +
                ", runningMode='" + runningMode + '\'' +
                ", connection='" + connection + '\'' +
                ", canReceivePetitions=" + canReceivePetitions +
                '}';
    }
}
