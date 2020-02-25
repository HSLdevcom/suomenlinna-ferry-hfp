package fi.hsl.suomenlinna_hfp;

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

public class Main {
    private static final List<String> SUOMENLINNA_FERRY_ROUTES = Arrays.asList("1019", "1019E");

    private static final Map<String, VehicleId> SUOMENLINNA_FERRY_IDS;

    static {
        Map<String, VehicleId> ids = new HashMap<>(4);
        ids.put( "230985490", new VehicleId(60, 2)); //SUOMENLINNA II
        ids.put("230024660", new VehicleId(60, 3)); //TOR
        ids.put("230108610", new VehicleId(60, 4)); //SUOKKI
        ids.put("230108590", new VehicleId(60, 5)); //EHRENSVÄRD

        SUOMENLINNA_FERRY_IDS = Collections.unmodifiableMap(ids);
    }

    public static void main(String[] args) throws Throwable {
        Map<String, Double> maxDistanceFromStop = new HashMap<>();
        maxDistanceFromStop.put("1080701", 45.0);
        maxDistanceFromStop.put("1520703", 35.0);

        double defaultMaxDistanceFromStop = 25;

        TripProcessor tripProcessor = new TripProcessor(ZoneId.of("Europe/Helsinki"), SUOMENLINNA_FERRY_ROUTES, maxDistanceFromStop, defaultMaxDistanceFromStop,
                Duration.of(2, ChronoUnit.MINUTES), //Allow registering for a trip up to 2 minutes before scheduled departure
                Duration.of(5, ChronoUnit.MINUTES), //Allow registering for a trip up to 5 minutes after scheduled departure
                Duration.of(15, ChronoUnit.MINUTES)); //Allow registering for next trip if the previous trip is not finished within 15 minutes of scheduled arrival to the final stop

        VesselLocationProvider vesselLocationProvider = new MqttVesselLocationProvider("wss://meri.digitraffic.fi:61619/mqtt", "digitraffic", "digitrafficPassword");
        GtfsProvider gtfsProvider = new HttpGtfsProvider("https://dev.hsl.fi/gtfs/hsl.zip", 12, TimeUnit.HOURS);
        MqttHfpPublisher mqttHfpPublisher = new MqttHfpPublisher("tcp://hsl-mqtt-lab-d.cinfra.fi:1883");

        new SuomenlinnaHfpProducer(SUOMENLINNA_FERRY_IDS, tripProcessor, gtfsProvider, vesselLocationProvider, mqttHfpPublisher).run();
    }
}
