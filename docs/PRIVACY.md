# Privacy

> [!IMPORTANT]
> Even though this project does not include telemetry, this mod involves data collection and transmission that you should be aware of.

## Data Collection

### Local Data Export

When enabled, performance metrics are written to files in a configurable directory.

### Network Transmission

When the network feature is enabled, this mod transmits data via two protocols:

**HTTP (TCP):**
- Exposes device information via `/api/deviceinfo` endpoint
- Any party with network access can query this endpoint

**UDP:**
- Sends performance metrics to the configured `network_host:sender_port`
- Transmitted data includes:
  - Session identifier
  - Timestamp
  - All enabled metrics (FPS, TPS, heap, CPU, etc.)
  - Complete system information (CPU, GPU, OS, Java version, etc.)

## Risks

### No Authentication

The HTTP endpoint and UDP receiver have no built-in authentication or encryption. Any party with network access can receive and store your data.

### No Encryption

Data is transmitted in plain text over both HTTP and UDP.

### Data Retention

Any remote server you send data to will retain that data according to their own policies. This mod has no control over how your data is stored or used by third parties.

### System Information Exposure

The `/api/deviceinfo` endpoint exposes detailed system information including:
- CPU model and core count
- GPU model
- Operating system and version
- Java version
- Total memory

## Recommendations

1. **Trust the endpoint**: Only enable the network feature if you trust the server you're sending data to.

2. **Use localhost for testing**: When testing, use `localhost` (`127.0.0.1`) to avoid exposing data externally.

3. **Firewall configuration**: Configure your firewall appropriately:
   - Block external access to `receiver_port` (HTTP server)
   - Only allow outbound UDP to your trusted monitoring server

4. **Understand what's transmitted**: Review the data format in [Network API](NETWORK-API.md) before enabling network features.

5. **Consider your environment**: In shared hosting environments, other parties may be able to access your server's HTTP endpoint.

6. **Use a reverse proxy for production**: For production deployments exposed to untrusted networks, configure a reverse proxy (e.g., nginx, Caddy) in front of the mod's HTTP endpoint. This provides:
   - TLS/SSL termination
   - Authentication (Basic Auth, JWT, etc.)
   - Rate limiting
   - Access logging
   - Additional security headers

## Disabling Network Features

By default, all network features are disabled.

```bash
/ptracker config network_enabled false
```

Or via Cloth Config GUI in the mod menu.
