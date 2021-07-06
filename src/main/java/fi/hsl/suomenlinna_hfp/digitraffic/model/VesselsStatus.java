package fi.hsl.suomenlinna_hfp.digitraffic.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VesselsStatus {
    public final int readErrors;
    public final int sentErrors;
    public final ZonedDateTime updateTime;
    public final String status;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public VesselsStatus(@JsonProperty("readErrors") int readErrors,
                         @JsonProperty("sentErrors") int sentErrors,
                         @JsonProperty("updateTime") ZonedDateTime updateTime,
                         @JsonProperty("status") String status) {
        this.readErrors = readErrors;
        this.sentErrors = sentErrors;
        this.updateTime = updateTime;
        this.status = status;
    }

    public boolean everythingOk() {
        return 0 == readErrors &&
                0 == sentErrors &&
                "CONNECTED".equals(status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VesselsStatus that = (VesselsStatus) o;
        return readErrors == that.readErrors && sentErrors == that.sentErrors && Objects.equals(updateTime, that.updateTime) && Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(readErrors, sentErrors, updateTime, status);
    }

    @Override
    public String toString() {
        return "VesselsStatus{" +
                "readErrors=" + readErrors +
                ", sentErrors=" + sentErrors +
                ", updateTime=" + updateTime +
                ", status='" + status + '\'' +
                '}';
    }
}
