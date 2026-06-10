package com.airavat.astralab.exports;

import com.airavat.astralab.core.FlightSample;
import com.airavat.astralab.core.ProjectMetadata;
import com.airavat.astralab.core.RocketModel;
import com.airavat.astralab.core.SimulationResult;
import com.airavat.astralab.graphs.GraphMetric;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

public final class ExportService {
    public static final String PROJECT_FILE = "project.aproj";
    public static final String MAIN_SOURCE_FILE = "main.arvt";
    public static final String EXPORTS_DIR = "exports";
    public static final String GRAPHS_DIR = "graphs";
    public static final String REPORTS_DIR = "reports";
    public static final String FLIGHT_DATA_CSV = "flight_data.csv";
    public static final String FLIGHT_DATA_JSON = "flight_data.json";
    public static final String REPORT_FILE = "report.txt";

    public Path prepareProject(Path projectDirectory, RocketModel rocket, String source) throws IOException {
        String projectName = rocket == null ? "AstraLab Project" : displayName(rocket.name());
        return prepareProject(projectDirectory, projectName, rocket, source);
    }

    public Path prepareProject(Path projectDirectory, String projectName, RocketModel rocket, String source) throws IOException {
        Files.createDirectories(projectDirectory);
        Files.createDirectories(projectDirectory.resolve(REPORTS_DIR));
        Files.createDirectories(projectDirectory.resolve(GRAPHS_DIR));
        Files.createDirectories(projectDirectory.resolve(EXPORTS_DIR));
        Files.writeString(projectDirectory.resolve(MAIN_SOURCE_FILE), source);
        ProjectMetadata metadata = new ProjectMetadata(projectName, ProjectMetadata.CURRENT_VERSION, MAIN_SOURCE_FILE);
        Files.writeString(projectDirectory.resolve(PROJECT_FILE), metadata.toJson());
        return projectDirectory;
    }

    public Path exportCsv(SimulationResult result, Path directory) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve(FLIGHT_DATA_CSV);
        StringBuilder csv = new StringBuilder("time_s,altitude_m,downrange_m,velocity_mps,acceleration_mps2,mass_kg,mach,dynamic_pressure_pa,x_m,y_m,z_m\n");
        for (FlightSample sample : result.samples()) {
            csv.append(String.format(Locale.US,
                    "%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.5f,%.4f,%.4f,%.4f,%.4f%n",
                    sample.time(),
                    sample.altitude(),
                    sample.downrange(),
                    sample.velocity(),
                    sample.acceleration(),
                    sample.mass(),
                    sample.mach(),
                    sample.dynamicPressure(),
                    sample.x(),
                    sample.y(),
                    sample.z()));
        }
        Files.writeString(file, csv.toString());
        return file;
    }

    public Path exportJson(SimulationResult result, Path directory) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve(FLIGHT_DATA_JSON);
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"rocket\": \"").append(escape(result.rocket().name())).append("\",\n");
        json.append("  \"summary\": {\n");
        json.append(String.format(Locale.US, "    \"apogee_m\": %.4f,%n", result.apogee()));
        json.append(String.format(Locale.US, "    \"max_velocity_mps\": %.4f,%n", result.maxVelocity()));
        json.append(String.format(Locale.US, "    \"max_mach\": %.5f,%n", result.maxMach()));
        json.append(String.format(Locale.US, "    \"max_dynamic_pressure_pa\": %.4f,%n", result.maxDynamicPressure()));
        json.append(String.format(Locale.US, "    \"landing_distance_m\": %.4f,%n", result.landingDistance()));
        json.append(String.format(Locale.US, "    \"stability_margin_calibers\": %.4f%n", result.stabilityMargin()));
        json.append("  },\n");
        json.append("  \"samples\": [\n");
        for (int i = 0; i < result.samples().size(); i++) {
            FlightSample sample = result.samples().get(i);
            json.append(String.format(Locale.US,
                    "    {\"time_s\": %.4f, \"altitude_m\": %.4f, \"downrange_m\": %.4f, \"velocity_mps\": %.4f, \"acceleration_mps2\": %.4f, \"mass_kg\": %.4f, \"mach\": %.5f, \"dynamic_pressure_pa\": %.4f}",
                    sample.time(),
                    sample.altitude(),
                    sample.downrange(),
                    sample.velocity(),
                    sample.acceleration(),
                    sample.mass(),
                    sample.mach(),
                    sample.dynamicPressure()));
            json.append(i == result.samples().size() - 1 ? "\n" : ",\n");
        }
        json.append("  ]\n");
        json.append("}\n");
        Files.writeString(file, json.toString());
        return file;
    }

    public Path exportReport(String reportText, SimulationResult result, Path directory) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve(REPORT_FILE);
        Files.writeString(file, reportText == null || reportText.isBlank() ? "No report generated.\n" : reportText);
        return file;
    }

    public Path exportGraphPng(SimulationResult result, GraphMetric metric, Path file) throws IOException {
        if (result == null || result.samples().isEmpty()) {
            throw new IOException("No simulation result available for graph export.");
        }
        Files.createDirectories(file.getParent());
        GraphMetric selected = metric == null ? GraphMetric.ALTITUDE : metric;
        BufferedImage image = renderGraph(result, selected);
        ImageIO.write(image, "png", file.toFile());
        return file;
    }

    public void exportStandardGraphs(SimulationResult result, Path directory) throws IOException {
        Files.createDirectories(directory);
        for (GraphMetric metric : Arrays.stream(GraphMetric.values()).filter(GraphMetric::standardExport).toList()) {
            exportGraphPng(result, metric, directory.resolve(metric.fileName()));
        }
    }

    public void persistSimulationOutputs(Path projectDirectory, SimulationResult result, String reportText) throws IOException {
        exportCsv(result, projectDirectory.resolve(EXPORTS_DIR));
        exportStandardGraphs(result, projectDirectory.resolve(GRAPHS_DIR));
        exportReport(reportText, result, projectDirectory.resolve(REPORTS_DIR));
    }

    public static String safeName(String value) {
        String cleaned = value == null ? "project" : value.replaceAll("[^A-Za-z0-9._-]+", "_");
        return cleaned.isBlank() ? "project" : cleaned;
    }

    private static String displayName(String value) {
        if (value == null || value.isBlank()) {
            return "AstraLab Project";
        }
        return value.replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static BufferedImage renderGraph(SimulationResult result, GraphMetric metric) {
        int width = 1200;
        int height = 760;
        int left = 92;
        int right = 38;
        int top = 66;
        int bottom = 86;
        int plotWidth = width - left - right;
        int plotHeight = height - top - bottom;

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (FlightSample sample : result.samples()) {
            double x = metric.xValue(sample);
            double y = metric.value(sample);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        minX = Math.min(0.0, minX);
        minY = Math.min(0.0, minY);
        double spanX = Math.max(1.0, maxX - minX);
        double spanY = Math.max(1.0, maxY - minY);
        minX -= spanX * 0.04;
        maxX += spanX * 0.08;
        minY -= spanY * 0.04;
        maxY += spanY * 0.08;
        spanX = maxX - minX;
        spanY = maxY - minY;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(16, 19, 23));
            g.fillRect(0, 0, width, height);
            g.setColor(new Color(28, 34, 41));
            g.fillRect(left, top, plotWidth, plotHeight);
            g.setColor(new Color(56, 65, 76));
            for (int i = 0; i <= 8; i++) {
                int x = left + (int) Math.round(plotWidth * i / 8.0);
                int y = top + (int) Math.round(plotHeight * i / 8.0);
                g.drawLine(x, top, x, top + plotHeight);
                g.drawLine(left, y, left + plotWidth, y);
            }
            g.setColor(new Color(206, 216, 226));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
            g.drawString(metric.title(), left, 36);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
            g.drawString(metric.xAxisLabel(), left + plotWidth / 2 - 42, height - 24);
            g.rotate(-Math.PI / 2.0);
            g.drawString(metric.yAxisLabel(), -top - plotHeight / 2 - 42, 26);
            g.rotate(Math.PI / 2.0);

            g.setStroke(new BasicStroke(3.0f));
            g.setColor(new Color(92, 200, 255));
            Integer previousX = null;
            Integer previousY = null;
            for (FlightSample sample : result.samples()) {
                int x = left + (int) Math.round((metric.xValue(sample) - minX) / spanX * plotWidth);
                int y = top + plotHeight - (int) Math.round((metric.value(sample) - minY) / spanY * plotHeight);
                if (previousX != null) {
                    g.drawLine(previousX, previousY, x, y);
                }
                previousX = x;
                previousY = y;
            }
            g.setColor(new Color(174, 187, 208));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            for (int i = 0; i <= 4; i++) {
                double xValue = minX + spanX * i / 4.0;
                int x = left + (int) Math.round(plotWidth * i / 4.0);
                g.drawString(String.format(Locale.US, "%.1f", xValue), x - 16, top + plotHeight + 22);

                double yValue = minY + spanY * i / 4.0;
                int y = top + plotHeight - (int) Math.round(plotHeight * i / 4.0);
                g.drawString(String.format(Locale.US, "%.1f", yValue), 24, y + 4);
            }
        } finally {
            g.dispose();
        }
        return image;
    }
}
