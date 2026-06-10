package com.airavat.astralab.graphs;

import com.airavat.astralab.core.FlightSample;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.function.ToDoubleFunction;

public enum GraphMetric {
    ALTITUDE("altitude", "Altitude vs Time", "Time (s)", "Altitude (m)", FlightSample::time, FlightSample::altitude, true),
    VELOCITY("velocity", "Velocity vs Time", "Time (s)", "Velocity (m/s)", FlightSample::time, FlightSample::velocity, true),
    ACCELERATION("acceleration", "Acceleration vs Time", "Time (s)", "Acceleration (m/s^2)", FlightSample::time, FlightSample::acceleration, true),
    DYNAMIC_PRESSURE("dynamic_pressure", "Dynamic Pressure vs Time", "Time (s)", "Dynamic Pressure (kPa)", FlightSample::time, sample -> sample.dynamicPressure() / 1000.0, true),
    TRAJECTORY("trajectory", "Trajectory", "Downrange (m)", "Altitude (m)", FlightSample::downrange, FlightSample::altitude, true),
    MASS("mass", "Mass vs Time", "Time (s)", "Mass (kg)", FlightSample::time, FlightSample::mass, false),
    MACH("mach", "Mach vs Time", "Time (s)", "Mach", FlightSample::time, FlightSample::mach, false);

    private final String key;
    private final String title;
    private final String xAxisLabel;
    private final String yAxisLabel;
    private final ToDoubleFunction<FlightSample> xExtractor;
    private final ToDoubleFunction<FlightSample> extractor;
    private final boolean standardExport;

    GraphMetric(
            String key,
            String title,
            String xAxisLabel,
            String yAxisLabel,
            ToDoubleFunction<FlightSample> xExtractor,
            ToDoubleFunction<FlightSample> extractor,
            boolean standardExport) {
        this.key = key;
        this.title = title;
        this.xAxisLabel = xAxisLabel;
        this.yAxisLabel = yAxisLabel;
        this.xExtractor = xExtractor;
        this.extractor = extractor;
        this.standardExport = standardExport;
    }

    public String key() {
        return key;
    }

    public String title() {
        return title;
    }

    public String xAxisLabel() {
        return xAxisLabel;
    }

    public String yAxisLabel() {
        return yAxisLabel;
    }

    public double xValue(FlightSample sample) {
        return xExtractor.applyAsDouble(sample);
    }

    public double value(FlightSample sample) {
        return extractor.applyAsDouble(sample);
    }

    public boolean standardExport() {
        return standardExport;
    }

    public String fileName() {
        return key + ".png";
    }

    @Override
    public String toString() {
        return title;
    }

    public static Optional<GraphMetric> fromKey(String key) {
        String normalized = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(metric -> metric.key.equals(normalized)).findFirst();
    }
}
