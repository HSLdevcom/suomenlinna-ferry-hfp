package fi.hsl.suomenlinna_hfp.gtfs.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Calendar {
    private String serviceId;
    private Boolean monday;
    private Boolean tuesday;
    private Boolean wednesday;
    private Boolean thursday;
    private Boolean friday;
    private Boolean saturday;
    private Boolean sunday;
    private LocalDate startDate;
    private LocalDate endDate;

    public Calendar(String serviceId, Boolean monday, Boolean tuesday, Boolean wednesday, Boolean thursday, Boolean friday, Boolean saturday, Boolean sunday, LocalDate startDate, LocalDate endDate) {
        this.serviceId = serviceId;
        this.monday = monday;
        this.tuesday = tuesday;
        this.wednesday = wednesday;
        this.thursday = thursday;
        this.friday = friday;
        this.saturday = saturday;
        this.sunday = sunday;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getServiceId() {
        return serviceId;
    }

    public Boolean getMonday() {
        return monday;
    }

    public Boolean getTuesday() {
        return tuesday;
    }

    public Boolean getWednesday() {
        return wednesday;
    }

    public Boolean getThursday() {
        return thursday;
    }

    public Boolean getFriday() {
        return friday;
    }

    public Boolean getSaturday() {
        return saturday;
    }

    public Boolean getSunday() {
        return sunday;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Set<DayOfWeek> getAvailableDaysOfWeek() {
        Map<DayOfWeek, Boolean> availableDaysOfWeek = new HashMap<>(7);
        availableDaysOfWeek.put(DayOfWeek.MONDAY, monday);
        availableDaysOfWeek.put(DayOfWeek.TUESDAY, tuesday);
        availableDaysOfWeek.put(DayOfWeek.WEDNESDAY, wednesday);
        availableDaysOfWeek.put(DayOfWeek.THURSDAY, thursday);
        availableDaysOfWeek.put(DayOfWeek.FRIDAY, friday);
        availableDaysOfWeek.put(DayOfWeek.SATURDAY, saturday);
        availableDaysOfWeek.put(DayOfWeek.SUNDAY, sunday);

        return availableDaysOfWeek.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Calendar calendar = (Calendar) o;
        return Objects.equals(serviceId, calendar.serviceId) &&
                Objects.equals(monday, calendar.monday) &&
                Objects.equals(tuesday, calendar.tuesday) &&
                Objects.equals(wednesday, calendar.wednesday) &&
                Objects.equals(thursday, calendar.thursday) &&
                Objects.equals(friday, calendar.friday) &&
                Objects.equals(saturday, calendar.saturday) &&
                Objects.equals(sunday, calendar.sunday) &&
                Objects.equals(startDate, calendar.startDate) &&
                Objects.equals(endDate, calendar.endDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId, monday, tuesday, wednesday, thursday, friday, saturday, sunday, startDate, endDate);
    }

    @Override
    public String toString() {
        return "Calendar{" +
                "serviceId='" + serviceId + '\'' +
                ", monday=" + monday +
                ", tuesday=" + tuesday +
                ", wednesday=" + wednesday +
                ", thursday=" + thursday +
                ", friday=" + friday +
                ", saturday=" + saturday +
                ", sunday=" + sunday +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
}
