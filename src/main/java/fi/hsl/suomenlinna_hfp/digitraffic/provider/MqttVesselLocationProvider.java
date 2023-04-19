package fi.hsl.suomenlinna_hfp.digitraffic.provider;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.hsl.suomenlinna_hfp.digitraffic.model.VesselLocation;
import fi.hsl.suomenlinna_hfp.digitraffic.model.VesselMetadata;
import fi.hsl.suomenlinna_hfp.digitraffic.model.VesselsStatus;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class MqttVesselLocationProvider extends VesselLocationProvider {
    private static final Logger LOG = LoggerFactory.getLogger(MqttVesselLocationProvider.class);

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final String brokerUri;
    private final String username;
    private final String password;

    private final Collection<String> mmsis;

    private MqttAsyncClient mqttAsyncClient;

    private final AtomicLong lastReceivedTime = new AtomicLong(System.nanoTime());

    public MqttVesselLocationProvider(String brokerUri, String username, String password, Collection<String> mmsis) {
        this.brokerUri = brokerUri;
        this.username = username;
        this.password = password;

        this.mmsis = mmsis;
    }

    @Override
    public void stop() {
        try {
            mqttAsyncClient.disconnect(0);
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

    @Override
    public void start(Consumer<VesselLocation> locationConsumer, Consumer<VesselMetadata> metadataConsumer, Consumer<Throwable> onConnectionFailed) throws MqttException {
        mqttAsyncClient = new MqttAsyncClient(brokerUri, MqttAsyncClient.generateClientId());

        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setCleanSession(true);
        connectOptions.setAutomaticReconnect(true);
        //MQTT keep-alive should be enabled for meri.digitraffic.fi broker
        connectOptions.setKeepAliveInterval(30);
        connectOptions.setMqttVersion(4);

        //Only add credentials if they are present
        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            connectOptions.setUserName(username);
            connectOptions.setPassword(password.toCharArray());
        }

        mqttAsyncClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                LOG.info("Connected to {} (reconnect: {})", brokerUri, reconnect);

                String[] topics = mmsis.stream().map(mmsi -> "vessels-v2/" + mmsi + "/+/#").toArray(String[]::new);
                int[] qos = new int[topics.length];
                Arrays.fill(qos, 0);

                try {
                    mqttAsyncClient.subscribe(topics, qos).waitForCompletion();
                    mqttAsyncClient.subscribe("vessels-v2/status", 0).waitForCompletion();
                } catch (MqttException e) {
                    LOG.error("Failed to subscribe MQTT topics {} with QoS {}", topics, qos);
                    onConnectionFailed.accept(e);

                    stop();
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                LOG.warn("Connection lost to {}, attempting to reconnect...", brokerUri, cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                if (topic.contains("metadata")) {
                    try {
                        injectMmsi(topic);
                        VesselMetadata vesselMetadata = objectMapper.readValue(message.getPayload(), VesselMetadata.class);
                        metadataConsumer.accept(vesselMetadata);
                    } catch (IOException e) {
                        LOG.warn("Failed to parse vessel metadata", e);
                    } finally {
                        objectMapper.setInjectableValues(new InjectableValues.Std());
                    }
                }
                if (topic.contains("location")) {
                    try {
                        injectMmsi(topic);
                        VesselLocation vesselLocation = objectMapper.readValue(message.getPayload(), VesselLocation.class);
                        locationConsumer.accept(vesselLocation);
                    } catch (IOException e) {
                        LOG.warn("Failed to parse vessel location", e);
                    } finally {
                        objectMapper.setInjectableValues(new InjectableValues.Std());
                    }
                }
                if (topic.contains("status")) {
                    try {
                        VesselsStatus vesselsStatus = objectMapper.readValue(message.getPayload(), VesselsStatus.class);
                        if (!vesselsStatus.everythingOk()) {
                            LOG.warn("There was something wrong in MQTT status message, is the connection working correctly? {}", vesselsStatus);
                        }
                    } catch (IOException e) {
                        LOG.warn("Failed to parse vessels/status message", e);
                    }
                }
                lastReceivedTime.set(System.nanoTime());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });
        mqttAsyncClient.connect(connectOptions, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                onConnectionFailed.accept(exception);
            }
        });
    }

    private void injectMmsi(String topic) {
        String mmsi = topic.split("/")[1];
        InjectableValues injectableValues = new InjectableValues.Std().addValue("mmsi", Integer.parseInt(mmsi));
        objectMapper.setInjectableValues(injectableValues);

    }

    @Override
    public long getLastReceivedTime() {
        return lastReceivedTime.get();
    }
}
