package com.airavat.astralab.physics;

import com.airavat.astralab.arvt.ArvtInterpreter;
import com.airavat.astralab.core.FlightSample;
import com.airavat.astralab.core.RocketModel;
import com.airavat.astralab.core.SimulationDiagnostics;
import com.airavat.astralab.core.SimulationResult;
import com.airavat.astralab.core.StabilityClassification;
import com.airavat.astralab.reports.ReportGenerator;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhysicsModelTest {
    private static final String SOURCE = """
            rocket MassRocket

            nosecone {
                length 0.25m
                mass 0.08kg
            }

            body {
                length 1.2m
                diameter 0.08m
                mass 0.35kg
            }

            fins {
                count 4
                span 0.12m
                sweep 0.05m
                mass 0.06kg
            }

            motor {
                thrust_curve E12
                propellant_mass 0.025kg
            }

            recovery {
                parachute_area 0.8m2
                deploy_delay 0.5s
                mass 0.05kg
            }

            simulate
            """;

    @Test
    void massDecreasesContinuouslyDuringBurnAndEndsAtBurnoutMass() {
        SimulationResult result = new ArvtInterpreter().execute(SOURCE).simulationResult().orElseThrow();
        MotorThrustCurve motor = MotorThrustCurve.from(result.rocket().motor());
        double previousMass = Double.POSITIVE_INFINITY;
        for (FlightSample sample : result.samples()) {
            if (sample.time() <= motor.burnTime()) {
                assertTrue(sample.mass() <= previousMass + 1.0e-6);
                previousMass = sample.mass();
            }
        }

        double postBurnMass = result.samples().stream()
                .filter(sample -> sample.time() >= motor.burnTime() + 0.1)
                .findFirst()
                .orElseThrow()
                .mass();
        assertEquals(result.rocket().dryMass(), postBurnMass, 0.01);
    }

    @Test
    void staticStabilityClassificationUsesRequestedThresholds() {
        assertEquals(StabilityClassification.UNSTABLE, PhysicsValidator.classifyStaticMargin(-0.01));
        assertEquals(StabilityClassification.MARGINALLY_STABLE, PhysicsValidator.classifyStaticMargin(0.5));
        assertEquals(StabilityClassification.STABLE, PhysicsValidator.classifyStaticMargin(1.5));
        assertEquals(StabilityClassification.VERY_STABLE, PhysicsValidator.classifyStaticMargin(2.1));
    }

    @Test
    void unstableRocketProducesProminentWarning() {
        RocketModel rocket = RocketModel.builder("UnstableRocket")
                .body(Map.of("length", "1.2m", "diameter", "0.08m", "mass", "0.2kg"))
                .nosecone(Map.of("length", "0.25m", "mass", "0.05kg"))
                .fins(Map.of("count", "0", "span", "0m", "sweep", "0m", "mass", "0kg"))
                .payload(Map.of("mass", "1.5kg"))
                .motor(Map.of("thrust_curve", "E12", "propellant_mass", "0.025kg"))
                .build();

        SimulationDiagnostics diagnostics = PhysicsValidator.analyze(rocket);

        assertEquals(StabilityClassification.UNSTABLE, diagnostics.stabilityClassification());
        assertTrue(diagnostics.staticMarginCalibers() < 0.0);
        assertTrue(diagnostics.warnings().stream().anyMatch(warning -> warning.contains("statically unstable")));
    }

    @Test
    void verticalLaunchWithoutWindHasZeroLandingDistance() {
        SimulationResult result = new ArvtInterpreter().execute(trajectorySource("0deg", "0m/s")).simulationResult().orElseThrow();

        assertEquals(0.0, result.landingDistance(), 0.001);
    }

    @Test
    void angledLaunchReportsNonzeroLandingDistance() {
        SimulationResult result = new ArvtInterpreter().execute(trajectorySource("3deg", "0m/s")).simulationResult().orElseThrow();

        assertTrue(result.landingDistance() > 1.0);
        assertTrue(result.samples().get(result.samples().size() - 1).downrange() > 1.0);
    }

    @Test
    void windProducesHorizontalDriftOnVerticalLaunch() {
        SimulationResult result = new ArvtInterpreter().execute(trajectorySource("0deg", "10m/s")).simulationResult().orElseThrow();

        assertTrue(result.landingDistance() > 1.0);
    }

    @Test
    void peakAccelerationWarningAppearsInReport() {
        SimulationResult result = new ArvtInterpreter().execute(trajectorySource("20deg", "0m/s")).simulationResult().orElseThrow();

        assertTrue(PhysicsValidator.resultWarnings(result).stream()
                .anyMatch(warning -> warning.contains("Peak acceleration")));
        assertTrue(new ReportGenerator().flightSummary(result).contains("Peak acceleration exceeds recommended validation threshold"));
    }

    @Test
    void stableRocketProducesStableClassification() {
        RocketModel rocket = RocketModel.builder("StableRocket")
                .nosecone(Map.of("length", "0.25m", "mass", "0.8kg"))
                .body(Map.of("length", "1.2m", "diameter", "0.08m", "mass", "0.2kg"))
                .fins(Map.of("count", "4", "span", "0.4m", "sweep", "0.05m", "mass", "0.02kg"))
                .motor(Map.of("thrust_curve", "E12", "propellant_mass", "0.025kg"))
                .build();

        StabilityClassification classification = PhysicsValidator.analyze(rocket).stabilityClassification();
        assertTrue(classification == StabilityClassification.STABLE || classification == StabilityClassification.VERY_STABLE);
    }

    private static String trajectorySource(String launchAngle, String wind) {
        return """
                rocket TrajectoryRocket

                nosecone {
                    length 0.25m
                    mass 0.08kg
                }

                body {
                    length 1.2m
                    diameter 0.08m
                    mass 0.35kg
                }

                fins {
                    count 4
                    span 0.12m
                    sweep 0.05m
                    mass 0.06kg
                }

                motor {
                    thrust_curve E12
                    propellant_mass 0.025kg
                }

                recovery {
                    parachute_area 0.8m2
                    deploy_delay 0.5s
                    mass 0.05kg
                }

                environment {
                    wind %s
                    temperature 288K
                }

                mission {
                    launch_angle %s
                    rail_length 1.0m
                    max_time 90s
                }

                simulate
                """.formatted(wind, launchAngle);
    }
}
