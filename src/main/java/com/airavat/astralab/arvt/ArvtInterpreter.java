package com.airavat.astralab.arvt;

import com.airavat.astralab.core.MonteCarloResult;
import com.airavat.astralab.core.RocketModel;
import com.airavat.astralab.core.SimulationDiagnostics;
import com.airavat.astralab.core.SimulationResult;
import com.airavat.astralab.physics.MonteCarloAnalyzer;
import com.airavat.astralab.physics.PhysicsEngine;
import com.airavat.astralab.physics.PhysicsValidator;
import com.airavat.astralab.reports.ReportGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ArvtInterpreter {
    private final PhysicsEngine physicsEngine;
    private final MonteCarloAnalyzer monteCarloAnalyzer;
    private final ReportGenerator reportGenerator;

    public ArvtInterpreter() {
        this.physicsEngine = new PhysicsEngine();
        this.monteCarloAnalyzer = new MonteCarloAnalyzer(physicsEngine);
        this.reportGenerator = new ReportGenerator();
    }

    public ProgramNode parse(String source) {
        return new Parser(new Lexer(source).tokenize()).parse();
    }

    public ExecutionResult execute(String source) {
        ProgramNode program = parse(source);
        RocketModel rocket = buildRocket(program);
        SimulationResult simulation = null;
        MonteCarloResult monteCarlo = null;
        String report = "";
        List<String> graphs = new ArrayList<>();
        List<String> exports = new ArrayList<>();
        List<String> console = new ArrayList<>();
        boolean clearConsole = false;
        SimulationDiagnostics diagnostics = PhysicsValidator.analyze(rocket);

        console.add("ARVT parsed: rocket " + rocket.name());
        console.add("Dry mass: %.3f kg, initial mass: %.3f kg".formatted(
                diagnostics.dryMassKg(),
                diagnostics.initialMassKg()));
        console.add("Static stability: %.2f calibers (%s)".formatted(
                diagnostics.staticMarginCalibers(),
                diagnostics.stabilityClassification().displayName()));
        console.add("Initial TWR: %.2f, launch acceleration: %.2f m/s^2".formatted(
                diagnostics.thrustToWeightRatio(),
                diagnostics.launchAccelerationMetersPerSecond2()));
        for (String warning : diagnostics.warnings()) {
            console.add(warning);
        }

        for (CommandNode command : program.commands()) {
            switch (command.name()) {
                case "simulate" -> {
                    simulation = physicsEngine.simulate(rocket);
                    console.add("Simulation complete: apogee %.1f m, max velocity %.1f m/s".formatted(
                            simulation.apogee(), simulation.maxVelocity()));
                    console.addAll(PhysicsValidator.resultWarnings(simulation));
                }
                case "report" -> {
                    if (simulation == null) {
                        simulation = physicsEngine.simulate(rocket);
                        console.add("Simulation complete for report generation.");
                        console.addAll(PhysicsValidator.resultWarnings(simulation));
                    }
                    report = reportGenerator.flightSummary(simulation);
                    console.add(report);
                }
                case "graph" -> {
                    if (simulation == null) {
                        simulation = physicsEngine.simulate(rocket);
                        console.add("Simulation complete for graph generation.");
                        console.addAll(PhysicsValidator.resultWarnings(simulation));
                    }
                    String graphName = command.argText().toLowerCase(Locale.ROOT);
                    if (!graphName.isBlank()) {
                        graphs.add(graphName);
                        console.add("Graph requested: " + graphName);
                    }
                }
                case "export" -> {
                    if (simulation == null) {
                        simulation = physicsEngine.simulate(rocket);
                        console.add("Simulation complete for export generation.");
                        console.addAll(PhysicsValidator.resultWarnings(simulation));
                    }
                    String exportName = command.argText().toLowerCase(Locale.ROOT);
                    if (!exportName.isBlank()) {
                        exports.add(exportName);
                        console.add("Export requested: " + exportName);
                    }
                }
                case "montecarlo" -> {
                    int runs = command.args().isEmpty() ? 100 : parseInt(command.args().get(0), 100);
                    monteCarlo = monteCarloAnalyzer.run(rocket, runs);
                    console.add(reportGenerator.monteCarloSummary(monteCarlo));
                }
                case "help" -> console.add(helpText());
                case "clear" -> clearConsole = true;
                case "save" -> console.add("Save command acknowledged. Use File > Save Project to write project files.");
                case "load" -> console.add("Load command acknowledged. Use File > Open Project to choose an .aproj file.");
                default -> console.add("Warning: unsupported command '" + command.name() + "'.");
            }
        }

        return new ExecutionResult(
                rocket,
                Optional.ofNullable(simulation),
                Optional.ofNullable(monteCarlo),
                report,
                graphs,
                exports,
                console,
                clearConsole);
    }

    private RocketModel buildRocket(ProgramNode program) {
        RocketModel.Builder builder = RocketModel.builder(program.rocketName());
        for (BlockNode block : program.blocks()) {
            Map<String, String> properties = block.properties();
            switch (block.name()) {
                case "nosecone" -> builder.nosecone(properties);
                case "body" -> builder.body(properties);
                case "fins" -> builder.fins(properties);
                case "motor" -> builder.motor(properties);
                case "payload" -> builder.payload(properties);
                case "recovery" -> builder.recovery(properties);
                case "avionics" -> builder.avionics(properties);
                case "environment" -> builder.environment(properties);
                case "mission" -> builder.mission(properties);
                default -> {
                    // Unknown blocks are ignored so ARVT projects can carry forward-compatible metadata.
                }
            }
        }
        return builder.build();
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String helpText() {
        return """
                ARVT commands:
                  simulate
                  report
                  graph altitude|velocity|acceleration|mach|dynamic_pressure
                  export csv|report|graphs
                  montecarlo N
                  save
                  load
                  help
                  clear
                """;
    }
}
