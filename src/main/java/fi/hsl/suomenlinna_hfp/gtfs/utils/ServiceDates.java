package fi.hsl.suomenlinna_hfp.gtfs.utils;

import xyz.malkki.gtfs.model.Calendar;
import xyz.malkki.gtfs.model.CalendarDate;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ServiceDates {
    private Map<String, Calendar> calendars;
    private Map<String, Map<LocalDate, CalendarDate>> exceptions;

    public ServiceDates(List<Calendar> calendars, List<CalendarDate> calendarDates) {
        this.calendars = calendars.stream().collect(Collectors.toMap(Calendar::getServiceId, Function.identity()));
        this.exceptions = calendarDates.stream()
                .collect(Collectors.groupingBy(CalendarDate::getServiceId))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        //Assumes that there are no multiple exceptions for a single date
                        entry -> entry.getValue().stream().collect(Collectors.toMap(CalendarDate::getDate, Function.identity())))
                );
    }

    /**
     * Gets a sorted set of dates when the service is running
     * @param serviceId Service ID
     * @return Set of dates sorted in ascending order
     */
    public SortedSet<LocalDate> getDatesForService(String serviceId) {
        SortedSet<LocalDate> dates = new TreeSet<>();

        if (exceptions.containsKey(serviceId)) {
            exceptions.get(serviceId).values().stream().filter(exception -> exception.getExceptionType() == 1).map(CalendarDate::getDate).forEach(dates::add);
        }

        if (calendars.containsKey(serviceId)) {
            Calendar calendar = calendars.get(serviceId);

            for (LocalDate date : calendar) {
                CalendarDate exception = exceptions.getOrDefault(serviceId, Collections.emptyMap()).get(date);

                if (calendar.getDaysOfWeek().contains(date.getDayOfWeek()) && (exception == null || exception.getExceptionType() != CalendarDate.EXCEPTION_TYPE_REMOVED)) {
                    dates.add(date);
                }
            }
        }

        return dates;
    }

    /**
     * Checks if the service is running on a specific date
     * @param serviceId Service ID
     * @param date Date
     * @return true if the service is running on the date, false if not
     */
    public boolean isServiceRunningOn(String serviceId, LocalDate date) {
        CalendarDate exception = exceptions.getOrDefault(serviceId, Collections.emptyMap()).get(date);
        if (exception == null) {
            Calendar calendar = calendars.get(serviceId);
            if (calendar == null) {
                throw new IllegalArgumentException("No calendar found for service ID: "+serviceId);
            }

            if (calendar.getDaysOfWeek().contains(date.getDayOfWeek())) {
                return (calendar.getStartDate().compareTo(date) <= 0) && (calendar.getEndDate().compareTo(date) >= 0);
            } else {
                return false;
            }
        } else if (exception.getExceptionType() == CalendarDate.EXCEPTION_TYPE_REMOVED) {
            return false;
        } else if (exception.getExceptionType() == CalendarDate.EXCEPTION_TYPE_ADDED) {
            return true;
        } else {
            throw new RuntimeException("Invalid calendar date exception type");
        }
    }
}
