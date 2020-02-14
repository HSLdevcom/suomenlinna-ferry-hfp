package fi.hsl.suomenlinna_hfp.gtfs.parser;

import fi.hsl.suomenlinna_hfp.common.model.LatLng;
import fi.hsl.suomenlinna_hfp.gtfs.model.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GtfsParser {
    private static final CSVFormat GTFS_CSV_FORMAT = CSVFormat.RFC4180.withFirstRecordAsHeader();

    private GtfsParser() {}

    public static GtfsFeed parseGtfs(ZipInputStream inputStream) throws IOException {
        List<Route> routes = null;
        List<Calendar> calendars = null;
        List<CalendarDate> calendarDates = null;
        List<Stop> stops = null;
        List<StopTime> stopTimes = null;
        List<Trip> trips = null;

        ZipEntry entry;

        while ((entry = inputStream.getNextEntry()) != null) {
            switch (entry.getName()) {
                case "routes.txt":
                    routes = parseRoutes(createCSVParser(inputStream));
                    break;
                case "calendar.txt":
                    calendars = parseCalendars(createCSVParser(inputStream));
                    break;
                case "calendar_dates.txt":
                    calendarDates = parseCalendarDates(createCSVParser(inputStream));
                    break;
                case "stops.txt":
                    stops = parseStops(createCSVParser(inputStream));
                    break;
                case "stop_times.txt":
                    stopTimes = parseStopTimes(createCSVParser(inputStream));
                    break;
                case "trips.txt":
                    trips = parseTrips(createCSVParser(inputStream));
                    break;
                default:
                    inputStream.closeEntry();
            }
        }

        return new GtfsFeed(routes, calendars, calendarDates, stops, stopTimes, trips);
    }

    private static List<Route> parseRoutes(CSVParser parser) {
        List<Route> routes = new ArrayList<>();
        parser.forEach(csvRecord -> {
            routes.add(
                    new Route(
                            csvRecord.get("route_id"),
                            csvRecord.get("route_short_name"),
                            csvRecord.get("route_long_name")
                    )
            );
        });

        return routes;
    }

    private static List<Calendar> parseCalendars(CSVParser parser) {
        List<Calendar> calendars = new ArrayList<>();
        parser.forEach(csvRecord -> {
            calendars.add(
                    new Calendar(
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
                    )
            );
        });

        return calendars;
    }

    private static List<CalendarDate> parseCalendarDates(CSVParser parser) {
        List<CalendarDate> calendarDates = new ArrayList<>();
        parser.forEach(csvRecord -> {
            calendarDates.add(
                    new CalendarDate(
                            csvRecord.get("service_id"),
                            LocalDate.parse(csvRecord.get("date"), DateTimeFormatter.BASIC_ISO_DATE),
                            Integer.parseInt(csvRecord.get("exception_type"))
                    )
            );
        });

        return calendarDates;
    }

    private static List<Stop> parseStops(CSVParser parser) {
        List<Stop> stops = new ArrayList<>();
        parser.forEach(csvRecord -> {
            stops.add(
                    new Stop(
                            csvRecord.get("stop_id"),
                            new LatLng(Double.parseDouble(csvRecord.get("stop_lat")), Double.parseDouble(csvRecord.get("stop_lon")))
                    )
            );
        });

        return stops;
    }

    private static List<StopTime> parseStopTimes(CSVParser parser) {
        List<StopTime> stopTimes = new ArrayList<>();
        parser.forEach(csvRecord -> {
            stopTimes.add(
                    new StopTime(
                            csvRecord.get("\uFEFFtrip_id"),
                            parseTime(csvRecord.get("arrival_time")),
                            parseTime(csvRecord.get("departure_time")),
                            csvRecord.get("stop_id"),
                            Integer.parseInt(csvRecord.get("stop_sequence")),
                            Integer.parseInt(csvRecord.get("pickup_type")),
                            Integer.parseInt(csvRecord.get("drop_off_type"))
                    )
            );
        });

        return stopTimes;
    }

    private static List<Trip> parseTrips(CSVParser parser) {
        List<Trip> trips = new ArrayList<>();
        parser.forEach(csvRecord -> {
            trips.add(
                    new Trip(
                            csvRecord.get("route_id"),
                            csvRecord.get("service_id"),
                            csvRecord.get("trip_id"),
                            Integer.parseInt(csvRecord.get("direction_id")),
                            csvRecord.get("trip_headsign")
                    )
            );
        });

        return trips;
    }

    private static Integer parseTime(String gtfsTime) {
        String[] parts = gtfsTime.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid time format: "+gtfsTime);
        }
        return Integer.parseInt(parts[0]) * 60 * 60 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]);
    }

    private static CSVParser createCSVParser(InputStream inputStream) throws IOException {
        return CSVParser.parse(inputStream, UTF_8, GTFS_CSV_FORMAT);
    }
}
