package com.airavat.astralab.physics;

public record Vector3(double x, double y, double z) {
    public static final Vector3 ZERO = new Vector3(0.0, 0.0, 0.0);
    public static final Vector3 Z = new Vector3(0.0, 0.0, 1.0);

    public Vector3 add(Vector3 other) {
        return new Vector3(x + other.x, y + other.y, z + other.z);
    }

    public Vector3 subtract(Vector3 other) {
        return new Vector3(x - other.x, y - other.y, z - other.z);
    }

    public Vector3 multiply(double scalar) {
        return new Vector3(x * scalar, y * scalar, z * scalar);
    }

    public Vector3 divide(double scalar) {
        return scalar == 0.0 ? ZERO : multiply(1.0 / scalar);
    }

    public double dot(Vector3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public Vector3 cross(Vector3 other) {
        return new Vector3(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x);
    }

    public double magnitude() {
        return Math.sqrt(dot(this));
    }

    public Vector3 normalize() {
        double length = magnitude();
        return length < 1.0e-9 ? ZERO : divide(length);
    }

    public double horizontalMagnitude() {
        return Math.sqrt(x * x + y * y);
    }
}
