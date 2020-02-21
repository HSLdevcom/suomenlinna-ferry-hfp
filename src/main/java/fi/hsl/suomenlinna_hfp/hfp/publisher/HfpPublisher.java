package fi.hsl.suomenlinna_hfp.hfp.publisher;

import fi.hsl.suomenlinna_hfp.hfp.model.Payload;
import fi.hsl.suomenlinna_hfp.hfp.model.Topic;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.function.Consumer;

public interface HfpPublisher {
    void connect(Runnable onSuccess, Consumer<Throwable> onError) throws Throwable;

    boolean isConnected();

    void publish(Topic topic, Payload payload) throws Throwable;
}
