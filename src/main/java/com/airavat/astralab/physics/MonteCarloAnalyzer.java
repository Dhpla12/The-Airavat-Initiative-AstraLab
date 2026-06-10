package com.airavat.astralab.physics;

import com.airavat.astralab.core.MonteCarloResult;
import com.airavat.astralab.core.RocketModel;
import com.airavat.astralab.core.SimulationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class MonteCarloAnalyzer {
    private final PhysicsEngine physicsEngine;

    public MonteCarloAnalyzer(PhysicsEngine physicsEngine) {
        this.physicsEngine = physicsEngine;
    }

    public MonteCarloResult run(RocketModel model, int runs) {
        int boundedRuns = Math.max(1, Math.min(10_000, runs));
        Random random = new Random(0xA57A1AB);
        List<SimulationResult> results = new ArrayList<>(boundedRuns);
        for (int i = 0; i < boundedRuns; i++) {
            SimulationConfig config = new SimulationConfig(
                    clamp(1.0 + random.nextGaussian() * 0.08, 0.75, 1.35),
                    clamp(1.0 + random.nextGaussian() * 0.05, 0.80, 1.20),
                    clamp(1.0 + random.nextGaussian() * 0.025, 0.90, 1.12),
                    clamp(1.0 + random.nextGaussian() * 0.20, 0.10, 2.50),
                    random.nextGaussian() * 1.5,
                    false);
            results.add(physicsEngine.simulate(model, config));
        }
        return new MonteCarloResult(results);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
