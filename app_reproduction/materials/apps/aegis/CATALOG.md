# Aegis reproduction supplementary materials

This directory is the target-specific material pack for the `aegis` (two-factor authentication token app) reproduction workspace, mounted as
read-only `/materials/app`. All entries are fictional test data and contain no real accounts.

## Entity materials

- `otpauth_uris.txt`: 12 fictional otpauth:// migration URIs covering different providers,
  accounts, and groups; usable directly as demo data for scan-import / manual entry.
- `entries.json`: structured entry data corresponding to the list above (name/provider/group/secret/digits/
  period), suitable for direct embedding or generating UI lists.
- Entry icons can reuse the fictional avatars in `/materials/common/images/avatars/`.

## Non-entity supplementary information

See `SUPPLEMENT.md`: TOTP concepts, the otpauth:// URI spec, Base32 secret format,
verification-code behavior (digits/period/countdown), and other information needed for reproduction.

## Usage rules

Do not modify this directory. To use a file, copy it into the `/workspace` project first and reference it there.
