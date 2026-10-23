# Changelog

All notable changes to the **ROR (Reincarnation of Revolutionary)** open-source project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [51.4.0] - 2026-10-23

### Added
- **Smart Fast Charging & Low Heat Optimization Suite**:
  - **Four Specialized Charging Modes**: Eco Cooling, Balanced Cooling, Fast & Cool, and Maximum Cooling.
  - **Fast & Cool Mode**: Maintains manufacturer-supported fast charging while intelligently throttling unnecessary background CPU/GPU spikes to curb heat build-up.
  - **Intelligent Fast Charging Detection**: Detects USB Power Delivery (PD), Qualcomm Quick Charge (QC), and vendor-specific fast charging protocols from hardware sysfs nodes without brand guessing.
  - **Real-Time Charging Dashboard**: Live monitoring for battery temperature (°C), charging current (mA), voltage (mV), thermal status, and cooling optimization delta.
  - **Auto Fast & Cool Daemon**: Automated foreground monitoring service tracking charger connection and disconnection with low memory overhead.
  - **Quick Settings Tile**: `ChargingCooldownTileService` allowing instantaneous one-tap mode switching directly from Android notification shade.
- **Dual Source Code Distribution**:
  - Independent Private Developer Source with 12 AI context reference guides.
  - Clean Public GitHub Open-Source distribution under GNU General Public License v3.0 (`GPL-3.0-only`).
  - Automated public export and privacy auditing pipeline (`export_public_source.ps1`).

### Fixed
- Fixed profile mapping discrepancy in Game Space ensuring Regular Mode profiles apply accurate governor clamps.
- Resolved Shizuku execution thread blocking on high-frequency UI updates.

---

## [51.0.0] - 2026-09-15

### Added
- **Game Space & Floating Gaming HUD**:
  - Floating bubble overlay with real-time FPS counter, CPU/GPU temperature, and clock frequency display.
  - Quick RAM cleaner and brightness lock.
- **Crosshair Studio**:
  - Draggable precision floating crosshair with 5 styles (Dot, Cross, Circle, Chevron, T-Bar).
- **Touch & Motion Enhancements**:
  - Touch Boost digitizer polling overclock.
  - Aim Boost non-linear touch curve stabilization.
  - Gyro Delay Fix with zero-batching IMU sensor event streaming.
- **Dual Engine Architecture**:
  - Standard Regular Profiles (0–5: Battery, Balance, Daily, Performance, Gaming, Extreme).
  - High-tier Phoenix Profiles (0–7) with frequency lock support.
