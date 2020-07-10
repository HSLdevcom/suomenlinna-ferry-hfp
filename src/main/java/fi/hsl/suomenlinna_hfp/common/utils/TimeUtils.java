package fi.hsl.suomenlinna_hfp.common.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimeUtils {
    private TimeUtils() {}

    private static final int ONE_DAY_IN_SECONDS = 24 * 60 * 60;

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

    /**
     * Converts GTFS time to LocalDateTime
     * @param startDate Start date in format YYYYMMDD
     * @param startTime Start time in format HH:mm:ss
     */
    public static LocalDateTime gtfsTimeToLocalDateTime(String startDate, String startTime) {
        final LocalDate date = LocalDate.parse(startDate, DateTimeFormatter.BASIC_ISO_DATE);
        final int startTimeInSeconds = parseTime(startTime);

        return gtfsTimeToLocalDateTime(date, startTimeInSeconds);
    }

    /**
     * Converts GTFS time to LocalDateTime
     */
    public static LocalDateTime gtfsTimeToLocalDateTime(LocalDate startDate, int startTime) {
        final int startTimeAfterMidnight = startTime % ONE_DAY_IN_SECONDS; //GTFS time can be over 24 hours, e.g. 28:00:00 would be 4am
        final int startDaysAfterDate = startTime / ONE_DAY_IN_SECONDS; //Find actual start date

        return startDate.plusDays(startDaysAfterDate).atTime(LocalTime.ofSecondOfDay(startTimeAfterMidnight));
    }
}
