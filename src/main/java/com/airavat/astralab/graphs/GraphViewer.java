package com.airavat.astralab.graphs;

import com.airavat.astralab.core.FlightSample;
import com.airavat.astralab.core.SimulationResult;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GraphViewer extends BorderPane {
    private final NumberAxis xAxis = new NumberAxis();
    private final NumberAxis yAxis = new NumberAxis();
    private final LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
    private final ComboBox<GraphMetric> metricCombo = new ComboBox<>();
    private final Text emptyText = new Text("Run a simulation to populate graphs.");
    private SimulationResult result;
    private double lastDragX;
    private double lastDragY;

    public GraphViewer() {
        getStyleClass().add("graph-pane");
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setLegendVisible(false);
        chart.setTitle(GraphMetric.ALTITUDE.title());
        xAxis.setLabel(GraphMetric.ALTITUDE.xAxisLabel());
        yAxis.setLabel(GraphMetric.ALTITUDE.yAxisLabel());
        xAxis.setForceZeroInRange(false);
        yAxis.setForceZeroInRange(false);

        metricCombo.getItems().setAll(GraphMetric.values());
        metricCombo.setValue(GraphMetric.ALTITUDE);
        metricCombo.valueProperty().addListener((obs, old, metric) -> plot(metric));

        Button reset = new Button("Reset View");
        reset.getStyleClass().add("toolbar-button");
        reset.setOnAction(event -> plot(metricCombo.getValue()));

        HBox tools = new HBox(8, metricCombo, reset);
        tools.setPadding(new Insets(8));
        tools.getStyleClass().add("top-bar");
        setTop(tools);
        setCenter(emptyText);

        chart.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (result == null) {
                return;
            }
            double factor = Math.exp(-event.getDeltaY() * 0.0015);
            zoomAxis(xAxis, factor);
            zoomAxis(yAxis, factor);
            event.consume();
        });
        chart.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            lastDragX = event.getX();
            lastDragY = event.getY();
        });
        chart.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (result == null || xAxis.isAutoRanging() || yAxis.isAutoRanging()) {
                return;
            }
            double deltaPixels = event.getX() - lastDragX;
            double deltaY = event.getY() - lastDragY;
            panAxis(xAxis, -deltaPixels / Math.max(1.0, chart.getWidth()));
            panAxis(yAxis, deltaY / Math.max(1.0, chart.getHeight()));
            lastDragX = event.getX();
            lastDragY = event.getY();
            event.consume();
        });
    }

    public void setSimulationResult(SimulationResult result) {
        this.result = result;
        plot(metricCombo.getValue());
    }

    public void selectMetric(GraphMetric metric) {
        metricCombo.setValue(metric);
        plot(metric);
    }

    public GraphMetric selectedMetric() {
        return metricCombo.getValue();
    }

    public Path exportCurrentPng(Path file) throws IOException {
        if (result == null) {
            throw new IOException("No simulation result available for graph export.");
        }
        Files.createDirectories(file.getParent());
        WritableImage image = chart.snapshot(new SnapshotParameters(), null);
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file.toFile());
        return file;
    }

    public Path exportCurrentToDirectory(Path directory) throws IOException {
        GraphMetric metric = selectedMetric() == null ? GraphMetric.ALTITUDE : selectedMetric();
        return exportCurrentPng(directory.resolve(metric.fileName()));
    }

    public void exportAll(Path directory) throws IOException {
        if (result == null) {
            throw new IOException("No simulation result available for graph export.");
        }
        Files.createDirectories(directory);
        GraphMetric original = selectedMetric();
        for (GraphMetric metric : GraphMetric.values()) {
            selectMetric(metric);
            exportCurrentPng(directory.resolve(metric.fileName()));
        }
        selectMetric(original);
    }

    private void plot(GraphMetric metric) {
        if (result == null || result.samples().isEmpty()) {
            setCenter(emptyText);
            return;
        }
        GraphMetric selected = metric == null ? GraphMetric.ALTITUDE : metric;
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (FlightSample sample : result.samples()) {
            double x = selected.xValue(sample);
            double y = selected.value(sample);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
            series.getData().add(new XYChart.Data<>(x, y));
        }
        chart.getData().setAll(series);
        chart.setTitle(selected.title());
        xAxis.setLabel(selected.xAxisLabel());
        yAxis.setLabel(selected.yAxisLabel());
        configureAxis(xAxis, Math.min(0.0, minX), maxX);
        configureAxis(yAxis, Math.min(0.0, minY), maxY);
        setCenter(chart);
    }

    private static void configureAxis(NumberAxis axis, double min, double max) {
        double span = Math.max(1.0, max - min);
        axis.setAutoRanging(false);
        axis.setLowerBound(min - span * 0.04);
        axis.setUpperBound(max + span * 0.08);
        axis.setTickUnit(niceTick(span / 8.0));
    }

    private static void zoomAxis(NumberAxis axis, double factor) {
        axis.setAutoRanging(false);
        double lower = axis.getLowerBound();
        double upper = axis.getUpperBound();
        double center = (lower + upper) / 2.0;
        double half = (upper - lower) * factor / 2.0;
        axis.setLowerBound(center - half);
        axis.setUpperBound(center + half);
    }

    private static void panAxis(NumberAxis axis, double fractionOfSpan) {
        double span = axis.getUpperBound() - axis.getLowerBound();
        double shift = fractionOfSpan * span;
        axis.setLowerBound(axis.getLowerBound() + shift);
        axis.setUpperBound(axis.getUpperBound() + shift);
    }

    private static double niceTick(double rough) {
        double exponent = Math.pow(10.0, Math.floor(Math.log10(Math.max(rough, 1.0e-9))));
        double fraction = rough / exponent;
        double nice = fraction <= 1.0 ? 1.0 : fraction <= 2.0 ? 2.0 : fraction <= 5.0 ? 5.0 : 10.0;
        return nice * exponent;
    }
}
