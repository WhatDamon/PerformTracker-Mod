![Icon](../src/main/resources/assets/performtracker/icon.png)
# PerformTracker

> [!NOTE]
> This mod will be locked to Minecraft 1.21.11 during the early development phase; we plan to support additional game versions in the future.

[![Available for Fabric](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.3.1/assets/compact/supported/fabric_vector.svg)](https://fabricmc.net/) [![Require Fabric API API](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.3.1/assets/compact/requires/fabric-api_vector.svg)](https://modrinth.com/mod/fabric-api/) [![Require Cloth Config API](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.3.1/assets/compact/requires/cloth-config-api_vector.svg)](https://modrinth.com/mod/cloth-config/)

A Minecraft Fabric mod for tracking game performance metrics. Initially designed for [Apple Silicon MC Shaders Guideline Project](https://github.com/WhatDamon/AppleSilicon-MCShaders)

## NeoForge/Forge?

![Won't support NeoForge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.3.1/assets/compact/unsupported/neoforge_vector.svg) ![Won't support Forge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.3.1/assets/compact/unsupported/forge_vector.svg)

This mod is intended solely as a Fabric mod; there are no plans to actively adapt it for NeoForge/Forge.

In theory, **it is compatible with the Sinytra Connector**, but we haven’t been able to test it yet because Connector isn’t compatible with Minecraft versions higher than 1.21.1. We may need to adapt it for future versions.

<!--![Sinytra Connector Compatible](https://raw.githubusercontent.com/Sinytra/.github/refs/heads/main/badges/connector/compacter.svg)![Forgified Fabric API Required on Sinytra Connector](https://raw.githubusercontent.com/Sinytra/.github/refs/heads/main/badges/forgified-fabric-api-neo/compacter.svg)-->

## Features

- **In-game Performance Display**: Shows real-time FPS, TPS, MSPT, heap memory, and CPU usage.
- **Metrics Export**: Exports performance data to CSV, JSON, or YAML formats.
- **Network API**: Exposes device info via HTTP and sends metrics via UDP for integration with monitoring tools.
- **Configurable Collection**: Toggle which metrics to collect and export.

## Documentation

- [Getting Started](GETTING-STARTED.md) - Installation, building, and quick start guide
- [Commands](COMMANDS.md) - Complete command reference
- [Configuration](CONFIGURATION.md) - Detailed configuration options
- [Network API](NETWORK-API.md) - HTTP and UDP API documentation
- [Privacy](PRIVACY.md) - Privacy risks and recommendations

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.

[![Available on GitHub](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.3.1/assets/compact/available/github_vector.svg)](https://github.com/WhatDamon/PerformTracker-Mod/) [![Help us Translate](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.3.1/assets/compact/translate/generic-plural_vector.svg)](https://github.com/WhatDamon/PerformTracker-Mod/tree/develop/src/main/resources/assets/performtracker/lang/)

## License

This project is licensed under the **Apache License, Version 2.0**.
