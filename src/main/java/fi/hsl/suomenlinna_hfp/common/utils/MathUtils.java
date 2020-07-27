package fi.hsl.suomenlinna_hfp.common.utils;

public class MathUtils {
    private MathUtils() {}

    /**
     * Returns percentage as integer
     * @param percentage Percentage 0.0 - 1.0
     * @return Integer, 0-100
     */
    public static int percentageAsInteger(double percentage) {
        return (int)Math.round(percentage * 100);
    }

    /**
     * Clamps value into range [min, max]
     * @param value Value
     * @param min Min value
     * @param max Max value
     * @return Value in range [min, max]
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
