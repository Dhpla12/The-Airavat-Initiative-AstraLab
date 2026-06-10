# ARVT Guide

## What is ARVT?

ARVT is AstraLab's text format for describing an educational rocket simulation. An ARVT file starts with a rocket declaration, followed by optional component blocks and simulation commands.

```arvt
rocket StudentRocket

simulate
report
```

## Rocket Definition

Every file must begin with:

```arvt
rocket RocketName
```

Names should use letters, numbers, or underscores.

## Nosecone

```arvt
nosecone {
    shape ogive
    length 0.25m
    mass 0.08kg
}
```

Supported fields:

- `shape`
- `length`
- `mass`

## Body

```arvt
body {
    length 1.2m
    diameter 0.08m
    mass 0.35kg
}
```

Supported fields:

- `length`
- `diameter`
- `mass`

## Fins

```arvt
fins {
    count 4
    span 0.12m
    sweep 0.05m
    mass 0.06kg
}
```

Supported fields:

- `count`
- `span`
- `sweep`
- `mass`

## Motor

```arvt
motor {
    thrust_curve E12
    propellant_mass 0.025kg
    impulse 40
}
```

Supported fields:

- `thrust_curve`
- `propellant_mass`
- `impulse`

The current motor model is an educational designation-based approximation.
Impulse values are interpreted as newton-seconds.

## Recovery

```arvt
recovery {
    parachute_area 0.8m2
    deploy_delay 0.5s
    mass 0.05kg
}
```

Supported fields:

- `parachute_area`
- `deploy_delay`
- `mass`

## Environment

```arvt
environment {
    wind 3m/s
    temperature 288K
}
```

Supported fields:

- `wind`
- `temperature`

## Mission

```arvt
mission {
    launch_angle 3deg
    rail_length 1.0m
    max_time 90s
}
```

Supported fields:

- `launch_angle`
- `rail_length`
- `max_time`

## Simulation Commands

Place commands after the rocket blocks.

```arvt
simulate
graph altitude
graph velocity
graph acceleration
graph dynamic_pressure
graph trajectory
export csv
export graphs
report
```

Supported commands:

- `simulate`
- `report`
- `graph altitude`
- `graph velocity`
- `graph acceleration`
- `graph mach`
- `graph dynamic_pressure`
- `graph trajectory`
- `export csv`
- `export report`
- `export graphs`
- `montecarlo N`
- `help`
- `clear`

## Reports

Reports include project name, rocket name, timestamp, apogee, maximum velocity, maximum acceleration, maximum Mach number, maximum dynamic pressure, landing distance, CG, CP, static margin, mass and thrust checks, and simulation warnings.

For projects, reports are written to:

```text
reports/report.txt
```

## Example Files

Bundled examples:

- `examples/student_rocket.arvt`
- `examples/high_altitude.arvt`
- `examples/sounding_rocket.arvt`
- `examples/two_stage_demo.arvt`

Starter templates:

- `examples/templates/basic_rocket.arvt`
- `examples/templates/project_main.arvt`
