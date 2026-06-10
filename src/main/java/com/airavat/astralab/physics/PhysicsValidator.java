package com.airavat.astralab.physics;

import com.airavat.astralab.core.RocketModel;
import com.airavat.astralab.core.SimulationDiagnostics;
import com.airavat.astralab.core.SimulationResult;
import com.airavat.astralab.core.StabilityClassification;

import java.util.ArrayList;
import java.util.List;

public final class PhysicsValidator {
    private static final double G0 = 9.80665;
    private static final double PEAK_ACCELERATION_WARNING_MPS2 = 100.0;

    private PhysicsValidator() {
    }

    public static SimulationDiagnostics analyze(RocketModel rocket) {
        MotorThrustCurve motor = MotorThrustCurve.from(rocket.motor());
        double dryMass = rocket.dryMass();
        double propellantMass = motor.propellantMass();
        double initialMass = dryMass + propellantMass;
        double burnoutMass = dryMass;
        double staticMargin = rocket.stabilityMarginCalibers();
        StabilityClassification classification = StabilityClassification.fromMargin(staticMargin);
        double thrustToWeight = initialMass > 0.0
                ? motor.averageThrust() / (initialMass * G0)
                : 0.0;
        double launchAcceleration = initialMass > 0.0
                ? motor.averageThrust() / initialMass - G0
                : 0.0;
        double integratedImpulse = motor.integratedImpulse(1.0);

        List<String> warnings = new ArrayList<>();
        if (staticMargin < 0.0) {
            warnings.add("WARNING: Rocket is statically unstable. Results may not represent a physically flyable vehicle.");
        } else if (staticMargin < 1.0) {
            warnings.add("WARNING: Rocket is marginally stable. Verify CG, CP, and fin sizing before flight use.");
        }
        if (thrustToWeight < 1.2) {
            warnings.add("WARNING: Initial thrust-to-weight ratio is below 1.2. Rail departure may be unsafe or impossible.");
        }
        if (dryMass < 0.0 || propellantMass < 0.0 || initialMass < 0.0 || burnoutMass < 0.0) {
            warnings.add("WARNING: Negative mass was supplied or derived. Check dry mass and propellant mass inputs.");
        }
        if (hasImpossibleGeometry(rocket)) {
            warnings.add("WARNING: Impossible or non-positive geometry was supplied. Check body, nosecone, fin, recovery, and mission dimensions.");
        }
        if (rocket.motor().thrustCurve() == null || rocket.motor().thrustCurve().isBlank()) {
            warnings.add("WARNING: Missing thrust curve designation. AstraLab used the default educational motor approximation.");
        }
        double impulseError = motor.totalImpulse() <= 0.0
                ? 0.0
                : Math.abs(integratedImpulse - motor.totalImpulse()) / motor.totalImpulse();
        if (impulseError > 0.05) {
            warnings.add("WARNING: Integrated thrust differs from declared total impulse by more than 5%.");
        }
        double specificImpulse = propellantMass > 0.0 ? motor.totalImpulse() / (propellantMass * G0) : 0.0;
        if (propellantMass <= 0.0 || specificImpulse < 50.0 || specificImpulse > 350.0) {
            warnings.add("WARNING: Propellant mass and total impulse imply an unusual specific impulse. Verify motor data.");
        }

        return new SimulationDiagnostics(
                rocket.estimatedCg(),
                rocket.estimatedCp(),
                staticMargin,
                classification,
                dryMass,
                propellantMass,
                initialMass,
                burnoutMass,
                motor.burnTime(),
                motor.averageThrust(),
                motor.totalImpulse(),
                integratedImpulse,
                thrustToWeight,
                launchAcceleration,
                warnings);
    }

    public static StabilityClassification classifyStaticMargin(double staticMarginCalibers) {
        return StabilityClassification.fromMargin(staticMarginCalibers);
    }

    public static List<String> resultWarnings(SimulationResult result) {
        List<String> warnings = new ArrayList<>();
        if (result.samples().isEmpty()) {
            warnings.add("WARNING: Horizontal trajectory data unavailable.");
            warnings.add("WARNING: Landing distance could not be computed.");
            return warnings;
        }

        RocketModel rocket = result.rocket();
        boolean horizontalMotionExpected = Math.abs(rocket.mission().launchAngleDeg()) > 0.1
                || Math.abs(rocket.environment().windSpeed()) > 0.1;
        if (horizontalMotionExpected && result.landingDistance() < 1.0e-3) {
            warnings.add("WARNING: Horizontal trajectory data unavailable.");
        }

        double finalAltitude = result.samples().get(result.samples().size() - 1).altitude();
        if (finalAltitude > 0.5) {
            warnings.add("WARNING: Landing distance could not be computed.");
        }

        if (result.maxAcceleration() > PEAK_ACCELERATION_WARNING_MPS2) {
            warnings.add("WARNING: Peak acceleration exceeds recommended validation threshold.");
        }
        return List.copyOf(warnings);
    }

    private static boolean hasImpossibleGeometry(RocketModel rocket) {
        return rocket.nosecone().length() <= 0.0
                || rocket.body().length() <= 0.0
                || rocket.body().diameter() <= 0.0
                || rocket.fins().count() < 0
                || rocket.fins().span() < 0.0
                || rocket.fins().sweep() < 0.0
                || rocket.recovery().parachuteArea() < 0.0
                || rocket.mission().railLength() <= 0.0
                || rocket.mission().maxTime() <= 0.0
                || rocket.environment().temperatureK() <= 0.0;
    }
}
