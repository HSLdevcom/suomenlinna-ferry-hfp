package fi.hsl.suomenlinna_hfp.hfp.publisher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.hsl.suomenlinna_hfp.hfp.model.Payload;
import fi.hsl.suomenlinna_hfp.hfp.model.Topic;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MqttHfpPublisher implements HfpPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(MqttHfpPublisher.class);

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    private final String brokerUri;

    private MqttAsyncClient mqttAsyncClient;

    private int connectionLostCount = 0;

    /**
     *
     * @param brokerUri
     */
    public MqttHfpPublisher(String brokerUri) {
        this.brokerUri = brokerUri;
    }

    @Override
    public void connect(Runnable onSuccess, Consumer<Throwable> onError) throws MqttException {
        mqttAsyncClient = new MqttAsyncClient(brokerUri, MqttClient.generateClientId(), new MemoryPersistence());

        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setCleanSession(false);
        connectOptions.setAutomaticReconnect(true);
        connectOptions.setMqttVersion(4);
        mqttAsyncClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                LOG.info("Connected to {} (reconnect: {})", brokerUri, reconnect);
            }

            @Override
            public void connectionLost(Throwable cause) {
                LOG.warn("Connection lost to {}, attempting to reconnect...", brokerUri, cause);

                if (++connectionLostCount > 5) {
                    LOG.error("Connection lost to {} more than 5 times, aborting..", brokerUri);
                    onError.accept(new IOException("Connection lost to "+brokerUri));
                    try {
                        mqttAsyncClient.disconnectForcibly(0, 1000);
                    } catch (MqttException e) {}
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                LOG.debug("Message sent to {} to topics {}", brokerUri, Arrays.toString(token.getTopics()));
            }
        });
        mqttAsyncClient.connect(connectOptions, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                if (onError != null) {
                    onError.accept(exception);
                }
            }
        });
    }

    @Override
    public boolean isConnected() {
        return mqttAsyncClient != null && mqttAsyncClient.isConnected();
    }

    @Override
    public void publish(Topic topic, Payload payload) throws MqttException {
        MqttMessage mqttMessage = new MqttMessage(gson.toJson(Collections.singletonMap(topic.eventType.name(), payload)).getBytes(UTF_8));
        mqttMessage.setQos(1);

        mqttAsyncClient.publish(topic.toString(), mqttMessage);
    }
}
