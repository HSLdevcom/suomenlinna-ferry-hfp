package fi.hsl.suomenlinna_hfp.gtfs.provider;

import fi.hsl.suomenlinna_hfp.gtfs.model.GtfsFeed;
import fi.hsl.suomenlinna_hfp.gtfs.parser.GtfsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.zip.ZipInputStream;

public class HttpGtfsProvider implements GtfsProvider {
    private static final Logger LOG = LoggerFactory.getLogger(HttpGtfsProvider.class);

    private volatile long lastUpdate = 0;

    private volatile ScheduledFuture scheduledFuture;

    private final HttpClient httpClient;

    private final String url;
    private final long interval;
    private final TimeUnit timeUnit;

    private final Collection<String> routeIds;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor((runnable) -> {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        return t;
    });

    public HttpGtfsProvider(HttpClient httpClient, String url, long interval, TimeUnit timeUnit, Collection<String> routeIds) {
        this.httpClient = httpClient;
        this.url = url;
        this.interval = interval;
        this.timeUnit = timeUnit;
        this.routeIds = routeIds;
    }

    @Override
    public long getLastUpdateTime() {
        return lastUpdate;
    }

    @Override
    public boolean isActive() {
        return scheduledFuture != null;
    }

    @Override
    public void start(Consumer<GtfsFeed> callback, Consumer<Throwable> onError) {
        if (isActive()) {
            throw new IllegalStateException("HttpGtfsProvider is already started");
        }

        scheduledFuture = executorService.scheduleAtFixedRate(() -> {
            Path gtfsFile = null;

            try {
                long startTime = System.nanoTime();
                LOG.info("Downloading GTFS feed from {}", url);

                gtfsFile = Files.createTempFile("gtfs", ".zip");
                HttpResponse<Path> response = httpClient.send(HttpRequest.newBuilder().uri(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofFile(gtfsFile));

                GtfsFeed gtfsFeed = GtfsParser.parseGtfs(response.body().toFile(), routeIds);

                LOG.info("GTFS feed downloaded in {}ms", (System.nanoTime() - startTime) / 1000000);

                lastUpdate = System.nanoTime();
                callback.accept(gtfsFeed);
            } catch (IOException | InterruptedException e) {
                LOG.warn("Failed to download GTFS feed from {}", url, e);
                onError.accept(e);
            } finally {
                if (gtfsFile != null) {
                    try {
                        Files.deleteIfExists(gtfsFile);
                    } catch (IOException e) {
                        LOG.warn("Failed to delete temporary file {}", gtfsFile.getFileName().toString(), e);
                    }
                }
            }
        }, 0, interval, timeUnit);
    }

    @Override
    public void stop() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
        scheduledFuture = null;
    }
}
