package fi.hsl.suomenlinna_hfp.common.utils;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.Assert.assertEquals;

public class TimeUtilsTest {
    @Test
    public void testParseTime() {
        assertEquals(35700, TimeUtils.parseTime("09:55:00"));
        assertEquals(35700, TimeUtils.parseTime("9:55:00"));
        assertEquals(102600, TimeUtils.parseTime("28:30:00"));
        assertEquals(30, TimeUtils.parseTime("00:00:30"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseTimeWithInvalidValue() {
        TimeUtils.parseTime("test");
    }

    @Test
    public void testGtfsTimeToLocalDateTime() {
        LocalDateTime localDateTime1 = TimeUtils.gtfsTimeToLocalDateTime("20200101", "09:00:00");
        assertEquals(LocalDate.of(2020, 1, 1), localDateTime1.toLocalDate());
        assertEquals(LocalTime.of(9, 0), localDateTime1.toLocalTime());

        LocalDateTime localDateTime2 = TimeUtils.gtfsTimeToLocalDateTime("20200101", "28:00:00");
        assertEquals(LocalDate.of(2020, 1, 2), localDateTime2.toLocalDate());
        assertEquals(LocalTime.of(4, 0), localDateTime2.toLocalTime());

        LocalDateTime localDateTime3 = TimeUtils.gtfsTimeToLocalDateTime("20200101", "50:00:00");
        assertEquals(LocalDate.of(2020, 1, 3), localDateTime3.toLocalDate());
        assertEquals(LocalTime.of(2, 0), localDateTime3.toLocalTime());
    }
}
