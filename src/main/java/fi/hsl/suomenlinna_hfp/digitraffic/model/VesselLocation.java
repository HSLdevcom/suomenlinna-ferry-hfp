package fi.hsl.suomenlinna_hfp.digitraffic.model;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.hsl.suomenlinna_hfp.common.model.LatLng;
import fi.hsl.suomenlinna_hfp.common.model.VehiclePosition;
import fi.hsl.suomenlinna_hfp.common.utils.SpeedUtils;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VesselLocation implements VehiclePosition {
    @JacksonInject("mmsi")
    public Integer mmsi;
    public final LatLng coordinates;
    public final Double speed; //Speed over ground in knots
    public final Double course; //Course over ground in degrees from north
    public final Integer status; //Status
    public final Double rateOfTurn; //Rate of turn
    public final Boolean positionAccurate;
    public final Boolean raim;
    public final Double heading; //Heading of the vessel
    public final Long timestamp;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public VesselLocation(@JsonProperty("sog") Double speed,
                          @JsonProperty("cog") Double course,
                          @JsonProperty("navStat") Integer status,
                          @JsonProperty("rot") Double rateOfTurn,
                          @JsonProperty("posAcc") Boolean positionAccurate,
                          @JsonProperty("raim") Boolean raim,
                          @JsonProperty("heading") Double heading,
                          @JsonProperty("time") Long timestamp,
                          @JsonProperty("lat") Double latitude,
                          @JsonProperty("lon") Double longitude) {
        this.speed = speed;
        this.course = course;
        this.status = status;
        this.rateOfTurn = rateOfTurn;
        this.positionAccurate = positionAccurate;
        this.raim = raim;
        this.heading = heading;
        this.timestamp = timestamp;

        this.coordinates = new LatLng(latitude, longitude);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VesselLocation that = (VesselLocation) o;
        return Objects.equals(mmsi, that.mmsi) &&
                Objects.equals(coordinates, that.coordinates) &&
                Objects.equals(speed, that.speed) &&
                Objects.equals(course, that.course) &&
                Objects.equals(status, that.status) &&
                Objects.equals(rateOfTurn, that.rateOfTurn) &&
                Objects.equals(positionAccurate, that.positionAccurate) &&
                Objects.equals(raim, that.raim) &&
                Objects.equals(heading, that.heading) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mmsi, coordinates, speed, course, status, rateOfTurn, positionAccurate, raim, heading, timestamp);
    }

    @Override
    public String toString() {
        return "VesselLocation{" +
                "mmsi=" + mmsi +
                ", coordinates=" + coordinates +
                ", speed=" + speed +
                ", course=" + course +
                ", status=" + status +
                ", rateOfTurn=" + rateOfTurn +
                ", positionAccurate=" + positionAccurate +
                ", raim=" + raim +
                ", heading=" + heading +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public String getId() {
        return String.valueOf(mmsi);
    }

    @Override
    public LatLng getCoordinates() {
        return coordinates;
    }

    @Override
    public double getSpeed() {
        return SpeedUtils.knotsToMetresPerSecond(speed);
    }

    @Override
    public double getHeading() {
        //If vessel heading is not available (special value 511), use vessel course for heading
        return Math.round(heading) == 511 ? course : heading;
    }

    @Override
    public long getTimestamp() {
        return timestamp * 1000;
    }
}
