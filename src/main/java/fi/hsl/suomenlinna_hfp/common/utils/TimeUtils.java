package fi.hsl.suomenlinna_hfp.common.utils;

public class TimeUtils {
    private TimeUtils() {}

    /**
     * Parses time in HH:mm:ss format to seconds
     * @param gtfsTime
     * @return Time in seconds
     */
    public static int parseTime(String gtfsTime) {
        String[] parts = gtfsTime.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid time format: "+gtfsTime);
        }
        return Integer.parseInt(parts[0]) * 60 * 60 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]);
    }
}
