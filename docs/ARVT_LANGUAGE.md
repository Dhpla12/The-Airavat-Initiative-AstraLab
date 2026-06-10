# ARVT Language Reference

ARVT is an interpreted domain-specific language for defining aerospace systems and
running educational flight analysis in AstraLab.

Files use the `.arvt` extension.

## Minimal Program

```arvt
rocket StudentRocket

body {
    length 1.2m
    diameter 0.08m
    mass 0.35kg
}

motor {
    thrust_curve E12
}

simulate
report
graph altitude
```

## Structure

An ARVT file begins with a rocket declaration:

```arvt
rocket RocketName
```

After that, the file may contain object blocks and commands.

```arvt
object_name {
    property value
}

command optional_arguments
```

Comments start with `#`.

## Supported Objects

- `rocket`
- `nosecone`
- `body`
- `fins`
- `motor`
- `payload`
- `recovery`
- `avionics`
- `environment`
- `mission`

## Object Properties

### `nosecone`

- `shape`: `ogive`, `conical`, `von_karman`, or descriptive text
- `length`: length in meters, centimeters, or millimeters
- `mass`: mass in kilograms or grams

### `body`

- `length`
- `diameter`
- `mass`

### `fins`

- `count`
- `span`
- `sweep`
- `mass`

### `motor`

- `thrust_curve`: motor designation such as `E12`, `G80`, `I280`
- `propellant_mass`: optional override
- `impulse`: optional total impulse override in newton seconds

AstraLab v0.1 synthesizes an educational thrust curve from the designation.
For example, `E12` means an E-class impulse range with approximately 12 N average thrust.

### `payload`

- `mass`

### `recovery`

- `parachute_area`
- `deploy_delay`
- `mass`

### `avionics`

- `mass`

### `environment`

- `wind`
- `temperature`

### `mission`

- `launch_angle`
- `rail_length`
- `max_time`

## Units

Supported units include:

- Length: `m`, `cm`, `mm`, `km`
- Area: `m2`, `m^2`, `cm2`, `cm^2`
- Mass: `kg`, `g`
- Time: `s`, `ms`
- Angle: `deg`, `rad`
- Speed: `m/s`, `km/h`
- Pressure: `Pa`, `kPa`
- Temperature: `K`

Values without units are interpreted in SI base units.

## Commands

- `simulate`
- `report`
- `graph altitude`
- `graph velocity`
- `graph acceleration`
- `graph mach`
- `graph dynamic_pressure`
- `export csv`
- `export report`
- `export graphs`
- `export json`
- `montecarlo N`
- `save`
- `load`
- `help`
- `clear`

## Interpreter Model

ARVT v0.1 is interpreted directly:

```text
source -> lexer -> tokens -> parser -> AST -> interpreter -> simulation/report/export
```

There is no compiler, bytecode, VM, or JIT.

## Project Format

AstraLab projects use this layout:

```text
ProjectName/
  main.arvt
  project.aproj
  reports/
  graphs/
  exports/
```

`project.aproj` is JSON:

```json
{
  "name": "Student Rocket",
  "version": "1.0",
  "main": "main.arvt"
}
```
