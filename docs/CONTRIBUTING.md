# Contributing to PerformTracker

Thank you for your interest in contributing to PerformTracker!

## Ways to Contribute

- **Bug Reports**: Report issues on GitHub with steps to reproduce (or logs)
- **Feature Requests**: Suggest new features or improvements
- **Pull Requests**: Submit code changes (see guidelines below)
- **Documentation**: Improve docs or translate into other languages

## AI Contributions

> [!NOTE]
> AI-assisted contributions are welcome, but **must undergo human review** before merging. PRs generated solely by AI without human review will not be accepted.

> [!WARNING]
> **AI Code Licensing**: Be aware that AI-generated code may have unclear or incompatible licenses. Many AI models output code with restrictions that conflict with Apache 2.0. You are responsible for ensuring AI-generated contributions are properly licensed.

When using AI tools to assist with code:
- Check AI model licensing
- Review all AI-generated changes thoroughly
- Ensure code follows project patterns and style
- Test changes before submitting
- Be prepared to explain and justify AI-generated code

## Development Setup

### Requirements

- Java 21 or higher
- Git
- Recommended IDE: IntelliJ IDEA

### Minecraft Version Porting

#### Mappings

If you wish to port the mod to another version of Minecraft, please note the mapping.

| Minecraft Version | Mappings            |
|-------------------|---------------------|
| 1.21.11 and older | **Yarn**            |
| 26.1 and above    | **Mojang Mappings** |

**References:**
- [Porting to 26.1](https://docs.fabricmc.net/26.1/develop/porting/)
- [Fabric API 26.1 Porting Guide](https://docs.fabricmc.net/26.1/develop/porting/fabric-api)

### Building

```bash
git clone https://github.com/WhatDamon/PerformTracker.git
cd PerformTracker
./gradlew build
```

The JAR will be in `build/libs/`.

### Code Style

- Follow existing code patterns in the project
- Use meaningful variable and method names
- Avoid unnecessary comments or docstrings
- Prefer code reuse - leverage existing utilities and patterns before adding new code
- Run the following commands before submitting PR
  - `./gradlew check`
  - `./gradlew test`
  - `./gradlew checkstyleMain`
  - `./gradlew checkstyleClient`
  - `./gradlew checkstyleTest`

## Pull Request Guidelines

### Before Submitting

1. **Fork** the repository and create a feature branch
2. **Test** your changes locally
3. **Write tests** when necessary - new features or bug fixes should have appropriate test coverage
4. **Keep changes focused** - one feature or fix per PR
5. **Update documentation** if needed

### Submit Your Works

Please use clear, concise PR titles:

```
type: short description
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `refactor`: Code restructuring (no behavior change)
- `docs`: Documentation changes
- `test`: Test additions/changes
- `style`: Code style changes
- `chore`: Maintenance tasks
- `workflow`: Workflow changes (including CI/CD changes)

### Network Architecture Notes

This project uses a hybrid TCP/UDP network architecture:

- **HTTP (TCP)** - `/api/deviceinfo` endpoint for device queries
- **UDP** - Fire-and-forget metrics transmission

When modifying network code, be aware of:
- `HttpServerManager.java` - HTTP server for device info
- `UdpMetricsClient.java` - UDP client for metrics
- Config validation in `ConfigAccess.java`

## Testing

### Running Tests

```bash
./gradlew test
```

### Network Testing

Use the test scripts in `tools/`:

```bash
# Test UDP metrics
python tools/test_udp_server.py --port 31416
```

For `/api/deviceinfo`, simply access it through web browser to test, or using `curl`.

Check out more details at [Network API](NETWORK-API.md)

## Localization

The project supports following languages:
- English (`en_us.json`)
- Simplified Chinese (`zh_cn.json`)
- Traditional Chinese (`zh_tw.json`)
- Classical Chinese (`lzh.json`)

**You're welcome to add new languages which are supported by Minecraft!**

## License Compliance

This project is licensed under **Apache License 2.0**.

### Requirements for Submissions

When contributing code, you must ensure:

1. **Include License Header**: All new source files must include the Apache 2.0 license header:

```java
/*
 * Copyright [YEAR] Damon Lu and open-source contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

2. **Third-Party Code**: Only submit code that you have the right to contribute. If using third-party libraries or code snippets:
   - Ensure they are compatible with Apache 2.0
   - Provide appropriate attribution if required
   - Do not introduce incompatible licenses

3. **No Additional Restrictions**: Do not submit code that imposes additional restrictions beyond Apache 2.0.

### Why This Matters

The Apache 2.0 license allows you to:
- Use the code commercially
- Modify and distribute
- Patent grants

While requiring:
- Attribution
- Clear indication of changes
- Inclusion of license notice

## Questions?

Feel free to open an issue for questions or discussions.
