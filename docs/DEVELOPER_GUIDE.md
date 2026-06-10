# AstraLab Developer Guide

## Goals

AstraLab v0.1 is a professional desktop engineering environment for aerospace
simulation workflows. It intentionally avoids game mechanics, CAD, and 3D visualization.

The core workflow is:

```text
ARVT file -> interpreted model -> physics simulation -> reports, graphs, exports
```

## Package Boundaries

Source code is organized under `com.airavat.astralab`:

- `core`: rocket domain model, quantities, simulation result data
- `physics`: atmosphere, motor curves, 6DOF state, RK4 simulation, Monte Carlo
- `arvt`: lexer, tokenizer, parser, AST, interpreter
- `editor`: ARVT code editor support
- `graphs`: graph metrics and interactive JavaFX graph viewer
- `reports`: text report generation
- `exports`: CSV, JSON, TXT, project structure writing
- `ui`: JavaFX application shell
- `packaging`: reserved for future packaging helpers

Keep these boundaries intact. UI code should not own physics formulas, and the parser
should not write files or touch JavaFX.

## Physics Scope

The v0.1 physics engine is educational and physically defensible:

- 6DOF rigid body state
- Quaternion orientation
- RK4 integration
- Variable motor mass
- Gravity
- Exponential atmosphere
- Wind model
- Launch rail constraint
- Aerodynamic drag
- Dynamic pressure
- Mach number
- Center of Gravity estimate
- Center of Pressure estimate
- Stability margin estimate
- Synthesized motor thrust curves
- Recovery deployment

Out of scope:

- CFD
- Orbital mechanics
- Finite element analysis
- Advanced turbulence solvers
- CAD geometry kernels
- 3D visualization

## ARVT Implementation

ARVT is intentionally interpreted:

- `Lexer` converts source into tokens.
- `Parser` builds `ProgramNode`, `BlockNode`, and `CommandNode`.
- `ArvtInterpreter` builds `RocketModel` and dispatches commands.

Do not introduce bytecode, a VM, or a compiler pipeline for v0.1.

## Adding ARVT Properties

1. Add the property to `RocketModel` or the relevant record.
2. Parse it in `RocketModel.Builder`.
3. Use it from `PhysicsEngine`, `ReportGenerator`, or the UI as needed.
4. Document it in `docs/ARVT_LANGUAGE.md`.

## Running

```bash
mvn clean javafx:run
```

## Building

```bash
mvn clean package
```

## Packaging

See `docs/PACKAGING.md`.

## Manual Smoke Test

1. Start AstraLab.
2. Confirm the splash screen appears.
3. Confirm the editor loads `StudentRocket`.
4. Press `Run`.
5. Confirm console output includes apogee and max velocity.
6. Select graph metrics and test scroll zoom and drag pan.
7. Run `montecarlo 100` from the console.
8. Export CSV, report, and graphs.
