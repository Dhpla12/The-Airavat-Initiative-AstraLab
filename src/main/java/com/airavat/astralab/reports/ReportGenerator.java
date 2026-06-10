package com.airavat.astralab.reports;

import com.airavat.astralab.core.MonteCarloResult;
import com.airavat.astralab.core.RocketModel;
import com.airavat.astralab.core.SimulationDiagnostics;
import com.airavat.astralab.core.SimulationResult;
import com.airavat.astralab.physics.PhysicsValidator;

import java.time.Instant;

public final class ReportGenerator {
    public String flightSummary(SimulationResult result) {
        return flightSummary(result, result.rocket().name());
    }

    public String flightSummary(SimulationResult result, String projectName) {
        RocketModel rocket = result.rocket();
        SimulationDiagnostics diagnostics = PhysicsValidator.analyze(rocket);
        return """
                AstraLab Flight Report

                Project Name: %s
                Rocket Name: %s
                Timestamp: %s

                Apogee: %.1f m
                Maximum Velocity: %.1f m/s
                Maximum Acceleration: %.1f m/s^2
                Maximum Mach Number: %.2f
                Maximum Dynamic Pressure: %.1f Pa
                Landing Distance: %.1f m

                Center of Gravity: %.3f m from nose
                Center of Pressure: %.3f m from nose
                Static Margin: %.2f calibers
                Stability Classification: %s

                Thrust-to-Weight Ratio: %.2f
                Launch Acceleration: %.2f m/s^2
                Dry Mass: %.3f kg
                Propellant Mass: %.3f kg
                Initial Mass: %.3f kg
                Burnout Mass: %.3f kg
                Burn Time: %.2f s
                Total Impulse: %.2f N*s
                Integrated Thrust: %.2f N*s

                Simulation Warnings:
                %s
                """.formatted(
                projectName == null || projectName.isBlank() ? rocket.name() : projectName,
                rocket.name(),
                Instant.now(),
                result.apogee(),
                result.maxVelocity(),
                result.maxAcceleration(),
                result.maxMach(),
                result.maxDynamicPressure(),
                result.landingDistance(),
                diagnostics.centerOfGravityMeters(),
                diagnostics.centerOfPressureMeters(),
                diagnostics.staticMarginCalibers(),
                diagnostics.stabilityClassification().displayName(),
                diagnostics.thrustToWeightRatio(),
                diagnostics.launchAccelerationMetersPerSecond2(),
                diagnostics.dryMassKg(),
                diagnostics.propellantMassKg(),
                diagnostics.initialMassKg(),
                diagnostics.burnoutMassKg(),
                diagnostics.burnTimeSeconds(),
                diagnostics.totalImpulseNewtonSeconds(),
                diagnostics.integratedImpulseNewtonSeconds(),
                warningBlock(diagnostics, result));
    }

    public String monteCarloSummary(MonteCarloResult result) {
        MonteCarloResult.Summary apogee = result.apogee();
        MonteCarloResult.Summary landing = result.landingDistance();
        return """
                Monte Carlo Summary

                Runs: %d

                Apogee Distribution:
                Mean: %.1f m
                Std Dev: %.1f m
                Min: %.1f m
                Max: %.1f m

                Landing Distribution:
                Mean: %.1f m
                Std Dev: %.1f m
                Min: %.1f m
                Max: %.1f m
                """.formatted(
                apogee.count(),
                apogee.mean(),
                apogee.stdDev(),
                apogee.min(),
                apogee.max(),
                landing.mean(),
                landing.stdDev(),
                landing.min(),
                landing.max());
    }

    private static String warningBlock(SimulationDiagnostics diagnostics) {
        return warningBlock(diagnostics, null);
    }

    private static String warningBlock(SimulationDiagnostics diagnostics, SimulationResult result) {
        java.util.List<String> warnings = new java.util.ArrayList<>(diagnostics.warnings());
        if (result != null) {
            warnings.addAll(PhysicsValidator.resultWarnings(result));
        }
        if (warnings.isEmpty()) {
            return "None";
        }
        return String.join("\n", warnings);
    }
}
