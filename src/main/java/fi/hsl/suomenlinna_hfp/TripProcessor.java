package fi.hsl.suomenlinna_hfp;

import fi.hsl.suomenlinna_hfp.common.model.LatLng;
import fi.hsl.suomenlinna_hfp.common.utils.TimeUtils;
import fi.hsl.suomenlinna_hfp.gtfs.utils.GtfsIndex;
import fi.hsl.suomenlinna_hfp.gtfs.utils.ServiceDates;
import fi.hsl.suomenlinna_hfp.hfp.model.TripDescriptor;
import fi.hsl.suomenlinna_hfp.hfp.model.VehicleId;
import fi.hsl.suomenlinna_hfp.hfp.utils.HfpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.malkki.gtfs.model.Route;
import xyz.malkki.gtfs.model.Stop;
import xyz.malkki.gtfs.model.StopTime;
import xyz.malkki.gtfs.model.Trip;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TripProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(TripProcessor.class);

    private Map<VehicleId, TripAndRouteWithStopTimes> registeredTrips = new HashMap<>();
    //Time when vehicle registered for the trip
    private Map<VehicleId, ZonedDateTime> registrationTimes = new HashMap<>();
    //Highest stop sequence that the vehicle has reached
    private Map<VehicleId, Integer> currentStop = new HashMap<>();
    //Whether the vehicle is at the current stop or not
    private Map<VehicleId, Boolean> isAtCurrentStop = new HashMap<>();
    //Time when the next trip of the same pattern (pattern = set of trips having same route and direction) would start
    private Map<VehicleId, ZonedDateTime> nextTripTime = new HashMap<>();

    private TripRegistrationCache tripRegistrationCache = new TripRegistrationCache(Duration.of(1, ChronoUnit.DAYS));

    private volatile Map<Stop, NavigableMap<ZonedDateTime, TripAndRouteWithStopTimes>> tripsByStartStopAndTime;

    private final ZoneId timezone;

    private final List<String> possibleRoutes;

    //Map of allowed route IDs for certain vehicle. If vehicle is not present in this map, it can be assigned to any route
    private final Map<VehicleId, List<String>> routeRestrictions;
    private final Map<String, Double> maxDistanceFromStop;

    private final double defaultMaxDistanceFromStop;

    private final Duration maxTimeBeforeDeparture;

    private final double maxDurationOfTrip;

    /**
     * @param timezone                   Timezone used
     * @param possibleRoutes             Possible routes for which the vehicles can register for
     * @param routeRestrictions
     * @param maxDistanceFromStop        Maximum distances from stops, can be empty
     * @param defaultMaxDistanceFromStop Default maximum distance from stop
     * @param maxTimeBeforeDeparture     Maximum time for when the vehicle can register for a trip before scheduled departure time
     * @param maxDurationOfTrip          Maximum duration of the trip, as multiplier (e.g. 1 = scheduled running time, 2 = 2x scheduled running time). If the vehicle has not reached its final stop after max duration, it can register for a next trip
     */
    public TripProcessor(ZoneId timezone, List<String> possibleRoutes, Map<VehicleId, List<String>> routeRestrictions, Map<String, Double> maxDistanceFromStop, double defaultMaxDistanceFromStop, Duration maxTimeBeforeDeparture, double maxDurationOfTrip) {
        this.timezone = timezone;
        this.possibleRoutes = possibleRoutes;
        this.routeRestrictions = routeRestrictions;
        this.maxDistanceFromStop = maxDistanceFromStop;
        this.defaultMaxDistanceFromStop = defaultMaxDistanceFromStop;
        this.maxTimeBeforeDeparture = maxTimeBeforeDeparture;
        this.maxDurationOfTrip = maxDurationOfTrip;
    }

    public boolean hasGtfsData() {
        synchronized (this) {
            return tripsByStartStopAndTime != null;
        }
    }

    public void updateGtfsData(GtfsIndex gtfsIndex, ServiceDates serviceDates) {
        synchronized (this) {
            this.tripsByStartStopAndTime = gtfsIndex.tripsByRouteId.entrySet().stream()
                    .filter(entry -> possibleRoutes.contains(entry.getKey()))
                    .flatMap(entry -> entry.getValue().stream())
                    .flatMap(trip -> {
                        SortedSet<StopTime> stopTimes = gtfsIndex.stopTimesByTripId.get(trip.getTripId());

                        Map<Stop, NavigableMap<ZonedDateTime, TripAndRouteWithStopTimes>> stopTimesByStartStopAndStartTime = new HashMap<>();

                        serviceDates.getDatesForService(trip.getServiceId()).forEach(date -> {
                            StopTime firstStopTime = stopTimes.first();
                            stopTimesByStartStopAndStartTime.compute(gtfsIndex.stopsById.get(firstStopTime.getStopId()), (key, value) -> {
                                if (value == null) {
                                    value = new TreeMap<>();
                                }

                                int departureTimeSeconds = stopTimes.first().getDepartureTime();
                                ZonedDateTime startTime = TimeUtils.gtfsTimeToLocalDateTime(date, departureTimeSeconds).atZone(timezone);

                                value.put(startTime, new TripAndRouteWithStopTimes(trip,
                                        gtfsIndex.routesById.get(trip.getRouteId()),
                                        date,
                                        stopTimes,
                                        stopTimes.stream().map(stopTime -> gtfsIndex.stopsById.get(stopTime.getStopId())).collect(Collectors.toSet())));
                                return value;
                            });
                        });

                        return stopTimesByStartStopAndStartTime.entrySet().stream();
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                       a.putAll(b);
                       return a;
                    }));
        }
    }

    public TripAndRouteWithStopTimes getRegisteredTrip(VehicleId vehicleId) {
        return registeredTrips.get(vehicleId);
    }

    public boolean isAtCurrentStop(VehicleId vehicleId) {
        return isAtCurrentStop.get(vehicleId);
    }

    public NavigableMap<StopTime, Stop> getCurrentAndNextStops(VehicleId vehicleId) {
        TripAndRouteWithStopTimes trip = registeredTrips.get(vehicleId);
        return trip.stopTimes.tailMap(currentStop.get(vehicleId)).values().stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        stopTime -> trip.stops.get(stopTime.getStopId()),
                        (a, b) -> a,
                        () -> new TreeMap<>(Comparator.comparingInt(StopTime::getStopSequence))
                ));
    }

    private Duration getMaxTripDuration(VehicleId vehicleId) {
        StopTime firstStopTime = registeredTrips.get(vehicleId).stopTimes.firstEntry().getValue();
        StopTime lastStopTime = registeredTrips.get(vehicleId).stopTimes.lastEntry().getValue();

        return Duration.of(Math.round(maxDurationOfTrip * (lastStopTime.getArrivalTime() - firstStopTime.getDepartureTime())), ChronoUnit.SECONDS);
    }

    private boolean canRegisterForTrip(VehicleId vehicleId, ZonedDateTime vehicleTime) {
        //Vehicle is not currently registered to any trip
        if (!registeredTrips.containsKey(vehicleId)) {
            return true;
        } else {
            TripAndRouteWithStopTimes trip = registeredTrips.get(vehicleId);

            return
                    //Vehicle has reached its final stop
                    currentStop.get(vehicleId).equals(trip.stopTimes.lastKey())
                    //Vehicle has not finished the previous trip in the specified time limit
                    || registrationTimes.get(vehicleId).plus(getMaxTripDuration(vehicleId)).isBefore(vehicleTime)
                    //Vehicle has not left the first stop and the next possible trip would start soon
                    || (isAtCurrentStop.get(vehicleId) && currentStop.get(vehicleId).equals(trip.stopTimes.firstKey()) && nextTripTime.get(vehicleId) != null && vehicleTime.until(nextTripTime.get(vehicleId), ChronoUnit.SECONDS) < maxTimeBeforeDeparture.getSeconds());
        }
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

        Map<Stop, NavigableMap<ZonedDateTime, TripAndRouteWithStopTimes>> tripsByStartStopAndTime;

        //Create copy of timetable data
        synchronized (this) {
            tripsByStartStopAndTime = this.tripsByStartStopAndTime;
        }

        if (canRegisterForTrip(vehicleId, time)) {
            registerForTrip(vehicleId, position, time, tripsByStartStopAndTime);
        } else {
            updateStopStatus(vehicleId, position);
        }
    }

    private void processRouteRestrictions(VehicleId vehicleId, Collection<TripAndRouteWithStopTimes> trips) {
        List<String> allowedRoutes = routeRestrictions.get(vehicleId);
        if (allowedRoutes != null) {
            //Remove trips that the vehicle can't register to
            trips.removeIf(tripAndRouteWithStopTimes -> !allowedRoutes.contains(tripAndRouteWithStopTimes.route.getRouteId()));
        }
    }

    private void registerForTrip(VehicleId vehicleId, LatLng position, ZonedDateTime time, Map<Stop, NavigableMap<ZonedDateTime, TripAndRouteWithStopTimes>> tripsByStartStopAndTime) {
        //Remove previous registration
        registeredTrips.remove(vehicleId);
        registrationTimes.remove(vehicleId);
        currentStop.remove(vehicleId);
        isAtCurrentStop.remove(vehicleId);

        //Used to find first possible trip
        NavigableMap<ZonedDateTime, TripAndRouteWithStopTimes> tripsFromStop = null;

        //Go through all possible trips
        for (Stop stop : tripsByStartStopAndTime.keySet()) {
            if (new LatLng(stop.getStopLat(), stop.getStopLon()).distanceTo(position) < maxDistanceFromStop.getOrDefault(stop.getStopId(), defaultMaxDistanceFromStop)) {
                tripsFromStop = tripsByStartStopAndTime.get(stop);
                break;
            }
        }

        //If the vehicle is near the first stop of a trip
        if (tripsFromStop != null) {
            //Filter routes that the vehicle cannot use
            processRouteRestrictions(vehicleId, tripsFromStop.values());

            //Trip that would have started before current time
            Map.Entry<ZonedDateTime, TripAndRouteWithStopTimes> earlierTrip = tripsFromStop.lowerEntry(time);

            //Trip that would start after current time
            Map.Entry<ZonedDateTime, TripAndRouteWithStopTimes> nextTrip = tripsFromStop.higherEntry(time);

            if (nextTrip == null) {
                LOG.info("GTFS schedule has no more trips to register for");
                return;
            }

            if (earlierTrip != null) {
                TripDescriptor earlierTripDescriptor = earlierTrip.getValue().getTripDescriptor();
                if (!tripRegistrationCache.hasTripBeenRegistered(earlierTripDescriptor)
                        /*If no trip has been registered for the pattern previously (i.e. the application has just started), we assume that the vehicle is running the trip
                            that starts after current time
                         */
                        && tripRegistrationCache.hasAnyTripBeenRegisteredForPattern(earlierTripDescriptor)) {
                    long secondsAfterStartTime = earlierTrip.getKey().until(time, ChronoUnit.SECONDS);
                    if (secondsAfterStartTime < earlierTrip.getKey().until(nextTrip.getKey(), ChronoUnit.SECONDS) - maxTimeBeforeDeparture.get(ChronoUnit.SECONDS)) {
                        LOG.debug("Could not register {} for trip {} / {} / {} / {} that started {} seconds ago", vehicleId, earlierTripDescriptor.routeId, earlierTripDescriptor.departureDate, earlierTripDescriptor.startTime, earlierTripDescriptor.directionId, secondsAfterStartTime);
                    } else {
                        tripRegistrationCache.addRegistration(earlierTripDescriptor);
                        registeredTrips.put(vehicleId, earlierTrip.getValue());
                        registrationTimes.put(vehicleId, time);
                        currentStop.put(vehicleId, earlierTrip.getValue().stopTimes.firstKey());
                        isAtCurrentStop.put(vehicleId, true);
                        nextTripTime.put(vehicleId, nextTrip.getKey());

                        return;
                    }
                }
            }

            //Earlier trip was already taken or it was too much in the past, let's try the next one
            TripDescriptor nextTripDescriptor = nextTrip.getValue().getTripDescriptor();

            long secondsUntilStartTime = time.until(nextTrip.getKey(), ChronoUnit.SECONDS);
            if (secondsUntilStartTime > maxTimeBeforeDeparture.get(ChronoUnit.SECONDS)) {
                LOG.debug("Could not register {} for trip {} / {} / {} / {} that starts in {} seconds", vehicleId, nextTripDescriptor.routeId, nextTripDescriptor.departureDate, nextTripDescriptor.startTime, nextTripDescriptor.directionId, secondsUntilStartTime);
                return;
            }

            if (tripRegistrationCache.hasTripBeenRegistered(nextTripDescriptor)) {
                //Some other vehicle was already registered for the same trip
                LOG.info("Could not register {} for trip {} / {} / {} / {}, as some other vehicle was already registered for it", vehicleId, nextTripDescriptor.routeId, nextTripDescriptor.departureDate, nextTripDescriptor.startTime, nextTripDescriptor.directionId);
                return;
            }

            tripRegistrationCache.addRegistration(nextTripDescriptor);
            registeredTrips.put(vehicleId, nextTrip.getValue());
            registrationTimes.put(vehicleId, time);
            currentStop.put(vehicleId, nextTrip.getValue().stopTimes.firstKey());
            isAtCurrentStop.put(vehicleId, true);
            nextTripTime.put(vehicleId, tripsFromStop.higherKey(nextTrip.getKey()));

            LOG.debug("Registered {} for trip {} / {} / {} / {}", vehicleId, nextTripDescriptor.routeId, nextTripDescriptor.departureDate, nextTripDescriptor.startTime, nextTripDescriptor.directionId);
        } else {
            LOG.debug("No trip found for {}, assuming that it is on a deadrun", vehicleId);
        }
    }

    private void updateStopStatus(VehicleId vehicleId, LatLng position) {
        if (!registeredTrips.containsKey(vehicleId)) {
            //If vehicle is not registered to a trip, there is no stop status to update
            return;
        }

        Integer currentStopSequence = currentStop.get(vehicleId);
        NavigableMap<Integer, StopTime> vehicleRoute = registeredTrips.get(vehicleId).stopTimes;
        Map<String, Stop> stops = registeredTrips.get(vehicleId).stops;

        Stop current = stops.get(vehicleRoute.get(currentStopSequence).getStopId());
        isAtCurrentStop.put(vehicleId, new LatLng(current.getStopLat(), current.getStopLon()).distanceTo(position) < maxDistanceFromStop.getOrDefault(current.getStopId(), defaultMaxDistanceFromStop));

        StopTime nextAfter = vehicleRoute.higherEntry(currentStopSequence).getValue();

        if (nextAfter != null) {
            Stop stop = stops.get(nextAfter.getStopId());

            if (new LatLng(stop.getStopLat(), stop.getStopLon()).distanceTo(position) < maxDistanceFromStop.getOrDefault(stop.getStopId(), defaultMaxDistanceFromStop)) {
                currentStop.put(vehicleId, nextAfter.getStopSequence());
                isAtCurrentStop.put(vehicleId, true);
            }
        }
    }

    public static class TripAndRouteWithStopTimes {
        public final Trip trip;
        public final Route route;
        public final LocalDate operatingDate;
        public final NavigableMap<Integer, StopTime> stopTimes;
        public final Map<String, Stop> stops;

        private TripAndRouteWithStopTimes(Trip trip, Route route, LocalDate operatingDate, Set<StopTime> stopTimes, Set<Stop> stops) {
            this.trip = trip;
            this.route = route;
            this.operatingDate = operatingDate;
            this.stopTimes = stopTimes.stream().collect(Collectors.toMap(StopTime::getStopSequence, Function.identity(), (a, b) -> a, TreeMap::new));
            this.stops = stops.stream().collect(Collectors.toMap(Stop::getStopId, Function.identity()));
        }

        public StopTime getFirstStopTime() {
            return stopTimes.firstEntry().getValue();
        }

        public LocalDateTime getStartTime() {
            return TimeUtils.gtfsTimeToLocalDateTime(operatingDate, getFirstStopTime().getDepartureTime());
        }

        public TripDescriptor getTripDescriptor() {
            return new TripDescriptor(
                    route.getRouteId(),
                    route.getRouteShortName(),
                    operatingDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    HfpUtils.formatStartTime(getFirstStopTime().getDepartureTime()),
                    String.valueOf(trip.getDirectionId() + 1),
                    trip.getTripHeadsign()
            );
        }
    }
}
