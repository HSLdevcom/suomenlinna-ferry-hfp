package fi.hsl.suomenlinna_hfp.hfp.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

public class Geohash {
    private final String[] levels;

    public Geohash(double latitude, double longitude) {
        String[] latitudeString = BigDecimal.valueOf(latitude).setScale(3, RoundingMode.DOWN).toPlainString().split("\\.");

        String[] longitudeString = BigDecimal.valueOf(longitude).setScale(3, RoundingMode.DOWN).toPlainString().split("\\.");

        levels = new String[] { latitudeString[0]+";"+longitudeString[0],
                new String(new char[]{ latitudeString[1].charAt(0), longitudeString[1].charAt(0)}),
                new String(new char[]{ latitudeString[1].charAt(1), longitudeString[1].charAt(1)}),
                new String(new char[]{ latitudeString[1].charAt(2), longitudeString[1].charAt(2)}),
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Geohash geohash = (Geohash) o;
        return Arrays.equals(levels, geohash.levels);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(levels);
    }

    @Override
    public String toString() {
        return String.join("/", levels);
    }
}
