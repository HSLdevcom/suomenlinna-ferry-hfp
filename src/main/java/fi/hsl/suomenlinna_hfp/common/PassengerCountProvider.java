package fi.hsl.suomenlinna_hfp.common;

import fi.hsl.suomenlinna_hfp.common.model.PassengerCount;

import java.time.LocalDateTime;

public interface PassengerCountProvider {
    PassengerCount getPassengerCountByStartTimeAndStopCode(LocalDateTime startTime, String stopCode) throws Throwable;
}
