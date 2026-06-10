package com.airavat.astralab.core;

public record FlightSample(
        double time,
        double altitude,
        double downrange,
        double velocity,
        double acceleration,
        double mass,
        double mach,
        double dynamicPressure,
        double x,
        double y,
        double z) {
}
