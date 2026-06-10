package com.airavat.astralab.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ProjectMetadata(String name, String version, String main) {
    private static final Pattern STRING_FIELD = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
    public static final String CURRENT_VERSION = "1.0";
    public static final String DEFAULT_MAIN = "main.arvt";

    public ProjectMetadata {
        name = name == null || name.isBlank() ? "AstraLab Project" : name.trim();
        version = version == null || version.isBlank() ? CURRENT_VERSION : version.trim();
        main = main == null || main.isBlank() ? DEFAULT_MAIN : main.trim();
    }

    public String toJson() {
        return """
                {
                  "name": "%s",
                  "version": "%s",
                  "main": "%s"
                }
                """.formatted(escape(name), escape(version), escape(main));
    }

    public static ProjectMetadata fromJson(String json) {
        String name = "AstraLab Project";
        String version = CURRENT_VERSION;
        String main = DEFAULT_MAIN;
        Matcher matcher = STRING_FIELD.matcher(json == null ? "" : json);
        while (matcher.find()) {
            switch (matcher.group(1)) {
                case "name" -> name = unescape(matcher.group(2));
                case "version" -> version = unescape(matcher.group(2));
                case "main" -> main = unescape(matcher.group(2));
                default -> {
                    // Forward-compatible metadata fields are intentionally ignored.
                }
            }
        }
        return new ProjectMetadata(name, version, main);
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String unescape(String value) {
        return value == null ? "" : value.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
