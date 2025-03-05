package fi.hsl.suomenlinna_hfp.gtfs.parser;

import fi.hsl.suomenlinna_hfp.gtfs.model.*;
import xyz.malkki.gtfs.model.*;
import xyz.malkki.gtfs.model.Calendar;
import xyz.malkki.gtfs.serialization.parser.GtfsFeedParser;
import xyz.malkki.gtfs.serialization.parser.ZipGtfsFeedParser;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class GtfsParser {
    private GtfsParser() {}

    /**
     * Parses GTFS feed from specified file
     * @param gtfsFile File that contains GTFS feed
     * @param routeIds List of route IDs that will be parsed or null if all routes should be parsed
     * @return GTFS feed
     * @throws IOException
     */
    public static GtfsFeed parseGtfs(File gtfsFile, Collection<String> routeIds) throws IOException {
        try (GtfsFeedParser gtfsFeedParser = new ZipGtfsFeedParser(gtfsFile.toPath())) {
            List<Route> routes = gtfsFeedParser.parseRoutes().filter(route -> routeIds.contains(route.getRouteId())).collect(Collectors.toList());
            List<Trip> trips = gtfsFeedParser.parseTrips().filter(trip -> routeIds.contains(trip.getRouteId())).collect(Collectors.toList());

            Set<String> serviceIds = new HashSet<>();
            Set<String> tripIds = new HashSet<>(trips.size());
            trips.forEach(trip -> {
                serviceIds.add(trip.getServiceId());
                tripIds.add(trip.getTripId());
            });

            List<Calendar> calendars = gtfsFeedParser.parseCalendars().filter(calendar -> serviceIds.contains(calendar.getServiceId())).collect(Collectors.toList());
            List<CalendarDate> calendarDates = gtfsFeedParser.parseCalendarDates().filter(calendarDate -> serviceIds.contains(calendarDate.getServiceId())).collect(Collectors.toList());

            List<StopTime> stopTimes = gtfsFeedParser.parseStopTimes().filter(stopTime -> tripIds.contains(stopTime.getTripId())).collect(Collectors.toList());

            Set<String> stopIds = new HashSet<>();
            stopTimes.forEach(stopTime -> stopIds.add(stopTime.getStopId()));

            List<Stop> stops = gtfsFeedParser.parseStops().filter(stop -> stopIds.contains(stop.getStopId())).collect(Collectors.toList());

            return new GtfsFeed(routes, calendars, calendarDates, stops, stopTimes, trips);
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }
}
