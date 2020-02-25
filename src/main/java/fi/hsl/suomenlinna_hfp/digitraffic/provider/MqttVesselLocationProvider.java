package fi.hsl.suomenlinna_hfp.digitraffic.provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.hsl.suomenlinna_hfp.common.model.LatLng;
import fi.hsl.suomenlinna_hfp.digitraffic.model.VesselLocation;
import fi.hsl.suomenlinna_hfp.digitraffic.model.VesselMetadata;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

public class MqttVesselLocationProvider implements VesselLocationProvider {
    private static final Logger LOG = LoggerFactory.getLogger(MqttVesselLocationProvider.class);

    private final Gson gson = new GsonBuilder().registerTypeAdapter(LatLng.class, new LatLng.LatLngDeserializer()).create();

    private final String brokerUri;
    private final String username;
    private final String password;

    private MqttAsyncClient mqttAsyncClient;

    public MqttVesselLocationProvider(String brokerUri, String username, String password) {
        this.brokerUri = brokerUri;
        this.username = username;
        this.password = password;
    }

    @Override
    public void stop() throws MqttException {
        mqttAsyncClient.disconnect(0);
        mqttAsyncClient = null;
    }

    @Override
    public void start(Collection<String> mmsis, Consumer<VesselLocation> locationConsumer, Consumer<VesselMetadata> metadataConsumer, Consumer<Throwable> onConnectionFailed) throws MqttException {
        mqttAsyncClient = new MqttAsyncClient(brokerUri, MqttAsyncClient.generateClientId());

        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setCleanSession(true);
        connectOptions.setAutomaticReconnect(true);
        connectOptions.setKeepAliveInterval(0);
        connectOptions.setMqttVersion(4);
        connectOptions.setUserName(username);
        connectOptions.setPassword(password.toCharArray());

        mqttAsyncClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                LOG.info("Connected to {} (reconnect: {})", brokerUri, reconnect);

                String[] topics = mmsis.stream().map(mmsi -> "vessels/" + mmsi + "/+/#").toArray(String[]::new);
                int[] qos = new int[topics.length];
                Arrays.fill(qos, 0);

                try {
                    mqttAsyncClient.subscribe(topics, qos).waitForCompletion();
                } catch (MqttException e) {
                    LOG.error("Failed to subscribe MQTT topics {} with QoS {}", topics, qos);
                    onConnectionFailed.accept(e);

                    try {
                        stop();
                    } catch (MqttException ex) {}
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                LOG.warn("Connection lost to {}, attempting to reconnect...", brokerUri, cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                if (topic.contains("metadata")) {
                    metadataConsumer.accept(gson.fromJson(message.toString(), VesselMetadata.class));
                }
                if (topic.contains("locations")) {
                    locationConsumer.accept(gson.fromJson(message.toString(), VesselLocation.class));
                }
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
}
