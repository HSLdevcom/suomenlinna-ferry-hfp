package fi.hsl.suomenlinna_hfp.common.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimeUtilsTest {
    @Test
    public void testParseTime() {
        assertEquals(35700, TimeUtils.parseTime("09:55:00"));
        assertEquals(35700, TimeUtils.parseTime("9:55:00"));
        assertEquals(102600, TimeUtils.parseTime("28:30:00"));
        assertEquals(30, TimeUtils.parseTime("00:00:30"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseTimeWithInvalidValue() {
        TimeUtils.parseTime("test");
    }
}
