package fi.hsl.suomenlinna_hfp.common.utils;

public class SpeedUtils {
    private SpeedUtils() {}

    public static double knotsToMetresPerSecond(double knots) {
        return knots * 0.51444;
    }

    public static double metresPerSecondToKnots(double metresPerSecond) {
        return metresPerSecond / 0.51444;
    }

    public static double kilometresPerHourToMetresPerSecond(double kilometresPerHour) {
        return kilometresPerHour / 3.6;
    }

    public static double metresPerSecondToKilometresPerHour(double metresPerSecond) {
        return metresPerSecond * 3.6;
    }
}
