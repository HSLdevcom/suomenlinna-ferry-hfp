package fi.hsl.suomenlinna_hfp.digitraffic.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fi.hsl.suomenlinna_hfp.common.model.LatLng;

import java.util.Objects;
import java.util.Properties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VesselLocation {
    public final Integer mmsi;
    public final LatLng coordinates;
    public final Properties properties;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public VesselLocation(@JsonProperty("mmsi") Integer mmsi,
                          @JsonProperty("geometry") @JsonDeserialize(using = LatLng.LatLngDeserializer.class) LatLng coordinates,
                          @JsonProperty("properties") Properties properties) {
        this.mmsi = mmsi;
        this.coordinates = coordinates;
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VesselLocation that = (VesselLocation) o;
        return Objects.equals(mmsi, that.mmsi) &&
                Objects.equals(coordinates, that.coordinates) &&
                Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mmsi, coordinates, properties);
    }

    @Override
    public String toString() {
        return "VesselLocation{" +
                "mmsi=" + mmsi +
                ", coordinates=" + coordinates +
                ", properties=" + properties +
                '}';
    }

    @JsonIgnoreProperties(value = { "timestamp" }, ignoreUnknown = true)
    public static class Properties {
        //Speed over ground in knots
        public final Double speed;
        //Course over ground in degrees from north
        public final Double course;
        //Status
        public final Integer status;
        //Rate of turn
        public final Double rateOfTurn;
        public final Boolean positionAccurate;
        public final Boolean raim;
        //Heading of the vessel
        public final Double heading;
        //Timestamp in milliseconds
        public final Long timestamp;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public Properties(@JsonProperty("sog") Double speed,
                          @JsonProperty("cog") Double course,
                          @JsonProperty("navStat") Integer status,
                          @JsonProperty("rot") Double rateOfTurn,
                          @JsonProperty("posAcc") Boolean positionAccurate,
                          @JsonProperty("raim") Boolean raim,
                          @JsonProperty("heading") Double heading,
                          @JsonProperty("timestampExternal") Long timestamp) {
            this.speed = speed;
            this.course = course;
            this.status = status;
            this.rateOfTurn = rateOfTurn;
            this.positionAccurate = positionAccurate;
            this.raim = raim;
            this.heading = heading;
            this.timestamp = timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Properties that = (Properties) o;
            return Objects.equals(speed, that.speed) &&
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
            return Objects.hash(speed, course, status, rateOfTurn, positionAccurate, raim, heading, timestamp);
        }

        @Override
        public String toString() {
            return "Properties{" +
                    "speed=" + speed +
                    ", course=" + course +
                    ", status=" + status +
                    ", rateOfTurn=" + rateOfTurn +
                    ", positionAccurate=" + positionAccurate +
                    ", raim=" + raim +
                    ", heading=" + heading +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
}
