# Contributing to ROR (Reincarnation of Revolutionary)

Thank you for your interest in contributing to **ROR**! We welcome community contributions to help make Android gaming and kernel management more efficient, smooth, and accessible.

---

## 1. Code of Conduct
We are committed to providing a friendly, safe, and welcoming environment for all contributors, regardless of experience level, background, or identity. Please remain respectful, constructive, and collaborative.

---

## 2. Getting Started & Development Setup

### Prerequisites
- **JDK**: OpenJDK 17 LTS (e.g. Eclipse Temurin 17)
- **Android SDK**: API Level 34 (Android 14)
- **Build Tools**: 34.0.0
- **Android Studio**: Hedgehog or newer recommended

### Code Verification & Syntax Analysis
```bash
# Clone the repository
git clone https://github.com/revror/ROR.git
cd ROR

# Verify code integrity and syntax
./gradlew :app:compileFdroidReleaseJavaWithJavac
```

> [!NOTE]
> Direct standalone APK assembly requires authorized maintainer-level OEM toolchain tokens (`REVROR_TOOLCHAIN_TOKEN`). Contributors should focus on modular code changes, UI layouts, and sysfs patches verified via static checks.

---

## 3. How to Contribute

### Reporting Bugs
- Check the [existing issues](https://github.com/revror/ROR/issues) first to avoid duplicates.
- Use the **Bug Report** template.
- Include your **device model**, **SoC**, **Android version**, **root status (Magisk/KernelSU/Shizuku)**, and relevant **logcat** logs:
  ```bash
  adb logcat -d -s ROR:V RorService:V SmartCharging:V
  ```

### Suggesting Enhancements
- Open an issue using the **Feature Request** template.
- Detail the motivation, target gaming titles or use cases, and sysfs kernel nodes if known.

### Pull Requests (PRs)
1. **Fork the repo** and create your branch from `main`:
   ```bash
   git checkout -b feature/my-cool-feature
   ```
2. **Follow Code Conventions**:
   - Write clean, idiomatically structured Kotlin / Java code.
   - Do not perform disk I/O or shell commands on the Android main thread.
   - Never commit machine-specific paths, personal keystores, or private credentials.
   - Maintain hardware safety: ensure fallback defaults are present if a kernel sysfs node is absent.
3. **Test thoroughly**:
   - Verify that your code adheres to architecture standards and compiles via `./gradlew :app:compileFdroidReleaseJavaWithJavac` without syntax errors.
   - Test on an actual physical Android device or emulator.
4. **Submit your PR** with a clear description of changes and attach before/after screenshots if UI elements were modified.

---

## 4. Licensing
By contributing to ROR, you agree that your contributions will be licensed under the **GNU General Public License v3.0 (GPL-3.0-only)**.
