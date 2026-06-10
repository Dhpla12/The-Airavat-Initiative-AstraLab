# AstraLab Quick Start

## Installation

### macOS

Build the installer on macOS:

```bash
scripts/packaging/package-macos.sh
```

Open `target/installer/AstraLab-1.0.0.dmg`, then drag `AstraLab.app` to `/Applications`.

### Windows

Build the MSI from PowerShell on Windows:

```powershell
scripts\packaging\package-windows.ps1
```

Run `target\installer\AstraLab-1.0.0.msi`. The installer adds AstraLab to the Start Menu, creates a shortcut, registers `.arvt` and `.aproj`, and appears in Add/Remove Programs.

### Linux

Build the app image on Linux:

```bash
scripts/packaging/package-linux.sh
```

The app image is written to `target/installer/AstraLab/`. If `appimagetool` is installed, an AppImage is also produced.

## Creating a Project

Use `File > New Project`.

Enter a project name and choose a location. AstraLab creates:

```text
ProjectName/
project.aproj
main.arvt
exports/
graphs/
reports/
```

The project opens automatically after creation.

## Creating an ARVT File

Use `File > New ARVT File`.

AstraLab opens a new unsaved tab named `NewFile.arvt*` with starter contents:

```arvt
rocket NewRocket

simulate
report
```

Use `File > Save` or `File > Save As` to choose a filename and location. Standalone ARVT files are saved as `.arvt`.

## Running Simulations

Click `Run`, or include the command:

```arvt
simulate
```

The console reports parsing, stability, thrust-to-weight ratio, warnings, and the simulation summary.

## Exporting Results

After a simulation, use the toolbar or File menu:

- `CSV` writes `exports/flight_data.csv`.
- `Graph` writes the selected graph PNG.
- `All Graphs` writes all standard graph PNGs.
- `report` in ARVT or the Report button writes `reports/report.txt`.

Simulation output is also updated automatically after each simulation run.

## Project Structure

Projects use a fixed layout:

```text
ProjectName/
project.aproj
main.arvt
exports/
    flight_data.csv
graphs/
    altitude.png
    velocity.png
    acceleration.png
    dynamic_pressure.png
    trajectory.png
reports/
    report.txt
```

Keep project assets inside this folder so Open Recent and operating-system file associations continue to work cleanly.

## Troubleshooting

If a file does not open, verify it ends in `.arvt` or `.aproj`.

If graphs or CSV files are missing, run the simulation again and check that the project folder is writable.

If the app will not launch from a packaged installer, rebuild on the target platform with a full JDK 21 or newer.

If file associations are not active after installation, reinstall the platform package and see `docs/PACKAGING.md` for manual registration notes.
