package fi.hsl.suomenlinna_hfp;

import com.typesafe.config.*;
import fi.hsl.suomenlinna_hfp.digitraffic.provider.*;
import fi.hsl.suomenlinna_hfp.gtfs.provider.*;
import fi.hsl.suomenlinna_hfp.health.*;
import fi.hsl.suomenlinna_hfp.hfp.model.*;
import fi.hsl.suomenlinna_hfp.hfp.publisher.*;

import java.io.*;
import java.net.http.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

public class Main {
    public static void main(String[] args) throws Throwable {
        Config config = ConfigFactory.load();

        List<String> suomenlinnaFerryRoutes = config.getStringList("routes");

        Map<String, VehicleId> suomenlinnaFerryIds = config.getConfigList("mmsiToVehicleId").stream().collect(Collectors.toMap(c -> c.getString("mmsi"), c -> new VehicleId(c.getInt("operator"), c.getInt("vehicle"))));

        double defaultMaxDistanceFromStop = config.getDouble("defaultMaxDistanceFromStop");

        Map<String, Double> maxDistanceFromStop = config.getObject("maxDistanceFromStop").entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> ((Number) entry.getValue().unwrapped()).doubleValue()));

        ZoneId timezone = ZoneId.of(config.getString("timezone"));

        String meriDigitrafficBroker = config.getString("meriDigitraffic.broker");
        String meriDigitrafficUser = config.getString("meriDigitraffic.user");
        String meriDigitrafficPassword = config.getString("meriDigitraffic.password");

        String gtfsUrl = config.getString("gtfs.url");
        Duration gtfsPollInterval = config.getDuration("gtfs.pollInterval");

        String publisherBroker = config.getString("publisher.broker");
        int publisherMaxReconnects = config.getInt("publisher.maxReconnects");

        Duration maxTimeBeforeDeparture = config.getDuration("tripProcessor.maxTimeBeforeDeparture");
        double maxTripDuration = config.getDouble("tripProcessor.maxTripDuration");

        TripProcessor tripProcessor = new TripProcessor(timezone, suomenlinnaFerryRoutes, maxDistanceFromStop, defaultMaxDistanceFromStop,
                maxTimeBeforeDeparture,
                maxTripDuration);

        VesselLocationProvider vesselLocationProvider = new MqttVesselLocationProvider(meriDigitrafficBroker, meriDigitrafficUser, meriDigitrafficPassword);

        HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        GtfsProvider gtfsProvider = new HttpGtfsProvider(httpClient, gtfsUrl, gtfsPollInterval.getSeconds(), TimeUnit.SECONDS, suomenlinnaFerryRoutes);

        MqttHfpPublisher mqttHfpPublisher = new MqttHfpPublisher(publisherBroker, publisherMaxReconnects);

        if (config.getBoolean("health.enabled")) {
            if (!config.getString("health.postEndpoint").equals("")) {
                createHealthServerWithNotification(vesselLocationProvider, mqttHfpPublisher, config.getString("health.postEndpoint"));
            } else {
                createHealthServerWithoutNotification(vesselLocationProvider, mqttHfpPublisher);
            }
        }

        new SuomenlinnaHfpProducer(suomenlinnaFerryIds, tripProcessor, gtfsProvider, vesselLocationProvider, mqttHfpPublisher).run();
    }

    private static void createHealthServerWithNotification(VesselLocationProvider vesselLocationProvider, MqttHfpPublisher mqttHfpPublisher, String postEndpoint) throws IOException {
        HealthServer healthServer = new HealthServer(8080, new HealthNotificationService(postEndpoint));
        healthServer.addCheck(() -> System.nanoTime() - vesselLocationProvider.getLastReceivedTime() < Duration.of(10, ChronoUnit.MINUTES).toNanos());
        healthServer.addCheck(() -> System.nanoTime() - mqttHfpPublisher.getLastSentTime() < Duration.of(10, ChronoUnit.MINUTES).toNanos());
    }

    private static void createHealthServerWithoutNotification(VesselLocationProvider vesselLocationProvider, MqttHfpPublisher mqttHfpPublisher) throws IOException {
        HealthServer healthServer = new HealthServer(8080);
        healthServer.addCheck(() -> System.nanoTime() - vesselLocationProvider.getLastReceivedTime() < Duration.of(10, ChronoUnit.MINUTES).toNanos());
        healthServer.addCheck(() -> System.nanoTime() - mqttHfpPublisher.getLastSentTime() < Duration.of(10, ChronoUnit.MINUTES).toNanos());
    }
}
