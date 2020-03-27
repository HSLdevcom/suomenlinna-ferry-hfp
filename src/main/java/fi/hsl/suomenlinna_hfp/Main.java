package fi.hsl.suomenlinna_hfp;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import fi.hsl.suomenlinna_hfp.digitraffic.provider.MqttVesselLocationProvider;
import fi.hsl.suomenlinna_hfp.digitraffic.provider.VesselLocationProvider;
import fi.hsl.suomenlinna_hfp.gtfs.provider.GtfsProvider;
import fi.hsl.suomenlinna_hfp.gtfs.provider.HttpGtfsProvider;
import fi.hsl.suomenlinna_hfp.health.HealthServer;
import fi.hsl.suomenlinna_hfp.hfp.model.VehicleId;
import fi.hsl.suomenlinna_hfp.hfp.publisher.MqttHfpPublisher;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws Throwable {
        Config config = ConfigFactory.load();

        List<String> suomenlinnaFerryRoutes = config.getStringList("routes");

        Map<String, VehicleId> suomenlinnaFerryIds = config.getConfigList("mmsiToVehicleId").stream().collect(Collectors.toMap(c -> c.getString("mmsi"), c -> new VehicleId(c.getInt("operator"), c.getInt("vehicle"))));

        double defaultMaxDistanceFromStop = config.getDouble("defaultMaxDistanceFromStop");

        Map<String, Double> maxDistanceFromStop = config.getObject("maxDistanceFromStop").entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> ((Number)entry.getValue().unwrapped()).doubleValue()));

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
            HealthServer healthServer = new HealthServer(8080);
            healthServer.addCheck(() -> System.nanoTime() - vesselLocationProvider.getLastReceivedTime() < Duration.of(10, ChronoUnit.MINUTES).toNanos());
            healthServer.addCheck(() -> System.nanoTime() - mqttHfpPublisher.getLastSentTime() < Duration.of(10, ChronoUnit.MINUTES).toNanos());
        }

        new SuomenlinnaHfpProducer(suomenlinnaFerryIds, tripProcessor, gtfsProvider, vesselLocationProvider, mqttHfpPublisher).run();
    }
}
