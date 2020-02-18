package fi.hsl.suomenlinna_hfp.hfp.model;

import fi.hsl.suomenlinna_hfp.hfp.model.Geohash;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GeohashTest {
    @Test
    public void testGeohash() {
        Geohash geohash = new Geohash(60.123, 24.789);

        assertEquals("60;24/17/28/39", geohash.toString());
    }
}
