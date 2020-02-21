package fi.hsl.suomenlinna_hfp.gtfs.provider;

import fi.hsl.suomenlinna_hfp.gtfs.model.GtfsFeed;

import java.util.function.Consumer;

public interface GtfsProvider {
    long getLastUpdateTime();
    boolean isActive();
    void start(Consumer<GtfsFeed> onGtfsAvailable, Consumer<Throwable> onError);
    void stop();
}
