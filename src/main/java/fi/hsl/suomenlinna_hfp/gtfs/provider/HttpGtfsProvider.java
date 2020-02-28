package fi.hsl.suomenlinna_hfp.gtfs.provider;

import fi.hsl.suomenlinna_hfp.gtfs.model.GtfsFeed;
import fi.hsl.suomenlinna_hfp.gtfs.parser.GtfsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
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

    private final String url;
    private final long interval;
    private final TimeUnit timeUnit;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor((runnable) -> {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        return t;
    });

    public HttpGtfsProvider(String url, long interval, TimeUnit timeUnit) {
        this.url = url;
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
            throw new IllegalStateException("HttpGtfsProvider is already started");
        }

        scheduledFuture = executorService.scheduleAtFixedRate(() -> {
            ZipInputStream inputStream = null;
            try {
                long startTime = System.nanoTime();
                LOG.info("Downloading GTFS feed from {}", url);

                URLConnection conn = new URL(url).openConnection();
                conn.connect();

                inputStream = new ZipInputStream(new BufferedInputStream(conn.getInputStream(), 128 * 1024));

                GtfsFeed gtfsFeed = GtfsParser.parseGtfs(inputStream);

                LOG.info("GTFS feed downloaded in {}ms", (System.nanoTime() - startTime) / 1000000);

                lastUpdate = System.nanoTime();
                callback.accept(gtfsFeed);
            } catch (IOException e) {
                LOG.warn("Failed to download GTFS feed from {}", url, e);
                onError.accept(e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        LOG.error("Failed to close input stream?", e);
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
