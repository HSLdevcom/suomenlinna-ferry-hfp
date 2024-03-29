package fi.hsl.suomenlinna_hfp.gtfs.provider;

import fi.hsl.suomenlinna_hfp.common.utils.DaemonThreadFactory;
import fi.hsl.suomenlinna_hfp.gtfs.model.GtfsFeed;
import fi.hsl.suomenlinna_hfp.gtfs.parser.GtfsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FileGtfsProvider implements GtfsProvider {
    private static final Logger LOG = LoggerFactory.getLogger(FileGtfsProvider.class);

    private volatile long lastUpdate = 0;

    private volatile ScheduledFuture scheduledFuture;

    private final String file;
    private final long interval;
    private final TimeUnit timeUnit;

    private final Collection<String> routeIds;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());

    public FileGtfsProvider(String file, long interval, TimeUnit timeUnit, Collection<String> routeIds) {
        this.file = file;
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
            throw new IllegalStateException("FileGtfsProvider is already started");
        }

        scheduledFuture = executorService.scheduleAtFixedRate(() -> {
            try {
                GtfsFeed gtfsFeed = GtfsParser.parseGtfs(new File(file), routeIds);
                lastUpdate = System.nanoTime();
                callback.accept(gtfsFeed);
            } catch (IOException e) {
                LOG.warn("Failed to parse GTFS feed from {}", file, e);
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
