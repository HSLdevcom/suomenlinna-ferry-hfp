package fi.hsl.suomenlinna_hfp.sbdrive.provider;

import fi.hsl.suomenlinna_hfp.common.VehiclePositionProvider;
import fi.hsl.suomenlinna_hfp.sbdrive.model.VehicleState;

import java.util.function.Consumer;

public abstract class VehicleStateProvider implements VehiclePositionProvider {
    @Override
    public void start(VehiclePositionConsumer vehiclePositionConsumer, VehicleMetadataConsumer vehicleMetadataConsumer, Consumer<Throwable> onError) throws Throwable {
        start(vehiclePositionConsumer::accept, onError);
    }

    public abstract void stop();
    public abstract void start(Consumer<VehicleState> onVehicleState, Consumer<Throwable> onError);
    public abstract long getLastReceivedTime();
}
