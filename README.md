<p align="center">
  <img src="assets/logos/ror.png" width="170" alt="ROR Logo" />
  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
  <img src="assets/logos/phoenix.png" width="170" alt="Phoenix Logo" />
</p>

<h1 align="center">ROR &bull; Reincarnation of Revolutionary</h1>

<p align="center">
  <strong>The Ultimate Next-Generation Android Gaming Optimizer, Kernel Tuning Engine & Thermal Management Platform</strong>
</p>

<p align="center">
  <a href="https://github.com/revror/ROR/actions"><img src="https://img.shields.io/badge/CI-Passing-brightgreen?style=for-the-badge&logo=githubactions&logoColor=white" alt="Build Status" /></a>
  <a href="https://github.com/revror/ROR/releases"><img src="https://img.shields.io/badge/Release-v51.4-red?style=for-the-badge&logo=android&logoColor=white" alt="Version" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-crimson?style=for-the-badge&logo=gnu&logoColor=white" alt="License: GPL-3.0" /></a>
  <a href="https://developer.android.com/about/versions/14"><img src="https://img.shields.io/badge/Android-11%20to%2014%20(API%2030--34)-black?style=for-the-badge&logo=android" alt="Target API" /></a>
  <a href="https://shizuku.rikka.app/"><img src="https://img.shields.io/badge/Privilege-Root%20%2F%20Shizuku-darkred?style=for-the-badge&logo=linux" alt="Root / Shizuku" /></a>
  <a href="CONTRIBUTING.md"><img src="https://img.shields.io/badge/PRs-Welcome-black?style=for-the-badge&logo=git&logoColor=white" alt="PRs Welcome" /></a>
</p>

<p align="center">
  <em>Designed for competitive esports athletes, performance enthusiasts, and mobile power users who demand deterministic frame stability, zero touch latency, and intelligent thermal management without sacrificing hardware safety.</em>
</p>

---

## ⚡ Index
1. [Overview](#-overview)
2. [Key Highlights & Features](#-key-highlights--features)
3. [Live Screenshots Gallery](#-live-screenshots-gallery)
4. [Architecture & System Layers](#-architecture--system-layers)
5. [Smart Fast Charging & Low Heat Optimization](#-smart-fast-charging--low-heat-optimization)
6. [Game Space & In-Game Floating HUD](#-game-space--in-game-floating-hud)
7. [Touch Boost, Aim Boost & Gyro Calibration](#-touch-boost-aim-boost--gyro-calibration)
8. [Dual Engine Architecture (Regular vs Phoenix)](#-dual-engine-architecture)
9. [Hardware Compatibility Matrix](#-hardware-compatibility-matrix)
10. [Privilege Models (Root & Shizuku)](#-privilege-models)
11. [Source Architecture & Policy](#-source-architecture--platform-integration-policy)
12. [Safety Principles & Zero-Fake Policy](#-safety-principles--zero-fake-policy)
13. [Community & Contributing](#-community--contributing)
14. [License](#-license)

---

## 🚀 Overview

**ROR (Reincarnation of Revolutionary)** is an open-source, low-level Android performance and kernel management framework. Built specifically for high-refresh-rate gaming devices and intensive mobile computing, ROR delivers real hardware sysfs orchestration, dynamic frequency governor tuning, input queue optimization, and intelligent fast-charging thermal controls.

Unlike generic "ram booster" or "battery saver" apps that simply invoke `killBackgroundProcesses()`, ROR interfaces directly with the Linux kernel subsystem (`cpufreq`, `devfreq`, `kgsl-3d0`, block I/O schedulers, and power supply sysfs nodes) via **Root (`libsu`)** or **Shizuku privileged IPC**.

---

## 🌟 Key Highlights & Features

- **🎮 Dual-Engine Profile Architecture**: 6 standardized open-source profiles (Battery, Balance, Daily, Performance, Gaming, Extreme) + 8 Phoenix tournament-grade profiles.
- **⚡ Smart Fast Charging Suite**: 4 intelligent cooling modes (Eco, Balanced, Fast & Cool, Max Cool) balancing maximum safe wattage with minimal heat build-up.
- **🕹️ Game Space & Floating HUD Overlay**: Draggable real-time performance telemetry display with live FPS, CPU/GPU temperatures, memory cleaner, and brightness lock.
- **🎯 Crosshair Studio & Aim Boost**: 5 precision hardware-accelerated crosshair styles and non-linear touch curve stabilization for competitive shooters.
- **🚀 Touch Boost & Gyro Delay Fix**: Digitizer polling rate elevation and zero-batch IMU sensor polling for near-zero input lag.
- **🛡️ Hardware Safety & Baseline Restorations**: Non-destructive operation with automatic baseline state snapshots that restore factory settings upon exit or reboot.
- **📱 Quick Settings Tile Integration**: Instant profile switching and charging cooldown activation directly from the Android status shade.

---

## 📸 Live Screenshots Gallery

<p align="center">
  <img src="assets/screenshots/home.png" width="31%" alt="Dashboard" />
  &nbsp;
  <img src="assets/screenshots/game-space.png" width="31%" alt="Game Space" />
  &nbsp;
  <img src="assets/screenshots/kernel-tuning.png" width="31%" alt="Kernel Tuning" />
</p>
<p align="center">
  <img src="assets/screenshots/tools.png" width="31%" alt="Quick Tools" />
  &nbsp;
  <img src="assets/screenshots/charging-cooldown.png" width="31%" alt="Charging Cooldown" />
</p>

---

## 🏗️ Architecture & System Layers

ROR is built with a decoupled, high-performance architecture ensuring zero main-thread overhead:

```
┌────────────────────────────────────────────────────────┐
│                      UI Layer                          │
│   Dashboard  │  Game Space  │  Kernel Tuning  │ Tools  │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│                   Orchestration Layer                  │
│    GsmProfileManager       │   SmartChargingManager    │
│    GameBubbleService       │   TouchBoostManager       │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│               Privilege & Bridge Engine                │
│    libsu (Root Shell)      │   Shizuku IPC Bridge      │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│              Linux Kernel Sysfs Subsystem              │
│   /sys/devices/system/cpu/  │  /sys/class/kgsl/kgsl-3d0│
│   /sys/block/*/queue/       │  /sys/class/power_supply │
└────────────────────────────────────────────────────────┘
```

---

## 🔋 Smart Fast Charging & Low Heat Optimization

The **Smart Fast Charging** suite delivers maximum safe charging speed while actively curbing thermal throttling and battery wear:

| Mode | Target Objective | Behavior & Thermal Policy |
|---|---|---|
| **Eco Cooling** | Maximum Longevity & Minimal Heat | Downclocks background services; holds battery current to cooler operational thresholds. |
| **Balanced Cooling** | Daily Fast Charging | Balances factory fast charging rates with gentle workload mitigation. |
| **Fast & Cool** *(Recommended)* | Intelligent Fast Charging | Unlocks manufacturer fast charging (PD, QC, HyperCharge) while throttling unnecessary CPU/GPU spikes to prevent heat generation. |
| **Maximum Cooling** | Aggressive Thermal Suppression | Prioritizes minimal device heat for high-ambient-temperature environments. |

### Real-Time Electrical Telemetry
- **Battery Temperature**: High-precision milli-Celsius readings converted to °C.
- **Charging Current**: Instantaneous microampere measurement (`current_now`).
- **Charging Voltage**: Real-time microvolt rail telemetry (`voltage_now`).
- **Protocol Detection**: Automatically identifies USB Power Delivery (PD), Qualcomm Quick Charge (QC), and manufacturer-specific fast charging protocols without hardcoded device guessing.

---

## 🎯 Game Space & In-Game Floating HUD

Built specifically for high-intensity competitive gaming:
- **Floating HUD (`GameBubbleService`)**: Draggable transparent overlay monitoring real-time frame rates (via `Choreographer` callbacks), CPU/GPU clocks, and thermal zones.
- **Crosshair Studio**: Precision floating crosshairs (Dot, Cross, Circle-Dot, Chevron, T-Bar) with full RGB color picking, size, and offset calibration.
- **Barrage Chat (Bullet Comments)**: Renders incoming notifications as flying horizontal banner text across the top of the display so critical gaming controls are never blocked.
- **One-Tap Memory Purge**: Instantly frees unused page caches (`/proc/sys/vm/drop_caches`) to prevent memory thrashing during gameplay.

---

## ⚡ Touch Boost, Aim Boost & Gyro Calibration

- **Touch Boost**: Configures Linux digitizer input queues (`/sys/class/touch/` or `/sys/devices/virtual/input/input*/`) to sustain high sampling rates (up to 240Hz/360Hz/480Hz).
- **Aim Boost**: Custom non-linear touch acceleration curves tailored for first-person shooters (PUBG Mobile, Call of Duty: Mobile, Free Fire).
- **Gyro Delay Fix**: Configures Android's `SensorManager` to stream IMU orientation vectors at `SENSOR_DELAY_FASTEST` without batching delays.

---

## ⚙️ Dual Engine Architecture

ROR accommodates both standard open-source environments and advanced tournament hardware:

### 1. Regular Mode (Full Open-Source GPL-3.0)
- Profiles 0 through 5:
  - `Profile 0`: Powersave
  - `Profile 1`: Balance (Stock baseline)
  - `Profile 2`: Daily Smooth
  - `Profile 3`: Performance
  - `Profile 4`: Gaming
  - `Profile 5`: Extreme
- Universal compatibility across all rooted devices and rootless Shizuku environments.

### 2. Phoenix Engine (Proprietary Enthusiast Tier)
- Unlocked 8-tier kernel engine (Profiles 0 through 7).
- Low-level frequency locking (bypassing vendor DVFS throttling).
- Hardware bypass charging controls.
- Cryptographically signed device authorization.

---

## 📱 Hardware Compatibility Matrix

| Chipset Family | Tested SoCs | CPU Tuning | GPU Tuning | Charging Telemetry |
|---|---|:---:|:---:|:---:|
| **Qualcomm Snapdragon** | 8 Gen 1/2/3, 888, 870, 7+ Gen 2, 680 | ✅ Full | ✅ KGSL Adreno | ✅ Full PD / QC |
| **MediaTek Dimensity** | 9300, 9200, 8300, 8100, 7200, G99 | ✅ Full | ✅ Devfreq Mali | ✅ PumpExpress / PD |
| **Google Tensor** | Tensor G1, G2, G3 | ✅ Full | ✅ Devfreq Mali | ✅ Full USB-PD |
| **Samsung Exynos** | 2200, 2400, 1380, 1280 | ✅ Full | ✅ Xclipse / Mali | ✅ Adaptive Fast Charge |

---

## 🔐 Privilege Models

ROR provides two secure execution backends:

1. **Root Access (Recommended for full kernel controls)**:
   - Compatible with **Magisk v24+**, **KernelSU**, and **APatch**.
   - Grants full access to sysfs frequency scaling, I/O schedulers, and ZRAM tunables.
2. **Shizuku Privileged Bridge (Rootless)**:
   - Compatible with any device running the **Shizuku** service.
   - Provides elevated system commands, display refresh rate locking, and process controls without modifying system partitions.

---

## 🔐 Source Architecture & Platform Integration Policy

> [!IMPORTANT]
> REVROR is distributed as a modular research and component framework under **GPL-3.0-only**.
> Direct unauthenticated standalone APK generation is restricted in this public repository to protect system safety, prevent malicious kernel repackaging, and protect users from unauthorized rebranding.

### Modular Compilation & OEM Provisioning Requirements
Direct standalone binary assembly requires multi-tier cryptographic platform keys, OEM kernel symbol alignment, and vendor distribution entitlement tokens. 

Downstream maintainers and platform integrators must provide:
1. **Target Hardware ABI Definition**: Strict alignment for SoC architecture sysfs nodes (`/sys/devices/system/cpu`, `/sys/class/kgsl/kgsl-3d0`, `/proc/gpufreq`).
2. **Platform Entitlement Token**: Configuration of authorized `REVROR_TOOLCHAIN_TOKEN` and OEM profile entitlement signature hashes.
3. **Hardware Keystore & Certificate Binding**: Cryptographic key pairing matching target device kernel security domains.

Unauthenticated invocations of standard assembly tasks are gated by `:app:verifyRorCoreToolchainIntegrity` to protect kernel governor safety.

### 📦 Official Pre-Compiled & Verified Releases
For gamers, power users, and community testers wanting ready-to-install, cryptographically verified APK packages with full performance and thermal suite capabilities:

- **Official GitHub Releases**: [REVROR Releases](https://github.com/revws/ror/releases)
- **Developer & Project Support**: [revWhiteShadow on Ko-fi](https://ko-fi.com/revWS)
- **Community & Announcements**: [Telegram @godTspeed](https://t.me/godTspeed)

---

## 🛡️ Safety Principles & Zero-Fake Policy

1. **No Fake Boosts**: ROR does not fabricate simulated speedometers, fake FPS numbers, or artificial battery percentages. All metrics reflect genuine Linux `/sys/` and Android HAL readouts.
2. **Baseline Snapshotting**: Prior to modifying any governor or frequency, ROR captures an in-memory snapshot of your system's factory parameters.
3. **Graceful Restoration**: Selecting the default profile or uninstalling restores original hardware states.
4. **Thermal Protection First**: ROR respects hardware thermal trip points and will never allow device temperatures to exceed critical silicon safety limits.

---

## 🤝 Community & Contributing

We welcome pull requests, bug reports, and hardware suggestions!
- Read our [Contributing Guidelines](CONTRIBUTING.md) before submitting code.
- Report security issues following our [Security Policy](SECURITY.md).
- Follow the discussion in GitHub Issues.

---

## 📄 License

This open-source repository and all included application source code are licensed under the **GNU General Public License v3.0 (GPL-3.0-only)**. See the [LICENSE](LICENSE) file for complete details.

```
Copyright (C) 2026 ROR Developers
Licensed under the GNU General Public License, Version 3.0.
```
