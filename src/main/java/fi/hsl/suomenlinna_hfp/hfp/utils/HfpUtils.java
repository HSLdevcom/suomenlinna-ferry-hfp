package fi.hsl.suomenlinna_hfp.hfp.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class HfpUtils {
    private static final DateTimeFormatter TST_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    public static String formatTst(long timestampSeconds) {
        return TST_FORMATTER.format(Instant.ofEpochMilli(timestampSeconds).atZone(ZoneId.of("UTC")));
    }

    public static String formatStartTime(int seconds) {
        int time = seconds% 86400; //HFP time is always in 24h format

        int hours = time / 3600;
        int minutes = time / 60 - hours * 60;

        return String.join(":", String.format("%02d", hours), String.format("%02d", minutes));
    }
}
