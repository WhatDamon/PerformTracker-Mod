# PerformTracker

>[!NOTE]
>This mod will be locked to Minecraft 1.21.11 during the early development phase; we plan to support additional game versions in the future.

A Minecraft Fabric mod for tracking game performance metrics.

## Features

- **In-game Performance Display**: Shows real-time FPS, TPS, MSPT, heap memory, and CPU usage.
- **Metrics Export**: Exports performance data to CSV, JSON, or YAML formats.
- **Network API**: Exposes device info via HTTP (TCP) and sends metrics via UDP for integration with monitoring tools.
- **Configurable Collection**: Toggle which metrics to collect and export.

## Commands

- `/ptracker start` - Start tracking
- `/ptracker stop` - Stop tracking
- `/ptracker deviceinfo` - Display device information
- `/ptracker config` - Configure individual settings

## Building

```bash
./gradlew build
```

The built JAR will be in `build/libs/`.

## Contributing

Contributions are welcome. Please feel free to submit issues or pull requests on the GitHub repository.

## Privacy Risks

**IMPORTANT**: Even though this project does not include telemetry, this mod involves data collection and transmission that you should be aware of:

1. **Local Data Export**: When enabled, performance metrics are written to files in a configurable directory.

2. **Network Transmission**: When the network feature is enabled, this mod transmits performance metrics via UDP and exposes system information via HTTP:
   - Session identifier
   - Timestamp
   - All enabled metrics (FPS, TPS, heap, CPU, etc.)
   - Complete system information

3. **No Authentication**: The HTTP endpoint and UDP receiver have no built-in authentication or encryption. Any party with network access can receive and store your data.

4. **Data Retention**: Any remote server you send data to will retain that data according to their own policies. This mod has no control over how your data is stored or used by third parties.

**Recommendations:**
- Only enable the network feature if you trust the endpoint
- Use localhost (`127.0.0.1`) or a trusted local server when testing
- Be aware of what data is being collected and transmitted
- Review the data before sharing any exported files
- Consider the implications of exposing system information to network endpoints
- Firewall rules should be configured appropriately for your use case

## License

This project is licensed under the **Apache License, Version 2.0**.
