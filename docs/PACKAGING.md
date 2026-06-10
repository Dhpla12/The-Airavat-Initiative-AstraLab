# Packaging AstraLab

AstraLab uses Maven, `jlink`, and `jpackage` for native desktop packaging.
Installers bundle a custom Java runtime so end users do not need Java, Maven,
Gradle, or command line tools.

## Build First

```bash
mvn clean package
```

The packaging scripts do this automatically.

## macOS

Run on macOS:

```bash
scripts/packaging/package-macos.sh
```

Outputs:

- `target/installer/AstraLab.app`
- `target/installer/AstraLab-1.0.0.dmg`

The app bundle declares document types for:

- `.arvt`
- `.aproj`

The macOS packaging script builds a custom runtime image at:

```text
target/runtime/AstraLabRuntime
```

That runtime includes the required JavaFX modules:

- `javafx.base`
- `javafx.graphics`
- `javafx.controls`
- `javafx.swing`

RichTextFX and its supporting libraries remain on the application classpath because
they are automatic modules and should not be forced into the `jlink` image.

Apple notarization and signing are not configured in v0.1. For distribution outside a
development machine, add a Developer ID certificate and notarization step.

To uninstall, remove `/Applications/AstraLab.app` or run:

```bash
scripts/uninstall-macos.sh
```

The uninstall script prompts before deleting `~/Library/Application Support/AstraLab`.

## Windows

Run in PowerShell on Windows:

```powershell
scripts\packaging\package-windows.ps1
```

Output:

- `target\installer\AstraLab-1.0.0.msi`

MSI generation with `jpackage` requires the WiX Toolset on the packaging machine.
The MSI registers `.arvt` and `.aproj` file associations, adds Start Menu and shortcut
entries, uses a stable upgrade UUID for future v0.1 updates, and appears in Add/Remove
Programs. Uninstall through Windows Settings or Control Panel. User projects are not
stored in the installation directory and are preserved by uninstall.

## Linux

Run on Linux:

```bash
scripts/packaging/package-linux.sh
```

Outputs:

- `target/installer/AstraLab/`
- `target/installer/AstraLab-1.0.0.AppImage` when `appimagetool` is installed
- `target/installer/linux-metadata/` with desktop and MIME metadata

`jpackage` creates an application image directly. AppImage wrapping is performed by
`appimagetool` because `jpackage` does not emit AppImage as a first-class target.

The package declares `.arvt` and `.aproj` file associations. If desktop integration
does not register automatically for your distribution, install the metadata manually:

```bash
mkdir -p ~/.local/share/applications ~/.local/share/mime/packages
cp scripts/packaging/linux/astralab.desktop ~/.local/share/applications/
cp scripts/packaging/linux/astralab-mime.xml ~/.local/share/mime/packages/
update-desktop-database ~/.local/share/applications
update-mime-database ~/.local/share/mime
```

To uninstall a user-local installation, run:

```bash
scripts/uninstall-linux.sh
```

The uninstall script prompts before deleting user data or projects.

## Runtime Bundling

Each script uses the JDK on the packaging machine as the runtime source. Use a Java 21+
JDK with `jlink` and `jpackage`. For JavaFX, Maven must resolve platform-specific
artifacts on the target platform, such as `mac-aarch64` on Apple Silicon.

The macOS script intentionally excludes JavaFX jars from `target/jpackage-input/lib`
after Maven downloads them. JavaFX is supplied by the linked runtime image instead,
and the launcher uses:

```text
--add-modules=javafx.base,javafx.graphics,javafx.controls,javafx.swing
```

This is required because JavaFX modules in a custom runtime are not resolved for a
classpath-launched application unless the launcher explicitly adds them.

## Common Troubleshooting

- If `jpackage` is missing, install a full JDK rather than a JRE.
- If Windows MSI fails, verify WiX is installed and available on `PATH`.
- If Linux AppImage is missing, install `appimagetool`; the app image is still generated.
- If JavaFX native library errors appear, rebuild on the target platform so Maven resolves
  the correct JavaFX platform artifacts.
- If file associations are not active, reinstall the native package or use the manual
  registration notes above for Linux.
