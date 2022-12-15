package fi.hsl.suomenlinna_hfp.digitraffic.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VesselsStatus {
    public final ZonedDateTime updateTime;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public VesselsStatus(@JsonProperty("updated") ZonedDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public boolean everythingOk() {
        return updateTime != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VesselsStatus that = (VesselsStatus) o;
        return Objects.equals(updateTime, that.updateTime);
    }

    @Override
    public int hashCode() {
        return updateTime != null ? updateTime.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "VesselsStatus{" +
                "updateTime=" + updateTime +
                '}';
    }
}
