package fi.hsl.suomenlinna_hfp.gtfs.utils;

import org.junit.Before;
import org.junit.Test;
import xyz.malkki.gtfs.model.Calendar;
import xyz.malkki.gtfs.model.CalendarDate;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServiceDatesTest {
    private ServiceDates serviceDates;

    @Before
    public void setup() {
        List<Calendar> calendars = Collections.singletonList(new Calendar("1", true, true, true, true, true, false, false, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31)));
        List<CalendarDate> calendarDates = Arrays.asList(new CalendarDate("1", LocalDate.of(2020, 1, 1), 2), new CalendarDate("1", LocalDate.of(2020, 1, 5), 1), new CalendarDate("1", LocalDate.of(2020, 2, 1), 1));
        serviceDates = new ServiceDates(calendars, calendarDates);
    }

    @Test
    public void testGetDatesForService() {
        assertTrue(serviceDates.getDatesForService("1").contains(LocalDate.of(2020, 1, 5)));
        assertTrue(serviceDates.getDatesForService("1").contains(LocalDate.of(2020, 2, 1)));
        assertFalse(serviceDates.getDatesForService("1").contains(LocalDate.of(2020, 1, 1)));
        assertFalse(serviceDates.getDatesForService("1").contains(LocalDate.of(2020, 1, 4)));
        assertTrue(serviceDates.getDatesForService("1").contains(LocalDate.of(2020, 1, 2)));
    }

    @Test
    public void testIsServiceRunningOn() {
        assertTrue(serviceDates.isServiceRunningOn("1", LocalDate.of(2020, 1, 5)));
        assertTrue(serviceDates.isServiceRunningOn("1", LocalDate.of(2020, 2, 1)));
        assertFalse(serviceDates.isServiceRunningOn("1", LocalDate.of(2020, 1, 1)));
        assertFalse(serviceDates.getDatesForService("1").contains(LocalDate.of(2020, 1, 4)));
        assertTrue(serviceDates.isServiceRunningOn("1", LocalDate.of(2020, 1, 2)));
    }
}
