# REVROR open-source security and build notes

REVROR is distributed as open source under the licenses present in this repository. Open-source code cannot be made
“uncopyable”: recipients must be able to read, modify, and redistribute code as allowed by its license. Claims to the
contrary would be misleading. REVROR instead uses practical, auditable hardening.

## Identity and release hardening

- The public application ID is `com.revror.revws`. `AppIntegrity` refuses to start a build under another runtime ID.
- Release builds enable R8 code shrinking, optimization, resource shrinking, and obfuscation.
- Updates are accepted only from HTTPS URLs hosted at `revror.pages.dev`.
- Release APKs should always be signed with the same private release key. Never commit that key or its passwords.
- Android/Play Protect must remain enabled. REVROR does not attempt to hide installation or bypass device security.
- Root and Shizuku commands are visible in source and are invoked only after the user enables the corresponding access.

These measures deter casual repackaging; a determined person can remove checks from a fork. Trademark policy and
signed-release verification are the appropriate ways to distinguish official builds.

## Access model

| Capability | Standard | Shizuku | Root |
| --- | --- | --- | --- |
| Live CPU/battery/RAM display | Yes | Yes | Yes |
| Overlay and Android settings | Yes | Yes | Yes |
| App force-stop/disable, Doze, Data Saver, refresh rate | No | Yes | Yes |
| CPU/GPU frequencies, I/O scheduler, entropy, SELinux | No | No | Yes |
| Full `library` profile engine | No | No | Yes |

Shizuku normally runs with Android shell privileges, not UID 0. The UI therefore never presents Shizuku as a way to
write protected kernel/sysfs nodes.

## Reproducible build outline

1. Install JDK 17 and Android SDK 34.
2. Set `JAVA_HOME` and `ANDROID_HOME`.
3. Run `gradlew :app:assembleFdroidRelease`.
4. Inspect and sign `app/build/outputs/apk/fdroid/release/app-fdroid-release-unsigned.apk` with the official release key.
5. Verify the package name, signing certificate, version code, and requested permissions before publishing.

## Third-party attribution

Upstream package namespaces and copyright headers—such as the retained Spectrum compatibility classes—remain in the
source for license compliance. They are implementation attribution, not user-facing REVROR branding. Shizuku is integrated
through its documented API/provider artifacts. Banner artwork in `drawable-nodpi/ror_banner_*.png` was created for REVROR.

