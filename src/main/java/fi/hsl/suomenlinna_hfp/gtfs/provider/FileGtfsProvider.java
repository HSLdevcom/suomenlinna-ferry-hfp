package fi.hsl.suomenlinna_hfp.gtfs.provider;

import fi.hsl.suomenlinna_hfp.gtfs.model.GtfsFeed;
import fi.hsl.suomenlinna_hfp.gtfs.parser.GtfsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.zip.ZipInputStream;

public class FileGtfsProvider implements GtfsProvider {
    private static final Logger LOG = LoggerFactory.getLogger(FileGtfsProvider.class);

    private volatile long lastUpdate = 0;

    private volatile ScheduledFuture scheduledFuture;

    private final String file;
    private final long interval;
    private final TimeUnit timeUnit;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public FileGtfsProvider(String file, long interval, TimeUnit timeUnit) {
        this.file = file;
        this.interval = interval;
        this.timeUnit = timeUnit;
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
                ZipInputStream inputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(new File(file))));

                GtfsFeed gtfsFeed = GtfsParser.parseGtfs(inputStream);
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
