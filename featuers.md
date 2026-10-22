# ROR (Reincarnation of Revolutionary) - Comprehensive Features Guide

> **Version:** 50.3 (Continuous Gaming & Kernel Edition)  
> **Package:** `com.revror.revws`  
> **Platform:** Android 11+ (Target SDK 34, Android 14)

---

## 1. System Architecture & Elevated Execution
- **Dual-Tier Elevation Engine**:
  - **Full Root Mode**: Direct su shell bridge utilizing modern `libsu` for Magisk, KernelSU, and APatch.
  - **Rootless Shizuku Mode**: Deep integration with the Shizuku IPC framework (`dev.rikka.shizuku:api`), executing system-level operations on unrooted devices.
  - **Automated Silent Permissions**: Automatically provisions `DUMP`, `PACKAGE_USAGE_STATS`, `SYSTEM_ALERT_WINDOW`, `WRITE_SECURE_SETTINGS`, and `ACCESS_NOTIFICATION_POLICY` silently on boot without interrupting the user.
- **Hardware Acceleration**: Full hardware rendering acceleration across all consoles, charts, and custom Canvas views (`android:hardwareAccelerated="true"`).

---

## 2. Dashboard & Performance Control Hub
- **4-Column Performance Profiles Grid**:
  - **Daily Use**: Balanced governor scaling, balanced energy consumption, ultra-smooth scrolling.
  - **Balanced**: Dynamic multi-tasking profile with adaptive frequency curves.
  - **Battery**: Aggressive downclocking, wake-lock throttling, and deep sleep battery preservation.
  - **Gaming (Cyber Crimson Ruby)**: Unlocked peak frequencies, 240Hz touch boost, max GPU governor, and live continuous breathing scale animation.
  - **Secondary Expandable Profiles**:
    - *CoolDown*: Rapid thermal dissipating governor settings.
    - *Turbo*: Maximum multi-core burst overclocking.
    - *Esports*: Ultra-low latency scheduling with isolated CPU cores.
    - *Diablo Hellfire*: Unthrottled sustained peak power profile.
- **Hardware Telemetry Overview**:
  - 16-point realtime hardware monitoring grid with 34dp circular pie gauges and realtime sparkline graphs.
  - Realtime CPU cluster load percentage and individual core frequencies.
  - Live RAM usage, cached buffers, active/inactive pages, and ZRAM compression ratio.
  - Battery capacity %, charging state, live temperature, and voltage telemetry.
  - Storage internal flash usage and disk I/O metrics.
  - Live network throughput meters (realtime KB/s upload & download).
- **Quick System Tools**:
  - Application Freezer & Package Manager.
  - Instant One-Tap Memory Cleaner (`drop_caches` + inactive page trim).
  - Floating CPU Overlay Widget with live frequencies and thermals.
  - Display refresh rate overdrive controls.

---

## 3. Game Space & Console Lobby
- **Dual Console Orientations**:
  - **Landscape PS5 Console Deck**: Designed for widescreen handheld gaming with gaming telemetry sidebar, center hero stage, and Energy Core quick controls.
  - **Portrait Handheld Mobile Deck**: Compact vertical layout with spotlight game card, horizontal game shelf, and full Energy Core panel.
- **Multiple Visual Layouts**:
  - **Cyber Laser Bento Grid**: High-tech interactive bento grid featuring live metrics, speed meter badges, and quick game launch tiles.
  - **PS5 Console Trio**: Television/console-style carousel shelf with focused game banner and hero telemetry.
  - **Super Base / Energy Core**: Full-screen console command center with all hardware assist tools and tuning sliders.
- **24 Live 60 FPS Animated Banners (Canvas Vector Engine)**:
  - High-performance, zero per-frame allocation rendering with automatic VSYNC synchronization and lifecycle sleep/resume on `onPause`/`onResume`.
  - **12 Core Cyberpunk & Sci-Fi Presets**:
    1. *REVROR Cyber Laser*: Rolling 3D perspective horizon grid, scanning crimson & cyan laser beams, and floating plasma nodes.
    2. *Neon Tokyo Synthwave*: Glowing retro synthwave sun with animated black scanline slices and scrolling wireframe floor.
    3. *Mecha Core*: Counter-rotating concentric HUD target rings, dynamic 360° radar sweep sector, and tactical crosshair marks.
    4. *Diablo Hellfire*: Molten obsidian core with 45+ animated rising embers and heat-distortion pulses.
    5. *Matrix Digital Rain*: 36 cascading columns of falling cyber glyphs with white/cyan leading heads and emerald trails.
    6. *Sakura Blossom & Cyber Samurai*: Swirling cherry blossom petals tumbling in the wind, neon crescent moon, and periodic katana slash gleams.
    7. *Solar Flare*: Undulating solar corona rays and orbiting golden plasma motes.
    8. *Quantum Nebula*: Deep cosmic dust swirling with 45 pulsating stars and shifting chromatic gas clouds.
    9. *Arctic Frost*: Sinusoidal aurora borealis ribbons shifting in cyan/green with falling crystal frost.
    10. *Apex Hazard / Neon Drift*: High-speed warp light streaks radiating outward from the center vanishing point.
    11. *Eva Unit-01*: Expanding concentric AT-field geometric octagons and bioluminescent mana spirals.
    12. *Stealth Carbon / RGB Chroma Wave*: Carbon fiber weave illuminated by a 45-degree full-spectrum RGB Chroma sweep.
  - **12 Anime Characters & Epic Gaming Themes**:
    13. *Anime Cyber Girl (Aika)*: Futuristic anime heroine silhouette with glowing electric cyan dual twintails swaying in the cyber wind, animated blinking neon eyes, and floating holographic UI data rings.
    14. *Shadow Monarch (Sung Jin-woo)*: Dark majestic anime shadow sovereign with billowing dark purple flame aura, glowing piercing cyan-violet eyes, rising shadow soldiers' mist, and phantom daggers.
    15. *Demon Nichirin Flame (Rengoku)*: Blazing anime demon slayer warrior with swirling fiery Nichirin katana flame arc, roaring fire dragon aura, and swirling embers.
    16. *Gojo Satoru (Limitless Void)*: Anime sorcerer with blindfold shifting, glowing celestial blue Six Eyes aura, radiating divergent infinity spheres, and pulsing blue-red-purple hollow spatial fractures.
    17. *Cyber Samurai Waifu*: Cyberpunk anime blade master with long flowing sakura pink hair, glowing neon katana with blade gleams, fluttering blossom petals, and rain droplets.
    18. *Dragon Ball Ultra Instinct*: Celestial god Ki aura undulating around an anime warrior silhouette, silver floating particles, and shockwave energy ripples.
    19. *Chibi Neko Gamer*: Cute anime catgirl gamer with animated twitching cat ears, bouncing gaming headset, pulsing heart glow, and floating 8-bit star sparkles.
    20. *Berserk Dragonslayer*: Dark fantasy anime swordsman silhouette against an eclipse blood moon, glowing crimson Brand of Sacrifice, and flying dark ravens.
    21. *Persona Phantom Thief*: Sleek anime phantom hero in crimson trench coat with fluttering cape, glowing yellow mask eyes, swirling blue summoning flames, and star card bursts.
    22. *Gundam Strike Freedom*: Legendary mobile suit with expanding glowing golden beam wings, pulsating plasma thrusters, and lock-on targeting reticles.
    23. *Cyber Void Valkyrie*: Angelic cybernetic anime heroine with glowing holographic energy wings flapping slowly, floating halo, and falling feather motes.
    24. *Arcane Jinx Supernova*: Wild anime rebel with glowing neon graffiti swirls, electric pink shark missile sparks, and pulsing energetic neon spray.
  - **Custom Gallery Wallpapers (ID ≥ 100)**:
    - High-end cinematic Ken Burns slow pan and zoom motion (1.00x to 1.07x) with ambient floating dust motes.
- **Game Space Accent Colors**:
  - 8 One-Tap Accents: Crimson, Cyan, Violet, Razer Green, Gold, Ice Blue, Blaze Orange, White.
- **In-Game Tactical HUD Tools**:
  - **Tactical Crosshair Overlay**: 5 customizable crosshair styles (Classic, Precision Dot, Tactical, Neon X, Target Ring), 8 neon colors, and adjustable pixel sizing.
  - **Floating Game Tools**: In-game collapsible sidebar offering real-time FPS, temperature, RAM purge, and profile switching without exiting the game.
  - **Instant RAM "CLEAN"**: Replaces traditional fan icon with dedicated kernel cache drop (`drop_caches`), freeing memory and boosting frame pacing instantly with haptic feedback.
  - **RGB Chroma Wave Switch**: Toggles between active Chroma spectrum wave illumination on glowing UI elements and battery-conserving Crimson Stealth mode.
  - **Display Refresh Overdrive**: Direct one-tap cycling between 60Hz, 90Hz, 120Hz, and 144Hz.
  - **Touch Boost 240Hz**: Maximizes touch screen input polling frequency and reduces touch-to-pixel response latency.
  - **Barrage Game Alerts**: 24 customizable pop-up notification banner styles tailored for distraction-free gaming.
  - **Anti-Disturb & Call Blocker**: Blocks intrusive incoming calls and notifications while in-game.

---

## 4. Typography & Header Styles (BannerTextStyleRegistry)
- 38+ Handcrafted Typography Themes combining custom weights (100–900), italics, letter spacing, drop shadows, and neon badge pills.
- Dedicated presets for Anime Characters, Cyberpunk, Synthwave, Mecha, Diablo, and Esports themes.

---

## 5. Low-Level Kernel & Hardware Tuning
- **CPU & Cluster Management**:
  - Independent frequency scaling and governor selection for Little, Mid, and Prime clusters.
  - Supported governors: `schedutil`, `performance`, `interactive`, `ondemand`, `conservative`, `powersave`.
  - Advanced Hotplugging engines: AutoSMP, BluPlug, IntelliPlug, ThunderPlug, MSM Hotplug, and CoreCtl.
  - CPU Boost driver parameters (input boost frequencies, boost duration ms).
- **GPU Control**:
  - GPU maximum and minimum frequency clamping.
  - GPU governors and power levels.
  - Adreno Idler and Simple GPU algorithm tuning.
- **Thermal & Safety Controls**:
  - Thermal throttling trip points inspection and mitigation.
  - High-temperature core shutdown prevention.
- **Virtual Memory & Storage I/O**:
  - I/O Schedulers: `cfq`, `bfq`, `maple`, `noop`, `deadline`, `kyber`, `zen`.
  - Read-ahead storage cache optimization (128 KB to 4096 KB).
  - Virtual memory parameters: `swappiness`, `dirty_ratio`, `dirty_background_ratio`, `vfs_cache_pressure`.
  - ZRAM compression engines (`lz4`, `zstd`, `lzo`) and disk sizing.
  - Kernel Samepage Merging (KSM) controls.
- **Screen & Display Calibration**:
  - KLapse night-mode ambient shift and color temperature correction.
  - RGB gamma calibration and color table control.
  - Contrast, saturation, and hue fine-tuning.
- **Audio & Battery Peripherals**:
  - High-performance audio digital gain booster (+6dB).
  - Battery charge limiter thresholds to prolong battery lifespan.
  - Power suspend modes: LCD hooks, autosleep, and userspace triggers.

---

## 6. Adsterra Ads Engine with DNS-Over-HTTPS (DoH) Bypass
- **High-Yield 728x90 Leaderboard Integration**: Configured with active key `b42fcfe592ba30a160cff1a209bed5f7` on `venisonglum.com`.
- **Bulletproof Ad-Blocker DNS Bypass**:
  - Completely immune to device-level or router-level ad-blocking DNS (e.g., AdGuard DNS `dns.adguard.com`, NextDNS, Pi-hole, local hosts files).
  - Automatically intercepts WebView network requests in `WebViewClient.shouldInterceptRequest()` and resolves edge IPs using direct IP Cloudflare DNS-over-HTTPS (`https://1.1.1.1/dns-query`).
- **Responsive Auto-Scaling Engine**:
  - Implements dynamic CSS matrix transform scaling (`Math.min(width / 728.0, height / 90.0)`).
  - Guarantees the full 728x90 leaderboard iframe fits cleanly onto any Android screen size or container height without clipping or horizontal overflow.
- **Standard Browser Compliance**: Full DOM Storage (`localStorage`/`sessionStorage`) and Chrome Mobile User-Agent provisioning ensuring 100% ad delivery and interaction.
