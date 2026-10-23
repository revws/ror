# Contributing to ROR (Reincarnation of Revolutionary)

First off, thank you for considering contributing to ROR! Open-source contributions from power users, Android developers, and esports enthusiasts help make ROR better for everyone.

---

## Code of Conduct

We are committed to providing a welcoming, inspiring, and harassment-free community for everyone. Be respectful and constructive in all discussions, pull requests, and issue reports.

---

## How Can I Contribute?

### 1. Reporting Bugs
- Check the [Issues tab](https://github.com/revws/rorexp/issues) to ensure the bug hasn't already been reported.
- Use our **Bug Report Template** (`.github/ISSUE_TEMPLATE/bug_report.md`).
- Provide your device model, Android version, kernel version, root status (Magisk, KernelSU, APatch, or non-root/Shizuku), and step-by-step reproduction instructions.
- Attach `adb logcat` output whenever relevant.

### 2. Suggesting Features
- Submit feature suggestions using the **Feature Request Template** (`.github/ISSUE_TEMPLATE/feature_request.md`).
- Detail the problem the feature addresses and why it benefits mobile gamers or kernel tuners.

### 3. Submitting Pull Requests
- Fork the repository and create your feature branch from `main`:
  ```bash
  git checkout -b feature/your-feature-name
  ```
- Follow Android standard Java/Kotlin code conventions.
- Maintain documentation integrity and do not remove existing docstrings or copyright headers.
- Test your changes thoroughly on a physical device or emulator before opening a PR.
- Target PRs to the `main` branch with a descriptive explanation of changes.

---

## Development Guidelines

- **Target SDK**: Android 14 (API level 34)
- **Minimum SDK**: Android 10 (API level 29)
- **Architecture**: Keep UI presentation decoupled from Linux kernel sysfs operations.
- **Safety Safeguards**: Always validate sysfs nodes before writing; never hardcode paths without fallback guards.

---

*Thank you for helping push Android mobile gaming performance forward!*
