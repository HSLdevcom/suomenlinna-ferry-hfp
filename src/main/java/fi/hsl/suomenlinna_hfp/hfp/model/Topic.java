package fi.hsl.suomenlinna_hfp.hfp.model;

import java.util.Locale;
import java.util.Objects;

public class Topic {
    public static final String HFP_V2_PREFIX = "/hfp/v2";

    public final String prefix;
    public final JourneyType journeyType;
    public final TemporalType temporalType;
    public final EventType eventType;
    public final TransportMode transportMode;
    public final VehicleId vehicleId;
    public final TripDescriptor tripDescriptor;
    public final String nextStop;
    public final Integer geohashLevel;
    public final Geohash geohash;

    public Topic(String prefix, JourneyType journeyType, TemporalType temporalType, EventType eventType, TransportMode transportMode, VehicleId vehicleId, TripDescriptor tripDescriptor, String nextStop, Integer geohashLevel, Geohash geohash) {
        this.prefix = prefix;
        this.journeyType = journeyType;
        this.temporalType = temporalType;
        this.eventType = eventType;
        this.transportMode = transportMode;
        this.vehicleId = vehicleId;
        this.tripDescriptor = tripDescriptor;
        this.nextStop = nextStop;
        this.geohashLevel = geohashLevel;
        this.geohash = geohash;
    }

    public Topic(String prefix, JourneyType journeyType, TemporalType temporalType, EventType eventType, TransportMode transportMode, VehicleId vehicleId) {
        this(prefix, journeyType, temporalType, eventType, transportMode, vehicleId, null, null, null, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Topic topic = (Topic) o;
        return Objects.equals(prefix, topic.prefix) &&
                journeyType == topic.journeyType &&
                temporalType == topic.temporalType &&
                eventType == topic.eventType &&
                transportMode == topic.transportMode &&
                Objects.equals(vehicleId, topic.vehicleId) &&
                Objects.equals(tripDescriptor, topic.tripDescriptor) &&
                Objects.equals(nextStop, topic.nextStop) &&
                Objects.equals(geohashLevel, topic.geohashLevel) &&
                Objects.equals(geohash, topic.geohash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, journeyType, temporalType, eventType, transportMode, vehicleId, tripDescriptor, nextStop, geohashLevel, geohash);
    }

    @Override
    public String toString() {
        String deadrun = String.join("/",
                prefix,
                journeyType.toString(),
                temporalType.toString(),
                eventType.toString(),
                transportMode.toString(),
                String.format("%04d", vehicleId.operatorId),
                String.format("%05d", vehicleId.vehicleId));

        if (journeyType != JourneyType.DEADRUN) {
            return String.join("/",
                    deadrun,
                    tripDescriptor.routeId,
                    tripDescriptor.directionId,
                    tripDescriptor.headsign,
                    tripDescriptor.startTime,
                    nextStop,
                    String.valueOf(geohashLevel),
                    geohash.toString());
        } else {
            return deadrun;
        }
    }

    public enum JourneyType {
        JOURNEY, DEADRUN;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum TemporalType {
        ONGOING, UPCOMING;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum EventType {
        VP, DUE, ARR, DEP, ARS, PDE, PAS, WAIT, DOO, DOC, TLR, TLA, DA, DOUT, BA, VOUT, VJA, VJOUT;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum TransportMode {
        BUS, TRAM, TRAIN, FERRY, METRO;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
