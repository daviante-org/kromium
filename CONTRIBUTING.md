# Contributing to Kromium

Thank you for your interest in contributing to **Kromium**! We welcome community contributions, bug reports, feature proposals, and documentation enhancements to help build the best Chromium browser engine for Kotlin and Java desktop applications.

Please take a few moments to review this guide before getting started. It outlines our development environment setup, how to build and test from source, our architectural principles, coding conventions, and our Pull Request workflow.

---

## Table of Contents

- [Contributor License Agreement (CLA)](#contributor-license-agreement-cla)
- [Code of Conduct](#code-of-conduct)
- [Prerequisites & Development Setup](#prerequisites--development-setup)
  - [Java Development Kit (JDK)](#java-development-kit-jdk)
  - [Operating System Requirements](#operating-system-requirements)
- [Building from Source](#building-from-source)
  - [Cloning the Repository](#cloning-the-repository)
  - [Common Gradle Commands](#common-gradle-commands)
  - [Running the Sample Applications](#running-the-sample-applications)
- [Repository Architecture](#repository-architecture)
  - [Core Principles](#core-principles)
  - [Module Structure](#module-structure)
- [Coding & Engineering Standards](#coding--engineering-standards)
  - [Strict Facade Decoupling (Zero Native Leaks)](#strict-facade-decoupling-zero-native-leaks)
  - [Coroutine & Reactive Conventions](#coroutine--reactive-conventions)
  - [Resource & Buffer Lifecycle](#resource--buffer-lifecycle)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Submitting a Pull Request](#submitting-a-pull-request)
  - [Branching Strategy](#branching-strategy)
  - [Pull Request Checklist](#pull-request-checklist)
  - [Review & CI Verification](#review--ci-verification)

---

## Contributor License Agreement (CLA)

Kromium is an open-source project published under the [Apache License, Version 2.0](LICENSE).

To safeguard the project, ensure clean intellectual property provenance, and enable [Daviante Group](https://github.com/daviantegroup) to maintain unified ownership of the codebase (including rights for commercial licensing, dual-licensing, or business acquisition), all contributions to this repository are governed by the following **Contributor License Agreement (CLA)**.

By submitting a Pull Request, code commit, patch, documentation update, or feature implementation (collectively, a "Contribution") to this repository or to Daviante Group, you represent, warrant, and agree to the following terms:

### 1. Assignment of Intellectual Property & Copyright
You hereby irrevocably, unconditionally, and perpetually assign, transfer, and convey to Daviante Group all present and future worldwide rights, title, and interest in and to your Contribution, including all copyrights, patent rights, trade secret rights, and all other proprietary interests.

### 2. Commercial Exploitation & Dual-Licensing
You acknowledge and agree that Daviante Group retains the sole, unrestricted, and exclusive right to commercially sell, license, sub-license, dual-license, monetize, or distribute the software, your Contributions, or derivative products worldwide without royalty, accounting, or obligation of compensation or attribution to you.

### 3. Originality & Legal Authority
You represent and warrant that:
- Your Contribution is entirely your own original creation.
- You have the full legal right, capacity, and authority to grant this assignment.
- Your Contribution does not infringe upon any third-party copyright, patent, trade secret, trademark, or other proprietary right.
- If you are employed or performing work under contract, your employer or client has authorized you to make this Contribution on their behalf, or has waived any claim to the Contribution.

### 4. Waiver of Moral Rights
To the maximum extent permitted by applicable law, you irrevocably waive all moral rights, paternity rights, or rights of integrity in and to each Contribution.

---

## Code of Conduct

We are committed to providing a welcoming, inclusive, and professional environment for all contributors. We expect all participants to:
- Be courteous, respectful, and collaborative.
- Provide constructive and actionable feedback on issues and code reviews.
- Focus on what is best for the community, security, and project architecture.

---

## Prerequisites & Development Setup

### Java Development Kit (JDK)

- **JDK 17 or higher** is required (JDK 21 is actively tested and recommended).
- Recommended distribution: **[Eclipse Temurin](https://adoptium.net/)** (OpenSearch / Adoptium) or **JetBrains Runtime (JBR)**.
- Ensure `JAVA_HOME` environment variable points to your JDK 17+ installation:
  ```bash
  # Verify Java version
  java -version
  ```

### Operating System Requirements

Kromium runs on Windows, macOS, and Linux:

- **Windows:**
  - Windows 10/11 (64-bit x86_64 or ARM64).
  - [Microsoft Visual C++ Redistributable 2015–2022](https://learn.microsoft.com/en-US/cpp/windows/latest-supported-vc-redist) is required for native CEF runtime dependencies.
- **macOS:**
  - macOS 12 Monterey or later (Apple Silicon ARM64 and Intel x86_64 supported).
  - Xcode Command Line Tools (`xcode-select --install`).
- **Linux:**
  - Any modern 64-bit distribution (Ubuntu 22.04 LTS+, Debian 12+, Fedora 38+).
  - System runtime libraries for Chromium/CEF and GTK:
    ```bash
    # Ubuntu / Debian
    sudo apt-get update && sudo apt-get install -y \
      libgtk-3-0 libnotify4 libnss3 libxss1 libasound2 libsecret-1-0 \
      libx11-xcb1 libxcb-dri3-0 libdrm2 libgbm1 xvfb
    ```

---

## Building from Source

Kromium uses the standard Gradle Wrapper (`gradlew` on Unix / macOS, `gradlew.bat` on Windows). You do not need to install Gradle manually.

### Cloning the Repository

Clone the repository and inspect the active branch (`stable` is the default integration branch):

```bash
git clone https://github.com/daviante-org/kromium.git
cd kromium
```

### Common Gradle Commands

| Task | Command (Unix/macOS) | Command (Windows) | Description |
| :--- | :--- | :--- | :--- |
| **Assemble** | `./gradlew assemble` | `.\gradlew.bat assemble` | Compiles all modules and builds output JARs without running tests. |
| **Test** | `./gradlew test` | `.\gradlew.bat test` | Runs unit tests across all modules. |
| **Full Check** | `./gradlew check` | `.\gradlew.bat check` | Runs all linters, static checks, and unit tests. |
| **Publish Local** | `./gradlew publishToMavenLocal` | `.\gradlew.bat publishToMavenLocal` | Publishes snapshot artifacts to your local `~/.m2/repository`. |
| **Clean** | `./gradlew clean` | `.\gradlew.bat clean` | Deletes build artifacts and caches. |

> [!TIP]
> **Headless Linux Testing:**  
> When running UI toolkit tests or full verification on headless Linux servers/CI, invoke tests through an X virtual framebuffer:
> ```bash
> xvfb-run --auto-servernum ./gradlew check --no-daemon
> ```

---

### Running the Sample Applications

The [`samples/`](samples/) directory contains reference applications demonstrating Kromium across different desktop UI frameworks. Running these is the best way to verify changes locally:

```bash
# Run JetBrains Compose Multiplatform sample
./gradlew :samples:kromium-sample-compose:run

# Run Swing sample (JPanel double-buffered Java2D)
./gradlew :samples:kromium-sample-swing:run

# Run JavaFX sample (StackPane Prism PixelBuffer)
./gradlew :samples:kromium-sample-javafx:run

# Run Eclipse SWT sample
./gradlew :samples:kromium-sample-swt:run

# Run Pure AWT sample
./gradlew :samples:kromium-sample-awt:run
```

> [!NOTE]
> **macOS Note for SWT:** The SWT sample automatically configures `-XstartOnFirstThread` required by macOS Cocoa event loops. If invoking SWT apps manually on macOS, this JVM flag is mandatory.

---

## Repository Architecture

### Core Principles

1. **Provider-Agnostic Facade:** The public developer API ([`kromium-api`](kromium-api)) is completely decoupled from the underlying Chromium Embedded Framework native implementation.
2. **Zero Native Leaks:** No classes from `org.cef.*` or native JNI handles may leak into public API signatures or UI toolkit modules.
3. **Reactive Coroutines:** State, navigation progress, and automation streams use Kotlin Coroutines (`StateFlow`, `SharedFlow`, `suspend` functions).
4. **Pure Off-Screen Rendering (OSR):** Kromium renders frames directly into off-screen byte buffers, ensuring zero heavyweight window handle conflicts, no airspace issues, and seamless alpha blending / layering across UI components.

### Module Structure

```
kromium/
├── kromium-api/              # Public facade contracts, configs, events, domain models (Zero native dependencies)
├── kromium-core/             # Shared utilities, runtime helpers, platform detection
├── kromium-provider-jcef/    # CEF / JCEF native implementation, process management, OSR render pipe
├── kromium-compose/          # JetBrains Compose Multiplatform binding (KromiumView)
├── kromium-swing/            # Swing binding (KromiumSwingCanvas)
├── kromium-javafx/           # JavaFX binding (KromiumJavaFxCanvas)
├── kromium-swt/              # Eclipse SWT binding (KromiumSwtCanvas)
├── kromium-awt/              # Pure AWT binding (KromiumAwtCanvas)
└── samples/                  # Executable reference implementations for each UI toolkit
```

---

## Coding & Engineering Standards

### Strict Facade Decoupling (Zero Native Leaks)

- All user-facing APIs must reside under `org.daviante.kromium.api.*`.
- **Never import `org.cef.*`** inside `kromium-api`, `kromium-core`, `kromium-compose`, `kromium-swing`, `kromium-javafx`, `kromium-swt`, or `kromium-awt`.
- Only `kromium-provider-jcef` is permitted to depend on native CEF bindings. If you need new engine functionality, define the interface in `kromium-api` first, then implement it in `kromium-provider-jcef`.

### Coroutine & Reactive Conventions

- Prefer `StateFlow` for state that has a current value (e.g., `KromiumNavigationState`, loading status).
- Prefer `SharedFlow` for event streams (e.g., console messages, download events).
- Do not perform blocking I/O or sleep calls on the UI thread or default dispatchers. Use `Dispatchers.IO` for file/network operations.
- Ensure all flows and coroutine scopes are tied to proper lifecycle lifespans and cancel cleanly upon browser disposal.

### Resource & Buffer Lifecycle

- Chromium native processes, off-screen shared memory buffers, and DevTools sessions consume significant system resources.
- Every newly created component or client MUST implement `AutoCloseable` or provide explicit `close()` / `dispose()` cleanup routines.
- Always guard native pointers against double-free errors and ensure safe garbage collection of direct `ByteBuffer` allocations.

---

## Commit Message Guidelines

We follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) specification. This enables clean changelogs, automated semantic releases, and clear git history.

Format: `<type>(<optional scope>): <description>`

### Types
- `feat`: A new feature or capability.
- `fix`: A bug fix.
- `docs`: Documentation updates or typo corrections.
- `style`: Formatting, missing semicolons, whitespace (no functional code change).
- `refactor`: Code changes that neither fix bugs nor add features.
- `perf`: Performance improvements (OSR throughput, buffer pooling, coroutine latency).
- `test`: Adding or correcting tests.
- `build`: Changes affecting build scripts or external dependencies (`build.gradle.kts`, `libs.versions.toml`).
- `ci`: Changes to CI/CD workflows (`.github/workflows/`).
- `chore`: Routine maintenance, updating `.gitignore`, license notices, etc.

### Examples
- `feat(api): add dynamic proxy authentication listener`
- `fix(provider-jcef): prevent race condition during OSR buffer swap on resize`
- `docs(getting-started): clarify JDK 21 installation steps`
- `perf(compose): optimize pixel buffer blitting to reduce Compose recompositions`

---

## Submitting a Pull Request

### Branching Strategy

1. The default and primary active branch is **`stable`**.
2. Create a focused topic branch from `stable` before making changes:
   ```bash
   git checkout stable
   git pull origin stable
   git checkout -b feat/your-feature-name
   # or: fix/issue-description, docs/topic-name
   ```

### Pull Request Checklist

Before submitting your PR, ensure:

- [ ] The project compiles cleanly via `./gradlew assemble`.
- [ ] All tests pass cleanly via `./gradlew check` (or `./gradlew test`).
- [ ] Code adheres to Kotlin coding conventions and formatting guidelines.
- [ ] No native `org.cef.*` imports exist outside `kromium-provider-jcef`.
- [ ] Any new or modified public APIs have comprehensive KDoc documentation.
- [ ] Commit messages follow the Conventional Commits specification.
- [ ] You have reviewed and accepted the [Contributor License Agreement (CLA)](#contributor-license-agreement-cla).

### Review & CI Verification

1. **Open the PR:** Target the `stable` branch on `daviante-org/kromium`.
2. **Describe Changes:** Explain what the PR accomplishes, which issues it resolves, and include steps to verify the changes. If visual UI components are altered, include screenshots or GIFs.
3. **Automated CI:** GitHub Actions will run automated build and test workflows across supported platforms. All checks must pass before a PR can be merged.
4. **Code Review:** A maintainer will review your contribution, suggest adjustments if necessary, and approve once ready.

Thank you for contributing to Kromium!
