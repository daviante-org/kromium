# Security Policy

The Kromium engineering team and Daviante Group take the security and integrity of Kromium and its downstream consumers very seriously. Kromium is a high-performance browser engine embedding native Chromium Embedded Framework (CEF) binaries with multi-toolkit JVM desktop bindings; security is central to our architecture.

This document describes our security policy, supported versions, how to report potential vulnerabilities responsibly, and what you can expect during the disclosure process.

---

## Supported Versions

Security updates and critical vulnerability patches are actively maintained for the following versions:

| Version | Status | Supported | Notes |
| :--- | :--- | :---: | :--- |
| `0.1.x` (`stable`) | Active Development | :white_check_mark: | Current active release branch. All security fixes land here. |
| `< 0.1.0` | Development Snapshots | :x: | Experimental and snapshot builds are not supported. |

We strongly advise all developers integrating Kromium to keep their dependencies updated to the latest minor/patch release on the `stable` branch.

---

## Reporting a Vulnerability

> [!CAUTION]
> **Please do not report security vulnerabilities through public GitHub issues, public pull requests, or public discussions.** Publicly disclosing vulnerabilities exposes applications and end-users before a mitigation can be authored, validated, and distributed.

### Preferred Method: GitHub Private Vulnerability Reporting

The fastest and most secure method to disclose a security concern is through GitHub's Private Vulnerability Reporting mechanism:

1. Navigate to the [Kromium Security Advisories](https://github.com/daviante-org/kromium/security/advisories) page.
2. Click **"Report a vulnerability"** to open a confidential draft advisory.
3. Provide full details regarding the vulnerability (see [What to Include](#what-to-include-in-a-report) below).
4. Submit the report. This opens a private communication channel directly with the project maintainers.

### Alternative Method: Direct Security Email

If you cannot use GitHub's advisory interface or prefer encrypted communication, you may contact the core security maintainers directly:

- **Email:** [code@daviante.org](mailto:code@daviante.org)
- **Subject line:** `[SECURITY] Kromium Vulnerability Report - <Short Summary>`

If you require PGP encryption, please mention this in an initial email or request our public PGP key before sending sensitive exploit material.

---

## What to Include in a Report

To help us investigate, triage, and reproduce the issue rapidly, please include as much of the following information as possible:

1. **Vulnerability Type:**
   - E.g., Memory corruption, native crash / segmentation fault in OSR pipeline, sandbox escape, remote code execution (RCE), proxy authentication credential leak, insecure TLS / certificate verification bypass, improper cross-origin handling, or permission escalation.
2. **Affected Components:**
   - Module name (e.g., `kromium-provider-jcef`, `kromium-api`, `kromium-compose`, `kromium-swing`, `kromium-javafx`, `kromium-swt`, `kromium-awt`).
   - Exact Kromium version or commit SHA.
   - Host Operating System (Windows, macOS, Linux) and architecture (`x86_64`, `aarch64` / Apple Silicon).
   - JDK distribution and version (e.g., Eclipse Temurin 17.0.10, JetBrains Runtime 21, etc.).
3. **Step-by-step Reproduction:**
   - Clear, reproducible steps to trigger the behavior.
   - A minimal reproducible Kotlin or Java code snippet, sample project, or test case.
   - Any malicious HTML/JavaScript payload required for demonstration (hosted safely or attached in a private archive).
4. **Threat Model & Impact Assessment:**
   - Attack vector (local vs. remote).
   - Privileges required and user interaction necessary (e.g., visiting a malicious URL vs. developer misconfiguration).
   - Real-world impact on applications embedding Kromium.
5. **Mitigations or Proposed Fixes (if known):**
   - Suggested code changes, configuration overrides, or temporary workarounds.

---

## Triage & Response Process

Once a vulnerability report is submitted:

1. **Initial Acknowledgment:**  
   A Kromium maintainer will acknowledge receipt of your report within **48 business hours**.
2. **Triage & Validation:**  
   The team will attempt to reproduce the vulnerability, evaluate its severity using the CVSS (Common Vulnerability Scoring System) framework, and verify whether it originates in Kromium's JVM facade, OSR rendering pipeline, or upstream CEF/Chromium. We aim to complete triage within **5 to 7 business days**.
3. **Patch Development:**  
   Fixes will be developed and verified in a private security fork or branch. You may be invited to collaborate or review the patch to confirm the resolution.
4. **Coordinated Disclosure & Advisory:**  
   We practice **Coordinated Vulnerability Disclosure (CVD)**:
   - A mutually agreed-upon public disclosure date will be established (standard embargo window is typically 90 days, or sooner upon release of a patched version).
   - A GitHub Security Advisory and CVE identifier will be requested and published alongside the release of the patched version.
   - Credit will be attributed to the finder in the advisory and release notes (unless anonymity is requested).

---

## Scope & Out-of-Scope Concerns

### In Scope
- Memory leaks, buffer overruns, or native crashes within the Kromium JNI / OSR pipeline (`kromium-provider-jcef`).
- Security bypasses in custom scheme handlers, proxy authentication, or SSL certificate error interception.
- Insecure defaults in `KromiumConfig` that undermine standard Chromium sandbox policies.
- Race conditions or state corruption in coroutine-based navigation and automation streams.
- Information disclosure between isolated `KromiumClient` instances sharing an engine.

### Out of Scope
- **Upstream Chromium / CEF Vulnerabilities:** Issues present strictly in upstream Chromium or CEF that are not specific to Kromium's integration layer should be reported directly to the [Chromium Security Team](https://www.chromium.org/Home/chromium-security/reporting-bugs/) or CEF project. However, notify us if an upstream issue critically impacts Kromium so we can bump dependencies.
- **Physical or Root Attacks:** Attacks that require root access, physical possession of the device, or debugging access to the host JVM process.
- **Social Engineering:** Phishing or social engineering targeting project maintainers or users.
- **Denial of Service via Unchecked Web Scripts:** Standard web pages consuming CPU/RAM via intensive WebGL/JavaScript execution when running inside an intentional browser instance, unless it causes host JVM memory corruption or bypasses process isolation.

---

## Safe Harbor & Research Guidelines

We consider security research conducted in good faith to be an invaluable service to the open-source community. If you conduct vulnerability research in accordance with this policy:

- We will not initiate legal action against you for unintentional violations or research activities that conform to this policy.
- We will work transparently with you to understand and resolve the issue quickly.
- We request that you give us reasonable time to investigate and mitigate the issue before disclosing it publicly.

Thank you for helping keep Kromium and its community safe!
