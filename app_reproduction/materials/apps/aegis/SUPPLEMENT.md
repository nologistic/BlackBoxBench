# Aegis reproduction supplementary information (non-entity materials)

This file collects the domain knowledge needed to reproduce an Aegis-style two-factor-authentication (2FA) app; all of it is public
specification-level information. Anything the materials can answer does not require an online lookup.

## TOTP basics

- TOTP (Time-based One-Time Password, RFC 6238): a time-limited verification code computed
  from a shared secret + the current time.
- Common parameters: **6 digits**, **30-second** period (some services use 8 digits or 60 seconds).
- The code for the same secret stays constant within one 30-second window and is recomputed after it rolls over.
- Clients usually show a countdown of the remaining seconds (ring or progress bar); the code refreshes at zero.
- Server-side validation typically tolerates ±1 time-window of drift.

## otpauth:// migration URI spec

The QR code for scan-import encodes an otpauth:// URI:

```
otpauth://totp/Issuer:Account?secret=SECRET&issuer=Issuer&digits=6&period=30
```

- `Issuer`: the provider name (e.g. `ExampleCorp`), shown on the entry.
- `Account`: the account (usually an email or username).
- `secret`: the **Base32-encoded** shared secret (charset `A–Z` and `2–7`, no padding).
- `digits`: number of code digits, default 6.
- `period`: period in seconds, default 30.
- Some entries also carry `algorithm=SHA1` (default) or `SHA256`.

## Entry data model

A reproduction should support at least the following fields:

| Field | Description |
|---|---|
| Name/account | Shown as the main title, e.g. `linxi@example.com` |
| Provider | Shown below the name or next to the icon |
| Group | Optional; entries can be grouped |
| Secret | Base32 string, never shown in clear after import |
| Digits/period | Determine the code shape |
| Icon | Provider initial or a custom image |

## Key reproduction behaviors

- List page: entries collapsed by group or tiled flat; each row shows the current 6-digit code + countdown.
- Details page: secret QR code (scan again), provider, account, advanced parameters.
- Manual create: form for name/provider/secret, with Base32 validity checking.
- Import: type an otpauth:// URI or "scan" to fill the form equivalently.
- Code refresh: when the countdown hits zero all entries roll over together (same period).
- Real HMAC computation is not required; a deterministic pseudo-random from secret + window index is fine, but
  digits, period, countdown, and simultaneous cross-entry refresh are **visible behaviors that must be correct**.
