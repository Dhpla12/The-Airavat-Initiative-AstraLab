package com.airavat.astralab.core;

import java.util.Locale;
import java.util.Map;

public final class RocketModel {
    private final String name;
    private final Nosecone nosecone;
    private final Body body;
    private final Fins fins;
    private final Motor motor;
    private final Payload payload;
    private final Recovery recovery;
    private final Avionics avionics;
    private final Environment environment;
    private final Mission mission;

    private RocketModel(Builder builder) {
        this.name = builder.name;
        this.nosecone = builder.nosecone;
        this.body = builder.body;
        this.fins = builder.fins;
        this.motor = builder.motor;
        this.payload = builder.payload;
        this.recovery = builder.recovery;
        this.avionics = builder.avionics;
        this.environment = builder.environment;
        this.mission = builder.mission;
    }

    public static Builder builder(String name) {
        return new Builder(name == null || name.isBlank() ? "UnnamedRocket" : name.trim());
    }

    public String name() {
        return name;
    }

    public Nosecone nosecone() {
        return nosecone;
    }

    public Body body() {
        return body;
    }

    public Fins fins() {
        return fins;
    }

    public Motor motor() {
        return motor;
    }

    public Payload payload() {
        return payload;
    }

    public Recovery recovery() {
        return recovery;
    }

    public Avionics avionics() {
        return avionics;
    }

    public Environment environment() {
        return environment;
    }

    public Mission mission() {
        return mission;
    }

    public double dryMass() {
        return nosecone.mass()
                + body.mass()
                + fins.mass()
                + payload.mass()
                + avionics.mass()
                + recovery.mass();
    }

    public double initialMass() {
        return dryMass() + motor.propellantMass();
    }

    public double length() {
        return nosecone.length() + body.length();
    }

    public double diameter() {
        return body.diameter();
    }

    public double referenceArea() {
        double radius = Math.max(0.001, diameter() / 2.0);
        return Math.PI * radius * radius;
    }

    public double estimatedCg() {
        double total = dryMass();
        if (total <= 0.0) {
            return length() * 0.5;
        }
        double noseCg = nosecone.length() * 0.45;
        double bodyCg = nosecone.length() + body.length() * 0.5;
        double finCg = nosecone.length() + body.length() * 0.84;
        double payloadCg = nosecone.length() + body.length() * 0.22;
        double avionicsCg = nosecone.length() + body.length() * 0.35;
        double recoveryCg = nosecone.length() + body.length() * 0.48;
        return (noseCg * nosecone.mass()
                + bodyCg * body.mass()
                + finCg * fins.mass()
                + payloadCg * payload.mass()
                + avionicsCg * avionics.mass()
                + recoveryCg * recovery.mass()) / total;
    }

    public double estimatedCp() {
        double noseContribution = nosecone.length() * 0.66;
        double finContribution = nosecone.length() + body.length() * 0.88;
        double finAuthority = Math.min(0.85, Math.max(0.25, fins.count() * fins.span() / Math.max(0.01, diameter()) * 0.08));
        return noseContribution * (1.0 - finAuthority) + finContribution * finAuthority;
    }

    public double stabilityMarginCalibers() {
        return (estimatedCp() - estimatedCg()) / Math.max(0.001, diameter());
    }

    public double dragCoefficient() {
        double finPenalty = 0.012 * Math.max(0, fins.count());
        double slenderness = length() / Math.max(0.001, diameter());
        double slendernessAdjustment = slenderness > 15 ? -0.04 : 0.02;
        return Math.max(0.28, 0.52 + finPenalty + slendernessAdjustment);
    }

    public record Nosecone(String shape, double length, double mass) {
        public static Nosecone defaults() {
            return new Nosecone("ogive", 0.25, 0.08);
        }
    }

    public record Body(double length, double diameter, double mass) {
        public static Body defaults() {
            return new Body(1.2, 0.08, 0.35);
        }
    }

    public record Fins(int count, double span, double sweep, double mass) {
        public static Fins defaults() {
            return new Fins(4, 0.12, 0.05, 0.06);
        }
    }

    public record Motor(String thrustCurve, double propellantMass, double impulseOverrideNs) {
        public static Motor defaults() {
            return new Motor("E12", 0.025, 0.0);
        }
    }

    public record Payload(double mass) {
        public static Payload defaults() {
            return new Payload(0.0);
        }
    }

    public record Recovery(double parachuteArea, double deployDelay, double mass) {
        public static Recovery defaults() {
            return new Recovery(0.8, 0.5, 0.05);
        }
    }

    public record Avionics(double mass) {
        public static Avionics defaults() {
            return new Avionics(0.0);
        }
    }

    public record Environment(double windSpeed, double temperatureK) {
        public static Environment defaults() {
            return new Environment(2.0, 288.15);
        }
    }

    public record Mission(double launchAngleDeg, double railLength, double maxTime) {
        public static Mission defaults() {
            return new Mission(3.0, 1.0, 90.0);
        }
    }

    public static final class Builder {
        private final String name;
        private Nosecone nosecone = Nosecone.defaults();
        private Body body = Body.defaults();
        private Fins fins = Fins.defaults();
        private Motor motor = Motor.defaults();
        private Payload payload = Payload.defaults();
        private Recovery recovery = Recovery.defaults();
        private Avionics avionics = Avionics.defaults();
        private Environment environment = Environment.defaults();
        private Mission mission = Mission.defaults();

        private Builder(String name) {
            this.name = name;
        }

        public Builder nosecone(Map<String, String> values) {
            nosecone = new Nosecone(
                    value(values, "shape", nosecone.shape()),
                    Quantity.parse(value(values, "length", ""), nosecone.length()),
                    Quantity.parse(value(values, "mass", ""), nosecone.mass()));
            return this;
        }

        public Builder body(Map<String, String> values) {
            body = new Body(
                    Quantity.parse(value(values, "length", ""), body.length()),
                    Quantity.parse(value(values, "diameter", ""), body.diameter()),
                    Quantity.parse(value(values, "mass", ""), body.mass()));
            return this;
        }

        public Builder fins(Map<String, String> values) {
            fins = new Fins(
                    Quantity.parseInt(value(values, "count", ""), fins.count()),
                    Quantity.parse(value(values, "span", ""), fins.span()),
                    Quantity.parse(value(values, "sweep", ""), fins.sweep()),
                    Quantity.parse(value(values, "mass", ""), fins.mass()));
            return this;
        }

        public Builder motor(Map<String, String> values) {
            String curve = value(values, "thrust_curve", motor.thrustCurve()).toUpperCase(Locale.ROOT);
            motor = new Motor(
                    curve,
                    Quantity.parse(value(values, "propellant_mass", ""), motor.propellantMass()),
                    Quantity.parse(value(values, "impulse", ""), motor.impulseOverrideNs()));
            return this;
        }

        public Builder payload(Map<String, String> values) {
            payload = new Payload(Quantity.parse(value(values, "mass", ""), payload.mass()));
            return this;
        }

        public Builder recovery(Map<String, String> values) {
            recovery = new Recovery(
                    Quantity.parse(value(values, "parachute_area", ""), recovery.parachuteArea()),
                    Quantity.parse(value(values, "deploy_delay", ""), recovery.deployDelay()),
                    Quantity.parse(value(values, "mass", ""), recovery.mass()));
            return this;
        }

        public Builder avionics(Map<String, String> values) {
            avionics = new Avionics(Quantity.parse(value(values, "mass", ""), avionics.mass()));
            return this;
        }

        public Builder environment(Map<String, String> values) {
            environment = new Environment(
                    Quantity.parse(value(values, "wind", ""), environment.windSpeed()),
                    Quantity.parse(value(values, "temperature", ""), environment.temperatureK()));
            return this;
        }

        public Builder mission(Map<String, String> values) {
            mission = new Mission(
                    Quantity.parse(value(values, "launch_angle", ""), mission.launchAngleDeg()),
                    Quantity.parse(value(values, "rail_length", ""), mission.railLength()),
                    Quantity.parse(value(values, "max_time", ""), mission.maxTime()));
            return this;
        }

        public RocketModel build() {
            return new RocketModel(this);
        }

        private static String value(Map<String, String> values, String key, String fallback) {
            return values.getOrDefault(key, fallback);
        }
    }
}
