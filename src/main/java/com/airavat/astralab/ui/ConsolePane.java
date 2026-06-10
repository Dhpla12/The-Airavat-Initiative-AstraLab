package com.airavat.astralab.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public final class ConsolePane extends BorderPane {
    private final TextArea output = new TextArea();
    private final TextField input = new TextField();
    private Consumer<String> commandHandler = command -> {
    };

    public ConsolePane() {
        Label title = new Label("CONSOLE OUTPUT");
        title.getStyleClass().add("panel-title");
        output.getStyleClass().add("console-output");
        output.setEditable(false);
        output.setWrapText(false);
        input.getStyleClass().add("console-input");
        input.setPromptText("Type an ARVT command, e.g. simulate, graph mach, montecarlo 100");
        input.setOnAction(event -> {
            String command = input.getText().trim();
            if (!command.isEmpty()) {
                append("> " + command);
                commandHandler.accept(command);
            }
            input.clear();
        });
        VBox content = new VBox(output, input);
        VBox.setVgrow(output, javafx.scene.layout.Priority.ALWAYS);
        setTop(title);
        setCenter(content);
        BorderPane.setMargin(content, new Insets(0));
    }

    public void setCommandHandler(Consumer<String> commandHandler) {
        this.commandHandler = commandHandler == null ? command -> {
        } : commandHandler;
    }

    public void appendLines(List<String> lines) {
        for (String line : lines) {
            append(line);
        }
    }

    public void append(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (!output.getText().isEmpty() && !output.getText().endsWith("\n")) {
            output.appendText("\n");
        }
        output.appendText(text.endsWith("\n") ? text : text + "\n");
    }

    public void clear() {
        output.clear();
    }
}
