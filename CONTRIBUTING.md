# Contributing to Kromium

Thank you for your interest in contributing to **Kromium**!

Kromium is an open-source project published under the [Apache License, Version 2.0](LICENSE). 

To safeguard the project, ensure clean intellectual property provenance, and enable [Daviante Group](https://github.com/daviantegroup) to maintain unified ownership of the codebase (including rights for commercial licensing, dual-licensing, or business acquisition), all contributions to this repository are governed by the following **Contributor License Agreement (CLA)**.

---

## Contributor License Agreement (CLA)

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

## Code Style & Pull Request Process

1. **Clean Code & Modern APIs:** All contributions must target modern APIs under `org.daviante.kromium.api.*`.
2. **Automated Testing:** Ensure all unit and integration tests pass cleanly via `./gradlew test`.
3. **No Deprecations or Leaks:** Public APIs must maintain zero leaks of internal Chromium/JCEF native structures.
