package com.airavat.astralab.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtmosphereTest {
    @Test
    void seaLevelPropertiesMatchStandardAtmosphere() {
        assertEquals(288.15, Atmosphere.temperature(0.0), 0.01);
        assertEquals(101_325.0, Atmosphere.pressure(0.0), 0.5);
        assertEquals(1.225, Atmosphere.density(0.0), 0.002);
    }

    @Test
    void densityPressureAndTemperatureDecreaseThroughTroposphere() {
        assertTrue(Atmosphere.temperature(1_000.0) < Atmosphere.temperature(0.0));
        assertTrue(Atmosphere.pressure(5_000.0) < Atmosphere.pressure(1_000.0));
        assertTrue(Atmosphere.density(10_000.0) < Atmosphere.density(1_000.0));
    }
}
