# Configuration

## General Settings

### `output_interval`

- **Default:** `5`
- **Range:** `1-3600` seconds

How often to collect and output performance metrics.

```bash
/ptracker config output_interval 10
```

### `chat_enabled`

- **Default:** `false`

Display real-time metrics in the chat bar.

```bash
/ptracker config chat_enabled true
```

### `export_enabled`

- **Default:** `true`

Save metrics to files for later analysis.

```bash
/ptracker config export_enabled true
```

### `export_directory`

- **Default:** `performance_data`

Directory where export files will be saved.

```bash
/ptracker config export_directory my_metrics
```

### `output_format`

- **Default:** `csv`
- **Options:** `csv`, `json`, `yaml`

File format for exported metrics.

```bash
/ptracker config output_format json
```

### `binary_units`

- **Default:** `true`

Use binary (1024-based) units (MiB/GiB) instead of decimal (1000-based).

```bash
/ptracker config binary_units true
```

## Metrics Collection

### `collect_fps`

- **Default:** `true`

Collect client-side frame rate statistics.

```bash
/ptracker config collect_fps true
```

### `collect_tps`

- **Default:** `true`

Collect server-side ticks per second.

```bash
/ptracker config collect_tps true
```

### `collect_mspt`

- **Default:** `true`

Collect mean server tick time in milliseconds.

```bash
/ptracker config collect_mspt true
```

### `collect_heap`

- **Default:** `true`

Collect JVM heap memory usage.

```bash
/ptracker config collect_heap true
```

### `collect_cpu`

- **Default:** `true`

Collect system CPU usage percentage.

```bash
/ptracker config collect_cpu true
```

## Network Settings

### `network_enabled`

- **Default:** `false`

> [!WARNING]
> Enable only if you understand the risks! This will expose your device information and metrics to the configured server.

```bash
/ptracker config network_enabled true
```

### `network_host`

- **Default:** `localhost`

Host for HTTP binding and UDP destination. Can be a hostname or IP address.

```bash
/ptracker config network_host 192.168.1.100
/ptracker config network_host myserver.com
```

### `receiver_port`

- **Default:** `31415`

Local HTTP server port for `/api/deviceinfo` endpoint.

```bash
/ptracker config receiver_port 31415
```

### `sender_port`

- **Default:** `31416`

Port for sending metrics via UDP.

```bash
/ptracker config sender_port 31416
```

## In-Game Configuration

You can also configure settings via the Cloth Config for a graphical interface.
