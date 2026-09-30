## Description

Please provide a summary of the changes proposed in this Pull Request, including the motivation and context.

- Resolves: #(issue number)

---

## Type of Change

- [ ] `feat`: A new feature or capability
- [ ] `fix`: A bug fix
- [ ] `docs`: Documentation updates or corrections
- [ ] `perf`: Performance enhancement (OSR rendering, buffer swap, coroutine throughput)
- [ ] `refactor`: Code reorganization with no functional or API changes
- [ ] `test`: Added or updated unit/integration tests
- [ ] `build` / `ci`: Build configuration, dependencies, or GitHub Actions workflows
- [ ] `chore`: Housekeeping, formatting, or repository maintenance

---

## Affected Modules

- [ ] `kromium-api`
- [ ] `kromium-core`
- [ ] `kromium-provider-jcef`
- [ ] `kromium-compose`
- [ ] `kromium-swing`
- [ ] `kromium-javafx`
- [ ] `kromium-swt`
- [ ] `kromium-awt`
- [ ] `samples`
- [ ] `docs` / `.github`

---

## Pre-submission Checklist

- [ ] My code adheres to the project's coding standards and idiomatic Kotlin conventions.
- [ ] **Zero Native Leaks:** No classes from `org.cef.*` are imported in `kromium-api` or UI modules.
- [ ] I have compiled the project successfully via `./gradlew assemble`.
- [ ] All tests pass cleanly via `./gradlew check` (or `./gradlew test`).
- [ ] I have added appropriate unit or integration tests for my changes (if applicable).
- [ ] I have updated relevant documentation in `docs/` or KDoc comments (if applicable).
- [ ] My commit messages follow the [Conventional Commits](https://www.conventionalcommits.org/) specification.
- [ ] I have read and agree to the [Contributor License Agreement (CLA)](../CONTRIBUTING.md#contributor-license-agreement-cla) in `CONTRIBUTING.md`.
