package fi.hsl.suomenlinna_hfp.gtfs.model;

import java.util.Objects;

public class Route {
    private String id;
    private String shortName;
    private String longName;

    public Route(String id, String shortName, String longName) {
        this.id = id;
        this.shortName = shortName;
        this.longName = longName;
    }

    public String getId() {
        return id;
    }

    public String getShortName() {
        return shortName;
    }
    public String getLongName() {
        return longName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route route = (Route) o;
        return Objects.equals(id, route.id) &&
                Objects.equals(shortName, route.shortName) &&
                Objects.equals(longName, route.longName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, shortName, longName);
    }

    @Override
    public String toString() {
        return "Route{" +
                "id='" + id + '\'' +
                ", shortName='" + shortName + '\'' +
                ", longName='" + longName + '\'' +
                '}';
    }
}
