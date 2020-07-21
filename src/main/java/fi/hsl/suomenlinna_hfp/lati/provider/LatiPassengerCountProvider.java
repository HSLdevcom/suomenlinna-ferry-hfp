package fi.hsl.suomenlinna_hfp.lati.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.*;
import fi.hsl.suomenlinna_hfp.common.PassengerCountProvider;
import fi.hsl.suomenlinna_hfp.common.model.PassengerCount;
import fi.hsl.suomenlinna_hfp.lati.model.LatiPassengerCount;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches passenger count for Suomenlinna ferry from HSL Lati API
 */
public class LatiPassengerCountProvider implements PassengerCountProvider {
    private static final Pattern FIRST_NUMBERS_PATTERN = Pattern.compile("\\d+");

    private static final String API = "/lati/departure?date=%s&stop=%s&variables=NOUSIJAT&variables=ALUS";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient;
    private final String endpoint;

    private final Map<String, String> vesselNameToMmsi;
    private final Map<String, Integer> mmsiToMaxPassengerCount;

    private final AsyncLoadingCache<PassengerCountKey, LatiPassengerCount> passengerCountCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .buildAsync(this::getPassengerCount);

    public LatiPassengerCountProvider(HttpClient httpClient, String endpoint, Map<String, String> vesselNameToMmsi, Map<String, Integer> mmsiToMaxPassengerCount) {
        this.httpClient = httpClient;
        this.endpoint = endpoint;
        this.vesselNameToMmsi = vesselNameToMmsi;
        this.mmsiToMaxPassengerCount = mmsiToMaxPassengerCount;
    }

    //Use URLConnection as Java 11 HttpClient seems to have problems with the API
    private LatiPassengerCount fetchPassengerCount(String url) throws IOException {
        try (InputStream is = new BufferedInputStream(new URL(url).openStream())) {
            return objectMapper.readValue(is, LatiPassengerCount.class);
        }
    }

    private LatiPassengerCount getPassengerCount(PassengerCountKey passengerCountKey) throws IOException {
        final LocalDateTime dateTime = passengerCountKey.dateTime;
        final String stopCode = passengerCountKey.stopCode;

        //Stop codes begin with 'H' in Helsinki -> take only first numbers from the code
        final Matcher matcher = FIRST_NUMBERS_PATTERN.matcher(stopCode);
        matcher.find();
        final String stopCodeNumbers = matcher.group();

        final String url = endpoint + String.format(API, dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace('T', '+'), stopCodeNumbers);

                /*final HttpResponse<InputStream> response = httpClient.send(HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .timeout(Duration.ofMillis(500))
                                .build(),
                        HttpResponse.BodyHandlers.buffering(HttpResponse.BodyHandlers.ofInputStream(), 1024));*/

        final LatiPassengerCount latiPassengerCount = fetchPassengerCount(url); //objectMapper.readValue(response.body(), LatiPassengerCount.class);
        if (latiPassengerCount.passengers == null) {
            return null;
        }

        return latiPassengerCount;
    }

    /**
     * Gets passenger count for ferry that departed at the specified time to specific direction
     * @param dateTime Departure time of the ferry
     * @param stopCode Stop code of the departure stop
     * @return
     */
    public CompletableFuture<PassengerCount> getPassengerCount(LocalDateTime dateTime, String stopCode) {
        return passengerCountCache.get(new PassengerCountKey(dateTime, stopCode))
                .thenApply(latiPassengerCount -> {
                    if (latiPassengerCount == null) {
                        return null;
                    }

                    final String mmsi = vesselNameToMmsi.get(latiPassengerCount.vessel);

                    return mmsi == null ?
                        null :
                        new PassengerCount(
                            mmsi,
                            latiPassengerCount.passengers,
                            mmsiToMaxPassengerCount.getOrDefault(mmsi, -1)
                        );
                });
    }

    @Override
    public CompletableFuture<PassengerCount> getPassengerCountByStartTimeAndStopCode(LocalDateTime startTime, String stopCode) {
        return getPassengerCount(startTime, stopCode);
    }

    //Used as cache key
    private static class PassengerCountKey {
        private final LocalDateTime dateTime;
        private final String stopCode;

        public PassengerCountKey(LocalDateTime dateTime, String stopCode) {
            this.dateTime = dateTime;
            this.stopCode = stopCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PassengerCountKey that = (PassengerCountKey) o;
            return Objects.equals(dateTime, that.dateTime) &&
                    Objects.equals(stopCode, that.stopCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dateTime, stopCode);
        }
    }
}
