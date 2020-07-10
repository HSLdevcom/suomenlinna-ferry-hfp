package fi.hsl.suomenlinna_hfp.common.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MathUtilsTest {
    @Test
    public void testPercentageAsInteger() {
        assertEquals(100, MathUtils.percentageAsInteger(1));
        assertEquals(88, MathUtils.percentageAsInteger(0.8794151));
        assertEquals(0, MathUtils.percentageAsInteger(0));
        assertEquals(50, MathUtils.percentageAsInteger(0.5));
    }

    @Test
    public void testClamp() {
        assertEquals(1, MathUtils.clamp(1.5, 0, 1), 0.001);
        assertEquals(0.5, MathUtils.clamp(0.5, 0, 1), 0.001);
        assertEquals(0, MathUtils.clamp(-7, 0, 1), 0.001);
    }
}
