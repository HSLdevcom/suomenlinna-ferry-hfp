package fi.hsl.suomenlinna_hfp.gtfs.provider;

import fi.hsl.suomenlinna_hfp.common.utils.DaemonThreadFactory;
import fi.hsl.suomenlinna_hfp.gtfs.model.GtfsFeed;
import fi.hsl.suomenlinna_hfp.gtfs.parser.GtfsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class HttpGtfsProvider implements GtfsProvider {
    private static final Logger LOG = LoggerFactory.getLogger(HttpGtfsProvider.class);

    private volatile long lastUpdate = 0;

    private volatile ScheduledFuture scheduledFuture;

    private final HttpClient httpClient;

    private final String url;
    private final long interval;
    private final TimeUnit timeUnit;

    private final Collection<String> routeIds;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());

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

    private HttpResponse<Path> downloadToFile(Path path) throws IOException, InterruptedException {
        LOG.info("Downloading GTFS feed from {}", url);

        final long startTime = System.nanoTime();

        final HttpResponse<Path> response = httpClient.send(HttpRequest.newBuilder().uri(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofFile(path));

        LOG.info("GTFS feed downloaded in {}ms", (System.nanoTime() - startTime) / 1000000);
        return response;
    }

    private GtfsFeed parseGtfs(File file) throws IOException {
        LOG.info("Parsing GTFS feed");

        final long startTime = System.nanoTime();

        final GtfsFeed gtfsFeed = GtfsParser.parseGtfs(file, routeIds);

        LOG.info("GTFS feed parsed in {}ms", (System.nanoTime() - startTime) / 1000000);
        return gtfsFeed;
    }

    @Override
    public void start(Consumer<GtfsFeed> callback, Consumer<Throwable> onError) {
        if (isActive()) {
            throw new IllegalStateException("HttpGtfsProvider is already started");
        }

        scheduledFuture = executorService.scheduleAtFixedRate(() -> {
            Path gtfsFile = null;

            try {
                gtfsFile = Files.createTempFile("gtfs", ".zip");

                HttpResponse<Path> response = downloadToFile(gtfsFile);
                GtfsFeed gtfsFeed = parseGtfs(response.body().toFile());

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
