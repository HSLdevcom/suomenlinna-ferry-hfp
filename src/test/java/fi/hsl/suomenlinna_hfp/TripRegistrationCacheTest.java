package fi.hsl.suomenlinna_hfp;

import fi.hsl.suomenlinna_hfp.hfp.model.TripDescriptor;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TripRegistrationCacheTest {
    private TripRegistrationCache tripRegistrationCache;

    @Before
    public void setup() {
        tripRegistrationCache = new TripRegistrationCache(Duration.of(1, ChronoUnit.DAYS));
    }

    @Test
    public void testNoTripHasBeenRegisteredForPatternInitially() {
        assertFalse(tripRegistrationCache.hasAnyTripBeenRegisteredForPattern(new TripDescriptor("1", "1", "2020-01-01", "00:00", "1", "")));
    }

    @Test
    public void testNoTripHasBeenRegisteredInitially() {
        assertFalse(tripRegistrationCache.hasTripBeenRegistered(new TripDescriptor("1", "1", "2020-01-01", "00:00", "1", "")));
    }

    @Test
    public void testTripCanBeRegistered() {
        TripDescriptor tripDescriptor = new TripDescriptor("1", "1", "2020-01-01", "00:00", "1", "");
        tripRegistrationCache.addRegistration(tripDescriptor);

        assertTrue(tripRegistrationCache.hasTripBeenRegistered(tripDescriptor));
    }

    @Test
    public void testRegisteringTripsOfSamePattern() {
        TripDescriptor trip1 = new TripDescriptor("1", "1", "2020-01-01", "00:00", "1", "");
        TripDescriptor trip2 = new TripDescriptor("1", "1", "2020-01-01", "00:40", "1", "");

        tripRegistrationCache.addRegistration(trip1);

        assertTrue(tripRegistrationCache.hasAnyTripBeenRegisteredForPattern(trip2));
    }
}
