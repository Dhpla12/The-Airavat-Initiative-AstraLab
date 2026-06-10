package com.airavat.astralab.ui;

import com.airavat.astralab.core.ProjectMetadata;
import com.airavat.astralab.core.RocketModel;
import com.airavat.astralab.exports.ExportService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ProjectService {
    private final ExportService exportService = new ExportService();

    public LoadedProject loadBundledExample() throws IOException {
        try (InputStream stream = ProjectService.class.getResourceAsStream("/examples/student_rocket.arvt")) {
            if (stream == null) {
                throw new IOException("Bundled example student_rocket.arvt was not found.");
            }
            return new LoadedProject(
                    "Student Rocket",
                    null,
                    null,
                    null,
                    new String(stream.readAllBytes(), StandardCharsets.UTF_8),
                    false);
        }
    }

    public LoadedProject load(Path file) throws IOException {
        Path normalized = file.toAbsolutePath().normalize();
        String fileName = normalized.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".aproj") && !fileName.endsWith(".arvt")) {
            throw new IOException("AstraLab can open only .arvt source files and .aproj project files.");
        }
        if (fileName.endsWith(".aproj")) {
            if (!Files.isRegularFile(normalized)) {
                throw new IOException("Project file was not found: " + normalized);
            }
            String projectJson = Files.readString(normalized);
            ProjectMetadata metadata = ProjectMetadata.fromJson(projectJson);
            if (metadata.main().isBlank()) {
                throw new IOException("project.aproj does not declare a main ARVT file.");
            }
            Path main = normalized.getParent().resolve(metadata.main()).normalize();
            if (!Files.isRegularFile(main)) {
                throw new IOException("Project source file was not found: " + main);
            }
            return new LoadedProject(metadata.name(), normalized, main, normalized.getParent(), Files.readString(main), true);
        }

        if (!Files.isRegularFile(normalized)) {
            throw new IOException("ARVT source file was not found: " + normalized);
        }
        Path projectFile = normalized.getParent() == null
                ? null
                : normalized.getParent().resolve(ExportService.PROJECT_FILE);
        if (projectFile != null && Files.isRegularFile(projectFile)) {
            ProjectMetadata metadata = ProjectMetadata.fromJson(Files.readString(projectFile));
            Path declaredMain = normalized.getParent().resolve(metadata.main()).normalize();
            if (declaredMain.equals(normalized)) {
                return new LoadedProject(metadata.name(), normalized, normalized, normalized.getParent(), Files.readString(normalized), true);
            }
        }
        return new LoadedProject(stripExtension(normalized.getFileName().toString()), normalized, normalized, null, Files.readString(normalized), false);
    }

    public Path saveProject(Path projectDirectory, String projectName, RocketModel rocket, String source) throws IOException {
        return exportService.prepareProject(projectDirectory, projectName, rocket, source);
    }

    public LoadedProject createProject(Path projectDirectory, String projectName, String source) throws IOException {
        RocketModel rocket = RocketModel.builder(projectName).build();
        saveProject(projectDirectory, projectName, rocket, source);
        Path projectFile = projectDirectory.resolve(ExportService.PROJECT_FILE).toAbsolutePath().normalize();
        Path sourceFile = projectDirectory.resolve(ExportService.MAIN_SOURCE_FILE).toAbsolutePath().normalize();
        return new LoadedProject(projectName, projectFile, sourceFile, projectDirectory.toAbsolutePath().normalize(), source, true);
    }

    public Path saveSource(Path sourcePath, String source) throws IOException {
        Path normalized = sourcePath.toAbsolutePath().normalize();
        if (normalized.getParent() != null) {
            Files.createDirectories(normalized.getParent());
        }
        Files.writeString(normalized, source == null ? "" : source);
        return normalized;
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot <= 0 ? fileName : fileName.substring(0, dot);
    }

    public record LoadedProject(
            String name,
            Path openPath,
            Path sourcePath,
            Path projectDirectory,
            String source,
            boolean projectBacked) {
    }
}
