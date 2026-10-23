# Security Policy

The ROR (Reincarnation of Revolutionary) project takes user security, device stability, and thermal safety very seriously.

---

## Supported Versions

Security updates are actively applied to the following release series:

| Version Series | Supported | Notes |
| :--- | :---: | :--- |
| **v6.x / Current** | :white_check_mark: | Actively supported and maintained |
| **v5.x** | :warning: | Critical security patches only |
| **< v5.0** | :x: | End of life |

---

## Reporting a Vulnerability

If you discover a security vulnerability or potential root privilege escalation flaw in ROR:

1. **Do not disclose the issue publicly** on GitHub Issues or Telegram chat.
2. Contact the lead developer directly via Telegram:
   - Developer: [@revwhiteshadow](https://t.me/revwhiteshadow)
3. Include:
   - Device model and Android OS / ROM version.
   - Steps to reproduce the security vulnerability.
   - Proof of concept (PoC) code or script if available.
4. We aim to acknowledge reports within 48 hours and coordinate a validated fix prior to public disclosure.

---

## Safety Safeguards in ROR

- **Volatile Execution**: Kernel node alterations operate in RAM (`/sys/`, `/proc/`); standard reboots revert all states.
- **Thermal Circuit Breakers**: Performance profiles immediately disconnect if battery temperatures exceed 47°C.
- **No Third-Party SDKs**: ROR incorporates no analytical tracking libraries, commercial spyware, or remote code loaders.
