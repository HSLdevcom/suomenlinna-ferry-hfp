package fi.hsl.suomenlinna_hfp.digitraffic.provider;

import fi.hsl.suomenlinna_hfp.digitraffic.model.VesselLocation;
import fi.hsl.suomenlinna_hfp.digitraffic.model.VesselMetadata;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Collection;
import java.util.function.Consumer;

public interface VesselLocationProvider {
    void stop();
    void start(Collection<String> mmsis, Consumer<VesselLocation> locationConsumer, Consumer<VesselMetadata> metadataConsumer, Consumer<Throwable> onError) throws Throwable;
}
