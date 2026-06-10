package com.airavat.astralab.physics;

public record StateDerivative(
        Vector3 positionDot,
        Vector3 velocityDot,
        Quaternion orientationDot,
        Vector3 angularVelocityDot,
        double massDot) {

    public StateDerivative add(StateDerivative other) {
        return new StateDerivative(
                positionDot.add(other.positionDot),
                velocityDot.add(other.velocityDot),
                orientationDot.add(other.orientationDot),
                angularVelocityDot.add(other.angularVelocityDot),
                massDot + other.massDot);
    }

    public StateDerivative multiply(double scalar) {
        return new StateDerivative(
                positionDot.multiply(scalar),
                velocityDot.multiply(scalar),
                orientationDot.multiply(scalar),
                angularVelocityDot.multiply(scalar),
                massDot * scalar);
    }
}
