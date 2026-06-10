package com.airavat.astralab.physics;

public record RigidBodyState(
        Vector3 position,
        Vector3 velocity,
        Quaternion orientation,
        Vector3 angularVelocity,
        double mass) {

    public RigidBodyState addScaled(StateDerivative derivative, double scale) {
        return new RigidBodyState(
                position.add(derivative.positionDot().multiply(scale)),
                velocity.add(derivative.velocityDot().multiply(scale)),
                orientation.add(derivative.orientationDot().multiply(scale)).normalize(),
                angularVelocity.add(derivative.angularVelocityDot().multiply(scale)),
                Math.max(0.001, mass + derivative.massDot() * scale));
    }
}
