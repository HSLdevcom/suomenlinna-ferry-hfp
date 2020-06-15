package fi.hsl.suomenlinna_hfp.digitraffic.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.hsl.suomenlinna_hfp.common.model.VehicleMetadata;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VesselMetadata implements VehicleMetadata {
    public final Integer mmsi;
    public final String name;
    public final Integer shipType;
    public final Integer referencePointA;
    public final Integer referencePointB;
    public final Integer referencePointC;
    public final Integer referencePointD;
    public final Integer positionType;
    public final Integer draught;
    public final Integer imo;
    public final String callSign;
    public final Long eta;
    public final Long timestamp;
    public final String destination;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public VesselMetadata(@JsonProperty("mmsi") Integer mmsi,
                          @JsonProperty("name") String name,
                          @JsonProperty("shipType") Integer shipType,
                          @JsonProperty("referencePointA") Integer referencePointA,
                          @JsonProperty("referencePointB") Integer referencePointB,
                          @JsonProperty("referencePointC") Integer referencePointC,
                          @JsonProperty("referencePointD") Integer referencePointD,
                          @JsonProperty("posType") Integer positionType,
                          @JsonProperty("draught") Integer draught,
                          @JsonProperty("imo") Integer imo,
                          @JsonProperty("callSign") String callSign,
                          @JsonProperty("eta") Long eta,
                          @JsonProperty("timestamp") Long timestamp,
                          @JsonProperty("destination") String destination) {
        this.mmsi = mmsi;
        this.name = name;
        this.shipType = shipType;
        this.referencePointA = referencePointA;
        this.referencePointB = referencePointB;
        this.referencePointC = referencePointC;
        this.referencePointD = referencePointD;
        this.positionType = positionType;
        this.draught = draught;
        this.imo = imo;
        this.callSign = callSign;
        this.eta = eta;
        this.timestamp = timestamp;
        this.destination = destination;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VesselMetadata that = (VesselMetadata) o;
        return Objects.equals(mmsi, that.mmsi) &&
                Objects.equals(name, that.name) &&
                Objects.equals(shipType, that.shipType) &&
                Objects.equals(referencePointA, that.referencePointA) &&
                Objects.equals(referencePointB, that.referencePointB) &&
                Objects.equals(referencePointC, that.referencePointC) &&
                Objects.equals(referencePointD, that.referencePointD) &&
                Objects.equals(positionType, that.positionType) &&
                Objects.equals(draught, that.draught) &&
                Objects.equals(imo, that.imo) &&
                Objects.equals(callSign, that.callSign) &&
                Objects.equals(eta, that.eta) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(destination, that.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mmsi, name, shipType, referencePointA, referencePointB, referencePointC, referencePointD, positionType, draught, imo, callSign, eta, timestamp, destination);
    }

    @Override
    public String toString() {
        return "VesselMetadata{" +
                "mmsi=" + mmsi +
                ", name='" + name + '\'' +
                ", shipType=" + shipType +
                ", referencePointA=" + referencePointA +
                ", referencePointB=" + referencePointB +
                ", referencePointC=" + referencePointC +
                ", referencePointD=" + referencePointD +
                ", positionType=" + positionType +
                ", draught=" + draught +
                ", imo=" + imo +
                ", callSign='" + callSign + '\'' +
                ", eta=" + eta +
                ", timestamp=" + timestamp +
                ", destination='" + destination + '\'' +
                '}';
    }

    @Override
    public String getId() {
        return String.valueOf(mmsi);
    }

    @Override
    public String getLabel() {
        return name;
    }
}
