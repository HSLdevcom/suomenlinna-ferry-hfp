package fi.hsl.suomenlinna_hfp.utils;

public class SpeedUtils {
    private SpeedUtils() {}

    public static double knotsToMetresPerSecond(double knots) {
        return knots * 0.51444;
    }

    public static double metresPerSecondToKnots(double metresPerSecond) {
        return metresPerSecond / 0.51444;
    }
}
