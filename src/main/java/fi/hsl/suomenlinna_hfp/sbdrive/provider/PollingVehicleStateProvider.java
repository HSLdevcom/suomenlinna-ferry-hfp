package fi.hsl.suomenlinna_hfp.sbdrive.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hsl.suomenlinna_hfp.common.utils.DaemonThreadFactory;
import fi.hsl.suomenlinna_hfp.sbdrive.model.Token;
import fi.hsl.suomenlinna_hfp.sbdrive.model.VehicleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class PollingVehicleStateProvider extends VehicleStateProvider {
    private static final Logger LOG = LoggerFactory.getLogger(PollingVehicleStateProvider.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient;

    private final String endpoint;
    private final String apiKey;

    private final Duration pollInterval;

    private final ScheduledExecutorService executor;
    private ScheduledFuture scheduledFuture;

    private final AtomicLong lastReceivedTime = new AtomicLong(System.nanoTime());

    private Token token;

    public PollingVehicleStateProvider(HttpClient httpClient, String endpoint, String apiKey, Duration pollInterval) {
        this.httpClient = httpClient;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.pollInterval = pollInterval;

        this.executor = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
    }

    @Override
    public void stop() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
        scheduledFuture = null;
    }

    @Override
    public void start(Consumer<VehicleState> onVehicleState, Consumer<Throwable> onError) {
        scheduledFuture = executor.scheduleWithFixedDelay(() -> {
            try {
                if (token == null || token.hasExpired()) {
                    final HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(endpoint + "/v1/auth/token"))
                            .header("Authorization", String.format("Bearer %s", apiKey))
                            .build();

                    final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 401) {
                        LOG.warn("API returned HTTP 401 Unauthorized when fetching token, maybe the API key is invalid?");
                        return;
                    }

                    token = objectMapper.readValue(response.body(), Token.class);

                    LOG.info("Updated SB Drive Dispatcher API token, new expiration time: {}", Instant.ofEpochSecond(token.expiration).atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                }

                final HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint + "/v1/vehicle/states"))
                        .header("Authorization", String.format("Bearer %s", token.token))
                        .build();

                final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 401) { //Unauthorized
                    LOG.info("API returned HTTP 401 Unauthorized, maybe the token expired?");
                    //Set token to null to fetch new token on next poll cycle
                    token = null;
                    return;
                }

                final VehicleState[] vehicleStates = objectMapper.readValue(response.body(), VehicleState[].class);
                for (VehicleState vehicleState : vehicleStates) {
                    if (!"connected".equals(vehicleState.connection)) {
                        //Ignore vehicle positions if vehicle is not connected to the API
                        continue;
                    }
                    onVehicleState.accept(vehicleState);
                }

                lastReceivedTime.set(System.nanoTime());
            } catch (Exception e) {
                LOG.warn("Error polling vehicle states", e);
                onError.accept(e);
            }
        }, 0, pollInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public long getLastReceivedTime() {
        return lastReceivedTime.get();
    }
}
