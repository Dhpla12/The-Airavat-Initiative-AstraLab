package com.airavat.astralab.arvt;

import com.airavat.astralab.core.MonteCarloResult;
import com.airavat.astralab.core.RocketModel;
import com.airavat.astralab.core.SimulationResult;

import java.util.List;
import java.util.Optional;

public record ExecutionResult(
        RocketModel rocket,
        Optional<SimulationResult> simulationResult,
        Optional<MonteCarloResult> monteCarloResult,
        String reportText,
        List<String> requestedGraphs,
        List<String> exportRequests,
        List<String> consoleLines,
        boolean clearConsole) {
    public ExecutionResult {
        requestedGraphs = List.copyOf(requestedGraphs);
        exportRequests = List.copyOf(exportRequests);
        consoleLines = List.copyOf(consoleLines);
    }
}
