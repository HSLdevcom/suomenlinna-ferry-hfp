package fi.hsl.suomenlinna_hfp.common;

import fi.hsl.suomenlinna_hfp.common.model.VehicleMetadata;
import fi.hsl.suomenlinna_hfp.common.model.VehiclePosition;

import java.util.function.Consumer;

public interface VehiclePositionProvider {
    void start(VehiclePositionConsumer vehiclePositionConsumer, VehicleMetadataConsumer vehicleMetadataConsumer, Consumer<Throwable> onError) throws Throwable;
    void stop();
    /**
     * Get the time when last vehicle position was received
     * @return Time in nanoseconds (as returned by {@link System#nanoTime()})
     */
    long getLastReceivedTime();

    @FunctionalInterface
    interface VehiclePositionConsumer {
        void accept(VehiclePosition vehiclePosition);
    }

    @FunctionalInterface
    interface VehicleMetadataConsumer {
        void accept(VehicleMetadata vehiclePosition);
    }
}
