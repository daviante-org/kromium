[Documentation Hub](../README.md) / **Deployment & Operations**

---

# Deployment & Operations

This section covers repository deployment, CI/CD automation, and library distribution infrastructure for Kromium.

---

## Guides in this Section

### 1. [Cloudflare R2 Maven Repository Guide](r2-maven-repository.md)
Complete end-to-end documentation for Kromium's binary distribution pipeline:
- **Cloudflare R2 Architecture:** S3-compatible, edge-cached, zero-egress artifact hosting at `https://repo.daviante.org/artifacts`.
- **Publisher Workflow:** Gradle publication to local staging and automated synchronization via AWS CLI.
- **GitHub Actions Automation:** Continuous deployment workflow secrets (`R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`) and publish steps.
- **Consumer Configuration:** How downstream Kotlin/Java applications configure Gradle (`settings.gradle.kts` / `build.gradle.kts`) and Apache Maven (`pom.xml`) to consume Kromium artifacts.

---

## Navigation

- [← Previous: Core API Reference](../api/README.md)
- [Home: Documentation Hub →](../README.md)
