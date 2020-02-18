package fi.hsl.suomenlinna_hfp.hfp.model;

import java.util.Objects;

public class TripDescriptor {
    public final String routeId;
    public final String routeName;
    public final String departureDate;
    public final String startTime;
    public final String directionId;
    public final String headsign;

    public TripDescriptor(String routeId, String routeName, String departureDate, String startTime, String directionId, String headsign) {
        this.routeId = routeId;
        this.routeName = routeName;
        this.departureDate = departureDate;
        this.startTime = startTime;
        this.directionId = directionId;
        this.headsign = headsign;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TripDescriptor that = (TripDescriptor) o;
        return Objects.equals(routeId, that.routeId) &&
                Objects.equals(routeName, that.routeName) &&
                Objects.equals(departureDate, that.departureDate) &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(directionId, that.directionId) &&
                Objects.equals(headsign, that.headsign);
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeId, routeName, departureDate, startTime, directionId, headsign);
    }

    @Override
    public String toString() {
        return "TripDescriptor{" +
                "routeId='" + routeId + '\'' +
                ", routeName='" + routeName + '\'' +
                ", departureDate='" + departureDate + '\'' +
                ", startTime='" + startTime + '\'' +
                ", directionId='" + directionId + '\'' +
                ", headsign='" + headsign + '\'' +
                '}';
    }
}