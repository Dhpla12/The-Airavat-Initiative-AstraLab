package com.airavat.astralab.physics;

public record Quaternion(double w, double x, double y, double z) {
    public static final Quaternion IDENTITY = new Quaternion(1.0, 0.0, 0.0, 0.0);

    public Quaternion multiply(Quaternion other) {
        return new Quaternion(
                w * other.w - x * other.x - y * other.y - z * other.z,
                w * other.x + x * other.w + y * other.z - z * other.y,
                w * other.y - x * other.z + y * other.w + z * other.x,
                w * other.z + x * other.y - y * other.x + z * other.w);
    }

    public Quaternion add(Quaternion other) {
        return new Quaternion(w + other.w, x + other.x, y + other.y, z + other.z);
    }

    public Quaternion multiply(double scalar) {
        return new Quaternion(w * scalar, x * scalar, y * scalar, z * scalar);
    }

    public Quaternion normalize() {
        double norm = Math.sqrt(w * w + x * x + y * y + z * z);
        if (norm < 1.0e-9) {
            return IDENTITY;
        }
        return new Quaternion(w / norm, x / norm, y / norm, z / norm);
    }

    public Quaternion conjugate() {
        return new Quaternion(w, -x, -y, -z);
    }

    public Vector3 rotate(Vector3 vector) {
        Quaternion result = multiply(new Quaternion(0.0, vector.x(), vector.y(), vector.z())).multiply(conjugate());
        return new Vector3(result.x(), result.y(), result.z());
    }

    public Quaternion derivative(Vector3 angularVelocity) {
        Quaternion omega = new Quaternion(0.0, angularVelocity.x(), angularVelocity.y(), angularVelocity.z());
        return multiply(omega).multiply(0.5);
    }

    public static Quaternion fromAxisAngle(Vector3 axis, double radians) {
        Vector3 n = axis.normalize();
        double half = radians / 2.0;
        double sin = Math.sin(half);
        return new Quaternion(Math.cos(half), n.x() * sin, n.y() * sin, n.z() * sin).normalize();
    }

    public static Quaternion fromTo(Vector3 from, Vector3 to) {
        Vector3 f = from.normalize();
        Vector3 t = to.normalize();
        double dot = f.dot(t);
        if (dot > 0.999999) {
            return IDENTITY;
        }
        if (dot < -0.999999) {
            Vector3 axis = Math.abs(f.x()) < 0.9 ? f.cross(new Vector3(1.0, 0.0, 0.0)) : f.cross(new Vector3(0.0, 1.0, 0.0));
            return fromAxisAngle(axis, Math.PI);
        }
        Vector3 cross = f.cross(t);
        return new Quaternion(1.0 + dot, cross.x(), cross.y(), cross.z()).normalize();
    }
}
