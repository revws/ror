# Security Policy

## Supported Versions

We provide security and bug updates for the active release branch of ROR.

| Version | Supported          | Notes |
| ------- | ------------------ | ----- |
| 51.x    | :white_check_mark: | Current active release branch |
| < 51.0  | :x:                | Deprecated |

---

## Reporting a Vulnerability

The ROR team takes security and hardware safety seriously. Because ROR operates with elevated system permissions (`root` via `libsu` and `Shizuku`), ensuring that command injection or privilege escalations are prevented is critical.

If you believe you have found a security vulnerability in ROR:
1. **Do not disclose the vulnerability publicly** in GitHub issues, forums, or social media.
2. Please report security issues privately through [GitHub Private Vulnerability Reporting](https://github.com/revror/ROR/security/advisories/new) or by emailing the project maintainers directly.
3. Include:
   - Type of vulnerability (e.g. arbitrary command injection, privilege leak, unsafe IPC).
   - Step-by-step reproduction instructions or a minimal Proof of Concept (PoC).
   - Affected devices, OS versions, and ROR build numbers.

---

## Response Timeline
- **Initial Acknowledgement**: Within 48 hours.
- **Triage & Assessment**: Within 5 business days.
- **Fix & Public Advisory**: Coordinated release within 30 days of confirmed triage.
