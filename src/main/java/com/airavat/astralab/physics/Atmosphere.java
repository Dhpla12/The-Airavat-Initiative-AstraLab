package com.airavat.astralab.physics;

public final class Atmosphere {
    private static final double SEA_LEVEL_TEMPERATURE = 288.15;
    private static final double SEA_LEVEL_PRESSURE = 101_325.0;
    private static final double TROPOPAUSE_ALTITUDE = 11_000.0;
    private static final double LAPSE_RATE = 0.0065;
    private static final double GAS_CONSTANT_AIR = 287.05;
    private static final double GAMMA_AIR = 1.4;
    private static final double G0 = 9.80665;

    private Atmosphere() {
    }

    public static double temperature(double altitudeMeters) {
        return temperature(altitudeMeters, SEA_LEVEL_TEMPERATURE);
    }

    public static double temperature(double altitudeMeters, double groundTemperatureK) {
        double h = Math.max(0.0, altitudeMeters);
        double ground = groundTemperatureK > 0.0 ? groundTemperatureK : SEA_LEVEL_TEMPERATURE;
        if (h <= TROPOPAUSE_ALTITUDE) {
            return Math.max(216.65, ground - LAPSE_RATE * h);
        }
        return 216.65;
    }

    public static double pressure(double altitudeMeters) {
        double h = Math.max(0.0, altitudeMeters);
        double exponent = G0 / (GAS_CONSTANT_AIR * LAPSE_RATE);
        if (h <= TROPOPAUSE_ALTITUDE) {
            double temperatureRatio = temperature(h) / SEA_LEVEL_TEMPERATURE;
            return SEA_LEVEL_PRESSURE * Math.pow(temperatureRatio, exponent);
        }
        double tropopausePressure = pressure(TROPOPAUSE_ALTITUDE);
        return tropopausePressure * Math.exp(-G0 * (h - TROPOPAUSE_ALTITUDE) / (GAS_CONSTANT_AIR * 216.65));
    }

    public static double density(double altitudeMeters) {
        return density(altitudeMeters, SEA_LEVEL_TEMPERATURE);
    }

    public static double density(double altitudeMeters, double groundTemperatureK) {
        double temperature = temperature(altitudeMeters, groundTemperatureK);
        return pressure(altitudeMeters) / (GAS_CONSTANT_AIR * temperature);
    }

    public static double speedOfSound(double altitudeMeters, double groundTemperatureK) {
        return Math.sqrt(GAMMA_AIR * GAS_CONSTANT_AIR * temperature(altitudeMeters, groundTemperatureK));
    }
}
