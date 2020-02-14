package fi.hsl.suomenlinna_hfp.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SpeedUtilsTest {
    @Test
    public void testKnotsToMetresPerSecond() {
        assertEquals(0, SpeedUtils.knotsToMetresPerSecond(0), 0.001);
        assertEquals(1.0288, SpeedUtils.knotsToMetresPerSecond(2), 0.001);
        assertEquals(5.1444, SpeedUtils.knotsToMetresPerSecond(10), 0.001);
    }
}
