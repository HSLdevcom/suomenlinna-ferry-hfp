package fi.hsl.suomenlinna_hfp.lati.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hsl.suomenlinna_hfp.common.PassengerCountProvider;
import fi.hsl.suomenlinna_hfp.common.model.PassengerCount;
import fi.hsl.suomenlinna_hfp.lati.model.LatiPassengerCount;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches passenger count for Suomenlinna ferry from HSL Lati API
 */
public class LatiPassengerCountProvider implements PassengerCountProvider {
    private static final Pattern FIRST_NUMBERS_PATTERN = Pattern.compile("\\d+");

    private static final String API = "/lati/departure?date=%s&stop=%s";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient;
    private final String endpoint;

    private final Map<String, String> vesselNameToMmsi;
    private final Map<String, Integer> mmsiToMaxPassengerCount;

    public LatiPassengerCountProvider(HttpClient httpClient, String endpoint, Map<String, String> vesselNameToMmsi, Map<String, Integer> mmsiToMaxPassengerCount) {
        this.httpClient = httpClient;
        this.endpoint = endpoint;
        this.vesselNameToMmsi = vesselNameToMmsi;
        this.mmsiToMaxPassengerCount = mmsiToMaxPassengerCount;
    }

    /**
     * Gets passenger count for ferry that departed at the specified time to specific direction
     * @param dateTime Departure time of the ferry
     * @param stopCode Stop code of the departure stop
     * @return
     */
    public PassengerCount getPassengerCount(LocalDateTime dateTime, String stopCode) throws IOException, InterruptedException {
        //Stop codes begin with 'H' in Helsinki -> take only first numbers from the code
        final Matcher matcher = FIRST_NUMBERS_PATTERN.matcher(stopCode);
        matcher.find();
        final String stopCodeNumbers = matcher.group();

        final HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder()
                .uri(URI.create(endpoint + String.format(API, dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace('T', '+'), stopCodeNumbers)))
                .build(),
                HttpResponse.BodyHandlers.ofString());

        final LatiPassengerCount latiPassengerCount = objectMapper.readValue(response.body(), LatiPassengerCount.class);
        if (latiPassengerCount.passengers == null) {
            return null;
        }

        return new PassengerCount(
                vesselNameToMmsi.get(latiPassengerCount.vessel),
                latiPassengerCount.passengers,
                mmsiToMaxPassengerCount.getOrDefault(latiPassengerCount.vessel, -1)
        );
    }

    @Override
    public PassengerCount getPassengerCountByStartTimeAndStopCode(LocalDateTime startTime, String stopCode) throws Throwable {
        return getPassengerCount(startTime, stopCode);
    }
}
