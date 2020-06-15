package fi.hsl.suomenlinna_hfp.digitraffic.provider;

import fi.hsl.suomenlinna_hfp.common.VehiclePositionProvider;
import fi.hsl.suomenlinna_hfp.common.model.VehicleMetadata;
import fi.hsl.suomenlinna_hfp.common.model.VehiclePosition;
import fi.hsl.suomenlinna_hfp.digitraffic.model.VesselLocation;
import fi.hsl.suomenlinna_hfp.digitraffic.model.VesselMetadata;

import java.util.function.Consumer;

public abstract class VesselLocationProvider implements VehiclePositionProvider {
    @Override
    public void start(VehiclePositionConsumer vehiclePositionConsumer, VehicleMetadataConsumer vehicleMetadataConsumer, Consumer<Throwable> onError) throws Throwable {
        Consumer<VesselLocation> vesselLocationConsumer = vehiclePositionConsumer::accept;
        Consumer<VesselMetadata> vesselMetadataConsumer = vehicleMetadataConsumer::accept;
        start(vesselLocationConsumer, vesselMetadataConsumer, onError);
    }

    public abstract void stop();
    public abstract void start(Consumer<VesselLocation> locationConsumer, Consumer<VesselMetadata> metadataConsumer, Consumer<Throwable> onError) throws Throwable;
    public abstract long getLastReceivedTime();
}
