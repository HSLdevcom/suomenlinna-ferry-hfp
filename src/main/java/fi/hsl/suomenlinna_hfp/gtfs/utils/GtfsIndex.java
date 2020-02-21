package fi.hsl.suomenlinna_hfp.gtfs.utils;

import fi.hsl.suomenlinna_hfp.gtfs.model.Route;
import fi.hsl.suomenlinna_hfp.gtfs.model.Stop;
import fi.hsl.suomenlinna_hfp.gtfs.model.StopTime;
import fi.hsl.suomenlinna_hfp.gtfs.model.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class GtfsIndex {
    private static final Logger LOG = LoggerFactory.getLogger(GtfsIndex.class);

    public final Map<String, Stop> stopsById;
    public final Map<String, Trip> tripsById;
    public final Map<String, Route> routesById;
    public final Map<String, List<Trip>> tripsByRouteId;
    public final Map<String, SortedSet<StopTime>> stopTimesByTripId;
    public final Map<String, Set<StopTime>> stopTimesByStopId;

    public GtfsIndex(Collection<Stop> stops, Collection<Trip> trips, Collection<Route> routes, Collection<StopTime> stopTimes) {
        long start = System.nanoTime();
        LOG.info("Indexing GTFS data");

        Map<String, Stop> stopsById = new HashMap<>(stops.size());
        stops.forEach(stop -> stopsById.put(stop.getId(), stop));

        Map<String, Trip> tripsById = new HashMap<>(trips.size());
        trips.forEach(trip -> tripsById.put(trip.getTripId(), trip));
        this.tripsByRouteId = Collections.unmodifiableMap(trips.stream()
                .collect(Collectors.groupingBy(Trip::getRouteId)).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Collections.unmodifiableList(entry.getValue())))
        );

        Map<String, Route> routesById = new HashMap<>(routes.size());
        routes.forEach(route -> routesById.put(route.getId(), route));

        Map<String, SortedSet<StopTime>> stopTimesByTripId = new HashMap<>(trips.size());
        Map<String, Set<StopTime>> stopTimesByStopId = new HashMap<>(stops.size());
        stopTimes.forEach(stopTime -> {
            stopTimesByTripId.compute(stopTime.getTripId(), (key, set) -> {
                if (set == null) {
                    set = new TreeSet<>(Comparator.comparingInt(StopTime::getStopSequence));
                }
                set.add(stopTime);
                return set;
            });

            stopTimesByStopId.compute(stopTime.getStopId(), (ket, set) -> {
                if (set == null) {
                    set = new HashSet<>();
                }
                set.add(stopTime);
                return set;
            });
        });

        this.stopsById = Collections.unmodifiableMap(stopsById);
        this.tripsById = Collections.unmodifiableMap(tripsById);
        this.routesById = Collections.unmodifiableMap(routesById);
        this.stopTimesByTripId = Collections.unmodifiableMap(stopTimesByTripId.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> Collections.unmodifiableSortedSet(entry.getValue()))));
        this.stopTimesByStopId = Collections.unmodifiableMap(stopTimesByStopId.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> Collections.unmodifiableSet(entry.getValue()))));

        LOG.info("GTFS data indexed in {}ms", (System.nanoTime() - start) / 1000000);
    }
}
