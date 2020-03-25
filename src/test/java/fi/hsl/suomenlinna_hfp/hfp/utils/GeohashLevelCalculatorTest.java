package fi.hsl.suomenlinna_hfp.hfp.utils;

import fi.hsl.suomenlinna_hfp.common.model.LatLng;
import fi.hsl.suomenlinna_hfp.hfp.model.TripDescriptor;
import fi.hsl.suomenlinna_hfp.hfp.model.VehicleId;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GeohashLevelCalculatorTest {
    private GeohashLevelCalculator geohashLevelCalculator;

    @Before
    public void setup() {
        geohashLevelCalculator = new GeohashLevelCalculator();
    }

    @Test
    public void testGeohashLevelIsZeroWhenTripOrNextStopChanges() {
        VehicleId vehicleId = new VehicleId(60, 2);

        TripDescriptor initialTripDescriptor = new TripDescriptor("1019", "19", "2020-01-01", "00:00", "2", "Kauppatori");
        String nextStop = "1";
        LatLng position = new LatLng(0, 0);

        //0 initially
        assertEquals(0, geohashLevelCalculator.getGeohashLevel(vehicleId, position, initialTripDescriptor, nextStop));

        //0 after next stop changes
        assertEquals(0, geohashLevelCalculator.getGeohashLevel(vehicleId, position, initialTripDescriptor, "2"));

        //0 after trip descriptor changes
        TripDescriptor tripDescriptor = new TripDescriptor("1019", "19", "2020-01-01", "00:00", "1", "Suomenlinna");
        assertEquals(0, geohashLevelCalculator.getGeohashLevel(vehicleId, position, tripDescriptor, "2"));
    }

    @Test
    public void testGeohashLevelIsZeroWhenIntegerCoordinateChanges() {
        VehicleId vehicleId = new VehicleId(60, 2);

        TripDescriptor tripDescriptor = new TripDescriptor("1019", "19", "2020-01-01", "00:00", "2", "Kauppatori");
        String nextStop = "1";
        LatLng position = new LatLng(65.52151, 24.51261);

        //0 initially
        assertEquals(0, geohashLevelCalculator.getGeohashLevel(vehicleId, position, tripDescriptor, nextStop));

        //0 when integer part of coordinates change
        assertEquals(0, geohashLevelCalculator.getGeohashLevel(vehicleId, new LatLng(66.42155, 24.65161), tripDescriptor, nextStop));
    }

    @Test
    public void testGeohashLevelIsCalculatedCorrectly() {
        VehicleId vehicleId = new VehicleId(60, 2);

        TripDescriptor tripDescriptor = new TripDescriptor("1019", "19", "2020-01-01", "00:00", "2", "Kauppatori");
        String nextStop = "1";
        LatLng position = new LatLng(65.521516, 24.512616);

        //0 initially
        assertEquals(0, geohashLevelCalculator.getGeohashLevel(vehicleId, position, tripDescriptor, nextStop));

        //Third digit of the fractional part changed
        position = new LatLng(65.523516, 24.512616);
        assertEquals(3, geohashLevelCalculator.getGeohashLevel(vehicleId, position, tripDescriptor, nextStop));

        //Sixth digit of the fractional part changed
        position = new LatLng(65.523516, 24.512617);
        assertEquals(5, geohashLevelCalculator.getGeohashLevel(vehicleId, position, tripDescriptor, nextStop));

        //First digit of the fractional part changed
        position = new LatLng(65.423516, 24.512617);
        assertEquals(1, geohashLevelCalculator.getGeohashLevel(vehicleId, position, tripDescriptor, nextStop));
    }
}
