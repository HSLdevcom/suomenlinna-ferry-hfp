package fi.hsl.suomenlinna_hfp.hfp.publisher;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hsl.suomenlinna_hfp.hfp.model.Payload;
import fi.hsl.suomenlinna_hfp.hfp.model.Topic;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class MqttHfpPublisher implements HfpPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(MqttHfpPublisher.class);

    private final ObjectMapper objectMapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    private final String brokerUri;
    private final int maxReconnects;

    private MqttAsyncClient mqttAsyncClient;

    private int connectionLostCount = 0;

    private AtomicLong lastSentTime = new AtomicLong(System.nanoTime());

    /**
     *
     * @param brokerUri
     * @param maxReconnects Maximum amount of reconnect attempts, -1 if unlimited
     */
    public MqttHfpPublisher(String brokerUri, int maxReconnects) {
        this.brokerUri = brokerUri;
        this.maxReconnects = maxReconnects;
    }

    @Override
    public void connect(Runnable onSuccess, Consumer<Throwable> onError) throws MqttException {
        mqttAsyncClient = new MqttAsyncClient(brokerUri, MqttClient.generateClientId(), new MemoryPersistence());

        DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(50);
        bufferOptions.setDeleteOldestMessages(true);
        mqttAsyncClient.setBufferOpts(bufferOptions);

        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setCleanSession(false);
        connectOptions.setAutomaticReconnect(true);
        connectOptions.setMqttVersion(4);
        connectOptions.setMaxInflight(100); //TODO: should be configurable
        mqttAsyncClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                LOG.info("Connected to {} (reconnect: {})", brokerUri, reconnect);
            }

            @Override
            public void connectionLost(Throwable cause) {
                LOG.warn("Connection lost to {}, attempting to reconnect...", brokerUri, cause);

                if (++connectionLostCount > maxReconnects && maxReconnects != -1) {
                    LOG.error("Connection lost to {} more than {} times, aborting..", brokerUri, maxReconnects);
                    onError.accept(new IOException("Connection lost to "+brokerUri));
                    disconnect();
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                LOG.debug("Message sent to {} to topics {}", brokerUri, Arrays.toString(token.getTopics()));
                lastSentTime.set(System.nanoTime());
            }
        });
        mqttAsyncClient.connect(connectOptions, null, new IMqttActionListener() {
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
    public void disconnect() {
        if (mqttAsyncClient != null) {
            try {
                mqttAsyncClient.disconnectForcibly(1000, 1000);
            } catch (MqttException e) {
                LOG.warn("Failed to disconnect MQTT client", e);
            }
            try {
                mqttAsyncClient.close(true);
            } catch (MqttException e) {
                LOG.warn("Failed to close MQTT client", e);
            }
            mqttAsyncClient = null;
        }
    }

    @Override
    public boolean isConnected() {
        return mqttAsyncClient != null && mqttAsyncClient.isConnected();
    }

    @Override
    public void publish(Topic topic, Payload payload) throws MqttException {
        LOG.info("Not publishing message to topic {}. Payload: {}", topic, payload);
        /*
        try {
            MqttMessage mqttMessage = new MqttMessage(objectMapper.writeValueAsBytes(Collections.singletonMap(topic.eventType.name(), payload)));
            mqttMessage.setQos(1);

            mqttAsyncClient.publish(topic.toString(), mqttMessage);
        } catch (JsonProcessingException e) {
            LOG.warn("Failed to serialize HFP message as JSON", e);
        }
         */
    }

    @Override
    public long getLastSentTime() {
        return lastSentTime.get();
    }
}
