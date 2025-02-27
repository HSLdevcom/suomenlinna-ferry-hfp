package fi.hsl.suomenlinna_hfp;

import fi.hsl.suomenlinna_hfp.common.PassengerCountProvider;
import fi.hsl.suomenlinna_hfp.common.VehiclePositionProvider;
import fi.hsl.suomenlinna_hfp.common.model.PassengerCount;
import fi.hsl.suomenlinna_hfp.common.model.VehicleMetadata;
import fi.hsl.suomenlinna_hfp.common.model.VehiclePosition;
import fi.hsl.suomenlinna_hfp.common.utils.MathUtils;
import fi.hsl.suomenlinna_hfp.gtfs.provider.GtfsProvider;
import fi.hsl.suomenlinna_hfp.gtfs.utils.GtfsIndex;
import fi.hsl.suomenlinna_hfp.gtfs.utils.ServiceDates;
import fi.hsl.suomenlinna_hfp.hfp.model.*;
import fi.hsl.suomenlinna_hfp.hfp.publisher.HfpPublisher;
import fi.hsl.suomenlinna_hfp.hfp.utils.GeohashLevelCalculator;
import fi.hsl.suomenlinna_hfp.hfp.utils.HfpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.malkki.gtfs.model.Stop;
import xyz.malkki.gtfs.model.StopTime;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

public class HfpProducer {
    private static final Logger LOG = LoggerFactory.getLogger(HfpProducer.class);

    private final Topic.TransportMode transportMode;
    private final Map<String, VehicleId> vehicleIdMap;

    private final TripProcessor tripProcessor;

    private final GtfsProvider gtfsProvider;
    private final VehiclePositionProvider vehiclePositionProvider;
    private final PassengerCountProvider passengerCountProvider;
    private final HfpPublisher hfpPublisher;

    private final BlockingQueue<Throwable> errorQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<VehiclePosition> vehiclePositionQueue = new ArrayBlockingQueue<>(100);

    private final Map<String, VehicleMetadata> vehicleMetadatas = Collections.synchronizedMap(new HashMap<>());

    private final GeohashLevelCalculator geohashLevelCalculator = new GeohashLevelCalculator();

    private Thread thread;

    public HfpProducer(Topic.TransportMode transportMode, Map<String, VehicleId> vehicleIdMap, TripProcessor tripProcessor, GtfsProvider gtfsProvider, VehiclePositionProvider vehiclePositionProvider, PassengerCountProvider passengerCountProvider, HfpPublisher hfpPublisher) {
        this.transportMode = transportMode;
        this.vehicleIdMap = vehicleIdMap;
        this.tripProcessor = tripProcessor;
        this.gtfsProvider = gtfsProvider;
        this.vehiclePositionProvider = vehiclePositionProvider;
        this.passengerCountProvider = passengerCountProvider;
        this.hfpPublisher = hfpPublisher;
    }

    private void onError(Throwable throwable) {
        errorQueue.offer(throwable);
        thread.interrupt();
    }

    public void run() throws Throwable {
        if (this.thread != null) {
            throw new IllegalStateException("HfpProducer is already running");
        }

        LOG.info("Starting HfpProducer");

        this.thread = Thread.currentThread();

        gtfsProvider.start(gtfsFeed -> {
            GtfsIndex gtfsIndex = new GtfsIndex(gtfsFeed.stops, gtfsFeed.trips, gtfsFeed.routes, gtfsFeed.stopTimes);
            ServiceDates serviceDates = new ServiceDates(gtfsFeed.calendars, gtfsFeed.calendarDates);

            tripProcessor.updateGtfsData(gtfsIndex, serviceDates);
        }, this::onError);
        LOG.info("gtfsProvider started. ErrorQueue size: {}", errorQueue.size());
        vehiclePositionProvider.start(vehiclePositionQueue::offer, vehicleMetadata -> vehicleMetadatas.put(vehicleMetadata.getId(), vehicleMetadata), this::onError);
        LOG.info("vehiclePositionProvider started. ErrorQueue size: {}", errorQueue.size());
        hfpPublisher.connect(() -> {}, this::onError);
        LOG.info("hfpPublisher connected. ErrorQueue size: {}", errorQueue.size());

        while (true) {
            if (!errorQueue.isEmpty()) {
                errorQueue.forEach(error -> {
                    LOG.error("Encountered error:", error);
                });
                LOG.info("Stopping program..");
                gtfsProvider.stop();
                vehiclePositionProvider.stop();
                hfpPublisher.disconnect();
                break;
            }

            try {
                VehiclePosition vehiclePosition = vehiclePositionQueue.take();

                if (tripProcessor.hasGtfsData()) {
                    VehicleId vehicleId = vehicleIdMap.get(vehiclePosition.getId());
                    if (vehicleId == null) {
                        continue;
                    }

                    VehicleMetadata vehicleMetadata = vehicleMetadatas.get(vehiclePosition.getId());

                    tripProcessor.processVehiclePosition(vehicleId, vehiclePosition.getCoordinates(), vehiclePosition.getTimestamp());

                    TripProcessor.TripAndRouteWithStopTimes trip = tripProcessor.getRegisteredTrip(vehicleId);
                    TripDescriptor tripDescriptor = trip != null ? trip.getTripDescriptor() : null;

                    String tst = HfpUtils.formatTst(vehiclePosition.getTimestamp());
                    //HFP timestamp is in seconds
                    int tsi = (int)(vehiclePosition.getTimestamp() / 1000L);
                    //Speed in metres per second
                    double spd = vehiclePosition.getSpeed();
                    //Heading in degrees from north
                    int hdg = (int)Math.round(vehiclePosition.getHeading());

                    if (tripDescriptor != null) {
                        //Get passenger count for the trip
                        OptionalInt occu = getPassengerCount(trip);

                        NavigableMap<StopTime, Stop> currentAndNextStops = tripProcessor.getCurrentAndNextStops(vehicleId);

                        Map.Entry<StopTime, Stop> currentStop = currentAndNextStops.firstEntry();
                        Map.Entry<StopTime, Stop> nextStop = currentAndNextStops.higherEntry(currentAndNextStops.firstKey());

                        boolean isAtCurrentStop = tripProcessor.isAtCurrentStop(vehicleId);

                        String nextStopId = isAtCurrentStop ? currentStop.getValue().getStopId() :
                                nextStop != null ? nextStop.getValue().getStopId() : currentStop.getValue().getStopId();

                        int geohashLevel = geohashLevelCalculator.getGeohashLevel(vehicleId, vehiclePosition.getCoordinates(), tripDescriptor, nextStopId);

                        Topic topic = new Topic(Topic.HFP_V2_PREFIX, Topic.JourneyType.JOURNEY, Topic.TemporalType.ONGOING, Topic.EventType.VP,
                                transportMode, vehicleId, tripDescriptor, nextStopId, geohashLevel,
                                new Geohash(vehiclePosition.getCoordinates().getLatitude(), vehiclePosition.getCoordinates().getLongitude()));

                        Payload payload = new Payload(tripDescriptor.routeName, tripDescriptor.directionId, vehicleId.operatorId, vehicleId.vehicleId,
                                tst, tsi, spd, hdg,
                                vehiclePosition.getCoordinates().getLatitude(), vehiclePosition.getCoordinates().getLongitude(), null, null, null, null,
                                tripDescriptor.departureDate, null, null, tripDescriptor.startTime, "GPS", isAtCurrentStop ? currentStop.getValue().getStopId() : null,
                                tripDescriptor.routeId, occu.orElse(0), vehicleMetadata != null ? vehicleMetadata.getLabel() : null);

                        hfpPublisher.publish(topic, payload);
                    } else {
                        Topic topic = new Topic(Topic.HFP_V2_PREFIX, Topic.JourneyType.DEADRUN, Topic.TemporalType.ONGOING, Topic.EventType.VP, transportMode, vehicleId);

                        Payload payload = new Payload(null, null, vehicleId.operatorId, vehicleId.vehicleId, tst, tsi, spd, hdg,
                                vehiclePosition.getCoordinates().getLatitude(), vehiclePosition.getCoordinates().getLongitude(),
                                null, null, null, null, null, null, null, null, "GPS",
                                null, null, null, vehicleMetadata != null ? vehicleMetadata.getLabel() : null);

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


    /**
     * Gets passenger count for the trip asynchronously. Returns empty value if operation is not yet completed
     * @param trip
     */
    private OptionalInt getPassengerCount(TripProcessor.TripAndRouteWithStopTimes trip) {
        if (passengerCountProvider != null) {
            final String stopCode = trip.stops.get(trip.getFirstStopTime().getStopId()).getStopCode();
            final CompletableFuture<PassengerCount> passengerCountFuture = passengerCountProvider.getPassengerCountByStartTimeAndStopCode(trip.getStartTime(), stopCode);

            if (passengerCountFuture.isDone()) {
                try {
                    final PassengerCount passengerCount = passengerCountFuture.join();
                    if (passengerCount == null) {
                        return OptionalInt.empty();
                    }

                    final int passengerCountAsPercentage = MathUtils.percentageAsInteger(MathUtils.clamp(passengerCount.getPercentage(), 0, 1));

                    return OptionalInt.of(passengerCountAsPercentage);
                } catch (Throwable throwable) {
                    LOG.warn("Failed to fetch passenger count", throwable);
                    return OptionalInt.empty();
                }
            } else {
                //Passenger count not available yet
                return OptionalInt.empty();
            }
        } else {
            return OptionalInt.empty();
        }
    }
}
