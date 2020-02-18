package fi.hsl.suomenlinna_hfp.digitraffic.model;

import com.google.gson.annotations.SerializedName;
import fi.hsl.suomenlinna_hfp.common.model.LatLng;

import java.util.Objects;

public class VesselLocation {
    public final Integer mmsi;
    @SerializedName("geometry")
    public final LatLng coordinates;
    public final Properties properties;

    public VesselLocation(Integer mmsi, LatLng coordinates, Properties properties) {
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

    public static class Properties {
        //Speed over ground in knots
        @SerializedName("sog")
        public final Double speed;
        //Course over ground in degrees from north
        @SerializedName("cog")
        public final Double course;
        //Status
        @SerializedName("navStat")
        public final Integer status;
        //Rate of turn
        @SerializedName("rot")
        public final Double rateOfTurn;
        @SerializedName("posAcc")
        public final Boolean positionAccurate;
        public final Boolean raim;
        //Heading of the vessel
        public final Double heading;
        //Timestamp in milliseconds
        @SerializedName("timestampExternal")
        public final Long timestamp;

        public Properties(Double speed, Double course, Integer status, Double rateOfTurn, Boolean positionAccurate, Boolean raim, Double heading, Long timestamp) {
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
