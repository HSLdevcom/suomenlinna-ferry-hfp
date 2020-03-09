package fi.hsl.suomenlinna_hfp;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import fi.hsl.suomenlinna_hfp.digitraffic.provider.MqttVesselLocationProvider;
import fi.hsl.suomenlinna_hfp.digitraffic.provider.VesselLocationProvider;
import fi.hsl.suomenlinna_hfp.gtfs.provider.GtfsProvider;
import fi.hsl.suomenlinna_hfp.gtfs.provider.HttpGtfsProvider;
import fi.hsl.suomenlinna_hfp.hfp.model.VehicleId;
import fi.hsl.suomenlinna_hfp.hfp.publisher.MqttHfpPublisher;

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

        TripProcessor tripProcessor = new TripProcessor(timezone, suomenlinnaFerryRoutes, maxDistanceFromStop, defaultMaxDistanceFromStop,
                Duration.of(5, ChronoUnit.MINUTES).plusSeconds(30), //Allow registering for a trip up to 5.5 minutes before scheduled departure
                3); //Allow registering for next trip if the previous trip is not finished within 3 * scheduled duration of the trip after registration

        VesselLocationProvider vesselLocationProvider = new MqttVesselLocationProvider(meriDigitrafficBroker, meriDigitrafficUser, meriDigitrafficPassword);
        GtfsProvider gtfsProvider = new HttpGtfsProvider(gtfsUrl, gtfsPollInterval.getSeconds(), TimeUnit.SECONDS);
        MqttHfpPublisher mqttHfpPublisher = new MqttHfpPublisher(publisherBroker);

        new SuomenlinnaHfpProducer(suomenlinnaFerryRoutes, suomenlinnaFerryIds, tripProcessor, gtfsProvider, vesselLocationProvider, mqttHfpPublisher).run();
    }
}
