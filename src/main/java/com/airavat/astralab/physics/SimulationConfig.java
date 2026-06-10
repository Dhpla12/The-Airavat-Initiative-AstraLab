package com.airavat.astralab.physics;

public record SimulationConfig(
        double dragScale,
        double thrustScale,
        double dryMassScale,
        double windScale,
        double windOffset,
        boolean recordSamples) {

    public static SimulationConfig nominal() {
        return new SimulationConfig(1.0, 1.0, 1.0, 1.0, 0.0, true);
    }
}
