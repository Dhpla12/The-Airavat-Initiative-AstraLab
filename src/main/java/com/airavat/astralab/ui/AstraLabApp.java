package com.airavat.astralab.ui;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.desktop.OpenFilesEvent;
import java.io.File;
import java.lang.reflect.Proxy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AstraLabApp extends Application {
    private static final long STARTUP_OPEN_FILE_WAIT_MILLIS = 500;
    private static final List<Path> PENDING_OPEN_FILES = new ArrayList<>();
    private static AstraLabApp currentApp;
    private static boolean openFileHandlersRegistered;

    static {
        registerOpenFileHandlers();
    }

    private final ProjectService projectService = new ProjectService();
    private MainWindow mainWindow;

    @Override
    public void init() {
        currentApp = this;
    }

    @Override
    public void start(Stage stage) throws Exception {
        logStartup("raw args: " + getParameters().getRaw());
        stage.setTitle("AstraLab v0.1");
        Scene splash = splashScene();
        addStylesheet(splash);
        stage.setScene(splash);
        stage.setWidth(1180);
        stage.setHeight(760);
        stage.show();

        ProjectService.LoadedProject initialProject = loadInitialProject();
        PauseTransition delay = new PauseTransition(Duration.millis(1100));
        delay.setOnFinished(event -> {
            mainWindow = new MainWindow(stage, initialProject);
            Scene scene = new Scene(mainWindow, 1280, 820);
            addStylesheet(scene);
            stage.setScene(scene);
            stage.setMinWidth(980);
            stage.setMinHeight(680);
            stage.centerOnScreen();
            openPendingFiles(initialProject);
        });
        delay.play();
    }

    private ProjectService.LoadedProject loadInitialProject() throws Exception {
        Optional<Path> launchFile = startupFileArgument();
        if (launchFile.isPresent()) {
            logStartup("loading startup argument: " + launchFile.get());
            return projectService.load(launchFile.get());
        }
        Optional<Path> queuedOpenFile = takeQueuedOpenFile(STARTUP_OPEN_FILE_WAIT_MILLIS);
        if (queuedOpenFile.isPresent()) {
            logStartup("loading queued document: " + queuedOpenFile.get());
            return projectService.load(queuedOpenFile.get());
        }
        logStartup("loading bundled example");
        return projectService.loadBundledExample();
    }

    private Optional<Path> startupFileArgument() {
        for (String arg : getParameters().getRaw()) {
            if (isSupportedDocumentPath(arg)) {
                return Optional.of(Path.of(arg).toAbsolutePath().normalize());
            }
        }
        return Optional.empty();
    }

    static void registerOpenFileHandlers() {
        if (openFileHandlersRegistered) {
            return;
        }
        openFileHandlersRegistered = true;
        registerDesktopOpenFileHandler();
        registerLegacyMacOpenFileHandler();
    }

    private static void registerDesktopOpenFileHandler() {
        try {
            initializeAwtEventBridge();
            if (!Desktop.isDesktopSupported()) {
                logStartup("desktop open-file handler unavailable: desktop unsupported");
                return;
            }
            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.APP_OPEN_FILE)) {
                logStartup("desktop open-file handler unavailable: action unsupported");
                return;
            }
            desktop.setOpenFileHandler(AstraLabApp::handleDesktopOpenFilesEvent);
            logStartup("desktop open-file handler registered");
        } catch (RuntimeException ignored) {
            logStartup("desktop open-file handler registration failed: " + ignored.getClass().getSimpleName());
            // Keep startup portable on platforms without desktop file-open events.
        }
    }

    private static void initializeAwtEventBridge() {
        Toolkit.getDefaultToolkit();
    }

    private static void handleDesktopOpenFilesEvent(OpenFilesEvent event) {
        logStartup("desktop open-file event: " + event.getFiles());
        for (File file : event.getFiles()) {
            queueExternalOpen(file.toPath());
        }
    }

    private static void registerLegacyMacOpenFileHandler() {
        try {
            Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
            Class<?> handlerClass = Class.forName("com.apple.eawt.OpenFilesHandler");
            Object application = applicationClass.getMethod("getApplication").invoke(null);
            Object handler = Proxy.newProxyInstance(
                    handlerClass.getClassLoader(),
                    new Class<?>[]{handlerClass},
                    (proxy, method, args) -> {
                        if ("openFiles".equals(method.getName()) && args != null && args.length == 1) {
                            try {
                                handleMacOpenFilesEvent(args[0]);
                            } catch (ReflectiveOperationException ignored) {
                                // Ignore malformed platform events and keep the app running.
                            }
                        }
                        return null;
                    });
            applicationClass.getMethod("setOpenFileHandler", handlerClass).invoke(application, handler);
            logStartup("legacy mac open-file handler registered");
        } catch (ReflectiveOperationException | LinkageError ignored) {
            logStartup("legacy mac open-file handler unavailable: " + ignored.getClass().getSimpleName());
            // Non-macOS platforms pass associated files as launcher arguments.
        }
    }

    private static void handleMacOpenFilesEvent(Object event) throws ReflectiveOperationException {
        Object files = event.getClass().getMethod("getFiles").invoke(event);
        if (!(files instanceof List<?> fileList)) {
            return;
        }
        for (Object file : fileList) {
            if (file instanceof File selectedFile) {
                logStartup("legacy mac open-file event: " + selectedFile);
                queueExternalOpen(selectedFile.toPath());
            }
        }
    }

    private static void queueExternalOpen(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (!isSupportedDocumentPath(normalized.toString())) {
            return;
        }
        synchronized (PENDING_OPEN_FILES) {
            if (!PENDING_OPEN_FILES.contains(normalized)) {
                PENDING_OPEN_FILES.add(normalized);
                logStartup("queued external document: " + normalized);
            }
        }
        try {
            Platform.runLater(AstraLabApp::openPendingFilesOnActiveWindow);
        } catch (IllegalStateException ignored) {
            // JavaFX has not started yet; startup will drain the pending file list.
        }
    }

    private static Optional<Path> takeQueuedOpenFile(long waitMillis) {
        long deadline = System.nanoTime() + waitMillis * 1_000_000L;
        do {
            synchronized (PENDING_OPEN_FILES) {
                if (!PENDING_OPEN_FILES.isEmpty()) {
                    return Optional.of(PENDING_OPEN_FILES.remove(0));
                }
            }
            if (waitMillis <= 0) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        } while (System.nanoTime() < deadline);
        return Optional.empty();
    }

    private void openPendingFiles(ProjectService.LoadedProject initialProject) {
        List<Path> paths;
        synchronized (PENDING_OPEN_FILES) {
            paths = new ArrayList<>(PENDING_OPEN_FILES);
            PENDING_OPEN_FILES.clear();
        }
        Path initialPath = initialProject.openPath() == null ? initialProject.sourcePath() : initialProject.openPath();
        for (Path path : paths) {
            if (initialPath == null || !initialPath.toAbsolutePath().normalize().equals(path)) {
                logStartup("opening pending document on startup window: " + path);
                mainWindow.openExternalPath(path);
            }
        }
    }

    private static void openPendingFilesOnActiveWindow() {
        AstraLabApp app = currentApp;
        if (app == null || app.mainWindow == null) {
            return;
        }
        List<Path> paths;
        synchronized (PENDING_OPEN_FILES) {
            paths = new ArrayList<>(PENDING_OPEN_FILES);
            PENDING_OPEN_FILES.clear();
        }
        for (Path path : paths) {
            logStartup("opening pending document on active window: " + path);
            app.mainWindow.openExternalPath(path);
        }
    }

    private static void logStartup(String message) {
        try {
            Path log = Path.of(System.getProperty("user.home"), "Library", "Application Support", "AstraLab", "startup.log");
            Files.createDirectories(log.getParent());
            Files.writeString(
                    log,
                    java.time.Instant.now() + " " + message + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Logging must never affect startup.
        }
    }

    private static boolean isSupportedDocumentPath(String value) {
        String lower = value == null ? "" : value.toLowerCase();
        return lower.endsWith(".arvt") || lower.endsWith(".aproj");
    }

    private Scene splashScene() {
        Label company = new Label("AIRAVAT AEROSPACE");
        company.getStyleClass().add("splash-title");
        Label product = new Label("AstraLab v0.1");
        product.getStyleClass().add("splash-product");
        Label subtitle = new Label("Aerospace Simulation Environment");
        subtitle.getStyleClass().add("splash-subtitle");
        VBox root = new VBox(10, company, product, subtitle);
        root.getStyleClass().add("splash");
        root.setAlignment(Pos.CENTER);
        return new Scene(root, 820, 420);
    }

    private void addStylesheet(Scene scene) {
        String css = AstraLabApp.class.getResource("/com/airavat/astralab/astralab.css").toExternalForm();
        scene.getStylesheets().add(css);
    }

    public static void main(String[] args) {
        registerOpenFileHandlers();
        launch(args);
    }
}
