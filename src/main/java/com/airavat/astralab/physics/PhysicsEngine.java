package com.airavat.astralab.physics;

import com.airavat.astralab.core.FlightSample;
import com.airavat.astralab.core.RocketModel;
import com.airavat.astralab.core.SimulationResult;

import java.util.ArrayList;
import java.util.List;

public final class PhysicsEngine {
    private static final double G0 = 9.80665;

    public SimulationResult simulate(RocketModel rocket) {
        return simulate(rocket, SimulationConfig.nominal());
    }

    public SimulationResult simulate(RocketModel rocket, SimulationConfig config) {
        MotorThrustCurve motor = MotorThrustCurve.from(rocket.motor());
        double dryMass = Math.max(0.05, rocket.dryMass() * config.dryMassScale());
        double initialMass = dryMass + motor.propellantMass();
        Vector3 railDirection = railDirection(rocket.mission().launchAngleDeg());
        Quaternion launchOrientation = Quaternion.fromTo(Vector3.Z, railDirection);
        RigidBodyState state = new RigidBodyState(
                Vector3.ZERO,
                Vector3.ZERO,
                launchOrientation,
                Vector3.ZERO,
                initialMass);

        List<FlightSample> samples = new ArrayList<>();
        double t = 0.0;
        double acceleration = 0.0;
        boolean recoveryDeployed = false;
        boolean launchRailCleared = false;
        int step = 0;

        while (t <= rocket.mission().maxTime()) {
            if (!recoveryDeployed
                    && state.position().z() > 5.0
                    && state.velocity().z() < 0.0
                    && t > motor.burnTime() + rocket.recovery().deployDelay()) {
                recoveryDeployed = true;
            }

            if (config.recordSamples() || step % 5 == 0) {
                samples.add(sample(rocket, config, state, t, acceleration, recoveryDeployed));
            }

            if (t > 0.5 && state.position().z() <= 0.0 && state.velocity().z() < 0.0) {
                break;
            }

            double dt = timeStep(t, motor, state);
            StateDerivative k1 = derivative(rocket, config, motor, state, t, recoveryDeployed, dryMass);
            StateDerivative k2 = derivative(rocket, config, motor, state.addScaled(k1, dt * 0.5), t + dt * 0.5, recoveryDeployed, dryMass);
            StateDerivative k3 = derivative(rocket, config, motor, state.addScaled(k2, dt * 0.5), t + dt * 0.5, recoveryDeployed, dryMass);
            StateDerivative k4 = derivative(rocket, config, motor, state.addScaled(k3, dt), t + dt, recoveryDeployed, dryMass);

            StateDerivative combined = k1
                    .add(k2.multiply(2.0))
                    .add(k3.multiply(2.0))
                    .add(k4)
                    .multiply(1.0 / 6.0);
            acceleration = combined.velocityDot().magnitude();
            state = state.addScaled(combined, dt);
            state = constrainMassAndGround(state, dryMass);
            if (!launchRailCleared) {
                launchRailCleared = state.position().dot(railDirection) >= rocket.mission().railLength();
                if (!launchRailCleared) {
                    state = constrainLaunchRail(state, rocket, railDirection, launchOrientation);
                }
            }
            t += dt;
            step++;
        }

        if (samples.isEmpty() || samples.get(samples.size() - 1).time() < t) {
            samples.add(sample(rocket, config, state, t, acceleration, recoveryDeployed));
        }
        return new SimulationResult(rocket, samples);
    }

    private static StateDerivative derivative(
            RocketModel rocket,
            SimulationConfig config,
            MotorThrustCurve motor,
            RigidBodyState state,
            double time,
            boolean recoveryDeployed,
            double dryMass) {

        double altitude = Math.max(0.0, state.position().z());
        double mass = Math.max(dryMass, state.mass());
        Vector3 axis = state.orientation().rotate(Vector3.Z).normalize();
        Vector3 wind = new Vector3(rocket.environment().windSpeed() * config.windScale() + config.windOffset(), 0.0, 0.0);
        Vector3 relativeVelocity = state.velocity().subtract(wind);
        double speed = relativeVelocity.magnitude();
        double density = Atmosphere.density(altitude, rocket.environment().temperatureK());
        double q = 0.5 * density * speed * speed;
        double mach = speed / Atmosphere.speedOfSound(altitude, rocket.environment().temperatureK());
        double cd = dragCoefficient(rocket, config, mach, recoveryDeployed);
        double area = rocket.referenceArea() + (recoveryDeployed ? Math.max(0.0, rocket.recovery().parachuteArea()) * 0.85 : 0.0);
        Vector3 drag = speed < 1.0e-6 ? Vector3.ZERO : relativeVelocity.normalize().multiply(-cd * q * area);
        Vector3 thrust = axis.multiply(motor.thrust(time, config.thrustScale()));
        Vector3 gravity = new Vector3(0.0, 0.0, -G0 * mass);
        Vector3 acceleration = thrust.add(drag).add(gravity).divide(mass);

        Vector3 angularAcceleration = angularAcceleration(rocket, state, relativeVelocity, q, mass);
        double massDot = state.mass() <= dryMass + 1.0e-6 ? 0.0 : -motor.massFlow(time, config.thrustScale());
        return new StateDerivative(
                state.velocity(),
                acceleration,
                state.orientation().derivative(state.angularVelocity()),
                angularAcceleration,
                massDot);
    }

    private static Vector3 angularAcceleration(
            RocketModel rocket,
            RigidBodyState state,
            Vector3 relativeVelocity,
            double dynamicPressure,
            double mass) {
        double speed = relativeVelocity.magnitude();
        if (speed < 2.0) {
            return state.angularVelocity().multiply(-0.5);
        }
        Vector3 axis = state.orientation().rotate(Vector3.Z).normalize();
        Vector3 flow = relativeVelocity.normalize();
        double stability = Math.max(-1.0, Math.min(4.0, rocket.stabilityMarginCalibers()));
        double authority = dynamicPressure * rocket.referenceArea() * rocket.length() * 0.035 * stability;
        Vector3 restoringTorque = axis.cross(flow).multiply(authority);
        Vector3 dampingTorque = state.angularVelocity().multiply(-0.14 * mass * rocket.length() * rocket.length());
        double inertia = mass * (3.0 * Math.pow(rocket.diameter() / 2.0, 2.0) + rocket.length() * rocket.length()) / 12.0;
        return restoringTorque.add(dampingTorque).divide(Math.max(0.001, inertia));
    }

    private static double dragCoefficient(RocketModel rocket, SimulationConfig config, double mach, boolean recoveryDeployed) {
        if (recoveryDeployed) {
            return 1.45;
        }
        double transonicBump = mach > 0.75 ? Math.min(0.18, (mach - 0.75) * 0.35) : 0.0;
        return (rocket.dragCoefficient() + transonicBump) * config.dragScale();
    }

    private static FlightSample sample(
            RocketModel rocket,
            SimulationConfig config,
            RigidBodyState state,
            double time,
            double acceleration,
            boolean recoveryDeployed) {
        double altitude = Math.max(0.0, state.position().z());
        Vector3 wind = new Vector3(rocket.environment().windSpeed() * config.windScale() + config.windOffset(), 0.0, 0.0);
        Vector3 relativeVelocity = state.velocity().subtract(wind);
        double speed = relativeVelocity.magnitude();
        double density = Atmosphere.density(altitude, rocket.environment().temperatureK());
        double dynamicPressure = 0.5 * density * speed * speed;
        double mach = speed / Atmosphere.speedOfSound(altitude, rocket.environment().temperatureK());
        double downrange = state.position().horizontalMagnitude();
        if (recoveryDeployed) {
            acceleration = Math.max(acceleration, 0.0);
        }
        return new FlightSample(
                time,
                altitude,
                downrange,
                state.velocity().magnitude(),
                acceleration,
                state.mass(),
                mach,
                dynamicPressure,
                state.position().x(),
                state.position().y(),
                state.position().z());
    }

    private static RigidBodyState constrainLaunchRail(
            RigidBodyState state,
            RocketModel rocket,
            Vector3 railDirection,
            Quaternion launchOrientation) {
        double railDistance = state.position().dot(railDirection);
        if (railDistance >= rocket.mission().railLength()) {
            return state;
        }
        double along = Math.max(0.0, railDistance);
        double velocityAlong = Math.max(0.0, state.velocity().dot(railDirection));
        return new RigidBodyState(
                railDirection.multiply(along),
                railDirection.multiply(velocityAlong),
                launchOrientation,
                Vector3.ZERO,
                state.mass());
    }

    private static RigidBodyState constrainMassAndGround(RigidBodyState state, double dryMass) {
        Vector3 position = state.position();
        Vector3 velocity = state.velocity();
        if (position.z() < 0.0) {
            position = new Vector3(position.x(), position.y(), 0.0);
            velocity = new Vector3(velocity.x(), velocity.y(), Math.min(0.0, velocity.z()));
        }
        return new RigidBodyState(
                position,
                velocity,
                state.orientation().normalize(),
                state.angularVelocity(),
                Math.max(dryMass, state.mass()));
    }

    private static Vector3 railDirection(double launchAngleDeg) {
        double angle = Math.toRadians(Math.max(0.0, launchAngleDeg));
        return new Vector3(Math.sin(angle), 0.0, Math.cos(angle)).normalize();
    }

    private static double timeStep(double time, MotorThrustCurve motor, RigidBodyState state) {
        double base = time <= motor.burnTime() + 0.5 ? 0.01 : 0.04;
        double velocityLimit = state.velocity().magnitude() > 250.0 ? 0.01 : base;
        return Math.max(0.005, Math.min(0.05, velocityLimit));
    }
}
