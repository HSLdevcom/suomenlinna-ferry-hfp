package fi.hsl.suomenlinna_hfp.model.gtfs;

import java.time.LocalDate;
import java.util.Objects;

public class CalendarDate {
    private String serviceId;
    private LocalDate date;
    private Integer exceptionType; // 1 = service has been added for the specified date, 2 = service has been removed for the specified date

    public CalendarDate(String serviceId, LocalDate date, Integer exceptionType) {
        this.serviceId = serviceId;
        this.date = date;
        this.exceptionType = exceptionType;
    }

    public String getServiceId() {
        return serviceId;
    }

    public LocalDate getDate() {
        return date;
    }

    /**
     * Exception type. 1 = service has been added for the date, 2 = service has been removed for the date
     * @return Exception type
     */
    public Integer getExceptionType() {
        return exceptionType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CalendarDate that = (CalendarDate) o;
        return Objects.equals(serviceId, that.serviceId) &&
                Objects.equals(date, that.date) &&
                Objects.equals(exceptionType, that.exceptionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId, date, exceptionType);
    }

    @Override
    public String toString() {
        return "CalendarDate{" +
                "serviceId='" + serviceId + '\'' +
                ", date=" + date +
                ", exceptionType=" + exceptionType +
                '}';
    }
}
