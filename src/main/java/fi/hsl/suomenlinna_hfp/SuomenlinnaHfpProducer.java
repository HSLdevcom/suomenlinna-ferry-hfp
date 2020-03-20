package fi.hsl.suomenlinna_hfp;

import fi.hsl.suomenlinna_hfp.common.utils.SpeedUtils;
import fi.hsl.suomenlinna_hfp.digitraffic.model.VesselLocation;
import fi.hsl.suomenlinna_hfp.digitraffic.model.VesselMetadata;
import fi.hsl.suomenlinna_hfp.digitraffic.provider.VesselLocationProvider;
import fi.hsl.suomenlinna_hfp.gtfs.model.*;
import fi.hsl.suomenlinna_hfp.gtfs.model.Calendar;
import fi.hsl.suomenlinna_hfp.gtfs.provider.GtfsProvider;
import fi.hsl.suomenlinna_hfp.gtfs.utils.GtfsIndex;
import fi.hsl.suomenlinna_hfp.gtfs.utils.ServiceDates;
import fi.hsl.suomenlinna_hfp.hfp.model.*;
import fi.hsl.suomenlinna_hfp.hfp.publisher.HfpPublisher;
import fi.hsl.suomenlinna_hfp.hfp.utils.HfpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SuomenlinnaHfpProducer {
    private static final Logger LOG = LoggerFactory.getLogger(SuomenlinnaHfpProducer.class);

    private final Map<String, VehicleId> mmsiToVehicleId;

    private final TripProcessor tripProcessor;

    private final GtfsProvider gtfsProvider;
    private final VesselLocationProvider vesselLocationProvider;
    private final HfpPublisher hfpPublisher;

    private final BlockingQueue<Throwable> errorQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<VesselLocation> vesselLocationQueue = new ArrayBlockingQueue<>(100);

    private final Map<Integer, VesselMetadata> vesselMetadatas = Collections.synchronizedMap(new HashMap<>());

    private Thread thread;

    public SuomenlinnaHfpProducer(Map<String, VehicleId> mmsiToVehicleId, TripProcessor tripProcessor, GtfsProvider gtfsProvider, VesselLocationProvider vesselLocationProvider, HfpPublisher hfpPublisher) {
        this.mmsiToVehicleId = mmsiToVehicleId;
        this.tripProcessor = tripProcessor;
        this.gtfsProvider = gtfsProvider;
        this.vesselLocationProvider = vesselLocationProvider;
        this.hfpPublisher = hfpPublisher;
    }

    private void onError(Throwable throwable) {
        errorQueue.offer(throwable);
        thread.interrupt();
    }

    public void run() throws Throwable {
        if (this.thread != null) {
            throw new IllegalStateException("SuomenlinnaHfpProducer is already running");
        }

        LOG.info("Starting SuomenlinnaHfpProducer");

        this.thread = Thread.currentThread();

        gtfsProvider.start(gtfsFeed -> {
            GtfsIndex gtfsIndex = new GtfsIndex(gtfsFeed.stops, gtfsFeed.trips, gtfsFeed.routes, gtfsFeed.stopTimes);
            ServiceDates serviceDates = new ServiceDates(gtfsFeed.calendars, gtfsFeed.calendarDates);

            tripProcessor.updateGtfsData(gtfsIndex, serviceDates);
        }, this::onError);
        vesselLocationProvider.start(mmsiToVehicleId.keySet(), vesselLocationQueue::offer, vesselMetadata -> vesselMetadatas.put(vesselMetadata.mmsi, vesselMetadata), this::onError);
        hfpPublisher.connect(() -> {}, this::onError);

        while (true) {
            if (!errorQueue.isEmpty()) {
                errorQueue.forEach(error -> {
                    LOG.error("Encountered error:", error);
                });
                LOG.info("Stopping program..");
                gtfsProvider.stop();
                try {
                    vesselLocationProvider.stop();
                } catch (Throwable e) {}
                try {
                    hfpPublisher.disconnect();
                } catch (Throwable e) {}
                break;
            }

            try {
                VesselLocation vesselLocation = vesselLocationQueue.take();


                if (tripProcessor.hasGtfsData()) {
                    VehicleId vehicleId = mmsiToVehicleId.get(String.valueOf(vesselLocation.mmsi));
                    VesselMetadata vesselMetadata = vesselMetadatas.get(vesselLocation.mmsi);

                    tripProcessor.processVehiclePosition(vehicleId, vesselLocation.coordinates, vesselLocation.properties.timestamp);

                    TripDescriptor tripDescriptor = tripProcessor.getRegisteredTrip(vehicleId);

                    String tst = HfpUtils.formatTst(vesselLocation.properties.timestamp);
                    //HFP timestamp is in seconds
                    int tsi = (int)(vesselLocation.properties.timestamp / 1000L);
                    //Convert vessel speed to metres per second
                    double spd = SpeedUtils.knotsToMetresPerSecond(vesselLocation.properties.speed);
                    //If vessel heading is not available (special value 511), use vessel course for heading
                    int hdg = (int)Math.round(Math.round(vesselLocation.properties.heading) == 511 ? vesselLocation.properties.course : vesselLocation.properties.heading);

                    if (tripDescriptor != null) {
                        NavigableMap<StopTime, Stop> currentAndNextStops = tripProcessor.getCurrentAndNextStops(vehicleId);

                        Map.Entry<StopTime, Stop> currentStop = currentAndNextStops.firstEntry();
                        Map.Entry<StopTime, Stop> nextStop = currentAndNextStops.higherEntry(currentAndNextStops.firstKey());

                        boolean isAtCurrentStop = tripProcessor.isAtCurrentStop(vehicleId);

                        String nextStopId = isAtCurrentStop ? currentStop.getValue().getId() :
                                nextStop != null ? nextStop.getValue().getId() : currentStop.getValue().getId();

                        Topic topic = new Topic(Topic.HFP_V2_PREFIX, Topic.JourneyType.JOURNEY, Topic.TemporalType.ONGOING, Topic.EventType.VP,
                                Topic.TransportMode.FERRY, vehicleId, tripDescriptor, nextStopId, 5,
                                new Geohash(vesselLocation.coordinates.getLatitude(), vesselLocation.coordinates.getLongitude()));

                        Payload payload = new Payload(tripDescriptor.routeName, tripDescriptor.directionId, vehicleId.operatorId, vehicleId.vehicleId,
                                tst, tsi, spd, hdg,
                                vesselLocation.coordinates.getLatitude(), vesselLocation.coordinates.getLongitude(), null, null, null, null,
                                tripDescriptor.departureDate, null, null, tripDescriptor.startTime, "GPS", isAtCurrentStop ? currentStop.getValue().getId() : null,
                                tripDescriptor.routeId, 0, vesselMetadata != null ? vesselMetadata.name : null);

                        hfpPublisher.publish(topic, payload);
                    } else {
                        Topic topic = new Topic(Topic.HFP_V2_PREFIX, Topic.JourneyType.DEADRUN, Topic.TemporalType.ONGOING, Topic.EventType.VP, Topic.TransportMode.FERRY, vehicleId);

                        Payload payload = new Payload(null, null, vehicleId.operatorId, vehicleId.vehicleId, tst, tsi, spd, hdg,
                                vesselLocation.coordinates.getLatitude(), vesselLocation.coordinates.getLongitude(),
                                null, null, null, null, null, null, null, null, "GPS",
                                null, null, null, vesselMetadata != null ? vesselMetadata.name : null);

                        hfpPublisher.publish(topic, payload);
                    }
                } else {
                    LOG.debug("No GTFS data available, cannot process vehicle position");
                }
            } catch (InterruptedException e) {
                LOG.warn("Thread interrupted, possibly because an error occured?", e);
            }
        }
    }
}
