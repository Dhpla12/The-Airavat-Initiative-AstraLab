package com.airavat.astralab.exports;

import com.airavat.astralab.arvt.ArvtInterpreter;
import com.airavat.astralab.core.SimulationResult;
import com.airavat.astralab.graphs.GraphMetric;
import com.airavat.astralab.reports.ReportGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportServiceTest {
    private static final String SOURCE = """
            rocket ExportRocket

            body {
                length 1.2m
                diameter 0.08m
                mass 0.35kg
            }

            motor {
                thrust_curve E12
            }

            simulate
            """;

    @TempDir
    Path tempDir;

    @Test
    void csvExportUsesFixedEditableOutputName() throws Exception {
        ExportService service = new ExportService();
        SimulationResult result = simulate();

        Path file = service.exportCsv(result, tempDir.resolve(ExportService.EXPORTS_DIR));

        assertEquals(tempDir.resolve(ExportService.EXPORTS_DIR).resolve(ExportService.FLIGHT_DATA_CSV), file);
        String csv = Files.readString(file);
        assertTrue(csv.startsWith("time_s,altitude_m,downrange_m"));
        assertTrue(csv.lines().count() > 5);
    }

    @Test
    void reportExportWritesReportTxtWithRequiredSections() throws Exception {
        ExportService service = new ExportService();
        SimulationResult result = simulate();
        String report = new ReportGenerator().flightSummary(result, "Export Project");

        Path file = service.exportReport(report, result, tempDir.resolve(ExportService.REPORTS_DIR));

        assertEquals(tempDir.resolve(ExportService.REPORTS_DIR).resolve(ExportService.REPORT_FILE), file);
        String saved = Files.readString(file);
        assertTrue(saved.contains("Project Name: Export Project"));
        assertTrue(saved.contains("Rocket Name: ExportRocket"));
        assertTrue(saved.contains("Static Margin:"));
        assertTrue(saved.contains("Simulation Warnings:"));
    }

    @Test
    void graphExportWritesStandardPngFiles() throws Exception {
        ExportService service = new ExportService();
        SimulationResult result = simulate();

        service.exportStandardGraphs(result, tempDir.resolve(ExportService.GRAPHS_DIR));

        for (GraphMetric metric : GraphMetric.values()) {
            if (metric.standardExport()) {
                Path file = tempDir.resolve(ExportService.GRAPHS_DIR).resolve(metric.fileName());
                assertTrue(Files.size(file) > 0, metric.fileName());
            }
        }
    }

    private static SimulationResult simulate() {
        return new ArvtInterpreter().execute(SOURCE).simulationResult().orElseThrow();
    }
}
