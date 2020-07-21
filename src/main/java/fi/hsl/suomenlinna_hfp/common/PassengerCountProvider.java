package fi.hsl.suomenlinna_hfp.common;

import fi.hsl.suomenlinna_hfp.common.model.PassengerCount;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

public interface PassengerCountProvider {
    CompletableFuture<PassengerCount> getPassengerCountByStartTimeAndStopCode(LocalDateTime startTime, String stopCode);
}
