# Create: Stressbound

Create: Stressbound is a Minecraft 1.21.1 NeoForge addon for Create 6. It adds server-friendly wireless stress links that can bridge rotational power between local Create networks, remote machines, Create contraptions, trains, and compatible moving structures.

Instead of running shafts across long distances, players can bind a Stress Transmitter to a Stress Receiver with the Kinetic Binder. The receiver then outputs the transmitter's rotational speed while still respecting stress budgets and server-side limits.

## Demo

### In-Game Test

![In-game stress link test](https://github.com/Mai-xiyu/Create-Stressbound/raw/main/docs/In-Game%20Test.gif)

### Ponder Tutorial

![Ponder tutorial](https://github.com/Mai-xiyu/Create-Stressbound/raw/main/docs/Ponder.gif)

### Wireless Controller Demo

![Wireless controller demo](https://github.com/Mai-xiyu/Create-Stressbound/raw/main/docs/Wireless%20Controller%20Demo.gif)

### Create Aeronautics Compatibility

![Create Aeronautics compatibility demo](https://github.com/Mai-xiyu/Create-Stressbound/raw/main/docs/Create%20Aeronautics%20Compatibility.gif)

## Features

- Adds `Stress Transmitter`, `Stress Receiver`, and `Kinetic Binder`.
- Provides a dedicated creative tab: `Create: Stressbound`.
- Transfers rotational speed through saved stress links without requiring a physical shaft line.
- Reserves configurable SU budgets per receiver to avoid free stress duplication.
- Supports static Create kinetic networks.
- Supports runtime anchors for Create contraptions and Create trains.
- Includes heuristic runtime-anchor support for Create Aeronautics / Simulated-style moving structures.
- Displays link state, remote speed, requested SU, and granted SU through Create goggles.
- Provides Ponder scenes for basic linking and moving-structure behavior.
- Includes English and Simplified Chinese localization.
- Keeps server control in config: link limits, receiver limits, stress budgets, overload mode, and evaluation interval.

## Requirements

- Minecraft `1.21.1`
- NeoForge `21.1.x`
- Create `6.0.10`
- Ponder `1.0.82+`
- Flywheel `1.0.6`
- Java `21`

Optional compatibility targets:

- Create contraptions
- Create trains
- Create Aeronautics / Simulated-style moving structures

## Quick Start

1. Place a `Stress Transmitter` next to an existing powered Create kinetic network.
2. Place a `Stress Receiver` where you want remote rotational output.
3. Right-click the transmitter with a `Kinetic Binder` to store it.
4. Right-click the receiver with the same binder to create the link.
5. Connect shafts, gearboxes, or machines to the receiver output.
6. Wear Create goggles to inspect speed, status, and SU budget.

Useful interactions:

- Shift-right-click a receiver with the `Kinetic Binder` to clear that receiver's link.
- Shift-right-click air with the `Kinetic Binder` to clear the stored transmitter selection.
- Use a Create wrench to adjust block facing as usual.

## Stress Budget Model

Stressbound is designed to avoid turning remote transmission into free power.

Each receiver reserves a configurable SU budget. During evaluation, the transmitter checks the available stress of its local network and grants output only if the linked receivers fit within the configured limits.

Important behavior:

- A transmitter does not generate stress by itself.
- A receiver outputs the remote rotational speed only while the link is valid, loaded, and not disabled.
- Strict overload mode can shut down all receivers on a transmitter when total reserved stress exceeds available stress.
- Server owners can limit links per player, receivers per transmitter, and max SU per link.

## Commands

The following commands require permission level 2:

```mcfunction
/stressbound links list
/stressbound links remove <uuid>
/stressbound links removeplayer <player>
/stressbound links setstress <uuid> <su>
/stressbound links setallstress <su>
/stressbound compat
```

For high-speed testing, raise the reserved stress budget of existing links:

```mcfunction
/stressbound links setallstress 8192
```

If that is still not enough for your setup, raise it further:

```mcfunction
/stressbound links setallstress 16384
```

## Configuration

The common config is generated at:

```text
config/create_stressbound-common.toml
```

Common options:

- `defaultRequestedStress`: default SU reservation for newly bound receivers.
- `maxStressPerLink`: maximum SU per link. Use `-1` for unlimited.
- `strictOverloadMode`: when enabled, over-budget transmitter groups stop instead of partially granting receivers.
- `evaluationIntervalTicks`: server tick interval for recalculating link state and stress budgets.
- `maxLinksPerPlayer`: maximum active links owned by one player.
- `maxReceiversPerTransmitter`: maximum receivers bound to one transmitter.
- `transmitterPoweredStops`: redstone signal disables transmitter output.
- `receiverPoweredStops`: redstone signal disables receiver output.

Existing links store their `RequestedStress` in world data. Changing `defaultRequestedStress` affects new links only. To update existing links, rebind them or use:

```mcfunction
/stressbound links setallstress <su>
```

## Compatibility Notes

Current support:

- Static Create kinetic networks: supported.
- Create contraptions: runtime anchor support.
- Create trains: runtime anchor support.
- Create Aeronautics / Simulated-style bodies: heuristic runtime anchor support.
- Valkyrien Skies: detected, but full runtime bridging is not implemented yet.

For server safety, moving receivers do not continuously rewrite Create kinetic networks while riding moving entities. Links remain saved and recover when endpoints return to a supported static/runtime state.

## Building

Windows:

```powershell
./gradlew.bat build
```

Linux / macOS:

```bash
./gradlew build
```

The built jar is written to:

```text
build/libs/create_stressbound-1.0-SNAPSHOT.jar
```

## License

All Rights Reserved.