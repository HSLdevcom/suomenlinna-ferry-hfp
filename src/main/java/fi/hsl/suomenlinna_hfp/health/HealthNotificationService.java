package fi.hsl.suomenlinna_hfp.health;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class HealthNotificationService {
    private final String postEndpoint;

    private final HttpClient httpClient;

    public HealthNotificationService(String postEndpoint, HttpClient httpClient) {
        this.postEndpoint = postEndpoint;
        this.httpClient = httpClient;
    }

    void notifySlackChannel() throws IOException {
        final String message = "{\"text\": \"Suomenlinnan lautoissa ongelmia!\"}";

        final HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(message, StandardCharsets.UTF_8))
                .setHeader("Accept", "application/json")
                .setHeader("Content-Type", "application/json")
                .build();

        try {
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (InterruptedException e) {
            throw new IOException("HTTP client was interrupted", e);
        }
    }
}
