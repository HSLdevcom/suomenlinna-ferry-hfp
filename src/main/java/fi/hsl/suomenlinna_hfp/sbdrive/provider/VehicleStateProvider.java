package fi.hsl.suomenlinna_hfp.sbdrive.provider;

import fi.hsl.suomenlinna_hfp.sbdrive.model.VehicleState;

import java.util.function.Consumer;

public interface VehicleStateProvider {
    void stop();
    void start(Consumer<VehicleState> onVehicleState, Consumer<Throwable> onError);
    long getLastReceivedTime();
}
