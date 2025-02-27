package fi.hsl.suomenlinna_hfp.gtfs.provider;

import fi.hsl.suomenlinna_hfp.common.utils.DaemonThreadFactory;
import fi.hsl.suomenlinna_hfp.gtfs.model.GtfsFeed;
import fi.hsl.suomenlinna_hfp.gtfs.parser.GtfsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private final long interval;
    private final TimeUnit timeUnit;

    private final Collection<String> routeIds;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());

    public HttpGtfsProvider(HttpClient httpClient, String url, long interval, TimeUnit timeUnit, Collection<String> routeIds) {
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
            Path gtfsFile = Paths.get("/hsl.zip");

            try {
                GtfsFeed gtfsFeed = parseGtfs(gtfsFile.toFile());

                lastUpdate = System.nanoTime();
                callback.accept(gtfsFeed);
            } catch (IOException e) {
                LOG.warn("Failed to read GTFS file", e);
                onError.accept(e);
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
