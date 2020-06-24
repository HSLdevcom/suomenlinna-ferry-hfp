package fi.hsl.suomenlinna_hfp;

import fi.hsl.suomenlinna_hfp.common.VehiclePositionProvider;
import fi.hsl.suomenlinna_hfp.common.model.VehicleMetadata;
import fi.hsl.suomenlinna_hfp.common.model.VehiclePosition;
import fi.hsl.suomenlinna_hfp.gtfs.model.Stop;
import fi.hsl.suomenlinna_hfp.gtfs.model.StopTime;
import fi.hsl.suomenlinna_hfp.gtfs.provider.GtfsProvider;
import fi.hsl.suomenlinna_hfp.gtfs.utils.GtfsIndex;
import fi.hsl.suomenlinna_hfp.gtfs.utils.ServiceDates;
import fi.hsl.suomenlinna_hfp.hfp.model.*;
import fi.hsl.suomenlinna_hfp.hfp.publisher.HfpPublisher;
import fi.hsl.suomenlinna_hfp.hfp.utils.GeohashLevelCalculator;
import fi.hsl.suomenlinna_hfp.hfp.utils.HfpUtils;
import fi.hsl.suomenlinna_hfp.sbdrive.model.VehicleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class HfpProducer {
    private static final Logger LOG = LoggerFactory.getLogger(HfpProducer.class);

    private final Topic.TransportMode transportMode;
    private final Map<String, VehicleId> vehicleIdMap;

    private final TripProcessor tripProcessor;

    private final GtfsProvider gtfsProvider;
    private final VehiclePositionProvider vehiclePositionProvider;
    private final HfpPublisher hfpPublisher;

    private final BlockingQueue<Throwable> errorQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<VehiclePosition> vehiclePositionQueue = new ArrayBlockingQueue<>(100);

    private final Map<String, VehicleMetadata> vehicleMetadatas = Collections.synchronizedMap(new HashMap<>());

    private final GeohashLevelCalculator geohashLevelCalculator = new GeohashLevelCalculator();

    private Thread thread;

    public HfpProducer(Topic.TransportMode transportMode, Map<String, VehicleId> vehicleIdMap, TripProcessor tripProcessor, GtfsProvider gtfsProvider, VehiclePositionProvider vehiclePositionProvider, HfpPublisher hfpPublisher) {
        this.transportMode = transportMode;
        this.vehicleIdMap = vehicleIdMap;
        this.tripProcessor = tripProcessor;
        this.gtfsProvider = gtfsProvider;
        this.vehiclePositionProvider = vehiclePositionProvider;
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
        vehiclePositionProvider.start(vehiclePositionQueue::offer, vehicleMetadata -> vehicleMetadatas.put(vehicleMetadata.getId(), vehicleMetadata), this::onError);
        hfpPublisher.connect(() -> {}, this::onError);

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

                    TripDescriptor tripDescriptor = tripProcessor.getRegisteredTrip(vehicleId);
                    boolean registeredForTrip = tripDescriptor != null;

                    if (tripDescriptor == null) {
                        tripDescriptor = getPetitionTripDescriptor(vehiclePosition);
                        //Not registered to an actual trip
                        registeredForTrip = false;
                    }

                    String tst = HfpUtils.formatTst(vehiclePosition.getTimestamp());
                    //HFP timestamp is in seconds
                    int tsi = (int)(vehiclePosition.getTimestamp() / 1000L);
                    //Speed in metres per second
                    double spd = vehiclePosition.getSpeed();
                    //Heading in degrees from north
                    int hdg = (int)Math.round(vehiclePosition.getHeading());

                    if (tripDescriptor != null) {
                        boolean isAtCurrentStop = false;
                        Map.Entry<StopTime, Stop> currentStop = null;
                        String nextStopId = "";

                        if (registeredForTrip) {
                            NavigableMap<StopTime, Stop> currentAndNextStops = tripProcessor.getCurrentAndNextStops(vehicleId);

                            currentStop = currentAndNextStops.firstEntry();
                            Map.Entry<StopTime, Stop> nextStop = currentAndNextStops.higherEntry(currentAndNextStops.firstKey());

                            isAtCurrentStop = tripProcessor.isAtCurrentStop(vehicleId);

                            nextStopId = isAtCurrentStop ? currentStop.getValue().getId() :
                                    nextStop != null ? nextStop.getValue().getId() : currentStop.getValue().getId();
                        }


                        int geohashLevel = geohashLevelCalculator.getGeohashLevel(vehicleId, vehiclePosition.getCoordinates(), tripDescriptor, nextStopId);

                        Topic topic = new Topic(Topic.HFP_V2_PREFIX, Topic.JourneyType.JOURNEY, Topic.TemporalType.ONGOING, Topic.EventType.VP,
                                transportMode, vehicleId, tripDescriptor, nextStopId, geohashLevel,
                                new Geohash(vehiclePosition.getCoordinates().getLatitude(), vehiclePosition.getCoordinates().getLongitude()));

                        Payload payload = new Payload(tripDescriptor.routeName, tripDescriptor.directionId, vehicleId.operatorId, vehicleId.vehicleId,
                                tst, tsi, spd, hdg,
                                vehiclePosition.getCoordinates().getLatitude(), vehiclePosition.getCoordinates().getLongitude(), null, null, null, null,
                                tripDescriptor.departureDate, null, null, tripDescriptor.startTime, "GPS", isAtCurrentStop ? currentStop.getValue().getId() : null,
                                tripDescriptor.routeId, 0, vehicleMetadata != null ? vehicleMetadata.getLabel() : null);

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

    //"Hack" to generate trip descriptors for robot bus 29R when it is running only when requested
    //TODO: remove this after robot bus 29R is no longer running or figure out a better way to do this
    private static TripDescriptor getPetitionTripDescriptor(VehiclePosition vehiclePosition) {
        if (vehiclePosition instanceof VehicleState) {
            VehicleState vehicleState = (VehicleState) vehiclePosition;

            //Values for 'can_receive_petitions' seem to be incorrect
            //if (vehicleState.canReceivePetitions) {
                LocalDateTime dateTime = Instant.ofEpochMilli(vehicleState.timestamp).atZone(ZoneId.of("Europe/Helsinki")).toLocalDateTime();

                LocalTime petitionStartTimeMorning = LocalTime.of(9, 55);
                LocalTime petitionEndTimeMorning = LocalTime.of(10, 50);
                LocalTime petitionStartTimeAfternoon = LocalTime.of(12, 30);
                LocalTime petitionEndTimeAfternoon = LocalTime.of(13, 20);

                if (dateTime.toLocalTime().isAfter(petitionStartTimeMorning) && dateTime.toLocalTime().isBefore(petitionEndTimeMorning)) {
                    return new TripDescriptor("1029R", "29R", dateTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE), petitionStartTimeMorning.format(DateTimeFormatter.ISO_LOCAL_TIME), "1", "Messukeskus");
                } else if (dateTime.toLocalTime().isAfter(petitionStartTimeAfternoon) && dateTime.toLocalTime().isBefore(petitionEndTimeAfternoon)) {
                    return new TripDescriptor("1029R", "29R", dateTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE), petitionStartTimeAfternoon.format(DateTimeFormatter.ISO_LOCAL_TIME), "1", "Messukeskus");
                }
            //}
        }

        return null;
    }
}
