package fi.hsl.suomenlinna_hfp.lati.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LatiPassengerCount {
    /**
     * Name of the vessel
     */
    public final String vessel;
    /**
     * Number of passengers onboard
     */
    public final Integer passengers;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public LatiPassengerCount(
            @JsonProperty("ALUS") String vessel,
            @JsonProperty("NOUSIJAT") Integer passengers
    ) {
        this.vessel = vessel;
        this.passengers = passengers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LatiPassengerCount that = (LatiPassengerCount) o;
        return Objects.equals(vessel, that.vessel) &&
                Objects.equals(passengers, that.passengers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vessel, passengers);
    }

    @Override
    public String toString() {
        return "LatiPassengerCount{" +
                "vessel='" + vessel + '\'' +
                ", passengers=" + passengers +
                '}';
    }
}
