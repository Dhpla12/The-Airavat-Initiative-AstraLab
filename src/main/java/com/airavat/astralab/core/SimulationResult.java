package com.airavat.astralab.core;

import java.util.Comparator;
import java.util.List;

public final class SimulationResult {
    private final RocketModel rocket;
    private final List<FlightSample> samples;
    private final double apogee;
    private final double maxVelocity;
    private final double maxAcceleration;
    private final double maxMach;
    private final double maxDynamicPressure;
    private final double landingDistance;
    private final double stabilityMargin;

    public SimulationResult(RocketModel rocket, List<FlightSample> samples) {
        this.rocket = rocket;
        this.samples = List.copyOf(samples);
        this.apogee = samples.stream().mapToDouble(FlightSample::altitude).max().orElse(0.0);
        this.maxVelocity = samples.stream().mapToDouble(FlightSample::velocity).max().orElse(0.0);
        this.maxAcceleration = samples.stream().mapToDouble(FlightSample::acceleration).max().orElse(0.0);
        this.maxMach = samples.stream().mapToDouble(FlightSample::mach).max().orElse(0.0);
        this.maxDynamicPressure = samples.stream().mapToDouble(FlightSample::dynamicPressure).max().orElse(0.0);
        this.landingDistance = samples.stream()
                .max(Comparator.comparingDouble(FlightSample::time))
                .map(FlightSample::downrange)
                .orElse(0.0);
        this.stabilityMargin = rocket.stabilityMarginCalibers();
    }

    public RocketModel rocket() {
        return rocket;
    }

    public List<FlightSample> samples() {
        return samples;
    }

    public double apogee() {
        return apogee;
    }

    public double maxVelocity() {
        return maxVelocity;
    }

    public double maxAcceleration() {
        return maxAcceleration;
    }

    public double maxMach() {
        return maxMach;
    }

    public double maxDynamicPressure() {
        return maxDynamicPressure;
    }

    public double landingDistance() {
        return landingDistance;
    }

    public double stabilityMargin() {
        return stabilityMargin;
    }
}
