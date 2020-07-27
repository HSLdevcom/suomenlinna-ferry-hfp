package fi.hsl.suomenlinna_hfp.gtfs.parser;

import fi.hsl.suomenlinna_hfp.common.model.LatLng;
import fi.hsl.suomenlinna_hfp.gtfs.model.*;
import fi.hsl.suomenlinna_hfp.gtfs.model.Calendar;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import static fi.hsl.suomenlinna_hfp.common.utils.TimeUtils.parseTime;
import static java.nio.charset.StandardCharsets.UTF_8;

public class GtfsParser {
    private static final CSVFormat GTFS_CSV_FORMAT = CSVFormat.RFC4180.withFirstRecordAsHeader();

    private GtfsParser() {}

    /**
     * Parses GTFS feed from specified file
     * @param gtfsFile File that contains GTFS feed
     * @param routeIds List of route IDs that will be parsed or null if all routes should be parsed
     * @return GTFS feed
     * @throws IOException
     */
    public static GtfsFeed parseGtfs(File gtfsFile, Collection<String> routeIds) throws IOException {
        try (ZipFile zipFile = new ZipFile(gtfsFile)) {
            List<Route> routes = parseCSVFromZipFile(zipFile, "routes.txt", parseRoutes(routeIds));
            List<Trip> trips = parseCSVFromZipFile(zipFile, "trips.txt", parseTrips(routeIds));

            Set<String> serviceIds = new HashSet<>();
            Set<String> tripIds = new HashSet<>(trips.size());
            trips.forEach(trip -> {
                serviceIds.add(trip.getServiceId());
                tripIds.add(trip.getTripId());
            });

            List<Calendar> calendars = parseCSVFromZipFile(zipFile,"calendar.txt", parseCalendars(serviceIds));
            List<CalendarDate> calendarDates = parseCSVFromZipFile(zipFile, "calendar_dates.txt", parseCalendarDates(serviceIds));

            List<StopTime> stopTimes = parseCSVFromZipFile(zipFile, "stop_times.txt", parseStopTimes(tripIds));

            Set<String> stopIds = new HashSet<>();
            stopTimes.forEach(stopTime -> stopIds.add(stopTime.getStopId()));

            List<Stop> stops = parseCSVFromZipFile(zipFile, "stops.txt", parseStops(stopIds));

            return new GtfsFeed(routes, calendars, calendarDates, stops, stopTimes, trips);
        }
    }

    private static Function<CSVRecord, Route> parseRoutes(Collection<String> routeIds) {
        return csvRecord -> {
            Route route = new Route(csvRecord.get("route_id"), csvRecord.get("route_short_name"), csvRecord.get("route_long_name"));
            return routeIds == null || routeIds.contains(route.getId()) ? route : null;
        };
    }

    private static Function<CSVRecord, Calendar> parseCalendars(Collection<String> serviceIds) {
        return csvRecord -> {
            Calendar calendar = new Calendar(
                    csvRecord.get("service_id"),
                    "1".equals(csvRecord.get("monday")),
                    "1".equals(csvRecord.get("tuesday")),
                    "1".equals(csvRecord.get("wednesday")),
                    "1".equals(csvRecord.get("thursday")),
                    "1".equals(csvRecord.get("friday")),
                    "1".equals(csvRecord.get("saturday")),
                    "1".equals(csvRecord.get("sunday")),
                    LocalDate.parse(csvRecord.get("start_date"), DateTimeFormatter.BASIC_ISO_DATE),
                    LocalDate.parse(csvRecord.get("end_date"), DateTimeFormatter.BASIC_ISO_DATE)
            );

            return serviceIds == null || serviceIds.contains(calendar.getServiceId()) ? calendar : null;
        };
    }

    private static Function<CSVRecord, CalendarDate> parseCalendarDates(Collection<String> serviceIds) {
        return csvRecord -> {
            CalendarDate calendarDate = new CalendarDate(csvRecord.get("service_id"), LocalDate.parse(csvRecord.get("date"), DateTimeFormatter.BASIC_ISO_DATE), Integer.parseInt(csvRecord.get("exception_type")));

            return serviceIds == null || serviceIds.contains(calendarDate.getServiceId()) ? calendarDate : null;
        };
    }

    private static Function<CSVRecord, Stop> parseStops(Collection<String> stopIds) {
        return csvRecord -> {
            Stop stop = new Stop(csvRecord.get("stop_id"), csvRecord.get("stop_code"), new LatLng(Double.parseDouble(csvRecord.get("stop_lat")), Double.parseDouble(csvRecord.get("stop_lon"))));

            return stopIds == null || stopIds.contains(stop.getId()) ? stop : null;
        };
    }

    private static Function<CSVRecord, StopTime> parseStopTimes(Collection<String> tripIds) {
         return csvRecord -> {
            StopTime stopTime = new StopTime(
                    csvRecord.get("\uFEFFtrip_id"),
                    parseTime(csvRecord.get("arrival_time")),
                    parseTime(csvRecord.get("departure_time")),
                    csvRecord.get("stop_id"),
                    Integer.parseInt(csvRecord.get("stop_sequence")),
                    Integer.parseInt(csvRecord.get("pickup_type")),
                    Integer.parseInt(csvRecord.get("drop_off_type"))
            );

            return tripIds == null || tripIds.contains(stopTime.getTripId()) ? stopTime : null;
        };
    }

    private static Function<CSVRecord, Trip> parseTrips(Collection<String> routeIds) {
        return csvRecord -> {
            Trip trip = new Trip(csvRecord.get("route_id"), csvRecord.get("service_id"), csvRecord.get("trip_id"), Integer.parseInt(csvRecord.get("direction_id")), csvRecord.get("trip_headsign"));

            return routeIds == null || routeIds.contains(trip.getRouteId()) ? trip : null;
        };
    }

    private static <T> List<T> parseCSVFromZipFile(ZipFile zipFile, String entryName, Function<CSVRecord, T> mapper) throws IOException {
        try (CSVParser csvParser = CSVParser.parse(new BufferedInputStream(zipFile.getInputStream(zipFile.getEntry(entryName)), 128 * 1024), UTF_8, GTFS_CSV_FORMAT)) {
            Iterator<CSVRecord> csvRecords = csvParser.iterator();
            return Stream.generate(() -> null)
                    .takeWhile(n -> csvRecords.hasNext())
                    .map(n -> csvRecords.next())
                    .map(mapper)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }
}
