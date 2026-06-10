package com.airavat.astralab.physics;

import com.airavat.astralab.core.RocketModel;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MotorThrustCurve {
    private static final Pattern DESIGNATION = Pattern.compile("([A-Z])([0-9]+(?:\\.[0-9]+)?)");

    private final String designation;
    private final double totalImpulse;
    private final double averageThrust;
    private final double burnTime;
    private final double propellantMass;

    private MotorThrustCurve(String designation, double totalImpulse, double averageThrust, double propellantMass) {
        this.designation = designation;
        this.totalImpulse = totalImpulse;
        this.averageThrust = averageThrust;
        this.burnTime = totalImpulse / Math.max(1.0, averageThrust);
        this.propellantMass = propellantMass;
    }

    public static MotorThrustCurve from(RocketModel.Motor motor) {
        String designation = motor.thrustCurve() == null ? "E12" : motor.thrustCurve().toUpperCase(Locale.ROOT);
        Matcher matcher = DESIGNATION.matcher(designation);
        double impulse = 40.0;
        double thrust = 12.0;
        if (matcher.matches()) {
            impulse = classImpulse(matcher.group(1).charAt(0));
            thrust = Double.parseDouble(matcher.group(2));
        }
        if (motor.impulseOverrideNs() > 0.0) {
            impulse = motor.impulseOverrideNs();
        }
        double propellant = motor.propellantMass() > 0.0 ? motor.propellantMass() : impulse / (180.0 * 9.80665);
        return new MotorThrustCurve(designation, impulse, thrust, propellant);
    }

    public String designation() {
        return designation;
    }

    public double totalImpulse() {
        return totalImpulse;
    }

    public double averageThrust() {
        return averageThrust;
    }

    public double burnTime() {
        return burnTime;
    }

    public double propellantMass() {
        return propellantMass;
    }

    public double integratedImpulse(double thrustScale) {
        if (burnTime <= 0.0) {
            return 0.0;
        }
        int intervals = 400;
        double dt = burnTime / intervals;
        double impulse = 0.0;
        double previous = thrust(0.0, thrustScale);
        for (int i = 1; i <= intervals; i++) {
            double time = i * dt;
            double current = thrust(time, thrustScale);
            impulse += (previous + current) * 0.5 * dt;
            previous = current;
        }
        return impulse;
    }

    public double thrust(double time, double thrustScale) {
        if (time < 0.0 || time > burnTime) {
            return 0.0;
        }
        double phase = time / burnTime;
        double shape = phase < 0.08
                ? phase / 0.08
                : phase > 0.88 ? Math.max(0.0, (1.0 - phase) / 0.12) : 1.0;
        double correction = totalImpulse / (averageThrust * burnTime * 0.94);
        return averageThrust * shape * correction * thrustScale;
    }

    public double massFlow(double time, double thrustScale) {
        if (time < 0.0 || time > burnTime || burnTime <= 0.0) {
            return 0.0;
        }
        return propellantMass / burnTime * thrustScale;
    }

    private static double classImpulse(char letter) {
        int index = Math.max(0, letter - 'A');
        return 2.5 * Math.pow(2.0, index);
    }
}
