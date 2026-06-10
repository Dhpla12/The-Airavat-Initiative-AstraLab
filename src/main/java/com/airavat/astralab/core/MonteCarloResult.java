package com.airavat.astralab.core;

import java.util.DoubleSummaryStatistics;
import java.util.List;

public final class MonteCarloResult {
    private final List<SimulationResult> runs;
    private final Summary apogee;
    private final Summary landingDistance;

    public MonteCarloResult(List<SimulationResult> runs) {
        this.runs = List.copyOf(runs);
        this.apogee = summarize(runs.stream().mapToDouble(SimulationResult::apogee).toArray());
        this.landingDistance = summarize(runs.stream().mapToDouble(SimulationResult::landingDistance).toArray());
    }

    public List<SimulationResult> runs() {
        return runs;
    }

    public Summary apogee() {
        return apogee;
    }

    public Summary landingDistance() {
        return landingDistance;
    }

    private static Summary summarize(double[] values) {
        DoubleSummaryStatistics stats = new DoubleSummaryStatistics();
        for (double value : values) {
            stats.accept(value);
        }
        double mean = stats.getAverage();
        double variance = 0.0;
        for (double value : values) {
            variance += Math.pow(value - mean, 2.0);
        }
        double stdDev = values.length > 1 ? Math.sqrt(variance / (values.length - 1)) : 0.0;
        return new Summary(stats.getCount(), stats.getMin(), stats.getMax(), mean, stdDev);
    }

    public record Summary(long count, double min, double max, double mean, double stdDev) {
    }
}
