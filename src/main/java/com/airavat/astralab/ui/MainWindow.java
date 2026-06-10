package com.airavat.astralab.ui;

import com.airavat.astralab.arvt.ArvtException;
import com.airavat.astralab.arvt.ArvtInterpreter;
import com.airavat.astralab.arvt.ExecutionResult;
import com.airavat.astralab.core.MonteCarloResult;
import com.airavat.astralab.core.RocketModel;
import com.airavat.astralab.core.SimulationDiagnostics;
import com.airavat.astralab.core.SimulationResult;
import com.airavat.astralab.editor.ArvtEditor;
import com.airavat.astralab.exports.ExportService;
import com.airavat.astralab.graphs.GraphMetric;
import com.airavat.astralab.graphs.GraphViewer;
import com.airavat.astralab.physics.PhysicsValidator;
import com.airavat.astralab.reports.ReportGenerator;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

public final class MainWindow extends BorderPane {
    private static final int MAX_RECENT_FILES = 8;
    private static final String RECENT_FILES_KEY = "recentFiles";
    private static final String NEW_ARVT_SOURCE = """
            rocket NewRocket

            simulate
            report
            """;

    private final Stage stage;
    private final ConsolePane console = new ConsolePane();
    private final GraphViewer graphViewer = new GraphViewer();
    private final TabPane editorTabs = new TabPane();
    private final ArvtInterpreter interpreter = new ArvtInterpreter();
    private final ExportService exportService = new ExportService();
    private final ReportGenerator reportGenerator = new ReportGenerator();
    private final ProjectService projectService = new ProjectService();
    private final Preferences preferences = Preferences.userNodeForPackage(MainWindow.class);
    private final Menu recentMenu = new Menu("Open Recent");
    private final Label status = new Label("Ready");

    public MainWindow(Stage stage, ProjectService.LoadedProject initialProject) {
        this.stage = stage;
        getStyleClass().add("main-shell");
        setTop(topChrome());
        setCenter(workspace());
        setBottom(statusBar());

        console.setCommandHandler(command -> {
            EditorDocument document = currentDocument();
            if (document != null) {
                executeSource(document.editor.getText() + "\n" + command);
            }
        });
        editorTabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> updateForSelectedTab());

        refreshRecentMenu();
        addDocument(initialProject, true);
        EditorDocument document = currentDocument();
        if (document != null) {
            executeSource(document.editor.getText());
            document.editor.requestEditorFocus();
        }
    }

    private VBox topChrome() {
        VBox chrome = new VBox(menuBar(), toolbar());
        chrome.getStyleClass().add("top-bar-stack");
        return chrome;
    }

    private MenuBar menuBar() {
        Menu file = new Menu("File");

        MenuItem newArvt = new MenuItem("New ARVT File");
        newArvt.setOnAction(event -> newArvtFile());

        MenuItem newProject = new MenuItem("New Project");
        newProject.setOnAction(event -> newProject());

        MenuItem open = new MenuItem("Open...");
        open.setOnAction(event -> openProject());

        MenuItem save = new MenuItem("Save");
        save.setOnAction(event -> saveCurrentTab());

        MenuItem saveAs = new MenuItem("Save As...");
        saveAs.setOnAction(event -> saveCurrentTabAs());

        MenuItem closeTab = new MenuItem("Close Tab");
        closeTab.setOnAction(event -> closeCurrentTab());

        MenuItem exportCurrentGraph = new MenuItem("Export Current Graph");
        exportCurrentGraph.setOnAction(event -> exportCurrent("current_graph"));

        MenuItem exportAllGraphs = new MenuItem("Export All Graphs");
        exportAllGraphs.setOnAction(event -> exportCurrent("graphs"));

        file.getItems().addAll(
                newArvt,
                newProject,
                new SeparatorMenuItem(),
                open,
                recentMenu,
                new SeparatorMenuItem(),
                save,
                saveAs,
                closeTab,
                new SeparatorMenuItem(),
                exportCurrentGraph,
                exportAllGraphs);

        MenuBar menuBar = new MenuBar(file);
        menuBar.getStyleClass().add("app-menu");
        return menuBar;
    }

    private HBox toolbar() {
        Label brand = new Label("AIRAVAT AEROSPACE  /  AstraLab v0.1 Beta");
        brand.getStyleClass().add("brand");

        Button open = button("Open", false);
        open.setOnAction(event -> openProject());

        Button newFile = button("New File", false);
        newFile.setOnAction(event -> newArvtFile());

        Button save = button("Save", false);
        save.setOnAction(event -> saveCurrentTab());

        Button saveAs = button("Save As", false);
        saveAs.setOnAction(event -> saveCurrentTabAs());

        Button run = button("Run", true);
        run.setOnAction(event -> {
            EditorDocument document = currentDocument();
            if (document != null) {
                executeSource(document.editor.getText());
            }
        });

        Button report = button("Report", false);
        report.setOnAction(event -> {
            EditorDocument document = currentDocument();
            if (document != null) {
                executeSource(document.editor.getText() + "\nsimulate\nreport");
            }
        });

        Button csv = button("CSV", false);
        csv.setOnAction(event -> exportCurrent("csv"));

        Button graph = button("Graph", false);
        graph.setOnAction(event -> exportCurrent("current_graph"));

        Button graphs = button("All Graphs", false);
        graphs.setOnAction(event -> exportCurrent("graphs"));

        Button clear = button("Clear", false);
        clear.setOnAction(event -> console.clear());

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(8, brand, spacer, newFile, open, save, saveAs, run, report, csv, graph, graphs, clear);
        toolbar.getStyleClass().add("top-bar");
        toolbar.setPadding(new Insets(7, 10, 7, 10));
        return toolbar;
    }

    private Button button(String text, boolean primary) {
        Button button = new Button(text);
        button.getStyleClass().add("toolbar-button");
        if (primary) {
            button.getStyleClass().add("primary");
        }
        return button;
    }

    private SplitPane workspace() {
        Label editorTitle = new Label("ARVT EDITOR");
        editorTitle.getStyleClass().add("panel-title");
        BorderPane editorPanel = new BorderPane(editorTabs, editorTitle, null, null, null);

        SplitPane top = new SplitPane(editorPanel, graphViewer);
        top.setDividerPositions(0.58);

        SplitPane vertical = new SplitPane(top, console);
        vertical.setOrientation(Orientation.VERTICAL);
        vertical.setDividerPositions(0.68);
        return vertical;
    }

    private HBox statusBar() {
        status.getStyleClass().add("status-text");
        HBox bar = new HBox(status);
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    private void executeSource(String source) {
        EditorDocument document = currentDocument();
        if (document == null) {
            return;
        }
        try {
            ExecutionResult result = interpreter.execute(source);
            if (result.clearConsole()) {
                console.clear();
            }
            document.currentRocket = result.rocket();
            result.simulationResult().ifPresent(simulation -> {
                document.currentSimulation = simulation;
                document.currentReport = reportGenerator.flightSummary(simulation, projectDisplayName(document));
                graphViewer.setSimulationResult(simulation);
            });
            result.monteCarloResult().ifPresent(monteCarlo -> document.currentMonteCarlo = monteCarlo);
            if (!result.reportText().isBlank() && document.currentSimulation != null) {
                document.currentReport = reportGenerator.flightSummary(document.currentSimulation, projectDisplayName(document));
            }
            console.appendLines(result.consoleLines());
            for (String requestedGraph : result.requestedGraphs()) {
                GraphMetric.fromKey(requestedGraph).ifPresent(graphViewer::selectMetric);
            }
            if (document.currentSimulation != null && result.simulationResult().isPresent() && document.hasOutputLocation()) {
                persistSimulationOutputs(document);
            } else if (document.currentSimulation != null && result.simulationResult().isPresent()) {
                console.append("Outputs will be written after this ARVT file or project is saved.");
            }
            for (String exportRequest : result.exportRequests()) {
                exportCurrent(exportRequest);
            }
            updateStatus(document);
        } catch (ArvtException ex) {
            console.append("Invalid ARVT syntax: " + humanMessage(ex));
            status.setText("Invalid ARVT syntax");
        } catch (RuntimeException ex) {
            console.append("Simulation failed: " + humanMessage(ex));
            status.setText("Simulation failed");
        }
    }

    private void exportCurrent(String request) {
        EditorDocument document = currentDocument();
        if (document == null) {
            return;
        }
        try {
            if (document.currentSimulation == null) {
                executeSource(document.editor.getText() + "\nsimulate");
            }
            if (document.currentSimulation == null || document.currentRocket == null) {
                console.append("Nothing to export yet. Run a simulation first.");
                return;
            }
            Path project = ensureOutputDirectory(document);
            String normalized = request == null ? "" : request.trim().toLowerCase();
            switch (normalized) {
                case "csv" -> {
                    Path file = exportService.exportCsv(document.currentSimulation, project.resolve(ExportService.EXPORTS_DIR));
                    console.append("CSV exported: " + file);
                }
                case "json" -> {
                    Path file = exportService.exportJson(document.currentSimulation, project.resolve(ExportService.EXPORTS_DIR));
                    console.append("JSON exported: " + file);
                }
                case "report" -> {
                    String report = reportText(document);
                    Path file = exportService.exportReport(report, document.currentSimulation, project.resolve(ExportService.REPORTS_DIR));
                    console.append("Report exported: " + file);
                }
                case "current_graph", "graph" -> {
                    graphViewer.setSimulationResult(document.currentSimulation);
                    Path file = graphViewer.exportCurrentToDirectory(project.resolve(ExportService.GRAPHS_DIR));
                    console.append("Current graph exported: " + file);
                }
                case "graphs", "png" -> {
                    graphViewer.setSimulationResult(document.currentSimulation);
                    graphViewer.exportAll(project.resolve(ExportService.GRAPHS_DIR));
                    console.append("Graphs exported: " + project.resolve(ExportService.GRAPHS_DIR));
                }
                default -> console.append("Unsupported export target: " + request);
            }
            status.setText("Export complete: " + normalized);
        } catch (IOException ex) {
            console.append("Export failed: " + humanMessage(ex));
            status.setText("Export failed");
            showError("Export failed", humanMessage(ex));
        }
    }

    private void persistSimulationOutputs(EditorDocument document) {
        try {
            Path project = ensureOutputDirectory(document);
            exportService.persistSimulationOutputs(project, document.currentSimulation, reportText(document));
            console.append("Simulation outputs updated: " + project);
        } catch (IOException ex) {
            console.append("Could not persist simulation outputs: " + humanMessage(ex));
            status.setText("Output persistence failed");
        }
    }

    private Path ensureOutputDirectory(EditorDocument document) throws IOException {
        if (document.projectDirectory != null) {
            Files.createDirectories(document.projectDirectory);
            return document.projectDirectory;
        }
        if (document.sourcePath != null && document.sourcePath.isAbsolute() && document.sourcePath.getParent() != null) {
            Path directory = document.sourcePath.getParent();
            Files.createDirectories(directory);
            return directory;
        }
        throw new IOException("Save this ARVT file or project before exporting outputs.");
    }

    private boolean saveCurrentTab() {
        EditorDocument document = currentDocument();
        return document != null && saveDocument(document);
    }

    private boolean saveCurrentTabAs() {
        EditorDocument document = currentDocument();
        return document != null && saveDocumentAs(document);
    }

    private void newArvtFile() {
        ProjectService.LoadedProject loaded = new ProjectService.LoadedProject(
                "NewFile.arvt",
                null,
                null,
                null,
                NEW_ARVT_SOURCE,
                false);
        EditorDocument document = addDocument(loaded, true);
        document.setDirty(true);
        document.editor.requestEditorFocus();
        status.setText("New ARVT file");
    }

    private void newProject() {
        NewProjectRequest request = promptForNewProject();
        if (request == null) {
            return;
        }
        Path projectDirectory = request.location().resolve(safeFolderName(request.projectName())).toAbsolutePath().normalize();
        try {
            if (shouldConfirmOverwrite(projectDirectory) && !confirmOverwrite(projectDirectory)) {
                return;
            }
            ProjectService.LoadedProject created = projectService.createProject(
                    projectDirectory,
                    request.projectName(),
                    defaultProjectSource(request.projectName()));
            EditorDocument document = addDocument(created, true);
            document.setDirty(false);
            console.append("Project created: " + projectDirectory);
            status.setText("Project created");
            addRecentFile(created.openPath());
            executeSource(document.editor.getText());
        } catch (IOException ex) {
            console.append("New project failed: " + humanMessage(ex));
            status.setText("New project failed");
            showError("New project failed", humanMessage(ex));
        }
    }

    private boolean saveDocument(EditorDocument document) {
        try {
            if (document.projectBacked && document.projectDirectory != null) {
                saveProjectDocument(document, document.projectDirectory);
            } else if (document.sourcePath != null) {
                projectService.saveSource(document.sourcePath, document.editor.getText());
                document.setDirty(false);
                addRecentFile(document.sourcePath);
                console.append("Saved: " + document.sourcePath);
                status.setText("Saved");
            } else {
                return saveDocumentAs(document);
            }
            return true;
        } catch (IOException ex) {
            console.append("Save failed: " + humanMessage(ex));
            status.setText("Save failed");
            showError("Save failed", humanMessage(ex));
            return false;
        }
    }

    private boolean saveDocumentAs(EditorDocument document) {
        if (!document.projectBacked) {
            return saveSourceDocumentAs(document);
        }
        return saveProjectDocumentAs(document);
    }

    private boolean saveSourceDocumentAs(EditorDocument document) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save ARVT File As");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ARVT Files", "*.arvt"));
        chooser.setInitialFileName(document.fileName().endsWith(".arvt") ? document.fileName() : document.fileName() + ".arvt");
        Path initial = document.sourcePath == null ? Path.of(System.getProperty("user.home")) : document.sourcePath.getParent();
        if (initial != null && Files.isDirectory(initial)) {
            chooser.setInitialDirectory(initial.toFile());
        }
        java.io.File selected = chooser.showSaveDialog(stage);
        if (selected == null) {
            return false;
        }
        Path file = ensureArvtExtension(selected.toPath().toAbsolutePath().normalize());
        try {
            if (Files.exists(file) && !confirmFileOverwrite(file)) {
                return false;
            }
            projectService.saveSource(file, document.editor.getText());
            document.openPath = file;
            document.sourcePath = file;
            document.projectDirectory = null;
            document.projectBacked = false;
            document.projectName = file.getFileName().toString();
            document.setDirty(false);
            addRecentFile(file);
            console.append("Saved: " + file);
            status.setText("Saved");
            updateForSelectedTab();
            return true;
        } catch (IOException ex) {
            console.append("Save As failed: " + humanMessage(ex));
            status.setText("Save As failed");
            showError("Save As failed", humanMessage(ex));
            return false;
        }
    }

    private boolean saveProjectDocumentAs(EditorDocument document) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Save AstraLab Project As");
        Path initial = document.projectDirectory != null
                ? document.projectDirectory
                : document.sourcePath == null ? Path.of(System.getProperty("user.home")) : document.sourcePath.getParent();
        if (initial != null && Files.isDirectory(initial)) {
            chooser.setInitialDirectory(initial.toFile());
        }
        java.io.File selected = chooser.showDialog(stage);
        if (selected == null) {
            return false;
        }
        Path directory = selected.toPath().toAbsolutePath().normalize();
        try {
            if (shouldConfirmOverwrite(directory) && !confirmOverwrite(directory)) {
                return false;
            }
            if (directory.getFileName() != null) {
                document.projectName = directory.getFileName().toString();
            }
            saveProjectDocument(document, directory);
            updateForSelectedTab();
            return true;
        } catch (IOException ex) {
            console.append("Save As failed: " + humanMessage(ex));
            status.setText("Save As failed");
            showError("Save As failed", humanMessage(ex));
            return false;
        }
    }

    private void saveProjectDocument(EditorDocument document, Path directory) throws IOException {
        RocketModel rocket = document.currentRocket == null
                ? RocketModel.builder(projectDisplayName(document)).build()
                : document.currentRocket;
        projectService.saveProject(directory, projectDisplayName(document), rocket, document.editor.getText());
        document.projectDirectory = directory;
        document.projectBacked = true;
        document.sourcePath = directory.resolve(ExportService.MAIN_SOURCE_FILE);
        document.openPath = directory.resolve(ExportService.PROJECT_FILE);
        document.setDirty(false);
        addRecentFile(document.openPath);
        console.append("Project saved: " + directory);
        status.setText("Project saved");
    }

    private void openProject() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open ARVT Project");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("AstraLab Project or ARVT", "*.aproj", "*.arvt"),
                new FileChooser.ExtensionFilter("ARVT Files", "*.arvt"),
                new FileChooser.ExtensionFilter("AstraLab Project Files", "*.aproj"));
        java.io.File selected = chooser.showOpenDialog(stage);
        if (selected != null) {
            openPath(selected.toPath());
        }
    }

    public void openExternalPath(Path path) {
        openPath(path);
    }

    private void openPath(Path path) {
        try {
            ProjectService.LoadedProject loaded = projectService.load(path);
            EditorDocument document = addDocument(loaded, true);
            console.append("Loaded: " + displayPath(loaded));
            executeSource(document.editor.getText());
            status.setText("Loaded " + loaded.name());
            addRecentFile(loaded.openPath() == null ? loaded.sourcePath() : loaded.openPath());
        } catch (IOException ex) {
            console.append("Open failed: " + humanMessage(ex));
            status.setText("Open failed");
            showError("Open failed", humanMessage(ex));
        }
    }

    private EditorDocument addDocument(ProjectService.LoadedProject loaded, boolean select) {
        EditorDocument existing = findOpenDocument(loaded);
        if (existing != null) {
            if (select) {
                editorTabs.getSelectionModel().select(existing.tab);
            }
            return existing;
        }

        EditorDocument document = new EditorDocument(loaded.name(), loaded.openPath(), loaded.sourcePath(), loaded.projectDirectory(), loaded.projectBacked());
        document.editor.setText(loaded.source());
        document.editor.textProperty().addListener((obs, oldText, newText) -> document.setDirty(true));
        document.tab.setUserData(document);
        document.tab.setContent(document.editor);
        document.tab.setOnCloseRequest(event -> {
            if (!confirmClose(document)) {
                event.consume();
            }
        });
        document.tab.setOnClosed(event -> {
            if (editorTabs.getTabs().isEmpty()) {
                addDocument(new ProjectService.LoadedProject("Untitled", null, null, null, "", false), true);
            }
        });
        updateTabTitle(document);
        editorTabs.getTabs().add(document.tab);
        if (select) {
            editorTabs.getSelectionModel().select(document.tab);
        }
        if (loaded.openPath() != null || loaded.sourcePath() != null) {
            addRecentFile(loaded.openPath() == null ? loaded.sourcePath() : loaded.openPath());
        }
        return document;
    }

    private EditorDocument findOpenDocument(ProjectService.LoadedProject loaded) {
        for (Tab tab : editorTabs.getTabs()) {
            EditorDocument document = (EditorDocument) tab.getUserData();
            if (samePath(document.openPath, loaded.openPath())
                    || samePath(document.sourcePath, loaded.sourcePath())
                    || samePath(document.projectDirectory, loaded.projectDirectory())) {
                return document;
            }
        }
        return null;
    }

    private void closeCurrentTab() {
        EditorDocument document = currentDocument();
        if (document != null && confirmClose(document)) {
            editorTabs.getTabs().remove(document.tab);
            if (editorTabs.getTabs().isEmpty()) {
                addDocument(new ProjectService.LoadedProject("Untitled", null, null, null, "", false), true);
            }
        }
    }

    private boolean confirmClose(EditorDocument document) {
        if (!document.dirty) {
            return true;
        }
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.YES);
        ButtonType discard = new ButtonType("Discard", ButtonBar.ButtonData.NO);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Save changes to " + document.fileName() + "?", save, discard, cancel);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("This tab has unsaved changes.");
        alert.initOwner(stage);
        Optional<ButtonType> choice = alert.showAndWait();
        if (choice.isEmpty() || choice.get() == cancel) {
            return false;
        }
        if (choice.get() == save) {
            return saveDocument(document);
        }
        return true;
    }

    private boolean shouldConfirmOverwrite(Path directory) throws IOException {
        if (Files.exists(directory.resolve(ExportService.PROJECT_FILE))
                || Files.exists(directory.resolve(ExportService.MAIN_SOURCE_FILE))) {
            return true;
        }
        if (Files.isDirectory(directory)) {
            try (Stream<Path> entries = Files.list(directory)) {
                return entries.findAny().isPresent();
            }
        }
        return false;
    }

    private boolean confirmOverwrite(Path directory) {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "The selected folder already contains files. AstraLab will update project.aproj, main.arvt, exports, graphs, and reports in this folder.",
                ButtonType.CANCEL,
                ButtonType.OK);
        alert.setTitle("Confirm Overwrite");
        alert.setHeaderText("Overwrite existing AstraLab project files?");
        alert.initOwner(stage);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private boolean confirmFileOverwrite(Path file) {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "The selected ARVT file already exists and will be overwritten.",
                ButtonType.CANCEL,
                ButtonType.OK);
        alert.setTitle("Confirm Overwrite");
        alert.setHeaderText("Overwrite " + file.getFileName() + "?");
        alert.initOwner(stage);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private NewProjectRequest promptForNewProject() {
        Dialog<NewProjectRequest> dialog = new Dialog<>();
        dialog.setTitle("New Project");
        dialog.setHeaderText("Create an AstraLab project");
        dialog.initOwner(stage);

        ButtonType create = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, create);

        TextField projectName = new TextField("NewProject");
        TextField location = new TextField(Path.of(System.getProperty("user.home"), "AstraLabProjects").toString());
        Button browse = new Button("Browse...");
        browse.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Choose Project Location");
            Path initial = Path.of(location.getText()).toAbsolutePath().normalize();
            if (Files.isDirectory(initial)) {
                chooser.setInitialDirectory(initial.toFile());
            }
            java.io.File selected = chooser.showDialog(stage);
            if (selected != null) {
                location.setText(selected.toPath().toString());
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        grid.add(new Label("Project Name"), 0, 0);
        grid.add(projectName, 1, 0, 2, 1);
        grid.add(new Label("Location"), 0, 1);
        grid.add(location, 1, 1);
        grid.add(browse, 2, 1);
        GridPane.setHgrow(projectName, Priority.ALWAYS);
        GridPane.setHgrow(location, Priority.ALWAYS);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button != create) {
                return null;
            }
            String name = projectName.getText().trim();
            String locationText = location.getText().trim();
            if (name.isBlank() || locationText.isBlank()) {
                return null;
            }
            return new NewProjectRequest(name, Path.of(locationText).toAbsolutePath().normalize());
        });

        return dialog.showAndWait().orElse(null);
    }

    private void refreshRecentMenu() {
        recentMenu.getItems().clear();
        List<Path> recentFiles = recentFiles();
        if (recentFiles.isEmpty()) {
            MenuItem empty = new MenuItem("No Recent Files");
            empty.setDisable(true);
            recentMenu.getItems().add(empty);
            recentMenu.setDisable(true);
            return;
        }
        recentMenu.setDisable(false);
        for (Path path : recentFiles) {
            MenuItem item = new MenuItem(recentLabel(path));
            item.setOnAction(event -> openPath(path));
            recentMenu.getItems().add(item);
        }
    }

    private List<Path> recentFiles() {
        String raw = preferences.get(RECENT_FILES_KEY, "");
        List<Path> paths = new ArrayList<>();
        for (String line : raw.split("\\R")) {
            if (!line.isBlank()) {
                Path path = Path.of(line).toAbsolutePath().normalize();
                if (Files.exists(path) && !paths.contains(path)) {
                    paths.add(path);
                }
            }
        }
        return paths;
    }

    private void addRecentFile(Path path) {
        if (path == null) {
            return;
        }
        Path normalized = path.toAbsolutePath().normalize();
        List<Path> paths = new ArrayList<>(recentFiles());
        paths.remove(normalized);
        paths.add(0, normalized);
        while (paths.size() > MAX_RECENT_FILES) {
            paths.remove(paths.size() - 1);
        }
        preferences.put(RECENT_FILES_KEY, String.join("\n", paths.stream().map(Path::toString).toList()));
        refreshRecentMenu();
    }

    private void updateForSelectedTab() {
        EditorDocument document = currentDocument();
        if (document == null) {
            graphViewer.setSimulationResult(null);
            status.setText("Ready");
            return;
        }
        graphViewer.setSimulationResult(document.currentSimulation);
        updateStatus(document);
        stage.setTitle("AstraLab v0.1 Beta - " + document.fileName());
    }

    private void updateStatus(EditorDocument document) {
        if (document.currentRocket == null) {
            status.setText("Ready");
            return;
        }
        SimulationDiagnostics diagnostics = PhysicsValidator.analyze(document.currentRocket);
        String simulationText = document.currentSimulation == null
                ? "no simulation"
                : "apogee %.1f m".formatted(document.currentSimulation.apogee());
        String warningPrefix = diagnostics.hasWarnings() ? "WARNING: " : "";
        status.setText("%sRocket %s | %.2f calibers %s | %s".formatted(
                warningPrefix,
                document.currentRocket.name(),
                diagnostics.staticMarginCalibers(),
                diagnostics.stabilityClassification().displayName(),
                simulationText));
    }

    private void updateTabTitle(EditorDocument document) {
        document.tab.setText(document.fileName() + (document.dirty ? "*" : ""));
    }

    private EditorDocument currentDocument() {
        Tab selected = editorTabs.getSelectionModel().getSelectedItem();
        return selected == null ? null : (EditorDocument) selected.getUserData();
    }

    private String reportText(EditorDocument document) {
        if (document.currentReport == null || document.currentReport.isBlank()) {
            document.currentReport = reportGenerator.flightSummary(document.currentSimulation, projectDisplayName(document));
        }
        return document.currentReport;
    }

    private String projectDisplayName(EditorDocument document) {
        if (document.projectName != null && !document.projectName.isBlank()) {
            return document.projectName;
        }
        if (document.currentRocket != null && !document.currentRocket.name().isBlank()) {
            return document.currentRocket.name();
        }
        return "AstraLab Project";
    }

    private String displayPath(ProjectService.LoadedProject loaded) {
        Path path = loaded.openPath() == null ? loaded.sourcePath() : loaded.openPath();
        return path == null ? loaded.name() : path.toString();
    }

    private String recentLabel(Path path) {
        if (path.getFileName() != null && ExportService.PROJECT_FILE.equals(path.getFileName().toString())) {
            try {
                String projectName = com.airavat.astralab.core.ProjectMetadata.fromJson(Files.readString(path)).name();
                return projectName + " - " + path.getParent();
            } catch (IOException ignored) {
                // Fall back to the raw file label below.
            }
        }
        Path fileName = path.getFileName();
        Path parent = path.getParent();
        if (parent == null) {
            return path.toString();
        }
        return fileName + " - " + parent;
    }

    private static Path ensureArvtExtension(Path file) {
        String name = file.getFileName().toString();
        if (name.toLowerCase().endsWith(".arvt")) {
            return file;
        }
        return file.resolveSibling(name + ".arvt");
    }

    private static String defaultProjectSource(String projectName) {
        return """
                rocket %s

                simulate
                report
                """.formatted(safeRocketName(projectName));
    }

    private static String safeRocketName(String value) {
        String cleaned = value == null ? "NewRocket" : value.replaceAll("[^A-Za-z0-9_]+", "");
        if (cleaned.isBlank()) {
            return "NewRocket";
        }
        if (!Character.isJavaIdentifierStart(cleaned.charAt(0))) {
            return "Rocket" + cleaned;
        }
        return cleaned;
    }

    private static String safeFolderName(String value) {
        String cleaned = value == null ? "NewProject" : value.replaceAll("[^A-Za-z0-9._-]+", "_");
        return cleaned.isBlank() ? "NewProject" : cleaned;
    }

    private static boolean samePath(Path first, Path second) {
        if (first == null || second == null) {
            return false;
        }
        return first.toAbsolutePath().normalize().equals(second.toAbsolutePath().normalize());
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    private static String humanMessage(Throwable ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }

    private record NewProjectRequest(String projectName, Path location) {
    }

    private final class EditorDocument {
        private final ArvtEditor editor = new ArvtEditor();
        private final Tab tab = new Tab();
        private Path openPath;
        private Path sourcePath;
        private Path projectDirectory;
        private String projectName;
        private boolean projectBacked;
        private boolean dirty;
        private RocketModel currentRocket;
        private SimulationResult currentSimulation;
        private MonteCarloResult currentMonteCarlo;
        private String currentReport = "";

        private EditorDocument(String projectName, Path openPath, Path sourcePath, Path projectDirectory, boolean projectBacked) {
            this.projectName = projectName;
            this.openPath = openPath == null ? null : openPath.toAbsolutePath().normalize();
            this.sourcePath = sourcePath == null ? null : sourcePath.toAbsolutePath().normalize();
            this.projectDirectory = projectDirectory == null ? null : projectDirectory.toAbsolutePath().normalize();
            this.projectBacked = projectBacked;
        }

        private void setDirty(boolean dirty) {
            this.dirty = dirty;
            updateTabTitle(this);
        }

        private String fileName() {
            if (sourcePath != null && sourcePath.getFileName() != null) {
                return sourcePath.getFileName().toString();
            }
            if (projectName != null && !projectName.isBlank()) {
                return projectName;
            }
            return "Untitled.arvt";
        }

        private boolean hasOutputLocation() {
            return projectDirectory != null || (sourcePath != null && sourcePath.getParent() != null);
        }
    }
}
