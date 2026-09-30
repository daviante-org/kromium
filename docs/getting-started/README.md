[Documentation Hub](../README.md) / **Getting Started**

---

# Getting Started with Kromium

Welcome to the **Getting Started** section of Kromium documentation. Whether you are building a modern Compose Multiplatform desktop application or integrating an embedded browser into existing Swing, JavaFX, SWT, or AWT systems, these guides will help you get running in minutes.

---

## Guides in this Section

### 1. [5-Minute Quickstart Guide](quickstart.md)
The fastest path to running Kromium:
- System prerequisites (JDK 17+, Windows, macOS, Linux).
- Adding Gradle dependencies with Version Catalogs or direct coordinates.
- Engine lifecycle bootstrapping (`JcefProvider.createEngine()`).
- Creating isolated browser clients and handling shutdown hooks.

### 2. [Multi-Framework UI Integration Guide](ui-frameworks.md)
Detailed integration instructions and full working code examples for all supported JVM desktop frameworks:
- **Compose Multiplatform:** `@Composable KromiumView` with reactive navigation state and loading overlay slots.
- **Swing:** `KromiumSwingCanvas` (`JPanel`) with double-buffered Java2D rendering.
- **JavaFX:** `KromiumJavaFxCanvas` (`StackPane`) powered by JavaFX Prism GPU `PixelBuffer`.
- **Eclipse SWT:** `KromiumSwtCanvas` (`Canvas`) with direct SWT GC drawing.
- **Pure AWT:** `KromiumAwtCanvas` (`Canvas`) for lightweight AWT containers.

---

## Navigation

- [← Home: Documentation Hub](../README.md)
- [Next: Core API Reference →](../api/README.md)
