package fi.hsl.suomenlinna_hfp;

import fi.hsl.suomenlinna_hfp.common.model.LatLng;
import fi.hsl.suomenlinna_hfp.gtfs.model.Stop;
import fi.hsl.suomenlinna_hfp.gtfs.model.StopTime;
import fi.hsl.suomenlinna_hfp.gtfs.model.Trip;
import fi.hsl.suomenlinna_hfp.hfp.model.TripDescriptor;
import fi.hsl.suomenlinna_hfp.hfp.model.VehicleId;
import fi.hsl.suomenlinna_hfp.gtfs.utils.GtfsIndex;
import fi.hsl.suomenlinna_hfp.gtfs.utils.ServiceDates;
import fi.hsl.suomenlinna_hfp.hfp.utils.HfpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class TripProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(TripProcessor.class);

    private Map<VehicleId, TripDescriptor> registeredTrips = new HashMap<>();
    private Map<VehicleId, SortedSet<StopTime>> route = new HashMap<>();
    private Map<VehicleId, SortedSet<ZonedDateTime>> scheduledArrivalTimes = new HashMap<>();
    private Map<VehicleId, SortedSet<Integer>> passedStops = new HashMap<>();

    private volatile GtfsIndex gtfsIndex;
    private volatile ServiceDates serviceDates;

    private final ZoneId timezone;

    private final List<String> possibleRoutes;
    private final Map<String, Double> maxDistanceFromStop;

    private final double defaultMaxDistanceFromStop;

    private final Duration maxTimeBeforeDeparture;
    private final Duration maxTimeAfterDeparture;

    /**
     *
     * @param timezone Timezone used
     * @param possibleRoutes Possible routes for which the vehicles can register for
     * @param maxDistanceFromStop Maximum distances from stops, can be empty
     * @param defaultMaxDistanceFromStop Default maximum distance from stop
     * @param maxTimeBeforeDeparture Maximum time for when the vehicle can register for a trip before scheduled departure time
     * @param maxTimeAfterDeparture Maximum time for when the vehicle can register for a trip after scheduled departure time
     */
    public TripProcessor(ZoneId timezone, List<String> possibleRoutes, Map<String, Double> maxDistanceFromStop, double defaultMaxDistanceFromStop, Duration maxTimeBeforeDeparture, Duration maxTimeAfterDeparture) {
        this.timezone = timezone;
        this.possibleRoutes = possibleRoutes;
        this.maxDistanceFromStop = maxDistanceFromStop;
        this.defaultMaxDistanceFromStop = defaultMaxDistanceFromStop;
        this.maxTimeBeforeDeparture = maxTimeBeforeDeparture;
        this.maxTimeAfterDeparture = maxTimeAfterDeparture;
    }

    public boolean hasGtfsData() {
        synchronized (this) {
            return gtfsIndex != null && serviceDates != null;
        }
    }

    public void updateGtfsData(GtfsIndex gtfsIndex, ServiceDates serviceDates) {
        synchronized (this) {
            this.gtfsIndex = gtfsIndex;
            this.serviceDates = serviceDates;
        }
    }

    public TripDescriptor getRegisteredTrip(VehicleId vehicleId) {
        return registeredTrips.get(vehicleId);
    }

    public Stop getCurrentStop(VehicleId vehicleId) {
        if (!hasGtfsData()) {
            throw new IllegalStateException("TripRegister does not have GTFS data");
        }

        Integer lastStopSequence = passedStops.getOrDefault(vehicleId, Collections.emptySortedSet()).last();
        if (lastStopSequence == null) {
            return null;
        }

        Optional<StopTime> lastStopTime = route.getOrDefault(vehicleId, Collections.emptySortedSet()).stream().filter(stopTime -> stopTime.getStopSequence().equals(lastStopSequence)).findAny();
        return lastStopTime.map(stopTime -> {
            synchronized (TripProcessor.this) {
                return gtfsIndex.stopsById.get(stopTime.getStopId());
            }
        }).orElse(null);
    }

    public Stop getNextStop(VehicleId vehicleId) {
        if (!hasGtfsData()) {
            throw new IllegalStateException("TripRegister does not have GTFS data");
        }

        Integer lastStopSequence = passedStops.getOrDefault(vehicleId, Collections.emptySortedSet()).last();
        if (lastStopSequence == null) {
            return null;
        }

        Optional<StopTime> nextStopTime = route.getOrDefault(vehicleId, Collections.emptySortedSet()).stream().filter(stopTime -> stopTime.getStopSequence() > lastStopSequence).findAny();
        return nextStopTime.map(stopTime -> {
            synchronized (TripProcessor.this) {
                return gtfsIndex.stopsById.get(stopTime.getStopId());
            }
        }).orElse(null);
    }

    private boolean canRegisterForTrip(VehicleId vehicleId, ZonedDateTime vehicleTime) {
        return !registeredTrips.containsKey(vehicleId) //Vehicle is not currently registered to any trip
            || scheduledArrivalTimes.get(vehicleId).last().isBefore(vehicleTime); //Vehicle has finished previous trip
    }

    /**
     * Processes vehicle position to get stop status and trip registration
     * @param vehicleId ID of the vehicle
     * @param position Position of the vehicle
     * @param timestamp Timestamp when the position was received
     */
    public void processVehiclePosition(VehicleId vehicleId, LatLng position, long timestamp) {
        if (!hasGtfsData()) {
            throw new IllegalStateException("TripRegister does not have GTFS data");
        }

        ZonedDateTime time = Instant.ofEpochMilli(timestamp).atZone(ZoneId.of("UTC"));

        GtfsIndex gtfsIndex;
        ServiceDates serviceDates;

        //Create copy of GTFS data
        synchronized (this) {
            gtfsIndex = this.gtfsIndex;
            serviceDates = this.serviceDates;
        }

        if (canRegisterForTrip(vehicleId, time)) {
            registerForTrip(vehicleId, position, time, gtfsIndex, serviceDates);
        } else {
            updateStopStatus(vehicleId, position, gtfsIndex);
        }
    }

    private void registerForTrip(VehicleId vehicleId, LatLng position, ZonedDateTime time, GtfsIndex gtfsIndexCopy, ServiceDates serviceDatesCopy) {
        //Remove previous registration
        registeredTrips.remove(vehicleId);
        route.remove(vehicleId);
        scheduledArrivalTimes.remove(vehicleId);
        passedStops.remove(vehicleId);

        //Cache distances to stops as most trips begin from same stops
        Map<Stop, Double> stopDistances = new HashMap<>();

        //Used to find first possible trip
        SortedMap<ZonedDateTime, StopTime> tripStartTimes = new TreeMap<>();
        Map<StopTime, LocalDate> operatingDates = new HashMap<>();

        //Go through all possible trips
        for (Trip trip : possibleRoutes.stream().map(gtfsIndexCopy.tripsByRouteId::get).flatMap(List::stream).collect(Collectors.toList())) {
            StopTime firstStopTime = gtfsIndexCopy.stopTimesByTripId.get(trip.getTripId()).first();

            Stop firstStop = gtfsIndexCopy.stopsById.get(firstStopTime.getStopId());

            if (stopDistances.computeIfAbsent(firstStop, stop -> stop.getCoordinates().distanceTo(position)) < maxDistanceFromStop.getOrDefault(firstStop.getId(), defaultMaxDistanceFromStop)) {
                //If the vehicle is near the first stop of the trip, find possible start times for the trip
                for (LocalDate operatingDate : serviceDatesCopy.getDatesForService(trip.getServiceId())) {
                    ZonedDateTime startTime = operatingDate.atStartOfDay(timezone).plusSeconds(firstStopTime.getDepartureTime());

                    if (startTime.until(time, ChronoUnit.SECONDS) < maxTimeAfterDeparture.getSeconds()) {
                        tripStartTimes.put(startTime, firstStopTime);
                        operatingDates.put(firstStopTime, operatingDate);

                        break;
                    }
                }
            }
        }

        if (!tripStartTimes.isEmpty()) {
            ZonedDateTime tripStartTime = tripStartTimes.firstKey();

            StopTime firstStopTime = tripStartTimes.get(tripStartTime);
            String startTime = HfpUtils.formatStartTime(firstStopTime.getDepartureTime());
            LocalDate operatingDate = operatingDates.get(firstStopTime);

            Trip trip = gtfsIndexCopy.tripsById.get(firstStopTime.getTripId());

            TripDescriptor tripDescriptor =  new TripDescriptor(trip.getRouteId(), gtfsIndexCopy.routesById.get(trip.getRouteId()).getShortName(), operatingDate.toString(), startTime, String.valueOf(trip.getDirectionId() + 1), trip.getHeadsign());

            long secondsUntilStartTime = time.until(tripStartTime, ChronoUnit.SECONDS);
            if (secondsUntilStartTime > maxTimeBeforeDeparture.get(ChronoUnit.SECONDS)) {
                LOG.debug("Could not register {} for trip {} / {} / {} / {} that starts in {} seconds", vehicleId, tripDescriptor.routeId, tripDescriptor.departureDate, tripDescriptor.startTime, tripDescriptor.directionId, secondsUntilStartTime);
                return;
            }

            if (registeredTrips.containsValue(tripDescriptor)) {
                //Some other vehicle was already registered for the same trip
                LOG.info("Could not register {} for trip {} / {} / {} / {}, as some other vehicle was already registered for it", vehicleId, tripDescriptor.routeId, tripDescriptor.departureDate, tripDescriptor.startTime, tripDescriptor.directionId);
                return;
            }

            registeredTrips.put(vehicleId, tripDescriptor);
            route.put(vehicleId, gtfsIndexCopy.stopTimesByTripId.get(trip.getTripId()));
            scheduledArrivalTimes.put(vehicleId, gtfsIndexCopy.stopTimesByTripId.get(trip.getTripId()).stream()
                    .map(stopTime -> operatingDate.atStartOfDay(timezone).plusSeconds(stopTime.getArrivalTime()))
                    .collect(Collectors.toCollection(TreeSet::new)));

            TreeSet<Integer> stops = new TreeSet<>();
            stops.add(firstStopTime.getStopSequence());

            passedStops.put(vehicleId, stops);

            LOG.debug("Registered {} for trip {} / {} / {} / {}", vehicleId, tripDescriptor.routeId, tripDescriptor.departureDate, tripDescriptor.startTime, tripDescriptor.directionId);
        } else {
            LOG.debug("No trip found for {}, assuming that it is on a deadrun", vehicleId);
        }
    }

    private void updateStopStatus(VehicleId vehicleId, LatLng position, GtfsIndex gtfsIndexCopy) {
        SortedSet<Integer> vehiclePassedStops = passedStops.get(vehicleId);
        SortedSet<StopTime> vehicleRoute = route.get(vehicleId);
        //If vehicle is not registered to a trip, there is no stop status to update
        if (vehiclePassedStops != null && vehicleRoute != null) {
            Integer maxPassedSequence = vehiclePassedStops.last();
            Optional<StopTime> nextAfter = vehicleRoute.stream().filter(stopTime -> stopTime.getStopSequence() > maxPassedSequence).findFirst();

            if (nextAfter.isPresent()) {
                Stop stop = gtfsIndexCopy.stopsById.get(nextAfter.get().getStopId());

                if (stop.getCoordinates().distanceTo(position) < maxDistanceFromStop.getOrDefault(stop.getId(), defaultMaxDistanceFromStop)) {
                    vehiclePassedStops.add(nextAfter.get().getStopSequence());
                }
            }
        }
    }
}
