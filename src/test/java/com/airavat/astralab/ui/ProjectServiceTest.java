package com.airavat.astralab.ui;

import com.airavat.astralab.core.RocketModel;
import com.airavat.astralab.exports.ExportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectServiceTest {
    private static final String SOURCE = """
            rocket BetaRocket

            body {
                length 1.0m
                diameter 0.08m
                mass 0.35kg
            }

            simulate
            """;

    @TempDir
    Path tempDir;

    @Test
    void saveProjectUsesStandardLayoutAndCanBeLoaded() throws Exception {
        ProjectService service = new ProjectService();
        RocketModel rocket = RocketModel.builder("BetaRocket").build();
        Path project = tempDir.resolve("BetaRocket");

        service.saveProject(project, "Beta Rocket", rocket, SOURCE);

        assertTrue(Files.exists(project.resolve(ExportService.PROJECT_FILE)));
        assertTrue(Files.exists(project.resolve(ExportService.MAIN_SOURCE_FILE)));
        assertTrue(Files.isDirectory(project.resolve(ExportService.EXPORTS_DIR)));
        assertTrue(Files.isDirectory(project.resolve(ExportService.GRAPHS_DIR)));
        assertTrue(Files.isDirectory(project.resolve(ExportService.REPORTS_DIR)));

        ProjectService.LoadedProject loaded = service.load(project.resolve(ExportService.PROJECT_FILE));
        assertEquals("Beta Rocket", loaded.name());
        assertEquals(project, loaded.projectDirectory());
        assertEquals(project.resolve(ExportService.MAIN_SOURCE_FILE), loaded.sourcePath());
        assertEquals(SOURCE, loaded.source());
    }

    @Test
    void saveProjectOverwritesExistingMainSource() throws Exception {
        ProjectService service = new ProjectService();
        RocketModel rocket = RocketModel.builder("BetaRocket").build();
        Path project = tempDir.resolve("BetaRocket");

        service.saveProject(project, "Beta Rocket", rocket, SOURCE);
        service.saveProject(project, "Beta Rocket", rocket, SOURCE.replace("BetaRocket", "UpdatedRocket"));

        String savedSource = Files.readString(project.resolve(ExportService.MAIN_SOURCE_FILE));
        assertTrue(savedSource.contains("rocket UpdatedRocket"));
    }

    @Test
    void createProjectCreatesStandardFolderAndLoadedProjectMetadata() throws Exception {
        ProjectService service = new ProjectService();
        Path project = tempDir.resolve("StudentRocket");

        ProjectService.LoadedProject created = service.createProject(project, "StudentRocket", SOURCE);

        assertEquals("StudentRocket", created.name());
        assertEquals(project.resolve(ExportService.PROJECT_FILE), created.openPath());
        assertEquals(project.resolve(ExportService.MAIN_SOURCE_FILE), created.sourcePath());
        assertEquals(project, created.projectDirectory());
        assertTrue(created.projectBacked());
        assertTrue(Files.isDirectory(project.resolve(ExportService.EXPORTS_DIR)));
        assertTrue(Files.isDirectory(project.resolve(ExportService.GRAPHS_DIR)));
        assertTrue(Files.isDirectory(project.resolve(ExportService.REPORTS_DIR)));
    }

    @Test
    void saveSourceUpdatesStandaloneArvtFileWithoutCreatingProjectFiles() throws Exception {
        ProjectService service = new ProjectService();
        Path source = tempDir.resolve("ExampleRocket.arvt");

        Path saved = service.saveSource(source, SOURCE);

        assertEquals(source, saved);
        assertEquals(SOURCE, Files.readString(source));
        assertTrue(Files.notExists(tempDir.resolve(ExportService.PROJECT_FILE)));
        ProjectService.LoadedProject loaded = service.load(source);
        assertEquals("ExampleRocket", loaded.name());
        assertEquals(source, loaded.sourcePath());
        assertTrue(!loaded.projectBacked());
    }
}
