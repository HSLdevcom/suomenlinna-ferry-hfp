package fi.hsl.suomenlinna_hfp;

import com.typesafe.config.*;
import fi.hsl.suomenlinna_hfp.common.VehiclePositionProvider;
import fi.hsl.suomenlinna_hfp.digitraffic.provider.*;
import fi.hsl.suomenlinna_hfp.gtfs.provider.*;
import fi.hsl.suomenlinna_hfp.health.*;
import fi.hsl.suomenlinna_hfp.hfp.model.*;
import fi.hsl.suomenlinna_hfp.hfp.publisher.*;
import fi.hsl.suomenlinna_hfp.sbdrive.provider.PollingVehicleStateProvider;

import java.io.*;
import java.net.http.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

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

        switch (configType) {
            case SUOMENLINNA:
                vehicleIdMap = config.getConfigList("mmsiToVehicleId").stream()
                        .collect(Collectors.toMap(c -> c.getString("mmsi"), c -> new VehicleId(c.getInt("operator"), c.getInt("vehicle"))));

                String meriDigitrafficBroker = config.getString("meriDigitraffic.broker");
                String meriDigitrafficUser = config.getString("meriDigitraffic.user");
                String meriDigitrafficPassword = config.getString("meriDigitraffic.password");

                transportMode = Topic.TransportMode.FERRY;
                vehiclePositionProvider = new MqttVesselLocationProvider(meriDigitrafficBroker, meriDigitrafficUser, meriDigitrafficPassword, vehicleIdMap.keySet());

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
            if (!config.getString("health.postEndpoint").equals("")) {
                createHealthServerWithNotification(vehiclePositionProvider, mqttHfpPublisher, config.getString("health.postEndpoint"));
            } else {
                createHealthServerWithoutNotification(vehiclePositionProvider, mqttHfpPublisher);
            }
        }

        new HfpProducer(transportMode, vehicleIdMap, tripProcessor, gtfsProvider, vehiclePositionProvider, mqttHfpPublisher).run();
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
            } catch (IllegalArgumentException iae) {
                return ConfigType.SUOMENLINNA;
            }
        }
    }

    private static void createHealthServerWithNotification(VehiclePositionProvider vehiclePositionProvider, MqttHfpPublisher mqttHfpPublisher, String postEndpoint) throws IOException {
        HealthServer healthServer = new HealthServer(8080, new HealthNotificationService(postEndpoint));
        healthServer.addCheck(() -> System.nanoTime() - vehiclePositionProvider.getLastReceivedTime() < Duration.of(10, ChronoUnit.MINUTES).toNanos());
        healthServer.addCheck(() -> System.nanoTime() - mqttHfpPublisher.getLastSentTime() < Duration.of(10, ChronoUnit.MINUTES).toNanos());
    }

    private static void createHealthServerWithoutNotification(VehiclePositionProvider vehiclePositionProvider, MqttHfpPublisher mqttHfpPublisher) throws IOException {
        HealthServer healthServer = new HealthServer(8080);
        healthServer.addCheck(() -> System.nanoTime() - vehiclePositionProvider.getLastReceivedTime() < Duration.of(10, ChronoUnit.MINUTES).toNanos());
        healthServer.addCheck(() -> System.nanoTime() - mqttHfpPublisher.getLastSentTime() < Duration.of(10, ChronoUnit.MINUTES).toNanos());
    }
}
