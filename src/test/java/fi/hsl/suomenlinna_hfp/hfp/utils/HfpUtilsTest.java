package fi.hsl.suomenlinna_hfp.hfp.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HfpUtilsTest {
    @Test
    public void testFormatStartTime() {
        assertEquals("7:00", HfpUtils.formatStartTime(25200));
        assertEquals("3:30", HfpUtils.formatStartTime(99000));
    }

    @Test
    public void testFormatTst() {
        assertEquals("2020-02-21T12:26:40.000Z", HfpUtils.formatTst(1582288000000L));
    }
}
