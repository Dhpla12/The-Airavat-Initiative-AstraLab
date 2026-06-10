package com.airavat.astralab.core;

import java.util.List;

public record SimulationDiagnostics(
        double centerOfGravityMeters,
        double centerOfPressureMeters,
        double staticMarginCalibers,
        StabilityClassification stabilityClassification,
        double dryMassKg,
        double propellantMassKg,
        double initialMassKg,
        double burnoutMassKg,
        double burnTimeSeconds,
        double averageThrustNewtons,
        double totalImpulseNewtonSeconds,
        double integratedImpulseNewtonSeconds,
        double thrustToWeightRatio,
        double launchAccelerationMetersPerSecond2,
        List<String> warnings) {

    public SimulationDiagnostics {
        stabilityClassification = stabilityClassification == null
                ? StabilityClassification.fromMargin(staticMarginCalibers)
                : stabilityClassification;
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
