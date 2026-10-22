# REVROR Features v2

This document explains what REVROR can do. It explains features only. It does not discuss colors, spacing, animations, or other design choices.

## Read this first

REVROR is an Android performance, gaming, monitoring, and kernel-control app. Some features work on a normal phone. Some need extra control:

- **Normal access** means the feature can work with ordinary Android permission.
- **Shizuku access** lets REVROR perform some Android system actions without full root.
- **Root access** gives REVROR permission to change protected kernel and system values.
- **Device support required** means the control works only when the phone's kernel or Android build provides that control.

Simple example: a switch cannot change a CPU governor if the phone's kernel does not expose a CPU-governor control. The app can offer the feature, but it cannot invent missing hardware support.

## 1. First start and access

### 1.1 License agreement

The first-start agreement explains the rules and risks. The user must accept it before entering the app.

### 1.2 Root detection

REVROR checks whether root is available. Root is used for protected operations such as kernel tuning, cache dropping, memory compaction, and deep performance profiles.

### 1.3 Shizuku support

REVROR can use Shizuku for supported Android-level actions. This is useful when full root is unavailable, although Shizuku cannot replace every root operation.

### 1.4 Permission guidance

The app directs the user to the Android settings pages needed for overlays, notifications, usage access, Wi-Fi, screen recording, and other protected functions.

### 1.5 Brand-integrity protection

REVROR checks important application identity values, required core-class signatures, Quick Settings components, and the approved APK signing certificate during startup. If a protected value is changed or removed, the app stops instead of continuing in an altered state.

### 1.6 Protected-package safety

Critical Android packages, the active keyboard, REVROR itself, and other essential services are protected from dangerous cleanup actions.

## 2. Main dashboard

### 2.1 Device summary

The dashboard shows the device name, Android version, and kernel information so the user knows which phone and software are being controlled.

### 2.2 Battery information

The app shows battery level, charging state, health, temperature, and voltage when Android provides those readings.

### 2.3 CPU information

REVROR shows CPU load, current frequency, available cores, and other processor information. This tells the user how busy the processor is.

### 2.4 Memory information

The app shows used RAM, free RAM, total RAM, and swap information. RAM is the short-term working space used by Android and running apps.

### 2.5 Storage information

The app shows used and available device storage. Storage is the longer-term space where apps and files live.

### 2.6 Temperature monitoring

REVROR reads available thermal sensors and shows temperature information. Missing sensors are skipped because different phones expose different readings.

### 2.7 Screen and sleep statistics

The app can show screen-on time, screen-off time, awake time, deep-sleep time, and the balance between awake and sleeping states.

### 2.8 Running-app information

REVROR can show app and process information that Android allows it to read. Extra access may be needed on newer Android versions.

### 2.9 Live graphs

Live graphs turn changing CPU, memory, and other readings into an easy history instead of showing only one number.

### 2.10 Expanded monitoring

The overview can be expanded to reveal more readings. This keeps the basic summary simple while still providing detailed information when requested.

## 3. Performance profiles

### 3.1 Daily Use profile

Daily Use is the general everyday mode. It aims for normal responsiveness without forcing the phone into the strongest performance or saving settings.

### 3.2 Balance profile

Balance tries to keep performance and battery use in the middle. It is useful when the user wants neither maximum speed nor maximum battery saving.

### 3.3 Battery profile

Battery reduces supported performance settings to help the phone use less power. The exact changes depend on available device controls.

### 3.4 CoolDown profile

CoolDown reduces supported performance settings, compacts memory, drops safe caches, and closes nonessential background apps. It is meant to help the phone settle down when it is hot or overloaded.

### 3.5 Gaming profile

Gaming applies the app's strongest supported game-performance settings. It can be selected before launching a game or during a game session.

### 3.6 Custom profiles

The user can save a personal combination of supported settings as a custom profile and apply it again later.

### 3.7 Profile list

Saved profiles are listed in one place so they can be opened, applied, edited, imported, exported, or removed.

### 3.8 Scheduled profiles

A profile can be connected to a schedule. This lets REVROR apply a chosen setup at the configured time.

### 3.9 Apply profile at boot

REVROR can reapply selected supported settings after the phone starts. This is useful because many kernel values return to their defaults after a reboot.

### 3.10 Profile widget

The home-screen widget provides profile access without opening the full app.

### 3.11 Profile Quick Settings tile

The REVROR Profile tile cycles through Daily Use, Balance, Battery, CoolDown, and Gaming directly from the notification shade. Its icon and label change with the selected profile.

### 3.12 Gaming Quick Settings tile

The REVROR Gaming tile switches Gaming mode on or returns to Daily Use without opening the app.

### 3.13 CoolDown Quick Settings tile

The REVROR CoolDown tile immediately applies the protected CoolDown profile and cleanup behavior.

### 3.14 HUD Quick Settings tile

The REVROR HUD tile starts or stops the floating live-performance overlay when Android overlay permission is available.

### 3.15 Night Quick Settings tile

The REVROR Night tile cycles through the kernel-supported time, brightness, and off modes.

### 3.16 Script Quick Settings tile

The REVROR Script tile runs the configured script when root and the selected script are available.

### 3.17 Tasker profile integration

Tasker-compatible profile support lets an automation task request a REVROR profile.

## 4. Quick performance tools

### 4.1 Smart optimizer

The optimizer reads available device conditions and applies a suitable supported optimization. It is a shortcut for users who do not want to tune every value manually.

### 4.2 Memory compaction

Memory compaction asks Android to place used memory pages together more efficiently. Root is normally required.

### 4.3 Filesystem cache drop

Cache drop tells Linux to release safe filesystem caches. It does not mean deleting personal files. Root is required.

### 4.4 Background cleanup

Background cleanup closes nonessential background apps while protecting the game, keyboard, REVROR, and critical Android services.

### 4.5 Junk and cache cleaner

The cleaner removes supported temporary and cache data. It does not intentionally remove the user's personal documents.

### 4.6 Scheduled cleanup

Cleanup can be run automatically on the app's schedule instead of requiring the user to start it manually every time.

### 4.7 Force Doze

Force Doze asks Android to enter a stronger idle state so background work is reduced while the device is not being used. Privileged access may be required.

### 4.8 Data Saver

Data Saver opens or controls Android's supported data-saving behavior so background apps use less mobile data.

### 4.9 Network boost

Network boost applies the supported low-latency network settings available on the device. It cannot make a weak internet connection physically faster.

### 4.10 Refresh-rate control

The user can request a supported screen refresh rate. The phone still decides what rates its display can actually use.

### 4.11 FPS unlocker

The FPS unlocker applies supported device and game properties intended to expose higher frame-rate choices. Presets are available, and the original values can be restored.

### 4.12 DNS changer

The DNS tool changes Android Private DNS settings. DNS is the service that turns a name such as `example.com` into a network address.

### 4.13 Built-in DNS choices

The included choices cover Cloudflare, Google, Quad9, AdGuard, Cisco OpenDNS, CleanBrowsing, AliDNS, family filtering, malware filtering, and ad-blocking DNS services. Each entry shows the server name and address information.

### 4.14 Battery scheduler

Battery-related actions can be scheduled so a supported profile or power behavior is used at the chosen time.

### 4.15 Thermal status

The thermal tool shows available heat information and supported thermal controls. Unsupported thermal drivers are left alone.

### 4.16 Boot settings

Boot settings decide which saved actions should run after Android starts.

### 4.17 Reboot manager

The reboot manager provides supported restart targets, such as a normal restart or special boot mode. Root is required for protected restart commands.

## 5. App Manager

### 5.1 Installed-app list

The App Manager lists installed applications with their icon, name, package name, and current state.

### 5.2 App search

Search narrows the list by app or package name.

### 5.3 User/system filters

The list can show all apps, user-installed apps, or system apps.

### 5.4 Launch app

An app can be opened directly from its App Manager entry.

### 5.5 Force-stop app

REVROR can stop a selected nonprotected app when the available privilege level allows it.

### 5.6 Enable or disable app

A selected nonprotected package can be enabled or disabled with privileged access.

### 5.7 Clear app data

The user can erase a selected nonprotected app's data when privileged access allows it. This resets that app, so it is an intentionally powerful action.

### 5.8 Safety exclusions

The App Manager blocks destructive actions against protected packages to reduce the chance of breaking Android or locking the user out of the phone.

## 6. Game Space library and launcher

### 6.1 Game library

Game Space keeps selected games in one launcher so the user does not need to hunt through the normal app drawer.

### 6.2 Add games

The Add Games screen lists launchable apps. The user can add games or remove them from the Game Space library.

### 6.3 Game recognition

REVROR uses Android app categories and package/name clues to identify likely games. The user can still choose apps manually.

### 6.4 Selected-game launch

Pressing Play starts the selected game and begins the REVROR game session for that package.

### 6.5 Game information snapshot

Game Space can show available information about the selected game, such as package, storage, session, and play-related details.

### 6.6 Session tracking

REVROR remembers the active game and tracks the current game session so game-only tools know when they are allowed to appear.

### 6.7 Switchable Game Space layouts

The available Game Space layouts can be changed from the layout control. Changing the layout does not remove the game library or its settings.

### 6.8 Portrait and landscape support

Game Space can work in supported portrait and landscape orientations.

### 6.9 Game profile selection

The user can choose the performance profile that should be used for a game session.

## 7. Game Space Center

Every item below is a real saved option. A switch means the feature can be turned on or off.

### 7.1 Peripheral connection mode

This controls whether Game Space prepares its peripheral-assistance behavior for game sessions.

### 7.2 Controller auto-detection

This lets REVROR react when Android reports a game controller.

### 7.3 Keyboard and mouse mapping option

This stores whether keyboard/mouse game assistance should be used where the device supports it.

### 7.4 Bluetooth priority

This requests the app's supported Bluetooth-first peripheral behavior during a game session.

### 7.5 Peripheral connection pop-up

This controls whether a connection message is shown when a supported peripheral is found.

### 7.6 Restore display settings after a game

When enabled, REVROR restores supported display values that were changed for the game.

### 7.7 Restore notification mode after a game

When enabled, the previous notification behavior is restored when the session ends.

### 7.8 Close overlays after a game

This removes game overlays when the active selected game is no longer in front.

### 7.9 Clear temporary cache after a game

This runs the supported temporary-cache cleanup at the end of a session.

### 7.10 Session summary

This controls whether REVROR shows the available summary when a game session finishes.

### 7.11 Atmosphere light options

The atmosphere switches control supported game-session light feedback, profile color feedback, notification pulse, and charging glow.

### 7.12 Low-battery effect reduction

When the battery falls below 20 percent, this option reduces nonessential atmosphere effects to avoid wasting power.

### 7.13 Game Space wallpaper

The user can choose a preset or personal image for Game Space. This is a functional personalization setting, not a performance control.

### 7.14 Accent choice

The user can save the preferred Game Space accent choice.

### 7.15 Banner image and blur

Game Space can use a preset or custom banner, and the user can control its blur amount or reset it.

### 7.16 Banner text style

The banner title supports multiple selectable text presets. Selecting one changes how the saved Game Space title is presented.

### 7.17 Branding watermark

The watermark can be enabled or disabled for the Game Space session.

### 7.18 Watermark session time

This adds the current session time to the watermark when the watermark is enabled.

### 7.19 Watermark active profile

This adds the selected performance profile to the watermark.

### 7.20 Compact watermark

This uses the smaller watermark form.

### 7.21 Keep screen awake

This prevents the display from sleeping while Game Space is active.

### 7.22 Opening animation option

This turns the Game Space opening transition on or off.

### 7.23 Intelligent highlight collection option

This saves whether the available highlight-collection behavior should be used during games.

### 7.24 Hide game icons on Home option

This stores whether games managed by Game Space should be hidden from the normal home experience where the launcher and Android build support that action.

## 8. Floating Game Tools

### 8.1 Game-only floating bubble

The floating bubble appears for the selected active game. It is hidden outside that game so REVROR tools do not cover the rest of the phone.

### 8.2 Drag and edge dock

The bubble can be moved and docked near an edge.

### 8.3 Collapsed and expanded states

The tool can stay as a small orb or open into the full control panel.

### 8.4 Live game metrics

The panel shows available live readings such as CPU, GPU, memory, temperature, network, frame, and session information.

### 8.5 In-game profile switcher

The user can change REVROR profiles without leaving the active game.

### 8.6 One-touch cleanup

This runs protected background cleanup while keeping the active game alive.

### 8.7 Network latency boost

This applies the supported network optimization directly from the game panel.

### 8.8 RAM and graphics-memory cleanup

The game panel can request supported memory cleanup. Android and the graphics driver decide which memory can safely be released.

### 8.9 Quick screenshot

The panel offers a shortcut to capture the game screen using Android's allowed capture method.

### 8.10 Speedrun stopwatch

The floating stopwatch can start, pause, reset, move, and close. It stays visible over the game when overlay permission is granted.

### 8.11 Floating calculator

The calculator provides basic calculation without requiring the user to leave the game.

### 8.12 Audio boost

The audio boost requests an extra 6 dB for the active audio path where Android permits it. The actual result depends on the device audio system.

### 8.13 Touch sampling boost

The user can request normal touch behavior or the supported high-sampling target. A phone cannot use a sampling rate its touch hardware does not support.

### 8.14 Peak refresh request

This requests the display's supported peak refresh behavior for the active game.

### 8.15 Thermal guard

Thermal Guard watches battery temperature. At a dangerous threshold it warns the user and can switch to CoolDown behavior.

### 8.16 Voice filters

The available voice presets are Off, Tactical Radio, Cyber Mecha, Deep Commander, Helium Scout, and Arena Echo. Android audio support determines the final result.

### 8.17 Floating performance HUD

The HUD shows supported CPU, GPU, frame, temperature, and memory readings above the game.

### 8.18 Sponsored panel

The floating window includes a default 728-by-90 web advertisement area and a visible sponsored fallback when the remote advertisement is unavailable.

## 9. Crosshair

### 9.1 One combined crosshair feature

All crosshair controls are kept together as one feature instead of being split into duplicate tools.

### 9.2 Crosshair on/off

The crosshair can be enabled or disabled for the game.

### 9.3 Crosshair styles

The built-in choices are Classic, Precision Dot, Tactical, Neon X, and Target Ring.

### 9.4 Crosshair color

The user can choose the crosshair color.

### 9.5 Crosshair size

The user can change the crosshair size.

### 9.6 Game-only protection

The crosshair is shown only over the active selected game and is removed when the game stops being the foreground app.

## 10. Hunting and screen filters

### 10.1 Off

Off removes the REVROR game filter and restores normal screen color.

### 10.2 Hunter

Hunter inverts the game colors. It is applied through the game screen filter, not to the whole Android system.

### 10.3 Night Vision

Night Vision applies the included night-view color treatment to the game.

### 10.4 Eagle Eye

Eagle Eye applies its brighter target-visibility treatment.

### 10.5 Ultra-Clear

Ultra-Clear applies its clarity-oriented game overlay.

### 10.6 Pure

Pure applies the included neutral game filter.

### 10.7 Cyberpunk

Cyberpunk applies its strong cyan-and-magenta style treatment.

### 10.8 Movie

Movie applies the included cinematic color treatment.

### 10.9 Black-and-white Sketch

Sketch removes color and applies the black-and-white sketch treatment. It does not leave colored tint in the result.

### 10.10 Film

Film applies the included film-style treatment.

### 10.11 Crayon

Crayon applies the included crayon-style color treatment.

### 10.12 Oil Painting

Oil Painting applies the included painting-style treatment.

### 10.13 Filter cleanup

Filters are reset when the selected game is no longer active, when the floating service closes, or when Off is selected. This stops a game filter from changing the rest of the phone.

## 11. Game interruption controls

### 11.1 Anti-disturb

Anti-disturb reduces interruptions during the game. REVROR restores the previous supported notification behavior after the session.

### 11.2 Barrage notifications

Barrage turns allowed notifications into short messages that travel over the game instead of opening a full interruption.

### 11.3 Notification access

Barrage needs Android notification-listener access. Without that permission, REVROR cannot read incoming notifications to display them.

### 11.4 Barrage style selection

The available choices are Minimal Stealth, Anime Mecha, Cyber Glass, Cocktail Party, REVROR Turbo, RGB Chroma Wave, Retro 8-Bit Arcade, Cyberpunk 2077, Dark Souls Bonfire, Valorant Radiant, Apex Champion, League Hextech, Doom Inferno, Sakura Blossom, Chibi Kawaii, Shonen Flame, Manga Monochrome, Evangelion Unit-01, Demon Slayer Nichirin, Ghost In The Shell, Jujutsu Cursed, Magical Girl, Persona Phantom, and Shadow Monarch.

### 11.5 Play-time reminders

The user can choose 15-, 30-, or 60-minute reminders. The reminder tells the user how long the game has been running.

## 12. Mini windows

### 12.1 Favorite mini apps

The user can choose installed apps that should appear in the mini-window list.

### 12.2 In-game app launch

A favorite app can be opened from the floating game panel without first returning to the launcher.

### 12.3 Freeform window request

REVROR requests Android's freeform-window behavior for a mini app. The device ROM must support freeform windows for this to work fully.

### 12.4 Active mini-app tracking

The floating service remembers which mini app is active so it can manage the current mini-window session.

### 12.5 Minimize and dock

Supported mini windows can be minimized or kept out of the way while the game continues.

## 13. Screen recorder

### 13.1 Recording permission

The recorder uses Android MediaProjection. Android shows a system confirmation because screen capture is sensitive.

### 13.2 Resolution choices

The available recording choices are 480p, 720p, and 1080p.

### 13.3 Start and stop controls

Recording can be started and stopped from the game tools.

### 13.4 Recording timer

A floating recording indicator shows that capture is active and displays elapsed time.

### 13.5 Saved recordings

Completed videos are stored in the REVROR recordings folder under the device Movies directory.

## 14. Wi-Fi and connection tools

### 14.1 Wi-Fi state

The Wi-Fi tool shows whether Wi-Fi is enabled and allows the supported Wi-Fi control for the Android version.

### 14.2 Network scan

REVROR can ask Android to scan for nearby Wi-Fi networks after the required nearby/location permission is granted.

### 14.3 Network list

The list shows the network name, security information, and signal information Android provides.

### 14.4 Connect to a network

The user can request connection to a supported saved, open, or password-protected network using Android's allowed connection flow.

### 14.5 Disconnect

The tool can request disconnection from the current Wi-Fi network where Android allows it.

### 14.6 Mobile-data shortcut

The game controls provide a mobile-data action or settings shortcut, depending on what the Android version allows third-party apps to change.

## 15. In-game display controls

### 15.1 Brightness lock

Brightness lock keeps the selected brightness while the game is active and restores the prior supported value afterward.

### 15.2 Color boost

Color boost applies the supported game-only color enhancement and removes it outside the game.

### 15.3 High refresh

High refresh requests the chosen supported high-refresh display mode for the game.

### 15.4 Touch boost

Touch boost requests the supported touch-response setting for the session.

## 16. Kernel tuning

Kernel controls are powerful. Root is normally required, and every control is device-dependent.

### 16.1 CPU governor

The governor decides how quickly the CPU raises or lowers its speed.

### 16.2 CPU minimum and maximum frequency

These controls set the lowest and highest CPU speeds allowed by the kernel.

### 16.3 Per-core and cluster controls

Supported devices can expose separate controls for individual CPU cores or CPU clusters.

### 16.4 CPU boost

CPU boost temporarily raises processor performance in response to supported events such as touch input.

### 16.5 CPU boost tunables

When the kernel exposes them, REVROR can manage CPU Boost, CPU Input Boost, Stune Boost, PowerHAL boost, and other supported boost drivers.

### 16.6 CPU hotplug

Hotplug controls decide when CPU cores go online or offline. REVROR supports the compatible hotplug driver found on the device rather than forcing a driver that is not present.

### 16.7 CPU voltage

Supported kernels can expose processor voltage controls. Incorrect voltage can cause crashes, so this feature is available only when the kernel supplies it.

### 16.8 GPU frequency

The GPU controls set supported graphics-processor frequency limits.

### 16.9 GPU governor and scaling

These controls decide how the graphics processor changes speed under load.

### 16.10 GPU helper drivers

REVROR can expose supported helpers such as Adreno Idler and devfreq boost when they exist in the kernel.

### 16.11 I/O scheduler

The I/O scheduler decides how storage read and write requests are ordered.

### 16.12 Read-ahead buffer

Read-ahead controls how much data Linux tries to read before an app directly requests it.

### 16.13 Advanced I/O settings

Compatible schedulers can expose extra scheduler-specific values through the advanced I/O page.

### 16.14 Swappiness

Swappiness controls how willing Linux is to move memory into swap or compressed swap.

### 16.15 VFS cache pressure

This controls how strongly Linux reclaims filesystem metadata caches.

### 16.16 Dirty-memory ratios

Dirty-ratio controls decide how much changed data may wait in memory before Linux writes it to storage.

### 16.17 ZRAM

ZRAM provides compressed memory used like swap. REVROR exposes available size, algorithm, and related controls when the device supports them.

### 16.18 ZSwap

ZSwap is another compressed caching layer for swap pages. It appears only on kernels that include it.

### 16.19 Kernel Samepage Merging

KSM looks for identical memory pages and merges them to save RAM. Its scanning behavior can be tuned where supported.

### 16.20 Low Memory Killer

LMK controls help Android decide when memory is low enough to close background processes.

### 16.21 TCP congestion algorithm

This chooses an available kernel method for controlling network traffic during congestion.

### 16.22 Entropy settings

Entropy controls expose supported Linux random-pool thresholds.

### 16.23 Thermal controls

Compatible thermal drivers expose their enable switches and tunables. Turning off thermal protection can damage a device, so these controls must be used carefully.

### 16.24 Battery controls

Supported kernels may expose charging current, voltage, charge limit, or related battery values.

### 16.25 Sound controls

Supported custom kernels can expose speaker, headphone, microphone, or digital gain controls.

### 16.26 Screen calibration

Compatible kernels can expose RGB, gamma, saturation, contrast, and other display-calibration values.

### 16.27 KLapse

REVROR Night Shift changes supported display color temperature over time. It can also be controlled through the REVROR Night Quick Settings tile.

### 16.28 Display and LED controls

Compatible devices can expose notification LED, backlight, and other display-driver settings.

### 16.29 Vibration controls

Supported kernels can expose vibration strength and related motor settings.

### 16.30 Power suspend

Power-suspend controls expose compatible kernel sleep behavior.

### 16.31 Wake gestures

Supported gestures include features such as double-tap to wake, double-tap to sleep, sweep to wake, sweep to sleep, and other driver-provided wake controls.

### 16.32 Wakelock controls

Wakelock tools show compatible kernel wakelocks and allow supported blocking controls. A wakelock is a request that keeps part of the phone awake.

### 16.33 SELinux control

On supported rooted devices, REVROR can show or request enforcing/permissive state. Enforcing is the safer normal state.

### 16.34 Miscellaneous kernel controls

Any supported kernel values that do not fit a larger group are placed in the miscellaneous section instead of being hidden.

## 17. Advanced tools

### 17.1 Device details

The details pages show available hardware, software, memory, CPU, input-device, and kernel information.

### 17.2 CPU time statistics

CPU statistics show how long the processor has spent at supported frequency states and in deep sleep.

### 17.3 Terminal

The built-in terminal lets an advanced user run commands with the available privilege level.

### 17.4 Script editor

The editor lets an advanced user create or edit shell scripts.

### 17.5 Script manager

Saved scripts can be listed, selected, and run. Scripts can make powerful system changes, so the user is responsible for script content.

### 17.6 Script Quick Settings tile

The Script tile provides fast access to the configured script from Android Quick Settings.

### 17.7 Apply script action

REVROR accepts its supported apply-script flow so a chosen script can be run through the app's controlled interface.

### 17.8 Custom controls

Advanced users can define supported custom kernel or file controls that are not already included in the normal lists.

### 17.9 Path reader

Path Reader reads a selected system or kernel path so the user can inspect the value exposed there.

### 17.10 Build property editor

The build-property tool reads and edits supported Android system properties with the required privilege. A wrong property can stop Android from booting correctly.

### 17.11 Backup

The backup tool saves supported REVROR settings or control data so it can be restored later.

### 17.12 Profile import and export

Profiles can be moved in or out of REVROR using the supported profile file format.

### 17.13 Flashing tool

The flashing page handles supported kernel or package flashing tasks. This is a root-level advanced feature and must be used only with files intended for the exact device.

### 17.14 Kernel updater

The updater supports the app's compatible kernel-update workflow when an appropriate update source is configured.

### 17.15 Spectrum support

Spectrum integration provides compatible kernel profile control when the installed kernel supports Spectrum.

### 17.16 REVROR profile tiles

The old externally branded profile tile is replaced by REVROR Profile, Gaming, CoolDown, HUD, Night, and Script tiles with dedicated icons and live state labels.

### 17.17 DashClock extension

The included extension can provide supported REVROR information to a compatible DashClock host.

## 18. General app tools

### 18.1 CPU floating overlay

The standalone CPU overlay can show available per-core frequency, load, governor, battery, and GPU information above other apps.

### 18.2 Overlay permission handling

Android's “display over other apps” permission is required for floating bubbles, crosshair, filters, timers, recorder indicators, calculators, and performance overlays.

### 18.3 Usage-access detection

Usage access helps REVROR identify which app is in the foreground. This is how game-only tools stay on the game instead of covering every screen.

### 18.4 Boot receiver

The boot receiver notices when Android has started and runs only the enabled apply-on-boot work.

### 18.5 Automatic restoration

Game-session features remember supported previous values and restore them when the game session ends.

### 18.6 Dashboard banner personalization

The dashboard can use a preset or user-selected banner image and a saved blur amount.

### 18.7 Animated home banners

A simple switch turns live home-banner animation on or off. When enabled, every preset or custom image receives continuously rendered energy particles, light sweeps, moving scan lines, chromatic glitch slices, and an animated energy wave. The image is not merely zoomed in and out. Two additional original anime-gaming choices are included: Anime Arena Vanguard and Waifu Cyber Squad.

### 18.8 Custom banner text

The user can set the dashboard or Game Space banner text and choose from the included text presets.

### 18.9 Android button configuration

Supported button actions can be configured through the app's button-control page.

### 18.10 Power-button gesture service

The service handles the configured supported power-button gesture behavior when the required Android access is available.

### 18.11 Feedback settings

Feedback controls decide which supported app sounds, vibration, or response signals are used.

## 19. Updates, help, and information

### 19.1 Update checker

REVROR checks the configured release source for a newer version and shows the available release information.

### 19.2 Update channel

The update-channel page controls which supported release stream the update checker uses.

### 19.3 Release page opening

When an update is available, the app can open the release or download page for the user.

### 19.4 Bug report

The bug-report action prepares useful app and device context so a problem can be described more clearly.

### 19.5 FAQ

The FAQ explains common usage questions inside the app.

### 19.6 About and licenses

The About area shows app information, version details, contributors, and open-source license information.

### 19.7 Developer and community links

The app can open its configured developer, community, Telegram, feedback, and donation links.

## 20. Important limits

### 20.1 REVROR cannot create missing hardware features

If the display supports only 60 Hz, asking for 120 Hz does not manufacture a 120 Hz panel. The same rule applies to touch rate, GPU controls, LEDs, charging controls, and kernel drivers.

### 20.2 Android may block an action

New Android versions restrict Wi-Fi, mobile data, processes, storage, overlays, and notifications. REVROR uses the access Android or the selected privilege service actually grants.

### 20.3 Root changes can be dangerous

CPU voltage, thermal protection, system properties, flashing, and arbitrary scripts can make a phone unstable or unbootable. These features are for users who understand the value they are changing.

### 20.4 Game-only means game-only

The crosshair, Hunter inversion, Sketch filter, other screen filters, brightness lock, color boost, and game floating tools are tied to the selected active game. They are cleaned up when the game ends or leaves the foreground.

### 20.5 A successful switch still depends on support

A saved option means REVROR remembers and attempts that behavior. The final result still depends on Android permissions, root/Shizuku state, the device ROM, the kernel, and the hardware.
