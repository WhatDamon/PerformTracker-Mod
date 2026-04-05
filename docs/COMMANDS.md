# Commands

## Tracking Commands

### `/ptracker start`

Start tracking performance metrics.

```bash
/ptracker start
```

**Output:** Shows the file path where metrics will be saved.

### `/ptracker stop`

Stop tracking and display collected samples count.

```bash
/ptracker stop
```

### `/ptracker deviceinfo`

Display detailed device and system information.

```bash
/ptracker deviceinfo
```

**Shows:**
- Device type (PC, Mac, Phone, etc.)
- CPU model and cores
- GPU model
- Total memory
- Operating system
- Java version

## Configuration Commands

### `/ptracker config`

List all configuration options with their current values.

```bash
/ptracker config
```

### `/ptracker config <option>`

View the current value and default for a specific option.

```bash
/ptracker config output_interval
```

**Output:**
```
output_interval: 5 (default: 5)
```

### `/ptracker config <option> <value>`

Set a configuration option value.

```bash
/ptracker config output_interval 10
/ptracker config chat_enabled true
/ptracker config network_host localhost
```

For detailed configuration options, see [Configuration](CONFIGURATION.md).
