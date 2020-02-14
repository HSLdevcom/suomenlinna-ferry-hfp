package fi.hsl.suomenlinna_hfp.common.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LatLngTest {
    @Test
    public void testDistanceTo() {
        LatLng from = new LatLng(60.1992303,24.9407805); //Opastinsilta 6A
        LatLng to = new LatLng(60.1987249,24.933365); //Pasila station

        assertEquals(413, from.distanceTo(to), 1);
    }
}
