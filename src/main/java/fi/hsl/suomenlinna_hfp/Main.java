package fi.hsl.suomenlinna_hfp;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import fi.hsl.suomenlinna_hfp.common.PassengerCountProvider;
import fi.hsl.suomenlinna_hfp.common.VehiclePositionProvider;
import fi.hsl.suomenlinna_hfp.digitraffic.provider.MqttVesselLocationProvider;
import fi.hsl.suomenlinna_hfp.gtfs.provider.GtfsProvider;
import fi.hsl.suomenlinna_hfp.gtfs.provider.HttpGtfsProvider;
import fi.hsl.suomenlinna_hfp.health.HealthNotificationService;
import fi.hsl.suomenlinna_hfp.health.HealthServer;
import fi.hsl.suomenlinna_hfp.hfp.model.Topic;
import fi.hsl.suomenlinna_hfp.hfp.model.VehicleId;
import fi.hsl.suomenlinna_hfp.hfp.publisher.MqttHfpPublisher;
import fi.hsl.suomenlinna_hfp.lati.provider.LatiPassengerCountProvider;
import fi.hsl.suomenlinna_hfp.sbdrive.provider.PollingVehicleStateProvider;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws Throwable {
        ConfigType configType = ConfigType.getByName(System.getenv("CONFIG"));
        Config config = ConfigFactory.load(configType.configFile);

        List<String> routes = config.getStringList("routes");

        double defaultMaxDistanceFromStop = config.getDouble("defaultMaxDistanceFromStop");

        Map<String, Double> maxDistanceFromStop = config.getObject("maxDistanceFromStop").entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> ((Number) entry.getValue().unwrapped()).doubleValue()));

        ZoneId timezone = ZoneId.of(config.getString("timezone"));

        Duration maxTimeBeforeDeparture = config.getDuration("tripProcessor.maxTimeBeforeDeparture");
        double maxTripDuration = config.getDouble("tripProcessor.maxTripDuration");

        TripProcessor tripProcessor = new TripProcessor(timezone, routes, maxDistanceFromStop, defaultMaxDistanceFromStop,
                maxTimeBeforeDeparture,
                maxTripDuration);

        String gtfsUrl = config.getString("gtfs.url");
        Duration gtfsPollInterval = config.getDuration("gtfs.pollInterval");

        String publisherBroker = config.getString("publisher.broker");
        int publisherMaxReconnects = config.getInt("publisher.maxReconnects");

        HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        GtfsProvider gtfsProvider = new HttpGtfsProvider(httpClient, gtfsUrl, gtfsPollInterval.getSeconds(), TimeUnit.SECONDS, routes);

        MqttHfpPublisher mqttHfpPublisher = new MqttHfpPublisher(publisherBroker, publisherMaxReconnects);

        Map<String, VehicleId> vehicleIdMap = null;
        Topic.TransportMode transportMode = null;
        VehiclePositionProvider vehiclePositionProvider = null;
        PassengerCountProvider passengerCountProvider = null;

        switch (configType) {
            case SUOMENLINNA:
                vehicleIdMap = config.getConfigList("mmsiToVehicleId").stream()
                        .collect(Collectors.toMap(c -> c.getString("mmsi"), c -> new VehicleId(c.getInt("operator"), c.getInt("vehicle"))));

                String meriDigitrafficBroker = config.getString("meriDigitraffic.broker");
                String meriDigitrafficUser = config.getString("meriDigitraffic.user");
                String meriDigitrafficPassword = config.getString("meriDigitraffic.password");

                transportMode = Topic.TransportMode.FERRY;
                vehiclePositionProvider = new MqttVesselLocationProvider(meriDigitrafficBroker, meriDigitrafficUser, meriDigitrafficPassword, vehicleIdMap.keySet());

                if (config.getBoolean("passengerCount.enabled")) {
                    String endpoint = config.getString("passengerCount.endpoint");

                    Map<String, String> vesselNameToMmsi = new HashMap<>();
                    Map<String, Integer> mmsiToMaxPassengerCount = new HashMap<>();
                    config.getConfigList("passengerCount.vessels").forEach(c -> {
                        String mmsi = c.getString("mmsi");

                        String vesselName = c.getString("name");
                        vesselNameToMmsi.put(vesselName, mmsi);
                        int maxPassengerCount = c.getInt("maxPassengers");
                        mmsiToMaxPassengerCount.put(mmsi, maxPassengerCount);
                    });

                    passengerCountProvider = new LatiPassengerCountProvider(httpClient, endpoint, vesselNameToMmsi, mmsiToMaxPassengerCount);
                }

                break;
            case SBDRIVE:
                vehicleIdMap = config.getConfigList("sbDriveVehicleIdToVehicleId").stream()
                        .collect(Collectors.toMap(c -> c.getString("sbDriveVehicleId"), c -> new VehicleId(c.getInt("operator"), c.getInt("vehicle"))));

                String sbDriveUrl = config.getString("sbDrive.url");
                String sbDriveApiKey = config.getString("sbDrive.apiKey");
                Duration sbDrivePollInterval = config.getDuration("sbDrive.pollInterval");

                transportMode = Topic.TransportMode.ROBOT;
                vehiclePositionProvider = new PollingVehicleStateProvider(httpClient, sbDriveUrl, sbDriveApiKey, sbDrivePollInterval);

                break;
        }

        if (config.getBoolean("health.enabled")) {
            HealthNotificationService healthNotificationService = null;
            if (!config.getString("health.postEndpoint").equals("")) {
                healthNotificationService = new HealthNotificationService(config.getString("health.postEndpoint"), httpClient);
            }

            createHealthServer(vehiclePositionProvider, mqttHfpPublisher, configType != ConfigType.SBDRIVE, healthNotificationService);
        }

        new HfpProducer(transportMode, vehicleIdMap, tripProcessor, gtfsProvider, vehiclePositionProvider, passengerCountProvider, mqttHfpPublisher).run();
    }

    private enum ConfigType {
        SUOMENLINNA("suomenlinna.conf"), SBDRIVE("sbdrive.conf");

        private final String configFile;

        ConfigType(String configFile) {
            this.configFile = configFile;
        }

        public static ConfigType getByName(String name) {
            try {
                return ConfigType.valueOf(name);
            } catch (NullPointerException | IllegalArgumentException e) {
                return ConfigType.SUOMENLINNA;
            }
        }
    }

    private static void createHealthServer(VehiclePositionProvider vehiclePositionProvider, MqttHfpPublisher mqttHfpPublisher, boolean publisherHealthCheck, HealthNotificationService healthNotificationService) throws IOException {
        HealthServer healthServer = healthNotificationService == null ? new HealthServer(8080) : new HealthServer(8080, healthNotificationService);
        healthServer.addCheck(() -> System.nanoTime() - vehiclePositionProvider.getLastReceivedTime() < Duration.of(10, ChronoUnit.MINUTES).toNanos());
        if (publisherHealthCheck) {
            healthServer.addCheck(() -> System.nanoTime() - mqttHfpPublisher.getLastSentTime() < Duration.of(10, ChronoUnit.MINUTES).toNanos());
        }
    }
}
