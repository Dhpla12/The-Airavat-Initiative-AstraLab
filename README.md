# AstraLab v0.1

AstraLab is a lightweight aerospace simulation environment for Airavat Aerospace.
It provides a JavaFX desktop IDE for ARVT files, interpreted aerospace system definitions,
educational flight simulation, graphing, report generation, and exports.

This is not a game, a CAD package, or a 3D visual simulator. The product direction is a
professional engineering workflow similar in spirit to MATLAB, OpenRocket, and scientific
computing environments.

## Run From Source

Requirements for developers:

- Java 21 or newer
- Maven 3.9 or newer

```bash
mvn clean javafx:run
```

The application loads the `student_rocket.arvt` example automatically on startup.

## Build

```bash
mvn clean package
```

The application jar is generated at:

```text
target/astralab-1.0.0.jar
```

Runtime dependencies are copied to:

```text
target/lib/
```

## Package Installers

Packaging scripts are in `scripts/packaging/`:

- `package-macos.sh` creates a `.app` bundle and DMG on macOS.
- `package-windows.ps1` creates an MSI on Windows.
- `package-linux.sh` creates a Linux app image and, when `appimagetool` is available, an AppImage.

See `docs/PACKAGING.md` for platform-specific notes.

## Documentation

- `docs/QuickStart.md`
- `docs/ARVT_Guide.md`
- `docs/ARVT_LANGUAGE.md`
- `docs/DEVELOPER_GUIDE.md`
- `docs/PACKAGING.md`

## Example ARVT Files

- `examples/student_rocket.arvt`
- `examples/high_altitude.arvt`
- `examples/sounding_rocket.arvt`
- `examples/two_stage_demo.arvt`
- `examples/templates/basic_rocket.arvt`
- `examples/templates/project_main.arvt`
