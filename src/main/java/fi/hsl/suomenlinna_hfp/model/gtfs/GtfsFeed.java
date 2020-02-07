package fi.hsl.suomenlinna_hfp.model.gtfs;

import java.util.List;

public class GtfsFeed {
    public final List<Calendar> calendars;
    public final List<CalendarDate> calendarDates;
    public final List<Stop> stops;
    public final List<StopTime> stopTimes;
    public final List<Trip> trips;

    public GtfsFeed(List<Calendar> calendars, List<CalendarDate> calendarDates, List<Stop> stops, List<StopTime> stopTimes, List<Trip> trips) {
        this.calendars = calendars;
        this.calendarDates = calendarDates;
        this.stops = stops;
        this.stopTimes = stopTimes;
        this.trips = trips;
    }
}
